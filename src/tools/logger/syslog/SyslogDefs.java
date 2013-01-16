
package tools.logger.syslog;

import java.text.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Definition of Syslog variables and levels
 *
 */
public class SyslogDefs {
	//
	// Syslog Levels
	//
	/** system is unusable */public static final int LOG_EMERG	= 0; 
	/** action must be taken immediately */public static final int LOG_ALERT	= 1; 
	/** critical conditions */public static final int LOG_CRIT	= 2; 
	/** error conditions */public static final int LOG_ERR		= 3; 
	/** warning conditions */public static final int LOG_WARNING	= 4; 
	/** normal but significant condition */public static final int LOG_NOTICE	= 5; 
	/** informational */public static final int LOG_INFO	= 6; 
	/** debug-level messages */public static final int LOG_DEBUG	= 7; 
	/** '*' in config, all levels */public static final int LOG_ALL		= 8; 

	//
	// Syslog Facilities
	/** kernel messages */
	public static final int LOG_KERN	= 0; 
	/** random user-level messages */
	public static final int LOG_USER	= 1; 
	/** mail system */
	public static final int LOG_MAIL	= 2; 	
	/** system daemons */
	public static final int LOG_DAEMON	= 3; 
	/** security/authorization messages */
	public static final int LOG_AUTH	= 4; 
	/** messages generated internally by syslogd */
	public static final int LOG_SYSLOG	= 5; 
	/** line printer subsystem */
	public static final int LOG_LPR		= 6; 
	/** network news subsystem */
	public static final int LOG_NEWS	= 7; 
	/** UUCP subsystem */
	public static final int LOG_UUCP	= 8; 
	/** clock daemon */
	public static final int LOG_CRON	= 9; 


	/* other codes through 15 reserved for system use */
	/** reserved for local use */	public static final int LOG_LOCAL0	= 16; 
	/** reserved for local use */	public static final int LOG_LOCAL1	= 17; 
	/** reserved for local use */	public static final int LOG_LOCAL2	= 18; 
	/** reserved for local use */	public static final int LOG_LOCAL3	= 19; 
	/** reserved for local use */	public static final int LOG_LOCAL4	= 20; 
	/** reserved for local use */	public static final int LOG_LOCAL5	= 21; 
	/** reserved for local use */	public static final int LOG_LOCAL6	= 22; 
	/** reserved for local use */	public static final int LOG_LOCAL7	= 23; 

	/** current number of facilities */	public static final int LOG_NFACILITIES	= 24;	

	
	// SYSLOG Flags (options)
	/** mask to extract priority part (internal) */	 public static final int LOG_PRIMASK	= 0x07;
	/** mask to extract facility part */	public static final int LOG_FACMASK	= 0x03F8;
	
	/** the "no priority" priority */	public static final int INTERNAL_NOPRI = 0x10;	
	/** log the pid with each message */	public static final int LOG_PID		= 0x01;	
	/** log on the console if errors in sending */	public static final int LOG_CONS	= 0x02;	
	/** delay open until first syslog() (default) */	public static final int LOG_ODELAY	= 0x04;	
	/** don't delay open */	public static final int LOG_NDELAY	= 0x08;	
	/** don't wait for console forks: DEPRECATED */	public static final int LOG_NOWAIT	= 0x10;	
	/** log to stderr as well */	public static final int LOG_PERROR	= 0x20;	
	/** log the open */	public static final int LOG_START	= 0x20;	
	
	/**Default syslog port*/
	public static final int	DEFAULT_PORT = 514;


