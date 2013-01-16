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
 * Created  :   27 July 2009
 *---------------------------------------------------------------------------*/
package tools.monitor;

//////////////////////////////////////////
//IMPORTS
import java.io.*;
import java.awt.*;
import org.jrobin.core.*;
import org.jrobin.graph.*;
import java.lang.management.*;  
//////////////////////////////////////////

/**
 * Collect statistics, store them in RRD database and generates graphs
 * Based on JRobin
 * @author Karel
 */
public class ServerStatistics {

	//---------------------------------------------
	// RRD file path
	private static String RRDPATH = "./";
	/** RRD database used to store statistics */
	public static final String RRD_FILE = "systemload.rrd";
	// pool of open RRD files (we have just a single one, but who cares)
	RrdDbPool rrdPool;
	//---------------------------------------------

	static java.awt.Font titleFont, textFont;
	static {
		if (java.awt.Font.getFont("Tahoma") !=null){
			titleFont = new java.awt.Font("Tahoma", java.awt.Font.PLAIN,13);
			textFont = new java.awt.Font("Tahoma", java.awt.Font.PLAIN,10);
		} else {
			titleFont = new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN,13);
			textFont = new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN,10);
		}
	}
	
	//---------------------------------------------
	// Image file paths
	private static String FILEPATH = "./";
	/**suffix added to daily graphs*/
	public static final String NAME_DAILY = "-daily.png";
	/**suffix added to weekly graphs*/
	public static final String NAME_WEEKLY = "-weekly.png";
	/**suffix added to monthly graphs*/
	public static final String NAME_MONTHLY = "-monthly.png";
	/**suffix added to yearly graphs*/
	public static final String NAME_YEARLY = "-yearly.png";
	//---------------------------------------------

	//---------------------------------------------
	// Sampling resolution
	/** Sampling resolution (in seconds): counters collections rate*/
	public static final int SAMPLING_RESOLUTION = 5; // seconds
	/**Graph will be recreated each minute (60 seconds)*/
	public static final int GRAPH_RESOLUTION = 60; // seconds
	//---------------------------------------------

	//---------------------------------------------
	OperatingSystemMXBean systembean = null; 
	GarbageCollectorMXBean gcbean = null;
	MemoryMXBean memorybean = null;

	private long gcCollectionCounter=0,gcCollectionTimeCounter=0;
	static final int nbCPU = Runtime.getRuntime().availableProcessors();

	private long lastSystemTime      = 0;
	private long lastProcessCpuTime  = 0;
	//---------------------------------------------

	//---------------------------------------------
	final RrdGraphDef serverLoadGraphDef = new RrdGraphDef();
	final RrdGraphDef gcGraphDef = new RrdGraphDef();
	final RrdGraphDef memGraphDef = new RrdGraphDef();
	final RrdGraphDef reqGraphDef = new RrdGraphDef();
	//---------------------------------------------
	
	GraphThread graphThread = null;
	CollectorThread collector = null;
	private boolean isrunning = false;

//	<--------------------------------------------------------------------------------->
	/**
	 * Build up a new System statistics monitor
	 * @param rrdpath Path in which to store RRD database
	 * @param outputpath Path in which to generate graphs
	 */
	public ServerStatistics(String rrdpath, String outputpath) {
		try{
			rrdPool = RrdDbPool.getInstance();
			FILEPATH = outputpath.endsWith("/")?outputpath:outputpath+'/';
			RRDPATH = rrdpath.endsWith("/")?rrdpath:rrdpath+'/';
			createMonitorObjects();
			createRrdFileIfNecessary(false);
			initGraphs();
		} catch (Exception e){
			reportException(e);
			System.err.println("Unable to start System statistics monitoring: "+e.getLocalizedMessage());
		}
	}
//	<--------------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------------->
	/**
	 * Start counters collection and graph generation
	 */
	public synchronized void start() {
		try{
			if (isrunning) return;
			isrunning = true;
			collector = new CollectorThread();
			collector.start();
			graphThread = new GraphThread();
			graphThread.start();
		} catch (Exception e){
			reportException(e);
			isrunning = false;
		}
	}
