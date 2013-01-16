/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008,2009 Karel Mittig
 *-----------------------------------------------------------------------------
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *-----------------------------------------------------------------------------
 * For any comment, question, suggestion, bugfix or code contribution please
 * contact Karel Mittig : karel [dot] mittig [at] gmail [dot] com
 * Created on 20 June 2009
 *-----------------------------------------------------------------------------*/
package icap.services.resources.gs;

////////////////////////////////
//Import
import icap.services.ServicesProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.*;
////////////////////////////////

import tools.logger.Log;

/**
 * Test class, used to call written scripts in order to check basic syntax errors
 * Implemented in 2 ways: one for JSR223, and one for java classes
 * Called after each script modification, return ok or detected error
 * NOTE: only code portions that are really called can be checked in that way 
 */
public class ScriptChecker {
	
	private static final String BUNDLE_NAME = "scriptchecker";
	static Properties properties = new Properties();
	static private String configurationFilename = "conf"+File.separator+BUNDLE_NAME+".properties";
//  <------------------------------------------------------------------------->
	static {
		try {
			File f= new File(configurationFilename);
			if (!f.exists()) f.createNewFile();
			properties.load(new FileInputStream(configurationFilename));
		} catch (Exception e){
			Log.error(Log.SEVERE,"Error loading configuration file:"+configurationFilename,e);
		}
	}
//  <------------------------------------------------------------------------->
	
	/**Set if scripts must be tested on loading and saving*/
	protected static boolean testScriptOnChange = ScriptChecker.getBooleanValue("checker.enable");
	
	/**Defines scripts test execution threshold (in milliseconds). default: 1000(1 second)*/
	protected static int scriptTestTimeOut 	= Integer.parseInt(ScriptChecker.getString("checker.timeout","1000").trim());
	

	
	private static String urltag;
	private static String requestheadertag;
	private static String requestbodytag; 
	private static String responseheadertag; 
	private static String responsebodytag; 
	private static String useridtag; 
	private static String usergrouptag; 
	private static String cachetag; 
	private static String debugstring;
	
	//Test parameters

	String username		= ScriptChecker.getString("checker.userid","");
	String usergroup	= ScriptChecker.getString("checker.usergroup","");
	ConcurrentHashMap<String, Object> sharedCache = new ConcurrentHashMap<String, Object>();
	
	String requestHeaders  	= ScriptChecker.getString("checker.requestheader","");
	String requestBody		= ScriptChecker.getString("checker.requestbody","");
	
	String responseHeaders 	=  ScriptChecker.getString("checker.responseheader","");
	String responseBody 	= ScriptChecker.getString("checker.responsebody","");
	String urlcontent 		= ScriptChecker.getString("checker.url","");
	