	static private ConcurrentHashMap<String,Integer> facHash;
	static private ConcurrentHashMap<String,Integer> priHash;


//	<------------------------------------------------------------------------->
	static
	{
		facHash = new ConcurrentHashMap<String,Integer>(20);

		facHash.put( "KERN",		new Integer(SyslogDefs.LOG_KERN) );
		facHash.put( "KERNEL",		new Integer(SyslogDefs.LOG_KERN) );
		facHash.put( "USER",		new Integer(SyslogDefs.LOG_USER) );
		facHash.put( "MAIL",		new Integer(SyslogDefs.LOG_MAIL) );
		facHash.put( "DAEMON",		new Integer(SyslogDefs.LOG_DAEMON) );
		facHash.put( "AUTH",		new Integer(SyslogDefs.LOG_AUTH) );
		facHash.put( "SYSLOG",		new Integer(SyslogDefs.LOG_SYSLOG) );
		facHash.put( "LPR",			new Integer(SyslogDefs.LOG_LPR) );
		facHash.put( "NEWS",		new Integer(SyslogDefs.LOG_NEWS) );
		facHash.put( "UUCP",		new Integer(SyslogDefs.LOG_UUCP) );
		facHash.put( "CRON",		new Integer(SyslogDefs.LOG_CRON) );
		facHash.put( "LOCAL0",		new Integer(SyslogDefs.LOG_LOCAL0) );
		facHash.put( "LOCAL1",		new Integer(SyslogDefs.LOG_LOCAL1) );
		facHash.put( "LOCAL2",		new Integer(SyslogDefs.LOG_LOCAL2) );
		facHash.put( "LOCAL3",		new Integer(SyslogDefs.LOG_LOCAL3) );
		facHash.put( "LOCAL4",		new Integer(SyslogDefs.LOG_LOCAL4) );
		facHash.put( "LOCAL5",		new Integer(SyslogDefs.LOG_LOCAL5) );
		facHash.put( "LOCAL6",		new Integer(SyslogDefs.LOG_LOCAL6) );
		facHash.put( "LOCAL7",		new Integer(SyslogDefs.LOG_LOCAL7) );

		priHash = new ConcurrentHashMap<String,Integer>(20);

		priHash.put( "EMERG",			new Integer(SyslogDefs.LOG_EMERG) );
		priHash.put( "EMERGENCY",		new Integer(SyslogDefs.LOG_EMERG) );
		priHash.put( "LOG_EMERG",		new Integer(SyslogDefs.LOG_EMERG) );
		priHash.put( "ALERT",			new Integer(SyslogDefs.LOG_ALERT) );
		priHash.put( "LOG_ALERT",		new Integer(SyslogDefs.LOG_ALERT) );
		priHash.put( "CRIT",			new Integer(SyslogDefs.LOG_CRIT) );
		priHash.put( "CRITICAL",		new Integer(SyslogDefs.LOG_CRIT) );
		priHash.put( "LOG_CRIT",		new Integer(SyslogDefs.LOG_CRIT) );
		priHash.put( "ERR",				new Integer(SyslogDefs.LOG_ERR) );
		priHash.put( "ERROR",			new Integer(SyslogDefs.LOG_ERR) );
		priHash.put( "LOG_ERR",			new Integer(SyslogDefs.LOG_ERR) );
		priHash.put( "WARNING",			new Integer(SyslogDefs.LOG_WARNING) );
		priHash.put( "LOG_WARNING",		new Integer(SyslogDefs.LOG_WARNING) );
		priHash.put( "NOTICE",			new Integer(SyslogDefs.LOG_NOTICE) );
		priHash.put( "LOG_NOTICE",		new Integer(SyslogDefs.LOG_NOTICE) );
		priHash.put( "INFO",			new Integer(SyslogDefs.LOG_INFO) );
		priHash.put( "LOG_INFO",		new Integer(SyslogDefs.LOG_INFO) );
		priHash.put( "DEBUG",			new Integer(SyslogDefs.LOG_DEBUG) );
		priHash.put( "LOG_DEBUG",		new Integer(SyslogDefs.LOG_DEBUG) );
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * @param code
	 * @return Facility encoded in given value
	 */
	static public int extractFacility( int code ) {
		return ( (code & SyslogDefs.LOG_FACMASK) >> 3 );
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * @param code
	 * @return Priority encoded in given value 
	 */
	static public int extractPriority( int code ) {
		return ( code & SyslogDefs.LOG_PRIMASK );
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * @param facility
	 * @param priority
	 * @return Code corresponding to given facility with given priority
	 */
	static public int computeCode( int facility, int priority )	{
		return ( (facility << 3) | priority );
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * @param level
	 * @return level name associated to given value
	 */
	static public String getPriorityName( int level ){
		switch ( level ) {
		case SyslogDefs.LOG_EMERG:		return "panic";
		case SyslogDefs.LOG_ALERT:		return "alert";
		case SyslogDefs.LOG_CRIT:		return "critical";
		case SyslogDefs.LOG_ERR:		return "error";
		case SyslogDefs.LOG_WARNING:	return "warning";
		case SyslogDefs.LOG_NOTICE:		return "notice";
		case SyslogDefs.LOG_INFO:		return "info";
		case SyslogDefs.LOG_DEBUG:		return "debug";
		}
		return "unknown level='" + level + "'";
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * @param facility
	 * @return facility name associated to given value
	 */
	static public String getFacilityName( int facility ) {
		switch ( facility ) {
			case SyslogDefs.LOG_KERN:		return "kernel";
			case SyslogDefs.LOG_USER:		return "user";
			case SyslogDefs.LOG_MAIL:		return "mail";
			case SyslogDefs.LOG_DAEMON:		return "daemon";
			case SyslogDefs.LOG_AUTH:		return "auth";
			case SyslogDefs.LOG_SYSLOG:		return "syslog";
			case SyslogDefs.LOG_LPR:		return "lpr";
			case SyslogDefs.LOG_NEWS:		return "news";
			case SyslogDefs.LOG_UUCP:		return "uucp";
			case SyslogDefs.LOG_CRON:		return "cron";
	
			case SyslogDefs.LOG_LOCAL0:		return "local0";
			case SyslogDefs.LOG_LOCAL1:		return "local1";
			case SyslogDefs.LOG_LOCAL2:		return "local2";
			case SyslogDefs.LOG_LOCAL3:		return "local3";
			case SyslogDefs.LOG_LOCAL4:		return "local4";
			case SyslogDefs.LOG_LOCAL5:		return "local5";
			case SyslogDefs.LOG_LOCAL6:		return "local6";
			case SyslogDefs.LOG_LOCAL7:		return "local7";
		}
		return "unknown facility='" + facility + "'";
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * @param priority
	 * @return priority corresponding to given name
	 * @throws ParseException
	 */
	static public int getPriority( String priority ) throws ParseException {
		Integer result = SyslogDefs.priHash.get(priority.toUpperCase());

		if ( result == null ){
			throw new ParseException ( "unknown priority '" + priority + "'", 0 );
		}

		return result.intValue();
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * @param facility
	 * @return facility corresponding to given name
	 * @throws ParseException
	 */
	static public int getFacility( String facility ) {
		Integer result = SyslogDefs.facHash.get(facility.toUpperCase());
		if ( result == null ) {
			return  LOG_USER;
		}
		return result.intValue();
	}
//	<------------------------------------------------------------------------->

}
