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
 * Created	:	7/07/02
 *----------------------------------------------------------------------------------
 */
package tools.httpserver;
///////////////////////////////////
//Import
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import tools.logger.Log;
///////////////////////////////////



/**
 * This class integrate methods to analyze HTTP message between a client (browser) and the http(s) server.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class UserTransaction implements HttpConstants {

	/**Constants used to identify requests*/
	private final static boolean debug = false;
	
	/**Current request Identification*/
	private METHOD typeReq=METHOD.GET;

	/**Opened stream between client (browser or applet)and server*/
	private Socket 				_clientSocket;
	private BufferedInputStream inputStream;
	private OutputStream 		outputStream;

	/**Client requested object (GET)*/
	private String objectRequested;


	private String httpHeader;
	private String contentType;
	UserSession _connection;


	/** Binary stream using to store request coming from client */
	ByteArrayOutputStream pageContent;

	
	/** User profile associated to this dialog*/
	User user = null;

//	<------------------------------------------------------------------------------------------>
	/** Create an dialog instance between browser and server*/
	public UserTransaction() {
		//nothing to do here, but init must be called after
	}
//	<------------------------------------------------------------------------------------------>

	/**
	 * @return Request Content type
	 */
	public String getContentType(){
		return this.contentType;
	}
//	<------------------------------------------------------------------------------------------>
	/** 
	 * Initialize a dialog using given connection and socket
	 * @param connection Connection used to manage session for end-user connection
	 * @param clientSocket End-user socket (no multiplexing)
	 */
	public void init(UserSession connection, Socket clientSocket) {
		try {
			this._connection = connection;
			this._clientSocket = clientSocket;
			inputStream = new BufferedInputStream(clientSocket.getInputStream());
			outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
		}catch(Exception e){
			if (Log.finest()) Log.trace(Level.FINEST, e);
		}
	}//End initialise
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>

	/**
	 * Read HTTP client request.
	 * DataInputStream is used to switch between char reader for HTTP header
	 * and byte header for body.
	 * @return HTTP request header
	 * TODO: implement a new InputStream reader to avoid readline deprecated method
	 */
	@SuppressWarnings("deprecation")
	public synchronized String readStream() {
		int contentLength=-1;// contentLength value
		pageContent = new ByteArrayOutputStream();

		try {
			StringBuilder sb = new StringBuilder();
			String s;
			DataInputStream dis = new DataInputStream(inputStream);
			while ((s=dis.readLine()) != null){
				if(s.equals("")) break;
				sb.append(s).append("\r\n");
				if (s.toLowerCase().indexOf("content-length")!=-1){
					contentLength= Integer.parseInt(s.substring(s.indexOf(" ")).trim());
				}
			}
			
			//Read content
			if (contentLength>0) {
				byte[] content = new byte[contentLength];
				int nbread = -1, toread = contentLength;
				while ( toread>0 && (nbread = dis.read(content) )!=-1){
					pageContent.write(content,0,nbread);
					toread -= nbread;
				}
			}//End reading content

			pageContent.close();

			return(sb.toString());
			
		}catch( InterruptedIOException iioe ) {
			if (debug) iioe.printStackTrace();
			if (Log.finest()) Log.trace(Level.FINER,"DP.readStream - Socket closed after timeout");
		}catch(Exception ioe ) {
			if (debug) ioe.printStackTrace();
			if (Log.finest()) Log.trace(Level.FINER,"DP.readStream - Socket closed by client.");
		}//End try&catch
		return null;
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Read stream coming from client.
	 * @return "REFUSED" if request is illegal, "POST" if the message was a post and requested URL if message was a valid GET 
	 */
	public String read() {
		String entete = readStream();
		if (entete==null || entete.trim().equals("")) return "";
		if (entete.equals("REFUSED")) return "REFUSED";
		objectRequested="";
		//Lecture de l'ent�te
		if (debug) System.err.println("---------------\r\n"+entete+"----------------");
		user=null;
		_connection.setAuthenticationStatus(false);
		String[] lines = entete.split("\r\n");
		for (String ligne_entete:lines){
			if (ligne_entete.length()==0) httpHeader+="\r\n";
			else httpHeader += (ligne_entete + "\r\n");
			if ( ligne_entete.toLowerCase().startsWith("get ") ) {
				objectRequested = ligne_entete.substring(4 ,ligne_entete.indexOf(" ", 5 )).trim();
				typeReq=METHOD.GET;
				objectRequested = checkForRoot(objectRequested.trim());
			} else if (ligne_entete.toLowerCase().startsWith("post ")) {
				objectRequested = ligne_entete.substring(5 ,ligne_entete.indexOf(" ", 5 )).trim();
				objectRequested = checkForRoot(objectRequested.trim());
				typeReq=METHOD.POST;
				if (objectRequested.endsWith("index.html")){
					user = identifyUser();
				}
			} else if (ligne_entete.toLowerCase().startsWith("authorization: ")) {
				//no more used => session cookie set instead
				//String receivedcode = ligne_entete.substring(ligne_entete.lastIndexOf(" "), ligne_entete.length()).trim();
				//user = HttpServer.getUserForB64Pwd(receivedcode);
				if (user == null ){
					if (HttpServer.debug>0) Log.trace(Level.FINER,"Authorization: wrong authorization header provided.");
				}
			}  else if (ligne_entete.toLowerCase().startsWith("cookie: ")) {
				//User authentication using cookie
				String receivedcode = ligne_entete.substring("cookie: ".length(), ligne_entete.length()).trim();
				int pos1 = receivedcode.indexOf(UserSession.cookiename);
				if (pos1==-1) continue;
				pos1 = receivedcode.indexOf("=",pos1)+1;
				if (pos1<1) continue;
				int pos2 = receivedcode.indexOf(",", pos1);
				if (pos2==-1) pos2 = receivedcode.indexOf(";", pos1);
				if (pos2==-1) pos2=receivedcode.length();
				if (pos2<pos1) continue;
				receivedcode = receivedcode.substring(pos1, pos2).trim();
				if (debug) System.err.println("Received cookie: ["+receivedcode+"]");
				User usercookie = HttpServer.extractUserFromSessionCookie(receivedcode);
				if (usercookie !=null){
					if (debug) System.err.println("Authentication:"+usercookie.getLogin());
					user = usercookie;
					_connection.setAuthenticationStatus(true);
				} else if (HttpServer.debug>0) {
					Log.trace(Level.FINER,"Bad cookie received.");
				}
			}  else if (ligne_entete.toLowerCase().startsWith("content-type: ")) {
				contentType = ligne_entete.substring("content-type: ".length()).trim();
			}
		}//End while
		return objectRequested;
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Try to identify user by looking on request body
	 * @return founded user, or null if no match
	 */
	public User identifyUser(){
		try{
			String content = this.getPageContent().toString();
			if (debug) System.err.println("identification params:"+content);
			String[] p1 = content.split("&");
			if (p1.length!=2) return null;
			String login=null, pwd=null;
			for (String s:p1){
				String[] p2 = s.split("=");
				if (p2.length!=2) return null;
				if (p2[0].equalsIgnoreCase("user")) login = TextTools.hexToUTF8(p2[1]).replace("+", " ");
				if (p2[0].equalsIgnoreCase("password")) pwd = TextTools.hexToUTF8(p2[1]).replace("+", " ");
			}
			if ( login== null || pwd ==null || login.length()==0 || pwd.length() ==0) return null;
			if (debug) System.err.println("identification :"+login+":"+pwd);
			return HttpServer.getUserFor(login, pwd);
		} catch (Exception e){
			if (debug) e.printStackTrace();
		}
		return null;
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Check if a requested object (GET) must be redirected to root page (index.html)
	 * @param   uri the requested uri to check 
	 * @return  the corrected uri, if needed   
	 */
	public String checkForRoot(String uri) {
		if (uri.indexOf("..")>-1) return "ILLEGAL";
		if (uri.equals("") || uri.equals("/") || uri.equals("index.htm")){
			uri = "/index.html";
		}
		if (!uri.startsWith("/")) uri="/"+uri;
		return uri;
	}
//	<------------------------------------------------------------------------------------------>


//	<------------------------------------------------------------------------------------------>
	/** 
	 * write output stream 
	 * @param pageHtml write given html page content into user socket 
	 */
	public void write( byte[] pageHtml ) {
		write( pageHtml, pageHtml.length );
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/** 
	 * write output stream 
	 * @param pageHtml
	 * @param size
	 */
	private void write( byte[] pageHtml, int size ) {
		try {
			outputStream.write( pageHtml, 0, size );
			outputStream.flush();
		} catch ( IOException ioe ) { 
			if (Log.finer()) Log.trace(Level.FINER, ioe);
			close();
		}
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/** close the dialog between client and server*/
	public void close(){
		this._connection.setAuthenticationStatus(false);
		try{
			outputStream.flush();
			outputStream.close();
			inputStream.close();
			_clientSocket.close();
		}catch(Exception e){
			//socket interruption => nothing to do anymore
		}
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/** @return true if dialog between client and browser is closed, false otherwise.*/
	public boolean isClosed(){
		if (_clientSocket==null) return true;
		return false;
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * @return Client socket associated to this dialog
	 */
	public Socket getClientSocket() { return _clientSocket; }
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * @return client socket output stream
	 */
	public OutputStream getOutputStream() { return outputStream; }
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * @return client socket input stream
	 */
	public BufferedInputStream getInputStream() { return inputStream; }
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * @return browser request header
	 */
	public String getHttpHeader() { return httpHeader; }
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Return client request type 
	 * @return  0=>GET; 1=>POST; 2=>other    
	 */
	public METHOD getTypeReq() { return typeReq; }
//	<------------------------------------------------------------------------------------------>

	/**
	 * @return the pageContent
	 */
	public ByteArrayOutputStream getPageContent() {
		return pageContent;
	}

	protected User getUser() {
		return user;
	}
	protected void resetUser() {
		user = null;
	}





}





