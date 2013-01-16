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
import java.net.*;
///////////////////////////////////


/**
 * HTTP server administration pages<br>
 * Allows to consult/modify administration parameters<br>
 * Switch from http to https requires:
 * <ul>
 * 	<li>generating a SSL key and storing it in a keystore.</li>
 *  <li>starting server with  switches "-Djavax.net.ssl.trustStore=keystore -Djavax.net.ssl.keyStore=keystore -Djavax.net.ssl.keyStorePassword="password"</li>
 * </ul>
 */
public class PageAdmin {
    static String comment = "";
    static String error = "";
    static boolean restart = false;
    
//  <------------------------------------------------------------------------------------------>
    /**
     * @return true if administration server must be restarted
     */
    public static boolean mustRestart(){
        return restart;
    }
//  <------------------------------------------------------------------------------------------>

    /**
     * @return Update administration server parameter and return server home page URL
     */
    public static String getRestartCommand(){
        restart = false;
        StringBuilder sb = new StringBuilder();
        if (HttpServer.overSSL) sb.append("https://"); else sb.append("http://");
        if (HttpServer.bounded_ip==null || HttpServer.bounded_ip.equals("")){
            try {
                InetAddress addr = InetAddress.getLocalHost();
                sb.append(addr.getHostName());
            } catch (UnknownHostException e) {
            	// unable to find ip => set to empty
            }
        } else {
            sb.append(HttpServer.bounded_ip);
        }
        sb.append(":").append(HttpServer.port);
        sb.append("/index.html");
        return sb.toString();
    }

    
//  <------------------------------------------------------------------------------------------>
    /**
     * Update Admin HTML page content with administration parameters 
     * @param content Admin HTML page content to update
     * @return Admin HTML page content updated with current values
     */
    public static String set(String content){
        String[] ips = new String[]{"","","",""};
        if (HttpServer.bounded_ip!=null && (!HttpServer.bounded_ip.equals(""))){
            ips = HttpServer.bounded_ip.split("\\.");
        }
        if (ips.length==4){
            for (int i=1; i<5; i++){
                content = content.replace("%admin.ipbounded"+i+"%",ips[i-1]);    
            }
        } else {
        	for (int i=1; i<5; i++){
                content = content.replace("%admin.ipbounded"+i+"%","");    
            }
        }
        if (!comment.equals("")){
            content = content.replace("<!-- comment -->", comment);
            comment="";
        }

        if (!error.equals("")){
            content = content.replace("<!-- error -->", error);
            error="";
        }
        
        content = content.replace("%admin.htmlpath%", HttpServer.path_To_Files);
        content = content.replace("%admin.port%", ""+HttpServer.port);
        content = content.replace("%admin.backlog%", ""+HttpServer.backlog);
        content = content.replace("%admin.threads%", ""+HttpServer.nb_threads);
        if (HttpServer.overSSL) {
            content = content.replace("%SSLCHECKED%", "CHECKED");
        } else {
            content = content.replace("%SSLCHECKED%", "");
        }
        return content;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Update Admin parameters with Admin html page form values
     * @param params form values as modified by user
     */
    public static void update(String[] params){
        restart = false;
        boolean resetcertiticate = false;
        try{
            String ip = "";
            String  port="";
            String  htmlpath="";
            String  ssl="";
            String  pwd="";
            String  pwd1="";
            String  pwd2="";
            boolean updatepwd = false;
            
            int i=0;
            for (String str:params){
                if (str.trim().length()==0) continue;
                String[] values = str.split("=");
                if (values.length!=2) continue;
                if (values[0].startsWith("admin.ipbounded")){
                    i++;
                    if (i<4) ip += values[1].trim()+".";
                    else ip += values[1].trim();
                } else if (values[0].equals("admin.port")){
                    port = values[1].trim();
                } else if (values[0].equals("admin.htmlpath")){
                    htmlpath = TextTools.hexToUTF8(values[1].trim());
                } else if (values[0].equals("admin.ssl")){
                    ssl = values[1].trim();
                } else if (values[0].equals("admin.pwd")){
                    updatepwd = true;
                    pwd = TextTools.hexToUTF8(values[1].trim());
                } else if (values[0].equals("admin.pwd1")){
                    updatepwd = true;
                    pwd1 = TextTools.hexToUTF8(values[1].trim());
                } else if (values[0].equals("admin.pwd2")){
                    pwd2 = TextTools.hexToUTF8(values[1].trim());
                } 
            }
            
            if (ip.equals("....")) ip = "";
            if (!ip.equals(HttpServer.bounded_ip)){
                HttpServer.bounded_ip = ip;
                restart = true;
                resetcertiticate = true;
            }
            if (!htmlpath.equals(HttpServer.path_To_Files)){
                HttpServer.path_To_Files =htmlpath; 
            }
            if(!port.equals(""+HttpServer.port)){
                HttpServer.port = Integer.parseInt(port);
                restart = true;
            }

            if((ssl.equals("") && HttpServer.overSSL) || ssl.equals("on")&& !HttpServer.overSSL){
                HttpServer.overSSL = !HttpServer.overSSL;
                restart = true;
            }
            
            if (updatepwd){
                if (!pwd1.equals(pwd2)){
                    error = "New passwords are not identical.";
                    restart = false;
                    return;
                }
                if (!HttpServer.checkPwd(pwd)){
                    error = "Invalid password";
                    restart = false;
                    return;
                }
                HttpServer.setPassword(pwd, pwd1);
                restart = true;
                resetcertiticate = true;
            }
            
            HttpServer.saveConf(
              new String[]{"admin.pwd","admin.ipbounded","admin.port","admin.htmlpath","admin.backlog",
            		  "admin.threads","admin.ssl", 
            		 }
            , new String[]{HttpServer.getEncryptedPassword(),HttpServer.bounded_ip,""+HttpServer.port,
            		  HttpServer.path_To_Files,""+HttpServer.backlog,
            		  ""+HttpServer.nb_threads,Boolean.toString(HttpServer.overSSL),
            		  }
            );
            if (restart) HttpServer.self.restart(resetcertiticate);

        } catch (Exception e){
            error = "Invalid parameter: ["+e.toString()+"]";
            restart = false;
        }
        if (!restart) comment = "Changes Committed";
    }
//  <------------------------------------------------------------------------------------------>


}
