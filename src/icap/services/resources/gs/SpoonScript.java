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
 * Created  :   27 sept. 2007
 *---------------------------------------------------------------------------*/

package icap.services.resources.gs;

//////////////////////////////////////////
//IMPORTS
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;
import tools.logger.Log;
import icap.core.Icap;
import icap.services.ServicesProperties;
import tools.httpserver.HttpConstants;
//////////////////////////////////////////

/**
 * GreasySpoon script, synchronized with a script file on disk <br>
 * scripts are updated based on file timestamp provided by the OS<br>
 * Scripts files must include following header:<br>
 * <b>==ServerScript==</b><br>
 * &#064;name          Say Hello!<br>
 * &#064;status     	on/off => allow to enable/disable script (enabled by default)<br>
 * &#064;description	Script description<br>
 * &#064;include       	regex matching url (i.e: http://www\.google\.com/.* ) and for which script is applied<br>
 * &#064;include       	other regexes<br>
 * &#064;exclude       	regex matching url (i.e: http://gmail\.google\.com/.* ) and for which script does not apply <br>
 * <b>==/ServerScript==</b><br><br>
 * Optional supported headers are:<br>
 * &#064;order		0..n ==> allows to ask this script to be executed in n position<br>
 * &#064;responsecode 0|2xx|3xx|5xx.. ==> Only for scripts on responses, allows to proceed only
 *  responses with given HTTP status code (0:proceed all)<br>
 * &#064;timeout	n(in ms) => set script execution timeout. This value cannot exceed 
 * overall timeout configured in GreasySpoon main options<br>
 * <br>
 * Note:
 * <ul>
 * 	<li> exclude regexes are evaluated <i>before</i> include regexes</li>
 * 	<li> per default, script is applied to all responses (exclude: none, include: .* )</li>
 *  <li> there is no limitation regarding the number of include/exclude regexes</li>
 *  </ul>
 * TODO: add simple domain matching to avoid regexes when not necessary
 * TODO: add cache cleaner
 * @author Karel MITTIG
 */ 
public abstract class SpoonScript extends Thread implements HttpConstants {

	/**Defines encoding used for scripts stored on disk*/
	public final static String SCRIPTENCODING = "UTF-8";
	static int ordering = 0;
	
	/** String used to indicate errors encountered with script*/
	protected String error="";

	/**Defines scripts execution time threshold (in milliseconds). default: 60000(60 seconds)*/
	protected static int scriptMaxTimeout = 60000;
	/**Defines maximum errors allowed for scripts (use 0 for infinite)*/
	protected static int errorThreshold = 0;
	
	/**Defines script maximum execution time (in milliseconds)*/
	protected int scriptTimeout = scriptMaxTimeout;
	
	protected static boolean bypassOnError = true;
	
	/**
	 * set rights required to modify script
	 */
	protected RIGHTS rights = RIGHTS.NONE;


	/**Absolute path to scripts files repository*/
	public static String path;
	/**A friendly name for the script*/
	String name="";

	/**A description of what the script is supposed to do.*/
	String description="";

	/** The service script by itself */
	String serverScript="";

	/** The position into which script must be applied*/
	private int order = 9999;

	/** The HTTP Responses status code that this script is interested in (0 for any)*/
	int[] processedStatusCodes = new int[]{200};
	
	/** Set if service is enabled or disabled*/
	boolean status = true;

	/** File containing the service script*/
	File file;
	/** last modified time stamp of file containing script*/
	long lastmodified;

	/**Script language (retrieved from ScriptEngineManager)*/
	String language="";


	/**
	 * Hash table that can be used by scripts to stored objects (for caching, session tracking, ...)
	 * The hash table uses strings as key in order to stock objects.
	 * Hash table is of the WeakHashMap type, which means that objects can be automatically flushed
	 * by the garbage collector if memory's level goes low. Therefore, there is no garantee for the object
	 * to be kept.   
	 * */
	//static WeakHashMap<String, Object> sharedCache = new WeakHashMap<String, Object>();
	static ConcurrentHashMap<String, Object> sharedCache = new ConcurrentHashMap<String, Object>();

