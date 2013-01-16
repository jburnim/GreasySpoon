/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008-2009 Karel Mittig
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
 *-----------------------------------------------------------------------------
 */
package tools.httpserver;
///////////////////////////////////
//Import
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
///////////////////////////////////

import tools.logger.Log;

/**
 * Define mime types associated with file extension.<br>
 * Allow server to set correct content type to HTTP message headers.<br>
 * Methods are static for convenient usage.<br>
 * Mime types are loaded from file "mime.types" which must be available in project root directory.<br>
 * File syntax is the same than Apache mime.types file.
 * @version 1.1
 * @author k.mittig 
 */
public class MimeType {
    
    /**
     * Hashtable containing mime types
     * Hashtable is filled with extension as key and mime type as value
     * */
    private static ConcurrentHashMap<String,String> mimetypes = null;
    
    
    /**
     * MIME File used to prefetch MIME/extensions
     * Format must be similar to Apache mime.types file
     */
    private static String _mimefile = HttpServer.conf_path+"mime.types";
    
    
    
//  <--------------------------------------------------------------------------->
    /**
     * Return content type associated with given extension.
     * @param   extension  the file extension (without ".")
     * @return  mime-type if founded, "unknown/unknown" otherwise
     */
    public final static String getContentTypeFor(String extension) {
    	try{
	        //If mimetypes is empty, populate it (called only once)
	        if (mimetypes==null)  {
	            loadMimeFile(_mimefile);
	        } 
	        String contentType = mimetypes.get(extension);
	        if (contentType==null) contentType= "unknown/unknown";
	        return contentType;
    	} catch(Exception e){
    		if (Log.info()) Log.error(Log.CONFIG,"MimeType configuration: unable to find mime type for "+extension);
    	}
    	return "unknown/unknown";
    }//End
//  <--------------------------------------------------------------------------->
    
//  <--------------------------------------------------------------------------->
    /**
     * Parse given mime to extensions file and fill hashtable with it
     * @param file the File containing mime to extensions 
     */
    private static void loadMimeFile(String file){
        //use a temporary table for mime loading
    	ConcurrentHashMap<String,String> temporarymimetypes = new ConcurrentHashMap<String,String>();
        File fich = new File(file);
        if (!fich.exists()){
            //System.err.println("Cannot find MIME file <"+fich.toString()+">");
            return;
        }// Endif
        
        try {       
            BufferedReader in = new BufferedReader(new FileReader(fich));
            String str;
            //Read a line
            while ((str = in.readLine()) !=null ){
                if (str.startsWith("#") || str.startsWith(";")) continue;//comments
                
                //Split lines on spaces and tabs 
                String[] values = str.split("[\\s\\t]+");
                
                //check if we have at least two values (MIME + 1..n extensions)
                //if not, drop mime type
                if (values == null || values.length <2) continue;
                
                //store all extensions with founded mime type
                for (int i=1;i<values.length; i++){
                    temporarymimetypes.put(values[i], values[0]);//orelse put it in hashtable
                }
            }//End while readLine
            //update mime table with temporary table
            mimetypes = temporarymimetypes;
            in.close();   
        } catch (IOException e){
            //System.err.println("Error in MIME file <"+fich.toString()+">.\r\n["+e.toString()+"]\r\nFile corrupted.");
        }//End try&catch
    }
//  <--------------------------------------------------------------------------->
    
//  <--------------------------------------------------------------------------->
    // Access Methods //
    /**
     * @return Returns the current mime file.
     */
    public static String getMimefile() {
        return _mimefile;
    }
    /**
     * @param mimefile The mime file to set. Mime table is automaticaly updated.
     */
    public static void setMimefile(String mimefile) {
        MimeType._mimefile = mimefile;
        refresh();
    }
    /**
     * Reload the mime table using current mime file
     */
    public static void refresh() {
        loadMimeFile(_mimefile);
    }
//  <--------------------------------------------------------------------------->
    
}