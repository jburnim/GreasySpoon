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
package icap.services;

///////////////////////////////////
//Import
import icap.IcapServer;
import icap.core.*;
import icap.services.resources.gs.SpoonScript;
import icap.services.resources.gs.SpoonScriptException;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import tools.logger.Log;
import tools.general.ClassPathLoader;
import tools.general.MimeMagic;
import tools.general.ExtendedByteArrayOutputStream;
///////////////////////////////////

/**
 * <p>
 * GreasySpoon service for both Request and Response mode<br>
 * <p>
 * Insert given content into HTML / WML / XHTML responses.<br>
 * Service implements a inflate/deflate function in order to uncompress optimized content.<br>
 * All responses are therefore compressed if client support it (Accept-encoding: gzip).<br>  
 * ICAP Protocol methods are provided by class AbstractService<br>
 * @author mittig
 */
public class GreasySpoon extends AbstractService {

	//-------------ICAP parser parameters-------------------
	/**Human readable name for service, used mainly for logs*/
	static String servicename = ServicesProperties.getString("GreasySpoon.servicename"); 
	//------------------------------------------------------
	StringBuilder logstr = new StringBuilder();

	/** Set if MIME magic must be used to control server responses MIME types */
	private static boolean mimemagiccheck = false;
	private static boolean trustServiceMimeTypePerDefault = true;

	private static boolean initialized = false;

	/**Define HTTP response mime types supported by the service (in RESPMOD) */
	private static String[] supportedContentTypes;

	/**Define HTTP response codes supported by the service (in RESPMOD) */
	//static int[] supportedResponseCodes = null;


	/**
	 * @return Returns the mimemagiccheck.
	 */
	public static boolean isMimemagiccheck() {
		return mimemagiccheck;
	}

	/**
	 * @return Returns the trustServiceMimeTypePerDefault.
	 */
	public static boolean isTrustServiceMimeTypePerDefault() {
		return trustServiceMimeTypePerDefault;
	}

	/**
	 * @return Returns the supportedContentTypes.
	 */
	public static String[] getSupportedContentTypes() {
		return supportedContentTypes;
	}

	/**
	 * @return Returns the compressanytime.
	 */
	public static boolean isCompressanytime() {
		return compressanytime;
	}

	/**Tag use to name/filter request mode scripts*/
	public static String greasySpoonReqTag; 
	/**Tag use to name/filter response mode scripts*/
	public static String greasySpoonRespTag; 
	/**Return all the file extensions supported by the service (format: <$language><$greasySpoonReqTag|$greasySpoonRespTag>*/
	public static String[] scriptsExtensions;

	/**Directory containing GeeasySpoon scripts (must be relative to application path)*/
	public static String scriptsDirectory = "serverscripts"; 
	/**Directory containing GeeasySpoon configuration (must be relative to application path)*/
	public static String confDirectory = "conf"; 
	protected static long scriptslistdate;

	/** Property file defining specific comments tags for associated languages (default://) */
	public static Properties languageComments = new Properties();
	private static String languageCommentsFile; 
	private static boolean compressanytime = false;
	static String[] compressibleContentTypes = new String[]{"text/", "application/xml", "application/x-javascript", "application/json"};

	/**
	 * @return Returns the compressibleContentTypes.
	 */
	public static String[] getCompressibleContentTypes() {
		return compressibleContentTypes;
	}

	/** GreasySpoon REQUEST scripts managed by the GreasySpoon service */
	public static SpoonScript[] reqSpoonScripts = new SpoonScript[0];
	/** GreasySpoon RESPONSE scripts managed by the GreasySpoon service */
	public static SpoonScript[] respSpoonScripts = new SpoonScript[0];

	/**Threaded timer used to monitor scripts directory and call reload method in case of changes*/
	//static Timer timer = new Timer();

	/** Set if scripts in error must be bypassed or if GreasySpoon will abort processing and reply with an error*/
	public static boolean bypassOnError = true;


