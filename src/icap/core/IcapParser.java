/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008 Karel Mittig
 *-----------------------------------------------------------------------------
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *  Please refer to the LICENSE.txt file that comes along with this source file
 *  or to http://www.gnu.org/licenses/gpl.txt for a full version of the license.
 *
 *-----------------------------------------------------------------------------
 * For any comment, question, suggestion, bugfix or code contribution please
 * contact Karel Mittig : karel [dot] mittig [at] gmail [dot] com
 * Created	:	27 janv. 2006
 *---------------------------------------------------------------------------*/
package icap.core;

///////////////////////////////////
//Import
import java.io.*;
import java.util.Hashtable;
import tools.general.Base64;
import tools.logger.Log;
import tools.general.ExtendedByteArrayOutputStream;
///////////////////////////////////


/**
 * ICAP protocol parser.<br>
 * Determine ICAP request type (OPTIONS/REQMOD/RESPMOD), parse ICAP request 
 * and embedded HTTP messages for REQMOD and RESPMOD.   
 * @author Mittig
 */
public abstract class IcapParser extends Thread implements Icap {
//	<------------------------------------------------------------------------------------------>

	//public static int debug = 0;
	//private final static boolean dev = false;
	/**
	 * Type of current parsed ICAP exchange
	 * values are: OPTIONS, REQMODE or RESPMODE
	 * */
	public TYPE type = TYPE.INVALID;

	/** Generic ICAP client implementation*/
	protected ClientBrand brand = ClientBrand.OTHER;
	
	/** Set if TCP connection must stay opened */
	boolean connection_opened = true;

	/**timer used to determine processing time*/
	public long starttime=  0;

	/**HTTP Parsed Informations*/
	protected String firstline="";
	/**The complete requested URL: http://<host>:<port>/<path>?<searchpart>*/
	protected String req_url="";
	/**The requested URL, without the search part: http://<host>:<port>/<path>*/
	protected String req_url_path="";
	/**The requested URL search part (URL parameters), without the '?': <searchpart>*/
	protected String req_url_searchpart="";
	/**HTTP requested server, without the port*/
	protected String host=null;
	/**HTTP request method: GET, POST, HEAD, PUT, DELETE, ...*/
	protected String httpmethod="";
	
	/** for preview greater than 0, indicates if preview bytes have been read */
	protected boolean previewreaded = false;
	/** indicates if all REQ/RESP body has been read during preview (preview>content length)*/
	protected boolean previewstarved = false;
	/** indicates if all REQ/RESP body has been read*/
	protected boolean bodyreaded = false;
	
	/**HTTP Request header*/
	protected StringBuilder reqHeader= new StringBuilder();
	/**HTTP Request body (byte format)*/
	protected ExtendedByteArrayOutputStream reqBody= new ExtendedByteArrayOutputStream();

	/** ICAP request headers */	protected Hashtable<String, String> icapHeaders = new Hashtable<String, String>();
	/** HTTP request headers */
	protected Hashtable<String, String> httpReqHeaders = new Hashtable<String, String>();
	/** HTTP response headers */
	protected Hashtable<String, String> httpRespHeaders = new Hashtable<String, String>();
	/**HTTP Request/Response body size*/
	long contentLength = -1;
	/**HTTP Response code (RESPMOD only)*/
	protected int rescode=502;
	protected StringBuilder resHeader= new StringBuilder();
	/**HTTP Response body (byte format)*/
	protected ExtendedByteArrayOutputStream resBody = new ExtendedByteArrayOutputStream();



	/**ICAP Parsed Information*/
	protected StringBuilder icapHeader = new StringBuilder();
	protected String i_encapsulated="";
	protected String i_service="";
	protected int i_req_hdr=0;
	protected int i_res_hdr=0;
	protected int i_res_body=0;
	protected int i_null_body=0;
	protected int i_req_body=0;
	protected int preview=-1;
	/**set if client supports ICAP 204 (outside preview)*/
	protected boolean allow_204=false;
	/**set if client supports ICAP 206 (outside preview)*/
	protected boolean allow_206=false;

