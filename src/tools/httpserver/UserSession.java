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
 * Created	:	2/09/02
 *----------------------------------------------------------------------------------
 */
package tools.httpserver;
///////////////////////////////////
// 	Import
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import tools.httpserver.custom.RequestsManager;
import tools.logger.Log;
///////////////////////////////////


//////////////////////////////////////////////////////////////////////
/**
 * Thread allowing to manage a connection between a user (web browser) and the http(s) server.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class UserSession extends Thread implements HttpConstants{

	private final static boolean debug = false;
	
   	/**Thread pool used to manage sessions*/
    private static PoolThreads pool;

	/**session thread status*/
    private boolean isAlive=true;
    /**user socket*/
    private Socket sock=null;

    /**Transaction between user and server*/
    private UserTransaction transaction=null;
	
	private boolean _authenticated = false;
	final static String cookiename = "sessionid";
	private static int joke=0;
	
//<------------------------------------------------------------------------------------------>
    /**
     * create a new session between a browser and HTTP server
     */
    public UserSession(){
    	super("Admin server - connection");
	    transaction=new UserTransaction();
    }
//<------------------------------------------------------------------------------------------>


/**
 * Disable the connection => stop dialog between browser and us
 */
public void disable(){
    try{
        closeTransaction();
        sock.close();
        this.isAlive = false;
        this.interrupt();
    } catch (Exception e){
    	//on exception nothing to do
    }
}

//<------------------------------------------------------------------------------------------>
/** 
* Start connection thread. The thread runs until explicit stop. It is in "task feeded" 
* (browser requests) by PoolThread.
* When a task arrives, poolThread use assignTask method to assign a new Socket to thread sock 
* => sock is no more null => treatment.
* When treatment is over, the thread calls restoreInPool method to see if there is an existing 
* waiting task to take. If not, sock is put to null and the thread go to sleep.
*/
@SuppressWarnings({ "static-access" })
public void run() {
	while (isAlive){
		while (sock!=null){
			_authenticated = false;
			proceedTransaction();
			sock = pool.restoreInPool(this);
	    }//while sock!=null
		try {
			this.sleep(1000);
		} catch (InterruptedException e){
			//Thread interrupted, new packet to treat
		}//End try&catch
	}//End while isAlive
}//End run
//<------------------------------------------------------------------------------------------>

//<------------------------------------------------------------------------------------------>
/**
 * Used to change task context (new connection, so new socket)
 * @param pendingSocket User connection request that is pending
 */
public void assignTask(Socket pendingSocket) {
  sock=pendingSocket;
  try {
	  //sock.setSoLinger(true,0);
	  //sock.setSoTimeout(60000);
  } catch (Exception e) {
  	if (Log.finest()) Log.trace(Level.FINEST, e);
  }
}
//<------------------------------------------------------------------------------------------>



