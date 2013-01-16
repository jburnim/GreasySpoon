/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008-2010 Karel Mittig
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
package tools.logger.syslog;


/**
 * Syslog facility to log messages on a local or distant syslog server
 * @author mittig
 */
public class SyslogClient {
    
    String ip="";
    int port = SyslogDefs.DEFAULT_PORT;
    int defaultFacility = SyslogDefs.LOG_USER;
    int defaultPriority = SyslogDefs.LOG_INFO;
    boolean enabled = true;
    private SyslogConnection syslog;
    
//	<------------------------------------------------------------------------->
    /**
     * Initialize client with given syslog server
     * @param ip The IP address of Syslog server
     * @param port The syslog server port
     * @param defaultFacility the default Facility on which to send messages
     * @param defaultPriority the default priority of the messages
     */
    public SyslogClient(String processName, String ip, int port, int defaultFacility, int defaultPriority){
        this.ip = ip;
        this.port = port;
        this.defaultFacility = defaultFacility;
        this.defaultPriority = defaultPriority;
        enabled = true;
        try{
        	syslog = new SyslogConnection(ip, port,processName, defaultFacility);
        } catch (Exception e){
            enabled = false;
            System.err.println("Unable to initialize Syslog client with server ["+ip+":"+port+"]");
            this.stop();
        }
    }
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
    /**
     * Initialize client with given SYSLOG server
     * Use default SYSLOG port, LOG_USER facility and INFO level as default configuration
     * @param serverAddress The IP address or DNS name of the SYSLOG server
     */
    public SyslogClient(String processName, String serverAddress){
        this(processName,serverAddress, SyslogDefs.DEFAULT_PORT,SyslogDefs.LOG_USER,SyslogDefs.LOG_INFO);
    }
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
    /**
     * Initialize client with given SYSLOG server
     * Use default SYSLOG port, LOG_USER facility and INFO level as default configuration
     * @param serverAddress The IP address or DNS name of the SYSLOG server
     */
    public SyslogClient(String processName, String serverAddress, String facility){
        this(processName,serverAddress, SyslogDefs.DEFAULT_PORT,SyslogDefs.getFacility(facility),SyslogDefs.LOG_INFO);
    }
//	<------------------------------------------------------------------------->
    
//	<------------------------------------------------------------------------->
    /**
     * Send the supplied String to syslog server using USER facility and INFO level
     * @param s String to be logged
     */
    public void log(String s) {
    		syslog.send(defaultFacility, defaultPriority, s);
    }
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
    /**
     * Send the supplied String to syslog server using USER facility and provided level
     * @see SyslogDefs for available levels
     * @param syslogLevel the syslog level
     * @param s String to be logged
     */
    public void log(int syslogLevel, String s) {
	   	syslog.send(defaultFacility,syslogLevel, s);
    }
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
    /**
     * Send the supplied String to syslog server using provided facility and level
     * @see SyslogDefs for available facilities and levels
     * @param syslogFacility the syslog facility of current process
     * @param syslogLevel the syslog level
     * @param s String to be logged
     */
    public void log(int syslogFacility, int syslogLevel, String s) {
	    	syslog.send(syslogFacility,syslogLevel, s);
    }
//	<------------------------------------------------------------------------->
    
//	<------------------------------------------------------------------------->
    /**
     * Stop and clean syslog client process
     */
    public void stop () {
    	syslog.close();
        enabled = false;
    }
//	<------------------------------------------------------------------------->

}
