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
package tools.httpserver.custom;
/////////////////////////////////////
//Imports
import icap.IcapServer;
import icap.services.resources.gs.SpoonScript;

import java.io.File;
import java.util.*;

import tools.httpserver.HttpServer;
/////////////////////////////////////

/**
 * Centralize specific project properties access
 */
public class ProjectSpecifics {

//	-----------------------------------------------------------------------------
	/**
	 * @return Project version
	 */
	public static String getProjectVersion(){
		return IcapServer.version;
	}
//	-----------------------------------------------------------------------------

//	<------------------------------------------------------------------------------------------>
	/**
	 * Restart application 
	 */
	public static void restartApplication(){
		IcapServer.restart();
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * Files to be stored in the backup
	 * @return a hash table containing Files to backup with the associated name and path to use in archive
	 */
	public static Hashtable<File, String> getFilesToBackup(){
		Hashtable<File, String> result = new Hashtable<File, String>();
		//SpoonScript script;
        Vector<SpoonScript> scripts = new Vector<SpoonScript>(Arrays.asList(icap.services.GreasySpoon.reqSpoonScripts));
        scripts.addAll(Arrays.asList(icap.services.GreasySpoon.respSpoonScripts));
		for (SpoonScript script : scripts){
			result.put(script.getFile(), "/serverscripts/"+script.getFile().getName());
		}
		storeFilesUnder(IcapServer.confDirectory, result, IcapServer.confDirectory);
		result.put(new File(HttpServer.getPathToFiles()+"/stats/systemload.rrd"), "/admin/stats/systemload.rrd");
		return result;
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Common files to be stored in the backup
	 * @return a hash table containing Files to backup with the associated name and path to use in archive
	 */
	public static Hashtable<File, String> getCommonFilesToBackup(){
		Hashtable<File, String> result = new Hashtable<File, String>();
		//SpoonScript script;
        Vector<SpoonScript> scripts = new Vector<SpoonScript>(Arrays.asList(icap.services.GreasySpoon.reqSpoonScripts));
        scripts.addAll(Arrays.asList(icap.services.GreasySpoon.respSpoonScripts));
		for (SpoonScript script : scripts){
			result.put(script.getFile(), "/serverscripts/"+script.getFile().getName());
		}
		return result;
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Add all files contained in given path to storage
	 * @param path The path to explore
	 * @param storage Hash table in which to store files
	 * @param storepath The location in which to store backup
	 */
	private static void storeFilesUnder(String path, Hashtable<File, String> storage,  String storepath){
		String[] files = new File(path).list();
		File f;
		//Retrieve files names
		for (String s:files){
			f = new File(path+s);
			if (f.isDirectory()) continue;
			storage.put(f, storepath+s);
		}
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * @return prefix name for backup archives containing all files 
	 * (including local specific files such as configuration)
	 */
	public static String getBackupPrefix(){
		return "backup-gs-"+ProjectSpecifics.getProjectVersion();
	}
//	<------------------------------------------------------------------------------------------>

//	<------------------------------------------------------------------------------------------>
	/**
	 * @return prefix name for backup archives containing generic files such as scripts
	 * Correspond to files that can are not linked to local configuration (network, ...) 
	 */
	public static String getCommonsPrefix(){
		return "backup-scripts-"+ProjectSpecifics.getProjectVersion();
	}
//	<------------------------------------------------------------------------------------------>

	

}
