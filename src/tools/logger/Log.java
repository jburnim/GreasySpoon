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
 * Created     : 2/06/01
 *--------------------------------------------------------*/
 
package tools.logger;

///////////////////////////////////
// 	Import
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.*;
import java.util.logging.Level;
///////////////////////////////////

//-----------------------------------------------------------------------------

/**
 * Threaded server allowing to store messages in logs file with log rotation, 
 * zip compression of rotated files and files deletion if file number exceeds 
 * a defined limit.<br>
 * Started before Apache log4j, kept because it still provides features not
 * directly embedded in Log4j: compression, files rotation and overwriting. 
 * @version 1.0, 6/08/03
 * @author Karel Mittig 
 */
public class Log extends Level{ 

	//final private static boolean debug = false; 
	private static final long serialVersionUID = 7129015785377379428L;

	//Log file in which to store messages
    /**Redirect messages in access log file (normally used to trace applicative traffic)*/
	public final static short ACCESS		= 0;
	/**Redirect messages  in server log file (normally used to trace server configuration/administration)*/
	public final static short ERROR 		= 1;
	/**Redirect messages in debug log file (normally used for debugging)*/
	public final static short TRACE			= 2;
	/**Redirect messages in service log file (normally used for service operations)*/
	public final static short SERVICE		= 3;
	
	/**Redirect messages  in error log file (normally used to trace server configuration/errors)*/	
	public final static short ADMIN 		= 4;
	
	/**flag used to force logs rotation / cleaning */
    public final static short MAINTENANCE       = 255;
	
	/**logs directory*/
	protected static String logPath = "./";
	/**file extension for log files*/
	protected static String extension = ".log";

	/**Supported log files*/
	protected static File debugLog;
	protected static File errorLog;
	protected static File adminLog;
	protected static File accessLog;
	protected static File serviceLog;

	
	/**Thread used to compress rotated log files (under zip format)*/
	protected Zipper zipper;
	
	/**
	* Threshold entries limit in after which a log file is rotated
	* Default value is set to 100000, which corresponds to an average log file size of
	* 0->8000 kB, and zipped files of 125 kB.
	*/
	protected static int maxentries = 10000;
	
	/**
	* Log files Threshold after which oldest files are deleted.
	* Default value is set to 100 (of each log file type)
    * Set to -1 for unlimited files
	* If threshold is reached, oldest file is deleted before log rotation
	*/
	protected static int maxfiles = 100;
	
	/**
	* Set if disk access must be buffered or not
	* Buffered mode must be preferred for "high performance" solutions, while
    * unbuffered access must be used for real time log processing.
	* Disk access is unbuffered by default.
	*/
	protected final static boolean isBuffered = false;
	
	
	/**internal log entries counters.*/
	private static int accessEntriesCounter = 0;
	private static int errorEntriesCounter = 0;
	private static int adminEntriesCounter = 0;
	private static int debugEntriesCounter = 0;
	private static int serviceEntriesCounter = 0;

	/**Internal debug level value.*/
	private static Level loglevel = Level.INFO;
	
	/**Log files names*/
	protected static String debugName	= "debug";
	protected static String errorName 	= "error";
	protected static String adminName 	= "admin";
	protected static String accessName 	= "access";
	protected static String serviceName = "service";
	
	/**Internal flows*/
	private static BufferedOutputStream accessBufferedOutputStream;
	private static BufferedOutputStream adminBufferedOutputStream;
	private static BufferedOutputStream errorBufferedOutputStream;
	private static BufferedOutputStream debugBufferedOutputStream;
	private static BufferedOutputStream serviceBufferedOutputStream;

	/**Variable interne.*/
	private static FileOutputStream accessFileoutput;
	private static FileOutputStream debugFileoutput;
	private static FileOutputStream errorFileoutput;
	private static FileOutputStream adminFileoutput;
	private static FileOutputStream serviceFileoutput;

