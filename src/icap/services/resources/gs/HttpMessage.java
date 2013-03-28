/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2009 Karel Mittig
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
 *-----------------------------------------------------------------------------*/
package icap.services.resources.gs;

////////////////////////////
//Import
import java.util.concurrent.ConcurrentHashMap;
import icap.core.*;
import tools.general.Compressor;
import org.json.*;
////////////////////////////


/**
 * Class used to provide HTTP message to native scripts
 * Provides several methods of common use to manipulate content  
 * @author Karel
 */
public class HttpMessage {
	
	private icap.core.Icap.TYPE type;
	private StringBuilder requestHeaders;
	protected String requestBody;
	private StringBuilder responseHeaders;
	protected String responseBody;
	private String username;
	private String usergroup;
	private AbstractService service;
	
	ConcurrentHashMap<String, Object> sharedCache;
	
//	<------------------------------------------------------------------------->  
	/**
	 * Construct an HTTP message instance that will be processed by a Java native script
	 * @param service ICAP service called
	 * @param type ICAP message type (either REQ or RESP)
	 * @param requestHeaders HTTP Request headers
	 * @param requestBody HTTP Request body (for POST mainly)
	 * @param responseHeaders HTTP Response headers
	 * @param responseBody HTTP Response body (if any)
	 * @param username User Name as extracted from ICAP or HTTP headers (see services.properties)
	 * @param usergroup User Group as extracted from ICAP or HTTP headers (see services.properties)
	 * @param sharedCache shared hash map used to store data to share between scripts
	 */
	public HttpMessage(AbstractService service, icap.core.Icap.TYPE type, String requestHeaders, String requestBody,
			String responseHeaders, String responseBody, String username,
			String usergroup, ConcurrentHashMap<String, Object> sharedCache) {
		this.type = type;
		this.requestHeaders = new StringBuilder(requestHeaders==null?"":requestHeaders);
		this.requestBody = requestBody;
		this.responseHeaders = new StringBuilder(responseHeaders==null?"":responseHeaders);
		this.responseBody =  responseBody;
		this.username = username;
		this.usergroup = usergroup;
		this.sharedCache = sharedCache;
		this.service = service;
	}
//	<------------------------------------------------------------------------->  
	
//	<------------------------------------------------------------------------->  
	/**
	 * Convenient method used to generate an HTTP message instance in REQ mode
	 * @param service ICAP service called
	 * @param requestHeaders HTTP Request headers
	 * @param requestBody HTTP Request body (for POST mainly)
	 * @param username User Name as extracted from ICAP or HTTP headers (see services.properties)
	 * @param usergroup User Group as extracted from ICAP or HTTP headers (see services.properties)
	 * @param sharedCache shared hash map used to store data to share between scripts
	 * @return HttpMessage
	 */
	public static HttpMessage newRequest(AbstractService service, String requestHeaders, String requestBody,
			String username,
			String usergroup, ConcurrentHashMap<String, Object> sharedCache) {
		return new HttpMessage(service, icap.core.Icap.TYPE.REQMOD, requestHeaders, requestBody,
				null, null, username,usergroup, sharedCache);
	}
	
	/**
	 * Convenient method used to generate an HTTP message instance in RESP mode
	 * @param service ICAP service called
	 * @param requestHeaders HTTP Request headers
	 * @param responseHeaders HTTP Response headers
	 * @param responseBody HTTP Response body (if any)
	 * @param username User Name as extracted from ICAP or HTTP headers (see services.properties)
	 * @param usergroup User Group as extracted from ICAP or HTTP headers (see services.properties)
	 * @param sharedCache shared hash map used to store data to share between scripts
	 * @return HttpMessage
	 */
	public static HttpMessage newResponse(AbstractService service, String requestHeaders,
			String responseHeaders, String responseBody, String username,
			String usergroup, ConcurrentHashMap<String, Object> sharedCache) {
		return new HttpMessage(service, icap.core.Icap.TYPE.RESPMOD, requestHeaders, null,
				responseHeaders, responseBody, username,usergroup, sharedCache);

	}
//	<------------------------------------------------------------------------->  
	
	/*************************************************
	 * 					GET Methods	 				 * 
	 *************************************************/
	

//	<------------------------------------------------------------------------->  
	/**
	 * @return HTTP request Header, in raw format
	 */
	public String getRequestHeaders() {return requestHeaders.toString();}
	/**
	 * @return HTTP response Header, in raw format
	 */
	public String getResponseHeaders() {return responseHeaders.toString();}
	
