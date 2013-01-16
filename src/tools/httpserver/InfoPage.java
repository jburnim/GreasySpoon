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
 *-----------------------------------------------------------------------------*/ 
package tools.httpserver;

//////////////////////////////////////////
//IMPORTS
import java.util.Date;
import tools.httpserver.custom.ProjectSpecifics;
import java.lang.management.*;
import tools.monitor.TrafficStatistics;
import java.io.*;
import java.text.*;
//////////////////////////////////////////

/**
 * Generate HTML content representing application version.<br>
 * Includes some system parameters: OS, architecture, memory<br>
 * Replace any HTML &lt;!--versions--> tag by generated HTML info
 */
public class InfoPage {

	static String headclass = "section";
	static String itemclass = "item";

	

//	-----------------------------------------------------------------------------
	/**
	 * @return Server parameters formatted as HTML Strings 
	 */
	public static String getText(){
		StringBuilder content = new StringBuilder();
		content.append("<div class=\""+headclass+"\">");
		content.append("System");
		content.append("</div>\r\n");
		content.append("<div class=\""+itemclass+"\">");
		content.append(getSystemParameters());
		content.append("</div>\r\n<br /><br />");

		content.append("<div class=\""+headclass+"\">");
		content.append("Java");
		content.append("</div>\r\n");
		content.append("<div class=\""+itemclass+"\">");
		content.append(getJavaParameters());
		content.append("</div>\r\n<br /><br />");

		content.append("<div class=\""+headclass+"\">");
		content.append("Locale");
		content.append("</div>\r\n");
		content.append("<div class=\""+itemclass+"\">");
		content.append(getLocaleParameters());
		content.append("</div>\r\n<br /><br />");

		content.append("<div class=\""+headclass+"\">");
		content.append("Runtime");
		content.append("</div>\r\n");
		content.append("<div class=\""+itemclass+"\">");
		content.append(getMemoryParameters());
		content.append("</div>\r\n<br /><br />");

		return content.toString();
	}
//	-----------------------------------------------------------------------------

//	-----------------------------------------------------------------------------
	/**
	 * @return System parameters formatted as HTML Strings 
	 */
	public static String getSystemParameters(){
		StringBuilder stb = new StringBuilder();
		stb.append("ICAP Server:\trelease ").append(ProjectSpecifics.getProjectVersion()).append("<br />\r\n");
		stb.append("Operating System:\t").append(System.getProperty("os.name")).append("<br>\r\n");
		stb.append("Version:\t").append(System.getProperty("os.version")).append("<br />\r\n");
		stb.append("Level:\t").append(System.getProperty("sun.os.patch.level")).append("<br />\r\n");
		stb.append("Architecture:\t").append(Runtime.getRuntime().availableProcessors()
				+" * "+System.getProperty("os.arch")).append("<br />\r\n");
		return stb.toString();
	}
//	-----------------------------------------------------------------------------

//	-----------------------------------------------------------------------------
	/**
	 * @return System parameters formatted as HTML Strings 
	 */
	public static String getMxParameters(){
		StringBuilder stb = new StringBuilder();
		MemoryMXBean mm = ManagementFactory.getMemoryMXBean();

		stb.append(mm.getObjectPendingFinalizationCount()).append("\r\n");
		stb.append("Heap usage: "+mm.getHeapMemoryUsage()).append("\r\n");
		stb.append("Non Heap usage: "+mm.getNonHeapMemoryUsage()).append("\r\n");

		ThreadMXBean tm = ManagementFactory.getThreadMXBean();
		stb.append(tm.getCurrentThreadCpuTime()).append("\r\n");
		stb.append(tm.getCurrentThreadUserTime()).append("\r\n");
		stb.append(tm.getPeakThreadCount()).append("\r\n");
		stb.append(tm.getDaemonThreadCount()).append("\r\n");


		ThreadInfo[] ti = tm.dumpAllThreads(false,false);
		for (ThreadInfo t:ti){
			stb.append(t.toString()).append("\r\n");
		}

		return stb.toString();
	}
//	-----------------------------------------------------------------------------


//	-----------------------------------------------------------------------------
	/**
	 * @return JVM parameters formatted as HTML Strings 
	 */
	public static String getJavaParameters(){
		StringBuilder stb = new StringBuilder();
		stb.append("Runtime:\t").append(System.getProperty("java.runtime.name")).append("<br />\r\n");
		stb.append("Version:\t").append(System.getProperty("java.vm.version")).append("<br />\r\n");
		stb.append("Vendor:\t").append(System.getProperty("java.vm.vendor")).append("<br />\r\n");
		return stb.toString();
	}
//	-----------------------------------------------------------------------------

//	----------------------------------------------------------------------------
	/**
	 * @return Locale parameters formatted as HTML Strings 
	 */
	@SuppressWarnings("deprecation")
	public static String getLocaleParameters(){
		StringBuilder stb = new StringBuilder();
		stb.append("Country:\t").append(System.getProperty("user.country")).append("<br />\r\n");
		stb.append("Time zone:\t").append(System.getProperty("user.timezone")).append("<br />\r\n");
		stb.append("Locale time:\t").append((new Date()).toLocaleString()).append("<br />\r\n");
		stb.append("Language:\t").append(System.getProperty("user.language")).append("<br />\r\n");
		return stb.toString();
	}
//	-----------------------------------------------------------------------------

//	----------------------------------------------------------------------------
	/**
	 * @return Time information for this server 
	 */
	@SuppressWarnings("deprecation")//I just hate new Calendar methods
	public static String getTimeInformation(){
		StringBuilder stb = new StringBuilder();
		stb.append("<div class=\"").append(headclass).append("\">").append("Time Information").append("</div>\r\n");
		stb.append("<div class=\"").append(itemclass).append("\">");
		long elapsed = (System.currentTimeMillis()- HttpServer.starttime) /1000;
		int days = (int)elapsed/86400;
		long rst = elapsed%86400;
		stb.append("How long server has been running:&nbsp;&nbsp;&nbsp;\t");
		if (days>0) stb.append(days).append(" Days, ");
		int hours = (int)rst/3600;
		rst = rst%3600;
		int min = (int)rst/60;
		stb.append(hours).append(" hours, ")
		.append(min).append(" minutes")
		.append("<br />\r\n");
		stb.append("Time Server started:&nbsp;&nbsp;&nbsp;\t");
		stb.append((new Date(HttpServer.starttime)).toLocaleString()).append("<br />\r\n");
		stb.append("</div>");
		stb.append("<div class='").append(itemclass).append("' align=right><i>Server Time Stamp:&nbsp;&nbsp;&nbsp;\t").append((new Date()).toLocaleString()).append("</i></div>\r\n");
		return stb.toString();
	}
//	-----------------------------------------------------------------------------

//	-----------------------------------------------------------------------------
	/**
	 * @return the Cumulative Activity for this server
	 */
	public static String getCurrentActivity(){
		StringBuilder stb = new StringBuilder();
		stb.append("<div class=\"").append(headclass).append("\">").append("Current Activity").append("</div>\r\n");
		stb.append("<div class=\"").append(itemclass).append("\">");
		long loadreqs = TrafficStatistics.getActivityLoad();
		long currentreqs = TrafficStatistics.fetchActivityCounter();
		stb.append("Processed Requests since last display:&nbsp;&nbsp;&nbsp;\t").append(currentreqs).append("<br />\r\n");
		stb.append("Instant load since last display:&nbsp;&nbsp;&nbsp;\t").append(loadreqs).append(" r/s<br />\r\n");
		stb.append("</div>");
		return stb.toString();
	}
//	-----------------------------------------------------------------------------