	/** fake httpMessage to test java native scripts*/
	public HttpMessage httpMessage;
	/** fake bindings to test JSR223 scripts*/
	public Bindings bindings;
	
//	<------------------------------------------------------------------------->
	/**
	 * Check parameters names from configuration
	 */
	private static void init(){
		testScriptOnChange = ScriptChecker.getBooleanValue("checker.enable");
		scriptTestTimeOut  = Integer.parseInt(ScriptChecker.getString("checker.timeout","1000").trim());
		urltag = ServicesProperties.getString("SpoonScript.urlparam","requestedurl");
		requestheadertag = ServicesProperties.getString("SpoonScript.reqheadparam"); 
		requestbodytag = ServicesProperties.getString("SpoonScript.reqbodyparam"); 
		responseheadertag = ServicesProperties.getString("SpoonScript.repheadparam"); 
		responsebodytag = ServicesProperties.getString("SpoonScript.respbodyparam"); 
		useridtag = ServicesProperties.getString("SpoonScript.useridparam"); 
		usergrouptag = ServicesProperties.getString("SpoonScript.usergroupparam"); 
		cachetag = ServicesProperties.getString("SpoonScript.cacheparam"); 
		debugstring = "trace";
	}
//	<------------------------------------------------------------------------->
 
//	<------------------------------------------------------------------------->
	/**
	 * Create a new test script for given JSR 223 engine
	 * @param engine The JSR 223 to use
	 */
	public ScriptChecker(ScriptEngine engine){
		init();
		bindings = engine.createBindings();
		bindings.put(urltag, urlcontent);
		bindings.put(responsebodytag, responseBody);
		bindings.put(responseheadertag, responseHeaders);
		bindings.put(requestbodytag,requestBody);
		bindings.put(requestheadertag, requestHeaders);
		bindings.put(useridtag, username);
		bindings.put(usergrouptag, usergroup);
		bindings.put(cachetag, sharedCache);
		bindings.put(debugstring, "");
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Create a test content for native scripts
	 * @param type The script type (either in REQMOD or in RESPMOD)
	 */
	public ScriptChecker(icap.core.Icap.TYPE type){
		this.httpMessage = new HttpMessage(null,type, requestHeaders, requestBody,
				responseHeaders, responseBody, username,
				usergroup, sharedCache);
	}
//	<------------------------------------------------------------------------->

	
//	<------------------------------------------------------------------------->
	/**
	 * Extract extension from script file and instantiate a script engine based on it
	 * Only used for JSR 223 scripts
	 * @param filename Script content
	 * @param content content to process
	 * @return modified content, of no use here
	 */
	public static String testScript(String filename, String content) {
		try{
			ScriptEngineManager scriptEngineManager;
			ScriptEngine engine;
			Compilable compilable;
			CompiledScript compiledscript;
			StringWriter errwriter = new StringWriter();
			
			try{
				scriptEngineManager = new ScriptEngineManager();
			}catch (Exception e){
				return e.getLocalizedMessage();
			}
			
			String extension = filename.substring(filename.lastIndexOf(".")+1); //$NON-NLS-1$
			engine = scriptEngineManager.getEngineByExtension(extension);
			if (engine==null){
				return "unable to instantiate script engine for \""+extension+"\" extension";
			}
			try {
				compilable = (Compilable) engine;
			} catch (java.lang.ClassCastException e){
				return e.getLocalizedMessage();
				//compiler is not available
			}
			engine.getContext().setErrorWriter(errwriter);
			try{
				compiledscript = compilable.compile(content);
				
				ScriptChecker test = new ScriptChecker(engine);
				compiledscript.eval(test.bindings);
				test.sharedCache.clear();
			}catch (Exception e){
				String error = e.getCause().toString().trim().replace("<Unknown Source>", filename);
				return error;
			}
		} catch (java.lang.NoClassDefFoundError nc){
			return ("["+filename+"]"+" unable to instantiate script engine for \""+filename.substring(filename.lastIndexOf(".")+1)+"\" extension"+ 
			"Please verify that corresponding engine is available in lib/scriptengines directory");
		} catch (Exception e){
			return ("["+filename+"]"+" unable to instantiate script engine for \""+filename.substring(filename.lastIndexOf(".")+1)+"\" extension: "+e.getLocalizedMessage());
		}
		return "";
	}
//	<------------------------------------------------------------------------->

	/**
	 * @return Returns the username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username The username to set.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return Returns the usergroup.
	 */
	public String getUsergroup() {
		return usergroup;
	}

	/**
	 * @param usergroup The usergroup to set.
	 */
	public void setUsergroup(String usergroup) {
		this.usergroup = usergroup;
	}

	/**
	 * @return Returns the requestHeaders.
	 */
	public String getRequestHeaders() {
		return requestHeaders;
	}

	/**
	 * @param requestHeaders The requestHeaders to set.
	 */
	public void setRequestHeaders(String requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	/**
	 * @return Returns the requestBody.
	 */
	public String getRequestBody() {
		return requestBody;
	}

	/**
	 * @param requestBody The requestBody to set.
	 */
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	/**
	 * @return Returns the responseHeaders.
	 */
	public String getResponseHeaders() {
		return responseHeaders;
	}

	/**
	 * @param responseHeaders The responseHeaders to set.
	 */
	public void setResponseHeaders(String responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	/**
	 * @return Returns the responseBody.
	 */
	public String getResponseBody() {
		return responseBody;
	}

	/**
	 * @param responseBody The responseBody to set.
	 */
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	/**
	 * @return Returns true if scripts are tested with ScriptChecker after a modification
	 */
	public static boolean isCheckEnabled() {
		return testScriptOnChange;
	}

	/**
	 * @param checkScripts Set if scripts must be tested using ScriptChecker after a modification
	 */
	public static void enableCheck(boolean checkScripts) {
		ScriptChecker.testScriptOnChange = checkScripts;
	}

	/**
	 * @return Returns The timeout threshold for script to run (in milliseconds).
	 */
	public static int getTimeOut() {
		return scriptTestTimeOut;
	}

	/**
	 * @param timeout The timeout threshold for script to run (in milliseconds).
	 */
	public static void setTimeOut(int timeout) {
		ScriptChecker.scriptTestTimeOut = timeout;
	}
	
	/**
	 * Update parameters with the new values
	 * @param attributes
	 */
	public static void updateParameters(Hashtable<String,String> attributes){
		String[] keys = attributes.keySet().toArray(new String[0]);
		for (String k:keys){
			String v = attributes.get(k);
			ScriptChecker.setValue(k, v);	
		}
		ScriptChecker.save();
		init();
	}
	
	
	


	
//  <------------------------------------------------------------------------->
	/**
	 * Reload resource bundle
	 */
	public static void refresh(){
		properties.clear();
		try {
			properties.load(new FileInputStream(configurationFilename));
		}catch (Exception e){
			e.printStackTrace();
		}
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Retrieve value associated with given key
	 * @param key The properties key to retrieve
	 * @return associated value, or empty string if unavailable
	 */
	public static String getString(String key) {
		try {
			return properties.getProperty(key).replace("\\r\\n", "\r\n");
		} catch (Exception e) {
			return null;
		}
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Retrieve value associated with given key
	 * @param key The properties key to retrieve
	 * @param defaultvalue default value to use if key is not set
	 * @return associated value, or empty string if unavailable
	 */
	public static String getString(String key,String defaultvalue) {
		if (!properties.containsKey(key)) return defaultvalue;
		return properties.getProperty(key).replace("\\r\\n", "\r\n");
	}
//  <------------------------------------------------------------------------->
	
	
//  <------------------------------------------------------------------------->
	/**
	 * Return parameter value as a boolean
	 * @param key Parameter name to look for
	 * @return true if parameter exists and value is equals to true, on, enable or enabled (case insensitive)
	 */
	public static boolean getBooleanValue(String key){
		if (!properties.containsKey(key)) return false;
		String value = getString(key).trim().toLowerCase();
		if (value.equals("true") || value.equals("on") || value.equals("enable") || value.equals("enabled") || value.equals("yes") ) {
			return true;
		}
		return false;
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Configure value associated with given key
	 * @param key The properties key to configure
	 * @param value The value to set for given key
	 */
	public static void setValue(String key, String value) {
		String encodedValue = value.replace("\r\n", "\\r\\n");
		properties.setProperty(key,encodedValue);
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Configure value associated with given key
	 */
	public static void save() {
		try {
			FileOutputStream out = new FileOutputStream(configurationFilename);
			properties.store(out, "ScriptChecker Properties");
			out.close();
		} catch (Exception e){
			Log.error(Log.CONFIG, "Error while saving ScriptChecker properties:",e);
		}
	}
//  <------------------------------------------------------------------------->
	
	
	
}
