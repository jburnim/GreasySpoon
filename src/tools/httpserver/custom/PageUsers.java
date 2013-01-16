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
import java.util.logging.Level;
import tools.httpserver.HttpConstants;
import tools.httpserver.HttpServer;
import tools.httpserver.TextTools;
import tools.httpserver.User;
import tools.logger.*;
import org.apache.commons.lang.StringEscapeUtils;
///////////////////////////////////
    
/**
 * HTML manipulation dedicated to GreasySpon service.<br>
 * Generate HTML tags for GreasySpoon scripts list.<br>
 * Replace &lt;!--content--> tag by generated HMTL list.  
 */

public class PageUsers implements HttpConstants{
    /** String used to indicate events*/
    static String comment = "";
    /** String used to indicate errors*/
    static String error = "";
    
    
    //public static ScriptList self;

//  <------------------------------------------------------------------------------------------>
    /**
     * Insert Scripts list into given HTML page  
     * @param content The HTML page to update
     * @return the updated HTML page
     */
    public static String setUsers(String content){
        content = content.replace("<!-- comment -->", comment);
        content = content.replace("<!-- error -->", error);
        comment = ""; error = "";
        
        StringBuilder sb = new StringBuilder();
        User user;
        RIGHTS[] rights = RIGHTS.values();
        for (int i=0; i<HttpServer.getUsers().size(); i++){
        	
        	user = HttpServer.getUsers().elementAt(i);
 
            sb.append("<tr>\r\n");
            sb.append("<td><input value=\""+i+"\" name=\"del\" type=\"checkbox\"></td>\r\n");
            sb.append("<td><input READONLY size=\"25\" name=\"login_"+i+"\" type=\"text\" value=\""+StringEscapeUtils.escapeHtml(user.getLogin())+"\"></td>\r\n");
            sb.append("<td><input size=\"25\" name=\"pwd_"+i+"\" type=\"password\" value=\"\"></td>\r\n");
            sb.append("<td><select name=\"type_"+i+"\">");
            
            for (int j=0; j<rights.length;j++){
                if (rights[j].equals(user.getRights())){
                    sb.append("<option class=\"item\"")
                        .append("selected value=\"")
                        .append(rights[j]).append("\">").append(rights[j]).append("</option>\r\n");
                } else {
                    sb.append("<option class=\"item\"")
                    .append("value=\"")
                    .append(rights[j]).append("\">").append(rights[j]).append("</option>\r\n");
                }
            }
            
            //<option value=\"0\">USER</option><option value=\"1\">ADMIN</option><option value=\"2\">NONE</option></select></td>\r\n");
            sb.append("</select><td>\r\n</tr>\r\n");
        }
        content = content.replace("<!-- users -->", sb.toString());
        return content;
    }
//  <------------------------------------------------------------------------------------------>
    
 
//  <------------------------------------------------------------------------------------------>
    /**
     * Update users based on provided parameters
     * @param params String table with each line composed of "param=value"
     */
    public static void update(String[] params){
        try{
            String login="", password="", id="";
            RIGHTS rights = RIGHTS.NONE;
            boolean del=false, tosave=false;

            for (int i=0; i<params.length; i++){
                password=""; login="";rights = RIGHTS.NONE;id="";
                del=false;
                String[] pv = params[i].split("=");
                if (pv.length!=2) continue;
                if (!pv[0].startsWith("login_")) continue;
                login = TextTools.hexToUTF8(pv[1]);
                id=pv[0].substring(pv[0].indexOf("_")+1);
                for (int j=0; j<params.length; j++){
                    String[] pv2 = params[j].split("=");
                    if (pv2.length!=2) continue;
                    if (pv2[0].equals("del") && TextTools.hexToUTF8(pv2[1]).equals(id)){
                        del = true;
                        continue;
                    } else if (TextTools.hexToUTF8(pv2[0]).equals("pwd_"+id)){
                        password = TextTools.hexToUTF8(pv2[1]).replace("+", " ");
                    } else if (TextTools.hexToUTF8(pv2[0]).equals("type_"+id)){
                        rights = RIGHTS.valueOf(TextTools.hexToUTF8(pv2[1]).replace("+", " "));
                    }
                }
                //ok, we've got the profile
                if (del){
                    HttpServer.delUser(login);
                    tosave = true;
                    continue;
                }
                User user = HttpServer.getUser(login);
                if (user==null) {
                	HttpServer.addUser(login, password, rights);
                	tosave = true;
                	continue;
                }

                if (!password.equals("")) {
                	user.setPwd(password);
                    tosave = true;
                    continue;
                }
                if (rights != user.getRights()) {
                	user.setRights(rights);
                    tosave = true;
                }
            }
            if (tosave){
                HttpServer.saveUsers();
            }
            comment = "Changes commited.";
            } catch (Exception e){
                if (Log.fine()) Log.trace(Level.FINE, e);
                error = e.toString();
            }
    }
//  <------------------------------------------------------------------------------------------>
    
    
    
 
}