//	<--------------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------------->
	/**
	 * Stop system monitoring
	 */
	public synchronized void stop(){
		isrunning = false;
		if (graphThread!=null && graphThread.isAlive()) graphThread.interrupt();
		if (collector!=null && collector.isAlive()) collector.interrupt();
	}
//	<--------------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------------->
	/**creates statistic collector objects*/
	private void createMonitorObjects() throws Exception {
		try {systembean = ManagementFactory.getOperatingSystemMXBean();} catch (Exception e){ reportException(e); 	}
		try {gcbean = ManagementFactory.getGarbageCollectorMXBeans().get(0);} catch (Exception e){ reportException(e); 	}
		try {memorybean = ManagementFactory.getMemoryMXBean();} catch (Exception e){ reportException(e); 	}
	}
//	<--------------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------------->
	/**
	 * @return CPU time consumed by JVM threads (this is NOT CPU total usage)
	 */
	@SuppressWarnings("restriction")
	public synchronized double getJvmCpuUsage() {
		if ( !(systembean instanceof com.sun.management.OperatingSystemMXBean) ) return -1;
		long systemTime = System.nanoTime();
		if ( lastSystemTime == 0 ) {
			lastProcessCpuTime = ((com.sun.management.OperatingSystemMXBean)systembean).getProcessCpuTime();
			lastSystemTime = systemTime;
			return -1;
		}
		long processCpuTime = ((com.sun.management.OperatingSystemMXBean)systembean).getProcessCpuTime();
		double cpuUsage = ((double) ( processCpuTime - lastProcessCpuTime )) / ((double)( systemTime - lastSystemTime ));
		lastSystemTime     = systemTime;
		lastProcessCpuTime = processCpuTime;
		return cpuUsage / nbCPU;
	}
//	<--------------------------------------------------------------------------------->



//	<--------------------------------------------------------------------------------->
	/**
	 * @return System load average
	 */
	private double getLoadAverage() {
		if (systembean!= null) return systembean.getSystemLoadAverage()/nbCPU;
		return -1;
	}  
//	<--------------------------------------------------------------------------------->
	
//	<--------------------------------------------------------------------------------->
	/** @return OS free memory (in %)*/
	@SuppressWarnings("restriction")
	private double getSystemMemoryUsage(){
		if ( systembean instanceof com.sun.management.OperatingSystemMXBean ){
			double tm = ((com.sun.management.OperatingSystemMXBean)systembean).getTotalPhysicalMemorySize();
			double fm = ((com.sun.management.OperatingSystemMXBean)systembean).getFreePhysicalMemorySize();
			return (tm-fm)/tm;
		} 
		return -1;
	}
//	<--------------------------------------------------------------------------------->
	
//	<--------------------------------------------------------------------------------->
	/**Return last number of GC calls since last method call*/ 
	private long getGcCalls(){
		long currentvalue = gcbean.getCollectionCount();
		long delta = currentvalue - gcCollectionCounter;
		gcCollectionCounter = currentvalue;
		return delta;
	}
//	<--------------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------------->
	/**Return last time spent in GC since last method call*/
	private long getGcTime(){
		long currentvalue = gcbean.getCollectionTime();
		long delta = currentvalue - gcCollectionTimeCounter;
		gcCollectionTimeCounter = currentvalue;
		return delta;
	}
//	<--------------------------------------------------------------------------------->

