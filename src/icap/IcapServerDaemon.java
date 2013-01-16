/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2009-2010 Karel Mittig
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
 * Created  :   May. 2009 by Brad
 *---------------------------------------------------------------------------*/
package icap;

///////////////////////////////////
//Import
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
///////////////////////////////////
/**
 * A basic attempt to wrap GreasySpoon with Apache commons-daemon 
 */
public class IcapServerDaemon implements Daemon {

	
	
    ///////////////////////////////////////////////////////////////////////
    // Linux/Unix style service
	//	<--------------------------------------------------------------------------->
    /**
     * Free all GS resources
     * This method is called after stop() call to cleanup everything
     * @see org.apache.commons.daemon.Daemon#destroy()
     */
    public void destroy() {
    	//icap.IcapServer.stopServers();
    	System.gc();
    }
    //	<--------------------------------------------------------------------------->
    
    //	<--------------------------------------------------------------------------->
    /**
     * Start functions and processes that require su rights 
     * @see org.apache.commons.daemon.Daemon#init(org.apache.commons.daemon.DaemonContext)
     */
    public void init(DaemonContext context) {
    	//nothing to do up to now as super-user
    }
    //	<--------------------------------------------------------------------------->
    
    //	<--------------------------------------------------------------------------->
    /**
     * Invoke IcapServer main() with null args to start the server with default config
     * and return cleanly without calling System.exit()
     * @see org.apache.commons.daemon.Daemon#start()
     */
    public void start() {
    	icap.IcapServer.turnStdOff = true;
    	icap.IcapServer.main(null);
    }
    //	<--------------------------------------------------------------------------->    
    
    //	<--------------------------------------------------------------------------->    
    /**
     * Stop running ICAP servers in the clean way by
     *  - closing connections
     *  - freeing resources
     * @see org.apache.commons.daemon.Daemon#stop()
     */
    public void stop() {
    	icap.IcapServer.stopServers();
    	//System.exit(0);
    }
    //	<--------------------------------------------------------------------------->    
    
   
     
    
    
    ///////////////////////////////////////////////////////////////////////
    // Windows style service (has nothing to do with the Daemon interface
    
    private static volatile boolean shouldRun = false;
    //	<--------------------------------------------------------------------------->
    /**
     * Daemon START Method under MS Windows 
     * @param aArgs
     */
    public static void windowsServiceStart(String[] aArgs) {
		shouldRun = true;
		// start the server
		icap.IcapServer.turnStdOff = true;
		icap.IcapServer.main(null);
		// wait until we're told to stop
		while (shouldRun) {
		    try {
		    	Thread.sleep(1000);
		    } catch (Exception e) {
			// still don't do anything until we're told to exit, i.e. shouldRun == false
		    }
		}//End infinite loop
		icap.IcapServer.stopServers();
    }
    //	<--------------------------------------------------------------------------->
    
    //	<--------------------------------------------------------------------------->
    /**
     * Daemon STOP Method under MS Windows 
     * @param aArgs
     */
    public static void windowsServiceStop(String[] aArgs) {
    	shouldRun = false;
    	icap.IcapServer.stopServers();
    }
    //	<--------------------------------------------------------------------------->
    
}
