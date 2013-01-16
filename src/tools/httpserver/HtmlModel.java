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
 * Created	:	28/08/02
 *----------------------------------------------------------------------------------
 */
package tools.httpserver;
///////////////////////////////////
// 	Import
import java.io.*;
import java.util.logging.Level;
import tools.logger.Log;
///////////////////////////////////

/**
 * Define HTML model, which must be extended to create an HTML page usable by server.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class HtmlModel {

  protected String nomFicHtml="";
  
  protected ByteArrayOutputStream pageByteHtml=null;
  
  protected FileInputStream ficHtml=null;
  
  protected byte content[]=null;

  protected boolean forcedownload = false;
  
  protected boolean isOver = false;
  
  String _downloadname = "";
  
  /** server realm*/
/*public static String serverRealm = "kmserver";
  public static int realmcounter=0;
  static {
	  try{
	  serverRealm = HttpServer.bounded_ip==null?InetAddress.getLocalHost().getHostAddress():HttpServer.bounded_ip;
	  } catch (Exception e){}
  }
  */
  
//<------------------------------------------------------------------------------------------>
/**
 * Generate an HTTP date header
 * @return http date header in String format
 * @see tools.httpserver.HttpVars
 */
public final static String generateDateHeader() {
    return "Date: " + HttpVars.getHTTPDate(System.currentTimeMillis());
}
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
/**
 * Set page content-type to application/octet-stream to force content download on browser side 
 * @param downloadname filename that will be proposed to user when downloading file
 */
public void forceDownload(String downloadname){
    forcedownload = true;
    this._downloadname = downloadname;
}
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
/**
 * Retrieve file content type using file extension
 * @param   extension the extension file (.txt, .html, .asx, ...) 
 * @return  the Content type (Mime string)
 * @see MimeType
 */
public String getContentType(String extension) {
    if (forcedownload) return "application/octet-stream";
    String ext = MimeType.getContentTypeFor(extension); 
    if (ext.startsWith("text")) ext+= "; charset=UTF-8";
	return ext; 
}
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
/**
 * Add authentification header (401 message) to sended file
 * @param   extension  File extension (html, txt, ...)
 */
  public void authHeader(String extension) {
	try {
		if (isOver) return;
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 401 Access forbidden\r\n");
        header.append(generateDateHeader()+"\r\n")
            .append("Server: K1Server\r\n")
            .append("WWW-Authenticate: Basic").append("\r\n")
            .append("Connection: close\r\n")
            .append("Content-Length: ").append(pageByteHtml.size()).append("\r\n");
   		header.append("Content-Type: "+getContentType(extension)).append("\r\n");
    	header.append("Expires: Now\r\n");
        header.append("\r\n");

        content=new byte[pageByteHtml.size()];
        content=pageByteHtml.toByteArray();
		
        pageByteHtml=new ByteArrayOutputStream(pageByteHtml.size()+header.length());
        pageByteHtml.write(header.toString().getBytes());
        pageByteHtml.write(content);
		
        if (ficHtml!=null) ficHtml.close();
        isOver = true;
    }catch(Exception e){
    	if (Log.finest()) Log.trace(Level.FINEST, e); 
    }
  }
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
  /**
   * Add 302 redirection header to sended file
   * @param location url where to redirec user
   * @param extension  File extension (html, txt, ...)
   * @param cookie Cookie value to set in redirection message (use null if none) 
   */
    public void redirectHeader(String location, String extension, String cookie) {
  	try {
  		if (isOver) return;
          StringBuilder header = new StringBuilder();
          header.append("HTTP/1.1 302 Found\r\n");
          header.append(generateDateHeader()+"\r\n")
              .append("Server: K1Server\r\n")
              //.append("WWW-Authenticate: Basic realm=\"")
              //.append(serverRealm).append(realmcounter).append("\"\r\n")
              .append("Cache-Control: no-cache\r\n")
              .append("Connection: close\r\n")
              .append("Location: "+location+"\r\n");
          if (cookie!=null) {
        	  header.append("Set-Cookie: ").append(cookie).append("\r\n");
          }
          header.append("Content-Length: ").append(pageByteHtml.size()).append("\r\n");
     	  header.append("Content-Type: "+getContentType(extension)).append("\r\n");
      	  header.append("Expires: Now\r\n");
          header.append("\r\n");

          content=new byte[pageByteHtml.size()];
          content=pageByteHtml.toByteArray();
  		
          pageByteHtml=new ByteArrayOutputStream(pageByteHtml.size()+header.length());
          pageByteHtml.write(header.toString().getBytes());
          pageByteHtml.write(content);
  		
          if (ficHtml!=null) ficHtml.close();
          isOver = true;
      }catch(Exception e){
      	if (Log.finest()) Log.trace(Level.FINEST, e); 
      }
    }
//  <------------------------------------------------------------------------------------------>
  
