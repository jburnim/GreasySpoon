/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008,2009 Karel Mittig
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
import icap.core.Icap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.Vector;
import tools.httpserver.*;
import tools.logger.Log;


/**
 * HTTP server logic
 * Manages custom operation on GET and POST requests
 */
public class RequestsManager implements HttpConstants{


    private static String restarttag = "<form method=\"post\" action=\"/filelist.html\">"
    	+ "<div class=\"item\">"
    	+ "<input title=\"Reload all configurations\" name=\"restart\" value=\"Force reload\" type=\"submit\" onClick=\"this.value='restarting server...'\" class=\"button\">"
    	+ "</div>"
    	+ "</form>";
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Called if message was a POST with parameters (?xxx)
	 * Allows to launch custom actions depending on received messages
	 * @param url parameters of the POST URL
	 * @param bd HTTP connection with the client (browser)
	 * @param user User who generated the request
	 * @return String containing updated page content
	 */
	public static String proceedPost(String url, UserTransaction bd, User user) {
		//System.err.println("Reveiced parameters for ["+url+"]");
		ByteArrayOutputStream pageContent = bd.getPageContent();
		try{
		if (url.endsWith("network.html")){
			//PageNetwork.updateNetwork(pageContent.toString().trim().split("&"));
			return "";
		} else if (url.endsWith("admin.html") && user.getRights()==RIGHTS.ADMIN){
			PageAdmin.update(pageContent.toString("UTF-8").trim().split("&"));
			return "";
		} else if (url.endsWith("editor.html") && user.getRights()==RIGHTS.ADMIN){
			PageEditor.update(pageContent.toString("UTF-8").trim().split("&"));
			return "";
		}else if (url.endsWith("confusers.html") && user.getRights()==RIGHTS.ADMIN){
			PageUsers.update(pageContent.toString("UTF-8").trim().split("&"));
			return "";
		} else if (url.endsWith("conflog.html")&& user.getRights()!=RIGHTS.NONE){
			PageLogs.update(pageContent.toString("UTF-8").trim().split("&"));
			return "";
		} else if (url.endsWith("listreqscripts.html")){
			ScriptList.update(pageContent.toString("UTF-8").trim().split("&"), true);
			return "";
		} else if (url.endsWith("listrespscripts.html")){
			ScriptList.update(pageContent.toString("UTF-8").trim().split("&"),false);
			return "";
		} else if (url.endsWith("createreqscript.html")&& user.getRights()!=RIGHTS.NONE){
			String params = ScriptList.create(pageContent.toString("UTF-8").trim().split("&"),Icap.TYPE.REQMOD, user);
			return params;
		}else if (url.endsWith("createrespscript.html")&& user.getRights()!=RIGHTS.NONE){
			String params = ScriptList.create(pageContent.toString("UTF-8").trim().split("&"),Icap.TYPE.RESPMOD, user);
			return params;
		} else if (url.endsWith("showconfig.html") && user.getRights()==RIGHTS.ADMIN){
			if (pageContent.toString("UTF-8").contains("restart=restarting")){
				HttpServer.restartProcess();
			}
		} else if (url.endsWith("maintenance.html") && user.getRights()==RIGHTS.ADMIN){
			Maintenance.storeBackup(bd.getContentType(), pageContent);
		} else if (url.endsWith("analyser.html") && user.getRights()==RIGHTS.ADMIN){
			ScriptCheckerPage.update(pageContent.toString("UTF-8").trim().split("&"));
			return "";
		} else if (url.endsWith("packages.html") && user.getRights()==RIGHTS.ADMIN){
			PackagesManagement.storePackage(bd.getContentType(), pageContent);
		} else if( user.getRights()==RIGHTS.ADMIN && pageContent.toString("UTF-8").contains("restart=restarting")){
				HttpServer.restartProcess();
		}
		return new String(pageContent.toByteArray(), "UTF-8");
		}catch (Exception e){
			e.printStackTrace();
		}
		return new String(pageContent.toByteArray());
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	private final static void checkRights(RIGHTS requestedRights, User user){
		boolean accessdenied = true;
		switch (requestedRights){
			case ADMIN:
				if (user.getRights() ==  RIGHTS.ADMIN) accessdenied = false;
				break;
			case USER:
				if (user.getRights() ==  RIGHTS.ADMIN) accessdenied = false;
				if (user.getRights() ==  RIGHTS.USER) accessdenied = false;
				break;
			case NONE:
				accessdenied = false; 
				break;
			default:
		}
		if (accessdenied){
			Log.error(Log.SEVERE, "User ["+user.getLogin()+"] is trying to call functions requiering "+ requestedRights.toString() +" privileges");
			throw new java.lang.SecurityException("HttpServer: User ["+user.getLogin()+"] is trying to call functions requiering Administrator privileges");
		}
	} 
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Do preprocessing actions on HTML page content
	 * Preprocessing is actions made on request <i>before</i> loading page from disk
	 * @param page the HtmlFile page to process
	 * @param user User who generated the request
	 * @return content to directly send back to client, or null if processing shall continue
	 */
	public static String preProcessGet(HtmlFile page,User user){
		try{
		
		//TOP frame: modify it depending on user rights
		if (page.getUrlPath().endsWith("top.html") && user.getRights()==RIGHTS.USER){
			page.setUrlPath(page.getUrlPath().replace("top.html", "top_user.html"));
		}
		if (page.getUrlPath().endsWith("top.html") && user.getRights()==RIGHTS.NONE){
			page.setUrlPath(page.getUrlPath().replace("top.html", "top_user.html"));
		}
		
		//Log files management: allows to rotate, download and delete files
		if (page.getUrlPath().endsWith("datalog.html") && page.getParameters().length()>0){
			checkRights(RIGHTS.ADMIN, user);
			if (page.getParameters().equals("rotatelogs")) {
				Log.forceRotation();
			} else {
				String[] params = page.getParameters().split("&");
				if (params.length>=2){
					boolean delete=false;
					Vector<String> files = new Vector<String>();
					for (String s:params){
						String[] values = s.split("=");
						if (values[1].equals("delete")){
							if (user.getRights()==RIGHTS.ADMIN) delete = true;
						}
						if (values[0].equals("file")){
							page.setFilename(values[1]);
							if (!values[1].endsWith(".log")) files.add(values[1]);
						}
					}

					if (delete){
						for (String filename:files){
							try {
								new File(Log.getLogPath()+filename).delete();
							} catch (Exception e){/*file does not exist - ignore*/}
						}
					} else {
						page.setUrlPath(Log.getLogPath()+page.getFilename());
						page.forceDownload(page.getFilename());
					}
				}
			}
		}
		//Backups files management: allows to upload, backup, download and restore files
		if (page.getUrlPath().endsWith("maintenance.html") && page.getParameters().length()>0){
			checkRights(RIGHTS.ADMIN, user);
			if (page.getParameters().equals("backupall")) {
				Maintenance.backupAll();
			} else if (page.getParameters().equals("backupcommons")) {
				Maintenance.backupCommons();
			} else {
				String[] params = page.getParameters().split("&");
				if (params.length>=2){
					boolean delete=false, restore = false;
					Vector<String> files = new Vector<String>();
					for (String s:params){
						String[] values = s.split("=");
						if (values[1].equals("delete")){
							if (user.getRights()==RIGHTS.ADMIN)  delete = true;
							continue;
						}
						if (values[1].equals("restore")){
							if (user.getRights()==RIGHTS.ADMIN)  restore = true;
							continue;
						}
						if (values[0].equals("file")){
							page.setFilename(values[1]);
							if (values[1].endsWith(".zip")&& values[1].indexOf("..")==-1) files.add(values[1]);
						}
					}

					if (delete){
						for (String filename:files){
							try {
								new File(Maintenance.getBackupPath()+filename).delete();
								Log.error(Log.INFO, "File "+Maintenance.getBackupPath()+filename+" deleted by "+user.getLogin());
							} catch (Exception e){
								e.printStackTrace();
								/*file does not exist - ignore*/}
						}
					} else if (restore && files.size()==1){
						Maintenance.restoreBackup(Maintenance.getBackupPath()+files.elementAt(0));
					} else {
						page.setUrlPath(Maintenance.getBackupPath()+page.getFilename());
						page.forceDownload(page.getFilename());
					}
				}
			}
		}
		
		//Backups files management: allows to upload, backup, download and restore files
		if (page.getUrlPath().endsWith("packages.html") && page.getParameters().length()>0){
			checkRights(RIGHTS.ADMIN, user);
			if (user.getRights()!=RIGHTS.ADMIN) return null;
			String[] params = page.getParameters().split("&");
			if (params.length>=2){
				boolean delete=false, install = false, uninstall=false;
				String packagename = "";
				for (String s:params){
					String[] values = s.split("=");
					if (values[0].equals("packagename")){
						packagename = values[1];
					} else if (values[0].equals("operation")){
						if (values[1].equals("delete")) delete = true;
						else if (values[1].equals("install")) install = true;
						else if (values[1].equals("uninstall")) uninstall = true;
					}
				}
				if (packagename.equals("")) return null;
				if (delete){
					PackagesManagement.deleteExtension(packagename);
				} else if (install){
					PackagesManagement.installExtension(packagename);
				} else if (uninstall){
					PackagesManagement.unInstallExtension(packagename);
				}
			}
		}
		
		//File edition: allows to save modified content
		if (page.getUrlPath().endsWith("editfile.html") && page.getParameters().length()>0){
			String[] params = page.getParameters().split("&");
			if (params.length!=0){
				boolean save=false;
				boolean xhr=false;
				String content = null;
				for (String s:params){
					String[] values = s.split("=");
					if (values[0].trim().equals("operation")){
						if (values[1].equals("save")) save = true;
					}
					if (values[0].trim().equals("filename")){
						page.setFilename(TextTools.hexToUTF8(values[1].replace("+", " ")));
					}
					if (values[0].trim().equals("content")){
						//System.err.println(values[1]);
						content = TextTools.hexToUTF8(values[1].replace("+", " "));
					}
					if (values[0].trim().equals("xhr")){
						if (values[1].equals("true")) xhr = true;
					}
				}
				if (save==true && page.getFilename()!=null && content!=null){
					FileEditor.save(page.getFilename(), content, user);
					FileEditor.error = ScriptList.refreshScript(page.getFilename());
					if (xhr) return "{\"commentcontent\": \"<!-- comment -->\", \"errorcontent\": \"<!-- error -->\"}";
				}
			}
		}
		//File list: allows to edit or delete file, except jar files
		if (page.getUrlPath().endsWith("filelist.html") && page.getParameters().length()>0 ){
			String[] params = page.getParameters().split("&");
			if (params.length>=2){
				boolean delete=false,edit=false;String filename="";
				for (String s:params){
					String[] values = s.split("=");
					if (values[1].equals("delete") ){
						checkRights(RIGHTS.ADMIN, user);
						if (user.getRights()==RIGHTS.ADMIN) delete = true;
						continue;
					}
					if (values[0].equals("file")){
						page.setFilename(HttpServer.conf_path+values[1]);
						filename = values[1];
						continue;
					}
					if (values[1].equals("edit")){
						edit = true;
					}
				}
				if (delete){
					if (!page.getFilename().endsWith(".jar")){
						try {
							new File(page.getFilename()).delete();
						} catch (Exception e){/*file does not exist - ignore*/}
					}
				} else if (edit){
					page.setUrlPath(HttpServer.getPathToFiles()+File.separator+"editfile.html");
				} else {
					page.setUrlPath(page.getFilename());
					page.forceDownload(filename);
				}
			}
		}
		
		//createreqscript.html: allows to create a new script file in req or resp mode
		if ((page.getUrlPath().endsWith("createreqscript.html") || page.getUrlPath().endsWith("createrespscript.html")) 
				&& page.getParameters().startsWith("edit")){
			page.setUrlPath(HttpServer.getPathToFiles()+File.separator+"editfile.html");
			page.setFilename(page.getParameters().substring(page.getParameters().indexOf("=")+1));
		}
		}catch (Exception e){
			Log.trace(Log.FINE, "Administration server - error preprocessing GET", e);
		}
		return null;
	}
//	<------------------------------------------------------------------------------------------>



//	<------------------------------------------------------------------------------------------>
	/**
	 * Do postprocessing actions on HTML page content
	 * Postprocessing is actions made on request <i>after</i> loading page from disk
	 * @param page the HtmlFile page to process
	 * @param user User who generated the request
	 */
	public static void postProcessGet(HtmlFile page,User user){
		try{
		String pagecontent = page.getPageContent();
		if (pagecontent.indexOf("<!--projectversion-->")>-1){
			pagecontent = pagecontent.replace("<!--projectversion-->", ProjectSpecifics.getProjectVersion());
		}
		if (pagecontent.indexOf("<!--versions-->")>-1){
			pagecontent = pagecontent.replace("<!--versions-->", InfoPage.getText());
		}
		if (pagecontent.indexOf("<!--scriptsinfo-->")>-1){
			pagecontent = pagecontent.replace("<!--scriptsinfo-->", ScriptsInfoPage.getText());
		}
		if (pagecontent.indexOf("<!--restartcmd-->")>-1){
			pagecontent = pagecontent.replace("<!--restartcmd-->", restarttag);
		}
		if (pagecontent.indexOf("<!--storagestats-->")>-1){
			pagecontent = pagecontent.replace("<!--storagestats-->", InfoPage.getDiskOccupation());
		}
		if (pagecontent.indexOf(" <!--currentactivity-->")>-1){
			pagecontent = pagecontent.replace("<!--currentactivity-->", InfoPage.getCurrentActivity());
		}
		if (pagecontent.indexOf("<!--cumulativeactivity-->")>-1){
			pagecontent = pagecontent.replace("<!--cumulativeactivity-->", InfoPage.getCumulativeActivity());
		}
		if (pagecontent.indexOf("<!--timeinformation-->")>-1){
			pagecontent = pagecontent.replace("<!--timeinformation-->", InfoPage.getTimeInformation());
		}
		
		if (page.getUrlPath().endsWith("menumaintenance.html")){
			//if (PackagesManagement.isWindows()){
				pagecontent = pagecontent.replace("<!--packages.html-->", "<a href ='packages.html' target ='main'>Language Packs</a>");
			//}
		}
		if (page.getUrlPath().endsWith("admin.html")){
			if (PageAdmin.mustRestart()) {
				page.redirect(PageAdmin.getRestartCommand());
				return;
			}
			pagecontent = PageAdmin.set(pagecontent);
		}  else if(page.getUrlPath().endsWith("menudata.html") || page.getUrlPath().endsWith("menuscripts.html") ){
			pagecontent = PageLogs.updateLogMenu(pagecontent);
		} else if(page.getUrlPath().endsWith("showconfig.html")){
			pagecontent = ConfigurationFile.getConfigFile(pagecontent);
		} else if(page.getUrlPath().endsWith("editor.html")){
			pagecontent = PageEditor.set(pagecontent);
		} else if (page.getUrlPath().endsWith("conflog.html")){
			pagecontent = PageLogs.set(pagecontent);
		} else if (page.getUrlPath().endsWith("showlog.html")){
			pagecontent = ShowLogs.set(pagecontent, page.getParameters());
		} else if (page.getUrlPath().endsWith("datalog.html")){
			pagecontent = pagecontent.replace("<!--logfiles-->", createFileSelector(Log.getLogPath()));
		} else if (page.getUrlPath().endsWith("maintenance.html")){
			pagecontent = pagecontent.replace("<!--backupfiles-->", createFileSelector(Maintenance.getBackupPath()));
		} else if (page.getUrlPath().endsWith("filelist.html")){
			pagecontent = pagecontent.replace("<!--filelist-->", createFileSelector(HttpServer.conf_path));
		} else if (page.getUrlPath().endsWith("editfile.html")){
			String error = ScriptList.checkScriptErrors(page.getFilename());
			FileEditor.error = error;
			pagecontent = FileEditor.set(pagecontent, page.getFilename());
		} else if (page.getUrlPath().endsWith("listreqscripts.html")){
			pagecontent = ScriptList.setScripts(pagecontent,true, user);
		} else if (page.getUrlPath().endsWith("listrespscripts.html")){
			pagecontent = ScriptList.setScripts(pagecontent,false, user);
		} else if (page.getUrlPath().endsWith("createreqscript.html")){
			pagecontent = ScriptList.newScript(pagecontent);
		} else if (page.getUrlPath().endsWith("createrespscript.html")){
			pagecontent = ScriptList.newScript(pagecontent);
		} else if (page.getUrlPath().endsWith("confusers.html") && user.getRights()==RIGHTS.ADMIN){
			pagecontent = PageUsers.setUsers(pagecontent);
		} else if (page.getUrlPath().endsWith("analyser.html") && user.getRights()==RIGHTS.ADMIN){
			pagecontent = ScriptCheckerPage.setContent(pagecontent);
		} else if (page.getUrlPath().endsWith("packages.html") && user.getRights()==RIGHTS.ADMIN){
			pagecontent = PackagesManagement.setContent(pagecontent);
		}
		page.setPageContent(pagecontent);
		}catch (Exception e){
			Log.trace(Log.FINE, "Administration server - error postprocessing GET", e);
		}
	}
//	<------------------------------------------------------------------------------------------>


//	<------------------------------------------------------------------------------------------>
	/**
     * Generate a html OPTION menu allowing to select files
     * @param path the directory path which files will be used as OPTION values 
     * @return html code corresponding to files selections menu
     */
	@SuppressWarnings("deprecation")
	public static String createFileSelector(String path){
		StringBuilder sb = new StringBuilder();
		try{
			String[] files = new File(path).list();
			Vector<String> files_v = new Vector<String>();
			File f;
			//Retrieve files names
			for (String s:files){
				f = new File(path+s);
				if (f.isDirectory()) continue;
				files_v.add(s);
			}
			files = null;
			files = files_v.toArray(new String[0]);
			String[] sizes = new String[files.length];
			String[] dates = new String[files.length];
			//Retrieve files size
			for (int i=0; i<files.length;i++){
				sizes[i] = ((new File(path+files[i])).length()/1024) + "Ko";
			}
			//Retrieve files date
			for (int i=0; i<files.length;i++){
				dates[i] = new Date((new File(path+files[i])).lastModified()).toLocaleString();
			}
			//Generate HTML OPTION content with <file name> <file date> <file size> 
			for (int i=0; i<files.length;i++){
				sb.append("<option value=\"").append(files[i]).append("\">&nbsp;[");
				sb.append(files[i]).append("]");
				sb.append("&nbsp;[").append(dates[i]).append("]");
				sb.append("&nbsp;[").append(sizes[i]).append("]&nbsp;</option>\r\n");
			}

		}catch (Exception e){
			return "";    
		}

		return sb.toString();
	}
//	<------------------------------------------------------------------------------------------>





}
