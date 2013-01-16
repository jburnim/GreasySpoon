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
 * Created  :   20 fev. 2007
 *---------------------------------------------------------------------------*/
package icap.core;

///////////////////////////////////
//Import
import icap.IcapServer;
import icap.ConnectionsPool;
import java.io.*;
import java.net.*;
import tools.logger.Log;
import tools.monitor.TrafficStatistics;
import tools.general.Compressor;
import tools.general.MimeMagic;
import tools.general.ExtendedByteArrayOutputStream;
///////////////////////////////////



/**
 * Abstract class silently providing all ICAP protocol parsing methods for both REQ and RESPMODE<br>
 * Services implementation must just provide getResponse() and getOptions() methods<br>
 * <p>
 * TODO: optimizations: add lazy parsing. Move methods to static.<br>
 * TODO: move configuration from static flat files to class method<br>
 * TODO: implement new DataInputStream class, with undeprecated readline() method<br> * <p>
 * @author mittig<br>
 */
@SuppressWarnings("deprecation")
public abstract class AbstractService extends IcapParser {

	/**Internal socket used to communicate with iCAP client*/
	private Socket socket;
	private BufferedOutputStream bufferedOutputStream;
	private DataInputStream dis;

	/**buffer used to parse requests*/
	final static int BYTEBUFFERSIZE = 65535;

	/**force TCP ACK (solve latency issue with some OS TCP stack implementation)*/
	private static boolean tcptweak = false;

	/**fake packet used to generate urgent data message (TCP optimization)*/
	private final static int URGENTDATA = 0;

	/**MTU size used to optimize chunk responses*/
	final static int MTU = 1448-7;

	/**Content length header size - simple optimization*/
	private final static int CONTENTLENGTHSIZE = "content-length: ".length();
	private final static int CONTENTTYPESIZE = "content-type: ".length();
	//private final static int CHARSETSIZE = "charset=".length();

	protected int RCODE = 500;

	/** ICAP server instantiating this service*/
	public IcapServer server;

	static ConnectionsPool connectionsPool;


	//	<------------------------------------------------------------------------->  
	/**
	 * Assign service in a task pool
	 * @param pool The task pool to which this service instance belongs
	 */
	public static void setPool(ConnectionsPool pool){
		connectionsPool = pool;
	}
	//	<------------------------------------------------------------------------->  

