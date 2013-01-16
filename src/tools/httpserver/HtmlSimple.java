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
 * Created	:	28/08/02
 *----------------------------------------------------------------------------------
 */
package tools.httpserver;
///////////////////////////////////
// 	Import
import java.io.*;
import java.util.logging.Level;
import tools.logger.Log;
///////////////////////////////////


/**
 * Create a simple HTML page from scratch.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class HtmlSimple extends HtmlModel {
 
//<------------------------------------------------------------------------------------------>
/**Create a simple HTML page with given title and body
 * @param title HTML Title value
 * @param body HTML Body value
 */
public HtmlSimple(String title, String body) {
	try{
	  String tmp="<HTML>\n";
	  tmp+="<HEAD><TITLE>"+title+"</TITLE></HEAD>\n";
	  tmp+="<BODY><H1>"+body+"</H1></BODY>\n";
	  tmp+="</HTML>\n";
	  pageByteHtml=new ByteArrayOutputStream();
	  pageByteHtml.write(tmp.getBytes());
	  addHeader("html");
	}catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e); }
}
//<------------------------------------------------------------------------------------------>


//<------------------------------------------------------------------------------------------>
/**Create a simple Text page containing given message
 * @param mess Html code for generated page 
 */
public HtmlSimple(String mess) {
	pageByteHtml=new ByteArrayOutputStream();
    try{
      pageByteHtml=new ByteArrayOutputStream();
      pageByteHtml.write(mess.getBytes());
	  addHeader("txt");
   	}catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e); }
}
//<------------------------------------------------------------------------------------------>


}