//	<--------------------------------------------------------------------------------->
	/**
	 * creates RRD file to store statistics
	 * @param overwriteExisting Set if RRD file must be deleted if it already exists
	 * @throws RrdException
	 * @throws IOException
	 */
	private void createRrdFileIfNecessary(boolean overwriteExisting) throws RrdException, IOException {
		String rrdPath = RRDPATH+RRD_FILE;
		File rrdFile = new File(rrdPath);
		
		if (rrdFile.exists()){
			if (overwriteExisting){
				rrdFile.delete();
				rrdFile = new File(rrdPath);
			} else {
				return;
			}
		}

		// create RRD file since it does not exist
		RrdDef rrdDef = new RrdDef(rrdPath, SAMPLING_RESOLUTION);
		rrdDef.addDatasource("serverload", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);
		rrdDef.addDatasource("sysmem", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);
		rrdDef.addDatasource("transactions", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);

		rrdDef.addDatasource("jvmload", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);
		rrdDef.addDatasource("jvmmem", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);
		rrdDef.addDatasource("gccalls", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);
		rrdDef.addDatasource("gctime", "GAUGE", 5 * SAMPLING_RESOLUTION, 0, Double.NaN);

		// create hourly, daily, weekly and yearly precision archives
		rrdDef.addArchive("AVERAGE", 0.5, 1, 4000);
		rrdDef.addArchive("AVERAGE", 0.5, 6, 4000);
		rrdDef.addArchive("AVERAGE", 0.5, 24, 4000);
		rrdDef.addArchive("AVERAGE", 0.5, 288, 4000);

		rrdDef.addArchive("MAX", 0.5, 1, 4000);
		rrdDef.addArchive("MAX", 0.5, 6, 4000);
		rrdDef.addArchive("MAX", 0.5, 24, 4000);
		rrdDef.addArchive("MAX", 0.5, 288, 4000);
		// create RRD file in the pool
		RrdDb rrdDb = rrdPool.requestRrdDb(rrdDef);
		rrdPool.release(rrdDb);
	}
//	<--------------------------------------------------------------------------------->

	
//	<--------------------------------------------------------------------------------->
	/**
	 * Initialize graphics rendering for statistics display
	 * @throws RrdException
	 */
	public void initGraphs() throws RrdException{
		// create common part of graph definition
		// SERVER LOAD GRAPH
		serverLoadGraphDef.setPoolUsed(true);
		serverLoadGraphDef.datasource("serverload",RRDPATH+RRD_FILE, "serverload", "MAX");
		serverLoadGraphDef.datasource("busy", "serverload,100,*");
		serverLoadGraphDef.area("busy", Color.RED, "Server load");
		serverLoadGraphDef.gprint("busy", "AVERAGE", ": %3.1f");

		serverLoadGraphDef.datasource("sysmem",RRDPATH+RRD_FILE, "sysmem", "MAX");
		serverLoadGraphDef.datasource("memstat", "sysmem,100,*");
		serverLoadGraphDef.line("memstat", Color.BLUE, "Memory load");
		serverLoadGraphDef.gprint("memstat", "AVERAGE", ": %3.1f");

		serverLoadGraphDef.setMaxValue(100);  
		skin(serverLoadGraphDef);

		// GC LOAD GRAPH
		gcGraphDef.datasource("gccalls",RRDPATH+RRD_FILE, "gccalls", "AVERAGE");
		gcGraphDef.datasource("gctime",RRDPATH+RRD_FILE, "gctime", "AVERAGE");
		gcGraphDef.area("gccalls", Color.GREEN, "GC Calls");
		gcGraphDef.line("gctime", Color.BLUE, "GC time (ms)");
		skin(gcGraphDef);

		// JVM LOAD GRAPH
		memGraphDef.datasource("jvmload",RRDPATH+RRD_FILE, "jvmload", "AVERAGE");
		memGraphDef.datasource("javacpu", "jvmload,100,*");
		memGraphDef.area("javacpu", Color.RED, "CPU load:");
		memGraphDef.gprint("javacpu", "AVERAGE", "%3.1f    ");

		memGraphDef.datasource("jvmmem",RRDPATH+RRD_FILE, "jvmmem", "MAX");
		memGraphDef.datasource("memstat", "jvmmem,100,*");
		memGraphDef.line("memstat", Color.BLUE, "  JVM Memory:");
		memGraphDef.gprint("memstat", "AVERAGE", "%3.1f");
		memGraphDef.setMaxValue(100);  

		skin(memGraphDef);

		// TRANSACTIONS GRAPH
		reqGraphDef.datasource("transactions_avg",RRDPATH+RRD_FILE, "transactions", "AVERAGE");
		reqGraphDef.datasource("transactions_max",RRDPATH+RRD_FILE, "transactions", "MAX");
		reqGraphDef.area("transactions_max", Color.RED, "Transaction load");
		reqGraphDef.setVerticalLabel("requests / s");
		reqGraphDef.gprint("transactions_avg", "AVERAGE", "    Average: %3.1f r/s");
		reqGraphDef.gprint("transactions_max", "MAX", "    Peak: %3.1f r/s");
		skin(reqGraphDef);
	}
