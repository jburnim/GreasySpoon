/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008-2009 Karel Mittig, Marton Balint
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
package tools.general;

////////////////////////////
// Import
import java.io.*;
import java.util.zip.*;
import java.util.regex.*;
////////////////////////////

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Class providing various methods to optimize HTML size.<br><br>
 * All methods can be called statically.
 * @version 1.1
 */

public class Compressor { 

	
	//	<------------------------------------------------------------------------->
	private final static int buffersize = 2048; //buffer size used to uncompress data
	static Pattern multilinecomment = Pattern.compile("(/\\*.*?\\*/)", Pattern.DOTALL);
	static Pattern emptyline = Pattern.compile("[\r\n]+[ \\t\\x0B\\f]*");
	static Pattern WHITESPACE =  Pattern.compile("[ \r\n\t]");
	
	private static ErrorReporter jsReporter = new ErrorReporter() {
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
        	return;
        }
        public void error(String message, String sourceName,int line, String lineSource, int lineOffset) {
        	throw new EvaluatorException(message);
        }
        public EvaluatorException runtimeError(String message, String sourceName,int line, String lineSource, int lineOffset) {
            return new EvaluatorException(message);
        }
    };
	//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Uncompress given data using GZIP algorithm
	 * @param compressedFile ByteArrayOutputStream containing GZIP compressed data
	 * @return a ByteArrayOutputStream containing uncompressed data
	 * @throws Exception
	 */
	public static ExtendedByteArrayOutputStream gunzip(ExtendedByteArrayOutputStream compressedFile) throws Exception {
		// Open the compressed file
		if (compressedFile == null || compressedFile.size() == 0 ) return compressedFile;
		
		ByteArrayInputStream compressed = new ByteArrayInputStream(compressedFile.toByteArray());
		GZIPInputStream in = new GZIPInputStream(compressed);

		// Open the output array
		ExtendedByteArrayOutputStream uncompressed = new ExtendedByteArrayOutputStream();

		// Transfer bytes from the compressed file to the output file
		byte[] buf = new byte[buffersize];
		int len=-1;
		try {
			while ((len = in.read(buf)) > 0) {
				uncompressed.write(buf, 0, len);
			}
		}catch (EOFException e){
			// Corrupted archive
			// FIXME: do we return extracted bytes (kind of fix) or do we return an error ??
			// By only catching the error, we are doing "our best". Uncomment next line to generate an error instead.
			//throw e;
		}
		// Close the file and stream
		in.close();
		return uncompressed;
	}
//	<------------------------------------------------------------------------->
	

