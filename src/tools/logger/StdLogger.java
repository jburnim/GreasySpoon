package tools.logger;

import java.io.*;
import java.util.logging.*;
import java.util.Date;
/**
 * Class logging 
 * @author Karel
 */
public class StdLogger {

	static PrintStream stdout = System.out;
	static PrintStream stderr = System.err;
	static int MAXFILES = 3;
	static int MAXFILESIZE = 10000; //(size in bytes)
	
//	<--------------------------------------------------------------------------->
    /**
     * Redirect stdOut and stdErr to given file
     * @param logname Name of the log file to create
     * @param path Path where to store std logs
     */
    public static void redirectStdFlows(String logname, String path) {
    	try{
		    // initialize logging to go to rolling log file
		    LogManager logManager = LogManager.getLogManager();
		    logManager.reset();
		
		    // log file max size 10K, 3 rolling files, append-on-open
		    Handler fileHandler = new FileHandler(path+logname+".%g.log", MAXFILESIZE, MAXFILES, true);
		    //fileHandler.setFormatter(new SimpleFormatter());
		    fileHandler.setFormatter(new CustomFormatter());
		    Logger.getLogger("").addHandler(fileHandler);        
			
		    // now bind stdout/stderr to logger
			Logger logger;
			LoggingOutputStream los;
			
			logger = Logger.getLogger("stdout");
			los = new LoggingOutputStream(logger, Level.INFO);
			System.setOut(new PrintStream(los, true));
			
			logger = Logger.getLogger("stderr");
			los= new LoggingOutputStream(logger, Level.SEVERE);
			System.setErr(new PrintStream(los, true));
    	}catch (Exception e){
    		e.printStackTrace();
    	}
    }
//	<--------------------------------------------------------------------------->
    
    
//	<--------------------------------------------------------------------------->
    /**
     * Restore stdOut and stdErr to standard streams
     */
    public static void restore(){
    	System.setOut(stdout);
		System.setErr(stderr);
    }
//	<--------------------------------------------------------------------------->
    
    /**
     * An OutputStream that writes contents to a Logger upon each call to flush()
     */
    static class LoggingOutputStream extends ByteArrayOutputStream {
        
        private String lineSeparator;
        
        private Logger logger;
        private Level level;
        
        /**
         * Constructor
         * @param logger Logger to write to
         * @param level Level at which to write the log message
         */
        public LoggingOutputStream(Logger logger, Level level) {
            super();
            this.logger = logger;
            this.level = level;
            lineSeparator = System.getProperty("line.separator");
        }
        
        /**
         * upon flush() write the existing contents of the OutputStream to the logger as 
         * a log record.
         * @throws java.io.IOException in case of error
         */
        public void flush() throws IOException {

            String record;
            synchronized(this) {
                super.flush();
                record = this.toString();
                super.reset();
            }
            
            if (record.length() == 0 || record.equals(lineSeparator)) {
                // avoid empty records
                return;
            }

            logger.logp(level, "", "", record);
        }
    }
//	<--------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------->
    /**
     * Custom formatter formats to log records to a single line
     * @author Karel
     */
    static class CustomFormatter extends Formatter {
        // This method is called for every log records
        public String format(LogRecord rec) {
            StringBuilder buf = new StringBuilder(1000);
            buf.append('[').append(new Date(rec.getMillis())).append(']');
            buf.append(' ');
            buf.append('[').append(rec.getLevel()).append(']');
            buf.append(' ');
            buf.append(formatMessage(rec));
            buf.append('\n');
            return buf.toString();
        }
    
        // This method is called just after the handler using this
        // formatter is created
        public String getHead(Handler h) {
            return "";
        }
    
        // This method is called just after the handler using this
        // formatter is closed
        public String getTail(Handler h) {
            return "";
        }
    }
//	<--------------------------------------------------------------------------->
    
}