//	<--------------------------------------------------------------------------------->
	
//	<--------------------------------------------------------------------------------->
	/**
	 * Customize generated graphs
	 * @param graphDef
	 * @throws RrdException
	 */
	private static void skin(RrdGraphDef graphDef) throws RrdException {
		graphDef.setShowSignature(false);
		graphDef.setAntiAliasing(true);
		graphDef.setUnitsExponent(0);
		graphDef.setMinValue(0);

		graphDef.setLargeFont(titleFont);
		graphDef.setSmallFont(textFont);
		graphDef.setColor(RrdGraphConstants.COLOR_BACK, Color.WHITE);
		graphDef.setColor(RrdGraphConstants.COLOR_CANVAS,Color.WHITE);
		graphDef.setColor(RrdGraphConstants.COLOR_MGRID,new Color(220,220,220));
		graphDef.setColor(RrdGraphConstants.COLOR_FONT,new Color(0,37,119));
		graphDef.setColor(RrdGraphConstants.COLOR_FRAME,new Color(0,37,119));
		graphDef.setColor(RrdGraphConstants.COLOR_GRID,new Color(240,240,240));
		graphDef.setColor(RrdGraphConstants.COLOR_ARROW,new Color(0,37,119));
		graphDef.setImageFormat("PNG");
		graphDef.setLazy(true);
		graphDef.setWidth(706);
		graphDef.setHeight(265);
	}
//	<--------------------------------------------------------------------------------->    

//	<--------------------------------------------------------------------------------->
	/**
	 * Thread in charge of collecting server statistic
	 * Run with lowest priority
	 * @author Karel
	 */
	public class CollectorThread extends Thread{

		// until the end of the world
		double serverLoad,sysMemLoad, jvmCpuLoad,jvmMemLoad;
		double usedmem,maxmem, gccalls, gctime,transactions;
		// request RRD database reference from the pool
		RrdDb rrdDb = null;
		int errorcounter = 0;
		//<--------------------------------------------------------------------------------->
		/**Create a new graphing thread. Start must be called explicitly.*/
		public CollectorThread(){
			super("Statistics collector");
			this.setPriority(Thread.MIN_PRIORITY);
			maxmem = Runtime.getRuntime().maxMemory();
		}
		//<--------------------------------------------------------------------------------->
		
		//<--------------------------------------------------------------------------------->
		public void run(){

				while (isrunning) {
					try {
						//Server LOAD
						serverLoad = getLoadAverage();
						sysMemLoad = getSystemMemoryUsage();

						//JVM Load
						Runtime rt = Runtime.getRuntime();
						usedmem = rt.totalMemory()- rt.freeMemory();
						jvmMemLoad = ((float)usedmem/(float)maxmem);
						jvmCpuLoad = getJvmCpuUsage();

						//------GC------------
						gccalls = getGcCalls();
						gctime = getGcTime();

						// Build up RRD statistics
						transactions = TrafficStatistics.getTransactionsDelta()/SAMPLING_RESOLUTION;

						rrdDb = rrdPool.requestRrdDb(RRDPATH+RRD_FILE);
						// create sample with the current timestamp
						Sample sample = rrdDb.createSample();
						// set value for load datasource
						if (serverLoad!=-1) sample.setValue("serverload", serverLoad>1?1:serverLoad);
						if (sysMemLoad!=-1) sample.setValue("sysmem", sysMemLoad>1?1:sysMemLoad);

						if (jvmCpuLoad!=-1) sample.setValue("jvmload", jvmCpuLoad>1?1:jvmCpuLoad);
						sample.setValue("jvmmem", jvmMemLoad);
						sample.setValue("gccalls", gccalls);
						sample.setValue("gctime", gctime);
						sample.setValue("transactions", transactions);
						
						// update database & release RRD database reference
						sample.update();
						rrdPool.release(rrdDb);
					} catch (org.jrobin.core.RrdException e) {
						errorcounter++;
						if (errorcounter>2) isrunning = false;
						reportException(e);
						try {
							if (rrdDb!=null) rrdPool.release(rrdDb);
							createRrdFileIfNecessary(true);
						} catch (Exception e1){
							reportException(e1);
							isrunning = false;
						}
					} catch (Exception e) {
						errorcounter++;
						if (errorcounter>2) isrunning = false;
						reportException(e);
						try {
							if (rrdDb!=null) rrdPool.release(rrdDb);
							createRrdFileIfNecessary(true);
						} catch (Exception e1){
							reportException(e1);
							isrunning = false;
						}
					}
					try {// wait for a while
						Thread.sleep(SAMPLING_RESOLUTION * 1000L);
					} catch (Exception e){}
				}//while loop
				try {// try to free RRD database
					if (rrdDb!=null) rrdPool.release(rrdDb);
				} catch (Exception e){}
		}
		//<--------------------------------------------------------------------------------->
	}
