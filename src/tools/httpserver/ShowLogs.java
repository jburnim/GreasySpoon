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
 * Created	:	9 dec. 06
 *---------------------------------------------------------------------------*/
package tools.httpserver;

///////////////////////////////////
//Import
import java.io.RandomAccessFile;
import java.io.File;
import tools.logger.*;
import java.util.*;
///////////////////////////////////

/**
 * Allows to consult log files.<br>
 * Read the last 200 lines from log file and replace:
 * <ul>
 * <li>%name% by log file name</li>
 * <li>%lines% by number of loaded lines</li>
 * <li>&lt;!--logfile--> by lines loaded from log file</li>
 * </ul>
 */
public class ShowLogs {

	private static int tailSize = 300;
	static Hashtable<String, Long> positionRecorder = new Hashtable<String, Long>();
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Extract parameters from an URL. Parameters name are stored in lowercase
	 * @param parameters the parameters line to extract (?aa=bb&cc=dd)
	 * @return Hashtable<String, String> containing parameters name as key and parameters value as value  
	 */
	public static Hashtable<String, String> extractParams(String parameters){
		Hashtable<String, String> hashtable = new Hashtable<String, String>();
		String[] params = parameters.split("&");
		for (String s:params){
			String[] tagvalue = s.split("=",2);
			if (tagvalue.length==2){
				hashtable.put(tagvalue[0].toLowerCase(), tagvalue[1]);
			} else {
				hashtable.put(tagvalue[0].toLowerCase(), "");
			}
		}
		return hashtable;
	}
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Update given HTML page with log content 
	 * @param htmlcontent The HTML file in which to insert log content
	 * @param parameters provides either The log file name directly, or "logname=[filename]" & "tail=[true|false]"
	 * @return	the update HTML file
	 */
	public static String set(String htmlcontent, String parameters){
		String logpath = Log.getLogPath();
		if (parameters == null) return "";
		String logname = "";
		boolean tail = false;
		
		if (parameters.indexOf("=")==-1) {
			logname = parameters; 
		} else {
			Hashtable<String, String> params = extractParams(parameters);
			logname = params.get("logname");
			if (params.containsKey("tail")){
				tail = params.get("tail").equals("true")?true:false;
			}
		}

		File fich = new File(logpath+logname);
		if (!fich.exists()){
			Log.error(Log.WARNING, "[ADMIN_INTERFACE] Access to an unexisting log file:"+logpath+logname);
			return "";
		}// Endif

		StringBuilder loglines = new StringBuilder();
		boolean prepend = logname.toLowerCase().startsWith("debug")?true:false;
		
		try {
			String[] tailed; 
			
			if (tail) {
				tailed = tailFromLast(logpath, logname);
			} else {
				tailed = tail(logpath, logname, tailSize);
			}

			for (String s:tailed){
				s = s.replace("<","&lt;").replace("\r\n","<br />");
				if (prepend) {
					loglines.insert(0, "\n").insert(0, s);
				} else {
					loglines.append("\n").append(s);
				}
			}
			if (tail){
				return loglines.toString().trim();
			}

			htmlcontent=htmlcontent.replace("%name%", logname);
			htmlcontent=htmlcontent.replace("%lines%", tailed.length+"");
			htmlcontent=htmlcontent.replace("<!--logfile-->", loglines.toString());
			return htmlcontent;
		} catch (Exception e){
			Log.error(Log.WARNING, "[ADMIN_INTERFACE] Exception while accessing to log file:"+logpath+logname, e);
			return "";
		}//End try&catch
	} 
	//<------------------------------------------------------------------------------------------>
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Tail log starting from last recorded call
	 * @param logpath the log file path
	 * @param logname the log file name
	 * @return Vector containing ordered strings
	 */
	private static String[] tailFromLast(String logpath, String logname) {
		RandomAccessFile raf = null;
		File file = new File(logpath+logname);
		long tailposition = -1; 
		String[] result = new String[0];

		try {
			raf = new RandomAccessFile(file,"r");
			if (positionRecorder.containsKey(logname)) {
				tailposition = positionRecorder.get(logname);
			}
			 // Get the position of last character using (i.e length-1). Let this be curPos.
			long curPos = raf.length() - 1;

			if (tailposition == curPos) {
				raf.close();
				return result;
			}
			
			if (tailposition==-1 || curPos<tailposition) {
				tailposition = 0;
				return tail(logpath, logname, tailSize);
			}
			
			
			byte[] bytearray = new byte[ (int)(curPos - tailposition)];
			raf.seek(tailposition);
			raf.read(bytearray);
			String res = new String(bytearray);
			raf.close();
			positionRecorder.put(logname,new Long(curPos));
			// Got N lines or less than that
			String[] resarray = res.split("\n");
			return resarray;
		} catch(Exception e) {
			try{ if (raf!=null) raf.close();}catch (Exception e1){}
			return result;
		} finally {
			try{ if (raf!=null) raf.close();}catch (Exception e1){}
		}
	}
	//<------------------------------------------------------------------------------------------>
	
	
	
