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
package tools.httpserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import tools.httpserver.custom.ProjectSpecifics;
import java.util.*;
import java.util.zip.*;
import java.io.*;


/**
 * Class used to backup/restore configuration
 */
public class Maintenance {

	//<------------------------------------------------------------------------------------------>
	/**
	 * Backup running configuration
	 */
	public synchronized static void backupAll(){
		try {
			String nameToZip = HttpServer.getPathToFiles()+"/backup/"+ProjectSpecifics.getBackupPrefix()+"-"+System.currentTimeMillis()+".zip";
			Hashtable<File, String> filesToBackup = ProjectSpecifics.getFilesToBackup();
			zipFiles(nameToZip, filesToBackup);
		} catch (Exception e){
			//e.printStackTrace();
		}
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Backup running configuration
	 */
	public synchronized static void backupCommons(){
		try {
			String nameToZip = HttpServer.getPathToFiles()+"/backup/"+ProjectSpecifics.getCommonsPrefix()+"-"+System.currentTimeMillis()+".zip";
			Hashtable<File, String> filesToBackup = ProjectSpecifics.getCommonFilesToBackup();
			zipFiles(nameToZip, filesToBackup);
		} catch (Exception e){
			//e.printStackTrace();
		}
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Store provided backup content on disk.
	 * Content must be provided using multipart format
	 * @param contentType Initial content type of the response
	 * @param contentFile the content to store
	 */
	public synchronized static void storeBackup(String contentType, ByteArrayOutputStream contentFile){
		try{
			String tmp = contentFile.toString("ISO-8859-1");
			int p1, p2;
			//System.err.println(tmp);
			p1 = contentType.indexOf("boundary=") + "boundary=".length();
			String boundary = "\r\n--"+contentType.substring(p1).trim();
			//System.err.println("Boundary:"+boundary);
			
	
			p1 = tmp.indexOf("filename=\"") + "filename=\"".length();
			p2 = tmp.indexOf("\"", p1);
			String name = HttpServer.getPathToFiles()+"/backup/"+tmp.substring(p1, p2);
			
			//System.err.println("filename:"+name);
			int offset = tmp.indexOf("\r\n\r\n", p2)+4;
			byte[] content = contentFile.toByteArray();
			
			int length = tmp.indexOf(boundary, offset) - offset;
			//System.err.println("offset:"+offset+"/length:"+length);
			//System.err.println("contentFile size:"+contentFile.size());
		
			FileOutputStream fos = new FileOutputStream(name);
			fos.write(content, offset, length);
			fos.flush();
			fos.close();
		}catch (Exception e){
			//error = e.getLocalizedMessage();
			//e.printStackTrace();
		}
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Restore given backup
	 * @param filename The ZIP archive containing the backup file
	 */
	public synchronized static void restoreBackup(String filename){
		//System.err.println("restoring:"+filename);
		unZipFiles(".", filename);
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * @return Path of backup directory
	 */
	public static String getBackupPath(){
		return HttpServer.getPathToFiles()+"/backup/";
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Compress files into a ZIP archive
	 * @param zipFile the ZIP file to create
	 * @param filesToAdd The files to add, associated to the path and name to store in ZIP archive 
	 * @return true if compression has been done correctly, false otherwise
	 */
	private synchronized static boolean zipFiles(String zipFile, Hashtable<File, String> filesToAdd) {
		try{
			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			int bytesRead;
			byte[] buffer = new byte[1024];
			CRC32 crc = new CRC32();
			Set<File> files = filesToAdd.keySet();
			for (File file:files) {
				//File file = new File(name);
				if (!file.exists()) {
					continue;
				}
				//System.err.println("Backuping: " + file.getAbsolutePath());

				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
				crc.reset();
				while ((bytesRead = bis.read(buffer)) != -1) {
					crc.update(buffer, 0, bytesRead);
				}
				bis.close();
				// Reset to beginning of input stream
				bis = new BufferedInputStream(new FileInputStream(file));
				ZipEntry entry = new ZipEntry(filesToAdd.get(file));
				entry.setMethod(ZipEntry.STORED);
				entry.setCompressedSize(file.length());
				entry.setSize(file.length());
				entry.setCrc(crc.getValue());
				zos.putNextEntry(entry);
				while ((bytesRead = bis.read(buffer)) != -1) {
					zos.write(buffer, 0, bytesRead);
				}
				bis.close();
			}
			zos.close();
			return true;
		} catch (Exception e){
			//e.printStackTrace();
		}
		return false;
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Extract files in given ZIP archive to given path
	 * Note1: directory structure is not created
	 * Note2: existing files are overwritten
	 * Note3: ".." is forbidden in path for security reasons 
	 * @param path The path where to extract files
	 * @param filename The ZIP archive path and filename
	 */
	public static void unZipFiles(String path, String filename) {
		try {
			byte[] buf = new byte[1024];
			ZipInputStream zipinputstream = null;
			ZipEntry zipentry;
			zipinputstream = new ZipInputStream(new FileInputStream(filename));

			zipentry = zipinputstream.getNextEntry();
			while (zipentry != null) { 
				//for each entry to be extracted
				String entryName = zipentry.getName();
				
				//Cleanup name by using generic path separator and
				entryName = entryName.replace('\\', '/');
				if (!entryName.startsWith("/")) entryName = "/"+entryName;

				//check that there is no try to move out of GS directory
				if (entryName.contains("..")) continue;
				
				int n;
				File newFile = new File(entryName);
				String directory = newFile.getParent();

				if(directory == null) {
					if(newFile.isDirectory()) break;
				}

				FileOutputStream fileoutputstream = new FileOutputStream(path+entryName);             

				while ((n = zipinputstream.read(buf, 0, 1024)) > -1){
					fileoutputstream.write(buf, 0, n);
				}

				fileoutputstream.close(); 
				zipinputstream.closeEntry();
				zipentry = zipinputstream.getNextEntry();
			}//while
			zipinputstream.close();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	//<------------------------------------------------------------------------------------------>

}
