package tools.logger.syslog;

import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * The Syslog class implements the UNIX syslog protocol allowing Java
 * to log messages to a specified UNIX host. Care has been taken to
 * preserve as much of the UNIX implementation as possible.
 * <br>
 * To use Syslog, simply create an instance, and use the Syslog() method
 * to log your message. The class provides all the expected syslog constants.
 * For example, LOG_ERR is Syslog.LOG_ERR.
 * <br>
 * @see DatagramSocket
 * @see	InetAddress
 */

public class SyslogConnection extends Object {
	// INSTANCE VARIABLES

	/**Connection ID*/
	private String processName;
	private int portNum;
	private int flags;


	/**
	 * This determines if the timestamp is added to the message in this, the client, or if it is left off for the server
	 * to fill in. Adding it in the client is the "standard" way, but most servers support the "old style" (no timestamp)
	 * messages as well. I leave this choice, since some servers may not work without it, and because some JDK's may not
	 * support SimpleDateFormat or TimeZone correctly.
	 * */
	private boolean	includeDate = false;
	private SimpleDateFormat	date1Format;
	private SimpleDateFormat	date2Format;

	/**Default SYSLOG daemon address (if not specified in send command)*/
	private InetAddress    boundAddress;
	
	private DatagramSocket socket;

	//	<------------------------------------------------------------------------->
	/**
	 * Creates a Syslog connection instance, targeted by default on LOCALHOST (127.0.0.1)
	 * The only flag locally used is 'LOG_PERROR', which will log the message to Java's 'System.err'.
	 * @param processName  
	 * @param flags
	 * @throws SyslogException
	 */
	public SyslogConnection( String processName, int flags ) throws SyslogException {
		super();
		try {
			this.processName = processName;
			this.boundAddress = InetAddress.getLocalHost();
		} catch ( Exception ex )	{
			String message = "Error opening SyslogConnection on localhost: " + ex.getMessage();
			throw new SyslogException( message );
		}
		this.portNum = SyslogDefs.DEFAULT_PORT;
		this.flags = flags;
		this.initialize();
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Creates a Syslog object instance, targeted for the UNIX host
	 * with the hostname 'hostname' on the syslog port 'port'.
	 * The only flags recognized are 'LOG_PERROR', which will log the
	 * message to Java's 'System.err'.
	 * @param hostname
	 * @param port
	 * @param processName
	 * @param flags
	 * @throws SyslogException
	 */
	public SyslogConnection( String hostname, int port, String processName, int flags ) throws SyslogException {
		super();
		this.processName = processName;
		this.portNum = port;
		this.flags = flags;
		try {
			this.boundAddress = InetAddress.getByName( hostname );
		} catch ( UnknownHostException ex )	{
			String message = "error locating host named '" + hostname + "': " + ex.getMessage();
			throw new SyslogException( message );
		}
		this.initialize();
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	private void initialize() throws SyslogException {
		try {
			this.socket = new DatagramSocket();
		} catch ( SocketException ex ){
			String message = "error creating syslog udp socket: " + ex.getMessage();
			throw new SyslogException( message );
		}
		if ( this.includeDate ) {
			// We need two separate formatters here, since there is
			// no way to get the single digit date (day of month) to
			// pad with a space instead of a zero.
			this.date1Format = new SimpleDateFormat( "MMM  d HH:mm:ss ", Locale.US );
			this.date2Format = new SimpleDateFormat( "MMM dd HH:mm:ss ", Locale.US );
			this.date1Format.setTimeZone ( TimeZone.getDefault() );
			this.date2Format.setTimeZone ( TimeZone.getDefault() );
		}
	}
	//	<------------------------------------------------------------------------->


	//	<----------------------------------------------------------------------------    
	/**
	 * Binds the Syslog class to a specified host for further logging.
	 * See the Syslog constructor for details on the parameters.
	 * @param hostname Syslog server hostname
	 * @throws SyslogException
	 */
	public void bind( String hostname) throws SyslogException {
		try {
			this.boundAddress = InetAddress.getByName( hostname );
		} catch ( Exception ex )	{
			String message = "error locating host named '" + hostname + "': " + ex.getMessage();
			throw new SyslogException( message );
		}
	}
	//	<------------------------------------------------------------------------->
	
	
	//	<------------------------------------------------------------------------->
	/**
	 * Unbinds the current syslog host.
	 */
	public void close(){
		try {
			if (this.socket!=null) this.socket.close();
		} catch ( Exception ex ) {
		}
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 * @param facility Syslog facility
	 * @param priority Syslog priority
	 * @param message  Syslog message content
	 * @throws SyslogException
	 */
	public void send(int facility, int priority, String message ) {
		try {
			this.send( this.boundAddress, this.portNum, facility, priority, message );
		} catch ( Exception ex ) {
			if ( (this.flags & SyslogDefs.LOG_CONS) != 0 ) {
				String error = "error sending syslog message with parameters '" +facility+':'+priority +':'+ message + "': " + ex.getMessage();
				System.err.println(error);
			}
		}
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 * @param addr Syslog server InetAddress
	 * @param facility Syslog facility
	 * @param priority Syslog priority
	 * @param message Syslog message content
	 * @throws SyslogException
	 */
	public void send(InetAddress addr, int facility, int priority, String message) {
		try {
			this.send( addr, this.portNum, facility, priority, message);
		} catch ( Exception ex ) {
			if ( (this.flags & SyslogDefs.LOG_CONS) != 0 ) {
				String error = "error sending syslog message with parameters '" + addr +":"+ facility+':'+priority +':'+ message + "': " + ex.getMessage();
				System.err.println(error);
			}
		}
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 * @param hostname server name
	 * @param fac Syslog facility
	 * @param pri Syslog priority
	 * @param msg Syslog message content
	 * @throws SyslogException
	 */
	public void	send( String hostname, int fac, int pri, String msg ) {
		try {
			InetAddress address = InetAddress.getByName( hostname );
			this.send( address, this.portNum, fac, pri, msg );
		} catch ( Exception ex ) {
			if ( (this.flags & SyslogDefs.LOG_CONS) != 0 ) {
				String error = "error locating host named '" + hostname + "': " + ex.getMessage();
				System.err.println(error);
			}
		}
	}
	//	<------------------------------------------------------------------------->


	//	<------------------------------------------------------------------------->
	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 * @param hostname  Syslog server InetAddress
	 * @param port Syslog server port
	 * @param facility Syslog facility
	 * @param priority Syslog priority
	 * @param  message Syslog message content
	 */
	public void send( String hostname, int port, int facility, int priority, String message ) {
		try {
			InetAddress address = InetAddress.getByName( hostname );
			this.send( address, port, facility, priority, message );
		} catch ( Exception ex ) {
			if ( (this.flags & SyslogDefs.LOG_CONS) != 0 ) {
				String error = "error locating host named '" + hostname + "': " + ex.getMessage();
				System.err.println(error);
			}
		}
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 * @param addr
	 * @param port
	 * @param fac
	 * @param pri
	 * @param msg
	 * @throws SyslogException
	 */
	public void send( InetAddress addr, int port, int fac, int pri, String msg ) throws SyslogException
	{
		int				pricode;
		int				length;
		int				idx;
		byte[]			data;
		byte[]			sBytes;
		String			nmObj;
		String			strObj;

		pricode = SyslogDefs.computeCode( fac, pri );
		Integer priObj = new Integer( pricode );

		if ( this.processName != null ){
			nmObj = new String( this.processName );
		} else {
			nmObj = new String( Thread.currentThread().getName() );
		}

		length = 4 + nmObj.length() + msg.length() + 1;
		length += (pricode > 99) ? 3 : ( (pricode > 9) ? 2 : 1 );

		String dStr = null;
		if ( this.includeDate ) {
			// See note above on why we have two formats...
			Calendar now = Calendar.getInstance();
			if ( now.get( Calendar.DAY_OF_MONTH ) < 10 ) {
				dStr = this.date1Format.format( now.getTime() );
			} else {
				dStr = this.date2Format.format( now.getTime() );
			}
			length += dStr.length();
		}

		data = new byte[length];

		idx = 0;
		data[idx++] = (byte)'<';

		strObj = Integer.toString( priObj.intValue() );
		sBytes = strObj.getBytes();
		System.arraycopy( sBytes, 0, data, idx, sBytes.length );
		idx += sBytes.length;

		data[idx++] = (byte)'>';

		if ( this.includeDate && dStr!=null) {
			sBytes = dStr.getBytes();
			System.arraycopy ( sBytes, 0, data, idx, sBytes.length );
			idx += sBytes.length;
		}

		sBytes = nmObj.getBytes();
		System.arraycopy( sBytes, 0, data, idx, sBytes.length );
		idx += sBytes.length;

		data[idx++] = (byte)':';
		data[idx++] = (byte)' ';

		sBytes = msg.getBytes();
		System.arraycopy( sBytes, 0, data, idx, sBytes.length );
		idx += sBytes.length;

		data[idx] = 0;

		DatagramPacket packet = new DatagramPacket( data, length, addr, port );

		try {
			socket.send( packet );
		} catch ( IOException ex ) {
			String message = "error sending message: '" + ex.getMessage() + "'";
			throw new SyslogException( message );
		}

		if ( (this.flags & SyslogDefs.LOG_PERROR) != 0 ) {
			if ( this.processName != null ) {
				System.err.print( this.processName + ": " );
			} else {
				System.err.print( Thread.currentThread().getName() + ": " );
			}
		} 
	}
	//	<------------------------------------------------------------------------->

}


