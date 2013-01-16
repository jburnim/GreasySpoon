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
 *-----------------------------------------------------------------------------*/
package tools.httpserver.custom;

///////////////////////////////////
//Import
import icap.services.resources.gs.JavaSpoonScript;
import icap.services.resources.gs.SpoonScript;
import icap.core.Icap;
import java.io.*;
import java.util.*;
import javax.script.*;
import tools.httpserver.HttpConstants;
import tools.httpserver.TextTools;
import tools.httpserver.User;
import org.apache.commons.lang.StringEscapeUtils;
///////////////////////////////////



    
/**
 * HTML manipulation dedicated to GreasySpon service.<br>
 * Generate HTML tags for GreasySpoon scripts list.<br>
 * Replace &lt;!--content--> tag by generated HMTL list.  
 */
public class ScriptList implements HttpConstants{
    /** String used to indicate events*/
    private static String comment = "";
    /** String used to indicate errors*/
    private static String error = "";
    
    
    //public static ScriptList self;

//  <------------------------------------------------------------------------------------------>
    /**
     * Insert Scripts list into given HTML page  
     * @param content The HTML page to update
     * @param requestmode Set if page is listing request mode script or not (if not, means response scripts)
     * @param user User who made the request
     * @return the updated HTML page
     */
    public static String setScripts(String content, boolean requestmode, User user){
        StringBuilder sb = new StringBuilder();
        SpoonScript script;
        SpoonScript[] scripts;
        if (requestmode) scripts = icap.services.GreasySpoon.reqSpoonScripts;
        else scripts = icap.services.GreasySpoon.respSpoonScripts;
        int counter = -1;

        for (int i=0; i<scripts.length; i++){
        	script = scripts[i];
        	counter++;
        	if (script.getPendingErrors().length()!=0){
        		error+=StringEscapeUtils.escapeHtml(script.getPendingErrors()).replace("\n", "<br />")+"<br />";
        	}
            sb.append("<tr class=\"tablecontent\">\r\n");
            //del checkbox
            sb.append("<td><input name=\"delete_").append(script.getFile().getName().toLowerCase()).append("\"");
            if (!allowed(user.getRights(), script.getRights()))sb.append(" disabled"); 
            sb.append(" value=\"").append(script.getScriptName().toLowerCase())
            	.append("\" type=\"checkbox\"></td>\r\n");
            //enabled checkbox
            sb.append("<td><input name=\"enable_").append(script.getFile().getName().toLowerCase()).append("\"");
            if (user.getRights()==RIGHTS.NONE) sb.append(" disabled");
            sb.append(" value=\"")
            	.append(script.getScriptName().toLowerCase()).append("\" type=\"checkbox\"")
            	.append(script.getStatus()?"CHECKED":"").append("></td>\r\n");
            //edit link
            sb.append("<td><a name=\"").append(script.getScriptName()).append("\" href=\"editfile.html?filename=")
            	.append(TextTools.utf8ToHex(script.getFile().getAbsolutePath()).replace(" ", "+"))
            	.append("\">Edit</a></td>\r\n");
            // rule number
            sb.append("<td>").append(counter).append("</td>\r\n");
            //name
            sb.append("<td>").append(script.getScriptName()).append("</td>\r\n");
            //language
            sb.append("<td>").append(script.getLanguage()).append("</td>\r\n");
            //description
            sb.append("<td>").append(script.getDescription()).append("</td>\r\n");
        }
        content = content.replace("<!--content-->", sb.toString());
        content = content.replace("<!-- comment -->", comment);
        content = content.replace("<!-- error -->", error);
        comment = ""; error = "";
        return content;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Check if user rights are enough to manipulate file
     * @param userrights User rights
     * @param filerights File attached rights
     * @return true if user is allowed to manipulate file
     */
    public static boolean allowed(RIGHTS userrights, RIGHTS filerights){
    	if (userrights==RIGHTS.NONE) return false; 
    	if (userrights==RIGHTS.ADMIN) return true;
    	if (userrights==filerights) return true;
    	if (filerights==RIGHTS.ADMIN) return false;
    	if (filerights==RIGHTS.NONE && userrights==RIGHTS.USER) return true;
    	return false;
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * @return number of REQMOD scripts
     */
    public static int getReqmodScriptsNumber(){
    	return icap.services.GreasySpoon.reqSpoonScripts.length;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * @return number of RESPMOD scripts 
     */
    public static int getRespmodScriptsNumber(){
    	return icap.services.GreasySpoon.respSpoonScripts.length;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Check pending errors for given script
     * @param filename The script filename (as stored on disk) to check
     * @return Pending error or empty script if none
     */
    public static String checkScriptErrors(String filename){
        SpoonScript script;
        try{
	        File scriptfile = new File(filename);
	        Vector<SpoonScript> scripts = new Vector<SpoonScript>(Arrays.asList(icap.services.GreasySpoon.reqSpoonScripts));
	        scripts.addAll(Arrays.asList(icap.services.GreasySpoon.respSpoonScripts));
	        for (int i=0; i<scripts.size(); i++){
	        	script = scripts.elementAt(i);
	        	if (script.getFile().equals(scriptfile)){
	        		return script.getPendingErrors();
	        	}
	        }
        }catch (Exception e){
        	//e.printStackTrace();
        }
        return "";
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Force script refresh 
     * @param filename The script to reload
     * @return Pending error(s) if any, empty string otherwise
     */
    public static String refreshScript(String filename){
        SpoonScript script;
        try{
	        File scriptfile = new File(filename);
	        Vector<SpoonScript> scripts = new Vector<SpoonScript>(Arrays.asList(icap.services.GreasySpoon.reqSpoonScripts));
	        scripts.addAll(Arrays.asList(icap.services.GreasySpoon.respSpoonScripts));
	        for (int i=0; i<scripts.size(); i++){
	        	script = scripts.elementAt(i);
	        	if (script.getFile().equals(scriptfile)){
	        		script.refresh();
	        		return script.getPendingErrors();
	        	}
	        }
        }catch (Exception e){
        	return e.getLocalizedMessage();
        }
        return "";
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * @return Shared cache Information
     */
    public static String getSharedCacheInfo(){
    	int sharedcacheSize = SpoonScript.getSharedCacheSize();
    	return ""+sharedcacheSize;
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Update GreasySpoon service scripts based on provided parameters
     * @param params_values	String table with each line composed of "param=value"
     * @param requestmode Set if information concerns REQMOD scripts or not (if not, means RESPMOD scripts)
     */
    public static void update(String[] params_values, boolean requestmode){
    	Hashtable<String, String> table = new Hashtable<String, String>();
        for (String s:params_values){
            String[] values = s.split("=");
            if (values.length==1) {
                table.put(values[0], "");
            } else if (values.length==2){
	            values[0] = values[0].replace("+", " ");
	            table.put(values[0], values[1] );
            }
        }
        Vector<SpoonScript> todelete = new Vector<SpoonScript>();
        Vector<SpoonScript> scripts = new Vector<SpoonScript>(Arrays.asList(
        		requestmode? icap.services.GreasySpoon.reqSpoonScripts
        				:icap.services.GreasySpoon.respSpoonScripts));
        
        boolean deleted = false;
        for (SpoonScript script:scripts){
       	
        	String name = script.getFile().getName().toLowerCase();
        	if (table.get("delete_"+name)!=null){
        		todelete.add(script);
        		deleted = true;
        		continue;
        	}
        	boolean newstatus = false;
        	if (table.get("enable_"+name)!=null){
        		newstatus = true;
        	}
        	if (script.getStatus()!=newstatus){
        		script.setStatus(newstatus);
        		script.saveWithNewStatus();
        	}
        }
        for (SpoonScript script:todelete){
        	scripts.remove(script);
        	script.getFile().delete();
			if (script.getFile().getName().endsWith(".java")){
				String classname = script.getFile().getParent() +"/nativejava/" + ((JavaSpoonScript)script).getInternalName() + ".class";
				new File(classname).delete();
			}
        }
        if (deleted){
        	if (requestmode) {
        		icap.services.GreasySpoon.reqSpoonScripts = scripts.toArray(new SpoonScript[0]);
        	} else {
        		icap.services.GreasySpoon.respSpoonScripts  = scripts.toArray(new SpoonScript[0]);
        	}
        }

    }
//  <------------------------------------------------------------------------------------------>
    
   
//  <------------------------------------------------------------------------------------------>
    /**
     * Insert info to create a new script into HTML page 
     * @param content HTML page to update 
     * @return HTML page updated with script creation menu
     */
    public static String newScript(String content){
        content = content.replace("<!-- comment -->", comment);
        content = content.replace("<!-- error -->", error);
        comment = ""; error = "";
        Vector<String[]> engines = getScriptEngines();
        StringBuilder listlanguague=new StringBuilder();

        for (int i=0; i<engines.size();i++){
        	if (engines.elementAt(i)[0].equalsIgnoreCase("xslt") || engines.elementAt(i)[0].equalsIgnoreCase("xpath")){
            	listlanguague.append("<option class=\"item\" title=\"script.language\" value=\"")
                .append(engines.elementAt(i)[0]).append("\">")
                .append(engines.elementAt(i)[0]).append("</option>\r\n");
        	} else {
        		listlanguague.append("<option class=\"item\" title=\"script.language\" value=\"")
        			.append(engines.elementAt(i)[0]).append("\">")
        			.append(engines.elementAt(i)[1])
        			.append("</option>\r\n");
        	}
        }
        content = content.replace("<!--languagelist-->",listlanguague.toString());
        return content;
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Create a new empty SpoonScript file using provided parameters
     * @param params_values parameters to use for file creation (table containing "param=value" strings)
     * @param type Set if script concerns REQMOD or RESPMOD
     * @param user User who generates the request
     * @return Action to do after: edit the new file if all is ok, return error otherwise
     */
    public static String create(String[] params_values, Icap.TYPE type, User user){
    	String name = null;
    	String language = null;
    	
        for (String s:params_values){
            String[] values = s.split("=");
            if (values.length!=2) continue;
            if (values[0].equals("script.name")) name = values[1].replace("+"," ").trim();
            if (values[0].equals("script.language")) language= TextTools.hexToUTF8(values[1]).trim();
        }
        if (name == null || language == null){
        	error = "Invalid script name or language";
        	return "";
        }
        SpoonScript[] scripts = type==Icap.TYPE.REQMOD?icap.services.GreasySpoon.reqSpoonScripts:icap.services.GreasySpoon.respSpoonScripts;
        for (SpoonScript script:scripts){
        	if (script.getScriptName().equalsIgnoreCase(name)){
            	error = "A script with this name already exists.";
            	return "";
        	}
        }
        
        ScriptEngineFactory factory = null;
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		for (ScriptEngineFactory factories : scriptEngineManager.getEngineFactories()){
			if (factories.getEngineName().equalsIgnoreCase(language)){
				factory = factories;
				break;
			}
		}
		String factorylanguage;
		String extension;
		if (factory==null){
			if (language.equals("java")){
				factorylanguage = "java";
				extension = "java";
			} else {
				error = "Provided language cannot be founded.";
				return "";
			}
		} else {
			factorylanguage = factory.getLanguageName();
			extension = factory.getExtensions().get(0);
		}
       
        String scriptdirectory = icap.services.GreasySpoon.scriptsDirectory;
        String commentstring= icap.services.GreasySpoon.languageComments.getProperty(factorylanguage,"//").trim();
        String tag;
        if (type == Icap.TYPE.REQMOD){
        	tag = icap.services.GreasySpoon.greasySpoonReqTag;
        } else {
        	tag = icap.services.GreasySpoon.greasySpoonRespTag;
        }
        String filecomment = SpoonScript.getScriptSkeleton(type, extension,commentstring);
		
    	String filename = scriptdirectory+File.separator+name.replace(" ", "_").replace("%20", "_")+tag+extension;

        SpoonScript.save(filename, filecomment.replace("%name%", name), user.getRights());
       	icap.services.GreasySpoon.forceReload();
        return "edit="+filename;
     }
//  <------------------------------------------------------------------------------------------>
    
    
//  <------------------------------------------------------------------------------------------>
    /**
     * @return a vector containing Scriptengines names and associated languages
     */
    public static Vector<String[]> getScriptEngines(){
    	Vector<String[]> vector = new Vector<String[]>();
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		for (ScriptEngineFactory factories : scriptEngineManager.getEngineFactories()){
			boolean inserted = false;
			for (int i=0; i<vector.size(); i++){
				String[] s = vector.elementAt(i);
				if (s[0].compareToIgnoreCase(factories.getEngineName())>0){
					vector.insertElementAt(new String[]{factories.getEngineName(),factories.getLanguageName()}, i);
					inserted = true;
					break;
				}
			}
			if (!inserted) vector.add(new String[]{factories.getEngineName(),factories.getLanguageName()});
		}
		if (javax.tools.ToolProvider.getSystemJavaCompiler()!=null){
			vector.add(new String[]{"java","java"});
		}
		return vector;
	}
//  <------------------------------------------------------------------------------------------>
    
    
 
}