//<------------------------------------------------------------------------------------------>
/** Communication between browser and server */
public void proceedTransaction() {
	initTransaction();
	int i=0;
	try {
		boolean running = true;
		while (sock!=null && !transaction.isClosed() && running){
			//If query is empty, return (i.e refuse connection)
            String uri = transaction.read();

			if (uri.equals(""))  {
				return;
			}
			//If query is invalid (url with <..> in the path,... refuse connection.
			if (uri.equals("ILLEGAL"))  {
	            Log.admin(Level.INFO,"HTTP/401\t"+sock.getInetAddress().getHostAddress()+"\t"+uri);
				if (joke>2) {
				   	transaction.close();
					return;
				}
				if (joke<2) transaction.write((new HtmlSimple("Access Denied","\tInvalid path")).getPage());
				if (joke==2) transaction.write((new HtmlSimple("Access Denied","\tToo many successive requested on invalid path - bye bye")).getPage());				
				joke++;
				running=false;
				transaction.close();
				continue;
			}
			//If user is not authenticated, refuse connection and return a 401 (auth needed) message
			if (uri.equals("REFUSED"))  {
				Log.admin(Level.INFO,"HTTP/403\t"+sock.getInetAddress().getHostAddress()+"\t"+uri);
				transaction.write((new HtmlSimple("Invalid credentials")).getPage());
				running=false;
				continue;
			}
			
			if (uri.endsWith("/logout.html"))  {
				transaction.write((HtmlRedirect.create302("/login.html", cookiename+"=\"\"; expires=Mon, 01-Jan-1901 00:00:00 GMT")).getPage());
				transaction.resetUser();
				this._authenticated = false;
				running=false;
				return;
			}
			
			//If user is not authenticated, refuse connection and return a 401 (auth needed) message
			User user = transaction.getUser();
			if (!_authenticated)  {
				if (user!=null){
					transaction.write((HtmlRedirect.create302("/index.html", cookiename+"="+HttpServer.generateSessionCookie(user))).getPage());
					return;
				} 
				Log.admin(Level.INFO,"HTTP/401\t[unknown]\t"+sock.getInetAddress().getHostAddress()+"\t"+uri);
				if (i==0) {
				  transaction.write(new HtmlAuth().getPage());
				  i++;
				}
				return;
			}
			
			//If client message was a post, return a 200-ok message
			if (transaction.getTypeReq()==METHOD.POST)  {
				Log.admin(Level.INFO,"HTTP/POST\t["+user.getLogin()+"]\t"+sock.getInetAddress().getHostAddress()+"\t"+uri);
                String params = RequestsManager.proceedPost(uri,transaction, user);
				if (HttpServer.debug>=3) Log.trace(Level.FINER,"POST:Sending confirmation mess");
				if (debug) System.err.println(params);
				
                transaction.write((new HtmlFile(HttpServer.path_To_Files+uri,params, user)).getPage());
				continue;
			}
			if (HttpServer.debug>=3) Log.trace(Level.FINER,"File requested:" + HttpServer.path_To_Files+uri);

			try {
				Log.admin(Level.INFO,"HTTP/GET\t["+user.getLogin()+"]\t"+sock.getInetAddress().getHostAddress()+"\t"+uri);
				transaction.write((new HtmlFile(HttpServer.path_To_Files+uri, user)).getPage());
			} catch (Exception e){
				if (HttpServer.debug>=2) Log.trace(Level.FINER,e.toString());
				Log.admin(Level.INFO,"HTTP/304\t["+user.getLogin()+"]\t"+sock.getInetAddress().getHostAddress()+"\t"+uri);
				transaction.write((new HtmlSimple("Administration","File not founded")).getPage());
			}//End try&catch
			if (HttpServer.debug>=3) Log.trace(Level.FINER,"File sended.");
		}//End while
        
	} catch (Exception e){
		if (HttpServer.debug>=2) e.printStackTrace();
		if (HttpServer.debug>=2) Log.trace(Level.FINER,e.toString());
	}
	if (HttpServer.debug>=3) Log.trace(Level.FINER,"Connection resetted.");
	closeTransaction();
}
//<------------------------------------------------------------------------------------------>


//<------------------------------------------------------------------------------------------>
/** Transaction initialization between browser and server*/
public void initTransaction(){
	try {
        //sock.setSoLinger(true, 0);
    	transaction.init(this, sock);
    }catch(Exception e){
        if (Log.finest()) Log.trace(Level.FINEST, e);
		transaction.write(new HtmlSimple("SmartDNS Configuration","Error").getPage());
	}
}
//<------------------------------------------------------------------------------------------>
	
	
//<------------------------------------------------------------------------------------------>
/** Close all dialogs between browser and server*/
public void closeTransaction(){
    transaction.close();
}
//<------------------------------------------------------------------------------------------>


//<------------------------------------------------------------------------------------------>
/** Set the pool manager for this thread
 * @param poolThreads Thread pool in which this session belongs
 */
public static void setPool(PoolThreads poolThreads){
	pool = poolThreads;
}
//<------------------------------------------------------------------------------------------>


//<------------------------------------------------------------------------------------------>
/**
 * Give the application absolute path 
 * @return String
 */
public final static String getApplicationPath(){
  		String AppPath = new String();
  		File current = new File("."); // set to the relative path
	   	AppPath = current.getAbsolutePath(); // get the real path
	   	AppPath = AppPath.substring(0,AppPath.length()-2); // remove the '\.'
   		AppPath = AppPath + File.separator;
   		return AppPath;
}// End getApplicationPath
//<------------------------------------------------------------------------------------------>



	
//<------------------------------------------------------------------------------------------>
/** @return true if the client (browser) is authenticated*/
public boolean isAuthenticated(){
	return this._authenticated;
}

/** 
 * Set if the client (browser) is authenticated
 * @param authenticated user authentication state
 */
public void setAuthenticationStatus(boolean authenticated){
	this._authenticated = authenticated;
}
//<------------------------------------------------------------------------------------------>

	
}