	//	<------------------------------------------------------------------------->    
	/**
	 * Default constructor. MUST be instantiate by implementing classes
	 * using super() constructor in order to ensure class initialization.
	 * @param _server The server which is instantiating this service
	 * @param clientsocket The ICAP client socket to proceed
	 */
	private AbstractService(IcapServer _server){
		super(_server.serverName);
		this.server = _server;
	} 
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->    
	/**
	 * Default constructor. MUST be instantiate by implementing classes
	 * using super() constructor in order to ensure class initialization.
	 * @param _server The server which is instantiating this service
	 * @param clientsocket The ICAP client socket to proceed
	 */
	public AbstractService(IcapServer _server,Socket clientsocket){
		super(_server.serverName);
		this.server = _server;
		if (clientsocket!=null)	this.setSocket(clientsocket);
	} 
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Assign given ICAP request to a thread
	 * @param clientsocket socket handling ICAP request
	 */
	public void assignTask(Socket clientsocket){
		this.socket = clientsocket;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**This methods offers possibility to services to flush their configuration on cleanup event*/
	public static void cleanup(){}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * @see icap.core.IcapParser#getServerName()
	 */
	public String getServerName(){
		return server.serverName;
	}
	//	<------------------------------------------------------------------------->   

	//	<------------------------------------------------------------------------->   
	/**
	 * Return service adapted OPTIONS message 
	 * @param bas the stream to fill with message
	 * @return ICAP response code
	 */
	public int getOptions(ByteArrayOutputStream bas) {
		bas.reset();
		if (!this.server.useKeepAliveConnections()) this.closeConnection();
		try {
			if (Log.finer()) Log.trace(Log.FINER, this.server.getOptions()+"---------------------");
			bas.write(this.server.getOptions().getBytes());
		} catch (Exception e){
			e.printStackTrace();
			return 500;
		}
		return 200;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Abstract method that must be provided by service in order to 
	 * tell running mode 
	 * @return Supported VectoringPoint: REQMOD, RESPMOD, REQRESPMOD
	 */
	public abstract VectoringPoint getSupportedModes();
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->    
	/**
	 * Abstract method that must be implemented by thread to respond to iCAP client<br>
	 * @param response stream to fill with the full response to send to iCAP client (ICAP response header AND body)
	 * @return ICAP response code associated to response stream 
	 */
	public abstract int getResponse(ByteArrayOutputStream response);
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Abstract method to implement - must return a literal (free format) description of the service
	 * @return a literal (free format) description of the service
	 */
	public abstract String getDescription();
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Try to find encoding for current content
	 * @param contenttype Response content type
	 * @return founded encoding
	 * @throws Exception
	 */
	public String getEncoding(String contenttype) throws Exception {
		int length = this.resBody.size()>5000?5000:this.resBody.size();
		byte[] array = this.resBody.getBytes(length);
		String content = new String (array,"ISO-8859-1");
		return MimeMagic.detectCharset(contenttype, content);
	} 
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Try to find encoding for given content
	 * @param contenttype Response content type
	 * @param content  content to parse to detect encoding
	 * @return founded encoding
	 * @throws Exception
	 */
	public String getEncoding(String contenttype,String content) throws Exception {
		int length = content.length()>5000?5000:content.length();
		String extract = content.substring(0, length);
		return MimeMagic.detectCharset(contenttype, extract);
	} 
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Check if content provided in HTTP body is compressed using HTTP protocol
	 * standard (mainly gzip)
	 * @return true if content is compressed, false otherwise
	 */
	public boolean isCompressed(){
		if (httpCompressionType()>0) return true;
		return false;
	}

	/**
	 * Check compression type: 0 for none, 1 for gzip, 2 for deflate
	 * @return
	 */
	private int httpCompressionType(){
		String ce = this.type==TYPE.RESPMOD ? getRespHeader("content-encoding") : getReqHeader("content-encoding");
		if ( ce == null) return 0;
		if (ce.contains("gzip")) return 1;
		if (ce.contains("deflate")) return 2;
		return 0;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Uncompress HTTP body if it's using standard HTTP compression method
	 * @param content the HTTP body (either from request or response)
	 * @return The uncompress body
	 * @throws Exception 
	 */
	public ExtendedByteArrayOutputStream uncompress(ExtendedByteArrayOutputStream content) throws Exception {
		switch (httpCompressionType()){
		case 0: return content;
		case 1:
			content = Compressor.gunzip(content);
			return content;
		case 2:
			content = Compressor.inflate(content);
			return content;
		default:
			return content;
		}
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Compress HTTP body using standard HTTP compression method
	 * Update HTTP headers accordingly
	 * @param content the HTTP body (either from request or response) to compress
	 * @return compressed content
	 * @throws Exception
	 */
	public ExtendedByteArrayOutputStream compress(ExtendedByteArrayOutputStream content) throws Exception {
		switch (httpCompressionType()){
		case 1:
			content = Compressor.gzip(content);
			return content;
		case 2:
			content = Compressor.deflate(content);
			return content;
		}

		switch (isHttpCompressible()){
		case 0: return content;
		case 1:
			content = Compressor.gzip(content);
			this.resHeader.delete(this.resHeader.length()-2,this.resHeader.length()).append("Content-Encoding: gzip\r\n\r\n");
			return content;
		case 2:
			content = Compressor.deflate(content);
			this.resHeader.delete(this.resHeader.length()-2,this.resHeader.length()).append("Content-Encoding: deflate\r\n\r\n");
			return content;
		}
		return content;
	}
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	private int isHttpCompressible(){
		if (this.type == TYPE.RESPMOD){
			// If an http content-encoding is defined, content is already compressed  
			String ce = getRespHeader("content-encoding");
			if (ce != null) return 0;
		}
		String ae = getReqHeader("accept-encoding");
		if (ae == null) return 0;
		if (ae.contains("gzip")) return 1;
		if (ae.contains("deflate")) return 2;
		return 0;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Read from client socket, call parse() to parse message and then call getResponse()<br />	 * TODO: create multiple chunks for responses <br />
	 */

	public void run()  {
		if (socket==null) {
			return;
		}
		do {
			while (this.socket==null){
				try{
					Thread.sleep(1000);
				} catch (java.lang.InterruptedException e){
				}
			}
			try {

				this.bufferedOutputStream = new BufferedOutputStream(this.socket.getOutputStream());				this.dis = new DataInputStream(new BufferedInputStream(this.socket.getInputStream(), BYTEBUFFERSIZE));				ByteArrayOutputStream response = new ByteArrayOutputStream();
				do {
					try {
						response.reset();

						TYPE requesttype = parse(this.dis); //Parse client request and retrieve method

						switch (requesttype){
						case INVALID:
							this.bufferedOutputStream.write(Icap._400CLIENTERROR); // Write back response to client
							this.connection_opened = false;
							break;
						case EMPTY:
							continue;
						case OPTIONS:
							this.RCODE = getOptions(response);
							break;
						case REQMOD:						case RESPMOD:
							readPreview();
							this.RCODE = getResponse(response);
							TrafficStatistics.hit();
							break;
						default://Normally can't exist, but who knows ?
							this.connection_opened = false;
							continue;
						}
						if (Log.finer()) Log.trace(Log.FINER, "THREAD ["+id+"] RESPONSE:\n---------------------\n"+response.toString()+"---------------------");
						response.writeTo(this.bufferedOutputStream); // Write back response to client

						if (this.brand==ClientBrand.NETAPP && this.RCODE==204 && this.preview==0 && requesttype==TYPE.REQMOD) {
							/**handle specific Network Appliance Netcache behavior when using preview of 0 size*/
							if (this.contentLength <= 0 ) break;
							this.clearZeroPreviewChunk();
						}

						if (this.connection_opened){//Connection still opened. Flush socket
							this.bufferedOutputStream.flush();
						}
						//Cannot manage persistent connections and thread pool simultaneously right now
						if (connectionsPool !=null) {
							this.bufferedOutputStream.flush();
							connection_opened = false;
						}
					} catch(java.net.SocketException socketex) {
						if (Log.fine()) Log.trace(Log.FINE,getServerName()+"=>Connection prematurely closed by ICAP client - End user aborted connection or transaction failure.",socketex);
						this.connection_opened = false;
					} catch(Exception exception) {
						exception.printStackTrace();
						if (Log.warning()) Log.error(Log.WARNING,getServerName()+" - Failure processing request in Thread ["+id+"]",exception);
						this.connection_opened = false;
					} catch(Throwable t) {
						if (Log.severe()) Log.error(Log.SEVERE,getServerName()+" - Failure processing request in Thread ["+id+"] -",t);
						this.connection_opened = false;
					} 
				} while (this.connection_opened);// End while
				//Close the connection
				this.bufferedOutputStream.close();
				this.dis.close();
				this.socket.close();
			} catch(Exception e) {
			} finally {
				if (this.bufferedOutputStream != null) try {this.bufferedOutputStream.close();} catch (Exception e){}
				if (this.dis != null) try {this.dis.close();} catch (Exception e){}
				if (this.socket!= null && !this.socket.isClosed()) try {this.socket.close();} catch (Exception e){}
			}
			if (Log.finer()) Log.trace(Log.FINER, getServerName()+"=>THREAD ["+id+"] CLOSED \r\n---------------------");
			if (connectionsPool !=null) this.socket = connectionsPool.restoreInPool(this);
		} while (!this.server.useKeepAliveConnections());
	}
	//	<------------------------------------------------------------------------------------------>

	
	//	<------------------------------------------------------------------------->
	/**
	 * Return an ICAP 200 OK message with full message content (content can be modified or not) 
	 * @param bas Byte stream containing ICAP request body
	 * @return 200 OK
	 * @throws Exception
	 */
	public int fullResponse(ByteArrayOutputStream bas) throws Exception {
		StringBuilder sb = new StringBuilder();

		sb.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF).append(server.icaphost);
		switch (this.type){
			case REQMOD:
				sb.append("Encapsulated: req-hdr=0");
				if (this.reqBody!=null && reqBody.size()>0){
					sb.append(", req-body="); //If body available, set tag req-body 
				} else {
					sb.append(", null-body="); //else null-body 
				}
				sb.append(this.reqHeader.length()).append(CRLF); //Define header offset
				//Set if connection is persistent
				if (server.useKeepAliveConnections()) {
					sb.append(HEAD_CONNECTION_KEEPALIVE);
				} else {
					sb.append(HEAD_CONNECTION_CLOSED);
					this.closeConnection();
				}
				sb.append(CRLF).append(this.reqHeader);
				bas.write(sb.toString().getBytes());
				if (this.reqBody!=null && reqBody.size()>0) {
					bas.write((Integer.toHexString(this.reqBody.size())+CRLF).getBytes());
					this.reqBody.writeTo(bas);
					bas.write(CRLF_b);
					bas.write(ENDCHUNK);
				}
				break;
			case RESPMOD:
				sb.append("Encapsulated: res-hdr=0");
				if (resBody!=null && resBody.size()>0){
					sb.append(", res-body="); //If body available, set tag req-body 
				} else {
					sb.append(", null-body="); //else null-body 
				}
				sb.append(this.resHeader.length()).append(CRLF); //If body available, set tag req-body
				//Set if connection is persistent
				if (server.useKeepAliveConnections()) {
					sb.append(HEAD_CONNECTION_KEEPALIVE);
				} else {
					sb.append(HEAD_CONNECTION_CLOSED);
					this.closeConnection();
				}
				sb.append(CRLF).append(this.resHeader);
				bas.write(sb.toString().getBytes());
				if (this.resBody!=null && resBody.size()>0) {
					bas.write((Integer.toHexString(this.resBody.size())+CRLF).getBytes());
					this.resBody.writeTo(bas);
					bas.write(CRLF_b);
					bas.write(ENDCHUNK);
				}
				break;
		}
		return 200;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Replies to ICAP client with an unmodified content
	 * Tries to use 204 (inside and outside preview) to optimize perfs
	 * @param bas Byte stream Stream handling request body (REQ or RESP)
	 * @return ICAP response code (200 OK message or 204 No Content)
	 * @throws Exception
	 */
	public int earlyResponse(ByteArrayOutputStream bas)  throws Exception{
		if (this.allow_204 || (this.preview != -1 && !this.bodyreaded ) ) {
			bas.write(server._204NOCONTENT);
			if (Log.finest()) Log.trace(Log.FINEST, "204 supported (Preview:"+this.preview +" or Allow 204:"+this.allow_204+" - send 204 No Content response");
			return 204;
		}

		// 204 not possible
		// retrieves body if not already done, then returns full content 
		this.getAllBody();
		if (Log.finest()) Log.trace(Log.FINEST, "204 not supported - send unchanged response");
		return fullResponse(bas);
	}
	//	<------------------------------------------------------------------------->
	

	//	<------------------------------------------------------------------------------------------>
	/**
	 * Read preview chunk(s!) and store it into reqBody / resBody 
	 * @return true if preview exists and has been readed, false otherwise
	 */
	public boolean readPreview(){
		previewreaded = true;
		if (this.i_res_body==0 && this.i_req_body==0) return false; //no body => abort

		if (this.preview==-1) return false;//we're not in preview mode => do nothing
		String readline = "";
		int chuncksize = 0;
		previewstarved = false;
		try {
			readline = this.dis.readLine().trim();

			//special case: empty body -  [CRLF][0; ieof][CRLF][CRLF]
			if (readline.equals("0; ieof")) {
				if (Log.finest()) Log.trace(Log.FINEST,"Reading starved during preview");
				previewstarved = true;
				this.dis.skip(4);
				previewreaded = true;
				return true;
			}
			/*if (this.preview==0) { // skip [CRLF][0][CRLF]
				this.dis.skip(2);
				readline = this.dis.readLine().trim();
				if (readline.endsWith("ieof")) { 
					if (Log.finest()) Log.trace(Log.FINEST,"Reading starved during preview");
					previewstarved = true;
				}
				this.dis.skip(2);
				previewreaded = true;
				return true;
			}
			 */
			chuncksize = Integer.parseInt(readline,16);//get chunk size
		} catch (NumberFormatException e){
			if (Log.fine()) Log.trace(Log.FINE,"ICAP preview error - invalid chunk size: ["+readline+"]", e);
			return false;
		} catch (Exception e){
			if (Log.fine()) Log.trace(Log.FINE,"ICAP preview error while parsing chunk size", e);
			return false;
		}

		//while (readChunk(chuncksize, true)>0){/*read preview chunk*/}
		//PATCH: thanks to Pawel Jasnos
		while ( (chuncksize = readChunk(chuncksize,true))>0){/*read all preview chunks until a 0 is returned*/}
		try{
			int leavingbytes = this.dis.available();
			if (leavingbytes>1) this.dis.skip(leavingbytes);//skip trailing CRLF
		} catch (Exception e){
			if (Log.fine()) Log.trace(Log.FINE,"ICAP preview error - error skipping trailing CRLF", e);
		}
		return true;
	}
	//	<------------------------------------------------------------------------------------------>



	//	<------------------------------------------------------------------------------------------>
	/**
	 * Read all HTTP body from iCAP request (either REQMOD POST/PUT body, or RESPMOD response body)<br>
	 * @return false if an error has been encountered<br>
	 * @throws Exception
	 */
	public boolean getAllBody() throws Exception {
		if (bodyreaded) return false;
		bodyreaded = true;
		if (this.i_res_body==0 && this.i_req_body==0) return false; //no body => abort
		if (this.preview>-1 && !previewreaded) readPreview();
		if (previewstarved) {
			if (this.preview == 0 ) return false;
			return true;
		}
		int chuncksize = 0;
		if (this.preview!=-1) {//we're still in preview mode => ask for the body
			if (Log.finest()) Log.trace(Log.FINEST,"ICAP Parser - 100 continue");
			this.bufferedOutputStream.write("ICAP/1.0 100 continue\r\n\r\n".getBytes());
			this.bufferedOutputStream.flush();
		}

		String readline = "";

		boolean readed = false;
		int private_counter=0;

		//Security loop, in case of trailing char (exotic implementation)
		//TODO: check if this is needed or not 
		while (!readed && private_counter<1){
			try {
				readline = this.dis.readLine().trim();
				chuncksize = Integer.parseInt(readline,16);//get chunk size
				readed = true;
			} catch (NumberFormatException e){
				if (Log.fine()) Log.trace(Log.FINE,"ICAP Parser error n° ["+private_counter+"]- invalid chunk size: ["+readline+"]", e);
				private_counter++;
			} catch (Exception e){
				private_counter++;
				//if (Log.fine()) Log.trace(Log.FINE,"ICAP Parser error while parsing chunk size", e);
			}
		}
		
		if (!readed && private_counter==1){
			if (Log.fine()) Log.trace(Log.FINE,"ICAP Parser error while parsing chunk size - null or empty chunk");
			throw (new IcapParserException("ICAP Parser error while parsing chunk size - null or empty chunk"));
			// comment above line and uncomment below line to correct ICAP request instead of returning error
			//return false;
		}
		while ( (chuncksize = readChunk(chuncksize,false))>0){
			/*read all chunks until a 0 is returned*/
		}
		if (this.dis.available()>=2)this.dis.skip(2);//skip trailing CRLF if present (depends on ICAP client implementation)
		return true;
	}
	//	<------------------------------------------------------------------------------------------>

	//	<------------------------------------------------------------------------------------------>    
	/**
	 * Read an iCAP chunk of given size. Chunk data is append to reqbody or resbody depending of the calling mode<br>
	 * @param chunksize The chunk size to read<br>
	 * @param previewreading Set if chunk reading is made during preview or not<br>
	 * @return the size of next available chunk<br>
	 */
	public synchronized int readChunk(int  chunksize,boolean previewreading){
		String readline="";
		try{
			if (chunksize==0) {
				return 0;//no more chunk => return 0
			}

			if (Log.finest()) Log.trace(Log.FINEST,"Reading chunck of ["+chunksize+"] datas for "+type.toString());
			byte[] content = new byte[chunksize];
			int nbread;

			while (chunksize!=0){ // Read TCP packets until chunk is fully readed
				if (tcptweak) this.socket.sendUrgentData(URGENTDATA); // tweak to speed up - only to use with some very specific ICAP clients
				nbread = this.dis.read(content,0,chunksize);
				switch (this.type){
				case RESPMOD: 
					this.resBody.write(content, 0, nbread);
					break;
				case REQMOD:
					this.reqBody.write(content, 0, nbread);
					break;
				default: break;
				}
				chunksize-=nbread;
			}

			//if (Log.finest()) Log.trace(Log.FINEST,"["+chunksize+"]---------------------");

			this.dis.skip(2);//skip [CRLF] after chunk
			readline = this.dis.readLine().trim(); //read next chunk size
			if (Log.finest()) Log.trace(Log.FINEST,"Next chunk size: ["+readline+"]");
			if (previewreading && readline.endsWith("ieof")){
				//no more chunks to read
				if (Log.finer()) Log.trace(Log.FINER,"All body readed during preview. disabling preview mode.");
				this.previewstarved = true;
				return 0;
			}
			return Integer.parseInt(readline,16);

		} catch (Exception e){
			if (Log.finer()) Log.trace(Log.FINER,"Error reading chunck . Forcing zero chunck");
			if (Log.finest()) Log.trace(Log.FINEST,"["+readline+"]", e);
			return 0;//==> error: stop reading body
			//TODO: check if it's not also better to clear already readed body ?

		}
	}
	//	<------------------------------------------------------------------------------------------>    

	//	<------------------------------------------------------------------------------------------>    
	/**
	 * Read zero preview chunk<br>
	 * (needed to stay in clean state when using 204 responses, as preview chunk may not have been 
	 * sent by client)<br>
	 */
	protected void clearZeroPreviewChunk(){
		try{
			//(MS Windows bug ?)
			if (tcptweak) this.socket.sendUrgentData(URGENTDATA); // tweak to speed up ACK
			byte[] content = new byte[5];
			this.dis.read(content); // skip [CRLF][0][CRLF]
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	//	<------------------------------------------------------------------------------------------>


	//	<------------------------------------------------------------------------------------------>
	/**
	 * Generate ICAP response using chunks
	 * @param header ICAP response + HTTP (request or response) header  
	 * @param body	body of the HTTP response to provide
	 * @throws Exception
	 */
	public synchronized void writeChunks(String header,  ByteArrayOutputStream body) throws Exception {
		this.bufferedOutputStream.write(header.getBytes());
		int chunck = header.length();
		int pos = 0;
		byte[] resp = body.toByteArray();
		body.flush();
		if (Log.finer()) Log.trace(Log.FINER, "THREAD ["+id+"] RESPONSE:\n---------------------\n"+header+"\r\n[....CHUNKS...]\r\n---------------------");

		if (chunck<MTU){
			chunck = resp.length>MTU-chunck?MTU-chunck:resp.length;
			this.bufferedOutputStream.write((Integer.toHexString(chunck)+CRLF).getBytes());
			this.bufferedOutputStream.write(resp, 0, chunck);
			pos = chunck;
		}

		this.bufferedOutputStream.flush();
		while (pos<resp.length){
			chunck = resp.length - pos>MTU?MTU:resp.length - pos;
			this.bufferedOutputStream.write((CRLF+Integer.toHexString(chunck)+CRLF).getBytes());
			this.bufferedOutputStream.write(resp, pos, chunck);
			pos +=  chunck;
			this.bufferedOutputStream.flush();
			if (Log.finest()) Log.trace(Log.FINEST, "["+chunck+"]");
		}
	}
	//	<------------------------------------------------------------------------------------------>



	//	<------------------------------------------------------------------------------------------>    
	/**
	 * Initialize all iCAP thread parameters<br>
	 * Called between each iteration for iCAP keep-alive connections<br>
	 */
	private void reset(){
		this.firstline="";
		this.req_url="";
		this.req_url_path="";
		this.req_url_searchpart="";
		this.httpmethod="";
		this.host=null;
		this.reqHeader.setLength(0);//avoid to create new object 
		//if (this.reqBody!=null) {	
		this.reqBody.reset();// = null;
		/*} else {
			this.reqBody = new ByteArrayOutputStream();
		}*/
		this.previewreaded = false;
		this.bodyreaded = false;
		this.previewstarved = false;
		this.icapHeaders.clear();
		this.httpReqHeaders.clear();
		this.httpRespHeaders.clear();
		this.contentLength = -1;
		this.rescode=502;
		this.resHeader.setLength(0);//avoid to create new object 
		this.resBody.reset();// avoid to create new object = null;
		this.icapHeader.setLength(0);// = new StringBuilder(); //avoid to create new object 

		this.i_encapsulated="";
		this.preview = -1;
		this.i_service="";
		this.i_req_hdr=0;
		this.i_req_body = 0;
		this.i_res_hdr = 0;
		this.i_res_body = 0;
		this.i_null_body = 0;
		this.allow_204 = false;
		this.allow_206 = false;
	} 
	//	<------------------------------------------------------------------------------------------>

	//	<------------------------------------------------------------------------------------------>
	/**
	 * Main ICAP parsing method<br>
	 * Sends request to successively ICAP parser, HTTP request parser and HTTP Response parser<br>
	 * @param bufferedreader The stream to read ICAP request from
	 * @return	Parsed Request type (REQMOD/RESPMOD/OPTIONS/INVALID)
	 * @throws Exception
	 */
	public synchronized TYPE parse(DataInputStream bufferedreader) throws Exception {
		reset();
		if (Log.finest()) Log.trace(Log.FINER, getServerName()+"=>THREAD ["+id+"] REQUEST\r\n---------------------");

		this.type = this.parseIcapHeader(bufferedreader);
		try {
			switch (this.type){
				case REQMOD: 
					parseHttpRequest(bufferedreader,this.reqHeader);
					if (Log.finest()) Log.trace(Log.FINEST, reqHeader.toString()+"--------- REQMOD ------------");
					break;
				case RESPMOD:
					parseHttpRequest(bufferedreader,this.reqHeader);
					parseHttpResponse(bufferedreader, this.resHeader);
					if (Log.finest()) Log.trace(Log.FINEST, reqHeader.toString()+"--------- RESPMOD ------------\r\n"+resHeader.toString());
					break;
				case INVALID:
					if (Log.finest()) {
						Log.trace(Log.FINEST, reqHeader.toString()+"---------- INVALID -----------");
						byte[] debugbuffer = new byte[bufferedreader.available()];
						bufferedreader.readFully(debugbuffer);
						if (Log.finest()) Log.trace(Log.ALL, bufferedreader.available()+":["+new String(debugbuffer)+"]");
					}
					break;
				case EMPTY:
					if (Log.finest()) Log.trace(Log.FINEST, reqHeader.toString()+"---------- EMPTY -----------");
					break;
				case OPTIONS:
					if (Log.finest()) Log.trace(Log.FINEST, reqHeader.toString()+"---------- OPTIONS -----------");
					break;
			}
		} catch (Exception e) {
			return TYPE.INVALID;
		}
		return this.type;
	}
	//	<------------------------------------------------------------------------------------------>


	//	<------------------------------------------------------------------------------------------>
	/**
	 * Parse an HTTP request provided in an iCAP REQMODE<br>
	 * @param bufferedreader The input stream from ICAP client
	 * @param requestHeader	 the request header in which to store parsed request
	 * @throws Exception		
	 */
	public void parseHttpRequest(DataInputStream bufferedreader, StringBuilder requestHeader) throws Exception {
		int traildot, i=0;
		//		---
		int headersize;// = this.i_req_body>0?this.i_req_body:this.i_null_body;

		//read computes REQ header 
		if (this.type==TYPE.REQMOD) {
			//in REQMOD, retrieve header size using req_body (for POST/PUT) or null_body otherwise  
			headersize = this.i_req_body>0?this.i_req_body:this.i_null_body;
		} else {
			//RESPMOD => res_header always exist, take it
			headersize = this.i_res_hdr;
		}

		//create byte array to stock header and read it
		byte[] header = new byte[headersize];
		int toread = headersize;
		while (toread>0){
			toread-=bufferedreader.read(header, headersize-toread, toread);
		}

		//append req header as string format
		requestHeader.append(new String(header));

		//generate table containing headers values 
		String[] heads = requestHeader.toString().split(CRLF);

		//		---
		String lowercaseheader;
		for (String header_line:heads){        // HTTP Header Parsing
			if (header_line.length()==0) break; // Empty line => header end =>  stop parsing
			lowercaseheader = header_line.toLowerCase();
			if (i==0) {//first line => extract request method, url, protocol, ...
				this.firstline = header_line;
				int pos = this.firstline.indexOf(' ');
				this.httpmethod = this.firstline.substring(0,pos).trim().toUpperCase();//HTTP method: GET/POST/HEAD/CONNECT/DELETE/OPTIONS/...
				if ( (pos=this.firstline.indexOf('?'))>0 ){
					this.req_url_path = this.firstline.substring(this.httpmethod.length()+1,pos);
					this.req_url_searchpart = this.firstline.substring(pos+1,this.firstline.lastIndexOf(" HTTP/1."));
					this.req_url = this.req_url_path+"?"+this.req_url_searchpart;
				} else {
					this.req_url = this.firstline.substring(this.httpmethod.length()+1,this.firstline.lastIndexOf(" HTTP/1."));
					this.req_url_path = this.req_url;
				}
				i++;
			} else if (lowercaseheader.startsWith("host: ")){ // correct url if needed
				//syntax= Host: www.host.com[:port]
				if (header_line.indexOf(":",7)==-1) {
					//	if no port is specified, extract line
					this.host = header_line.substring(6);
				} else {
					//	orelse extract up to ":" char
					this.host = header_line.substring(6, header_line.indexOf(":",7));
				}
				if (!this.req_url.startsWith("http://") && !this.req_url.startsWith ("ftp://")) {
					if (this.req_url.startsWith("/")){
						this.req_url = "http://"+this.host+this.req_url;
						this.req_url_path = "http://"+host+req_url_path;
					} else {
						this.req_url = "http://"+this.host+"/"+this.req_url;
						this.req_url_path = "http://"+this.host+this.req_url_path;
					}
				}
			} else if (header_line.toLowerCase().startsWith("content-length:")){
				this.contentLength = Long.parseLong(header_line.substring(16).trim());
				this.httpReqHeaders.put("content-length", contentLength+"");
			} else { 
				// general header => put it in hash table (no lazy parsing for the moment)
				traildot = header_line.indexOf(":");
				this.httpReqHeaders.put(lowercaseheader.substring(0,traildot), header_line.substring(traildot+2));
			}
		}//End for HTTP Request header parsing
	}
	//	<------------------------------------------------------------------------->    

	//	<------------------------------------------------------------------------->
	/**
	 * Update HTTP header response with given content length 
	 * @param newContentLength the new Content-Length value to set for the HTTP response 
	 */
	public void updateContentLength(long newContentLength){
		switch (this.type){
		case REQMOD:
			if (this.httpReqHeaders.containsKey("content-length")){
				int pos1 = this.reqHeader.toString().toLowerCase().indexOf("content-length")+CONTENTLENGTHSIZE;
				int pos2 = this.reqHeader.indexOf("\r\n", pos1);
				this.reqHeader = this.reqHeader.replace(pos1,pos2, newContentLength+"");
			} else {
				if (newContentLength !=0) this.reqHeader.insert(this.reqHeader.length()-2, "Content-Length: " + newContentLength+ "\r\n");
			}
			break;
		case RESPMOD:
			if (this.httpRespHeaders.containsKey("content-length")){
				int pos1 = this.resHeader.toString().toLowerCase().indexOf("content-length")+CONTENTLENGTHSIZE;
				int pos2 = this.resHeader.indexOf("\r\n", pos1);
				this.resHeader = this.resHeader.replace(pos1,pos2, newContentLength+"");
			} else {
				this.resHeader.insert(this.resHeader.length()-2, "Content-Length: " + newContentLength+ "\r\n");
				//this.resHeader.delete(this.resHeader.length()-2,this.resHeader.length()).append("Content-Length: ").append(newContentLength).append("\r\n\r\n");
			}
			break;
		default:
		}
		this.contentLength = newContentLength;
	}
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	/**
	 * @return HTTP content length
	 */
	public long getContentLength(){
		return this.contentLength;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Update HTTP header response with given content type 
	 * @param newContentType the new Content-Type value to set for the HTTP response 
	 */
	public void updateContentType(String newContentType){
		switch (this.type){
		case REQMOD:
			if (this.httpReqHeaders.containsKey("content-type")){
				int pos1 = this.reqHeader.toString().toLowerCase().indexOf("content-type")+CONTENTTYPESIZE;
				int pos2 = this.reqHeader.indexOf("\r\n", pos1);
				this.reqHeader = this.reqHeader.replace(pos1,pos2, newContentType+"");
			} else {
				this.reqHeader.insert(this.reqHeader.length()-2, "Content-Type: " + newContentType+ "\r\n");
			}
			break;
		case RESPMOD:
			if (this.httpRespHeaders.containsKey("content-type")){
				int pos1 = this.resHeader.toString().toLowerCase().indexOf("content-type")+CONTENTTYPESIZE;
				int pos2 = this.resHeader.indexOf("\r\n", pos1);
				this.resHeader = this.resHeader.replace(pos1,pos2, newContentType+"");
			} else {
				this.resHeader.insert(this.resHeader.length()-2, "Content-Type: " + newContentType+ "\r\n");
				//this.resHeader.delete(this.resHeader.length()-2,this.resHeader.length()).append("Content-Type: ").append(newContentType).append("\r\n\r\n");
			}
			break;
		default:
		}
	}
	//	<------------------------------------------------------------------------->


	//	<------------------------------------------------------------------------->
	/**
	 * Parse an HTTP response from given buffer<br>
	 * @param bufferedreader the stream from which to read response
	 * @param res_header StringBuffer into which response header will be filled
	 * @throws Exception
	 */
	public void parseHttpResponse(DataInputStream bufferedreader, StringBuilder res_header) throws Exception {
		int i=0;
		int traildot = 0;

		//Read response header
		int respsize = (this.i_res_body>0?this.i_res_body:this.i_null_body) - this.i_res_hdr;
		byte[] reshead = new byte[respsize];
		int toread = respsize;

		while (toread>0){
			toread-=bufferedreader.read(reshead, respsize-toread, toread);
		}

		res_header.append(new String(reshead));
		String[] heads = res_header.toString().split(CRLF);

		//Parse response headers and store it in hashtable (headers names are stored in lowercase)
		for (String header_line:heads){
			if (header_line.equals("")){
				//response header readed
				break;
			}
			// HTTP header Parsing
			if (i==0) {
				//first line => HTTP response code
				try{
					this.rescode = Integer.parseInt(header_line.substring(9,12));//HTTP/1.x [yyy] Text
				} catch (Exception e){
					this.rescode = 502;
				}
				i++;
			} else if (header_line.toLowerCase().startsWith("content-length:")){
				this.contentLength = Long.parseLong(header_line.substring(16).trim());
				this.httpRespHeaders.put("content-length", contentLength+"");
			} else {
				traildot = header_line.indexOf(":");
				if (traildot!=-1) this.httpRespHeaders.put(header_line.substring(0,traildot).toLowerCase(), header_line.substring(traildot+2));
			}
		}
	}
	//	<------------------------------------------------------------------------------------------>    

	//	<------------------------------------------------------------------------------------------>
	/**
	 * @return Returns the current client socket.
	 */
	public Socket getSocket() {
		return this.socket;
	}
	/**
	 * @param newsocket The client socket to use.
	 */
	public void setSocket(Socket newsocket) {
		this.socket = newsocket;
	}

	/**
	 * @return true if TCP optimization is activated
	 */
	public static boolean isTcpTweakEnabled() {
		return tcptweak;
	}

	/**
	 * Activate TCP optimization<br>
	 */
	public static void enableTcpTweak() {
		if (!IcapServer.turnStdOff) System.out.println("TCP optimization enabled");
		else Log.error(Log.CONFIG, "TCP optimization enabled");
		tcptweak = true;
	}
	//	<------------------------------------------------------------------------------------------>    
}