//	<------------------------------------------------------------------------->
	/**
	 * Uncompress given data using Deflate algorithm
	 * @param compressedFile ByteArrayOutputStream containing GZIP compressed data
	 * @return a ByteArrayOutputStream containing uncompressed data
	 * @throws Exception
	 */
	public static ExtendedByteArrayOutputStream inflate(ExtendedByteArrayOutputStream compressedFile) throws Exception {
		// Open the compressed file
		if (compressedFile == null || compressedFile.size() == 0 ) return compressedFile;
		
		//set up inflater with nowrap option (ZLIB header and checksum fields will not be used)
		Inflater inflater = new Inflater(true);
		byte dummybyte = 0; // nowrap requires a dummy empty byte to be added at the end of the stream
		compressedFile.write(dummybyte);
		
		ByteArrayInputStream compressed = new ByteArrayInputStream(compressedFile.toByteArray());
		InflaterInputStream in = new InflaterInputStream(compressed,inflater);
		// Open the output array
		ExtendedByteArrayOutputStream uncompressed = new ExtendedByteArrayOutputStream();

		// Transfer bytes from the compressed file to the output file
		byte[] buf = new byte[buffersize];
		int len;
		int mayread = Math.min(in.available(), buffersize);
		while ((len = in.read(buf,0,mayread)) > 0) {
			uncompressed.write(buf, 0, len);
		}
		// Close the file and stream
		in.close();
		return uncompressed;
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * Compress given stream using GZIP algorithm
	 * @param stream the uncompressed stream
	 * @return compressed stream
	 * @throws Exception
	 */
	public static ExtendedByteArrayOutputStream gzip(ExtendedByteArrayOutputStream stream) throws Exception {
		// Compress the bytes
		if (stream == null || stream.size()==0) return stream;
		byte[] content = stream.toByteArray();
		stream.reset();
		GZIPOutputStream compressor = new GZIPOutputStream(stream);
		compressor.write(content);
		compressor.close();
		return stream;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Compress given stream using Inflate algorithm
	 * @param stream the uncompressed stream
	 * @return compressed stream
	 * @throws Exception
	 */
	public static ExtendedByteArrayOutputStream deflate(ExtendedByteArrayOutputStream stream) throws Exception {
		// Compress the bytes
		if (stream == null || stream.size() == 0 ) return stream;
		byte[] content = stream.toByteArray();
		stream.reset();
		DeflaterOutputStream compressor = new DeflaterOutputStream(stream);
		//ZipOutputStream compressor = new ZipOutputStream(stream);
		compressor.write(content);
		compressor.close();
		return stream;
	}
//	<------------------------------------------------------------------------->


/** CONTENT AWARE COMPRESSION METHODS*/

	/**
	 * Parse given HTML code and remove all unnecessary code
	 * @param htmlpage HTML code to parse
	 * @return cleaned code
	 */
	public static String compressHtml(String htmlpage){
		try {
			StringBuilder html = new StringBuilder(htmlpage);
			int h1, h2, s1, s2,cs1,cs2, pos = 0;
			String script = "";
			while (pos<html.length()){
				h1 = html.indexOf("<!--",pos);
				s1 = html.indexOf("<script",pos);
				if (h1==-1 && s1==-1) break;

				if (s1==-1 || (h1!=-1 && h1<s1)){
					cs1 = html.indexOf("<style",pos);
					if (cs1!=-1 && cs1<h1){
						cs2 = html.indexOf("</style>",pos);
						if (cs2!=-1 && cs2>h1){
							//comment is in a style tag => abort
							pos = cs2;
							continue;
						}
					}
					//html comment first
					h2 = html.indexOf("-->",h1);
					if (h2==-1) {
						//invalid html code
						return html.toString();
					}
					if (html.substring(h1,h1+"<!--[if IE".length()).equals("<!--[if IE")){
						//proprietary tag interpreted by IE
						pos = h2+3;
					} else {
						html.delete(h1,h2+3);
						pos = h1+3;
					}
					continue;
				}
				//script first
				s2 = html.indexOf("</script",s1);
				if (s2==-1) pos = s1+7;
				s2 = html.indexOf(">",s2)+1;
				script = html.substring(s1, s2);
				script = compressJavascript(script);
				html.replace(s1, s2, script);
				pos = s1+script.length();
			}

			if (html.indexOf("<textarea")==-1){
				//remove whitespace characters only if there is no textarea
				Matcher matcher;
				matcher = emptyline.matcher(html);
				return matcher.replaceAll("\n");
			} 
			return html.toString();
		} catch (Exception e){
			return htmlpage;
		}
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Remove comments and empty lines from scripts
	 * @param script Script content to clean
	 * @return cleaned script
	 */
	public static String compressJavascript(String script){
		try{
			int length = script.length();
			int firstlinefeed = script.indexOf("\n");
			if (firstlinefeed==-1 || (length>255 && firstlinefeed>255)){
				//script seems to be already minified=> skip
				return script;
			}
			Matcher matcher;
			String[] lines = script.split("[\r\n]");
			StringBuilder sb = new StringBuilder();
			for (String s:lines){
				if (s.length()>500) return script;//too long lines, seems to be also compressed => skip
				s = s.trim();
				if (s.equals("<!--")) continue;
				if (s.contains("//")){
					if (s.startsWith("//")) continue;//comment line
					//we must control that // is not quoted in a string ""
					int rec=-1,pos=0; boolean indoublequote = false; boolean insinglequote = false;
					while (pos < s.length()-1){
						if (s.charAt(pos)=='\\') {//escape char
							pos+=2;continue;
						}
						if (indoublequote && s.charAt(pos)!='\"'){pos++;continue;}
						if (insinglequote && s.charAt(pos)!='\''){pos++;continue;}
						if (s.charAt(pos)=='\"') { indoublequote = !indoublequote;pos++;continue;}
						if (s.charAt(pos)=='\'') { insinglequote = !insinglequote;pos++;continue;}
						if (s.charAt(pos)=='/' && s.charAt(pos+1)=='/'){
							rec = pos;break;//comment founded
						}
						pos++;					
					}
					if (rec!=-1){
						s = s.substring(0,rec);
					}
				}
				sb.append(s).append("\n");//leave a trailing cr to avoid problems
			}
			matcher = multilinecomment.matcher(sb);
			script = matcher.replaceAll("");
			return script;
		} catch (Exception e){
			return script;
		}
	}
//	<------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * Remove comments and empty lines from scripts
	 * @param script Script content to clean
	 * @return cleaned script
	 */
	public static String compressCss(String script){
		try{
			Matcher matcher;
			String[] lines = script.split("[\r\n]");
			StringBuilder sb = new StringBuilder();
			for (String s:lines){
				s = s.trim();
				if (s.equals("<!--")) continue;
				if (s.contains("//")){
					if (s.startsWith("//")) continue;//comment line
					//we must control that // is not quoted in a string ""
					int rec=-1,pos=0; boolean indoublequote = false; boolean insinglequote = false;
					while (pos < s.length()-1){
						if (s.charAt(pos)=='\\') {//escape char
							pos+=2;continue;
						}
						if (indoublequote && s.charAt(pos)!='\"'){pos++;continue;}
						if (insinglequote && s.charAt(pos)!='\''){pos++;continue;}
						if (s.charAt(pos)=='\"') { indoublequote = !indoublequote;pos++;continue;}
						if (s.charAt(pos)=='\'') { insinglequote = !insinglequote;pos++;continue;}
						if (s.charAt(pos)=='/' && s.charAt(pos+1)=='/'){
							rec = pos;break;//comment founded
						}
						pos++;					
					}
					if (rec!=-1){
						s = s.substring(0,rec);
					}
				}
				sb.append(s).append("\n");//leave a trailing cr to avoid problems
			}
			matcher = multilinecomment.matcher(sb);
			script = matcher.replaceAll("");
			return script;
		} catch (Exception e){
			return script;
		}
	}
//	<------------------------------------------------------------------------->
	
	
	/*****************************************************************
	 * Compression methods from Marton Balint to improve compression *
	 *****************************************************************/

//	<------------------------------------------------------------------------------------------>
	/**
	 * Javascript compression method, based on yuiCompressor
	 * @param content uncompressed JavaScript
	 * @return A compressed JavaScript 
	 */
	public static String cleanupJavaScript(String content) {
		int length = content.length();
		int firstlinefeed = content.indexOf("\n");
		if (firstlinefeed==-1 || (length>255 && firstlinefeed>255)){
			//script seems to be already minified=> skip
			return content;
		}
		Reader reader = new StringReader(content);
		Writer writer = new StringWriter();		
		try {
			JavaScriptCompressor jsc = new JavaScriptCompressor(reader,jsReporter);
			jsc.compress(writer, -1, true, false, false, false);
		    //options: (int linebreak, boolean munge, boolean verbose, boolean preserveAllSemiColons, boolean disableOptimizations)
		} catch (Exception e) {
			return content;
		}
		return writer.toString();				
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * CSS compression method, based on YUI Compressor
	 * @param content uncompressed JavaScript
	 * @return A compressed JavaScript 
	 */
	public static String cleanupCSS(String content) {
		Reader reader = new StringReader(content);
		Writer writer = new StringWriter();
		try {
			CssCompressor cssc = new CssCompressor(reader);
			cssc.compress(writer, -1);
		} catch (Exception e) {
			return content;
		}
		return writer.toString();				
	}
//	<------------------------------------------------------------------------------------------>
		

//	<------------------------------------------------------------------------------------------>
	/**
	 * whitespace characters removal from strings 
	 * @param content content to clean
	 * @param last Megadja, hogy a bemenet elejen maradjon-e whitespace
	 * @return A bemenet immaron a whitespace karakterek osszevonasaval
	 */
	private static String cleanupWhitespace(String content, boolean last) {
		String ret = WHITESPACE.matcher(content).replaceAll(" ");
		if (ret.startsWith(" ") && last) ret = ret.replaceFirst(" ", "");
		return ret;
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Megadja, hogy az adott regularis kifejezesre illeszkedo minta melyik pozicioban talalhato.
	 * @param str A karakterlanc, amiben keresni kell.
	 * @param regex A regularis kifejezes.
	 * @param start A kereses kezdopozicioja a karakterlancban.
	 * @return A talalat indexe, -1 ha nincs talalat.
	 */
	private static int indexOfRegexp(String str, String regex, int start) {
		int res = -1;
		Matcher m = Pattern.compile(regex).matcher(str);
		if (m.find(start)) res = m.start();
		return res;
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Megadja, hogy az adott regularis kifejezesre illeszkedo minta melyik pozicioban talalhato.
	 * @param str A karakterlanc, amiben keresni kell.
	 * @param regex A kereses kezdopozicioja a karakterlancban.
	 * @return A talalat indexe, -1 ha nincs talalat.
	 */
	private static int indexOfRegexp(String str, String regex) {
		return indexOfRegexp(str, regex, 0);
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * Egy HTML tag-et egyszerusit, kisbetusit.
	 * @param content A html tag
	 * @return Az egyszerusitett html tag
	 */
	private static String cleanupHTMLTag(String content) {
		if (content.startsWith("<!")) return content;
		int spos = indexOfRegexp(content, "["+WHITESPACE+"]");
		if (spos == -1) {
			return content.toLowerCase();
		} else {
			String tagName = content.substring(0, spos);
			return tagName.toLowerCase()+content.substring(spos);
		}
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * HTML-t tisztit meg a felesleges szokozoktol, whitespacetol, kommentektol.
	 * @param content A megtisztitando HTML
	 * @return A tisztitott HTML
	 */
	public static String cleanupHTML(String content) {
		int pos = 0;
		StringBuilder result = new StringBuilder("");
		int preLevel = 0;
		int scriptLevel = 0;
		int styleLevel = 0;
		boolean last = true;
		String lastappend = "";
		String lasttag = "";
		int telen = 0;
		while (pos < content.length()) {
			int tbpos = content.indexOf("<", pos);
			if (preLevel > 0 || scriptLevel > 0 || styleLevel > 0) {
				result.append(content.substring(pos, tbpos == -1 ? content.length():tbpos));
			} else {
				result.append(lastappend = cleanupWhitespace(content.substring(pos, tbpos == -1 ? content.length():tbpos), last));
				last = !(lastappend.length() > 0 && !lastappend.endsWith(" "));
			}
			if (tbpos != -1) {
				int tepos = content.indexOf(">", tbpos);
				String currenttag = content.substring(tbpos, tepos == -1 ? content.length(): tepos+1);
				if (currenttag.length() >= 4 && currenttag.substring(0, 4).equals("<!--") && scriptLevel == 0 && styleLevel == 0) {
					tepos = content.indexOf("-->", tbpos);
					telen = 3;
				} else if (currenttag.length() >= 7 && currenttag.substring(0, 7).equalsIgnoreCase("<script") && scriptLevel == 0 && styleLevel == 0) {
					telen = 9;
					tepos = indexOfRegexp(content, "\\<\\/[Ss][Cc][Rr][Ii][Pp][Tt]\\>", tbpos);
					if (tepos != -1) {
						result.append(cleanupHTMLTag(currenttag) + cleanupJavaScript(content.substring(tbpos+currenttag.length(), tepos)) +  "</script>");
					} else {
						result.append(content.substring(tbpos, tepos == -1 ? content.length(): tepos+telen));
					}
				} else if (currenttag.length() >= 6 && currenttag.substring(0, 6).equalsIgnoreCase("<style") && scriptLevel == 0 && styleLevel == 0) {
					telen = 8;
					tepos = indexOfRegexp(content, "\\<\\/[Ss][Tt][Yy][Ll][Ee]\\>", tbpos);
					if (tepos != -1) {
						result.append(cleanupHTMLTag(currenttag) + cleanupCSS(content.substring(tbpos+currenttag.length(), tepos)) +  "</style>");
					} else {
						result.append(content.substring(tbpos, tepos == -1 ? content.length(): tepos+telen));
					}
				} else {
					telen = 1;
					result.append(lasttag = cleanupHTMLTag(currenttag));
					if (lasttag.startsWith("<script")) scriptLevel++;
					if (lasttag.startsWith("</script") && scriptLevel > 0) scriptLevel--;
					if (lasttag.startsWith("<style")) styleLevel++;
					if (lasttag.startsWith("</style") && styleLevel > 0) styleLevel--;
					if (lasttag.startsWith("<pre")) preLevel++;
					if (lasttag.startsWith("</pre") && preLevel > 0) styleLevel--;
				}
				if (tepos != -1) {
					pos = tepos+telen;
				} else {
					pos = content.length();
				}
			} else {
				pos = content.length();
			}
		}
		return result.toString();
	}
//	<------------------------------------------------------------------------------------------>

	
	
}