	/**Static class instance for static access*/
	private static Log logger = new Log();
    /**Platform line separator*/
	public static final String returnchar = System.getProperty("line.separator");
	
	private static boolean enabled = false;
	private static boolean debugEnabled = true;
	private static boolean errorEnabled = true;
	private static boolean adminEnabled = true;
	private static boolean accessEnabled = true;
	private static boolean serviceEnabled = true;
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
/**Constructor, initialize logs files, archive existing logs if not empty*/
private Log() {
    super("Rotation log",Level.INFO.intValue());
}
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
/**
 * Modify log repository<br>
 * <b>Provided directory MUST exist</b>
 * @param path the new Path where logs must be stored
 */
public static void setLogPath(String path){
    File f= new File(path);
    if (!f.isDirectory()){
    	System.err.println("Invalid log directory ["+path+"]");
    	return;
    }
    if (logPath.equals(f.getAbsolutePath()+File.separator)) return;
 	logPath = f.getAbsolutePath()+File.separator;
	if (enabled) {
		disable();
		enable();
	}
 	Log.error(Log.CONFIG,"Log directory set to ["+logPath+"]");
}

/**
 * @return directory in which log files are stored
 */
public static  String getLogPath(){return logPath;}
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
/**
 * Initialize and enable the Log process.
 * Create a static log object that can be used by any class to store log infos. 
 * This method MUST be called before generating logs (orelse log messages will be discarded).
 */
public static void enable(){
	logger.init();
	enabled = true;
}
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
/**
 * @return true if logs are enabled
 */
public static boolean isEnable(){
	return enabled;
}
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
/**
 * Stop the Log process
 */
public static void disable(){
	logger.finalize();
	enabled = false;
}
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
/**
 * Initialize the different logs files (access, error, debug)
 * Note: debug file is only initialized if debug > NONE (0)
 */
private void init(){
	try {
			if (accessBufferedOutputStream==null){
				accessLog = new File(logPath+accessName+extension);
			  	accessFileoutput = new FileOutputStream(accessLog,true);
				accessBufferedOutputStream = new BufferedOutputStream(accessFileoutput);
			}
			if (errorBufferedOutputStream==null){
				errorLog = new File(logPath+errorName+extension);
		   		errorFileoutput = new FileOutputStream(errorLog,true);
				errorBufferedOutputStream = new BufferedOutputStream(errorFileoutput);
			}
			if (loglevel!=Level.OFF &&debugBufferedOutputStream==null) {
				debugLog = new File(logPath+debugName+extension);
		   		debugFileoutput = new FileOutputStream(debugLog,true);
				debugBufferedOutputStream = new BufferedOutputStream(debugFileoutput);
			}
			if (serviceBufferedOutputStream==null) {
				serviceLog = new File(logPath+serviceName+extension);
		   		serviceFileoutput = new FileOutputStream(serviceLog,true);
				serviceBufferedOutputStream = new BufferedOutputStream(serviceFileoutput);
			}
			if (adminBufferedOutputStream==null) {
				adminLog = new File(logPath+adminName+extension);
		   		adminFileoutput = new FileOutputStream(adminLog,true);
		   		adminBufferedOutputStream = new BufferedOutputStream(adminFileoutput);
			}
	} catch(Exception e){
			System.err.println("Cannot initialize Log: " + e.toString());
	}// try&catch
}
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
/**On stop, close opened streams*/
protected void finalize() {
	if (accessBufferedOutputStream!=null)  {
		closeStream(accessBufferedOutputStream);
		accessBufferedOutputStream=null;
	}
	if (errorBufferedOutputStream!=null) {
		closeStream(errorBufferedOutputStream);
		errorBufferedOutputStream=null;
	}
	if (debugBufferedOutputStream!=null)  {
		closeStream(debugBufferedOutputStream);
		debugBufferedOutputStream=null;
	}
	if (serviceBufferedOutputStream!=null)  {
		closeStream(serviceBufferedOutputStream);
		serviceBufferedOutputStream=null;
	}
	if (adminBufferedOutputStream!=null)  {
		closeStream(adminBufferedOutputStream);
		adminBufferedOutputStream=null;
	}
}
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
private void closeStream(BufferedOutputStream stream){
	try {
		stream.flush();
		stream.close();
	} catch(Exception e){
		Log.error(Level.SEVERE, "Log error ==> Unable to close stream: ", e);
	}
}
//-----------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
* Log Rotation method (compress current log file).
* This method is called when internal log counter reaches defined entries threshold
* TODO: Add a scheduled rotation (every hours, ...)
* Compressed file is stored using [log file name] + [absolute time in milliseconds].
* This name format allows to easily find the oldest file to suppress if needed
* @param filename The log filename (prefix) to rotate
*/
public final synchronized void rotateLog(String filename) {
	File fichierLog = new File(logPath+filename+extension);
	if (!fichierLog.exists() || fichierLog.length()==0) {
		return;
	}

	//Check if zipped files are exceeding max threshold. If yes delete oldest one.
	if (maxfiles>0){
		try{
		    //get zipped files list
	        File profilesDirectory = new File(logPath);
	        Filter logfilter = retrieveFilter(profilesDirectory,filename,".zip");
	        //filter contains zip files number and oldest one
	        if (logfilter.oldestfile!=null && logfilter.size>=maxfiles){
	            //System.out.println("Rotation: Too much files. Deleting "+logfilter.oldestfile);
	            new File(logPath+logfilter.oldestfile).delete();
	        }
	    }catch (Exception e){
	    }
	}

	try {
		String nameToZip = filename+System.currentTimeMillis();
		File rotatefile = new File(nameToZip+extension);
		if (fichierLog.renameTo(new File(nameToZip+extension))) {
			fichierLog.delete();
			new Zipper(rotatefile, nameToZip);
		} 
	}catch (Exception e) {
		Log.error(Level.SEVERE, "Error while rotating log: " + filename, e);
	}
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Retrieve file list from given directory starting with given prefix and ending with given suffix 
 * @param directory Directory to search files in
 * @param prefix Prefix to filter
 * @param suffix Suffix to filter
 * @return Files matching the criteria
 */
public File[] filter(File directory, String prefix, String suffix){
    Filter filter = new Filter(prefix, suffix);
    return directory.listFiles(filter);
}
/**
 * Create a file filter from given directory starting with given prefix and ending with given suffix 
 * @param directory Directory to search files in
 * @param prefix Prefix to filter
 * @param suffix Suffix to filter
 * @return Associated Filter based on given criteria
 */
public Filter retrieveFilter(File directory, String prefix, String suffix){
    Filter filter = new Filter(prefix, suffix);
    directory.listFiles(filter);
    return filter;
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Internal class used to retrieve oldest compressed log file
 */
private class Filter implements FilenameFilter{
    String prefix, suffix, oldestfile;
    int size = 0;
    /**
     * Filter files starting with given prefix and ending with given suffix
     * @param _prefix Log filename (service, server, debug, access)
     * @param _suffix (.log, .zip, ...)
     */
    public Filter(String _prefix, String _suffix){
        this.prefix = _prefix;
        this.suffix = _suffix;
    }
    /**@return the oldest file matching this filter*/
    //public String getOldestfile(){return oldestfile;}
    /** @return number of files matching this filter*/
    //public int getSize(){return size;}
    
    /**
     * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
     */
    public boolean accept(File dir,String name){
        if (name.startsWith(prefix) && name.endsWith(suffix)){
            if (oldestfile==null) oldestfile = name;
            else oldestfile = (oldestfile.compareTo(name)<0)?oldestfile:name;
            size++;
            return true;
        }
        return false;
    }
}
//--------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
* Internal Thread used to compress log files
* Compression level is set to maximum (=9).
*/
private class Zipper extends Thread  {
	File fileToZip;
	String nameToZip;
	static final int BUFFER = 2048;
	byte data[] = new byte[BUFFER];
	/**
	 * Create a thread that will compress given file  
	 * @param _fileToZip file to compress
	 * @param _nameToZip name of the compressed file
	 */
	public Zipper(File _fileToZip, String _nameToZip) {
		super("Zipper");
		this.setPriority(Thread.MIN_PRIORITY);
		fileToZip = _fileToZip;
		nameToZip = _nameToZip;
		start();
	}

	/**
	 * @see java.lang.Thread#run()
	 */
	public final void run() {
		try  {
		   BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToZip), BUFFER);
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(logPath+nameToZip+".zip",true)));
			zos.setLevel(6);
			zos.putNextEntry(new ZipEntry(nameToZip+extension));
			int readedbytes;
			while (( readedbytes = bis.read(data, 0,BUFFER)) != -1 ){
				zos.write(data, 0, readedbytes);
			}//End while readLine
			bis.close();
			zos.close();
			fileToZip.delete();
		}catch (Exception e) {
			error(Level.SEVERE, "Error while zipping the log : ",e);
		}
	}//End run
}//End Thread Zipper
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Store a message in ACCESS log.<br>
 * All accesses should be logged (so no Level is asked) 
 * @param message Message content
 */
