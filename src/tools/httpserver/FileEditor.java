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
 * Created	:	1 nov. 07
 *---------------------------------------------------------------------------*/

package tools.httpserver;

///////////////////////////////////
//Import
import java.io.*;
import org.apache.commons.lang.StringEscapeUtils;
///////////////////////////////////



/**
 * Provides facilities to edit and save text files stored on disk<br>
 * Replace following html tags in page:
 * <ul>
 * <li>&lt;!-- comment -->	\t replaced by server events (save, ...)</li>
 * <li>&lt;!-- error --> \t replaced by server errors</li>
 * <li>%name% \t replaced by file name (without path)</li>
 * <li>&lt;!--filename-->\t replaced by file name (with path)</li>
 * <li>&lt;!--filecontent-->\t replaced by file content</li>
 * </ul>
 */
public class FileEditor implements HttpConstants {

	static String comment = "";
	/**Store error encountered when editing file - for user display*/
	public static String error = "";

	static enum FILESTYPES {/**JavaScript*/JS, /**Java*/JAVA, /**Ruby*/RB,
		/**HTML*/HTML, /**PHP*/PHP, /**SQL*/SQL, /**XML*/XML,
		/**XSL*/XSL,/**PERL*/PDL,/**CSS*/CSS, 
		/**Python*/PY, /**Bash like configuration*/CONF, /**Any text-like file*/TEXT}

//	<------------------------------------------------------------------------------------------>
	/**
	 * Return content with  "&lt;!--filecontent-->" replaced by a textarea containing given file content
	 * @param content	The html content to update
	 * @param filename	The filename which content is inserted into textarea
	 * @return the modified html page
	 */
	public static String set(String content, String filename){
		if (content.startsWith("{")){//special case for JSON content
			content = content.replace("<!-- comment -->", comment.replace("\"", "\\'").replace("\r", "").replace("\n", ""));
			content = content.replace("<!-- error -->", error.replace("\"", "\\'").replace("\r\n", ""));
		} else {
			content = content.replace("<!-- comment -->", comment);
			content = content.replace("<!-- error -->", error);
		}
		comment="";
		error="";
		if (filename==null) return content;
		File fich = new File(filename);
		if (!fich.exists()){
			content = content.replace("<!-- error -->","File not founded: [" + filename+"]");
			return "";
		}// Endif

		StringBuilder sb = new StringBuilder();
		try {       
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fich),"UTF8"));
			String str;
			//Read a line
			while ((str = in.readLine()) !=null ){
				if (str.startsWith("#rights=")) continue;
				sb.append(str).append("\r\n");
			}//End while readLine
			in.close();  
		} catch (IOException e){
			content = content.replace("<!-- error -->","Error reading File : [" + e.getMessage()+"]");
			return "";
		}//End try&catch
		content=content.replace("%name%", fich.getName());
		content=content.replace("<!--filename-->", filename);

		if (HttpServer.editor_on) {
			int p1 = content.indexOf("<!-- START_EDITOR_DISABLED -->");
			int p2 = content.indexOf("<!-- END_EDITOR_DISABLED -->");
			if (p1!=-1 && p2>p1) content = content.substring(0,p1)+ content.substring(p2);

			filename = filename.toLowerCase();
			String editoroption = ",syntax: \"";
			try{
				String extension = filename.indexOf('.')>0?filename.substring(filename.lastIndexOf('.')+1):"unknown";
				switch (FILESTYPES.valueOf(extension.toUpperCase())){
					case JS: editoroption+="js";break;
					case JAVA: editoroption+="java";break;
					case RB: editoroption+="ruby";break;
					case HTML: editoroption+="html";break;
					case PHP: editoroption+="php";break;
					case SQL: editoroption+="sql";break;
					case XML: editoroption+="xml";break;
					case XSL: editoroption+="xml";break;
					case PDL: editoroption+="perl";break;
					case CSS: editoroption+="css";break;
					case PY: editoroption+="python";break;
					case CONF: editoroption+="conf";break;
					default: editoroption+="conf";
				}
			} catch (Exception e){
				editoroption+="conf";
			}
			editoroption += "\"";

			editoroption += "\r\n,autocompletion: "+ (HttpServer.autocomplete_on?"true":"false");
			editoroption += "\r\n,start_highlight: "+ (HttpServer.start_highlight?"true":"false");
			editoroption += "\r\n,word_wrap: "+ (HttpServer.word_wrap?"true":"false");
			editoroption += "\r\n,replace_tab_by_spaces: "+ (HttpServer.replace_tab_by_spaces?"4":"false");
			content=content.replace("<!--editoroptions-->", editoroption);
		} else {
			int p1 = content.indexOf("<!-- START_EDITOR_ENABLED -->");
			int p2 = content.indexOf("<!-- END_EDITOR_ENABLED -->");
			if (p1!=-1 && p2>p1) content = content.substring(0,p1)+ content.substring(p2);
		} 
		content=content.replace("<!--filecontent-->", StringEscapeUtils.escapeHtml(sb.toString()));
		return content;
	} 
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Save provided content into given filename.
	 * Existing file with given name is deleted.
	 * @param filename the file name to use
	 * @param htmlcontent the content to save into file
	 * @param user User issuing the request
	 */
	public static void save(String filename,String htmlcontent, User user){

		if (filename==null) {
			error="Invalid file name";
			return;
		}
		File file = new File(filename);
		if (!file.getAbsolutePath().startsWith(HttpServer.getApplicationPath())){
			error="Invalid file path";
			return;
		}
		boolean insertrights = true;
		if (file.getAbsolutePath().startsWith(HttpServer.conf_path)){
			insertrights = false;
		}
		RIGHTS rights=user.getRights();
		if (!insertrights) rights=RIGHTS.ADMIN;
		if (file.exists()){
			try {       
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"));
				String str = in.readLine();
				//Read a line
				if (str.startsWith("#rights=")) rights = RIGHTS.valueOf(str.substring(str.indexOf("=")+1));
				in.close();
			} catch (Exception e){
				rights=user.getRights();
			}//End try&catch
			if (!allowed(user.getRights(), rights)) {
				error = "Modification denied due to insuffisant access rights";
				return;
			}
			file.delete();
			file = new File(filename);
		}// Endif
		try {       
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF8"));
			
			if (insertrights) out.write("#rights="+rights.toString()+"\r\n");
			out.write(htmlcontent);
			out.flush();
			out.close();
		} catch (IOException e){
			error=e.getLocalizedMessage();
			return;
		}//End try&catch

		comment = "file saved.";
	} 
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Check if user rights are enough to manipulate file
	 * @param userrights User rights
	 * @param filerights File attached rights
	 * @return true if user is allowed to manipulate file
	 */
	public static boolean allowed(RIGHTS userrights, RIGHTS filerights){
		if (userrights==RIGHTS.ADMIN) return true;
		if (userrights==filerights) return true;
		if (filerights==RIGHTS.ADMIN) return false;
		if (filerights==RIGHTS.NONE && userrights==RIGHTS.USER) return true;
		return false;
	}
//	<------------------------------------------------------------------------------------------>


}
