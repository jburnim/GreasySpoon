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
 * Created  :   27 janv. 2006
 *---------------------------------------------------------------------------*/
package icap;



///////////////////////////////////
//Import
import icap.core.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.lang.reflect.*;
import tools.httpserver.*;
import tools.logger.Log;
import tools.logger.StdLogger;
import tools.general.MimeMagic;
///////////////////////////////////


/**
 * Main application class, launch ICAP servers.<br>
 * Parse configuration file, configure and instantiate the requested services.<br>
 * Each service is run with its own port and server socket.
 * Version history:<br>
 * <ul>
 * <li>1.0.0 	 : Major code review - includes several addons (thanks to Brad)</li>
 * <li>0.6.0 	 : switch to 1.0.0</li>
 * <li>0.5.6 	 : Small improvements</li>
 * <li>0.5.5 	 : Fix ordering tag and log display under Linux</li>
 * <li>0.5.4-b02 : Fix bug in Preview mode for starved responses, and in REQMOD with POST requests</li>
 * <li>0.5.4 	 : Fix bug in Compressor, in Preview mode, in update content-length/update mimetype methods</li>
 * <li>0.5.3 	 : Fix bug in reqmod preview - GS upgrade to support responsecode tag</li>
 * <li>0.5.2 	 : Add MimeMagic and fix infinite loop when receiving invalid chunks in preview mode</li>
 * <li>0.5.1 	 : Fix HTML encoding issues</li>
 * <li>0.5.0-b03 : Code cleanup and bug prevention enforcement (log level setting, invalid regexes,...)</li>
 * <li>0.5.0-rc1 : full support of preview added. Simultaneous clients brands support added.</li>
 * <li>0.4.2-rc2 : Fusion between REQMOD and RESPMOD services: services are now allowed to run in both modes simultaneously</li>
 * <li>0.4.0-rc1 : Overall cleanup: each service can have its own ICAP parameters</li>
 * <li>0.3.2-00  : Fixed ICAP bugs with netcaches.</li>
 * <li>0.3.1-b01 : Banner Service added.</li>
 * <li>0.3.0-b04 : Multiple services support.</li>
 * </ul>
 * <p>
 * @author Karel
 */
public class IcapServer extends Thread implements Icap {

	/** pre-generated 204 no content response that can be directly used by services implementations */
	public byte[] _204NOCONTENT;  

	/** Set if log are activated or not*/
	//public static boolean log = false;

	/**Server version*/
	public final static String version = "1.0.8-01";
	
		/**internal flag to check server availability*/
	private boolean running=false;

	/**IP to listen to*/
	private InetSocketAddress IP = null;
	/**ICAP listening port (1344 per default)*/
	private int port = 1344;

	/**If max connections is reach, number of connections to put in wait state before dropping*/
	private int backlog = 2000;

	static private boolean optimizeTCP = false;

	/** ICAP service name used to communicate with ICAP client */
	public String serverName = "";

	VectoringPoint mode;

	/** OPTIONS response send to ICAP client */
	protected String OPTIONS="ICAP/1.0 200 OK\r\n";
	/** Service ISTAG*/
	protected String ISTAG="";

	/** Configuration file directory, ending with trailing path separator */
	public final static String confDirectory = "conf"+File.separatorChar;

	/** config file name per default*/
	static String defaultconfig = confDirectory+"icapserver.conf";


	/** Properties containing service configuration parameters */
	public Properties serviceconfig = new Properties();

	/** set if keep-alive connections are used or not*/
	protected boolean keepalive = false;
	/**icap host header*/
	public String icaphost = "";

	private Class<? extends AbstractService> IcapService=null;
	private Constructor<? extends AbstractService> constructor=null;

	static HttpServer httpserver;
	static boolean webadminstatus = false;

	/** Vector used to store running servers /services */
	private static Vector<IcapServer> servers = new Vector<IcapServer>();

	/** config file used at runtime */
	private static String runningconfig = "";

	private ServerSocket serviceSocket = null;
	
	/**Flag used to log all displayed messages to a file (no more std/err output)*/
	public static boolean turnStdOff = false;
	