	//	<------------------------------------------------------------------------->        
	/**
	 * Create a service thread to proceed server connection
	 * @param icapserver ICAP server managing the service
	 * @param clientsocket	ICAP client socket
	 */
	public GreasySpoon(IcapServer icapserver, Socket clientsocket){
		super(icapserver, clientsocket);
		this.server = icapserver;
		if (clientsocket!=null) this.setSocket(clientsocket);
		try{
			initialized = initialize();
			createTimerTask();
		} catch (Exception e){
			System.err.println("GreasySpoon initialisation failure - check if language engines are correctly installed");
			e.printStackTrace();
			System.exit(1);
		}

	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/** set class status to uninitialized*/
	public static void cleanup(){
		initialized = false;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Return ICAP vectoring points supported by this service
	 * @see icap.core.AbstractService#getSupportedModes()
	 */
	public VectoringPoint getSupportedModes(){
		return VectoringPoint.REQRESPMOD;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Parse parameters from configuration files
	 */
	private synchronized boolean initialize(){
		if (initialized) return initialized;
		ServicesProperties.refresh();
		SpoonScript.init();
		supportedContentTypes = ServicesProperties.getString("GreasySpoon.mimesupported").split("\\s+");
		greasySpoonReqTag = ServicesProperties.getString("GreasySpoon.reqtag"); 
		greasySpoonRespTag = ServicesProperties.getString("GreasySpoon.resptag");
		compressanytime = ServicesProperties.getString("GreasySpoon.compress")!=null && ServicesProperties.getBooleanValue("GreasySpoon.compress");
		if (ServicesProperties.getString("GreasySpoon.compressibleContentTypes") != null) compressibleContentTypes = ServicesProperties.getString("GreasySpoon.compressibleContentTypes").split("\\s+");
		mimemagiccheck = ServicesProperties.getString("GreasySpoon.mimemagic")!=null && ServicesProperties.getBooleanValue("GreasySpoon.mimemagic");
		if (mimemagiccheck){
			if (Log.config()) Log.error(Log.CONFIG,"GreasySpoon: MimeMagic enabled");
		}
		scriptsExtensions = getExtensions();

		languageCommentsFile = GreasySpoon.confDirectory+File.separator+ServicesProperties.getString("GreasySpoon.comments"); 
		try{
			languageComments.clear();
			languageComments.load(new FileInputStream(languageCommentsFile));
		} catch (Exception e){
			if (Log.info()) Log.error(Log.INFO,ServicesProperties.getString("GreasySpoon.err1")+languageCommentsFile);
		} 
		scriptsDirectory = this.server.serviceconfig.getProperty("scriptsDirectory",scriptsDirectory); 
		confDirectory = this.server.serviceconfig.getProperty("confDirectory",confDirectory);

		String libsPath = scriptsDirectory+File.separator+";"+this.server.serviceconfig.getProperty("path","");
		for (String s:ServicesProperties.stringPropertyNames()){
			if (s.startsWith("path.")){
				libsPath = libsPath+";"+ServicesProperties.getString(s,"");
			}
		}
		ClassPathLoader.addPath(libsPath);
		libsPath = "";
		for (String s:ServicesProperties.stringPropertyNames()){
			if (s.startsWith("props.")){
				libsPath = libsPath+";"+ServicesProperties.getString(s,"");
			}
		}
		ClassPathLoader.setProperties(libsPath);

		//Scripts execution parameters
		SpoonScript.setScriptMaxTimeout(Integer.parseInt(this.server.serviceconfig.getProperty("maxtimeout", ""+SpoonScript.getScriptMaxTimeout())));
		SpoonScript.setErrorThreshold(Integer.parseInt(this.server.serviceconfig.getProperty("errorthreshold", "0")));
		bypassOnError = Boolean.parseBoolean(this.server.serviceconfig.getProperty("bypassonerror", "true"));
		SpoonScript.path = libsPath;
		forceReload();
		return true;
	}
	//<------------------------------------------------------------------------->

	//<------------------------------------------------------------------------->
	private static boolean _reload_pending = false;
	/**
	 * Force Scripts reloading => Parse script directory and reload ALL scripts
	 * and content.
	 */
	public static synchronized void forceReload(){
		_reload_pending = true;
		Vector<SpoonScript> tmpreqvector = new Vector<SpoonScript>();//use a vector copy to avoid concurrent accesses
		Vector<SpoonScript> tmprespvector = new Vector<SpoonScript>();//use a vector copy to avoid concurrent accesses
		String[] scriptslist = getFilesList();
		String path = getApplicationPath()+scriptsDirectory+File.separator;
		for (String s:scriptslist){
			SpoonScript sps;
			if (s.indexOf(greasySpoonReqTag)>0){
				sps = SpoonScript.loadFromFile(path+s,Icap.TYPE.REQMOD);
				tmpreqvector.add(sps);
			} else {
				sps = SpoonScript.loadFromFile(path+s,Icap.TYPE.RESPMOD);
				tmprespvector.add(sps);
			}
		}
		reqSpoonScripts = orderScripts(tmpreqvector);
		respSpoonScripts = orderScripts(tmprespvector);
		initialized = true;
		_reload_pending = false;
	}
	//<------------------------------------------------------------------------->

	/**
	 * Scripts reloading => Parse script directory and update modified/new scripts
	 */
	public static synchronized void reload(){
		if (_reload_pending) return;
		_reload_pending = true;
		Vector<SpoonScript> tmpreqvector = new Vector<SpoonScript>();//use a vector copy to avoid concurrent accesses
		Vector<SpoonScript> tmprespvector = new Vector<SpoonScript>();//use a vector copy to avoid concurrent accesses
		String[] scriptslist = getFilesList();
		for (String s:scriptslist){
			SpoonScript sps = null;

			if (s.indexOf(greasySpoonReqTag)>0){
				sps = getLastScriptVersionFrom(reqSpoonScripts,s,Icap.TYPE.REQMOD);
				tmpreqvector.add(sps);
			} else {
				sps = getLastScriptVersionFrom(respSpoonScripts,s,Icap.TYPE.RESPMOD);
				tmprespvector.add(sps);
			}
		}
		reqSpoonScripts = orderScripts(tmpreqvector);
		respSpoonScripts = orderScripts(tmprespvector);
		initialized = true;
		_reload_pending = false;
	}
	//<------------------------------------------------------------------------->

	//<------------------------------------------------------------------------->
	private static synchronized SpoonScript getLastScriptVersionFrom(SpoonScript[] scripts, String s,Icap.TYPE type){
		String path = getApplicationPath()+scriptsDirectory+File.separator;
		s = path+s;
		File f = new File(s);
		for (SpoonScript existingsp:scripts){
			if (existingsp.getFile().equals(f)){
				if (!existingsp.isModified()) {
					return existingsp;
				}
				break;
			}
		}
		SpoonScript sps = SpoonScript.loadFromFile(s,type);
		return sps;
	}
	//<------------------------------------------------------------------------->


	//<------------------------------------------------------------------------->
	private static synchronized SpoonScript[] orderScripts(Vector<SpoonScript> newscripts){
		for (int i=0; i<newscripts.size()-1; i++){
			for (int j=i;j<newscripts.size();j++){
				if (newscripts.elementAt(j).getOrder()<newscripts.elementAt(i).getOrder()){
					SpoonScript tmp = newscripts.elementAt(i);
					newscripts.set(i,newscripts.elementAt(j));
					newscripts.set(j,tmp);
				}
			}
		}
		return newscripts.toArray(new SpoonScript[0]);
	}
	//<------------------------------------------------------------------------->

	//<------------------------------------------------------------------------->
	/**
	 * Reorder scripts regarding their order preference.
	 * Don't work directly on the vector to avoid sync issues
	 */
	public static synchronized void reorder(){
		if (_reload_pending) return;
		Vector<SpoonScript> tmpreqvector = new Vector<SpoonScript>();//use a vector copy to avoid concurrent accesses
		Vector<SpoonScript> tmprespvector = new Vector<SpoonScript>();//use a vector copy to avoid concurrent accesses

		for (SpoonScript sps:GreasySpoon.reqSpoonScripts){
			tmpreqvector.add(sps);
		}
		reqSpoonScripts = orderScripts(tmpreqvector);

		for (SpoonScript sps:GreasySpoon.respSpoonScripts){
			tmprespvector.add(sps);
		}
		respSpoonScripts = orderScripts(tmprespvector);
	}
	//<------------------------------------------------------------------------->

	//<------------------------------------------------------------------------->
	/**
	 * Create a timer to monitor script directory and reload scripts in case 
	 * of changes 
	 */
	private static boolean _timer_initialized = false;
	private synchronized void createTimerTask(){
		if (_timer_initialized) return;
		new CleanerThread();

	}  
	//<------------------------------------------------------------------------->

	//<------------------------------------------------------------------------->
	/**
	 * Simple thread that check script directory and update it if changes are
	 * detected. 
	 */
	public static class CleanerThread extends Thread{

		/**
		 * Internal Thread used to survey scripts directory and reload it
		 * if changes are detected. 
		 */
		public CleanerThread(){
			super("ScriptWatcher");
			this.start();
		}
		/**
		 * @see java.lang.Thread#run()
		 */
		@SuppressWarnings("static-access") 
		public void run(){
			try{
				File f_directory = new File(getApplicationPath()+scriptsDirectory);
				_timer_initialized = true;
				while (_timer_initialized){
					try{
						//Don't use refreshScripts() method because of lock issues
						if (scriptslistdate != f_directory.lastModified()){
							reload();
						}
						this.sleep(1000);
					} catch (InterruptedException e){
						//on interruption, just continue
					}
				}
			}catch (Exception e1){
				Log.error(Log.WARNING, "GreasySpoon script watcher interruption", e1);
			}
			_timer_initialized = false;
		}
	} 
	//<------------------------------------------------------------------------->


	//	<------------------------------------------------------------------------->
	/**
	 * @see icap.core.AbstractService#getDescription()
	 */
	public String getDescription(){
		return ServicesProperties.getString("GreasySpoon.description"); 
	}
	//	<------------------------------------------------------------------------->


	//	<------------------------------------------------------------------------->
	/**
	 * Main AbstractService method implementation
	 * ICAP request has been parsed => generate response
	 * @see icap.core.AbstractService#getResponse(java.io.ByteArrayOutputStream)
	 */
	public int getResponse(ByteArrayOutputStream bas)  {
		if (Log.isEnable()) logstr.setLength(0);
		bas.reset();
		long timing = System.nanoTime();
		int returncode=-1;
		try{
			switch (this.getType()){
			case REQMOD: 
				if (Log.isEnable()) this.logstr.append("HTTP/").append(this.httpmethod).append(" ").append(this.getAbsolutePath());
				returncode = getReqmodResponse(bas);
				break;
			case RESPMOD: 
				if (Log.isEnable()) logstr.append("HTTP/").append(this.rescode).append(" ").append(this.getAbsolutePath());
				returncode = getRespModResponse(bas);
				break;
			default: break;
			}
			if (returncode!=-1){
				timing = (System.nanoTime()-timing)/1000000;
				if (Log.isEnable()) Log.access(logstr.insert(0,String.format("%1$-4s [%2$-7s] [%3$-10s] ICAP/%4$-3s ", timing, type.toString(), servicename, returncode)));
				return returncode;
			}
		} catch (SpoonScriptException spe){
			try{
				bas.reset();
				bas.write(_504SERVERERROR);
			} catch (Exception e){
				//even error return failed => do nothing more
			}
			timing = (System.nanoTime()-timing)/1000000;
			if (Log.isEnable()) Log.access(logstr.insert(0,String.format("%1$-4s [%2$-7s] [%3$-10s] ICAP/%4$-3s ", timing, type.toString(), servicename, 504)));
			return 504;
			
		} catch (java.io.EOFException eof){
			try{
				bas.reset();
				bas.write(_400CLIENTERROR);
			} catch (Exception e){
				//even error return failed => do nothing more
			}
			timing = (System.nanoTime()-timing)/1000000;
			if (Log.isEnable()) Log.access(logstr.insert(0,String.format("%1$-4s [%2$-7s] [%3$-10s] ICAP/%4$-3s ", timing, type.toString(), servicename, 400)));
			return 400;
		} catch (IcapParserException ipe){
			try{
				bas.reset();
				bas.write(_400CLIENTERROR);
			} catch (Exception e){
				//even error return failed => do nothing more
			}
			timing = (System.nanoTime()-timing)/1000000;
			if (Log.isEnable()) Log.access(logstr.insert(0,String.format("%1$-4s [%2$-7s] [%3$-10s] ICAP/%4$-3s ", timing, type.toString(), servicename, 400)));
			return 400;
		} catch (Exception e){
			Log.error(Log.INFO,"Greasyspoon Exception while applying scripts - ", e);
		}  catch (Throwable e){
			bas.reset();
			System.gc();
			Log.error(Log.WARNING,"Greasyspoon failure while applying scripts: - ", e);
		}
		//bug: return a 500 error code
		try{
			bas.reset();
			bas.write(_500SERVERERROR);
		} catch (Exception e){
			//even error return failed => do nothing more
		}
		timing = (System.nanoTime()-timing)/1000000;
		if (Log.isEnable()) Log.access(logstr.insert(0,String.format("%1$-4s [%2$-7s] [%3$-10s] ICAP/%4$-3s ", timing, type.toString(), servicename, 500)));
		return 500;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->   
	/**
	 * Check if provided mime type is supported by current greasyspoon configuration
	 * @param contenttype The content type associated to the body
	 * @param mimeTypesToCheck mime types associated to GS engine
	 * @return true if content type is declared as supported
	 */
	public static boolean isMimeTypeSupported(String contenttype,String[] mimeTypesToCheck){
		boolean ismimesupported = false;
		for (String ct:mimeTypesToCheck){
			if (ct.equals("*")) return true;
			if (contenttype.contains(ct)){
				ismimesupported = true;
				break;
			}
		}
		return ismimesupported;
	} 
	//	<------------------------------------------------------------------------->   

	//	<------------------------------------------------------------------------->   
	/**
	 * AbstractRespmodeService implementation
	 * ICAP request has been parsed: generate a response for RESP MODE
	 * @param bas The stream in which response will be provided
	 * @return	ICAP response code
	 * @throws Exception 
	 */
	public synchronized int getRespModResponse(ByteArrayOutputStream bas) throws Exception {

		String contenttype = this.getRespHeader("content-type"); 
		if (contenttype!=null) {
			contenttype = contenttype.toLowerCase();
		} else {
			if (this.getContentLength()==0){
				contenttype = "text/html";//no body provided: set response to HTML by default
			} else {
				contenttype = "application/octet-stream";//RFC 2616: if no content type is provided, consider it as octet stream
			}
		}
		if (Log.finest()) Log.trace(Log.FINEST,"Content-type = "+contenttype); 
		if (Log.isEnable()) logstr.append(" [").append(contenttype).append("]"); 
		//----------------------------------------------------------
		if (contenttype==null){//this case should never occur
			if (Log.isEnable()) logstr.append(" [no mime-type]");
			return earlyResponse(bas);
		}
		//Check supported content types => skip services and return 204
		if (!isMimeTypeSupported(contenttype, supportedContentTypes)){
			if (Log.isEnable()) logstr.append(" [unsupported mime-type]");
			return earlyResponse(bas);
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		// Check if there are applicable scripts
		Vector<SpoonScript> scriptsToApply = new Vector<SpoonScript>();
		for (SpoonScript sps:respSpoonScripts){
			if (sps.mode != this.type) continue;
			//if (sps.getStatus() && sps.isApplicable(this.getAbsolutePath().toLowerCase(),this.rescode)) {
			if (sps.getStatus() && sps.isApplicable(this.getReqUrl().toLowerCase(),this.rescode)) {
				scriptsToApply.add(sps);
			}
		}
		// In case no script is applicable, return a 204 (abort response processing)  
		if (scriptsToApply.size()==0) {
			if (Log.isEnable()) logstr.append(" [no matching scripts]");
			return earlyResponse(bas);
		}
		//----------------------------------------------------------

		String mimemagic = null;

		//----------------------------------------------------------
		//If preview mode is activated, use it to check content type using MIME Magic
		if (this.preview>0 && mimemagiccheck 
				&& resBody!=null && resBody.size()>MimeMagic.getMagicBytesSize()
				&& !isCompressed() ){
			mimemagic = MimeMagic.detectMime(contenttype, this.req_url_path, this.resBody,trustServiceMimeTypePerDefault);
			//Check supported content types => skip srv and return 204
			if (!contenttype.startsWith(mimemagic)){
				if (!isMimeTypeSupported(mimemagic, supportedContentTypes)){ 
					if (Log.isEnable()) logstr.append(" [mimemagic<>").append(mimemagic).append("][unsupported mime-type]");
					return earlyResponse(bas);
				}
			}
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		//If here, there are actions to perform
		//Retrieve the body
		this.getAllBody();
		//----------------------------------------------------------

		boolean bodyavailable = true;
		if (resBody==null) bodyavailable = false;
		//----------------------------------------------------------
		// Deflate the body if gzipped
		boolean initiallyGzipped = isCompressed();
		if (bodyavailable){
			if ( initiallyGzipped ) resBody = uncompress(resBody);
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		//Double MIME type check using MIME Magic
		if (mimemagic==null && mimemagiccheck && bodyavailable && (resBody.size()>100 || !resBody.toString().trim().equals("")) ){
			mimemagic = MimeMagic.detectMime(contenttype, this.req_url_path, this.resBody,trustServiceMimeTypePerDefault);
			//Check supported content types => skip srv and return 204
			if (!contenttype.startsWith(mimemagic)){
				/* Uncomment next lines if you want to update provided MIME type with magic */
				/*
				String charset = MimeMagic.getEncodingFromContentType(contenttype);
				contenttype = mimemagic;
				if (charset!=null) contenttype+="; charset="+charset;
				this.updateContentType(contenttype);
				*/
				if (!isMimeTypeSupported(mimemagic, supportedContentTypes)){ //send unmodified response in 200
					if (Log.isEnable()) logstr.append(" [mimemagic<>").append(mimemagic).append("][unsupported mime-type]");
					if (initiallyGzipped) this.resBody = compress(this.resBody);
					return earlyResponse(bas);
				}
			}
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		// GreasySpoon scripts must be inserted => lets do it
		String content, encoding;

		try{//detect encoding for current content
			encoding = this.getEncoding(contenttype);
			if (Log.isEnable()) logstr.append(" [encoding/").append(encoding).append("]");
			//Parse content as String with good(?) encoding
			content = bodyavailable?(new String (this.resBody.toByteArray(),encoding)):("");
		} catch (Exception e){
			if (Log.isEnable()) logstr.append(" [unknown encoding]");
			if (initiallyGzipped) this.resBody = compress(this.resBody);
			return fullResponse(bas);
		}

		//computes hash for content in order to detect modifications
		int intitialcontenthash = content==null?0:content.hashCode();
		content = applyScripts(logstr, this, content, this.getReqUrl(), scriptsToApply);
		//----------------------------------------------------------

		//----------------------------------------------------------
		// check if content has been modified - if yes, update it 
		if (intitialcontenthash != (content==null?0:content.hashCode())){
			if (!bodyavailable && content!=null && content.length()!=0) {
				bodyavailable = true;
				this.resBody = new ExtendedByteArrayOutputStream();
			}

			if (bodyavailable && content!=null) {
				this.resBody.reset();//clear old body

				try{//detect encoding for output content
					String ct = this.getResponseHeaders().toLowerCase(); 
					int pos1 =  ct.indexOf("content-type: ");
					if (pos1!=-1) {
						pos1+="content-type: ".length();
						ct = ct.substring(pos1, ct.indexOf("\r\n",pos1));
					} else {
						ct = contenttype;
					}
					String encoding2 = this.getEncoding(ct, content);
					if (encoding2!=null && !encoding2.equals(encoding)) {
						encoding = encoding2;
						if (Log.isEnable()) logstr.append(" [+oe/").append(encoding).append("]");
					}
				} catch (Exception e){}
				
				if (encoding!=null) {
					this.resBody.write(content.getBytes(encoding));
				} else {
					this.resBody.write(content.getBytes());	
				}
			}
		} else { //reset content to free memory
			content = null;
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		//compress response if browser supports it
		if ( bodyavailable ){
			if ( initiallyGzipped ) {
				long timing = System.currentTimeMillis();
				this.resBody = compress(this.resBody);
				if (Log.isEnable()) this.logstr.append(" [gzip:"+(System.currentTimeMillis()-timing)+"]"); 
			} else if ( compressanytime ){
				if (isMimeTypeSupported(mimemagic!=null?mimemagic:contenttype, compressibleContentTypes)){
					long timing = System.currentTimeMillis();
					this.resBody = compress(this.resBody);
					if (Log.isEnable()) this.logstr.append(" [gzip-opt:"+(System.currentTimeMillis()-timing)+"]"); 
				}
			}
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		//Generate Response
		updateContentLength(bodyavailable?this.resBody.size():0);
		//All done => return response
		return fullResponse(bas);
		//----------------------------------------------------------
	}
	//	<------------------------------------------------------------------------->


	//	<------------------------------------------------------------------------->   
	/**
	 * AbstractRespmodeService implementation
	 * ICAP request has been parsed: generate a response for REQMOD
	 * @param bas The stream in which response will be provided
	 * @return	ICAP response code
	 * @throws Exception 
	 */
	public synchronized int getReqmodResponse(ByteArrayOutputStream bas) throws Exception {
		//----------------------------------------------------------
		// Check if there is applicable scripts
		Vector<SpoonScript> scriptsToApply = new Vector<SpoonScript>();
		for (SpoonScript sps:reqSpoonScripts){
			if (sps.mode != this.type) continue;
			if (sps.getStatus() && sps.isApplicable(this.getReqUrl().toLowerCase())) {
				scriptsToApply.add(sps);
			}
		}
		// In case no script is applicable, return a 204 (abort response processing)  
		if (scriptsToApply.size()==0) {
			if (Log.isEnable()) logstr.append(" [no scripts to apply]");
			if (this.brand==ClientBrand.NETAPP) {//204 not supported by netcache ==> generates 200 (works similar way to 204, no body needed)
				StringBuilder sb = new StringBuilder();
				sb.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF); 
				//add ICAP host, then HTTP header at position zero (constant)
				sb.append(server.icaphost).append("Encapsulated: ").append(this.i_encapsulated).append(CRLF); 
				sb.append("Cache-Control: no-cache").append(CRLF).append(CRLF); 
				sb.append(this.reqHeader);
				bas.write(sb.toString().getBytes());
				return 200;
			}
			// For ICAP clients supporting 204 in REQMOD, return 204
			return earlyResponse(bas);
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		//If here, there are actions to perform
		//Retrieve the body
		boolean containsBody = false;
		if (i_req_body>0){
			containsBody = this.getAllBody();
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		// Deflate the body if compressed
		boolean initiallyGzipped = isCompressed();
		if (containsBody){ 
			if (initiallyGzipped) reqBody = uncompress(reqBody);
		}
		//----------------------------------------------------------

		//----------------------------------------------------------
		// GreasySpoon scripts must be inserted => lets do it
		//----------------------------------------------------------
		String content=null;
		if (containsBody) {
			content = reqBody.toString();
		}

		//store initial content hash 
		int intitialcontenthash = content==null?0:content.hashCode();
		content = applyScripts(logstr, this, content, this.getReqUrl(), scriptsToApply);


		//----------------------------------------------------------
		// Update request body only if it has been modified by scripts
		// Avoid unnecessary manipulation, and also possible encoding issues
		//----------------------------------------------------------
		if (intitialcontenthash != (content==null?0:content.hashCode()) ){ // content has been changed by scripts
			if (content!=null && content.length()==0) content = null;
			reqBody.reset();//clear old body

			if (content!=null) {
				if (reqBody==null) reqBody=new ExtendedByteArrayOutputStream();
				reqBody.write(content.getBytes());	
			}
		}
		//----------------------------------------------------------

		//recompress request if was compressed initially
		if ( initiallyGzipped){ 
			this.reqBody = compress(reqBody);
			if (Log.isEnable()) logstr.append(" [gzip]"); 
		}
		//----------------------------------------------------------

		//Update length header with the new content length
		updateContentLength(content==null?0:reqBody.size());


		if (!reqHeader.subSequence(0, 5).equals("HTTP/")){
			return fullResponse(bas);
		}

		//HTTP Request has been changed into HTTP Response (starts with HTTP/xxx instead of GET/POST/....)
		// Let's create ICAP response
		StringBuilder sb = new StringBuilder();
		sb.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF);//.append("Server: ICAP-Server-Software/1.0\r\n"); 
		sb.append(server.icaphost).append("Encapsulated: res-hdr=0"); 
		if (reqBody!=null && reqBody.size()>0){
			sb.append(", res-body="); //If body available, set tag req-body 
		} else {
			sb.append(", null-body="); //else null-body 
		}
		sb.append(reqHeader.length()).append(CRLF);
		//Set if connection is persistent
		if (server.useKeepAliveConnections()) {
			sb.append(HEAD_CONNECTION_KEEPALIVE);
		} else {
			sb.append(HEAD_CONNECTION_CLOSED);
			this.closeConnection();
		}
		//End of ICAP header
		sb.append(CRLF);

		//Add the complete HTTP header 
		sb.append(reqHeader);

		bas.write(sb.toString().getBytes());

		//If a body is available, add it using chunk
		if (reqBody!=null && reqBody.size()>0) {
			bas.write((Integer.toHexString(reqBody.size())+CRLF).getBytes());
			reqBody.writeTo(bas);
			bas.write(CRLF_b);
			bas.write(ENDCHUNK);
		}
		//All done => return response
		return 200;
	}
	//	<------------------------------------------------------------------------->




	//	<------------------------------------------------------------------------->
	/**
	 * Call scripts to apply on given content/context
	 * @param logstr			Log string that will be enriched by called services
	 * @param serverThread		The service thread handling the ICAP request
	 * @param originalContent	The original content of the request/response (request in REQMOD, response in RESPMOD) 
	 * @param url				The complete requested URL (http://fqdn/path?parameters)
	 * @param scriptsToApply	The list of scripts to call on the provided request/response
	 * @return					The request/response content potentially modified by script(s)
	 * @throws Exception		In case of script error without Error bypass activated
	 */
	public static String applyScripts(StringBuilder logstr, AbstractService serverThread, String originalContent, String url, Vector<SpoonScript> scriptsToApply) throws Exception{
		if (scriptsToApply.size()==0) return originalContent;
		String newcontent = originalContent==null ? null: new String (originalContent);
		long timing;
		boolean error = false;
		for (SpoonScript sps:scriptsToApply){
			timing = System.nanoTime();
			try {
				newcontent = sps.apply(serverThread, newcontent, url);
			} catch (SpoonScriptException e) {
				error = true;
			}
			timing = (System.nanoTime()-timing)/1000000;
			if (error && !GreasySpoon.bypassOnError){
				//script error and no error bypass
				logstr.append(" [").append(sps.getScriptName()).append(":ERROR:").append(timing).append("]");
				throw new SpoonScriptException("GreasySpoon: script error with no error bypass allowed");
			}
			Log.service(Log.INFO, String.format("%1$-20s %2$-6d %3$s", sps.getScriptName(),timing, url));
			logstr.append(" [").append(sps.getScriptName()).append(":").append(timing).append("]");
		}
		return newcontent;
	}
	//	<------------------------------------------------------------------------->




	///////////////////////////
	///////////////////////////
	//	TOOLS Methods
	///////////////////////////
	///////////////////////////

	//	---------------------------------------------------------------------------
	/**
	 * Give the application absolute path 
	 * @return String the application absolute path
	 */
	protected final static String getApplicationPath(){
		String AppPath = new String();
		File current = new File("."); // set to the relative path 
		AppPath = current.getAbsolutePath(); // get the real path
		AppPath = AppPath.substring(0,AppPath.length()-2); // remove the '\.'
		AppPath = AppPath + File.separator;
		return AppPath;
	}// End getApplicationPath
	//	---------------------------------------------------------------------------

	//	---------------------------------------------------------------------------
	/**
	 * @return files list contained in <directory>
	 */
	private static String[] getFilesList(){
		//open given directory
		File f_directory = new File(getApplicationPath()+scriptsDirectory);
		scriptslistdate = f_directory.lastModified();
		//retrieve all files matching accept method
		String[] filelist = f_directory.list();
		Vector<String> acceptedfiles = new Vector<String>(); 
		for (String s:filelist){
			if (accept(s)) acceptedfiles.add(s);
		}
		return acceptedfiles.toArray(new String[0]);
	}
	//	---------------------------------------------------------------------------

	//	---------------------------------------------------------------------------
	/**
	 * Filter profiles directory to retain only profiles files
	 * @param name file name to check
	 * @return true if the file name ends with $fileExtension, false otherwise
	 */
	private static boolean accept(String name){
		for (String s:scriptsExtensions){
			if (name.endsWith(s)) return true;
		}
		return false;
	}
	//	---------------------------------------------------------------------------

	//  <------------------------------------------------------------------------------------------>
	/**
	 * @return List of GS script compatible file extensions
	 */
	private static String[] getExtensions(){
		Vector<String> vector = new Vector<String>();
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		for (ScriptEngineFactory factories : scriptEngineManager.getEngineFactories()){
			vector.add(greasySpoonReqTag+factories.getExtensions().get(0));
			vector.add(greasySpoonRespTag+factories.getExtensions().get(0));
		}
		//Add GS native
		if (javax.tools.ToolProvider.getSystemJavaCompiler()!=null){
			vector.add(greasySpoonReqTag+"java");
			vector.add(greasySpoonRespTag+"java");
		}
		return vector.toArray(new String[0]);
	}
	//  <------------------------------------------------------------------------------------------>

}
