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
 * Created	:	7 dec. 06
 *---------------------------------------------------------------------------*/
package tools.httpserver;


///////////////////////////////////
//Import
import tools.logger.Log;
import java.util.logging.Level;
import java.io.*;
///////////////////////////////////

/**
 * Logs administration page.<br>
 * Allows to configure log parameters (path/level)
 */
public class PageLogs {
    static String comment = "";
    static String error = "";
//  <------------------------------------------------------------------------------------------>
    /**
     * Generate configuration page for logging<br>
     * <!--loglevels--> will be replaced by configuration menu 
     * @param content HTML page to update with log configuration code
     * @return updated HTML page 
     */
    public static String set(String content){

        if (!comment.equals("")){
            content = content.replace("<!-- comment -->", comment);
            comment="";
        }

        if (!error.equals("")){
            content = content.replace("<!-- error -->", error);
            error="";
        }
        content = content.replace("%log.path%",Log.getLogPath());
        content = content.replace("%log.maxentries%",""+Log.getMaxentries());
        content = content.replace("%log.maxfiles%",""+Log.getMaxfiles());
        
        Level level = Log.getLogLevel();
        String[] levels = new String[]{"OFF","SEVERE","WARNING","INFO","CONFIG","FINE","FINER","FINEST","ALL"};
        String[] comments = new String[]{
        		"OFF is a special level that can be used to turn off logging"
        		,"SEVERE is a message level indicating a serious failure"
        		,"WARNING is a message level indicating a potential problem"
        		,"INFO is a message level for informational messages"
        		,"CONFIG is a message level for static configuration messages"
        		,"FINE is a message level providing tracing information"
        		,"FINER indicates a fairly detailed tracing message"
        		,"FINEST indicates a highly detailed tracing message"
        		,"ALL indicates that all messages should be logged - VERY verbose mode"};

        StringBuilder loglevels=new StringBuilder();
        for (int i=0; i<levels.length;i++){
            if (Level.parse(levels[i]).equals(level)){
                loglevels.append("<option class=\"item\" title=\"").append(comments[i])
                    .append("\" selected value=\"")
                    .append(levels[i]).append("\">").append(levels[i]).append("</option>\r\n");
            } else {
                loglevels.append("<option class=\"item\" title=\"").append(comments[i])
                    .append("\" value=\"")
                .append(levels[i]).append("\">").append(levels[i]).append("</option>\r\n");
            }
        }
        
        content = content.replace("<!--loglevels-->",loglevels.toString());

        content = content.replace("%log_access_enabled%", Log.isAccessEnabled()?"CHECKED":"");
        content = content.replace("%log_error_enabled%", Log.isErrorEnabled()?"CHECKED":"");
        content = content.replace("%log_service_enabled%", Log.isServiceEnabled()?"CHECKED":"");
        content = content.replace("%log_admin_enabled%", Log.isAdminEnabled()?"CHECKED":"");
        content = content.replace("%log_debug_enabled%", Log.isDebugEnabled()?"CHECKED":"");
        
        return content;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Update DataMenu to only show activated logs in interface 
     * @param content The data menu content
     * @return updated data menu
     */
    public static String updateLogMenu(String content){
        content = content.replace("<!--accesslog-->",Log.isAccessEnabled()?"<div id='accesslog'><a href ='showlog.html?access.log' target ='main'>Access Log</a></div>":"");
        content = content.replace("<!--errorlog-->", Log.isErrorEnabled()?"<div id='errorlog'><a href ='showlog.html?error.log' target ='main'>Error Log</a></div>":"");
        content = content.replace("<!--servicelog-->", Log.isServiceEnabled()?"<div id='servicelog'><a href ='showlog.html?service.log' target ='main'>Service Log</a></div>":"");
        content = content.replace("<!--adminlog-->", Log.isAdminEnabled()?"<div id='adminlog'><a href ='showlog.html?admin.log' target ='main'>Admin Log</a></div>":"");
        content = content.replace("<!--debuglog-->", Log.isDebugEnabled()?"<div id='debuglog'><a href ='showlog.html?debug.log' target ='main'>Debug Log</a></div>":"");
    	return content;
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Update logging configuration based on provided parameters
     * @param params user provided parameters
     */
    public static void update(String[] params){
       try{
            
            String  path="";
            String level = "";
            int maxfiles = Log.getMaxfiles();
            int maxentries = Log.getMaxentries();
            boolean tosave=false;
            boolean log_access_enabled=false,
            	log_error_enabled=false,log_admin_enabled=false,
            	log_service_enabled=false, log_debug_enabled=false;
            
            
            for (String str:params){
                if (str.trim().length()==0) continue;
                String[] values = str.split("=");
                if (values.length!=2) continue;
                if (values[0].equals("log.path")){
                    path = TextTools.hexToUTF8(values[1].trim());
                } else if (values[0].equals("log.level")){
                    level = values[1].trim().toUpperCase();
                }else if (values[0].equals("log.maxentries")){
                	maxentries = Integer.parseInt(values[1].trim());
                }else if (values[0].equals("log.maxfiles")){
                	maxfiles = Integer.parseInt(values[1].trim());
                } else if (values[0].equals("log.access.enabled")){
                	log_access_enabled = true;
                } else if (values[0].equals("log.error.enabled")){
                	log_error_enabled = true;
                }else if (values[0].equals("log.admin.enabled")){
                	log_admin_enabled = true;
                }else if (values[0].equals("log.service.enabled")){
                	log_service_enabled = true;
                }else if (values[0].equals("log.debug.enabled")){
                	log_debug_enabled = true;
                }
            }
            if (log_access_enabled != Log.isAccessEnabled()){
            	Log.setAccessEnabled(log_access_enabled); tosave = true;
            }
            if (log_error_enabled != Log.isErrorEnabled()){
            	Log.setErrorEnabled(log_error_enabled); tosave = true;
            }
            if (log_admin_enabled != Log.isAdminEnabled()){
            	Log.setAdminEnabled(log_admin_enabled); tosave = true;
            }
            if (log_service_enabled != Log.isServiceEnabled()){
            	Log.setServiceEnabled(log_service_enabled); tosave = true;
            }
            if (log_debug_enabled != Log.isDebugEnabled()){
            	Log.setDebugEnabled(log_debug_enabled); tosave = true;
            }
            
            if (!path.equals(Log.getLogPath())){
                File file = new File(path);
                if (!file.isDirectory() || !file.exists()){
                    error = "Path does not exist or is invalid.";
                    return;
                }
                Log.setLogPath(path);
                tosave = true;
            }
            
            if (!Level.parse(level).equals(Log.getLogLevel())){
                Log.setLogLevel(level);
                tosave = true;
            }
            if (maxfiles != Log.getMaxfiles()){
                Log.setMaxfiles(maxfiles);
                tosave = true;
            }
            if (maxentries != Log.getMaxentries()){
                Log.setMaxentries(maxentries);
                tosave = true;
            }
            if (tosave) {
                HttpServer.saveConf(new String[]{
                	"log.access.enabled"
                	,"log.error.enabled"
                	,"log.admin.enabled"
                	,"log.service.enabled"
                	,"log.debug.enabled"
                    ,"log.path"
                    ,"log.level"
                    ,"log.maxfiles"
                    ,"log.maxentries"
                },new String[]{
                	log_access_enabled?"on":"off"
                	,log_error_enabled?"on":"off"
                	,log_admin_enabled?"on":"off"
                	,log_service_enabled?"on":"off"
                	,log_debug_enabled?"on":"off"
                    ,path
                    ,level
                    ,""+maxfiles
                    ,""+maxentries
                });
                comment = "Changes Committed";
            }
            
        } catch (Exception e){
            if (Log.fine()) Log.trace(Level.FINE, e);
            error = "Invalid Parameter(s)";
        }
       
    }
//  <------------------------------------------------------------------------------------------>


}
