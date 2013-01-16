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
 package icap.services;

/////////////////////////////////
// Import
import java.util.*;
import java.io.*;
import tools.logger.Log;
/////////////////////////////////


/**
 * Properties loader for ICAP services resources
 */
public class ServicesProperties {
	
	private static final String BUNDLE_NAME = "services";
	private static Properties properties = new Properties();
	static private String configurationFilename = "conf"+File.separator+BUNDLE_NAME+".properties";
	
//  <------------------------------------------------------------------------->
	static {
		try {
			properties.load(new FileInputStream(configurationFilename));
		} catch (Exception e){
			Log.error(Log.SEVERE,"Error loading configuration file:"+configurationFilename,e);
			System.exit(1);
		}
	}
//  <------------------------------------------------------------------------->

//  <------------------------------------------------------------------------->
	private ServicesProperties() {
		//		do nothing
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Reload resource bundle
	 */
	public static void refresh(){
		//RESOURCE_BUNDLE.clearCache();
		//RESOURCE_BUNDLE =  ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
		properties.clear();
		try {
			properties.load(new FileInputStream(configurationFilename));
		}catch (Exception e){
			e.printStackTrace();
		}
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Retrieve value associated with given key
	 * @param key The properties key to retrieve
	 * @return associated value, or empty string if unavailable
	 */
	public static String getString(String key) {
		try {
			//return RESOURCE_BUNDLE.getString(key).replace("\\r\\n", "\r\n");
			return properties.getProperty(key).replace("\\r\\n", "\r\n");
		} catch (Exception e) {
			return null;
		}
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Retrieve value associated with given key
	 * @param key The properties key to retrieve
	 * @param defaultvalue default value to use if key is not set
	 * @return associated value, or empty string if unavailable
	 */
	public static String getString(String key,String defaultvalue) {
		if (!properties.containsKey(key)) return defaultvalue;
		return properties.getProperty(key).replace("\\r\\n", "\r\n");
	}
//  <------------------------------------------------------------------------->
	
	
//  <------------------------------------------------------------------------->
	/**
	 * Return parameter value as a boolean
	 * @param key Parameter name to look for
	 * @return true if parameter exists and value is equals to true, on, enable or enabled (case insensitive)
	 */
	public static boolean getBooleanValue(String key){
		if (!properties.containsKey(key)) return false;
		String value = getString(key).trim().toLowerCase();
		if (value.equals("true") || value.equals("on") || value.equals("enable") || value.equals("enabled") || value.equals("yes") ) {
			return true;
		}
		return false;
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Configure value associated with given key
	 * @param key The properties key to configure
	 * @param value The value to set for given key
	 */
	public static void setValue(String key, String value) {
		//String encodedValue = value.replace("\\", "\\\\");
		String encodedValue = value.replace("\r\n", "\\r\\n");
		properties.setProperty(key,encodedValue);
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Remove the value associated with given key
	 * @param key The properties key to delete
	 */
	public static void removeValue(String key) {
		properties.remove(key);
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * @return The list of keys as Set of Strings
	 */
	public static Set<String> stringPropertyNames(){
		return properties.stringPropertyNames();
	}
//  <------------------------------------------------------------------------->
	
//  <------------------------------------------------------------------------->
	/**
	 * Configure value associated with given key
	 */
	public static void save() {
		try {
			FileOutputStream out = new FileOutputStream(configurationFilename);
			properties.store(out, "ICAP Services Properties");
			out.close();
		} catch (Exception e){
			Log.error(Log.CONFIG, "Error while saving ICAP Services properties:",e);
		}
	}
//  <------------------------------------------------------------------------->

}
