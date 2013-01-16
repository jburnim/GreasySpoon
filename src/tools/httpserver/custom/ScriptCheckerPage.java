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
import icap.services.resources.gs.ScriptChecker;
import tools.httpserver.HttpConstants;
import org.apache.commons.lang.StringEscapeUtils;
import java.util.Hashtable;
import tools.httpserver.TextTools;
///////////////////////////////////



    
/**
 * HTML manipulation dedicated to GreasySpon service.<br>
 * Allows to display/edit script checker content.<br>
 */
public class ScriptCheckerPage implements HttpConstants{
    /** String used to indicate events*/
    static String comment = "";
    /** String used to indicate errors*/
    static String error = "";

//  <------------------------------------------------------------------------------------------>
    /**
     * Insert Scripts list into given HTML page  
     * @param content The HTML page to update
     * @return the updated HTML page
     */
    public static String setContent(String content){
        //StringBuilder sb = new StringBuilder();
        ScriptChecker sc = new ScriptChecker(icap.core.Icap.TYPE.RESPMOD);
        
        content = content.replace("%checker.enable%", ScriptChecker.isCheckEnabled()?"CHECKED":"");
        content = content.replace("%checker.timeout%", ""+ScriptChecker.getTimeOut());
        content = content.replace("%checker.userid%", sc.getUsername());
        content = content.replace("%checker.usergroup%", sc.getUsergroup());
        content = content.replace("<!--checker.requestheader-->",StringEscapeUtils.escapeHtml(sc.getRequestHeaders()));
        content = content.replace("<!--checker.requestbody-->",StringEscapeUtils.escapeHtml(sc.getRequestBody()));
        content = content.replace("<!--checker.responseheader-->",StringEscapeUtils.escapeHtml(sc.getResponseHeaders()));
        content = content.replace("<!--checker.responsebody-->",StringEscapeUtils.escapeHtml(sc.getResponseBody()));
        content = content.replace("<!-- comment -->", comment);
        content = content.replace("<!-- error -->", error);
        comment = ""; error = "";
        return content;
    }
//  <------------------------------------------------------------------------------------------>

    
//  <------------------------------------------------------------------------------------------>
    /**
     * Update GreasySpoon service scripts based on provided parameters
     * @param params_values	String table with each line composed of "param=value"
     */
    public static void update(String[] params_values){
    	boolean checkeradded = false;
    	Hashtable<String,String> attributes = new Hashtable<String,String>();
    	for (String pv:params_values){
    		int pos = pv.indexOf("=");
    		if (pos==-1) continue;
    		String k = pv.substring(0,pos).trim();
    		String v = TextTools.hexToUTF8(pv.substring(pos+1).trim()).replace("+", " ");
    		if (k.equalsIgnoreCase("submit")) continue;
    		if (k.equalsIgnoreCase("checker.enable")) {
    			checkeradded = true;
    		}
    		attributes.put(k,v); 
    	}
    	if (!checkeradded){
    		attributes.put("checker.enable","false");
    	}
    	ScriptChecker.updateParameters(attributes);
    }
//  <------------------------------------------------------------------------------------------>
    
    
 
}
