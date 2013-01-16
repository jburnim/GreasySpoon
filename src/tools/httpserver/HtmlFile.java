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

import tools.httpserver.custom.RequestsManager;
import tools.logger.*;
import java.util.logging.Level;
///////////////////////////////////


/**
 * Create an HTML page using file content stored onto disk.
 * @version 1.0
 * @author k.mittig 
 */
//<------------------------------------------------------------------------------------------>
public class HtmlFile extends HtmlModel implements HttpConstants {

    private int tailleData=2000;
    private byte data[] = null;

    String parameters;
    String url_path;
    String filename=null;
   
    
//  <------------------------------------------------------------------------------------------>
    /**
     * Create HTML response content for given URI and eventual options
     * @param uri the requested URL (with eventual parameters for GET requests)
     * @param user User who generates the request
     * @exception   FileNotFoundException  
     */
    public HtmlFile(String uri, User user) throws FileNotFoundException {
    	this(uri,"",user);
    }
    
    /**
     * Create HTML response content for given URI and eventual options
     * @param uri the requested URL (with eventual parameters for GET requests)
     * @param options the request parameters (parameters of GET requests if not provided in uri, or parameters of POST requests)
     * @param user User who generates the request
     * @throws FileNotFoundException If no resource is asssociated to given uri
     */
    public HtmlFile(String uri, String options, User user) throws FileNotFoundException {
    	this.parameters = options;
    	this.url_path = uri;
        try {
            if (url_path.contains("?")){
            	this.parameters = uri.substring(url_path.indexOf("?")+1);
            	this.url_path = this.url_path.substring(0, this.url_path.indexOf("?"));
            }
            filename=null;
            String xhr = RequestsManager.preProcessGet(this, user);
            if (xhr==null){
	            File fich = new File(url_path);
	            BufferedInputStream in = new BufferedInputStream(new FileInputStream(fich));		
	            pageByteHtml=new ByteArrayOutputStream();
	            data = new byte[tailleData]; // buffer used in read method
	            int nbRead=0;
	            try{
	                while ((nbRead = in.read(data))> 0) {
	                    pageByteHtml.write(data, 0, nbRead);
	                }
	            } catch (Exception e){
	                if (Log.finest()) Log.trace(Level.FINEST, e);
	            }
	            in.close();
            } else {
            	this.setPageContent(xhr);
            }

            String extension = url_path.substring(url_path.lastIndexOf(".")+1, url_path.length());
            if (MimeType.getContentTypeFor(extension).equals("text/html") || xhr!=null){
            	RequestsManager.postProcessGet(this, user);
             }
            pageByteHtml.close();
            if (this.forcedownload){
                this.addDownloadHeader();
            }else if(url_path.endsWith(".css") || url_path.endsWith(".js") || url_path.endsWith(".gif") ) {
                addCacheableHeader(extension);
            } else {
                addHeader(extension);
            }
        } catch(FileNotFoundException fe){
        	//fe.printStackTrace();
        	if (Log.finest()) Log.trace(Level.FINEST, fe);
            throw fe;
        } catch(Exception e){
        	//e.printStackTrace();
            if (Log.finest()) Log.trace(Level.FINEST, e);
        }
    }
//  <------------------------------------------------------------------------------------------>

//  <------------------------------------------------------------------------------------------>
    /**
     * @return HTML page content as a String
     */
    public String getPageContent(){
    	try {
    		return new String(pageByteHtml.toByteArray(), "UTF-8");
    	} catch (Exception e){
    		return new String(pageByteHtml.toByteArray());
    	}
    }
//  <------------------------------------------------------------------------------------------>
    
//  <------------------------------------------------------------------------------------------>
    /**
     * @param newContent new html content to use as page content
     */
    public void setPageContent(String newContent){
    	try{
	        pageByteHtml=new ByteArrayOutputStream();
	        pageByteHtml.write(newContent.getBytes("UTF8"));
	    } catch(Exception e){ 
	        if (Log.finest()) Log.trace(Level.FINEST, e);
	    }
    }
//  <------------------------------------------------------------------------------------------>
    
	/**
	 * @return HTML file name as stored on disk
	 */
	public String getFilename() {
		return filename;
	}
	/**
	 * set HTML file name as stored on disk
	 * @param newfilename HTML file name as stored on disk
	 */
	public void setFilename(String newfilename) {
		this.filename = newfilename;
	}

	/**
	 * @return HTML url path as seen on HTTP server side
	 */
	public String getUrlPath() {
		return url_path;
	}

	/**
	 * @param newurlpath HTML file name as seen on HTTP server side
	 */
	public void setUrlPath(String newurlpath) {
		this.url_path = newurlpath;
	}

	/**
	 * @return parameters provided in the GET URI (?param=value&...)
	 */
	public String getParameters() {
		return parameters;
	}

	/**
	 * @param newparameters update parameters provided in the GET URI (?param=value&...)
	 */
	public void setParameters(String newparameters) {
		this.parameters= newparameters;
	}




}
