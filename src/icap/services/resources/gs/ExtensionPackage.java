package icap.services.resources.gs;

import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import icap.services.ServicesProperties;
import org.apache.commons.compress.archivers.tar.*;



/**
 * Extension Package containing a packaged language for GreasySpoon  
 * @author Karel
 */
public class ExtensionPackage {

	private static String LibPath = "lib/";
	//private static String javaExtPath = System.getProperty("java.ext.dirs").split(File.pathSeparator)[0]+File.separatorChar;
	private static String javaExtPath = "pkg/ext/";
	
	
	private File extensionFile;
	Manifest manifest;
	String[] javaLibs = null, pkgExtensions=null;

	String pkgLibrary, pkgName, libVersion, pkgVersion, pkgPath, comment,statuscomment,pkgProps;
	String codedName;
	
	private boolean installationStatus = false;
	private StringBuilder message = new StringBuilder("");
	
	private static boolean debug = false;
	
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Load an extension from given package
	 * @param packagefile the file to parse
	 * @throws Exception
	 */
	public ExtensionPackage(String packagefile) throws Exception {
		extensionFile = new File(packagefile);
		JarFile jarfile = new JarFile(extensionFile);
		this.manifest = jarfile.getManifest();
		Attributes att = manifest.getMainAttributes();
		pkgName = att.getValue("Specification-Title");
		comment = att.getValue("Specification-Comment");
		libVersion = att.getValue("Specification-Version");
		pkgVersion = att.getValue("Implementation-Version");
		pkgLibrary = att.getValue("Greasyspoon-Library");
		pkgPath = att.getValue("GreasySpoon-Path");
		pkgExtensions = att.getValue("Greasyspoon-Library-JavaExtension")!=null?att.getValue("Greasyspoon-Library-JavaExtension").split(";"):null;
		
		//Prepend libraries repository to package paths 
		if (pkgPath!=null && !pkgPath.trim().equals("")) {
			String[] pathes = pkgPath.split(";");
			pkgPath = "";
			for (String s:pathes){
				if (s.startsWith("/") || s.startsWith("\\")){
					pkgPath += LibPath.substring(0, LibPath.length()-1)+s.trim()+";";
				} else pkgPath += LibPath+s.trim()+";"; 
			}
		}
		pkgProps = att.getValue("Package-Properties")!=null?att.getValue("Package-Properties"):null;
		
		if (pkgProps!=null && !pkgProps.trim().equals("")) {
			String absolutepath = new File(LibPath).getAbsolutePath()+File.separatorChar;
			pkgProps = pkgProps.replace("{$path}",absolutepath);
		}

		loadpending();
		javaLibs = att.getValue("Greasyspoon-Java")!=null?att.getValue("Greasyspoon-Java").split(";"):null;
		codedName = (pkgName.trim()+"-"+pkgVersion.trim()).toLowerCase();
		jarfile.close();
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	private void loadpending(){
		if (installationStatus==true) return;
		
		File uninstallFile = new File(ExtensionManagement.getExtensionPath()+codedName+".uninstall");
		if (!uninstallFile.exists()){
			return;
		}// Endif
		
		try {       
			BufferedReader breader = new BufferedReader(new FileReader(uninstallFile));
			StringBuilder sb = new StringBuilder();
			String str;
			//Read a line
			while ((str = breader.readLine()) !=null ){
				sb.append(str).append("\n");
			}//End while readLine
			breader.close();
			this.statuscomment =  "Following file were not deleted during uninstallation:\n"+sb.toString();
		} catch (IOException e){
		}//End try&catch
		return;
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Install this package locally
	 * @throws Exception
	 */
	public void install() throws Exception {
		long time = System.currentTimeMillis();
		JarFile jarfile = new JarFile(extensionFile);

		StringBuilder fileLog = new StringBuilder();
		
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		
		if (pkgLibrary!=null) {
			if (pkgLibrary.endsWith(".tar")){
				//tar.gz file: uncompress it
				extractFile(jarfile.getInputStream(jarfile.getEntry(pkgLibrary)),LibPath,new File(pkgLibrary).getName());
				if (debug) System.out.println("detaring :"+LibPath+pkgLibrary +" in " + LibPath);
				fileLog.append(uncompress(LibPath+pkgLibrary, LibPath));
				File tgzfile = new File(LibPath+pkgLibrary);
				if (debug) System.out.println("deleting :"+LibPath+pkgLibrary);
				tgzfile.delete();
			} else {
				fileLog.append(extractDir(jarfile.getEntry(pkgLibrary),jarfile.getName(),LibPath));
			}
			if (debug) System.out.println("extracted:"+pkgLibrary);
		}
		
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		
		File dir = new File(javaExtPath);
		if (! dir.exists()){
			dir.mkdir();
		}
		
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		
		if (javaLibs!=null) {
			for (String s:javaLibs){
				 fileLog.append(extractFile(jarfile.getInputStream(jarfile.getEntry(s)),javaExtPath,new File(s).getName()));
			}
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");

		if (pkgExtensions!=null){
			for (String s:pkgExtensions){
				 fileLog.append(copyfile(LibPath+s,javaExtPath+new File(s).getName()));
				 if (debug) System.err.println("installing file from package:"+LibPath+s+" == >"+javaExtPath+new File(s).getName());
			}
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		
		installationStatus = true;
		if (pkgPath!=null && !pkgPath.trim().equals("")) {
			ServicesProperties.setValue("path."+pkgName, pkgPath);
			ServicesProperties.save();
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		if (pkgProps!=null && !pkgProps.trim().equals("")) {
			ServicesProperties.setValue("props."+pkgName, pkgProps);
			ServicesProperties.save();
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		if (fileLog.length()>0) {
			File uninstallLogFile = new File(ExtensionManagement.getExtensionPath()+codedName+".uninstall");
			if (uninstallLogFile.exists()) {
				uninstallLogFile.delete();
				uninstallLogFile.createNewFile();
			}
			BufferedWriter uninstallLog = new BufferedWriter(new FileWriter(uninstallLogFile));
			uninstallLog.write(fileLog.toString());
			uninstallLog.close();
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		if (fileLog.length()==0) {
			throw new Exception("No file were installed - aborting");
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		long passed = (System.currentTimeMillis() - time)/1000;
		if (debug) System.err.println("Time spent:"+passed);
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	private static StringBuilder uncompress(String archive, String outputPath){
		StringBuilder directoriesList = new StringBuilder();
		StringBuilder fileList = new StringBuilder();
		int counter = 0;
		try{
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(archive));
			TarArchiveInputStream input = new TarArchiveInputStream(in);
			TarArchiveEntry entry = null;
			File extractionfile;
			byte[] buff = new byte[8096];
			while ( (entry = input.getNextTarEntry()) != null) {
				counter++;
				if (entry.isDirectory()){
					extractionfile = new File(outputPath, entry.getName());
					extractionfile.mkdirs();
					if (debug) System.err.println("making dir:"+extractionfile);
					directoriesList.append(extractionfile.getAbsolutePath()).append("\n");
				} else {
					extractionfile = new File(outputPath, entry.getName());
					if (debug) System.err.println("extracting file:"+extractionfile);
					OutputStream out = new FileOutputStream(extractionfile);
					long size = entry.getSize();
					long toread = size;
					int len;
					while ((len = input.read(buff, 0, toread>buff.length?buff.length:(int)toread) ) > 0){
						out.write(buff, 0, len);
						toread-=len;
					}
					out.close();
					fileList.append(extractionfile.getAbsolutePath()).append("\n");
				}
				extractionfile.setLastModified(entry.getModTime().getTime());
				String mode = Integer.toOctalString(entry.getMode());
				int ownerRgts = Integer.parseInt(mode.substring(0, 1));
				int groupRgts  = Integer.parseInt(mode.substring(1, 2));
				int othersRgts = Integer.parseInt(mode.substring(2,3));

				if (debug) {
					System.err.println("file properties:"
						+"groupname:"+entry.getGroupName()+"\r\n"
						+"getUserName:"+entry.getUserName()+"\r\n"
						+"getMode:"+ownerRgts+":"+groupRgts+":"+othersRgts+"\r\n"
						+"getModTime:"+entry.getModTime()+"\r\n"
						+"getLinkName:"+entry.getLinkName()+"\r\n"
						+"getUserId:"+entry.getUserId()+"\r\n"
						+"getGroupId:"+entry.getGroupId()+"\r\n"
					);
				}
				extractionfile.setExecutable(false);
				extractionfile.setReadable(false);
				extractionfile.setWritable(false);
				setRights(othersRgts,extractionfile,false);//defaults rights
				setRights(ownerRgts,extractionfile,true);//owner rights
				if (!icap.IcapServer.turnStdOff) {
					if (counter%10 ==0) System.out.print(".");
				}
			}
			in.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		return fileList.append(directoriesList);
	}
	
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	private static void setRights(int rights, File file, boolean owner){
		/*
		0  	---  	000  	All types of access are denied
		1 	--x 	001 	Execute access is allowed only
		2 	-w- 	010 	Write access is allowed only
		3 	-wx 	011 	Write and execute access are allowed
		4 	r-- 	100 	Read access is allowed only
		5 	r-x 	101 	Read and execute access are allowed
		6 	rw- 	110 	Read and write access are allowed
		7 	rwx 	111 	Everything is allowed
		*/
		if ((rights&4)==4) file.setReadable(true,owner);
		if ((rights&2)==2) file.setWritable(true,owner);
		if ((rights&1)==1) file.setExecutable(true,owner);
		
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Copy file
	 * @param srFile source file
	 * @param dtFile destination file
	 * @return dtFile if successful, empty string otherwise
	 */
	private static String copyfile(String srFile, String dtFile){
	    try{
	      File f1 = new File(srFile);
	      File f2 = new File(dtFile);
	      InputStream in = new FileInputStream(f1);
	      
	      //For Append the file.
	      //OutputStream out = new FileOutputStream(f2,true);

	      //For Overwrite the file.
	      OutputStream out = new FileOutputStream(f2);

	      byte[] buf = new byte[1024];
	      int len;
	      while ((len = in.read(buf)) > 0){
	        out.write(buf, 0, len);
	      }
	      in.close();
	      out.close();
	    }
	    catch(FileNotFoundException ex){
	    	return "";
	    }
	    catch(IOException e){
	    	return "";
	    }
	    return dtFile;
	  }
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Uninstall this package locally
	 * @throws Exception
	 */
	public void uninstall() throws Exception {
		if (installationStatus==false) return;
	
		File uninstallFile = new File(ExtensionManagement.getExtensionPath()+codedName+".uninstall");
		if (!uninstallFile.exists()){
			System.err.println("Error: Cannot find install log <"+uninstallFile.toString()+"> needed to remove package "+this.pkgName);
			return;
		}// Endif
		
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		
		StringBuilder failedFiles = new StringBuilder();
		try {       
			BufferedReader breader = new BufferedReader(new FileReader(uninstallFile));
			String str;
			int counter =0;
			//Read a line
			while ((str = breader.readLine()) !=null ){
				counter++;
				File lib = new File(str);
				if (!lib.exists()) continue;
				if (lib.isDirectory()) {
					if (!deleteDirectory(lib,true)){
						failedFiles.append(lib.getAbsolutePath()).append("\n");
					}
				} else if (!lib.delete()){
					failedFiles.append(lib.getAbsolutePath()).append("\n");
				}
				if (!icap.IcapServer.turnStdOff) {
					if (counter%10 ==0) System.out.print(".");
				}
			}//End while readLine
			breader.close();       
		} catch (IOException e){
			System.err.println("Error in uninstall file <"+uninstallFile.toString()+">. File corrupted.");
		}//End try&catch
		uninstallFile.delete();
		
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		
		if (failedFiles.length()>0){
			uninstallFile.createNewFile();
			BufferedWriter uninstallLog = new BufferedWriter(new FileWriter(uninstallFile));
			uninstallLog.write(failedFiles.toString());
			uninstallLog.close();
			statuscomment = "Following file could not be deleted:\n"+failedFiles.toString();
		}
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
		ServicesProperties.removeValue("path."+pkgName);
		ServicesProperties.removeValue("props."+pkgName);
		ServicesProperties.save();
		if (!icap.IcapServer.turnStdOff) System.out.print(".");
/*
		//Other method: suppress directories - simpler, faster, but !! risky !!
		if (pkgLibrary!=null){
			File libraryDir = new File(LibPath+pkgLibrary);
			deleteDirectory(libraryDir, false);
		}
		if (javaLibs!=null) {
			for (String s:javaLibs){
				File javalib = new File(javaExtPath+new File(s).getName());
				if (debug) System.err.println("Deleting:"+javalib);
				if (javalib.exists()) {
					if (!javalib.delete()){
						installLog.write(javalib.getAbsolutePath());
						javalib.deleteOnExit();
					} else {
						if (debug) System.err.println("Deleted:"+javalib);
					}
				} else {
					if (debug) System.err.println("File not founded:"+javalib);
				}
			}
		}
*/		
	}
	//<------------------------------------------------------------------------------------------>


	//<------------------------------------------------------------------------------------------>
	/**
	 * Delete Directory recursively
	 * @param path The directory to delete
	 * @param dirsOnly Set if only empty sub-directories are deleted, or if included files are also deleted
	 * @return true if directory was correctly deleted
	 */
	private boolean deleteDirectory(File path, boolean dirsOnly) {
		String fullpath = path.getAbsolutePath();
		if (debug) System.err.println(fullpath);
		if (!fullpath.startsWith(LibPath) && !dirsOnly) {
			//logInstalledFile(fullpath);
			if (debug) System.err.println("Error: invalid AND potentialy dangerous GreasySpoon package definition");
			return false;
		}
		
		if( path.exists() ) {
			if (debug) System.err.println("Deleting:"+ path);
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i],dirsOnly);
				}
				else {
					if (!dirsOnly) files[i].delete();
				}
			}
		}
		return( path.delete() );
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	public String toString(){
		String tostring = pkgName+" Extension Package"
		+ (comment==null?"":("\r\n"+comment))
		+ "\r\nLibrary version: "+libVersion
		+ "\r\nPackage version: "+pkgVersion
		+ "\r\nLibrary location: "+(pkgLibrary==null?"none":pkgLibrary)
		+ "\r\nLibrary path: "+(pkgPath==null?"none":pkgPath)
		+ "\r\nPackage ID: "+this.codedName
		+ "\r\nJVM Extensions: ";
		if (javaLibs!=null) {
			for (String s:javaLibs){
				tostring+=s+";";
			}
		} else tostring+="NONE";
		tostring+= "\r\nstatus: "+(this.installationStatus?"installed":"uninstalled")+"\n";
		if (statuscomment!=null && statuscomment.length()>0) {
			tostring+= statuscomment;
		}
		return tostring;
	}
	/**
	 * @return HTML formated version of the toString() method
	 */
	public String toHtml(){
		String tostring = "<b>"+pkgName+" Extension Package</b>"
		+ (comment==null?"":("<br /><i>"+comment+"</i>"))
		+ "<br /><b>Library version: </b>"+libVersion
		+ "<br /><b>Package version: </b>"+pkgVersion
		+ "<br /><b>Library location: </b>"+(pkgLibrary==null?"<i>none</i>":pkgLibrary)
		+ "<br /><b>Library path: </b>"+(pkgPath==null?"<i>none</i>":pkgPath)
		+ "<br /><b>Package ID: </b>"+this.codedName
		+ "<br /><b>JVM Extensions: </b>";
		if (javaLibs!=null) {
			for (String s:javaLibs){
				tostring+=s+";";
			}
		} else tostring+="NONE";
		tostring+= "<br /><b>status: </b>"+(this.installationStatus?"installed":"uninstalled")+"<br />";
		if (statuscomment!=null && statuscomment.length()>0) {
			tostring+= "<FONT COLOR=RED>"+statuscomment.replace("\n", "<br />")+"</FONT>";
		}
		return tostring;
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Extract compressed object to given file(path+name) 
	 * @param zipobject The compressed object
	 * @param path the path where to extract file
	 * @param name the name of the file to create
	 * @return name of the created (extracted) file
	 * @throws Exception
	 */
	private String extractFile(InputStream zipobject,String path, String name)  throws Exception {
		byte[] buf = new byte[1024];
		int n;
		File f = new File(path+name);
		if (f.exists()) {
			message.append(path+name).append("\n");
			return "";
		}
		FileOutputStream fileoutputstream = new FileOutputStream(f);
		while ((n = zipobject.read(buf, 0, 1024)) > -1){
			fileoutputstream.write(buf, 0, n);
		}
		fileoutputstream.close(); 
		zipobject.close();
		return path+name+"\n";
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Extract compressed directory to given path 
	 * @param zipentry the compressed directory to extract
	 * @param zipFile the file containing the compressed directory
	 * @param extractionpath the path where to extract the directory
	 * @return List of extracted directories and file
	 * @throws Exception
	 */
	private StringBuilder extractDir(ZipEntry zipentry, String zipFile,String extractionpath) throws Exception {
		byte[] buf = new byte[8128];
		int n;
		String entryName;
		String rootPath = zipentry.getName();

		//Cleanup name by using generic path separator and
		rootPath = rootPath.replace('\\', '/');
		File newFile = new File(extractionpath+rootPath);
		newFile.mkdirs();
		if (debug)System.out.println("creating repository:"+newFile);
		//lets inspect the zip file
		ZipInputStream zipinputstream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
		BufferedOutputStream bos;
		StringBuilder directoriesList = new StringBuilder();
		StringBuilder fileList = new StringBuilder();
		while ( (zipentry = zipinputstream.getNextEntry()) != null) {
			
			//for each entry to be extracted
			entryName = zipentry.getName();
			//Cleanup name by using generic path separator and
			entryName = entryName.replace('\\', '/');
			if (debug)System.out.println("extracting:"+entryName);
			//check that entry match library path
			if (!entryName.startsWith(rootPath) || entryName.contains("..")) {
				if (debug) System.out.println("SKIP: entryname: ["+entryName+ "] ==> [" + rootPath +"]");
				zipinputstream.closeEntry();
				continue;
			}
			newFile = new File(entryName);
			if (newFile.exists()){
				zipinputstream.closeEntry();
				message.append(entryName).append("\n");
				continue;
			}
			
			//if entry is a path, just create it
			if (entryName.endsWith("/")){
				if (debug)System.out.println("Entry is a path - creating it: "+entryName);
				File destfile =  new File(extractionpath+entryName);
				destfile.mkdirs();
				directoriesList.append(extractionpath).append(entryName).append("\n");
				continue;
			} else {
				//extract file content
				bos = new BufferedOutputStream(new FileOutputStream(extractionpath+entryName));
				while ((n = zipinputstream.read(buf)) > -1){
					bos.write(buf, 0, n);
				}
				if (debug)System.out.println("Entry is a file - extracting it to "+extractionpath+entryName);
				bos.close();
				fileList.append(extractionpath).append(entryName).append("\n");
			}
			//close entry
			zipinputstream.closeEntry();
		}//while
		zipinputstream.close();
		return fileList.append(directoriesList);
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * @return True if this package is already installed locally
	 */
	public boolean isInstalled(){
		return this.installationStatus;
	}
	//<------------------------------------------------------------------------------------------>

	//<------------------------------------------------------------------------------------------>
	/**
	 * Set the installation status for this package
	 * @param isInstalled 
	 */
	public void setInstalled(boolean isInstalled){
		this.installationStatus = isInstalled;
		this.loadpending();
	}
	//<------------------------------------------------------------------------------------------>


	/**
	 * @return Returns the codedName.
	 */
	public String getCodedName() {
		return codedName;
	}

	/**
	 * @return Returns the codedName.
	 */
	public String getName() {
		return this.pkgName;
	}


	/**
	 * @return Returns the libVersion.
	 */
	public String getLibVersion() {
		return libVersion;
	}


	/**
	 * @return Returns the pkgVersion.
	 */
	public String getPkgVersion() {
		return pkgVersion;
	}


	/**
	 * @return Returns the extensionFile.
	 */
	public File getExtensionFile() {
		return extensionFile;
	}



}
