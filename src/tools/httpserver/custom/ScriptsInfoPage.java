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

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import tools.httpserver.InfoPage;
import icap.services.GreasySpoon;
import icap.services.resources.gs.SpoonScript;


/**
 * Generate HTML content dedicated to GreasySpoon service<br>
 * Includes some system parameters: OS, architecture, memory<br>
 * Replace any HTML &lt;!--versions--> tag by generated HTML info
 */
public class ScriptsInfoPage {

    static String headclass = "section";
    static String itemclass = "item";
    
//  -----------------------------------------------------------------------------    
    /**
     * @return HTML description for scripts application
     */
    public static String getText(){
        StringBuilder content = new StringBuilder();

        content.append("<div class=\""+headclass+"\">");
        content.append("Scripts");
        content.append("</div>\r\n");
        content.append("<div class=\""+itemclass+"\">");
        content.append(getScriptsParameters());
        content.append("</div>\r\n<br /><br />");
        
        content.append("<div class=\""+headclass+"\">");
        content.append("Languages");
        content.append("</div>\r\n");
        content.append("<div class=\""+itemclass+"\">");
        content.append(getEnginesParameters());
        content.append("</div>\r\n<br /><br />");

        content.append("<div class=\""+headclass+"\">");
        content.append("Locale");
        content.append("</div>\r\n");
        content.append("<div class=\""+itemclass+"\">");
        content.append(InfoPage.getLocaleParameters());
        content.append("</div>\r\n<br /><br>");

        /*content.append("<div class=\""+headclass+"\">");
        content.append("Java Runtime");
        content.append("</div>\r\n");
        content.append("<div class=\""+itemclass+"\">");
        content.append(InfoPage.getJavaParameters());
        content.append(InfoPage.getMemoryParameters());
        content.append("</div>\r\n<br /><br>");
        */
        return content.toString();
    }
//  -----------------------------------------------------------------------------
    
//  -----------------------------------------------------------------------------
    /**
     * @return HTML string with scripts description
     */
    public static String getScriptsParameters(){
        StringBuilder stb = new StringBuilder();
        stb.append("Scripts running on requests:\t").append(ScriptList.getReqmodScriptsNumber()).append("<br />\r\n");
        stb.append("Scripts running on responses:\t").append(ScriptList.getRespmodScriptsNumber()).append("<br />\r\n");
        stb.append("<br />");
        stb.append("<span title='Scripts exceeding this threshold will be AUTOMATICALLY turned off'>Maximum successive script errors/timeout allowed:\t").append(SpoonScript.getErrorThreshold()==0?" Infinite" : SpoonScript.getErrorThreshold()).append("</span><br />\r\n");
        stb.append("Maximum processing time allowed before aborting:\t").append(SpoonScript.getScriptMaxTimeout()).append(" ms<br />\r\n");
        stb.append("Action on error:\t").append(GreasySpoon.bypassOnError?" BYPASS":" Returns ICAP_ERROR").append("<br />\r\n");
        stb.append("<br />");
        stb.append("Processed Mime Types:\t");
        for (String s:GreasySpoon.getSupportedContentTypes()){
        	stb.append(s).append(";&nbsp;");
        }
        stb.append("<br />\r\n");
        stb.append("Verify Mime Types using MimeMagic:\t").append(GreasySpoon.isMimemagiccheck()?" Enabled":" Disabled").append("<br />\r\n");
        if (GreasySpoon.isMimemagiccheck()) stb.append("Trust server content-type if MimeMagic fails:\t").append(GreasySpoon.isTrustServiceMimeTypePerDefault()?" Yes":" No").append("<br />\r\n");
        stb.append("<br />");
        stb.append("Optimize (compress) content if browser supports it:\t").append(GreasySpoon.isCompressanytime()?" Enabled":" Disabled").append("<br />\r\n");
        if (GreasySpoon.isCompressanytime()) {
        	stb.append("Compressed content:\t");
            for (String s:GreasySpoon.getCompressibleContentTypes()){
            	stb.append(s).append(";&nbsp;");
            }
            stb.append("<br />\r\n");
        }
        return stb.toString();
    }
//-----------------------------------------------------------------------------

//  -----------------------------------------------------------------------------
    /**
     * @return HTML string with scripts description
     */
    public static String getSharedCacheInfo(){
        StringBuilder stb = new StringBuilder();
        stb.append("Scripts running on requests:\t").append(ScriptList.getReqmodScriptsNumber()).append("<br />\r\n");
        stb.append("Scripts running on responses:\t").append(ScriptList.getRespmodScriptsNumber()).append("<br />\r\n");
        stb.append("Maximum successive errors allowed:\t").append(SpoonScript.getErrorThreshold()).append("<br />\r\n");
        stb.append("Maximum processing time allowed before aborting:\t").append(SpoonScript.getScriptMaxTimeout()).append(" ms<br />\r\n");
        return stb.toString();
    }
//-----------------------------------------------------------------------------
    
//  -----------------------------------------------------------------------------
    /**
     * @return HTML string with ScriptEngines description
     */
    public static String getEnginesParameters(){
        StringBuilder stb = new StringBuilder();
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		for (ScriptEngineFactory factories : scriptEngineManager.getEngineFactories()){
	        stb.append("\t").append(factories.getEngineName())
	        	.append(" - language:\t").append(factories.getLanguageName())
	        	.append(" - version:\t").append(factories.getLanguageVersion())
	        	.append("<br>\r\n");			
		}
		//Add GS Java scripts
		if (javax.tools.ToolProvider.getSystemJavaCompiler()!=null){
			stb.append("\t").append("Native Gs Scripts")
	    	.append(" - language:\t").append("Java")
	    	.append(" - version:\t").append(System.getProperty("java.vm.version"))
	    	.append("<br />\r\n");
		}
        return stb.toString();
    }
//-----------------------------------------------------------------------------
}