	/**Defines the size of the thread pool (if used) */
	public int poolSize	= 20;
	
	/**ICAP connections timeout, in ms. Force connection close if no data is received after this time.*/
	public static int ICAP_SO_TIMEOUT = 900000;

	
	//	<--------------------------------------------------------------------------->
	/**
	 * Create an new ICAP server, using given name, IP and port and running in given
	 * Vectoring point mode (REQMOD or RESPMOD) <br>
	 * @param name Service name
	 * @param IP the IP iCAP server will listen to
	 * @param port the TCP port iCAP server will listen to
	 * @param serviceconfigfile service configuration file
	 */
	public IcapServer(String name, String IP, int port, String serviceconfigfile){
		super("IcapServer");
		this.serverName = name;

		//load service properties (if any)
		if (serviceconfigfile!=null){
			try{
				this.serviceconfig.load(new FileInputStream(confDirectory+serviceconfigfile));
			} catch (Exception e){
				System.err.println("invalid service configuration file provided for ["+name+"] :"+confDirectory+serviceconfigfile);
			}
		}

		//set service IP address
		try {
			if (IP==null) {
				this.IP = new InetSocketAddress(InetAddress.getLocalHost(), port);
			} else if (IP.equals("*")){
				this.IP = new InetSocketAddress(port);
			} else {
				this.IP = new InetSocketAddress(InetAddress.getByName(IP), port);
			}
		} catch (Exception e){
			if (!turnStdOff) System.err.println("[ICAP Server]: Unable to open socket on local host with port:"+port+". Aborting.");
			Log.error(Log.SEVERE, "[ICAP Server]: Unable to open socket on local host with port:"+port+". Aborting.");
			System.exit(1);
		}


		this.port = port;
		this.icaphost = "Host: "+ this.IP.getAddress().getHostAddress()+":"+ port+CRLF;


		//Initialize instance of service class
		try {
			this.IcapService = Class.forName("icap.services."+this.serverName).asSubclass(AbstractService.class);
			/*if (usePool){
				this.constructor = IcapService.getConstructor(IcapServer.class);
				this.mode = (this.constructor.newInstance(this)).getSupportedModes();
			} else {
			*/
				this.constructor = IcapService.getConstructor(IcapServer.class, Socket.class);
				this.mode = (this.constructor.newInstance(this, null)).getSupportedModes();
			//}
			
		} catch (Exception e) {
			if (!turnStdOff) System.err.println("[ICAP Server]: Unable to instanciate service: ["+serverName+"]. Aborting.");
			Log.error(Log.SEVERE, "[ICAP Server]: Unable to instanciate service: ["+serverName+"]. Aborting.", e);
			System.exit(-1);
		} 

		//set OPTIONS parameters from configuration or from default if not provided
		this.ISTAG = "ISTag: \""+serviceconfig.getProperty("ISTag", name+"-"+IcapServer.version)+"\"";
		this.OPTIONS +=this.ISTAG+CRLF;
		this.OPTIONS +="Service: "+this.serviceconfig.getProperty("Service", name).trim()+CRLF;
		this.OPTIONS +="Service-ID: "+this.serviceconfig.getProperty("Service-ID", name).trim()+CRLF;
		this.OPTIONS +="Methods: "+ (this.mode==VectoringPoint.REQRESPMOD?"REQMOD, RESPMOD":this.mode.toString())+CRLF;
		this.OPTIONS +="Options-TTL: "+this.serviceconfig.getProperty("Options-TTL", "300").trim()+CRLF;
		
		// Check max connections parameter. If not set, fix a 100 value and no pool.
		// Otherwise, set up a pool with given connections number
		if (this.serviceconfig.containsKey("Max-Connections")){
			this.poolSize = Integer.parseInt(this.serviceconfig.getProperty("Max-Connections").trim());
			this.OPTIONS +="Max-Connections: "+this.serviceconfig.getProperty("Max-Connections").trim()+CRLF;
		} else {
			this.OPTIONS +="Max-Connections: "+100+CRLF;
		}
				
		this.OPTIONS +="Allow: "+this.serviceconfig.getProperty("Allow", "204").trim()+CRLF;
		if (this.serviceconfig.containsKey("Preview")) {
			OPTIONS +="Preview: "+this.serviceconfig.getProperty("Preview", "0").trim()+CRLF;
			OPTIONS +="Transfer-Preview: "+this.serviceconfig.getProperty("Transfer-Preview", "*").trim()+CRLF;
			if (this.serviceconfig.containsKey("Transfer-Complete")) OPTIONS +="Transfer-Complete: "+serviceconfig.getProperty("Transfer-Complete")+CRLF;
		}
		if (this.serviceconfig.containsKey("Transfer-Ignore")) OPTIONS +="Transfer-Ignore: "+serviceconfig.getProperty("Transfer-Ignore")+CRLF;
		this.OPTIONS +="X-Include: "+serviceconfig.getProperty("X-Include", "X-Client-IP, X-Authenticated-Groups, X-Authenticated-User, X-Subscriber-Id").trim()+CRLF;
		if (this.serviceconfig.containsKey("Options-custom")) OPTIONS +=serviceconfig.getProperty("Options-custom")+CRLF;
		//this.OPTIONS +="Connection: close"+CRLF;
		this.OPTIONS +=CRLF;

		this._204NOCONTENT = (Icap._204_NOCONTENT + this.ISTAG+CRLF+CRLF).getBytes();

		// configure keep alive parameter for ICAP. 
		// Keep-alive cannot be activated simultaneously with connection pool.
		String ka = this.serviceconfig.getProperty("Keep-Alive","disable").trim().toLowerCase();
		if (ka.equals("off") || ka.startsWith("disable") || ka.equals("no") || ka.equals("close") ){
			this.keepalive = false;
			if (turnStdOff && Log.config()) Log.error(Level.CONFIG, "Keep-Alive connections desactivated. Connections pooling activated with ["+poolSize +"] threads");
			else System.out.println("Keep-Alive connections desactivated. Connections pooling activated with ["+poolSize +"] threads");
			backlog = poolSize;
		} else {
			this.keepalive = true;
			if (turnStdOff && Log.config()) Log.error(Level.CONFIG, "Keep-Alive connections activated. Connections pooling deactivated.");
			else System.out.println("Keep-Alive connections activated. Connections pooling deactivated.");
		}
		servers.add(this);
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	/**
	 * Allows to turn HTTP administration interface on/off
	 * @param status true to enable the interface
	 * @param config The configuration file used by the HTTP server
	 */
	static void setWebAdmin(boolean status, String config){
		if (webadminstatus == status) return;
		if (status==true){
			if (httpserver!=null) {
				httpserver.start();
			} else {
				httpserver = new HttpServer(config);
			}
		} else {
			if (httpserver!=null) httpserver.stop();
		}
	}
//	<--------------------------------------------------------------------------->

//	---------------------------------------------------------------------------
	/**
	 * Set System property with given value
	 * @param property The property to configure
	 * @param value The value to set
	 */
	public synchronized static void setProperty(String property, String value){
		Properties systemSettings = System.getProperties();
		if (value!=null) {
			systemSettings.put(property, value);
		} else {
			systemSettings.put(property, "");
		}
		System.setProperties(systemSettings);
	}
//	---------------------------------------------------------------------------

//	<--------------------------------------------------------------------------->  
	/**
	 * Read Server parameters.<br>Instantiate a new ICAP service for each corresponding
	 * configuration file line.
	 */
	private static void readConf(String config) {
		File fich = new File(config);
		if (!fich.exists()){
			System.err.println("Cannot find ICAP server configuration file <"+fich.toString()+">");
			System.err.println("Starting using default parameters");
			return;
		}// Endif
		try {       
			BufferedReader in = new BufferedReader(new FileReader(fich));
			String str;

			//pre-parse configuration to set log path correctly
			String logpath = null, loglevel = null;
			int maxlogentries=-1, maxlogfiles=-1;
			boolean accessEnabled=true,
			errorEnabled=true,adminEnabled=true,
			serviceEnabled=true, debugEnabled=true;
			while ((str = in.readLine()) !=null ){
				if (str.startsWith("#") || str.startsWith(";")) continue;
				if (str.startsWith("log.path")) {
					logpath = str.split("\\s+",2)[1];
				} else if (str.startsWith("log.level")) {
					loglevel = str.split("\\s+",2)[1].trim();
				} else if (str.startsWith("log.maxentries")) {
					maxlogentries = Integer.parseInt(str.split("\\s+",2)[1].trim());
				} else if (str.startsWith("log.maxfiles")) {
					maxlogfiles = Integer.parseInt(str.split("\\s+",2)[1].trim());
				} else if (str.startsWith("log.access.enable")) {
					accessEnabled = str.split("\\s+",2)[1].trim().toLowerCase().equals("on")?true:false;
				} else if (str.startsWith("log.error.enable")) {
					errorEnabled = str.split("\\s+",2)[1].trim().toLowerCase().equals("on")?true:false;
				} else if (str.startsWith("log.admin.enable")) {
					adminEnabled = str.split("\\s+",2)[1].trim().toLowerCase().equals("on")?true:false;
				}else if (str.startsWith("log.service.enable")) {
					serviceEnabled = str.split("\\s+",2)[1].trim().toLowerCase().equals("on")?true:false;
				} else if (str.startsWith("log.debug.enable")) {
					debugEnabled = str.split("\\s+",2)[1].trim().toLowerCase().equals("on")?true:false;
				} else if (str.startsWith("log.silent")) {
					turnStdOff = str.split("\\s+",2)[1].trim().toLowerCase().equals("on")?true:false;
				}
			}//End while readLine
			in.close();
			if (turnStdOff) StdLogger.redirectStdFlows("std",logpath);
			if (!IcapServer.turnStdOff) System.out.println("IcapServer version " + version);
			Log.setAccessEnabled(accessEnabled);
			Log.setErrorEnabled(errorEnabled);
			Log.setAdminEnabled(adminEnabled);
			Log.setServiceEnabled(serviceEnabled);
			Log.setDebugEnabled(debugEnabled);

			if (logpath != null) Log.setLogPath(logpath);
			if (loglevel != null) Log.setLogLevel(loglevel);
			if (maxlogentries != -1) Log.setMaxentries(maxlogentries);
			if (maxlogfiles != -1) Log.setMaxfiles(maxlogfiles);
			//Parse configuration for real services parameters			
			in = new BufferedReader(new FileReader(fich));  
			while ((str = in.readLine()) !=null ){
				if (str.startsWith("#") || str.startsWith(";")) continue;
				String[] values = str.split("\\s+");
				if (values == null || values.length <2) continue;
				if (values[0].equalsIgnoreCase("icap")){
					if (values.length==4) new IcapServer(values[1], values[2], Integer.parseInt(values[3]),null).enable();
					else if (values.length==5) new IcapServer(values[1], values[2], Integer.parseInt(values[3]), values[4]).enable();
					else System.err.println("Invalid service definition: "+str);
				}  else if (values[0].equalsIgnoreCase("chunk_tweak")){
					if (values[1].equalsIgnoreCase("on") || values[1].equalsIgnoreCase("true") ) {
						AbstractService.enableTcpTweak();
						if (turnStdOff && Log.config()) Log.error(Level.CONFIG,"Forced chunks acknowledgment activated");
						else System.out.println("Forced chunks acknowledgment activated");
					}
				} else if (values[0].equalsIgnoreCase("proxyhost")){
					IcapServer.setProperty("http.proxyHost",values[1].trim());
					if (turnStdOff && Log.config()) Log.error(Level.CONFIG,"Setting HTTP proxy as ["+values[1]+"]");
					else System.out.println("Setting HTTP proxy as ["+values[1]+"]");
				} else if (values[0].equalsIgnoreCase("proxyport")){
					IcapServer.setProperty("http.proxyPort", values[1].trim());
					if (turnStdOff && Log.config()) Log.error(Level.CONFIG,"Setting HTTP proxy port as ["+values[1]+"]");
					else System.out.println("Setting HTTP proxy port as ["+values[1]+"]");
				} else if (values[0].equalsIgnoreCase("tcp_lowlatency")){
					if (values[1].equalsIgnoreCase("on")){
						if (turnStdOff && Log.config()) Log.error(Level.CONFIG,"TCP low latency activated.");
						else System.out.println("TCP low latency activated.");
						IcapServer.optimizeTCP = true;
					}
				}  else if (values[0].equalsIgnoreCase("admin.enabled")){
					if (values[1].equalsIgnoreCase("on")) {
						IcapServer.setWebAdmin(true, config);
					} else {
						IcapServer.setWebAdmin(false, config);
					}
				} /*else if (values[0].equalsIgnoreCase("lightspeed")){
					if (values[1].equalsIgnoreCase("on")){
						System.out.println("Tuning TCP stack");
						IcapServer.tuneServer = true;
					}
				}*/
			}//End while readLine
			in.close();       
		} catch (IOException e){
			System.err.println("Error in ICAP Server configuration file <"+config+">. File corrupted.");
			System.exit(-1);
		}//End try&catch
		//throw new Exception();
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	/**
	 * Start ICAP Server (if not already running)<br>
	 */
	public void enable(){
		if (running) return;
		String message = "Starting ICAP Server ["+this.serverName+"] in "+(mode==VectoringPoint.REQRESPMOD?"REQMOD and RESPMOD":mode.toString());
		if (!turnStdOff) System.out.println(message);
		Log.error(Log.INFO, message);
		running = true;
		this.start();
		message = "ICAP Server started on "+IP.getAddress().getHostAddress()+":"+IP.getPort();
		if (!turnStdOff) System.out.println(message);
		Log.error(Log.INFO, message);
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	/**
	 * Disable ICAP Server(if running)<br>
	 */
	public void disable(){
		if (!running) return;
		running = false;
		try {
			this.interrupt();
			serviceSocket.close();
		} catch (Exception e){
			//e.printStackTrace();
		}
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	/**
	 * Launch ICAP Server thread on ICAP port<br>
	 */
	public void run() {
		//ServerSocket sock = null;
		AbstractService serviceinstance;
		//double infinite loop to handle exceptions
		int criticalfailure =0;
		ConnectionsPool pool = null;
		while (running) {
			try {
				serviceSocket = new ServerSocket();
				//Set server performances priority to connection time, then latency, and last bandwidth
				if (optimizeTCP){
					serviceSocket.setPerformancePreferences(1,2,0); // optimize for latency
					serviceSocket.setReceiveBufferSize(16777216);
					serviceSocket.setReuseAddress(true);
				}
				/*if (lightspeed){
					try {
						SocketImplFactory factory = new jfs.net.FastSocketImplFactory();
						Socket.setSocketImplFactory(factory);
						ServerSocket.setSocketFactory(factory);
					} catch (Exception e){
						e.printStackTrace();
					}
				}*/
				serviceSocket.bind(IP,backlog);
				criticalfailure = 0;

				if (!keepalive) pool = new ConnectionsPool(this, poolSize,constructor);
				while (running) {
					try{
						Socket clientSocket = serviceSocket.accept();
						if (optimizeTCP && keepalive){
							clientSocket.setPerformancePreferences(0,2,1); // optimize for latency
							clientSocket.setKeepAlive(true); //optimization for keepalive sockets
							clientSocket.setSendBufferSize(131072);
							clientSocket.setTcpNoDelay(true); //disable tcp slow start
							clientSocket.setSoTimeout(ICAP_SO_TIMEOUT);//don't block more than given time
							clientSocket.setReuseAddress(true);
						}
						if (Log.finest()) Log.trace(Log.FINE,"New ICAP Connection received");
						if (!keepalive){
							pool.assignTask(clientSocket);
						} else {
							serviceinstance = constructor.newInstance(this, clientSocket);
							serviceinstance.start();
						}
					} catch (Exception e){
						//e.printStackTrace();
						if (running){
							Log.error(Log.SEVERE, "Failure while creating new socket processing instance : ", e);
						} 
					} catch (Throwable t){
						//t.printStackTrace();
						if (running){
							Log.error(Log.SEVERE, "Failure while creating new socket processing instance : ", t);
							System.gc();
						}
					}
				}//while true
			} catch (java.net.BindException e1){
				if (!turnStdOff) System.err.println("Error: Unable to open socket on ["+IP.getHostName()+":"+port+"]");
				if (!turnStdOff) System.err.println("Server halted.");
				Log.error(Log.SEVERE, "Error: Unable to open socket on ["+IP.getHostName()+":"+port+"]. Server Halted");
				System.exit(1);
			} catch (Exception e) {
				if (criticalfailure==0) Log.error(Log.SEVERE, "Critical Network failure. Trying to reinitialize server",e);
				try {if (serviceSocket!=null) serviceSocket.close();}catch (Exception e1){}
				System.gc();
				criticalfailure++;
				if (criticalfailure > 3) {
					Log.error(Log.SEVERE, "Error: Unable to open socket on ["+IP.getHostName()+":"+port+"]. Server Halted");
					System.exit(1);
				}
			} finally {
				try{
					if (serviceSocket!=null) serviceSocket.close();
					if (!keepalive && pool!=null) {
						pool.disable();
						pool = null;
					}
				} catch (Exception e1){
					//do nothing on cleanup error
				}
			}
		}//End while running
		Log.error(Log.SEVERE, this.serverName+ " halted");
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	protected static void registerShutdown(){
    // Add a shutdownHook to the JVM
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Log.error(Log.SEVERE, "ICAP Server halted.");
			}
    	});
	}
//	<--------------------------------------------------------------------------->
    
//	<--------------------------------------------------------------------------->   
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "true");
		if (args==null || args.length==0) {
			runningconfig = defaultconfig;
			readConf(defaultconfig);
			registerShutdown();
			return;
		}
		if (!turnStdOff) System.out.println("IcapServer version " + version);
		if (args!=null && args.length>0){
			if (args.length==1 && !args[0].contains("help") && !args[0].contains("?") && !args[0].startsWith("-")){
				try{
					runningconfig = args[0];
					readConf(args[0]);
					registerShutdown();
					return;
				} catch (Exception e){
					if (!turnStdOff) System.out.println("Invalid parameter: ["+args[0]+"]");
					Log.error(Log.SEVERE, "Invalid parameter: ["+args[0]+"]");
				}
			}
			//Special case to manage GS packages
			if (args.length>=2 && args[0].equals("-e")){
				icap.services.resources.gs.ExtensionManagement.loadExtensions();
				if (args[1].equals("list")){
					icap.services.resources.gs.ExtensionManagement.echoExtensionsList();
				} else if (args[1].equals("install") && args.length==3){
					if (!turnStdOff) System.out.print("Installing package "+args[2]+".....");
					else Log.error(Log.CONFIG,"Installing package "+args[2]+".....");
					if (icap.services.resources.gs.ExtensionManagement.installExtension(args[2])){
						if (!turnStdOff) System.out.println("done");
						else Log.error(Log.CONFIG, "done");
					} else {
						if (!turnStdOff) System.out.println("error - check if package ID is correctly spelled");
						else Log.error(Log.CONFIG, "error - check if package ID is correctly spelled");
					}
				} else if ( (args[1].equals("uninstall") || args[1].equals("remove") ) && args.length==3){
					if (!turnStdOff) System.out.print("Uninstalling package "+args[2]+".....");
					if (icap.services.resources.gs.ExtensionManagement.unInstallExtension(args[2])){
						if (!turnStdOff) System.out.println("done");
						else Log.error(Log.CONFIG, "done");
					} else {
						if (!turnStdOff) System.out.println("error removing package - check if package ID is correctly spelled");
						else Log.error(Log.CONFIG, "error removing package - check if package ID is correctly spelled");
					}
				} else {
					if (!turnStdOff) System.out.println("invalid command parameters");
					else Log.error(Log.CONFIG, "invalid command parameter: " +args[1]);
				}
				return;
			}
		}
		System.out.println("parameters:\r\n\t [configurationfile] \tuse given configuration file instead of default");
		System.out.println("\t -e list \tlist available language extensions");
		System.out.println("\t -e install \t[Package ID] => install given extension (requires root privilege)");
		System.out.println("\t -e uninstall \t[Package ID] => uninstall given extension (requires root privilege)");
		System.out.println("\t --help	\tthis help");
		System.exit(0);
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	/**
	 * Stop all services and restart application by reloading configuration file
	 */
	public static void restart(){
		if (Log.warning()) Log.error(Log.INFO, "Restarting ICAP server");
		//stop all servers
		for (IcapServer server:servers){
			try {
				server.disable();
				server.finalize();
			} catch (Exception e){
				if (Log.warning()) Log.error(Log.WARNING, "error stopping server " + server.serverName, e);
			} catch (Throwable t){
				if (Log.warning()) Log.error(Log.WARNING, "error stopping server " + server.serverName);
			}
		}
		boolean stopped = true;
		int counter = 0;
		do {
			counter++;
			try {
				Thread.sleep(100);
			} catch (Exception e){}
			for (IcapServer server:servers){
				if (server.isAlive()) stopped = false;
			}
		} while (!stopped && counter<1000);
		if (!stopped) {
			if (Log.warning()) Log.error(Log.SEVERE, "Error while restarting ICAP server - aborting");
			return;
		}
		
		for (IcapServer server:servers){
			try{
				Method cleanup = server.IcapService.getDeclaredMethod("cleanup");
				cleanup.invoke(null);
			} catch (Exception e){
				if (Log.warning()) Log.error(Log.SEVERE, "Unable to cleanup service configuration when restarting ICAP server");
				//cleanup error => do nothing
			}
		}
		servers.clear();
		ResourceBundle.clearCache();
		MimeMagic.reload();
		readConf(runningconfig);
		if (Log.warning()) Log.error(Log.INFO, "ICAP server restarted");
	}
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
	/**
	 * Stop all services and clean associated resources
	 */
	public static void stopServers(){
		if (Log.warning()) Log.error(Log.WARNING, "Halting ICAP server");
		//stop all servers
		for (IcapServer server:servers){
			try {
				server.disable();
				server.finalize();
			} catch (Exception e){
				if (Log.warning()) Log.error(Log.WARNING, "error halting server " + server.serverName, e);
			} catch (Throwable t){
				if (Log.warning()) Log.error(Log.WARNING, "error halting server " + server.serverName);
			}
		}
		boolean stopped = true;
		int counter = 0;
		do {
			counter++;
			try {
				Thread.sleep(100);
			} catch (Exception e){}
			for (IcapServer server:servers){
				if (server.isAlive()) stopped = false;
			}
		} while (!stopped && counter<1000);
		if (!stopped) {
			if (Log.warning()) Log.error(Log.SEVERE, "Error while halting ICAP server - aborting");
			return;
		}
		
		for (IcapServer server:servers){
			try{
				Method cleanup = server.IcapService.getDeclaredMethod("cleanup", (Class<?>)null);
				cleanup.invoke(new Object[]{null});
			} catch (Exception e){
				//cleanup error => do nothing
			}
		}
		servers.clear();
		ResourceBundle.clearCache();
	}
//	<--------------------------------------------------------------------------->
	
//	<--------------------------------------------------------------------------->
	/**
	 * @return the complete OPTIONS response as returned by server to ICAP client
	 */
	public String getOptions(){
		return this.OPTIONS;
	}

	/**
	 * @return true if server is using keep-alive connections with ICAP client (similar to HTTP 1.1)
	 */
	public boolean useKeepAliveConnections() {
		return keepalive;
	}

	/**
	 * @param keepalive set if server will use keep-alive connections with ICAP client (similar to HTTP 1.1)
	 */
	public void setKeepAliveConnections(boolean keepalive) {
		this.keepalive = keepalive;
	}
	/**
	 * Return ICAP ISTAG Header(see RFC 3507)<br>.
	 * Value is either generated from service name and ICAP server version, or loaded from service configuration file.
	 * @return ISTAG complete Header: [ISTAG: "istag_value"] (without trailing CRLF)   
	 */
	public String getISTAG(){
		return this.ISTAG;
	}
//	<--------------------------------------------------------------------------->
}