//	<--------------------------------------------------------------------------------->

	
//	<--------------------------------------------------------------------------------->
	/**
	 * Thread in charge of generating statistic images for administration
	 * Run with lowest priority
	 * @author Karel
	 */
	public class GraphThread extends Thread{

		//<--------------------------------------------------------------------------------->
		/**Create a new graphing thread. Start must be called explicitly.*/
		public GraphThread(){
			super("Statistics grapher");
			this.setPriority(Thread.MIN_PRIORITY);
		}
		//<--------------------------------------------------------------------------------->
		
		//<--------------------------------------------------------------------------------->
		public void run(){
			long endTime;
			while (isrunning) {
				// ending timestamp is the current timestamp, starting timestamp will be adjusted for each graph
				endTime = Util.getTime();
				try {
					graphIt(serverLoadGraphDef, endTime,"Server Load", "load");
					graphIt(gcGraphDef, endTime,"GC Calls", "gc");
					graphIt(memGraphDef, endTime,"JVM Load", "jvm");
					graphIt(reqGraphDef, endTime,"Transactions", "req");
					// sleep for a while
				} catch (Exception e) {
					reportException(e);
				}

				try{
					Thread.sleep(GRAPH_RESOLUTION * 1000L);
				} catch (Exception e){
					reportException(e);
				}
			}//End while
		}
		//<--------------------------------------------------------------------------------->
	}
//	<--------------------------------------------------------------------------------->
	

//	<--------------------------------------------------------------------------------->
	private void graphIt(RrdGraphDef graphDef, long endTime,String comment, String fileprefix) throws Exception{
		// daily graph
		long startTime = endTime - 86400; 
		graphDef.setTimeSpan(startTime, endTime);
		graphDef.setTitle(comment+" - current day");
		graphDef.setFilename(FILEPATH+fileprefix+NAME_DAILY);
		new RrdGraph(graphDef); // uses pool

		// weekly graph
		startTime = endTime - 604800;
		graphDef.setTimeSpan(startTime, endTime);
		graphDef.setTitle(comment+"  - last week");
		graphDef.setFilename(FILEPATH+fileprefix+NAME_WEEKLY);
		new RrdGraph(graphDef); // uses pool


		// monthly graph
		startTime = endTime - 2678400;
		graphDef.setTimeSpan(startTime, endTime);
		graphDef.setTitle(comment+"  - last month");
		graphDef.setFilename(FILEPATH+fileprefix+NAME_MONTHLY);
		new RrdGraph(graphDef); // uses pool

		// yearly graph
		startTime = endTime - 31536000;
		graphDef.setTimeSpan(startTime, endTime);
		graphDef.setTitle(comment+" - last year");
		graphDef.setFilename(FILEPATH+fileprefix+NAME_YEARLY);
		new RrdGraph(graphDef); // uses pool
	}
//	<--------------------------------------------------------------------------------->


//	<--------------------------------------------------------------------------------->
	// reports exception by printing it on the stderr device
	private static void reportException(Exception e) {
		System.err.println("Statistics ERROR : " + e);
		e.printStackTrace();
	}
//	<--------------------------------------------------------------------------------->

}

