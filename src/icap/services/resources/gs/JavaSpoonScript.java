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
 * Created  :   27 July 2009
 *---------------------------------------------------------------------------*/

package icap.services.resources.gs;

//////////////////////////////////////////
//IMPORTS
import tools.logger.Log;
import icap.core.Icap;
import tools.general.JavaRtCompiler;
import java.lang.reflect.*;
//////////////////////////////////////////

/**
 * Extension of GreasySpoon script, in order to support direct Java coding without JSR223 overhead<br>
 * Scripts must conform to NativeScript skeleton and are compiled and loaded in memory
 * Compiled result is stored on disk but is more or less useless
 * TODO: check if scripts are correctly unloaded from memory after deletion
 * TODO: check if writing compiled class on disk has an interest or working in memory can be enough
 * @author Karel MITTIG
 */ 
public class JavaSpoonScript extends SpoonScript {

	private int errorcounter = 0;
	private final static int linecorrection = -3;
	Constructor<NativeScript> constructor;
	/**package in which native scripts are automatically inserted*/
	public static String nativepackage = "serverscripts.nativejava";
	private String _internalName;
	
//	<------------------------------------------------------------------------->
	/**
	 * Create a new GreasySpoon Script object with its associated script engine.<br>
	 * The script engine is initialized when loading script from disk (as it
	 * requires the file extension to instantiate the associated engine. 
	 * @param icapmode The mode in which this script is running (REQMOD/RESPMOD)
	 */
	public JavaSpoonScript(Icap.TYPE icapmode){
		super(icapmode);
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Extract extension from script file and instantiate a script engine based on it 
	 */
	public void initEngine() {
			if (this.file==null) return;
			String filename = this.file.getName();
			String extension = filename.substring(filename.lastIndexOf(".")+1); //$NON-NLS-1$
			language = "java";
			if (Log.finest()) Log.trace(Log.FINEST, "Native Java interface instantiated for \""+extension+"\" extension"); 
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
	 */
	public String apply(icap.core.AbstractService service, String content, String url) throws Exception {
		try{
			if (constructor == null) return content;
			String username = service.getIcapHeader(icapuserheader);
			try{
				if (username !=null && username.length()!=0) {
					if (username.startsWith("LDAP")){ 
						int a = username.toLowerCase().indexOf("cn="); 
						int b = username.indexOf(",",a);
						if (b==-1) b =username.length(); 
						username = username.substring(a, b);
					} else {
						username = username.substring(username.indexOf("/")+1);
					}
				} else {
					username = service.getIcapHeader(userfallbackheader);
					if (username==null) username = service.getReqHeader(userfallbackheader);
					if (username==null) username = service.getRespHeader(userfallbackheader);
					if (username==null) username = "";
				}
			}catch (Exception e){//user unavailable => let it to null
			}
			String usergroup = service.getIcapHeader(icapgroupheader);
			try{
				if (usergroup ==null || usergroup.length()==0) {
					//usergroup = InetAddress.getByName(username.trim()).getCanonicalHostName();
					usergroup = service.getReqHeader(groupfallbackheader);
					if (usergroup==null) usergroup = service.getRespHeader(groupfallbackheader);
					if (usergroup==null) usergroup = "";
				}
			}catch (Exception e){
				//group unavailable => let it to null  
			}
			HttpMessage httpmessage;
			switch (service.getType()){
				case RESPMOD:
					httpmessage = HttpMessage.newResponse(service, service.getRequestHeaders(),
							service.getResponseHeaders(), content, username, usergroup, sharedCache) ;
					break;
				case REQMOD:
					httpmessage = HttpMessage.newRequest(service, service.getRequestHeaders(),  content==null?"":content
							, username, usergroup, sharedCache);
					break;
				default:
					return content;
			}
			
			String result ="";
			NativeScript script = constructor.newInstance();
			ScriptRunner scriptrunner = new ScriptRunner(script, httpmessage);
			scriptrunner.join(scriptTimeout);

			if (scriptrunner.isAlive()) {
				StackTraceElement[] traces = scriptrunner.getStackTrace();
				int errorline = traces[0].getLineNumber()+linecorrection;
				scriptrunner.forceKill();
				errorcounter++;
				if (errorThreshold!=0 && errorcounter>errorThreshold) {
					this.setStatus(false);
					if (Log.warning()) Log.error(Log.WARNING,String.format("%1$-20s Script threshold errors exceeded : script disabled",name));
					error += "["+name+"] Script has been automatically turned to off due to "+errorcounter+" successive errors.\n";
				} else {
					if (Log.warning()) Log.error(Log.WARNING, String.format("%1$-20s  timeout - threshold of %2$6s ms exceeded - aborted on line %3$s - URL: [%4$s]", name,scriptTimeout,errorline,url));
				}
			} else {
				if (errorcounter>0) errorcounter=0;
	
				// retrieve the potential response header and body
				// request headers and body are not updated for now (should be added for service in reqmod)
				switch (service.getType()){
					case RESPMOD:
						result = httpmessage.responseBody;
						service.setResponseHeaders(httpmessage.getResponseHeaders().replace("\r\n\r\n", "\r\n").concat("\r\n"));
						break;
					case REQMOD:
						result = httpmessage.requestBody;
						service.setRequestHeaders(httpmessage.getRequestHeaders().replace("\r\n\r\n", "\r\n").concat("\r\n"));
						break;
					default: break;
				}
				content = result;
				return result;
			}
		} catch (Throwable e){
			if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error in script on URL [%2$s] : %3$s ",name, url,e.getLocalizedMessage().trim()));
			errorcounter++;
			if (errorThreshold!=0 && errorcounter>errorThreshold) {
				this.setStatus(false);
				if (Log.warning()) Log.error(Log.WARNING,String.format("%1$-20s Script threshold errors exceeded : script disabled",name));
			}
		}
		throw new SpoonScriptException(name + " :script failure");
	}
//	<------------------------------------------------------------------------->



//	<------------------------------------------------------------------------->
	/**
	 * Thread used to run script
	 * Allows to implement timeouts on script execution
	 */
	public class ScriptRunner extends Thread implements UncaughtExceptionHandler{
		NativeScript scriptToRun;
		HttpMessage httpmessage;
		/**
		 * Launch script evaluation using a dedicated thread<br>
		 * Allows to interrupt evaluation if needed (infinite loop, timeout, ...)
		 * @param script The script to evaluate
		 * @param httpmessage HTTP Message object to process
		 */
		public ScriptRunner(NativeScript script, HttpMessage httpmessage){
			super("JavaSpoonScript - runner");
			this.setPriority(Thread.MIN_PRIORITY);
			this.scriptToRun = script;
			this.httpmessage = httpmessage;
			this.setUncaughtExceptionHandler(this);
			this.start();
		}
		/**
		 * Destroy running script
		 */
		@SuppressWarnings("deprecation")
		public void forceKill(){
			try{
				//heavy interruption method, required as there is no direct stop method in ScriptEngines
				this.suspend();
				this.interrupt();
				this.stop();
				this.finalize();
				//done
			} catch (Exception e){
			} catch (Throwable e){
			}
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		public void run(){
			try{
				scriptToRun.main(httpmessage);
			} catch (java.lang.NoSuchMethodError nse){
				if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error (233) in script : %2$s",name, nse.getStackTrace()[0]).toString());
			} catch (java.lang.NoClassDefFoundError ncdf){
				if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error (235) in script : %2$s",name, ncdf.getStackTrace()[0]).toString());
			} catch (Throwable e){
				if (Log.warning()) 
				try{
					Log.service(Log.WARNING,String.format("%1$-20s Error (237) in script : %2$s",name, e.getLocalizedMessage() +':' +e.getStackTrace()[0]).toString());
				} catch (Exception e1){
					Log.service(Log.WARNING,String.format("%1$-20s Error (237) in script : %2$s",name, e.getLocalizedMessage()));
				}
				//abort
			}
		}
		/**
		 * Intercept all possible thread exceptions
		 */
		public void uncaughtException(Thread arg0, Throwable t) {
			// TODO Auto-generated method stub
			if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error (246) in script : %2$s",name, t.getLocalizedMessage()));
		}
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Clear script parameters in order to allow a reload from file
	 */
	public void reset(){
		this.clearPatterns();
		this.constructor = null;
	}
//	<------------------------------------------------------------------------->

	/**
	 * @return Internal java class name as compiled in memory and on disk
	 */
	public String getInternalName(){
		return _internalName;
	}
	
//	<------------------------------------------------------------------------->
	/**
	 * Update script content with new content
	 * @param newServerScript Script content to use
	 */
	@SuppressWarnings("unchecked")
	public void setServerScript(String newServerScript) {
		this.serverScript = newServerScript;
		try{
			//Compile code
			_internalName = this.getScriptName() + (this.mode == Icap.TYPE.REQMOD?"_req":"_resp");
			Class<NativeScript> nativeScriptInstance = JavaRtCompiler.memoryCompile(
					packageScript(new StringBuilder(newServerScript), _internalName)
					,_internalName, nativepackage, JavaRtCompiler.COMPILER_DIRECTORY);
			//Retrieve constructor
			constructor = nativeScriptInstance.getConstructor();
			testScript();
		} catch (Exception e){
			String error = parseError(e);
			this.constructor = null;
			Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",this.getScriptName(),error));
			this.error = "Error in script ["+this.getScriptName()+"]:"+error+"<br>";
		}  catch (java.lang.Error err){
			String error = parseError(err);
			this.constructor = null;
			Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",this.getScriptName(),error));
			this.error = "Error in script ["+this.getScriptName()+"]:"+error+"<br>";
		}

	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/* (non-Javadoc)
	 * @see icap.services.resources.SpoonScript#testScript()
	 */
	private void testScript(){
		try{
			if (!ScriptChecker.testScriptOnChange) return;
			ScriptChecker test = new ScriptChecker(this.mode);
			if (constructor == null) return;
			NativeScript script = constructor.newInstance();
			ScriptRunner scriptrunner = new ScriptRunner(script, test.httpMessage);
			scriptrunner.join(ScriptChecker.scriptTestTimeOut);
			if (scriptrunner.isAlive()) {
				StackTraceElement[] traces = scriptrunner.getStackTrace();
				scriptrunner.forceKill();
				this.error = "Error in script ["+this.getScriptName()+"]: infinite loop or contention detected on line "+(traces[0].getLineNumber()+linecorrection)+"<br>";
			} else {
				//script.main(test.httpMessage);
				this.error = "";
			}
		} catch (Throwable t){
			String error = parseError(t);
			this.constructor = null;
			Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",this.getScriptName(),error));
			this.error = "Error in script ["+this.getScriptName()+"]:"+error+"<br>";
		} 		
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Parse an exception in order to provide useful output for developer
	 * Correct line numbers so transparently added headers stay invisible  
	 * @param t The exception to parse
	 * @return formatted error
	 */
	public static String parseError(Throwable t){
		String error = "";
		String topatch ="";
		try{
			if (t.getMessage()!= null){
				String[] elements;
				if (t.getMessage().contains(",")) elements= t.getMessage().split(",");
				else elements = new String[]{t.getMessage()};
				topatch += t.getClass().getCanonicalName() + " at ";
				for (String s:elements){
					int p1 = s.indexOf(".java:") + ".java:".length();
					s = s.substring(p1);
					int p2 = s.indexOf(":");
					int line = Integer.parseInt(s.substring(0,p2).trim());
					topatch += "line "+ (line+linecorrection) + s.substring(p2, s.length()-1) + " ;";
				}
			} else {
				StackTraceElement ste = t.getStackTrace()[0];
				topatch = t.getClass().getCanonicalName() + ": line "  +(ste.getLineNumber()+linecorrection);
			}
			error = topatch.replace("<", "&lt;").replace(">", "&gt;").replace("\r", " ").replace("\n", " ");
		} catch(Exception e1){
			error = t.getMessage();
		}
		return error;
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Package a Java script so it complies to NativeScript interface
	 * @param sb the Java source script to package
	 * @param scriptname the class name (to generate)
	 * @return Packaged source code
	 */
	public static String packageScript(StringBuilder sb, String scriptname){
		sb.insert(0, "package "+nativepackage+";\r\nimport icap.services.resources.gs.*;\r\n");
		int lastimport = sb.indexOf(";", sb.lastIndexOf("import ")+1);
		sb.insert(lastimport+1, "\r\npublic class " +  scriptname+ " extends NativeScript {");
		sb.append("\r\n}");
		return sb.toString();
	}
//	<------------------------------------------------------------------------->

}

