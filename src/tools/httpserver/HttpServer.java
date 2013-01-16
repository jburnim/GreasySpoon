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
//Import
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.util.*;
import java.util.logging.Level;
import tools.logger.Log;
import tools.sec.Encryption;
import tools.httpserver.custom.ProjectSpecifics;
import tools.monitor.*;
///////////////////////////////////


/**
 * Create an HTTP(s) server that can be used for application administration.
 * Switch from http to https requires:
 * <ul>
 * 	<li>generating a SSL key and storing it in a keystore.</li>
 *  <li>starting server with  switches "-Djavax.net.ssl.trustStore=keystore -Djavax.net.ssl.keyStore=keystore -Djavax.net.ssl.keyStorePassword="password"</li>
 * </ul>
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class HttpServer implements HttpConstants {



    /**Thread receiving TCP requests*/
    private static TcpServer serveurTCP;

    boolean running = false;

    /**Debug level (0=none, 3=all)*/
    public static int debug = 0;

    /**Pool managing threaded connections*/
    PoolThreads connexionPool;

    /**Path to access server delivered content*/
    protected static String path_To_Files = getApplicationPath()+"admin";
    protected static String path_To_Images = File.separatorChar+"img"+File.separatorChar;

    /**Set if server must run over SSL (HTTPS) or not*/
    protected static boolean overSSL = false;

    private static String keystoreName = "certificate.ssl";

    /**Default administrator mail address*/
    protected static String adminmail = "none";	

    private static User admin = new User("admin", "admin", RIGHTS.ADMIN);
    
    /**Path to configuration files that can be edited through administration*/
    public static String conf_path = getApplicationPath()+"conf"+File.separator;
    
    /**HTTP server configuration file name*/
    protected static String configurationFile = "httpserver.conf";

    /**Listening port (default:3132)*/
    protected static int port = 3132;
    /**Nb threads in pool, ie max number of simultaneous connections(Default: 10)*/
    protected static int nb_threads = 10;
    /**bounded IP (allow to restrict client connection to one fixed IP, default:null)*/
    protected static String bounded_ip="";
    /**TCP queue used for pending connection requests.*/
    protected static int backlog=10;

    /**Server startup time*/
    public static long starttime = System.currentTimeMillis();
    protected static HttpServer self;
    
    static Encryption encrypter =  new Encryption("5642315dfhuieh5zef3sdfgSDFGfg");

    protected static Vector<User> users = new Vector<User>();

    protected int maxusersid = 0;
 
    /** Editor Parameters */
    protected static boolean editor_on = true;
	protected static boolean autocomplete_on = false;
	protected static boolean start_highlight  = true;
	protected static boolean word_wrap  = true;
	protected static boolean replace_tab_by_spaces  = true;
	
	protected static ServerStatistics systemLoadMonitor;
 
//  <------------------------------------------------------------------------------------------>
    /**
     * Create a new Http Server.
     * Load the configuration and start the server.
     */
    public HttpServer() {
        self = this;
        getConfigParameters();
        start();
    } 
    /**
     * Create a new Http Server.
     * Load the configuration and start the server.
     * @param configurationfile Configuration file (absolute or relative) to load parameters from 
     */
    public HttpServer(String configurationfile) {
        self = this;
        HttpServer.configurationFile = configurationfile;
        getConfigParameters();
        systemLoadMonitor = new ServerStatistics(path_To_Files+"/stats/",path_To_Files+"/stats/");
        start();
        
    } 
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Restart application 
     */
    public static void restartProcess(){
    	ProjectSpecifics.restartApplication();
    }
