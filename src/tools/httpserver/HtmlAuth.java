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
 * Create an HTML page for authentification
 * @version 1.0
 * @author k.mittig 
 */
public class HtmlAuth extends HtmlModel {
  
  static String loginPage = HttpServer.path_To_Files+File.separator+"login.html";
  private static int tailleData=2000;
  private byte data[] = null;

//<------------------------------------------------------------------------------------------>
  /**
   * Create an authentification page based on loginPage file<br>
   */
  public HtmlAuth() {
    try{
        File fich = new File(loginPage);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fich));		
        pageByteHtml=new ByteArrayOutputStream();
        data = new byte[tailleData]; // buffer used in read method
        int nbRead=0;
        //read content using bit arrays of 2000/1024 bit (after http header)
        try{
            while ((nbRead = in.read(data))> 0) {
                pageByteHtml.write(data, 0, nbRead);
            }
        } catch (Exception e){
        	e.printStackTrace();
            if (Log.finest()) Log.trace(Level.FINEST, e);
        }
        in.close();

        String extension = loginPage.substring(loginPage.lastIndexOf(".")+1, loginPage.length());
        pageByteHtml.close();
        addHeader(extension);
    }catch(Exception e){
    	e.printStackTrace();
    	if (Log.finest()) Log.trace(Level.FINEST, e); 
    }
  }
//<------------------------------------------------------------------------------------------>

}