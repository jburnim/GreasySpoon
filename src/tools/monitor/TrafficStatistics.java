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
 * Created  :   22 July 2009
 *---------------------------------------------------------------------------*/
package tools.monitor;

/**
 * Class used to store traffic statistics
 * @author Karel
 */
public class TrafficStatistics {

	private static long globalRequestCounter = 0;
	private static long monitorCounter = 0;
	private static long deltaCounter = 0;
	private static long timeStamp = System.nanoTime();
	
	/**
	 * Increase transactions counter with a hit
	 */
	public static void hit(){
		globalRequestCounter++;
	}
	
	/**
	 * Similar to fetchActivityCounter, but protected so it can be used to store result in rrd
	 * @return Transactions made since last call to this method
	 */
	protected static long getTransactionsDelta(){
		long d = globalRequestCounter - deltaCounter;
		deltaCounter = globalRequestCounter;
		return d;
	}
	
	/**
	 * @return The average transactions per seconds made since last call to this method
	 */
	public static long getActivityLoad(){
		long timeStamp2 = System.nanoTime();
		long deltaseconds = (timeStamp2 - timeStamp) / 1000000000;
		if (deltaseconds == 0) return (globalRequestCounter - monitorCounter);
		long load = (globalRequestCounter - monitorCounter)/deltaseconds;
		timeStamp = timeStamp2;
		return load;
	}
	
	/**
	 * @return Transactions made since last call to this method
	 */
	public static long fetchActivityCounter(){
		long d = globalRequestCounter - monitorCounter;
		monitorCounter = globalRequestCounter;
		return d;
	}

	
	/**
	 * @return The total transactions recorded by this class
	 */
	public static long getRequestsTotal(){
		return globalRequestCounter;
	}
	
}