	/**
	 * @return User Name as authenticated by Squid, or user IP address otherwise
	 */
	public String getUsername() {return username;}
	/**
	 * @return User Group as authenticated by Squid, or User-Agent header as fallback
	 */
	public String getUsergroup() {return usergroup;}
	
	/**
	 * @return shared cache to share content between scripts
	 */
	public ConcurrentHashMap<String, Object> getSharedCache() {return sharedCache;}

	/**
	 * @return the HTTP body underlying bytes
	 */
	public byte[] getUnderlyingBytes(){
		if (this.type == Icap.TYPE.REQMOD){
			return this.service.getReqBody().toByteArray();
		} else {
			return this.service.getRespBody().toByteArray();
		}
	}
	/**
	 * @return The requested URL
	 */
	public String getUrl(){
		if (service!=null) return service.getReqUrl();
		return this.requestHeaders.substring(this.requestHeaders.indexOf(" ")+1, this.requestHeaders.indexOf("HTTP/"));
	}
	
	/**
	 * Return HTTP request header value
	 * @param key the HTTP request header to look for
	 * @return request header value, or null if non existing
	 */
	public String getRequestHeader(String key){
		return service!=null?service.getReqHeader(key.toLowerCase()):null;
	}
	/**
	 * Return HTTP response header value
	 * @param key the HTTP response header to look for
	 * @return response header value, or null if non existing
	 */
	public String getResponseHeader(String key){
		return service!=null?service.getRespHeader(key.toLowerCase()):null;
	}

	/**
	 * Return Icap request header value
	 * @param key the Icap request header to look for
	 * @return response header value, or null if non existing
	 */
	public String getIcapRequestHeader(String key){
		return service!=null?service.getIcapHeader(key.toLowerCase()):null;
	}
	
	public String getIcapRequestHeaders()
	{
		return service!=null?service.getIcapHeaders():null;
	}
//	<------------------------------------------------------------------------->  

	
	
//	<------------------------------------------------------------------------->
	/**
	 * Retrieve Message body (either request or response body depending on the message type)
	 * Specific REQ/RESP access not needed as REQBODY is not provided in RESPMODE  
	 * @return HTTP Message body (either request or response body depending on the message type)
	 */
	public String getBody() {
		if (this.type == Icap.TYPE.REQMOD){
			return this.requestBody;
		} else {
			return this.responseBody;
		}
	}
//	<------------------------------------------------------------------------->
	