//<------------------------------------------------------------------------------------------>
/**
 * Add HTTP header to bytearray to send
 * @param extension  html file extension (.txt, .html, .asx, ...)
 */
  public void addHeader(String extension) {
	if (isOver) return;
    try {
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 200 OK\r\n")
        	.append(generateDateHeader()+"\r\n")
        	.append("Server: K1Server\r\n")
			.append("Accept-Ranges: bytes\r\n")
			.append("Content-Length: "+pageByteHtml.size()+"\r\n")
			.append("Connection: close\r\n")
			.append("Content-Type: "+getContentType(extension)+"\r\n")
			.append("Cache-Control: no-cache\r\n")
			.append("Expires: Now\r\n")
			.append("\r\n")
        	;
        content=new byte[pageByteHtml.size()];
        content=pageByteHtml.toByteArray();
		
        pageByteHtml=new ByteArrayOutputStream(pageByteHtml.size()+header.length());
        pageByteHtml.write(header.toString().getBytes());
        pageByteHtml.write(content);
		
        if (ficHtml!=null) ficHtml.close();
        isOver = true;
    }catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e); }
  }
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
  /**
   * Add HTTP header to bytearray to send
   */
    public void addDownloadHeader() {
      if (isOver) return;
      try {
          StringBuilder header = new StringBuilder();
          header.append("HTTP/1.1 200 OK\r\n")
            .append(generateDateHeader()+"\r\n")
            .append("Server: K1Server\r\n")
            .append("Accept-Ranges: bytes\r\n")
            .append("Content-Length: "+pageByteHtml.size()+"\r\n")
            .append("Connection: close\r\n")
            .append("Content-Type: application/octet-stream\r\n")
            .append("Content-Disposition: attachment; filename=").append(_downloadname).append("\r\n")
            .append("Cache-Control: no-cache\r\n")
            .append("Expires: Now\r\n")
            .append("\r\n")
            ;
          content=new byte[pageByteHtml.size()];
          content=pageByteHtml.toByteArray();
        
          pageByteHtml=new ByteArrayOutputStream(pageByteHtml.size()+header.length());
          pageByteHtml.write(header.toString().getBytes());
          pageByteHtml.write(content);
        
          if (ficHtml!=null) ficHtml.close();
          isOver = true;
      }catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e); }
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
    * Send a redirect to client
    * @param location The location (URI) where to redirect client: http(s)://site(:port)(/path)
    * HtmlModel page : the page to update
    */
    public void redirect(String location) {
    	redirect(location, this);
    }
//  <------------------------------------------------------------------------------------------>
    	
//  <------------------------------------------------------------------------------------------>
    /**
     * Send a redirect to client
     * @param location The location (URI) where to redirect client: http(s)://site(:port)(/path)
     * @param page the html page to update
     */
      public void redirect(String location, HtmlModel page) {
    	if (isOver) return;
        try {
            StringBuilder header = new StringBuilder();
            String redir = "<html><BODY onLoad=\"top.location.href='"+location+"'\"></BODY></html>";
            header.delete(0, header.length());
            header.append("HTTP/1.1 200 OK\r\nContent-Type: text/html")
              .append("\r\nContent-Length: ").append(redir.length())
              .append("\r\nCache-Control: no-cache")
              .append("\r\nConnection: close\r\n\r\n")
              .append(redir)
              ;
            page.pageByteHtml=new ByteArrayOutputStream(header.length());
            page.pageByteHtml.write(header.toString().getBytes());
            if (page.ficHtml!=null) page.ficHtml.close();
            isOver = true;
        }catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e); }
      }
//<------------------------------------------------------------------------------------------>
  
  
//<------------------------------------------------------------------------------------------>
  /**
   * Add HTTP header to bytearray to send
   * @param extension  html file extension (.txt, .html, .asx, ...)
   */
    public void addCacheableHeader(String extension) {
      try {
    	  if (isOver) return;
          StringBuilder header = new StringBuilder();
          header.append("HTTP/1.1 200 OK\r\n")
            .append(generateDateHeader()+"\r\n")
            .append("Server: K1Server\r\n")
            .append("Accept-Ranges: bytes\r\n")
            .append("Content-Length: "+pageByteHtml.size()+"\r\n")
            .append("Connection: close\r\n")
            .append("Content-Type: "+getContentType(extension)+"\r\n")
            .append("Cache-Control: max-age=3600\r\n")
            .append("\r\n")
            ;
          content=new byte[pageByteHtml.size()];
          content=pageByteHtml.toByteArray();
        
          pageByteHtml=new ByteArrayOutputStream(pageByteHtml.size()+header.length());
          pageByteHtml.write(header.toString().getBytes());
          pageByteHtml.write(content);
      
          if (ficHtml!=null) ficHtml.close();
          isOver = true;
      }catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e);}
    }
//  <------------------------------------------------------------------------------------------>


//<------------------------------------------------------------------------------------------>
/**@return HTML page under byte format*/
  public byte[] getPage() { return (pageByteHtml.toByteArray()); }
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
/**@return file name (if existing)*/
 public String getNomFic() { return nomFicHtml; }
//<------------------------------------------------------------------------------------------>

}