	//-----------------------------------------------------------------------------
	/**
	 * @return the Cumulative Activity for this server
	 */
	public static String getCumulativeActivity(){
		StringBuilder stb = new StringBuilder();
		stb.append("<div class=\"").append(headclass).append("\">").append("Cumulative Activity").append("</div>\r\n");
		stb.append("<div class=\"").append(itemclass).append("\">");
		long elapsed = (System.currentTimeMillis() - HttpServer.starttime)/1000;
		long totalreqs = TrafficStatistics.getRequestsTotal();
		stb.append("Total Processed Requests:&nbsp;&nbsp;&nbsp;\t").append(totalreqs).append("<br />\r\n");
		float average = elapsed==0? totalreqs : totalreqs / elapsed;
		stb.append("Overall Average load:&nbsp;&nbsp;&nbsp;\t").append(average).append(" r/s<br />\r\n"); 
		stb.append("</div>");
		return stb.toString();
	}
	//-----------------------------------------------------------------------------

	
	//-----------------------------------------------------------------------------
	/**
	 * @return the Disk occupation for this server (total and free space)
	 * Free space is in red if lower than 10% of total space and lower than 2 Go
	 */
	public static String getDiskOccupation(){
		String path = HttpServer.getApplicationPath();
		File f = new File(path);
		float totalSpace = f.getTotalSpace()/1048576;
		float freespace = f.getUsableSpace()/1048576;
		boolean limit = freespace<(totalSpace/10) && freespace < 2000;
		StringBuilder stb = new StringBuilder();
		stb.append("<div class=\"").append(headclass).append("\">").append("Disk occupation").append("</div>\r\n");
		stb.append("<div class=\"").append(itemclass).append("\">");
		NumberFormat formatter = new DecimalFormat("#,###,###");
		stb.append("Disk partition size:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\t")
				.append(formatter.format(totalSpace))
				.append(" Mo<br />\r\n");
		
		stb.append(limit?"<font color='red'>":"");
		stb.append("Free space remaining:&nbsp;&nbsp;&nbsp;\t")
			.append(formatter.format(freespace))
			.append(" Mo<br />\r\n");
		stb.append(limit?"</font>":"").append("</div>");
		return stb.toString();
	}
	//-----------------------------------------------------------------------------
	
//	-----------------------------------------------------------------------------
	/**
	 * @return JVM memory and uptime parameters formatted as HTML String
	 */

	public static String getMemoryParameters(){
		StringBuilder stb = new StringBuilder();
		long elapsed = (System.currentTimeMillis()- HttpServer.starttime) /1000;
		stb.append("Uptime:\t");
		int days = (int)elapsed/86400;
		long rst = elapsed%86400;
		if (days>0) stb.append(days).append(" d:");
		int hours = (int)rst/3600;
		rst = rst%3600;
		int min = (int)rst/60;
		rst = rst%60;
		stb.append(hours).append(" h:")
		.append(min).append(" m:")
		.append(rst).append(" s")
		.append("<br />\r\n");
		stb.append("Free Memory:\t").append((int)((Runtime.getRuntime().maxMemory()-Runtime.getRuntime().totalMemory())/1048576.f*100)/100.f).append(" Mo<br>\r\n");
		stb.append("   Memory usage (allocated/used/buffer):\t")
		.append((int)(Runtime.getRuntime().maxMemory()/1048576.f*100)/100.f)
		.append(" / ")
		.append((int)(Runtime.getRuntime().totalMemory()/1048576.f*100)/100.f)
		.append(" / ")
		.append((int)(Runtime.getRuntime().freeMemory()/1048576.f*100)/100.f)
		.append(" Mo<br />\r\n");
		return stb.toString();
	}
//	-----------------------------------------------------------------------------



}