	/**Thread ID (for trace and debug)*/
	protected int id =0;
	/**internal counters used in traces to show thread ID*/
	private static int internalcounter = 0;
	
//	<------------------------------------------------------------------------->    
	/**
	 * Create an ICAP parser. Class extends thread in order to make parser threadable.
	 * @param threadName Thread ID/name (mostly for debugging purpose)
	 */
	public IcapParser(String threadName){
		super();
		this.id = internalcounter++;
		this.setName(threadName+"-"+id);
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * @return Server (Service) name
	 */
	public abstract String getServerName();
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Close client connection
	 */
	public void closeConnection(){
		connection_opened = false;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Parse given buffer to find icap header<br>
	 * iCAP request is stored in icapHeader BufferedString<br>
	 * @param bufferedreader the buffer to parse
	 * @return iCAP request type: OPTIONS, REQMODE, RESPMODE
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public TYPE parseIcapHeader(DataInputStream bufferedreader) throws Exception {
		String readline = "";
		int traildot;
		type = TYPE.INVALID;
		allow_204 = false;
		allow_206 = false;
		//record processing time
		starttime = System.currentTimeMillis();

		//-----------------------------------------------------------------------------------
		//read and parse first line to retrieve request type: OPTIONS, REQMOD, RESPMOD 
		//-----------------------------------------------------------------------------------
		try {
			while (type==TYPE.INVALID){
				readline = bufferedreader.readLine();
				if (readline==null) {
					if (Log.finest()) Log.trace(Log.FINEST, "----------CLOSED BUFFER-----------");
					return TYPE.INVALID;
				}
				if (!readline.startsWith("REQMOD") && !readline.startsWith("RESPMOD") && !readline.startsWith("OPTIONS")) {
					if (readline.trim().equals("")) continue;
					return TYPE.INVALID;
					//continue;//unknown or invalid intermediary data (unreaded chunk for example)=> ignore
				} 

				icapHeader.append(readline).append(CRLF);
				readline = readline.toLowerCase(); // case insensitive
				
				// Options request: fast return
				if (readline.startsWith("options")) {
					while(!(readline = bufferedreader.readLine()).equals("")) continue;
					if (Log.finer()) Log.trace(Log.FINER, "---------------------");
					return TYPE.OPTIONS;
				}
				i_service = readline.substring(readline.indexOf("://")+3,readline.lastIndexOf(" ")); // get requested icap url
				//chek for req or resp mode
				if (readline.startsWith("reqmod")) {
					type = TYPE.REQMOD;
				} else if (readline.startsWith("respmod")) {
					type = TYPE.RESPMOD;
				} else {
					//nothing founded => abort
					if (Log.finest()) Log.trace(Log.FINEST, "----------UNKNOWN REQUEST : "+readline);
					return TYPE.INVALID;
				}
				if (readline.indexOf("brand=netapp")!=-1){
					this.brand = ClientBrand.NETAPP;
				} else {
					this.brand = ClientBrand.OTHER;
				}
				if (Log.finest()) Log.trace(Log.FINEST,"ICAP Client brand:" + brand.toString());
			}
			if (Log.finest()) Log.trace(Log.FINEST, "----------EMPTY REQUEST-----------");
		} catch (Exception e){
			Log.error(Log.FINE, "ICAP Parser => Connection resetted by ICAP Client");
			return type;
		}

		String lowercase = "";
		//-----------------------------------------------------------------------------------
		// Type is either REQMODE or RESPMODE => Let's parse header
		//-----------------------------------------------------------------------------------
		while((readline = bufferedreader.readLine()) != null)  {

			icapHeader.append(readline).append(CRLF);

			//Empty line => end of icap header => abort parsing
			if (readline.length()==0) {
				break;
			}

			lowercase = readline.toLowerCase();//put readed line in lowercase to avoid parsing error

			if (lowercase.startsWith("preview:")){
				preview = Integer.parseInt(readline.substring(9).trim());
			}
			
			if (lowercase.startsWith("allow:")){
				allow_204 = lowercase.contains("204");
				allow_206 = lowercase.contains("206");
			}

			if (lowercase.startsWith("encapsulated:")){
				i_encapsulated = lowercase.substring(14).trim();
				int pos,pos1;
				pos = i_encapsulated.indexOf("req-hdr=");
				if (pos>0){
					pos1 = i_encapsulated.indexOf(",",pos+8);
					i_req_hdr = Integer.parseInt(i_encapsulated.substring(pos+8,pos1).trim());
				}
				//REQMOD: parse req-hdr and null-body parameters
				if (type==TYPE.REQMOD){
					if (i_encapsulated.indexOf("req-body=")>0)
						i_req_body = Integer.parseInt(i_encapsulated.substring(i_encapsulated.lastIndexOf("=")+1).trim());
					else 
						i_null_body = Integer.parseInt(i_encapsulated.substring(i_encapsulated.lastIndexOf("=")+1).trim());
				} else {
					//RESPMOD: parse res-hdr and res-body parameters
					pos = i_encapsulated.indexOf("res-hdr=");
					if (pos>0){
						pos1 = i_encapsulated.indexOf(",",pos+8);
						i_res_hdr = Integer.parseInt(i_encapsulated.substring(pos+8,pos1).trim());
					}
					pos = i_encapsulated.indexOf("res-body=");
					if (pos>0){
						i_res_body = Integer.parseInt(i_encapsulated.substring(pos+9).trim());
					} else {
						pos = i_encapsulated.indexOf("null-body=");
						i_null_body = Integer.parseInt(i_encapsulated.substring(pos+10).trim());
					}             
				}
				continue;
			} 
			//If here, standard header => put it in ICAP header hashtable
			try{
				traildot = readline.indexOf(":"); 
				//B64 decoding for specific ICAP headers
				if (lowercase.startsWith("x-authenticated-user") 
						|| lowercase.startsWith("x-authenticated-groups")) {
					icapHeaders.put(lowercase.substring(0,traildot), Base64.decodeString(readline.substring(traildot+2)));
					if (Log.finer()) Log.trace(Log.FINER, "****BASE64 decoding["+Base64.decodeString(readline.substring(traildot+2))+"]");
					continue;
				}
				icapHeaders.put(lowercase.substring(0,traildot), readline.substring(traildot+2));
			} catch (Exception e){
				if (Log.info()) Log.error(Log.INFO,"ICAP Parser error (parseIcapHeader)",e);
			}
		}
		if (Log.finer()) Log.trace(Log.FINER, icapHeader.toString()+"---------------------");
		return type;
	}
//	<------------------------------------------------------------------------------------------>
	

//<------------------------------------------------------------------------------------------>
/**
 * Return parsed message type (REQMODE, RESPMODE, OPTIONS, INVALID)
 * @return ICAP service Type (REQMODE, RESPMODE, OPTIONS, INVALID)
 */
public TYPE getType(){
	return this.type;
}
//<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Extract host name from given URL<br>
	 * @param url the URL to parse
	 * @return server host name embedded in given URL
	 */
	public final static String extractHostName(String url){
		if (url.indexOf("/")==-1) return url;
		String tmp = url.toLowerCase();
		if (tmp.startsWith("http://")) tmp = tmp.substring(7);
		int n=0;
		if ((n = tmp.indexOf("/"))!=-1){
			tmp = tmp.substring(0,n);
		}
		return tmp;
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Extract host name and content accessed from given url<br>
	 * @param url the full URL to parse
	 * @return [host][content]
	 */
	public final static String[] splitHostContent(String url){
		String[] resp = new String[]{"",""};
		if (url.indexOf("/")==-1) {
			resp[0] = url;
			return resp;
		}
		String tmp = url;
		if (tmp.startsWith("http://")) tmp = tmp.substring(7);
		int n=0;
		if ((n = tmp.indexOf("/"))!=-1){
			resp[0] = tmp.substring(0,n);
			resp[1] = tmp.substring(n+1);
		}
		if (resp[1].endsWith("/")) resp[1] = resp[1].substring(0,resp[1].length()-1);
		return resp;
	}
//	<------------------------------------------------------------------------------------------>


	/**
	 * Returns the HTTP request body
	 * @return Returns the HTTP request body, or null if not available.
	 */
	public ExtendedByteArrayOutputStream getReqBody() {
		return reqBody;
	}
	/**
	 * Returns the HTTP response body
	 * @return Returns the HTTP response body, or null if not available.
	 */
	public ExtendedByteArrayOutputStream getRespBody() {
		return resBody;
	}
	/**
	 * Returns true if HTTP request contains a body (ReqBody is never provided in response mode)
	 * @return Returns true if HTTP request contains a body.
	 */
	public boolean containsReqBody() {
		if (reqBody==null || reqBody.size()==0) return false;
		return true;
	}
	/**
	 * Extract and return HTTP message first line (should normally be the client GET/POST/PUT/... or server response code 
	 * @return Returns the HTTP request/response first line.
	 */
	public String getFirstline() {
		return firstline;
	}
	/**
	 * Extract and return HTTP server hostname
	 * @return Returns the requested host.
	 */
	public String getHost() {
		if (host==null) return extractHostName(req_url_path);
		return host;
	}

	/**
	 * Retrieve header value from ICAP request.<br>
	 * All ICAP headers names are stored using lowercase to avoid case sensitive problems.
	 * @param headername ICAP Header name to retrieve
	 * @return Returns the header value of given header name (or null if none) in iCAP request.<br>
	 * All headers are stored in lower case.
	 */
	public String getIcapHeader(String headername) {
		return this.icapHeaders.get(headername);
	}
	/**
	 * Retrieve a specific header value from HTTP request header
	 * Headers are stored in lowercase to be case insensitive
	 * @param headername HTTP header name to retrieve from request
	 * @return Returns the header value of given header name (or null if none) in HTTP request.
	 */
	public String getReqHeader(String headername) {
		return this.httpReqHeaders.get(headername);
	}
	/**
	 * Returns the complete HTTP request header 
	 * @return a String containing the entire request header 
	 */
	public String getRequestHeaders(){
		return this.reqHeader.toString();
	}
	/**
	 * Replace HTTP request headers by the provided String
	 * @param requestHeaders String containing the new request header to set
	 */
	public void setRequestHeaders(String requestHeaders){
		this.reqHeader.setLength(0);
		this.reqHeader.append(requestHeaders);
	}
	/**
	 * Retrieve a specific header value from HTTP response header
	 * Headers are stored in lowercase to be case insensitive
	 * @param headername HTTP header name to retrieve from HTTP response
	 * @return The value of provided header name (or null if not available) 
	 */
	public String getRespHeader(String headername) {
		return this.httpRespHeaders.get(headername);
	}
	
	/**
	 * Update response header with given value
	 * @param headername header parameter's name to update
	 * @param value the value to set
	 */
	public void updateRespHeader(String headername,String value) {
		this.httpRespHeaders.put(headername,value);
	}
	/**
	 * Update request header with given value
	 * @param headername header parameter's name to update
	 * @param value the value to set
	 */
	public void updateReqHeader(String headername,String value) {
		this.httpReqHeaders.put(headername,value);
	}
	
	/**
	 * Returns the complete HTTP response header
	 * @return Returns the entire HTTP response header 
	 */
	public String getResponseHeaders(){
		return this.resHeader.toString();
	}
	/**
	 * Replace HTTP response headers by the provided String
	 * @param responseHeaders String containing the new response header to set
	 */
	public void setResponseHeaders(String responseHeaders){
		this.resHeader.setLength(0);
		this.resHeader.append(responseHeaders);
	}
	/**
	 * Returns the full HTTP URL of parsed HTTP message.<br>
	 * URL will have following syntax:<br>
	 * "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]<br>
	 * @return Returns the full HTTP URL of parsed HTTP message.
	 */
	public String getReqUrl() {
		return req_url;
	}
	/**
	 * Returns the absolute path of parsed HTTP message.<br>
	 * Absolute Path is defined as the URL minus the parameters query<br>
	 * For example, an URL like http://host[:port]/ressource.html?param=value
	 * will return http://host[:port]/ressource.html as absolute path <br>
	 * @return Returns the absolute path of parsed HTTP message
	 */
	public String getAbsolutePath() {
		return req_url_path;
	}


	/**
	 * Returns the query of parsed HTTP message.<br>
	 * Query is defined as the parameters given at the end of the URL using "?"<br>
	 * For example, an URL like http://host[:port]/ressource.html?param=value
	 * will return param=value as query<br>
	 * @return Returns the query of parsed HTTP message
	 */
	public String getQuery() {
		return req_url_searchpart;
	}


	/**
	 * @return HTTP method from request (GET/POST/...)
	 */
	public String getRequestMethod() {
		return httpmethod;
	}
	
	/**
	 * @return HTTP Status code (200, 401, ...)
	 */
	public int getStatusCode() {
		return rescode;
	}
}