public final static synchronized void access(String message) {
	if (!accessEnabled || loglevel == Level.OFF ) return;
	logger.store(ACCESS, null, new StringBuilder(message));
}
/**
 * Store a message in ACCESS log.<br>
 * All accesses should be logued (so no Level is asked)
 * @param message Message content
 */
public final static synchronized void access(StringBuilder message) {
	if (!accessEnabled || loglevel == Level.OFF ) return;
    logger.store(ACCESS, null, message);
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Store a message in SERVICE log
 * @param level Message Level
 * @param message Message content
 */
public final static synchronized void service(Level level, String message) {
	if (!serviceEnabled || level.intValue()<loglevel.intValue()) return;
	logger.store(SERVICE, level, new StringBuilder(message));
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Store a message in SERVER log
 * @param level Message Level
 * @param message Message content
 */
public final static synchronized void error(Level level, String message) {
	if (!errorEnabled || level.intValue()<loglevel.intValue()) return;
	logger.store(ERROR, level, new StringBuilder(message));
}
//--------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
 * Store a message in SERVER log
 * @param level Message Level
 * @param message Message content
 * @param exception Associated exception
 */
public final static synchronized void error(Level level,String message, Exception exception) {
	if (!errorEnabled || level.intValue()<loglevel.intValue()) return;
	logger.store(ERROR, level,new StringBuilder(message).append("\t").append(exception.getMessage()));
}
/**
 * Store a message in SERVER log
 * @param level Message Level
 * @param message Message content
 * @param throwable Associated exception
 */
public final static synchronized void error(Level level,String message, Throwable throwable) {
	if (!errorEnabled || level.intValue()<loglevel.intValue()) return;
	logger.store(ERROR, level,new StringBuilder(message).append("\t").append(throwable.toString()));
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Store a message in SERVER log
 * @param level Message Level
 * @param message Message content
 */
public final static synchronized void admin(Level level, String message) {
	if (!adminEnabled || level.intValue()<loglevel.intValue()) return;
	logger.store(ADMIN, level, new StringBuilder(message));
}
//--------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
 * Store a message in SERVER log
 * @param level Message Level
 * @param message Message content
 * @param exception Associated exception
 */
public final static synchronized void admin(Level level,String message, Exception exception) {
	if (!adminEnabled || level.intValue()<loglevel.intValue()) return;
	logger.store(ADMIN, level,new StringBuilder(message).append("\t").append(exception.getMessage()));
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Store a message in debug log
 * @param level	the message level
 * @param message Message content	
 */
public final static synchronized void trace(Level level, String message) {
	if (!debugEnabled  || level.intValue()<loglevel.intValue()) return;
	logger.store(TRACE,level, new StringBuilder(message));
}
//--------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
 * Store a message in debug log with given level
 * Exceptions are logged if level is >= FINE
 * @param level Message level
 * @param exception Exception that will be used as message content
 */
public final static synchronized void trace(Level level, Exception exception) {
	if (!debugEnabled) return;
	if (level.intValue()<loglevel.intValue()) return;
	if (loglevel.intValue()>Level.INFO.intValue()) {
		logger.store(TRACE, level, new StringBuilder(exception.getMessage()));
	} else {
		StackTraceElement[]  trace = exception.getStackTrace();
		for (int i=0; i<trace.length;i++) {
			logger.store(TRACE, level, new StringBuilder(trace[i].toString()));
		}
	}//Endif
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Direct message log method.<br> 
 * Messages are filtered depending of their level compared to the current setted Log level.<br>
 * If log level is >= FINE, Exception stack trace is added to the log file.
 * @param file The log file in which to store message
 * @param level	the Message level
 * @param exception Exception to store	
 */
public final static synchronized void log(int file, Level level, Exception exception) {
    if (!enabled) return;
    if (level.intValue()<loglevel.intValue()) return;
    if (loglevel.intValue()>Level.INFO.intValue()) {
        logger.store(file,level, new StringBuilder(exception.getMessage()));
    } else {
        StackTraceElement[]  trace = exception.getStackTrace();
        for (int i=0; i<trace.length;i++) {
            logger.store(file,level, new StringBuilder(trace[i].toString()));
        }
    }//Endif
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Direct message log method. Messages are filtered depending of their level compared to the current setted Log level.<br>
 * @param file The log file in which to store message
 * @param level	the Message level
 * @param message Message content	
 */
public final static synchronized void log(int file, Level level, String message) {
    if (!enabled) return;
    if (level.intValue()<loglevel.intValue()) return;
    logger.store(file,level, new StringBuilder(message));
}
//--------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
 * Store message in DEBUG log file.<br>
 * Messages are filtered depending of their level compared to the current setted Log level.
 * If log level is >= FINE, Exception stack trace is added to the log file.
 * @param level  Message level
 * @param message Message content	
 * @param exception	Associated exception (if any)
 */
public final static synchronized void trace(Level level, String message, Exception exception) {
	if (!debugEnabled) return;
	if (level.intValue()<loglevel.intValue()) return;
	if (loglevel.intValue()>Level.FINE.intValue()) {
		logger.store(TRACE,level, new StringBuilder(message).append("\t").append(exception.getMessage()));
	} else {
		logger.store(TRACE, level,new StringBuilder(message).append("\t").append(exception.getMessage()));
		StackTraceElement[]  trace = exception.getStackTrace();
		for (int i=0; i<trace.length;i++) {
			logger.store(TRACE,level, new StringBuilder(trace[i].toString()));
		}
	}//Endif
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**
 * Static method calling class Logger in order to store information (call to store in background).<br>
 * Messages are filtered depending of their level compared to the current setted Log level.
 * If log level is >= FINE, Exception stack trace is added to the log file.
 * @param logtype Log file in which to store info
 * @param level   Message level		
 * @param message Message content	
 * @param exception	Associated exception (if any)
 */
public final static synchronized void println(int logtype, Level level, String message,Exception exception) {
	if (!enabled) return;
	if (level.intValue()<loglevel.intValue()) return;
	if (loglevel.intValue()>Level.FINE.intValue()) {
        if(exception!=null) logger.store(logtype, level,new StringBuilder(message).append("\t").append(exception.getMessage()));
        else logger.store(logtype,level, new StringBuilder(message));
	} else {
        if(exception!=null) {
            logger.store(logtype,level, new StringBuilder(message).append("\t").append(exception.getMessage()));
    		StackTraceElement[]  trace = exception.getStackTrace();
    		for (int i=0; i<trace.length;i++) {
    			logger.store(logtype,level, new StringBuilder(trace[i].toString()));
    		}
        }
        else logger.store(logtype, level,new StringBuilder(message));
	}//Endif
}
//--------------------------------------------------------------------------------------

/**
 * Force logs rotation even if capacity threshold has not been reached
 */
public final static void forceRotation(){
    logger.store(MAINTENANCE, Level.INFO,new StringBuilder(""));
}

//--------------------------------------------------------------------------------------
/**Formatter for log dates (use [dd/MMM/yyyy:HH:mm:ss Z], ex:[17/Aug/2005:10:22:42 +0200])*/
static SimpleDateFormat formatter = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]",Locale.US);
static{formatter.setTimeZone(TimeZone.getDefault());}
static Date recdate = new Date();

/**
 * Store received messages into file log.
 * If log file entries exceed a specified threshold, (default:10000),
 * create a zipper thread that rotate and compress current log.
 * Note: information received is automatically prepended with date (universal time)
 */
 private final synchronized void store(int logtype, Level level, StringBuilder message) {
	if (!enabled) return;
	try {
        //fast retrieve of current time
        long absoluteTime = System.currentTimeMillis();
        //update static date object
        recdate.setTime(absoluteTime);
        //Generate log line
        if (level!=null) {
        	message.insert(0,String.format("%1$s \t[%2$-10s",formatter.format(recdate), level.getName()+']'));
        	//message.insert(0, String.format("   %1$-7s", level.getName()));
        } else {
        	message.insert(0,String.format("%1$s \t",formatter.format(recdate)));
        	//message.insert(0," - ");
        }
        //message.insert(0,String.format("%1$s   %2$s   %3$-7s",formatter.format(recdate), absoluteTime,level.getName()));
        //message.insert(0,"   ").insert(0, absoluteTime).insert(0, "   ").insert(0, formatter.format(recdate));
         message.append(returnchar);
		//message = formatter.format(recdate)+" \t" +absoluteTime+" \t" + message+returnchar;
        
        //Store log line in requested file
		switch (logtype){
			case ACCESS:
					accessEntriesCounter++;
					accessBufferedOutputStream.write(message.toString().getBytes());
					if (!isBuffered) accessBufferedOutputStream.flush();
					if (accessEntriesCounter>=maxentries) {
					    	closeStream(accessBufferedOutputStream);
							accessFileoutput.close();
							rotateLog(accessName);
							accessFileoutput = new FileOutputStream(accessLog,true);
							accessBufferedOutputStream = new BufferedOutputStream(accessFileoutput);
							accessEntriesCounter=0; 
					}
					break;
			case SERVICE:
					serviceEntriesCounter++;
					serviceBufferedOutputStream.write(message.toString().getBytes());
					if (!isBuffered) serviceBufferedOutputStream.flush();
					if (serviceEntriesCounter>=maxentries) {
				    		closeStream(serviceBufferedOutputStream);
							serviceFileoutput.close();
							rotateLog(serviceName);
							serviceFileoutput = new FileOutputStream(serviceLog,true);
							serviceBufferedOutputStream = new BufferedOutputStream(serviceFileoutput);
							serviceEntriesCounter=0; 
					}
					break;
			case ERROR:
				errorEntriesCounter++;
				errorBufferedOutputStream.write(message.toString().getBytes());
				if (!isBuffered) errorBufferedOutputStream.flush();
				if (errorEntriesCounter>=maxentries) {
		    			closeStream(errorBufferedOutputStream);					    
						errorFileoutput.close();
						rotateLog(errorName);
						errorFileoutput = new FileOutputStream(errorLog,true);
						errorBufferedOutputStream = new BufferedOutputStream(errorFileoutput);
						errorEntriesCounter=0; 
				}
				break;					
			case ADMIN:
					adminEntriesCounter++;
					adminBufferedOutputStream.write(message.toString().getBytes());
					if (!isBuffered) adminBufferedOutputStream.flush();
					if (adminEntriesCounter>=maxentries) {
			    			closeStream(adminBufferedOutputStream);					    
			    			adminFileoutput.close();
							rotateLog(adminName);
							adminFileoutput = new FileOutputStream(adminLog,true);
							adminBufferedOutputStream = new BufferedOutputStream(adminFileoutput);
							adminEntriesCounter=0; 
					}
					break;
			case TRACE:
					debugEntriesCounter++;
					debugBufferedOutputStream.write(message.toString().getBytes());
					if (!isBuffered) debugBufferedOutputStream.flush();
					if (debugEntriesCounter>=maxentries) {
					    	closeStream(debugBufferedOutputStream);
							debugFileoutput.close();
							rotateLog(debugName);
							debugFileoutput = new FileOutputStream(debugLog,true);
							debugBufferedOutputStream = new BufferedOutputStream(debugFileoutput);
							debugEntriesCounter = 0;
					}
					break;
                    
            case MAINTENANCE:
	                closeStream(adminBufferedOutputStream);
	                adminFileoutput.close();
	                rotateLog(adminName);
	                adminFileoutput = new FileOutputStream(adminLog,true);
	                adminBufferedOutputStream = new BufferedOutputStream(adminFileoutput);
	                adminEntriesCounter = 0;
                    closeStream(debugBufferedOutputStream);
                    debugFileoutput.close();
                    rotateLog(debugName);
                    debugFileoutput = new FileOutputStream(debugLog,true);
                    debugBufferedOutputStream = new BufferedOutputStream(debugFileoutput);
                    debugEntriesCounter = 0;
                    closeStream(errorBufferedOutputStream);                     
                    errorFileoutput.close();
                    rotateLog(errorName);
                    errorFileoutput = new FileOutputStream(errorLog,true);
                    errorBufferedOutputStream = new BufferedOutputStream(errorFileoutput);
                    errorEntriesCounter=0; 
                    closeStream(serviceBufferedOutputStream);
                    serviceFileoutput.close();
                    rotateLog(serviceName);
                    serviceFileoutput = new FileOutputStream(serviceLog,true);
                    serviceBufferedOutputStream = new BufferedOutputStream(serviceFileoutput);
                    serviceEntriesCounter=0; 
                    closeStream(accessBufferedOutputStream);
                    accessFileoutput.close();
                    rotateLog(accessName);
                    accessFileoutput = new FileOutputStream(accessLog,true);
                    accessBufferedOutputStream = new BufferedOutputStream(accessFileoutput);
                    accessEntriesCounter=0; 
                break;
			}//End switch
	} catch(Exception e){
		System.err.println("[GREASYSPOON FATAL ERROR] Loging facility - Log file cannot be created, is locked by ADMIN/ROOT or disk space full");
	}// catch
}//End store
//--------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------
/**
 * @return current log level
 */
public static Level getLogLevel(){
	return Log.loglevel;
}

/**
 * Set log debug level
 * Messages sent to Log with a greater debug level than this level 
 * are silently discarded
 * @param _newLogLevel The debug level to support
 */
public static void setLogLevel(Level _newLogLevel){
	if (_newLogLevel==loglevel && enabled) return;
	try{
		if (_newLogLevel!=Level.OFF && loglevel==Level.OFF) {
				debugLog = new File(logPath+debugName+extension);
			   	debugFileoutput = new FileOutputStream(logPath+debugName+extension,true);
				debugBufferedOutputStream = new BufferedOutputStream(debugFileoutput);
                if (Log.config())error(Level.CONFIG, "Debug Mode Enable - Level:" + _newLogLevel);
		} else if (_newLogLevel == Level.OFF){
				debugBufferedOutputStream.flush();
				debugBufferedOutputStream.close();
				debugFileoutput.close();
                if (Log.config())error(Level.CONFIG,"Debug Mode disabled.");
                
		}//Endif
	} catch (Exception e){
	}
	if ( (Log.loglevel == Level.OFF || !enabled) &&  _newLogLevel!= Level.OFF ){
		Log.enable();
		Log.error(Log.CONFIG,"Logs activated.");
	} else if (Log.loglevel != Level.OFF &&  _newLogLevel == Level.OFF){
		Log.error(Log.CONFIG,"Logs disabled.");
		Log.disable();
	} 
	Log.loglevel = _newLogLevel;
}
/**
 * Update log level to given value
 * @param levelName log level provided as upper case string 
 */
public static void setLogLevel(String levelName){
    try{
        setLogLevel(Level.parse(levelName));
    }catch (Exception e){
        if (Log.config())Log.error(Log.CONFIG, "Invalid log level: "+levelName);
    }
}
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
/**@return true if log level is set to FINEST or higher level */
public final static boolean finest(){if (loglevel.intValue()<Level.FINER.intValue()) return true;return false;}
/**@return true if log level is set to FINER or higher level */
public final static boolean finer(){if (loglevel.intValue()<Level.FINE.intValue()) return true;return false;}
/**@return true if log level is set to FINE or higher level */
public final static boolean fine(){if (loglevel.intValue()<Level.CONFIG.intValue()) return true;return false;}
/**@return true if log level is set to CONFIG or higher level */
public final static boolean config(){if (loglevel.intValue()<Level.INFO.intValue()) return true;return false;}
/**@return true if log level is set to INFO or higher level */
public final static boolean info(){if (loglevel.intValue()<Level.WARNING.intValue()) return true;return false;}
/**@return true if log level is set to WARNING or higher level */
public final static boolean warning(){if (loglevel.intValue()<Level.SEVERE.intValue()) return true;return false;}
/**@return true if log level is set to SEVERE or higher level */
public final static boolean severe(){if (loglevel.intValue()<Level.OFF.intValue()) return true;return false;}
//--------------------------------------------------------------------------------------

/**
 * @return The maximum entries in log file before rotation
 */
public static int getMaxentries() {
	return maxentries;
}

/**
 * @param maxentries Set the maximum entries in log file before rotation
 */
public static void setMaxentries(int maxentries) {
	Log.maxentries = maxentries;
}

/**
 * @return the maximum log files before deletion
 */
public static int getMaxfiles() {
	return maxfiles;
}

/**
 * @param maxfiles set the maximum log files before deletion
 */
public static void setMaxfiles(int maxfiles) {
	Log.maxfiles = maxfiles;
}

/**
 * @return Returns the debugEnabled.
 */
public static boolean isDebugEnabled() {
	return debugEnabled;
}

/**
 * @param debugEnabled The debugEnabled to set.
 */
public static void setDebugEnabled(boolean debugEnabled) {
	Log.debugEnabled = debugEnabled;
}

/**
 * @return Returns the errorEnabled.
 */
public static boolean isErrorEnabled() {
	return errorEnabled;
}

/**
 * @param errorEnabled The errorEnabled to set.
 */
public static void setErrorEnabled(boolean errorEnabled) {
	Log.errorEnabled = errorEnabled;
}

/**
 * @return Returns the adminEnabled.
 */
public static boolean isAdminEnabled() {
	return adminEnabled;
}

/**
 * @param adminEnabled The adminEnabled to set.
 */
public static void setAdminEnabled(boolean adminEnabled) {
	Log.adminEnabled = adminEnabled;
}

/**
 * @return Returns the accessEnabled.
 */
public static boolean isAccessEnabled() {
	return accessEnabled;
}

/**
 * @param accessEnabled The accessEnabled to set.
 */
public static void setAccessEnabled(boolean accessEnabled) {
	Log.accessEnabled = accessEnabled;
}

/**
 * @return Returns the serviceEnabled.
 */
public static boolean isServiceEnabled() {
	return serviceEnabled;
}

/**
 * @param serviceEnabled The serviceEnabled to set.
 */
public static void setServiceEnabled(boolean serviceEnabled) {
	Log.serviceEnabled = serviceEnabled;
}

}