	/*************************************************
	 * 					SET Methods	 				 * 
	 *************************************************/
	
//	<------------------------------------------------------------------------->  
	/**
	 * Replace HTTP message header by given one 
	 * (either request header or response header depending on the message type) 
	 * @param newHeader the new header to set
	 */
	public void setHeaders(StringBuilder newHeader) {
		if (this.type == Icap.TYPE.REQMOD){
			this.requestHeaders = newHeader;
		} else {
			this.responseHeaders = newHeader;
		}
		
	}
	/**
	 * Replace HTTP message header by given one 
	 * (either request header or response header depending on the message type) 
	 * @param newHeader the new header to set
	 */
	public void setHeaders(String newHeader) {
		setHeaders(new StringBuilder(newHeader));
	}
//	<------------------------------------------------------------------------->  

//	<------------------------------------------------------------------------->
	/**
	 * Add given header to HTTP Message
	 * This method does not override existing headers with same name
	 * @param headerName the Header name to add
	 * @param headerValue the header value to add
	 */
	public void addHeader(String headerName, String headerValue){
		StringBuilder h;
		if (this.type == Icap.TYPE.REQMOD){
			h = this.requestHeaders;
			if (service!=null) service.updateReqHeader(headerName.toLowerCase(),headerValue);
		} else {
			 h = this.responseHeaders;
			 if (service!=null) service.updateRespHeader(headerName.toLowerCase(),headerValue);
		}
		h.append(headerName).append(": ").append(headerValue).append("\r\n");
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Delete given header in HTTP Message header
	 * @param headerName the header to delete (case sensitive)
	 */
	public void deleteHeader(String headerName){
		String lhtf = "\r\n"+headerName.toLowerCase();
		String hlc;
		StringBuilder h;
		if (this.type == Icap.TYPE.REQMOD){
			hlc = requestHeaders.toString().toLowerCase();
			h = requestHeaders;
			
		} else {
			hlc = responseHeaders.toString().toLowerCase();
			h = responseHeaders;
		}
		int p1 = hlc.indexOf(lhtf);
		if (p1==-1) return;
		int p2 = hlc.indexOf("\r\n",p1+headerName.length());
		h.delete(p1, p2);
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Rewrite given header with the provided value
	 * @param headerName The header to rewrite
	 * @param newValue
	 */
	public void rewriteHeader(String headerName, String newValue){
		StringBuilder h;
		if (this.type == Icap.TYPE.REQMOD){
			h = this.requestHeaders;
			if (service!=null) service.updateReqHeader(headerName.toLowerCase(),newValue);
		} else {
			 h = this.responseHeaders;
			 if (service!=null) service.updateRespHeader(headerName.toLowerCase(),newValue);
		}
		int p1 = h.indexOf("\r\n"+headerName);
		if (p1==-1){
			h.append(headerName).append(": ").append(newValue).append("\r\n");
			return;
		} else {
			p1+=headerName.length()+4; //skip previous \r\n and trailing ":_", so +4
		}
		int p2 = h.indexOf("\r\n",p1);
		h.replace(p1, p2, newValue);
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Update request URL with given one
	 * @param newUrl The new URL to use
	 */
	public void setUrl(String newUrl){
		try{
		if (this.type != Icap.TYPE.REQMOD || service==null) return;

		int p1 = requestHeaders.indexOf("\r\n");
		String url = requestHeaders.substring(0, p1);
		int old_start = url.indexOf(' ')+1;
		int old_end = url.lastIndexOf(' ');
		String oldhost = service.getHost();
		
		if (newUrl.indexOf("://")==-1 && !service.getRequestMethod().equals("CONNECT")) {
			//relative path without host: do not modify host header
			newUrl = "http://"+service.getHost()+ (newUrl.startsWith("/")?newUrl:'/'+newUrl);
			requestHeaders.replace(old_start, old_end, newUrl);
			return;
		}
		
		if (service.getRequestMethod().equals("CONNECT")){
			requestHeaders.replace(old_start, old_end, newUrl);
			rewriteHeader("Host", newUrl);
			return;
		}
		p1 = newUrl.indexOf("://")+3;
		int p2 = newUrl.indexOf("/", p1+4);
		String newHost =  newUrl.substring(p1,p2);
		requestHeaders.replace(old_start, old_end, newUrl);
		if (!oldhost.equals(newHost)) rewriteHeader("Host", newHost);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
//	<------------------------------------------------------------------------->
	
	
//	<------------------------------------------------------------------------->
	/**
	 * @return either Request Method in REQMOD, or Status code in RESPMOD
	 */
	public String getType(){
		if (this.type == Icap.TYPE.REQMOD) return service.getRequestMethod();
		return service!=null?service.getStatusCode()+"":null;
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Replace HTTP message body by given one 
	 * (either request body or response body depending on the message type) 
	 * @param newBody the new body to set
	 */
	public void setBody(String newBody) {
		if (this.type == Icap.TYPE.REQMOD){
			this.requestBody = newBody;
		} else {
			this.responseBody = newBody;
		}
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Experimental: minify response content for CSS, HTML and JavaScript
	 */
	public void minify(){
		if (this.type != Icap.TYPE.RESPMOD || service==null) return;
		String contenttype = service.getRespHeader("content-type").toLowerCase(); 
		if (contenttype.contains("css")){
			this.responseBody = Compressor.cleanupCSS(this.responseBody);
			return;
		}
		if (contenttype.contains("html")){
			this.responseBody = Compressor.cleanupHTML(this.responseBody);
			//this.responseBody = Compressor.compressHtml(this.responseBody);
			return;
		}
		if (contenttype.contains("javascript")){
			this.responseBody = Compressor.cleanupJavaScript(this.responseBody);
			return;
		}
	}
//	<------------------------------------------------------------------------->
	
	
//	<------------------------------------------------------------------------->
	/**
	 * Experimental: provides simple&efficient converter from XML to JSON
	 */
	public void toJson(){
		if (this.type != Icap.TYPE.RESPMOD || service == null) return;
		String contenttype = service.getRespHeader("content-type").toLowerCase();
        if (contenttype==null || !contenttype.contains("xml")) return;
        try{
        	this.responseBody = XML.toJSONObject(this.responseBody).toString();
            this.rewriteHeader("Content-Type","application/json; charset=UTF-8");
        } catch (Exception e){
        }
	}
//	<------------------------------------------------------------------------->
	
}
