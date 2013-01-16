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
 * Created	:	17 July 09
 *---------------------------------------------------------------------------*/
package tools.httpserver;


/**
 * Editor parameters page<br>
 * Allows to consult/modify editor parameters<br>
 */
public class PageEditor {
    static String comment = "";
    static String error = "";
    
     
//  <------------------------------------------------------------------------------------------>
    /**
     * Update Editor HTML page content with administration parameters 
     * @param content Editor HTML page content to update
     * @return Editor HTML page content updated with current values
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

        content = content.replace("%editor_on%", HttpServer.editor_on?"CHECKED":"");
        content = content.replace("%autocomplete_on%", HttpServer.autocomplete_on?"CHECKED":"");
        content = content.replace("%start_highlight%", HttpServer.start_highlight?"CHECKED":"");
        content = content.replace("%word_wrap%", HttpServer.word_wrap?"CHECKED":"");
        content = content.replace("%replace_tab_by_spaces%", HttpServer.replace_tab_by_spaces?"CHECKED":"");

        return content;
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * Update Editor parameters with Editor html page form values
     * @param params form values as modified by user
     */
    public static void update(String[] params){
        try{
            String  editor_on="";
            String  autocomplete_on="";
            String  start_highlight="";
            String  word_wrap="";
            //String  fullscreen="";
            String  replace_tab_by_spaces="";

            
            for (String str:params){
                if (str.trim().length()==0) continue;
                String[] values = str.split("=");
                if (values.length!=2) continue;
                if (values[0].equals("editor.editor_on")){
                	editor_on = values[1].trim();
                } else if (values[0].equals("editor.autocomplete_on")){
                	autocomplete_on = values[1].trim();
                } else if (values[0].equals("editor.start_highlight")){
                	start_highlight = values[1].trim();
                } else if (values[0].equals("editor.word_wrap")){
                	word_wrap = values[1].trim();
                } else if (values[0].equals("editor.replace_tab_by_spaces")){
                	replace_tab_by_spaces = values[1].trim();
                } 
            }
            

            if((editor_on.equals("") && HttpServer.editor_on) || editor_on.equals("on")&& !HttpServer.editor_on){
                HttpServer.editor_on = !HttpServer.editor_on;
            }
            if((autocomplete_on.equals("") && HttpServer.autocomplete_on) || autocomplete_on.equals("on")&& !HttpServer.autocomplete_on){
                HttpServer.autocomplete_on = !HttpServer.autocomplete_on;
            }
            if((start_highlight.equals("") && HttpServer.start_highlight) || start_highlight.equals("on")&& !HttpServer.start_highlight){
                HttpServer.start_highlight = !HttpServer.start_highlight;
            }
            if((word_wrap.equals("") && HttpServer.word_wrap) || word_wrap.equals("on")&& !HttpServer.word_wrap){
                HttpServer.word_wrap = !HttpServer.word_wrap;
            }
            if((replace_tab_by_spaces.equals("") && HttpServer.replace_tab_by_spaces) || replace_tab_by_spaces.equals("on")&& !HttpServer.replace_tab_by_spaces){
                HttpServer.replace_tab_by_spaces = !HttpServer.replace_tab_by_spaces;
            }
            
            HttpServer.saveConf(
              new String[]{
            		  "admin.editor.editor_on","admin.editor.autocomplete_on","admin.editor.start_highlight",
            		  "admin.editor.word_wrap","admin.editor.replace_tab_by_spaces",
              		   }
            , new String[]{
            		  Boolean.toString(HttpServer.editor_on),
            		  Boolean.toString(HttpServer.autocomplete_on),
            		  Boolean.toString(HttpServer.start_highlight),
            		  Boolean.toString(HttpServer.word_wrap),
            		  Boolean.toString(HttpServer.replace_tab_by_spaces),
    		  }
            );
        } catch (Exception e){
            error = "Invalid parameter: ["+e.toString()+"]";
        }
        comment = "Changes Committed";
    }
//  <------------------------------------------------------------------------------------------>


}
