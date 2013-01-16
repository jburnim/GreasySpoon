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
package icap.services.resources.gs;

///////////////////////////////////
//Import
import java.io.*;
import java.util.*;
///////////////////////////////////


/**
 * Class used to backup/restore configuration
 */
public class ExtensionManagement {

	/**Path to directory containing extension packages*/
	public static String extensionPath = "pkg"+File.separatorChar;
	
	/**File Extension used for extension packages*/
	public static String extensionName = ".gsx";
	
	/**Properties file containing installed packages*/
	private final static String installedPackages = "extensions.ini";
	private final static String installedtag = "installed.";
	

	/**List of existing extension packages*/
	static Vector<ExtensionPackage> packages = new Vector<ExtensionPackage>();
	static Properties packageStatus = new Properties();


	private final static boolean debug = false;

	//<------------------------------------------------------------------------------------------>
	/**
	 * @return Absolute path of extension directory (with trailing '/')
	 */
	public static String getExtensionPath(){
		return new File(extensionPath).getAbsolutePath()+File.separatorChar;
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Install extension identified with given ID 
	 * @param extensionID the extension ID
	 * @return true if extension has been correctly installed, false otherwise
	 */
	public static synchronized boolean installExtension(String extensionID) {

		for (ExtensionPackage ep:packages){
			if (debug) System.err.println("I: ["+extensionID.toLowerCase().trim()+"] ["+ep.codedName+"]");
			if (ep.codedName.equals(extensionID.toLowerCase().trim())) {
				if (ep.isInstalled()) {
					if (debug) System.err.println("Package already installed");
					return false;
				}
				
				try {
					if (debug) System.err.println("Installing extension "+extensionID+" ....");
					if (!icap.IcapServer.turnStdOff) System.out.println("Please wait, this can take some minutes.");
					ep.install();
					ep.setInstalled(true);
					int freeid = 0;
					while (packageStatus.containsKey(installedtag+freeid))freeid++;
					packageStatus.setProperty(installedtag+freeid, ep.codedName);
					packageStatus.store(new FileWriter(extensionPath+installedPackages), "GreasySpoon - List of installed packages");
					if (debug) System.err.println("...Installed !");
					return true;
				} catch (Exception e){
					if (debug) e.printStackTrace();
					return false;
				}
			}
		}//end for packages
		return false;
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Remove extension identified with given ID 
	 * @param extensionID the extension ID
	 * @return true if extension has been correctly removed, false otherwise
	 */
	public static synchronized boolean unInstallExtension(String extensionID) {
		for (ExtensionPackage ep:packages){
			if (debug) System.err.println("I: ["+extensionID.toLowerCase().trim()+"] ["+ep.codedName+"]");
			if (ep.codedName.equals(extensionID.toLowerCase().trim())) {
				if (! ep.isInstalled()) {
					if (debug) System.err.println("Package not installed - skipped");
					return false;
				}
				
				try {
					if (debug) System.err.println("Uninstalling extension "+extensionID+" ....");
					if (!icap.IcapServer.turnStdOff) System.out.println("Please wait, this can take some minutes.");
					ep.uninstall();
					ep.setInstalled(false);
					int freeid = 0;
					while (packageStatus.containsKey(installedtag+freeid))freeid++;
					for (Object key:packageStatus.keySet()){
						if (packageStatus.get(key).toString().equalsIgnoreCase(ep.codedName)){
							packageStatus.remove(key);
							break;
						}
					}
					packageStatus.store(new FileWriter(extensionPath+installedPackages), "GreasySpoon - List of installed packages");
					if (debug) System.err.println("...Uninstalled !");
					return true;
				} catch (Exception e){
					if (debug) e.printStackTrace();
					return false;
				}
			}
		}//end for packages
		return false;
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * @param extensionID Delete given extension
	 */
	public static void deleteExtension(String extensionID){
		for (ExtensionPackage ep:packages){
			if (ep.getCodedName().equals(extensionID)){
				File f = ep.getExtensionFile();
				if (f.exists() && f.canWrite()){
					f.delete();
				}
				loadExtensions();
				return;
			}
		}
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Load all available extensions
	 */
	public static void loadExtensions(){
		String[] extensions = getExtensionsList();
		packages.clear();
		try {
			for (String e:extensions){
				if (debug) System.err.println("loading:"+extensionPath+e);
				packages.add(new ExtensionPackage(extensionPath+e));
			}
		} catch (Exception e){
			if (debug) e.printStackTrace();
		}
		File f = new File(extensionPath+installedPackages);
		try{
			if (!f.exists()) f.createNewFile();
			packageStatus.load(new FileReader(f));
		} catch (Exception e){
			if (debug) e.printStackTrace();	
		}
		for (String s:packageStatus.stringPropertyNames()){
			if (! s.startsWith(installedtag)) continue;
			String packageName = packageStatus.getProperty(s,"");
			for (ExtensionPackage ep:packages){
				if (ep.codedName.equals(packageName.toLowerCase().trim())) {
					ep.setInstalled(true);
					break;
				}
			}//end for packages
		}//End for properties
		for (ExtensionPackage ep:packages){
			if (!ep.isInstalled()) {
				ep.setInstalled(false);
			}
		}//end for packages
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * @return Vector containing the available extensions
	 */
	public static Vector<ExtensionPackage> getExtensions() {
		if (debug) for (ExtensionPackage pkg:packages) System.err.println(pkg.toString());
		return packages;
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Echo extensions list to System.out
	 */
	public static void echoExtensionsList() {
		System.out.println("Extension packages:");
		for (ExtensionPackage pkg:packages) {
			System.out.println("----------------------------------");
			System.out.println(pkg.toString());
		}
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * @return files list contained in <directory>
	 */
	public static String[] getExtensionsList(){
		//open given directory
		File f_directory = new File(extensionPath);
		//retrieve all files matching accept method
		String[] filelist = f_directory.list();
		Vector<String> acceptedfiles = new Vector<String>(); 
		for (String filename:filelist){
			if (filename.endsWith(extensionName)){
				acceptedfiles.add(filename);
				if (debug) System.err.println("getExtensionsList:"+filename);
			}
		}
		return acceptedfiles.toArray(new String[0]);
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 *  Test it
	 **/
	/*public static void main(String[] args){
		loadExtensions();
		getExtensions();
		echoExtensionsList();
		//JRuby-1.3.1-1.0.0-b01
		//installExtension("jruby-1.0.1");
		//unInstallExtension("JRuby-1.3.1-1.0.0-b01");

		installExtension("java-1.0.0-b01");
		unInstallExtension("java-1.0.0-b01");
		if (installExtension("java-1.0.0-b01")) unInstallExtension("java-1.0.0-b01");
	}*/
	//<------------------------------------------------------------------------------------------>
}