	/**
	 * A regex pattern defining on which sites the script can be executed. There can be more than one.<br>
	 * Use PERL regex syntax to match URLs (ex: http://www\.orange\.fr/.*)<br>
	 * default: one, set to '.*' 
	 */
	private Vector<Pattern> includes = new Vector<Pattern>();

	/**
	 * A regex pattern defining sites that are out of the scope of the script. There can be more than one<br>
	 * Exclude regexes are evaluated <u>before</u> include commands<br>
	 * Default: none
	 */
	private Vector<Pattern> excludes = new Vector<Pattern>();


	/** mode in which this script is running (REQMOD/RESPMOD)*/
	public Icap.TYPE mode;
	protected static String urltag = ServicesProperties.getString("SpoonScript.urlparam","requestedurl");
	protected static String requestheadertag = ServicesProperties.getString("SpoonScript.reqheadparam"); 
	protected static String requestbodytag = ServicesProperties.getString("SpoonScript.reqbodyparam"); 
	protected static String responseheadertag = ServicesProperties.getString("SpoonScript.repheadparam"); 
	protected static String responsebodytag = ServicesProperties.getString("SpoonScript.respbodyparam"); 
	protected static String useridtag = ServicesProperties.getString("SpoonScript.useridparam"); 
	protected static String usergrouptag = ServicesProperties.getString("SpoonScript.usergroupparam"); 
	protected static String cachetag = ServicesProperties.getString("SpoonScript.cacheparam"); 
	protected static String icapuserheader = ServicesProperties.getString("SpoonScript.icapuserheader");
	protected static String userfallbackheader = ServicesProperties.getString("SpoonScript.userfallbackheader");
	protected static String icapgroupheader = ServicesProperties.getString("SpoonScript.icapgroupheader"); 
	protected static String groupfallbackheader = ServicesProperties.getString("SpoonScript.groupfallbackheader");
	protected static String debugstring = "trace";
	
