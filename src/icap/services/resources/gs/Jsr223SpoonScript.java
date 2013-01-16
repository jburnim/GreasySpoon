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
import javax.script.*;
import tools.logger.Log;
import icap.core.Icap;
//////////////////////////////////////////


/**
 * GreasySpoon script, synchronized with a script file on disk <br>
 * scripts are updated based on file time stamp provided by the OS<br>
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
 * @author Karel MITTIG
 */ 
public class Jsr223SpoonScript extends SpoonScript {

	
	private int errorcounter = 0;
	
	/**Script engine and environment*/
	ScriptEngineManager scriptEngineManager;
	ScriptEngine engine;
	Compilable compilable;
	CompiledScript compiledscript;
	StringWriter errwriter = new StringWriter();
	

//	<------------------------------------------------------------------------->
	/**
	 * Create a new SpoonScript object with its associated script engine.<br>
	 * The script engine is initialized when loading script from disk (as it
	 * requires the file extension to instantiate the associated engine. 
	 * @param icapmode The mode in which this script is running (REQMOD/RESPMOD)
	 */
	public Jsr223SpoonScript(Icap.TYPE icapmode){
		super(icapmode);
		try{
			this.scriptEngineManager = new ScriptEngineManager();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Extract extension from script file and instantiate a script engine based on it 
	 */
	public void initEngine() {
		try{
			if (this.file==null || this.engine!= null) return;
			String filename = this.file.getName();
			String extension = filename.substring(filename.lastIndexOf(".")+1); //$NON-NLS-1$
			this.engine = this.scriptEngineManager.getEngineByExtension(extension);
			if (this.engine==null){
				if (Log.warning()) Log.service(Log.WARNING,"unable to instantiate script engine for \""+extension+"\" extension"); 
				return;
			}
			
			language = engine.getFactory().getLanguageName();
			if (Log.finest()) Log.trace(Log.FINEST, "Scriptengine ["+ engine.getFactory().getEngineName()+"] instantiated for \""+extension+"\" extension"); 
			//bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
			try {
				compilable = (Compilable) engine;
			} catch (java.lang.ClassCastException e){
				//compiler is not available
			}
			this.engine.getContext().setErrorWriter(errwriter);
			//invocableEngine = (Invocable)engine;
		} catch (java.lang.NoClassDefFoundError nc){
			if (Log.warning()) Log.service(Log.WARNING,"["+this.file.getName()+"]"+" unable to instantiate script engine for \""+this.file.getName().substring(this.file.getName().lastIndexOf(".")+1)+"\" extension"+ 
			"Please verify that corresponding engine is available in lib/scriptengines directory");
			//nc.printStackTrace();
		} catch (Exception e){
			if (Log.warning()) Log.error(Log.WARNING,"["+this.file.getName()+"]"+" unable to instantiate script engine for \""+this.file.getName().substring(this.file.getName().lastIndexOf(".")+1)+"\" extension",e);
		}
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
	public String apply(icap.core.AbstractService service, String content, String url) throws Exception{
		try{
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
			errwriter.flush();
			Bindings bindings = this.engine.createBindings();
			//this.engine.getContext().setErrorWriter(errwriter);
			//this.engine.getContext().setWriter(errwriter);
			switch (service.getType()){
				case RESPMOD:
					bindings.put(urltag, service.getReqUrl());
					bindings.put(responsebodytag,  content);
					bindings.put(responseheadertag, service.getResponseHeaders());
					//bindings.put(requestbodytag,service.getReqBody());
					bindings.put(requestheadertag, service.getRequestHeaders());
					bindings.put(useridtag, username);
					bindings.put(usergrouptag, usergroup);
					bindings.put(cachetag, sharedCache);
					bindings.put(debugstring, "");
					break;
				case REQMOD:
					bindings.put(urltag, service.getReqUrl());
					bindings.put(requestbodytag, content==null?"":content);
					bindings.put(requestheadertag, service.getRequestHeaders());
					bindings.put(useridtag, username);
					bindings.put(usergrouptag, usergroup);
					bindings.put(cachetag, sharedCache);
					bindings.put(debugstring, "");
					break;
				default: break;
			}
			String result ="";

			ScriptRunner scriptrunner = new ScriptRunner(serverScript, bindings);
			scriptrunner.join(scriptTimeout);
			String debug = bindings.get(debugstring).toString().trim();
			if (Log.fine()) if (debug != null && debug.length()>0) Log.service(Log.FINE, String.format("%1$-20s  trace log [%2$s]", name,debug));
			if (scriptrunner.isAlive()) {
				scriptrunner.forceKill();
				bindings.clear();
				errorcounter++;
				if (errorThreshold!=0 && errorcounter>errorThreshold) {
					this.setStatus(false);
					if (Log.warning()) Log.error(Log.WARNING,String.format("%1$-20s Script threshold errors exceeded : script disabled",name));
					error += "["+name+"] Script has been automatically turned to off due to "+errorcounter+" successive errors.\n";
				} else {
					if (Log.warning()) Log.error(Log.WARNING, String.format("%1$-20s  timeout - threshold of %2$6s ms exceeded - aborted - URL: [%3$s]", name,scriptTimeout,url));
				}
			} else {
				if (errorcounter>0) errorcounter--;
	
				// retrieve the potential response header and body
				// request headers and body are not updated for now (should be added for service in reqmod)
				switch (service.getType()){
					case RESPMOD:
						result = (String)(bindings.get(responsebodytag));
						service.setResponseHeaders(bindings.get(responseheadertag).toString().replace("\r\n\r\n", "\r\n").concat("\r\n"));
						break;
					case REQMOD:
						result = (String)(bindings.get(requestbodytag));
						service.setRequestHeaders(bindings.get(requestheadertag).toString().replace("\r\n\r\n", "\r\n").concat("\r\n"));
						break;
					default: break;
				}
				bindings.clear();
				if (errwriter.getBuffer().length()>0){
					if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",name, errwriter.getBuffer()));
					errwriter.flush();
				}
				content = result;
				return result;
			}
		} catch (Throwable t){
			if (errwriter.getBuffer().length()>0){
				if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",name, errwriter.getBuffer()));
				errwriter.flush();
			}
			if (Log.warning()) Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",name, t.getLocalizedMessage().trim()));
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
		Bindings _bindings;
		String scriptToRun;
		/**
		 * Launch script evaluation using a dedicated thread<br />
		 * Allows to interrupt evaluation if needed (infinite loop, timeout, ...)
		 * @param script The script to evaluate
		 * @param bindings script environment
		 */
		public ScriptRunner(String script, Bindings bindings){
			super("Jsr223SpoonScript - runner");
			this.setPriority(Thread.MIN_PRIORITY);
			this._bindings = bindings;
			this.scriptToRun = script;
			this.start();
			this.setUncaughtExceptionHandler(this);
		}
		/**
		 * Destroy running script
		 */
		@SuppressWarnings("deprecation")
		public void forceKill(){
			try{
				//heavy interruption method, required as there is no direct stop method in ScriptEngines
				//mandatory as JSR 223 threaded scripts are capricious - interrupt is not enough
				this.suspend();
				this.interrupt();
				if (this!=null && this.isAlive())this.stop();
				this.finalize();
				//done
			} catch (Exception e){
				//e.printStackTrace();
			}/*catch (java.lang.ThreadDeath td){
				//td.printStackTrace();
			}*/ catch (Throwable t){
				//t.printStackTrace();
			}

		}

		/**
		 * @see java.lang.Thread#run()
		 */
		public void run(){
			try{
				if (compiledscript!=null){
					compiledscript.eval(this._bindings);
				} else {
					engine.eval(this.scriptToRun, this._bindings);
				}
			} catch (ScriptException se){
				//se.printStackTrace();
				try {
					int line = se.getCause().getStackTrace()[0].getLineNumber();
					if (line!=-1) errwriter.append(line+":");
				} catch (Exception e){}
				errwriter.append(se.getCause().getLocalizedMessage());
				//abort
			} catch (java.lang.NoSuchMethodError nse){
				if (errwriter.getBuffer().length()>0) errwriter.append("<br />");
				int line = nse.getCause().getStackTrace()[0].getLineNumber();
				if (line!=-1) errwriter.append(line+":");
				errwriter.append("NoSuchMethodError: ").append((nse.getStackTrace()[0]).toString());
			} catch (java.lang.RuntimeException re){
				int line = re.getCause().getStackTrace()[0].getLineNumber();
				if (line!=-1) errwriter.append(line+":");
				errwriter.append(re.getLocalizedMessage());
				//abort
			} catch (Throwable t){
				errwriter.append(t.getLocalizedMessage());
			}
		}
		/**
		 * Intercept all possible thread exceptions
		 */
		public void uncaughtException(Thread arg0, Throwable t) {
			// TODO Auto-generated method stub
			errwriter.append(t.getLocalizedMessage());
		}
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Clear script parameters in order to allow a reload from file
	 */
	public void reset(){
		this.clearPatterns();
		this.compiledscript = null;
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * Update script content with new content
	 * @param newServerScript Script content to use
	 */
	public void setServerScript(String newServerScript) {
		this.serverScript = newServerScript;
		try{
			this.compiledscript = this.compilable.compile(this.serverScript);
			testScript();
		} catch (Throwable t) {
			String error = t.getCause().toString().trim().replace("<Unknown Source>", this.getScriptName());
			this.compiledscript = null;
			Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",this.getScriptName(),error));
			this.error = "Error in script ["+this.getScriptName()+"]:"+error;
		}

	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	private void testScript(){
		if (!ScriptChecker.testScriptOnChange) return;
		String err = "";
		this.errwriter.getBuffer().setLength(0);
		try {
			ScriptChecker test = new ScriptChecker(this.engine);
			//this.compiledscript = this.compilable.compile(this.serverScript);
			this.error = "";
			ScriptRunner scriptrunner = new ScriptRunner(this.serverScript,test.bindings);
			scriptrunner.join(ScriptChecker.scriptTestTimeOut);
			if (scriptrunner.isAlive()) {
				//StackTraceElement[] traces = scriptrunner.getStackTrace();
				scriptrunner.forceKill();
				this.error = "Error in script ["+this.getScriptName()+"]: infinite loop or processing time threshold exceeded";
				return;
			}
			err = this.errwriter.toString().trim();
		} catch (Throwable t){
			err = t.getCause().toString();
		}
		if (!err.equals("")){
			err = err.trim().replace("<Unknown Source>", this.getScriptName()==null?"":this.getScriptName());
			this.compiledscript = null;
			Log.service(Log.WARNING,String.format("%1$-20s Error in script : %2$s",this.getScriptName(),err));
			this.error = "Error in script ["+this.getScriptName()+"]:"+err;
		}
	}
//	<------------------------------------------------------------------------->

}


