
package tools.logger.syslog;

import java.lang.Exception;

/**
 * Syslog Exception 
 **/
public class SyslogException extends Exception {
	
	private static final long serialVersionUID = 1L;

	SyslogException() {
		super();
	}
	
	SyslogException( String msg ) {
		super( msg );
	}
}