	/** Set script lines number (add carriage return at the end if needed). Used for edition only.*/
	private static int minlines = 20;
	
//	<------------------------------------------------------------------------->
	/**
	 * Initialize class parameters
	 */
	public static void init(){
		requestheadertag = ServicesProperties.getString("SpoonScript.reqheadparam"); 
		requestbodytag = ServicesProperties.getString("SpoonScript.reqbodyparam"); 
		responseheadertag = ServicesProperties.getString("SpoonScript.repheadparam"); 
		responsebodytag = ServicesProperties.getString("SpoonScript.respbodyparam"); 
		useridtag = ServicesProperties.getString("SpoonScript.useridparam"); 
		usergrouptag = ServicesProperties.getString("SpoonScript.usergroupparam"); 
		cachetag = ServicesProperties.getString("SpoonScript.cacheparam"); 
		icapuserheader = ServicesProperties.getString("SpoonScript.icapuserheader");
		userfallbackheader = ServicesProperties.getString("SpoonScript.userfallbackheader");
		icapgroupheader = ServicesProperties.getString("SpoonScript.icapgroupheader"); 
		groupfallbackheader = ServicesProperties.getString("SpoonScript.groupfallbackheader");
		sharedCache.clear();
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Create a new Spoonscript object with its associated script engine.<br>
	 * The script engine is initialized when loading script from disk (as it
	 * requires the file extension to instantiate the associated engine. 
	 * @param icapmode The mode in which this script is running (REQMOD/RESPMOD)
	 */
	public SpoonScript(Icap.TYPE icapmode){
		super("SpoonScript");
		this.mode = icapmode;
		scriptTimeout = scriptMaxTimeout;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Retrieve Script skeleton in order to help developpers when creating new
	 * scripts 
	 * @param icapmode The ICAP mode in which script will run
	 * @param extension Script extension (language)
	 * @param comment custom comment or skeleton part
	 * @return The script skeleton that should be directly usable by developer (i.e. it must be compilable) 
	 */
	public final static String getScriptSkeleton(Icap.TYPE icapmode, String extension,String comment){
		int linecounter = minlines;
		String h = ServicesProperties.getString("SpoonScript."+extension+".scriptheader");
		if (h==null || h.equals("")){
			h = ServicesProperties.getString("SpoonScript.scriptheader");
		}
		String[] lines = h.split("\r\n");
		String tagtoclear, tagtodelete;
		switch (icapmode){
			case REQMOD:
				tagtodelete = "%HEAD__RESP%";
				tagtoclear = "%HEAD__REQ%";
				break;
			default:
				tagtodelete = "%HEAD__REQ%";
				tagtoclear = "%HEAD__RESP%";
		}
		StringBuilder sb = new StringBuilder();
		for (String s:lines){
			if (s.trim().startsWith(tagtodelete)) continue;
			if (s.trim().startsWith(tagtoclear)) s=s.replace(tagtoclear, "");
			if (s.trim().startsWith("%SKELETON%")) {
				s=s.replace("%SKELETON%", "");
				linecounter--;
			} else {
				sb.append(comment);
			}
			sb.append(s).append("\r\n");
		}
		for (int i=0; i<linecounter; i++){
			sb.append("\r\n");
		}
		return sb.toString();

	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Apply current spoonscript to server response
	 * For now, this method is only applicable on String content (response body is handled as a String)
	 * @param service	The ICAP service providing the response
	 * @param content 	The response body
	 * @param url		The requested URL
	 * @return	Modified response body
	 * @throws Exception in case of script error (failure, timeout, ...)
	 */
	public abstract String apply(icap.core.AbstractService service, String content, String url) throws Exception;

//	<------------------------------------------------------------------------->
	/**
	 * Clear script parameters in order to allow a reload from file
	 */
	public abstract void reset();
	/**Abstract method giving opportunity to do initialization steps for implemented language engine */
	public abstract void initEngine();
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
/**
 * @return Detected compilation errors in the script
 */
public String getPendingErrors(){
	return error;
}
//<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Check if script must be applied for given URL
	 * @param url the URL to test (full format, for example http://www.orange.com/items/sports.html)<b>
	 * Warning: URL should be provided in lower case (regexes are case sensitive)
	 * @return true if script must be applied, false otherwise
	 */
	public final boolean isApplicable(String url){
		refresh();
		if (this.excludes.size()>0){
			for (Pattern pattern:this.excludes){
				if (pattern.matcher(url).matches()) return false;
			}
		}
		//default include: wildcard
		if (this.includes.size()==0) return true;

		for (Pattern pattern:this.includes){
			if (pattern.matcher(url).matches()) return true;
		}
		//no matching include
		return false;
	}
//	<------------------------------------------------------------------------->
	
	protected final void clearPatterns(){
		this.excludes.clear();
		this.includes.clear();
	}
	
//	<------------------------------------------------------------------------->
	/**
	 * Check if script must be applied for given URL and response code
	 * @param url the URL to test (full format, for example http://www.orange.com/items/sports.html)<b>
	 * @param responseCode the HTTP status code of the response to check
	 * Warning: url should be provided as lowercase (regexes are case sensitive)
	 * @return true if script must be applied, false otherwise
	 */
	public final boolean isApplicable(String url, int responseCode){
		refresh();
		if (this.mode == Icap.TYPE.RESPMOD){
			boolean b = false;
			for (int i:processedStatusCodes){
				if (i == responseCode || i == 0){
					b = true;
					break;
				}
			}
			if (!b) return false;
		}
		return isApplicable(url);
	}
//	<------------------------------------------------------------------------->
	

//	<------------------------------------------------------------------------->
	/**
	 * Add an include condition to userscript
	 * @param include the URL to add
	 */
	public final void addInclude(String include){
		if (include==null || include.trim().length() == 0) return;
		try {
			Pattern pattern = Pattern.compile(include.trim());
			this.includes.add(pattern);
		} catch (Exception e){
			Log.service(Log.WARNING, String.format("%1$-20s Invalid include expression %2$s", this.getScriptName(),include.trim()));
		}
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Remove an include condition to userscript
	 * @param pattern the regex Pattern to remove
	 */
	public final void delInclude(Pattern pattern){
		this.includes.remove(pattern);
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Add an exclude condition to userscript
	 * @param exclude the regex Pattern to add
	 */
	public final void addExclude(String exclude){
		if (exclude==null || exclude.trim().length() == 0) return;
		try{
			Pattern pattern = Pattern.compile(exclude.trim());
			this.excludes.add(pattern);
		} catch (Exception e){
			Log.service(Log.WARNING, String.format("%1$-20s Invalid exclude expression %2$s", this.getScriptName(),exclude.trim()));
		}
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Remove an exclude condition to userscript
	 * @param exclude the regex Pattern to remove
	 */
	public final void delExclude(Pattern exclude){
		this.excludes.remove(exclude);
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Turn script status to on/off
	 * @param newstatus the new status to set
	 */
	public final void setStatus(boolean newstatus){
		if (this.status == newstatus) return; 
		if (Log.finest()) Log.trace(Log.FINEST, "["+this.name+" ] Script status set to " + newstatus); //$NON-NLS-1$ //$NON-NLS-2$
		this.status = newstatus;
	}

	/**
	 * @return script status (either enabled or disabled)
	 */
	public final boolean getStatus(){
		refresh();
		if (Log.finest()) Log.trace(Log.FINEST, "["+this.name+" ] Script status set to " + status); //$NON-NLS-1$ //$NON-NLS-2$
		return this.status;
	}

	/**
	 * Check if file has been modified (using filesystem timestamp), and if true reload it
	 */
	public final void refresh(){
		if ( isModified()) reload();
	}

	/**
	 * @return The File storing script on disk
	 */
	public final File getFile() {
		return this.file;
	}

	/**
	 * Set spoonscript to run script in given file.<br>
	 * Initialize script engine based on file extension 
	 * @param newfile The file containing the script
	 */
	public final synchronized void setFile(File newfile) {
		this.file = newfile;
		initEngine();
		this.lastmodified = this.file.lastModified();
	}

	/**
	 * @return A short description of what the script does
	 */
	public final String getDescription() {
		return this.description;
	}

	/**
	 * A short description of what the script does. 
	 * @param newdescription Description of the script 
	 */
	public final void setDescription(String newdescription) {
		this.description = newdescription;
	}

	/**
	 * @return Script name as used for logs.
	 */
	public final String getScriptName() {
		return this.name;
	}

	/**
	 * Set script name. This name is used mainly for logs.
	 * @param newname A short name for the script.<br> Name should be unique.
	 */
	public final void setScriptName(String newname) {
		this.name = newname;
	}

	/**
	 * Check if script file is up to date. If not, reload the script from disk
	 * @return the script as stored on disk
	 */
	public final String getServerScript() {
		if ( isModified()) reload();
		return this.serverScript;
	}
	
	/**
	 * @return True if script has been modified, false otherwise
	 */
	public final boolean isModified(){
		return this.lastmodified != this.file.lastModified();
	}

	/**
	 * Update script content with new content
	 * @param newServerScript Script content to use
	 */
	public abstract void setServerScript(String newServerScript);
	/**Method called after script modification in order to do some sanity checks*/
	//public abstract void testScript();
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Reload script from file
	 */
	private final synchronized void reload(){
		Log.service(Log.INFO,"["+this.getScriptName()+"]\t script reloaded after modification event"); //$NON-NLS-1$
		load(this.file);
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * Create a new SpoonScript object based on given filename
	 * @param filename the script filename to use to generate script
	 * @param mode The ICAP mode in which this script runs (either REQMOD or RESPMOD)
	 * @return a new SpoonScript object based on given filename
	 */
	public final static SpoonScript loadFromFile(String filename, Icap.TYPE mode){
		File fich = new File(filename);
		if (!fich.exists()){
			Log.service(Log.INFO,"GreasySpoon script <"+filename+"> deleted."); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}// Endif
		SpoonScript gms;
		if (filename.endsWith(".java")){
			gms = new JavaSpoonScript(mode);
			return gms.load(fich);
		} else {
			gms = new Jsr223SpoonScript(mode);
			return gms.load(fich);
		}
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Create SpoonScript object from file. Parse control parameters
	 * and try to generate a compiled version if possible.
	 * @param file	The file to read script from (provided with absolute path) 
	 * @return	the updated SpoonScript object/a new SpoonScript; or null if file was not founded
	 */
	protected final SpoonScript load(File file){
		if (Log.finest()) Log.trace(Log.FINEST, "Reloading script from "+file.getName());
		this.reset();

		try {       
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), SCRIPTENCODING));
			String str;
			StringBuilder sb = new StringBuilder();

			boolean intoHeader = false, headerreaded = false;
			int pos =-1;

			while ((str = in.readLine()) !=null ){
				if (str.startsWith("#rights=")){
					this.setRights(RIGHTS.valueOf(str.substring(str.indexOf("=")+1)));
					continue;
				}
				sb.append(str).append("\r\n");
				//Parse Server script header
				if (!intoHeader && str.contains("==ServerScript==")) intoHeader =true; 
				if (!headerreaded && intoHeader && str.contains("==/ServerScript==")) headerreaded = true; 
				if (!headerreaded && intoHeader){
					if ( (pos = str.indexOf("@name"))!=-1 ){ 
						this.setScriptName(str.substring(pos+"@name".length()).trim()); 
						continue;
					} 
					if ( (pos = str.indexOf("@description"))!=-1 ){ 
						this.setDescription(str.substring(pos+"@description".length()).trim());
						continue;
					} 
					if ( (pos = str.indexOf("@order"))!=-1 ){ 
						this.setOrder(Integer.parseInt(str.substring(pos+"@order".length()).trim()));
						continue;
					} 
					if ( (pos = str.indexOf("@responsecode"))!=-1 ){
						String[] respcodes = str.substring(pos+"@responsecode".length()).trim().split("\\s+");
						int[] supportedResponseCodes;
						String logs = "";
						try { // Parse response codes that will be processed by scripts
							if (respcodes==null || respcodes.length==0 || (respcodes.length==1 && respcodes[0].trim().equals("")) ) {
								supportedResponseCodes = new int[]{200};
								logs = "200";
							} else {
								supportedResponseCodes = new int[respcodes.length];
								for (int i=0; i<respcodes.length; i++){
									if (respcodes[i].trim().equals("*") || respcodes[i].trim().equals("0")) { // * or 0: process all responses
										supportedResponseCodes = new int[]{0};
										logs = "0";
										break;
									}
									supportedResponseCodes[i] = Integer.parseInt(respcodes[i].trim());
									logs += " "+supportedResponseCodes[i];
								}
							}
						} catch (Exception e) { // default: process only 200 OK responses 
							supportedResponseCodes = new int[]{200};
							logs = "200";
						}
						this.setResponseCodeFilter(supportedResponseCodes);
						continue;
					}
					if ( (pos = str.indexOf("@include"))!=-1 ){ 
						this.addInclude(str.substring(pos+"@include".length()).trim());
						continue;
					} 
					if ( (pos = str.indexOf("@exclude"))!=-1 ){ 
						this.addExclude(str.substring(pos+"@exclude".length()).trim());
						continue;
					} 
					if ( (pos = str.indexOf("@timeout"))!=-1 ){ 
						this.setScriptTimeout(Integer.parseInt(str.substring(pos+"@timeout".length()).trim()));
						continue;
					} 
					if ( (pos = str.indexOf("@status"))!=-1 ){ 
						if (str.substring(pos+"@status".length()).trim().indexOf("off")!=-1){ 
							this.setStatus(false);
						} else {
							this.setStatus(true);
						}
						continue;
					}				
				}//End of header parsing

			}//End while readLine
			in.close();
			this.setFile(file);
			this.setServerScript(sb.toString());
			return this;

		} catch (Exception e){
			Log.error(Log.WARNING, "Error in SpoonScript file <"+file.getAbsoluteFile().toString()+">. File corrupted."); 
			return null;
		}//End try&catch
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * Update script file on disk with the new status value 
	 */
	public final void saveWithNewStatus(){
		try{
			int pos1 = this.serverScript.indexOf("==ServerScript==")+"==ServerScript==".length(); //$NON-NLS-1$ //$NON-NLS-2$
			int pos2 = this.serverScript.indexOf("==/ServerScript==",pos1); //$NON-NLS-1$
			int statuspos = this.serverScript.indexOf("@status", pos1); //$NON-NLS-1$
			if (statuspos!=-1 &&  statuspos < pos2){
				// status founded in header
				statuspos += "@status".length()+1; //$NON-NLS-1$
				int endline = this.serverScript.indexOf("\r\n", statuspos); //$NON-NLS-1$
				this.serverScript = this.serverScript.substring(0,statuspos) + (status?"on":"off") +this.serverScript.substring(endline); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				//insert status in header
				int nextline = this.serverScript.indexOf("\r\n", pos1)+2; //$NON-NLS-1$
				String comment=""; //$NON-NLS-1$
				comment = icap.services.GreasySpoon.languageComments.getProperty(this.language,"//"); //$NON-NLS-1$
				this.serverScript = this.serverScript.substring(0,nextline) + comment + "@status " + (status?"on":"off")  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+"\r\n"+this.serverScript.substring(nextline); //$NON-NLS-1$
			}
			save(this.getFile().getAbsolutePath(),this.serverScript, this.rights);
		}catch (Exception e){
			this.error += "Error while synchronizing file status on disk: "+this.file.getName()+"<br>"; 
		}
	}
//	<------------------------------------------------------------------------->

//	---------------------------------------------------------------------------
	/**
	 * Save provided content into given filename.
	 * Existing file with given name is deleted.
	 * @param filename the file name to use
	 * @param htmlcontent the content to save into file
	 * @param rights RIGHTS attached to the script (ADMIN, USER)
	 */
	public final static void save(String filename,String htmlcontent, RIGHTS rights){
		if (filename==null) {
			return;
		}
		File fich = new File(filename);
		if (fich.exists()){
			fich.delete();
			fich = new File(filename);
		}// Endif
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fich), SCRIPTENCODING));
			out.write("#rights="+rights.toString()+"\r\n");
			out.write(htmlcontent);
			out.flush();
			out.close();
		} catch (IOException e){
			return;
		}//End try&catch
	} 
//	---------------------------------------------------------------------------

//	---------------------------------------------------------------------------
	/**
	 * @return Position on which this script would like to be applied 
	 * (compared to other scripts position) 
	 */
	public final int getOrder() {
		return this.order;
	}

	/**
	 * Update the script order. Ask GreasySpoon service to reorder scripts
	 * according to this new value
	 * @param neworder the new position to apply
	 */
	public final void setOrder(int neworder) {
		if (this.order==neworder) return;
		this.order = neworder;
		icap.services.GreasySpoon.reorder();
	}
//	---------------------------------------------------------------------------

	/**
	 * @return Script language 
	 */
	public final String getLanguage(){
		return this.language;
	}
//	<------------------------------------------------------------------------->

	/**
	 * Retrieve number of successive errors after which script is forced as disabled.<br>
	 * Threshold is common to all scripts. 
	 * @return Errors threshold
	 */
	public final static int getErrorThreshold() {
		return errorThreshold;
	}

	/**
	 * Set number of successive errors after which script is forced as disabled.<br>
	 * Threshold is common to all scripts. 
	 * @param newErrorThreshold Errors Threshold
	 */
	public final static void setErrorThreshold(int newErrorThreshold) {
		SpoonScript.errorThreshold = newErrorThreshold;
		Log.error(Log.CONFIG, "GreasySpoon scripts error threshold set to "+newErrorThreshold +" successive errors");
	}

	/**
	 * Get scripts maximum execution time after which script is considered in timeout and interrupted.<br>
	 * Maximum timeout is common to all scripts. 
	 * @return Scripts maximum timeout (in milliseconds) 
	 */
	public final static int getScriptMaxTimeout() {
		return scriptMaxTimeout;
	}

	/**
	 * Get scripts maximum execution time after which script is considered in timeout and interrupted.<br>
	 * Maximum timeout is common to all scripts.
	 * @param newScriptMaxTimeout new scripts maximum timeout (in milliseconds)
	 */
	public final static void setScriptMaxTimeout(int newScriptMaxTimeout) {
		if (newScriptMaxTimeout<0) newScriptMaxTimeout=0;
		SpoonScript.scriptMaxTimeout = newScriptMaxTimeout;
		Log.error(Log.CONFIG, "GreasySpoon scripts maximum timeout set to "+newScriptMaxTimeout +" ms");
	}

	/**
	 * Get current script maximum execution time after which script is considered in timeout and interrupted.<br>
	 * Timeout value cannot exceed MaxTimeout value 
	 * @see "SpoonScript.getScriptMaxTimeout()"
	 * @return timeout value associated to this script, in milliseconds 
	 */
	public final int getScriptTimeout() {
		return scriptTimeout;
	}

	/**
 	 * Set current script maximum execution time after which script is considered in timeout and interrupted.<br>
	 * Timeout value cannot exceed MaxTimeout value 
	 * @see "SpoonScript.getScriptMaxTimeout()"
	 * @param newScriptTimeout value, in milliseconds
	 */
	public final void setScriptTimeout(int newScriptTimeout) {
		if (newScriptTimeout>scriptMaxTimeout || newScriptTimeout<1) {
			this.scriptTimeout = scriptMaxTimeout;
		} else {
			this.scriptTimeout = newScriptTimeout;
		}
	}

	/**
	 * @return RIGHTS attached to the script
	 * @see tools.httpserver.HttpConstants.RIGHTS
	 */
	public final RIGHTS getRights() {
		return rights;
	}

	/**
	 * Update RIGHTS attached to the script
	 * @param rights RIGHTS attached to the script
	 * @see tools.httpserver.HttpConstants.RIGHTS
	 */
	public final void setRights(RIGHTS rights) {
		this.rights = rights;
	}

	/**
	 * @return The HTTP response code for which script will be called (200, 302, ..., or 0 for all)
	 */
	public final int[] getResponseCodeFilter() {
		return processedStatusCodes;
	}

	/**
	 * @param statuscodes Set HTTP response code for which script will be called (200, 302, ..., or 0 for all)
	 */
	public final void setResponseCodeFilter(int[] statuscodes) {
		this.processedStatusCodes = statuscodes;
	}
	
	//-----------------------------------------------------------
	//-- Shared cache methods
	/**
	 * @return Hash table <String, Object> used to share objects between scripts
	 */
	public final static ConcurrentHashMap<String, Object> getSharedCache(){
		return sharedCache;
	}
	/**
	 * @return the script shared Hash table size 
	 */
	public final static int getSharedCacheSize(){
		return sharedCache.size();
	}
	/**
	 * Flush Script shared Hash table
	 */
	public final static void flushSharedCache(){
		sharedCache.clear();
	}
	//-----------------------------------------------------------
}

