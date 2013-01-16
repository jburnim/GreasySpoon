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
//Import
import java.io.*;
///////////////////////////////////
import java.util.logging.Level;

import tools.logger.Log;

/**
 * Define a HTTP redirection message using a 302 or javascript.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class HtmlRedirect extends HtmlModel {

//	<------------------------------------------------------------------------------------------>
	/**
	 * Create a page with a javascript in order to redirect client to given url
	 * @param   urlToRedirect  The url where to redirect the client
	 */
	private HtmlRedirect(String html) {
		try{
			pageByteHtml=new ByteArrayOutputStream();
			pageByteHtml.write(html.getBytes());
		}catch(Exception e){ if (Log.finest()) Log.trace(Level.FINEST, e); }
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Create a redirection message using javascript code
	 * @param urlToRedirect the URL where to redirect browser
	 * @return HTTP redirection message 
	 */
	public static HtmlRedirect javascriptRedirect(String urlToRedirect){
		String tmp=	"<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"+
		"<title>JavaScript Redirect</title>\n"+
		"</head>\n<body>\n<script LANGUAGE=\"JavaScript\">\n"+
		"function redirect()\n{ window.location = \""+urlToRedirect+"\" }\n"+
		"setTimeout(\"redirect();\", 0)\n</script>\n</html>\n";

		HtmlRedirect redirect = new HtmlRedirect(tmp);
		return redirect;
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>  
	/**
	 * Generate an HTTP 302 response
	 * @param location location for 302 response
	 * @return HTTP redirection message 
	 */
	public static HtmlRedirect create302(String location){
		HtmlRedirect redirect = new HtmlRedirect("<html><body>Redirecting to "
				+location+"</body></html>");
		redirect.redirectHeader(location, "html",null);
		return redirect;
	}
//	<------------------------------------------------------------------------------------------>

	//	<------------------------------------------------------------------------------------------>  
	/**
	 * Generate an HTTP 302 response
	 * @param location location for 302 response
	 * @param cookie Cookie value to set in 302 response
	 * @return HTTP redirection message 
	 */
	public static HtmlRedirect create302(String location,String cookie){
		HtmlRedirect redirect = new HtmlRedirect("<html><body>Redirecting to "
				+location+"</body></html>");
		redirect.redirectHeader(location, "html",cookie);
		return redirect;
	}
//	<------------------------------------------------------------------------------------------>
}