	//<------------------------------------------------------------------------------------------>
	/**
	 * Extract line strings from bytearray and store them into a vector
	 *   
	 * @param bytearray The byte array to parse
	 * @param lineCount The maximum lines to extract
	 * @param storageVector Vector used to store the lines
	 * @return true if lineCount has been extracted, false otherwise
	 */
	private static boolean parseLinesFromLast(byte[] bytearray, int lineCount, Vector<String> storageVector) {
		String lastNChars = new String (bytearray);//creates a String out of it
		StringBuilder sb = new StringBuilder(lastNChars);
		lastNChars = sb.reverse().toString();//reverses the string
		StringTokenizer tokens= new StringTokenizer(lastNChars,"\n");//extracts the lines
		while(tokens.hasMoreTokens()) {
			sb = new StringBuilder((String)tokens.nextToken());
			//characters in extracted line will be in reverse order, 
			// so it reverses the line just before storing in Vector
			storageVector.add(sb.reverse().toString());
			if(storageVector.size() == lineCount) {
				return true;//indicates we got 'lineCount' lines
			}
		}
		return false; //indicates didn't read 'lineCount' lines
	}
	//<------------------------------------------------------------------------------------------>
	
	private static int chunkSize = 2000;
	//<------------------------------------------------------------------------------------------>
	/**
	 * Reads last N lines from the given file. File reading is done in chunks.
	 * @param logpath Path to the log file
	 * @param fileName The file name to parse
	 * @param lineCount The number of line to extract
	 * @return A String array containing lineCount lines or less
	 */
	public static String[] tail(String logpath, String fileName, int lineCount) {
		Vector<String> lastNlines = new Vector<String>();	
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(logpath+fileName,"r");

			int delta=0;
			 // Get the position of last character using (i.e length-1). Let this be curPos.
			long curPos = raf.length() - 1;
			positionRecorder.put(fileName,new Long(curPos));
			long fromPos;
			byte[] bytearray;

			int counter = 0;
			while(counter<tailSize) {//security counter, to avoid possible infinite loop
				counter++;
				 // Move the cursor to fromPos = (curPos - chunkSize). Use seek().
				fromPos = curPos - chunkSize;
				 
				//If fromPos is less than or equal to ZERO then go from beginning, else go from end of the file
				if(fromPos <= 0){
					 //5 Read characters from beginning of file to curPos. Go to step-9.
					raf.seek(0);
					bytearray = new byte[(int)curPos];
					raf.readFully(bytearray);
					 // Read 'chunksize' characters from fromPos.
					parseLinesFromLast(bytearray, lineCount, lastNlines);
					//All lines are read when num of lines in file is less than N
					break;
				} else {		

					raf.seek(fromPos);
					bytearray = new byte[chunkSize];
					 // Read 'chunksize' characters from fromPos.
					raf.readFully(bytearray);
					// Extract the lines.
					if(parseLinesFromLast(bytearray, lineCount, lastNlines)){
						// Requested number of lines are read.
						break;
					}
					//Last line may be a incomplete, so discard it. Modify curPos appropriately
					delta = ((String)lastNlines.get(lastNlines.size()-1)).length();
					//In case we read an incomplete line with length higher than chunk, increase chunk size
					if (delta == chunkSize) chunkSize = chunkSize * 2;
					lastNlines.remove(lastNlines.size()-1);
					curPos = fromPos + delta;
				}
			}
			raf.close();
			// Got N lines or less than that
			return lastNlines.toArray(new String[0]);
		} catch(Exception e) {
			try{ if (raf!=null) raf.close();}catch (Exception e1){}
			return lastNlines.toArray(new String[0]);
		} finally {
			try{ if (raf!=null) raf.close();}catch (Exception e1){}
		}
	}
	//<------------------------------------------------------------------------------------------>

	
	//<------------------------------------------------------------------------------------------>
	/**
	 * @return the maximum number of lines showed in logs
	 */
	public static int getTailSize() {
		return tailSize;
	}

	/**
	 * @param tailSize Defines the maximum number of lines showed in logs
	 */
	public static void setTailSize(int tailSize) {
		ShowLogs.tailSize = tailSize;
	}	
	//<------------------------------------------------------------------------------------------>
	
}