//  <------------------------------------------------------------------------------------------>
    
 
//  <------------------------------------------------------------------------------------------>
    /**
     * Generates a session cookie to authenticate a user session
     * @param user User account for which to generate session cookie
     * @return generated session cookie value
     */
    public static String generateSessionCookie(User user){
    	Encryption userencrypter =  new Encryption(user.getPwd());
    	return encrypter.encrypt(user.getLogin()+":"+userencrypter.encrypt(user.getLogin()));
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Extract User account from session cookie
     * @param cookievalue the cookie value provided by user browser 
     * @return User profile associated to session cookie, or null if cookie is invalid 
     */
    public static User extractUserFromSessionCookie(String cookievalue){
    	String uncr = encrypter.decrypt(cookievalue);
    	if (uncr ==null )return null;
    	int spare = uncr.indexOf(":");
    	if (spare<0) return null;
    	String login  = uncr.substring(0,spare);
    	User user = null;
    	if (login.equals(admin.getLogin())){
    		user = admin;
    	} else {
	    	for (User u:users){
	    		if (u.getLogin().equals(login)) {
	    			user = u;
	    			break;
	    		}
	    	}
    	}
    	if (user==null) return null;
    	Encryption userencrypter =  new Encryption(user.getPwd());
    	String usercodedlogin  = userencrypter.decrypt(uncr.substring(spare+1));
    	if (login.equals(usercodedlogin)) return user;
    	return null;
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Check and retrieve user with given login and password 
     * @param login user login
     * @return user matching given login and password, or null if none
     */
    public static User getUser(String login){
    	User user = null;
    	for (User u:users){
    		if (u.getLogin().equalsIgnoreCase(login)) {
    			user = u;
    			break;
    		}
    	}
    	return user;
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Check and retrieve user with given login and password 
     * @param login user login
     * @param pwd user password
     * @return user matching given login and password, or null if none
     */
    public static User getUserFor(String login, String pwd){
    	User user = null;
    	if (login.equalsIgnoreCase(admin.getLogin()) && pwd.equals(admin.getPwd())){
    		user = admin;
    	} else {
	    	for (User u:users){
	    		if (u.getLogin().equalsIgnoreCase(login) && u.getPwd().equals(pwd)) {
	    			user = u;
	    			break;
	    		}
	    	}
    	}
    	return user;
    }
//  <------------------------------------------------------------------------------------------>
    
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Start the server.
     * @return true if server is running
     */
    public boolean start() {
        if (running) return running;
        systemLoadMonitor.start();
        running = true;
        connexionPool = new PoolThreads(nb_threads);
        addTCP();
        return running;
    }

    /**
     * Stop HTTP server
     * @return false if server is halted
     */
    public boolean stop() {
        if (!running) return running;
        running = false;
        systemLoadMonitor.stop();
        connexionPool.disable();
        serveurTCP.interrupt();
        connexionPool = null;
        serveurTCP.disable();
        return running;
    }
    /**
     * restart the server (sends stop=>start)
     * @param resetKeystore Set if SSL keystore must be resetted (updated with new password)
     */
    public void restart(boolean resetKeystore){
    	if (resetKeystore) SslSocketFactory.resetKeystore(conf_path+keystoreName, admin.getPwd());
        stopstart();
    }
    private void stopstart(){
        if (!stop()) start();
    }
    
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Update server bounded IP address (adress from which access is allowed)
     * @param ip the IP address to bound
     */
    public void setIP(String ip){
        bounded_ip = ip.trim();
        restart(false);
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Set server listening port
     * @param newport The port to use.
     */
    public void setPort(int newport){
        port = newport;
        restart(false);
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Switch server between http and https
     * @param usessl True to enable https, False to enable http 
     */
    public void useSsl(boolean usessl){
        overSSL = usessl;
        restart(false);
    }

    /**
     * Set server HTML file path.<br>
     * Only files under this path are directly reachable. 
     * @param path Path to HTML files
     */
    public void setFilePath(String path){
        path_To_Files = path;
        restart(false);
    }
    /**
     * Set server socket backlog.<br>
     * If maximum simultaneous connections are reached, new connections are put in waiting state in backlog.<br>
     * If backlog becomes full, new connections are rejected.
     * @param backlogsize Backlog size
     */
    public void setBacklog(int backlogsize){
        HttpServer.backlog = backlogsize;
        restart(false);
    }
//  <------------------------------------------------------------------------------------------>


//  <------------------------------------------------------------------------------------------>
    /**
     * Set number of supported threads (simultaneous connections allowed).<br>
     * If threads are all used, new connections fall into backlog
     * @param threadsnb Thread number that the server supports.
     */
    public void setThreadNb(int threadsnb){
        nb_threads = threadsnb;
        connexionPool = new PoolThreads(nb_threads);
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Check is given pwd is correct
     * @param   password  pwd to check, in clear 
     * @return  true if it matches server pwd, false otherwise   
     */
    public static boolean checkPwd(String password) {
        if (password.equals(admin.getPwd())) return true;
        return false;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Check is given Base64 encoded pwd is correct
     * @param   b64pwd B64 encoded login+pwd (browser standard)
     * @return  true if it matches server pwd, false otherwise
     */
    public static RIGHTS checkB64Pwd(String b64pwd) {
        if (b64pwd.equals(encodeInBase64(admin.getLogin(),admin.getPwd()))) return RIGHTS.ADMIN;
        for (User user:users){
        	if (b64pwd.equals(encodeInBase64(user.getLogin(),user.getPwd()))) return user.getRights();
        }
        return RIGHTS.NONE;
    }
    
    /**
     * Extract User from base 64 login/pwd encoding
     * @param b64pwd the base 64 login/pwd encoding as provided by browser
     * @return User account if lg/pwd are valid, null otherwise
     */
    public static User getUserForB64Pwd(String b64pwd) {
    	if (b64pwd.equals(encodeInBase64(admin.getLogin(),admin.getPwd()))) return admin;
        for (User user:users){
        	if (b64pwd.equals(encodeInBase64(user.getLogin(),user.getPwd()))) return user;
        }
        return null;
    }
//  <------------------------------------------------------------------------------------------>



//  <------------------------------------------------------------------------------------------>
    /**
     * Set a new password.
     * @param   oldpwd the old password, needed to accept the change
     * @param   newpwd the new password
     * @return  true if change is accepted (old pwd is correct), false otherwise
     */
    public static boolean setPassword(String oldpwd, String newpwd) {
        if (!oldpwd.equals(admin.getPwd())) return false;
        admin.setPwd(newpwd);
        //update configuration file with new pwd
        return true;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Ajoute a TCP server socket
     */
    public void addTCP() {
        serveurTCP = new TcpServer();
        serveurTCP.start();
    }
//  <------------------------------------------------------------------------------------------>


//  <------------------------------------------------------------------------------------------>

    /**
     * TCP server thread
     * Manages indifferently http/https connections
     */
    public class TcpServer extends Thread{
    	
    	/**Create a server thread for administration interface*/
    	public TcpServer(){
    		super("WebAdmin");
    	}
    	
        /**
         * @see java.lang.Thread#run()
         */
        public void run() {
            if (overSSL) {
                serveSslTCP();
            } else {
                serveNormalTCP();
            }
        }

        /**
         * Halt running server thread
         */
        public void disable(){
            try{
                if (sslServerSocket!=null) {
                    sslServerSocket.close();
                }
                if (normalServerSocket!=null) {
                    normalServerSocket.close();
                }
            } catch (Exception e){
            	//nothing to clean
            }
        }
        /**
         * Launch TCP server socket over SSL (https) 
         */
        SSLServerSocket sslServerSocket = null;
        void serveSslTCP() {
            sslServerSocket = null;
            /**Flux de reception/emission*/
            try {
                //SSLServerSocketFactory sslSrvFact = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                //sslServerSocket =(SSLServerSocket)(sslSrvFact.createServerSocket());
            	sslServerSocket =(SSLServerSocket)SslSocketFactory.createServerSocket(conf_path+keystoreName, admin.getPwd());
                sslServerSocket.setReuseAddress(true);
                sslServerSocket.bind(bounded_ip.equals("")?new InetSocketAddress(port):new InetSocketAddress(bounded_ip,port),backlog);
                if (Log.info())Log.error(Level.INFO,"Administration Server started on https://"+(bounded_ip.equals("")?"0.0.0.0":bounded_ip)+":"+port+"/");
                try{
                    Socket s;
                    while (running) {
                            s= sslServerSocket.accept();
                            connexionPool.assignTask(s);
                    }//while true
                } catch (Exception e) {
                    if (Log.finest()) Log.trace(Level.FINEST, e);
                }
            } catch (Exception e) {
                if (Log.severe())Log.error(Level.SEVERE, "HTTPS administration server error", e);
            }//try&catch
            finally {
                try{
                    sslServerSocket.close();
                } catch (Exception e1){
                	//nothing more to clean
                }
            }
            if (Log.info())Log.error(Level.INFO,"HTTPS Server stopped");
        }
//      <------------------------------------------------------------------------------------------>


//      <------------------------------------------------------------------------------------------>
        /**
         * Launch TCP server socket (http) 
         */
        ServerSocket normalServerSocket = null;
        void serveNormalTCP() {
            normalServerSocket = null;
            try {
            	normalServerSocket = new ServerSocket();
                normalServerSocket.setReuseAddress(true);
                normalServerSocket.bind(bounded_ip.equals("")?new InetSocketAddress(port):new InetSocketAddress(bounded_ip,port),backlog);
                if (Log.info())Log.error(Level.INFO,"Administration Server started on http://"+(bounded_ip.equals("")?"0.0.0.0":bounded_ip)+":"+port+"/");
                try{
                    while (running) {
                       connexionPool.assignTask(normalServerSocket.accept());
                    }//while true
                } catch (Exception e) {
                	//runtime exceptions are discarded
                }
            } catch (Exception e) {
                if (Log.severe()) Log.error(Level.SEVERE, "Unable to start Web Administration process - check IPv4/IPv6 configuration or force IPv4 mode using '-Djava.net.preferIPv4Stack=true' option - ", e);
                try{normalServerSocket.close();}catch (Exception e1){
                	//nothing more to clean
                }
            }//try&catch
            finally {
                try{
                    normalServerSocket.close();
                } catch (Exception e1){
                	//nothing more to clean
                }
            }
            if (Log.info())Log.error(Level.WARNING,"HTTP Server stopped");
        }
//      <------------------------------------------------------------------------------------------>
    }

//  <------------------------------------------------------------------------------------------>
    /**
     * Return the application absolute path (with a trailing "/") 
     * @return String the application absolute path (with a trailing "/")
     */
    public final static String getApplicationPath(){
        String AppPath = new String();
        File current = new File("."); // set to the relative path
        AppPath = current.getAbsolutePath(); // get the real path
        AppPath = AppPath.substring(0,AppPath.length()-2); // remove the '\.'
        AppPath = AppPath + File.separator;
        return AppPath;
    }// End getApplicationPath
//  <------------------------------------------------------------------------------------------>


    /**B64 encoder array*/
    private final static char base64Array [] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/'
    };

//  <------------------------------------------------------------------------------------------>
    /**
     * Return browser standard username+pwd B64 encoding.<br>
     * Standard (default) encoding in response to HTTP 403 authentication requests.  
     * @param login login or username
     * @param password password
     * @return B64 encoded string containing username:password
     */
    public static String encodeInBase64(String login, String password) {
        return base64Encode (login + ":" + password);       
    }
//  <------------------------------------------------------------------------------------------>


//  <------------------------------------------------------------------------------------------>
    /**
     * B64 encoding method
     * @param   string  String to encode
     * @return  encoded string   
     */
    private static String base64Encode (String string)    {
        String encodedString = "";
        byte bytes [] = string.getBytes ();
        int i = 0;
        int pad = 0;
        while (i < bytes.length) {
            byte b1 = bytes [i++];
            byte b2;
            byte b3;
            if (i >= bytes.length) {
                b2 = 0;
                b3 = 0;
                pad = 2;
            }
            else {
                b2 = bytes [i++];
                if (i >= bytes.length) {          
                    b3 = 0;
                    pad = 1;
                }
                else
                    b3 = bytes [i++];
            }
            byte c1 = (byte)(b1 >> 2);
            byte c2 = (byte)(((b1 & 0x3) << 4) | (b2 >> 4));
            byte c3 = (byte)(((b2 & 0xf) << 2) | (b3 >> 6));
            byte c4 = (byte)(b3 & 0x3f);
            encodedString += base64Array [c1];
            encodedString += base64Array [c2];
            switch (pad) {
            case 0:
                encodedString += base64Array [c3];
                encodedString += base64Array [c4];
                break;
            case 1:
                encodedString += base64Array [c3];
                encodedString += "=";
                break;
            case 2:
                encodedString += "==";
                break;
            }
        }
        return encodedString;      
    }
//  <------------------------------------------------------------------------------------------>



//  <------------------------------------------------------------------------------------------>
    /**
     * Analyze configuration file. Initialize server values.
     */
    public void getConfigParameters(){
        String fileToEdit = configurationFile;
        File fich = new File(fileToEdit);
        if (!fich.exists()){
            if (Log.warning())Log.error(Level.SEVERE,"Error. Cannot find configuration file <"+fich.toString()+">");
            return;
        }// Endif

        try {   	
            BufferedReader in = new BufferedReader(new FileReader(fileToEdit));
            String str;

            //Read a line
            while ((str = in.readLine()) !=null ){
                str.trim();
                parseConf(str);
            }//End while readLine
            in.close(); 		
        } catch (IOException e){
            if (Log.warning())Log.error(Level.SEVERE,"Error in configuration file <"+configurationFile+">. File corrupted.");
            System.exit(-1);
        }//End try&catch
    }//End getConfigParameters
//  <------------------------------------------------------------------------------------------>



//  <------------------------------------------------------------------------------------------>
    /**
     * Parse a configuration file string. Set founded server values.
     * @param str Configuration line to parse
     */
    public void parseConf(String str){
        // Regarde s'il s'agit d'un commentaire
        if (str.length()==0) return;
        if (str.startsWith("#") || str.startsWith(";")) return;

        //Configuration des param�tres du serveur d'administration
        if (str.toLowerCase().startsWith("admin.htmlpath")) path_To_Files = readSingleString(str);
        else if (str.toLowerCase().startsWith("log.level")) Log.setLogLevel(readSingleString(str));
        else if (str.toLowerCase().startsWith("log.path")) Log.setLogPath(readSingleString(str));
        
        else if (str.toLowerCase().startsWith("admin.ipbounded")) bounded_ip = readSingleString(str);
        else if (str.toLowerCase().startsWith("admin.port")) port = readSingleValue(str);
        else if (str.toLowerCase().startsWith("admin.backlog")) backlog = readSingleValue(str);
        else if (str.toLowerCase().startsWith("admin.threads")) nb_threads = readSingleValue(str);
        else if (str.toLowerCase().startsWith("admin.pwd")) setEncryptedPassword(readSingleString(str));
        else if (str.toLowerCase().startsWith("admin.ssl")) overSSL = readSingleBoolean(str);
        
        else if (str.toLowerCase().startsWith("admin.editor.editor_on")) editor_on = readSingleBoolean(str);
        else if (str.toLowerCase().startsWith("admin.editor.autocomplete_on")) autocomplete_on = readSingleBoolean(str);
        else if (str.toLowerCase().startsWith("admin.editor.start_highlight")) start_highlight = readSingleBoolean(str);
        else if (str.toLowerCase().startsWith("admin.editor.word_wrap")) word_wrap = readSingleBoolean(str);
        else if (str.toLowerCase().startsWith("admin.editor.replace_tab_by_spaces")) replace_tab_by_spaces = readSingleBoolean(str);
        
        else if (str.toLowerCase().startsWith("admin.mail")) adminmail = readSingleString(str);
        else if (str.toLowerCase().startsWith("admin.user.")) {
        	String[] params = str.split("\\s+");
        	addUserWithEncryptedPwd(params[params.length-3],params[params.length-2],RIGHTS.valueOf(params[params.length-1]));
        }
    }	

//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Update server configuration file.
     * @param params the parameters to store
     * @param values associated values to parameters
     * @param purge prefix for which existing matching parameters will be discarded (set to null to avoid) 
     */
    protected static void saveConf(String[] params, String[] values) {
    	saveConf(params, values, null);
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Update server configuration file.
     * @param params the parameters to store
     * @param values associated values to parameters
     * @param purge prefix for which existing matching parameters will be discarded (set to null to avoid) 
     */
    protected static void saveConf(String[] params, String[] values, String purge) {
        boolean[] todo = new boolean[params.length];
        for (int i=0; i<params.length;i++){
            todo[i]=true;
        }
        File confFile = new File(configurationFile);
        StringBuilder conf=new StringBuilder();
        if (confFile.exists()){
            try {   	
                BufferedReader in = new BufferedReader(new FileReader(confFile));
                String str;
                //Read a line
                while ((str = in.readLine()) !=null ){
                    str.trim();
                    if (purge!=null && str.toLowerCase().startsWith(purge)) continue;
                    boolean matched = false;
                    for (int i=0; i<params.length;i++){
                    	if (!params[i].endsWith(" ")) params[i]+=" ";
                        if (str.toLowerCase().startsWith(params[i])) {
                            if (values[i]!=null) conf.append(params[i]).append("\t").append(values[i]).append("\n");
                            todo[i] = false;
                            matched = true;
                            break;
                        }   
                    }
                    if (!matched) {
                        conf.append(str).append("\n");
                    }
                }//End while readLine
                in.close(); 		
            } catch (IOException e){
                if (Log.config())Log.error(Level.WARNING,
                           "Error saving configuration file <"+configurationFile
                           +">. File corrupted.");
                //System.exit(-1);
            }//End try&catch
        }
        for (int i=0; i<todo.length;i++){
            if (todo[i] && values[i]!=null ) {
                conf.append(params[i]).append("  \t").append(values[i]).append("\n");
                todo[i] = true;
            }   
        }
        confFile.delete();
        confFile=new File(configurationFile);
        try {
            RandomAccessFile raf = new RandomAccessFile(confFile,"rw");
            raf.writeBytes(conf.toString());
            raf.close();
        }// try
        catch(IOException e){
            if (Log.config())Log.error(Level.CONFIG,"HttpServer->writeconfig: " + e.toString());
        }// catch
    }
//  <------------------------------------------------------------------------->

//  <------------------------------------------------------------------------->
    /** Save current users profiles*/
    public synchronized static void saveUsers(){
    	String[] params = new String[users.size()];
    	String[] values = new String[users.size()];
    	User user;
    	for (int i=0; i<users.size(); i++){
    		user = users.elementAt(i);
    		params[i] = "admin.user."+i;
    		values[i] = user.getLogin() + " " + encrypter.encrypt(user.getPwd()) + " " + user.getRights();
    	}
    	saveConf(params, values, "admin.user.");
    }
//  <------------------------------------------------------------------------->

//  <------------------------------------------------------------------------------------------>
    /**
     * Read a single value from a configuration file line
     * (Syntax: param = value)
     * @param   conf  configuration file line
     * @return  founded boolean value, false otherwise   
     */
    public static boolean readSingleBoolean(String conf) {
        String[] values = conf.split("\\s+");
        if (values.length<2) return false;
        if (values[values.length-1].trim().equalsIgnoreCase("true") || values[values.length-1].trim().equalsIgnoreCase("on")) return true;
        return false;
    }
//  <------------------------------------------------------------------------------------------>


//  <------------------------------------------------------------------------------------------>
    /**
     * Read a single value from a configuration file line
     * (Syntax: param = value)
     * @param   conf  configuration file line
     * @return  parameter value 
     */
    public static String readSingleString(String conf) {
        //String value = "";
        String[] values = conf.split("\\s+",2);
        if (values.length<2) return "";
        return values[1].trim();
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Read a single value from a configuration file line
     * (Syntax: param = value)
     * @param   conf  configuration file line
     * @return  parameter value 
     */
    public int readSingleValue(String conf) {
        String[] values = conf.split("\\s+");
        int result = 0;
        if (values.length<2) return result;
        try {
            result = Integer.parseInt(values[values.length-1]);
        } catch(NumberFormatException e){
            if (Log.config())Log.error(Level.CONFIG,"Error in configuration file: " + conf);
        }//End catch
        return result;
    }
//  <------------------------------------------------------------------------------------------>


//  <------------------------------------------------------------------------------------------>
    /**
     * Get pwd under internal encryption mode
     * @return Encrypted password
     */
    public static String getEncryptedPassword(){
    	return encrypter.encrypt(admin.getPwd());
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Set internal pwd by providing encrypted pwd
     * @param password encrypted password to use
     */
    public static void setEncryptedPassword(String password)	{
    	admin.setPwd(encrypter.decrypt(password));
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Add a user account to authorized users
     * @param login The user login
     * @param password Encrypted user password
     * @param rights Rights associated to account
     */
    public static void addUserWithEncryptedPwd(String login, String password, RIGHTS rights){
    	String clearuserpwd = encrypter.decrypt(password);
    	addUser(login,clearuserpwd, rights);
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Add a user account to authorized users
     * @param login The user login
     * @param password clear user password
     * @param rights Rights associated to account
     */
    public static void addUser(String login, String password, RIGHTS rights){
    	if (login.equalsIgnoreCase(admin.getLogin())){
    		Log.error(Log.WARNING, "Trying to add user account with [admin] login - account rejected");
    		return;
    	}
    	User user = new User(login,password, rights);
    	User olduser = HttpServer.getUser(login);
    	if (olduser!=null){
    		users.remove(olduser);	
    	}
    	users.add(user);
    }
//  <------------------------------------------------------------------------------------------>
    
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Remove a user account from authorized users
     * @param login The user login
     */
    public static void delUser(String login){
    	if (login.equalsIgnoreCase(admin.getLogin())){
    		Log.error(Log.WARNING, "Trying to remove user account with [admin] login - account rejected");
    		return;
    	}
    	User user = HttpServer.getUser(login);
    	if (user!=null){
    		users.remove(user);	
    	}
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * static method called by external applications
     * (for example JNT service tool under MS Windows when running as a service)
     */
    public static void stopApplication(){
        System.exit(0);
    }
//  <------------------------------------------------------------------------------------------>


    /**
     * Default main to start server without attached application
     * @param args Not used here
     */
    public static void main(String args[])	{
        new HttpServer();
    }
	/**
	 * @return path to Administration HTML pages
	 */
	public static String getPathToFiles() {
		return path_To_Files;
	}
	/**
	 * @return Administration users
	 */
	public static Vector<User> getUsers() {
		return users;
	}
}//End class HttpServer
