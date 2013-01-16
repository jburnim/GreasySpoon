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
 *-----------------------------------------------------------------------------*/
package tools.general;


//Import
import java.lang.reflect.*;
import java.io.*;
import java.net.*;



/**
 * Tricky methods to enrich java path and libraries on runtime
 * Pretty dirty (magic ? ;-) )
 */
@SuppressWarnings("unchecked")
public class ClassPathLoader implements FilenameFilter {

	private static ClassPathLoader instance = new ClassPathLoader();
	private static final Class<URL>[] parameters = new Class[]{URL.class};
	private static String[] fileExtensions 	= new String[]{"jar", "class"};
	private final static String initialPath = System.getProperty("java.class.path");


//	---------------------------------------------------------------------------
	/**
	 * Configure system properties
	 * @param properties String containing a list of [param]=[value] entries separated by ';'
	 */
	public static void setProperties(String properties){
		try {
			if (properties!=null && !properties.trim().equals("")) {
				String[] props = properties.split(";");
				if (props!=null) {
					for (String s:props){
						if (!s.contains("=")) continue;
						String[] s2v = s.split("=");
						System.setProperty(s2v[0].trim(),  s2v[1].trim());
					}
				}
			}
		} catch (Exception e){
			System.err.println("Error configuring system properties - configuration line: ["+properties+"]");
		}
	}
//	---------------------------------------------------------------------------

//	---------------------------------------------------------------------------
	/**
	 * Add given directories to current path
	 * @param path 1..n directory(ies) separated using system path separator char
	 */
	public static void addPath(String path) {
		try {
			//Rewrite custom path separator to OS separator
			if (path.indexOf(";")!=-1 && File.pathSeparatorChar!=';'){
				path = path.replace(";",File.pathSeparator);
			} else if (path.indexOf(":")!=-1 && File.pathSeparatorChar!=':'){
				path = path.replace(":",File.pathSeparator);
			}
			//Split paths and reinitialize path to start value
			String[] elements = path.split(File.pathSeparator);
			String newpath = initialPath;

			//for each provided path, look for new libraries to load
			for (String s:elements){
				if (s.trim().equals("")) continue;
				File filepath = new File(s);
				//check is path is a directory. If not, should be a jar or class => try to add it directly to resources 
				if (!filepath.isDirectory()) {

					//Check if resource is already in the path: if yes, just skip it
					if (newpath.indexOf(s)!=-1) continue;
					//New resource to add: let's do it
					newpath = newpath + File.pathSeparatorChar + filepath.getAbsolutePath();
					addFile(filepath);
					continue;
				}
				//path is a directory: Add it, then parse it to find any jar or class resources
				newpath = newpath + File.pathSeparatorChar + filepath.getAbsolutePath();
				String[] resources = instance.getFilesList(s);
				for (String resource:resources){
					//Check if resource is already in the path: if yes, just skip it
					if (newpath.indexOf(resource)!=-1){
						continue;
					}
					//New resource to add: let's do it
					File resourcefile = new File(s+File.separatorChar+resource);
					newpath = newpath + File.pathSeparatorChar + resourcefile.getAbsolutePath();
					addFile(resourcefile);
				}
			}
			//All done. Let's update class path with the new one
			System.setProperty("java.class.path", newpath);
		} catch (Exception e){
			e.printStackTrace();
		}
	}//end method
//	---------------------------------------------------------------------------

//	---------------------------------------------------------------------------
	/**
	 * Add given file to running class path
	 * @param f File to add
	 */
	private static void addFile(File f) {
		try{
			//f = duplicateLib(f);
			if (f!=null) addURL(f, f.toURI().toURL());
		} catch (Exception e){
			//e.printStackTrace();
		}
	}//end method
//	---------------------------------------------------------------------------

//	---------------------------------------------------------------------------
	/**
	 * Duplicate a library in a temporary directory
	 * @param f The library to duplicate
	 * @return duplicated file
	 */
	/*private static File duplicateLib(File f){
		try{
			File strDirectoy = new File("./.tmp/").getAbsoluteFile();

		    // Create one directory
		    strDirectoy.mkdir();
		    String fname= f.getName();
		    String prefix = fname.substring(0, fname.lastIndexOf('.'));
		    String suffix = fname.substring(fname.lastIndexOf('.'));
		    File filedest = File.createTempFile(prefix,suffix,strDirectoy);
		    // Delete temp file when program exits.
		    filedest.deleteOnExit();

			InputStream in = new FileInputStream(f);
		    OutputStream out = new FileOutputStream(filedest);

		    // Transfer bytes from in to out
		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0) {
		           out.write(buf, 0, len);
		    }
		    in.close();
		    out.close();
		    return filedest;
		}catch (Exception e){
			//e.printStackTrace();
		}
		return null;
	}*/
//	---------------------------------------------------------------------------

//	---------------------------------------------------------------------------
	/**
	 * Add resource characterized by given URL to class path
	 * Let's do some hack on Java ClassLoader
	 * @param u the library URL to add
	 */
	private static void addURL(File f, URL u) {
		try {
			URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Class<URLClassLoader> sysclass = URLClassLoader.class;
			Method method = sysclass.getDeclaredMethod("addURL",parameters);
			method.setAccessible(true);
			method.invoke(sysloader,new Object[]{ u });
		} catch (Exception e) {
			//e.printStackTrace();
		}//end try catch
	}//end method
//	---------------------------------------------------------------------------



//	---------------------------------------------------------------------------
	/**
	 * @return files list contained in <directory> and matching accept() filter
	 */
	private String[] getFilesList(String path){
		//open given directory
		File f_directory = new File(path);
		//retrieve all files matching accept method
		return f_directory.list(this);
	}
//	---------------------------------------------------------------------------

//	---------------------------------------------------------------------------
	/**
	 * Implementation of FilenameFilter Interface
	 * Filter profiles directory to retain only profiles files
	 * @param dir  directory to check
	 * @param name file name to check
	 * @return true if the file name ends with $fileExtension, false otherwise
	 */
	public boolean accept(File dir,String name){
		for (String s:fileExtensions){
			if (name.endsWith(s)) return true;
		}
		return false;
	}
//	---------------------------------------------------------------------------


}
