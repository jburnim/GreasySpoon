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
 * Created on 8 mars 2005
 *-----------------------------------------------------------------------------*/
package tools.httpserver;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Tools to manipulate various encoding.<br>
 * Supports hexa<=>ascii encoding/decoding and utf8<=>ascii decoding  
 */
public class TextTools {

//	<------------------------------------------------------------------------------------------>    
	/**
	 * Convert hex encoded string (ex: %20 for space) into ascii
	 * @param stringToConvert the hex encoded string
	 * @return ascii convertion
	 */
	public final static String hexToAscii(String stringToConvert){
		int pos=0, start = 0;
		String codevalue="";
		StringBuilder result=new StringBuilder();
		try{
			while ( (pos=stringToConvert.indexOf('%',start))!=-1){
				codevalue = stringToConvert.substring(pos+1,pos+3);
				char car = (char)(Integer.parseInt(codevalue, 16));
				if (pos==start) result.append(car);
				else result.append(stringToConvert.substring(start,pos)).append(car);
				start = pos+3;
			}
			if (start<stringToConvert.length()) result.append(stringToConvert.substring(start));
			return result.toString();
		}catch (Exception e){
			return null;
		}
	}
//	<------------------------------------------------------------------------------------------>
	/**
	 * Convert UTF-8 encoded string into ascii
	 * @param stringToConvert the UTF8 encoded string
	 * @return ASCII convertion
	 */
	public final static String hexToUTF8(String stringToConvert){
		try{
			return URLDecoder.decode(stringToConvert, "UTF8");
		} catch (Exception e){
			return hexToAscii(stringToConvert);
		}
	}
//	<------------------------------------------------------------------------------------------>    
	/**
	 * Convert ascii String to hex String 
	 * @param stringToConvert the ascii string to convert
	 * @return hex encoded string
	 */
	private final static String asciiToHex(String stringToConvert){
		StringBuilder result=new StringBuilder();
		char[] characters = stringToConvert.toCharArray();
		for (char c:characters){
			if ( (c>47 && c<58) || (c>63 && c<91) || (c>96 && c<123) || c==42 || c==45 || c==46 || c==95){
				result.append(c);
			} else result.append("%").append(Integer.toHexString(c).toUpperCase());
		}
		return result.toString();
	}
	
	/**
	 * Convert given ASCII String to UTF8 
	 * @param stringToConvert ASCII string to convert
	 * @return String converted into UTF8 encoding
	 */
	public final static String utf8ToHex(String stringToConvert){
		try{
			return URLEncoder.encode(stringToConvert, "UTF8");
		} catch (Exception e){
			return asciiToHex(stringToConvert);
		}
	}
//	---------------------------------------------------------------------------


}
