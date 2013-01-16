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

import java.io.*;
import java.util.*;
import icap.services.resources.gs.ExtensionManagement;
import icap.services.resources.gs.ExtensionPackage;



/**
 * Class used to backup/restore configuration
 */
public class PackagesManagement {

	static String comment="", error="";
//  <------------------------------------------------------------------------------------------>
    /**
     * Insert Scripts list into given HTML page  
     * @param content The HTML page to update
     * @return the updated HTML page
     */
    public static String setContent(String content){
        //<!--availablepackages-->
        //<!--installedpackages-->
    	
      	StringBuilder availablepackages = new StringBuilder();
      	StringBuilder installedpackages = new StringBuilder();
      	ExtensionManagement.loadExtensions();
    	Vector<ExtensionPackage> packages = ExtensionManagement.getExtensions();
    	for (ExtensionPackage p:packages){
			String item = "<option class='pkg' value='"+ p.getCodedName()
				+"' details='"+p.toHtml()+"'>"
				+p.getName()+ " package "+p.getPkgVersion()+" (version " + p.getLibVersion() + ")" 
				+"</option>\r\n";
    		if (p.isInstalled()){
    			installedpackages.append(item);
    		} else {
    			availablepackages.append(item);
    		}
    	}
    	
    	if (availablepackages!=null) content = content.replace("<!--availablepackages-->", availablepackages.toString());
    	if (installedpackages!=null) content = content.replace("<!--installedpackages-->", installedpackages.toString());
        if (comment!=null) content = content.replace("<!-- comment -->", comment);
        if (error!=null) content = content.replace("<!-- error -->", error);
        comment = ""; error = "";
        return content;
    }
//  <------------------------------------------------------------------------------------------>

    
//  <------------------------------------------------------------------------------------------>
    /**
     * Try to delete extension package
     * @param packagename the package ID to delete
     */
    public static void deleteExtension(String packagename){
    	ExtensionManagement.deleteExtension(packagename);
    }
    
    /**
     * Try to remove extension package
     * @param packagename the package ID to remove
     */
    public static void unInstallExtension(String packagename){
    	if (ExtensionManagement.unInstallExtension(packagename)){
    		comment="Package removed.";
    	} else {
    		error="Unable to remove package.";
    	}
    }

    /**
     * Try to install extension package
     * @param packagename the package ID to install
     */
    public static void installExtension(String packagename){
	    if (ExtensionManagement.installExtension(packagename)){
	    	comment="Package installed.";
		} else {
			error="Unable to install package - possible reasons: package libraries already installed or insufficient rights"
				+"<br /> Try install from shell using -e parameter";
		}
    }
//  <------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Store provided extension content on disk.
	 * Content must be provided using multipart format
	 * @param contentType Initial content type of the response
	 * @param contentFile the content to store
	 */
	public synchronized static void storePackage(String contentType, ByteArrayOutputStream contentFile){
		try{
			String tmp = contentFile.toString("ISO-8859-1");
			int p1, p2;
		
			p1 = contentType.indexOf("boundary=") + "boundary=".length();
			String boundary = "\r\n--"+contentType.substring(p1).trim();
			//System.err.println("Boundary:"+boundary);
			
	
			p1 = tmp.indexOf("filename=\"") + "filename=\"".length();
			p2 = tmp.indexOf("\"", p1);
			String filename=tmp.substring(p1, p2).trim();
			if (filename==null || filename.trim().length()==0) return;
			String name = ExtensionManagement.getExtensionPath()+filename;
			
			//System.err.println("filename:"+name);
			int offset = tmp.indexOf("\r\n\r\n", p2)+4;
			int length = tmp.indexOf(boundary, offset) - offset;
			//System.err.println("offset:"+offset+"/length:"+length);
			File extdir = new File (ExtensionManagement.getExtensionPath());
			extdir.mkdir();
		
			byte[] content = contentFile.toByteArray();
			FileOutputStream fos = new FileOutputStream(name);
			fos.write(content, offset, length);
			fos.flush();
			fos.close();
		}catch (Exception e){
			error = e.getLocalizedMessage();
			//e.printStackTrace();
		}
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * @return true if server OS is Windows
	 */
	public static boolean isWindows(){
		String os = System.getProperty("os.name").toLowerCase();
	    return (os.indexOf( "win" ) >= 0); 
	}
 
	/**
	 * @return true if server OS is MacOS
	 */
	public static boolean isMac(){
		String os = System.getProperty("os.name").toLowerCase();
	    return (os.indexOf( "mac" ) >= 0); 
	}
 
	/**
	 * @return true if server OS is Unix
	 */
	public static boolean isUnix(){
		String os = System.getProperty("os.name").toLowerCase();
		//linux or unix
	    return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
	}
	//<------------------------------------------------------------------------------------------>

}
