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
 * Created	:	9 dec. 06
 *---------------------------------------------------------------------------*/
package tools.httpserver;

///////////////////////////////////
// 	Import
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
///////////////////////////////////


/**
 * Load HttpServer configuration file and replace &lt;!--config--> tag in HTML by configuration content.  
 */
public class ConfigurationFile {
	
    /**
     * Load HttpServer configuration file and replace
     * <!--config--> tag in HTML by configuration content.
     * @param htmlcontent into which configuration file is inserted
     * @return updated HTML content
     */
    public static String getConfigFile(String htmlcontent){
        String fileToEdit = HttpServer.configurationFile;
        File fich = new File(fileToEdit);
            if (!fich.exists()){
                return "";
            }// Endif
        StringBuilder sb = new StringBuilder();
        try {       
               BufferedReader in = new BufferedReader(new FileReader(fileToEdit));
               String str;
                //Read a line
                while ((str = in.readLine()) !=null ){
                    sb.append(str).append("\r\n");
                }//End while readLine
                in.close();  
          } catch (IOException e){
              return "";
          }//End try&catch
          htmlcontent=htmlcontent.replace("<!--config-->", sb.toString());
          return htmlcontent;
    } 
}
