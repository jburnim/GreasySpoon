/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008 Karel Mittig
 *-----------------------------------------------------------------------------
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *-----------------------------------------------------------------------------
 * For any comment, question, suggestion, bugfix or code contribution please
 * contact Karel Mittig : karel [dot] mittig [at] gmail [dot] com
 * Created on 14 June 2008
 *-----------------------------------------------------------------------------*/
package tools.general;

////////////////////////////////
//Import
import javax.activation.MimetypesFileTypeMap;
import java.nio.charset.Charset;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.util.regex.*;
////////////////////////////////

/**
 * Class trying to extract MIME type and encoding for given content<br />
 * Provides a simple implementation of MimeMagic function based on <i>magic.mime</i> 
 * used by UNIX <i>file</i> command and <i>Apache</i> MimeMagic module<br />
 * TODO: support all operators (only EQUALS supported for now)
 * TODO: implements ENCODING if provided in magic.mime file
 * @author Karel MITTIG
 */
public class MimeMagic {

	/** switch to debug (development only)*/
	//private final static int debug = 0;
	
	/**MimetypesFileTypeMap used to parse/retrieve MIME from file extension (lazy implementation)*/
	static MimetypesFileTypeMap mimeinstance = new MimetypesFileTypeMap();

	
	/** simple charset string length (optim) */
	private final static int CHARSETSIZE = "charset=".length();
	
	/** Default MIME type to return if none can be founded */
	public final static String UNKNOWN = "application/octet-stream";
	
	/** Hash table containing charset name associated to system charset */
	public static ConcurrentHashMap<String, String> availableCharsets;
	

	/** Table containing MAGIC matchers rules */
	static MimeMagicMatcher[] magicmatchers = new MimeMagicMatcher[0];
	
	/** Number of bytes to use for file recognition */
	private static int magicbytessize = 32; 
	
	/** Path to magic.mime and mime.types file */
	private static String confpath = "./conf/";
	
    /**
     * MIME File used to fetch MIME types associated to file extensions
     * Format must be similar to Apache mime.types file
     */
    private static String _mimefile = confpath+"mime.types";

    /**
     * MIME Magic File used to identify MIME types using magic bytes
     * Format must be similar to Unix magic.mime file (see file command under Unix)
     */
    private static String _mimemagicfile = confpath+"magic.mime";
    
    
    /** MAGIC matchers types */
    public static enum MIMEMAGICTYPE {
    	/**byte*/BYTE,/**short*/SHORT,/**long*/LONG,/**String*/STRING,/**date*/DATE,
    	/**short (Big endian)*/BESHORT, /**long (Big endian)*/BELONG, /**date (Big endian)*/BEDATE,
    	/**short (Little endian)*/LESHORT,/**long (Little endian)*/LELONG, /**date (Little endian)*/LEDATE
   	}

    //On instantiation, load magic.mime and mime.types files
	static {reload();}

	
//	<------------------------------------------------------------------------->
	/**
	 * load magic.mime and mime.types files
	 */
	public static void reload(){
		if (availableCharsets!=null) availableCharsets.clear();
		availableCharsets = fetchAvailableCharsets();
		try{
			loadMimeMagicFile();
			mimeinstance = new MimetypesFileTypeMap(_mimefile);
		} catch (Exception e){
			//e.printStackTrace();
			mimeinstance = new MimetypesFileTypeMap();
		}
	}
//	<------------------------------------------------------------------------->
	
	
//	<------------------------------------------------------------------------->	
	/**
	 * Build up a list of all system available charsets 
	 * @return hash table containing charsets names or aliases, associated to system charset 
	 */
	private static ConcurrentHashMap<String, String> fetchAvailableCharsets(){
		ConcurrentHashMap<String, String> charsetlist = new ConcurrentHashMap<String, String>();
	    SortedMap<String, Charset> charsets = Charset.availableCharsets();
	    Set<String> names = charsets.keySet();
	    //if (debug>2) System.out.println("Available Charsets:");
	    for (Iterator<String> e = names.iterator(); e.hasNext();) {
	      String name = e.next().toLowerCase().trim();
	      Charset charset = (Charset) charsets.get(name);
	      if (name==null || charset== null) continue;
	      name = name.trim();
	      //if (debug>2) System.out.println(charset);
	      charsetlist.put(name, name);
	      Set<String> aliases = charset.aliases();
	      
	      for (Iterator<String> ee = aliases.iterator(); ee.hasNext();) {
	    	  try{ 
	    		  String charsetname = ee.next();
	    		  if (charsetname==null || charsetname.equals("null")) continue;
	    		  charsetlist.put(charsetname.toLowerCase().trim(), name);
	    		  //if (debug>2) System.out.println("    " + charsetname);
	    	  } catch (Exception e1){
	    		  //if (debug>2) e1.printStackTrace();
	    	  }
	      }
	    }
	    return charsetlist;
	  }
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Retrieve MIME type using file extension  (requires at least ".xxx")
	 * @param filename the File name to analyze
	 * @return extension associated MIME type, or "application/octet-stream" if not founded
	 */
	public static String getMimeTypeByExtension(String filename){
		return mimeinstance.getContentType(filename);
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Try to find Charset encoding for given MIME header (Content-Type header)
	 * @param contentTypeValue HTTP Content-Type value
	 * @return founded encoding associated to charset provided in header, or null if none
	 */
	public static String getCharsetFromContentType(String contentTypeValue){
		try{
			String ct = contentTypeValue.toLowerCase();
			String encoding=null;
			if (ct!=null && ct.contains("charset")){ 
				int i1 = ct.indexOf("charset=")+CHARSETSIZE; 
				int i2 = ct.indexOf(";",i1); 
				if (i2==-1) {
					encoding = ct.substring(i1).trim();
				} else {
					encoding = ct.substring(i1,i2).trim();
				}
				//CHARSET provided. Let's check it to see if it is valid (valid means here available)
				return availableCharsets.get(encoding);
			}
		} catch (Exception e){
			//no CHARSET founded in HTTP response
		}
		return null;
	}
//	<------------------------------------------------------------------------->
	
//	<------------------------------------------------------------------------->
	/**
	 * Retrieve System Charset associated to given charset name
	 * @param charsetname The charset name to retrieve
	 * @return System Charset value associated to given charset name, or null if unavailable
	 */
	public static String getCharset(String charsetname){
		if (charsetname==null) return null;
		return availableCharsets.get(charsetname.toLowerCase());
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Extract charset encoding from HTML/XHTML file<br>
	 * Search for meta tag in head defining content charset
	 * @param content the (X)HTML content
	 * @return encoding in head if defined and valid, or UTF-8 for HTML v4.x and above, or ISO-8859-1 for HTML <4.x
	 */
	public static String extractHtmlEncoding(String content){
		if (content == null) return null;
		String announcecharset = null;
		
		try{// First search for charset meta tag in the head
			int headpos = content.indexOf("</head>");
			if (headpos!=-1) {
				String head = content.substring(0, headpos);
				int charsetpos = -1;
				//content type founded
				if ( (charsetpos = head.indexOf("http-equiv=\"content-type\""))!=-1) {
					int charsetend = head.indexOf(">",charsetpos);
					charsetpos = head.indexOf("charset=",charsetpos);
					if (charsetend!=-1 && charsetpos<charsetend)  charsetend = head.indexOf("\"",charsetpos);
					if (charsetpos!=-1 && charsetend!=-1 && charsetpos<charsetend) {
						//charset provided
						announcecharset = getCharset(head.substring(charsetpos+CHARSETSIZE,charsetend).trim());
						if (announcecharset!=null) return announcecharset;
					}
				}
			}
		}catch (Exception e){
			//Error parsing HTML response
		}
		return null;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Extract charset encoding from XML content
	 * @param content the XML content to analyze
	 * @return encoding defined in XML definition, or UTF-8 as default
	 */
	public static String extractXmlEncoding(String content){
		if (content == null) return null;
		String announcecharset = null;
		//<?xml version="1.0" encoding="encoding to search"?>
		try{// search for encoding in XML header
			int headpos = content.indexOf("?>");
			if (headpos!=-1) {
				String head = content.substring(0, headpos);
				int charsetpos = -1;
				//encoding founded => extract it
				if ( (charsetpos = head.indexOf("encoding=\""))!=-1) {
					charsetpos = head.indexOf("\"", charsetpos)+1;
					int charsetend = head.indexOf("\"",charsetpos);
					if (charsetpos!=-1 && charsetend!=-1 && charsetpos<charsetend) {
						//charset provided
						announcecharset = getCharset(head.substring(charsetpos,charsetend).trim());
						if (announcecharset!=null) return announcecharset;
					}
				}
			}
		}catch (Exception e){
			//Error parsing XML response
		}
		return null;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Detect charset encoding using HTTP Content type and content
	 * @param contenttype HTTP Content type 
	 * @param content Request/Response body
	 * @return Detected charset, or ISO-8859-1 as RFC 2616 default encoding
	 */
	public static String detectCharset(String contenttype, String content){
		String ct = content.toLowerCase().trim();
		String httpencoding = getCharsetFromContentType(contenttype);
		String contentencoding = null;
		
		String doctype = null;
		int idoctype = 0;//0 for 
		if (ct.startsWith("<!doctype")){
			int doctypepos = ct.indexOf(">");
			if (doctypepos!=-1 ) {
				doctype = ct.substring(0,ct.indexOf(">"));
				if (doctype.contains("xhtml 1")) {
					idoctype=3;
					contentencoding = extractXmlEncoding(ct);
					if (contentencoding != null) return contentencoding;//XML declaration takes precedence
				} else if (doctype.contains("html 4")) idoctype=2;//html 4.x
				else if (doctype.contains("html")) idoctype=1;//html older than 4.x
			}
		} else if (ct.startsWith("<?xml")) idoctype=4;
		
		
		if (contenttype.contains("xml") || idoctype==4){
			contentencoding = extractXmlEncoding(ct);
			//System.err.println("XML: "+contentencoding);
		}
		if ((contenttype.contains("htm") && idoctype<3) || (idoctype>2 && contentencoding==null)){
			contentencoding = extractHtmlEncoding(ct);
			//System.err.println("HTM: "+contentencoding);
			if (contentencoding == null && httpencoding==null){
				if (idoctype==1) return "us-ascii";
				if (idoctype==2) return "ISO-8859-1";
			}
		}
		//if (debug==1) System.err.println("Mimemagic:encoding 1:"+contentencoding);
		if (contentencoding!=null) return contentencoding;
		//if (debug==1) System.err.println("Mimemagic:encoding 2:"+httpencoding);
		if (httpencoding != null) return httpencoding;//HTTP Content Type charset
		if (idoctype==4) return "UTF-8";//XML default encoding
		//if (debug==1) System.err.println("Mimemagic:encoding def:"+"ISO-8859-1");
		return "ISO-8859-1";//RFC 2616 default encoding
	}
//	<------------------------------------------------------------------------->
	
//  <--------------------------------------------------------------------------->
    /**
     * Parse magic.mime file
     * Only 2 levels of filters are supported for now (>> tag)
     */
    private static void loadMimeMagicFile(){
        File fich = new File(MimeMagic._mimemagicfile);
        if (!fich.exists()){
        	System.err.println("Cannot find magic.mime file <"+fich.toString()+">");
            return;
        }// Endif
        
        try {       
            BufferedReader in = new BufferedReader(new FileReader(fich));
            String str;
            //Read a line
            MimeMagicMatcher matcher = null;
            MimeMagicMatcher submatcher= null;
            String s_offset, type, s_value, s_mime, s_encoding;
            int rule_level = 0;
            
            //use a temporary vector for magic.mime loading
            Vector<MimeMagicMatcher> _magicmatchers = new Vector<MimeMagicMatcher>();
            
            while ((str = in.readLine()) !=null ){
                if (str.startsWith("#") || str.startsWith(";")) continue;//comments
                //replace blanks [\ ] by octal value (\040)
                str = str.replace("\\ ", "\\040");
                //Split parameters based on spaces and tabs 
                String[] values = str.split("[\\s\\t]+");
                
                //check if we have at least 3 values (offset, type, value). If not, drop the definition
                if (values == null || values.length <3) continue;
                
                rule_level = 0; s_mime = null; s_encoding = null; //reset values
                int offset;
                try{
	                //parse offset value
	                if (values[0].startsWith(">>")){ //level 2 matcher
	                	if (submatcher == null) continue; //Houston, everything's ok ?
	                	rule_level = 2;
	                	s_offset = values[0].substring(1).trim();
	                } else if (values[0].startsWith(">")){ //level 1 matcher
	                	if (matcher == null) continue; //Houston, we've got a problem => just forget about it
	                	rule_level = 1;
	                	s_offset = values[0].substring(1).trim();
	                } else {//level 0 matcher
	                	s_offset = values[0].trim();
	                }
	                //done, read offset value for real
                	offset = Integer.parseInt(s_offset);
                } catch (Exception e){
                	continue;
                }
            	
                //read mimemagic TYPE definition
           		type = values[1].trim(); // type of value to look for
           		s_value = values[2].trim(); //value to look for
            	if (values.length>3) s_mime = values[3].trim(); //associated mime type
            	if (values.length>4) s_encoding = values[4].trim();//associated encoding
            	
        		try{
        			switch (rule_level){
	        			case 1:
	        				submatcher = new MimeMagicMatcher(offset,type, s_value,s_mime,s_encoding);
		            		if (submatcher!=null && matcher!=null ) matcher.addSubMatcher(submatcher);
		            		break;
	        			case 2:
	        				if (submatcher!=null) submatcher.addSubMatcher(new MimeMagicMatcher(offset,type, s_value,s_mime,s_encoding));
	        				break;
	        			default:
	        				matcher = new MimeMagicMatcher(offset,type, s_value,s_mime,s_encoding);
        			}
        		} catch (Exception e){
        			//if (debug>=1) System.err.println("Unsupported definition:"+str);
        			matcher = null;
        			continue;
        		}
        		_magicmatchers.add(matcher);
            }//End while readLine
            // Convert vector to array for performances purpose
            magicmatchers = _magicmatchers.toArray(new MimeMagicMatcher[_magicmatchers.size()]);
            in.close();   
        } catch (IOException e){
        	//if (debug>=1) System.err.println("Error in MIME file <"+fich.toString()+">.\r\n["+e.toString()+"]\r\nFile corrupted.");
            return;
        }//End try&catch
        //if (debug>0) System.out.println("Magic mime file <"+fich.toString()+"> loaded");
    }
//  <--------------------------------------------------------------------------->
	
//  <--------------------------------------------------------------------------->
/**
 *  MimeMagic Rule 
 */
public static class MimeMagicMatcher{
	private Vector<MimeMagicMatcher> subMatchers = new Vector<MimeMagicMatcher>();
	private Number matchervalue;
	private Number mask = null;
	private String	m_str;//string
	private int offset;
	private MIMEMAGICTYPE matchertype;
	private String matchermimetype;
	private boolean caseInsensitive = false;
	private boolean spaces = false;
	private Pattern patternmatcher;
	
	//TODO: implements ENCODING and OPERATOR 
	/**Use encoding provided in MimeMagic rule - to implement*/
	@SuppressWarnings("unused")
	private String matcherencoding;	
	/**Set OPERATOR to used when processing MimeMagic rule - to implement*/
	@SuppressWarnings("unused")
	private OPERATOR operator = OPERATOR.EQUALS;
	
    /**List of possible operators used in MimeMagic file*/
    public static enum OPERATOR {/**EQUALS*/EQUALS,/**LOWER*/LOWER,/**HIGHER*/HIGHER, /**NOT EQUALS*/NOT_EQUALS}
	//	<------------------------------------------------------------------------->
	/**
	 * Create a MIME Magic matcher based on magic.mime definition line (see http://acm.ist.utl.pt/cgi-bin/man/man2html?5+magic)
	 * @param offset byte position where to start matching condition
	 * @param type Type of matcher (numerical or String for now)
	 * @param value Value to look after
	 * @param mimetype	MIME type associated to this matcher
	 * @param encoding Default encoding associated to this MIME type
	 * @throws Exception 
	 */
	public MimeMagicMatcher(int offset, String type, String value, String mimetype, String encoding) throws Exception{
		//see http://acm.ist.utl.pt/cgi-bin/man/man2html?5+magic
		
		//Extract operator
        char c = value.charAt(0);
        if (c=='=' || c=='<' || c=='>' ){
        	//operator char
        	switch (c){
        		case '<':this.operator = OPERATOR.LOWER;break;
        		case '>':this.operator = OPERATOR.HIGHER;break;
        		case '!':this.operator = OPERATOR.NOT_EQUALS;break;
        		default: this.operator = OPERATOR.EQUALS;
        	}
        	value = value.substring(1);
        }
		
        //Parse string values
		if (type.toUpperCase().startsWith("STRING")){
			//pre-process string
			if (value.indexOf("\\")!=-1) value = parseStringValues(value);
			//Remove all leaving escaped char
			value = value.replace("\\", "");

			String m_regex = "(?s)";//consider whole string (does not interrupt on '\n')
			if (type.contains("c")) caseInsensitive = true;
			if (type.contains("B") && value.contains(" ")) {//Requires one or more spaces
				spaces = true;
				//m_regex = "\\s*\\t*" + m_regex;
				m_regex += "\\s*\\t*";
				String[] elems = value.split("[\\s\\t]+");
				for (String s:elems){
					m_regex += "(?:"+(caseInsensitive?s.toLowerCase():s)+")\\s+";
				}
				m_regex +=".*";
				patternmatcher = Pattern.compile(m_regex);
			}
			if (type.contains("b") && value.contains(" ")) { //Optional spaces are accepted
				spaces = true;
				m_regex = "\\s*\\t*" + m_regex;
				String[] elems = value.split("[\\s\\t]+");
				for (String s:elems){
					m_regex += "(?:"+(caseInsensitive?s.toLowerCase():s)+")\\s*";
				}
				m_regex +=".*";
				patternmatcher = Pattern.compile(m_regex);
			}
			type = "STRING";
		} else { // Other numerical TYPE
			type = type.toUpperCase();
		}

		//Extract byte mask for numerical values
		String s_mask = null;
		if ( type.indexOf('&')!=-1){
			s_mask = type.substring(type.indexOf('&')+1);
			type = type.substring(0,type.indexOf('&'));
		}
		
		matchertype = MIMEMAGICTYPE.valueOf(type);
		matchermimetype = mimetype;
		matcherencoding = encoding;
		this.offset = offset;
		
		//Java always use Big endian => convert all to Big
		switch (matchertype){
			case BYTE:
					matchervalue = Short.decode(value);
					if (s_mask!=null) mask = Short.decode(s_mask);
					break;
			case BESHORT:
			case SHORT: 
					matchervalue = Integer.decode(value);
					if (s_mask!=null) mask = Integer.decode(s_mask);
					matchertype = MIMEMAGICTYPE.SHORT;
					break;
			case LESHORT: 
					matchervalue = Integer.reverseBytes(Integer.decode(value));
					if (s_mask!=null) mask = Integer.decode(s_mask);//Integer.reverseBytes(Integer.decode(s_mask));
					matchertype = MIMEMAGICTYPE.SHORT;
					break; 						
			case BELONG: 
			case LONG:	
				matchervalue = Long.decode(value);
				if (s_mask!=null) mask =  Long.decode(s_mask);
				matchertype = MIMEMAGICTYPE.LONG;
				break;
			case LELONG: 	
				matchervalue = Long.reverseBytes(Long.decode(value));
				if (s_mask!=null) mask =  Long.decode(s_mask);//Long.reverseBytes(Long.decode(s_mask));
				matchertype = MIMEMAGICTYPE.LONG;
				break;
						
			case LEDATE:
			case BEDATE:
			case DATE:  
				matchervalue = Long.decode(value);
				if (s_mask!=null) mask =  Long.decode(s_mask);
				matchertype = MIMEMAGICTYPE.DATE;
				break;
			case STRING:
			default:	
				matchertype = MIMEMAGICTYPE.STRING;
				m_str = value;
				if (caseInsensitive) m_str = m_str.toLowerCase();
		}//End switch
		
		//if (debug>2)System.out.println(this.toString());
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	public String toString(){
		String str = "["+this.offset+"]["+this.matchertype.toString()+"][";
		if (this.matchertype == MIMEMAGICTYPE.STRING){
			if (this.caseInsensitive) str+="i][";
			if (this.spaces) {
				str+= "bB,"+patternmatcher.pattern()+"][";
			} else {
				str+= m_str+"]";
			}
		} else {
			if (mask!=null) str+=mask+"][";
			str+= matchervalue+"]";
		}
		if (this.subMatchers.size()>0) {
			for (MimeMagicMatcher mm:subMatchers){
				str+= "   " + mm.toString();
			}
		}
		return str;
	}
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	/**
	 * String value parser. Do its best to parse encoded chars
	 * @param s String to parse
	 * @return Java encoded string
	 */
	private static String parseStringValues(String s) {
        int seekpos = 0;
        char c;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        String res="";
        String parse="";
        boolean startconvert = false;
        int charvalue = 0;
        
        for (int i=0; i<s.length();i++){
        	c = s.charAt(i);
        	if (startconvert) seekpos++; //seek position
        	
        	if (c=='\\' && !startconvert){//escape starting
        		startconvert = true;
        		seekpos = 0;//init seek position
        		parse = "";
        		continue;
        	}
        	if (!startconvert){//unescaped char
        		res += c;continue;
        	}
        	
        	if (startconvert && seekpos==1 && !Character.isLetterOrDigit(c)){//escaped char
       			res += c;
       			//buf.write(c);
       			startconvert = false;
       			continue;
        	}
        	if (c!='\\') parse +=c;
        	if (seekpos==3 || c=='\\'){//max char encoding sequence (3 numbers) or new escape
        		try{
	        		if (parse.charAt(0)=='x'){//hexadecimal value
	        			parse = parse.substring(1);
	        			charvalue = Integer.parseInt(parse,16);
	        			buf.write((char) charvalue); // horrible stuff
	        			res += buf.toString();
	        			buf.reset();
	        		} else {
	        			charvalue = Integer.parseInt(parse,8);
	        			res += (char)charvalue;
	        		}
        		} catch (Exception e){//failure parsing value: consider it as escaped char
        			res += '\\'+parse;
        		}
    			seekpos = 0;
        		if (c!='\\'){
        			startconvert = false;
        		} else {
            		parse = "";
        		}
        	}
        }
        if (seekpos!=0 && !parse.equals("")){
        	//unparsed content
    		try{
        		if (parse.charAt(0)=='x'){//hexadecimal value
        			parse = parse.substring(1);
        			charvalue = Integer.parseInt(parse,16);
        			buf.write((char) charvalue); // horrible stuff
        			res += buf.toString();
        			buf.reset();
        		} else {
        			charvalue = Integer.parseInt(parse,8);
        			res += (char)charvalue;
        		}
    		} catch (Exception e){//failure parsing value: consider it as escaped char
    			res += '\\'+parse;
    		}
        }
        //Replace special chars
        res = res.replace("\\0", ""+'\u0000');
        res = res.replace("\\r", ""+'\r');
        res = res.replace("\\n", ""+'\n');
        res = res.replace("\\n", ""+'\n');
        //if (debug>1) System.err.println(res);
        return res;
    }
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	/**
	 * Add a sub condition to this matcher
	 * @param submatcher
	 */
	public void addSubMatcher(MimeMagicMatcher submatcher){
		this.subMatchers.add(submatcher);
	}
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	/**
	 * @return MIME type associated to this matcher
	 */
	public String getMimeType(){
		//if (matchermimetype==null && subMatcher!=null) return subMatcher.getMimeType();
		return matchermimetype;
	}
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	/**
	 * Check if this filter matches given byte array
	 * @param filebyte The file first bytes to parse
	 * @return MIME type of provided bytes if filter (and subfilters) matches, null otherwise
	 */
	public String match(byte[] filebyte){
		boolean match = false; 
		if (offset>(filebyte.length-2)) return null;
		try{
			switch (this.matchertype){
				case BYTE:
					short ns = (short)(0xFF & filebyte[offset]);
					if (mask!=null) {
						if ((ns & mask.shortValue()) == matchervalue.shortValue()){
							match = true;
						} 
					} else if (matchervalue.shortValue() == ns){
						match = true;
					} 
					break;
				case SHORT:
					int ni = unsignedShort(filebyte,offset);
					if (mask!=null) {
						if ((ni & mask.intValue()) == matchervalue.intValue()){
							match = true;
						} 
					}if (matchervalue.intValue() == ni){
						match = true;
					}
					break;
				case LONG:
				case DATE:
					long nl = unsignedInt(filebyte,offset);
					if (mask!=null) {
						if ((nl & mask.longValue()) == matchervalue.longValue()){
							match = true;
						} 
					} if (matchervalue.longValue() == nl){
						match = true;
					} 
					break;
				default:
	
					if (!spaces) {
						if ( (filebyte.length - offset - m_str.length()) <1) return null;
						String str = null;
						try{
							str = new String(filebyte,offset,m_str.length());
						} catch (Exception e){
							//if (debug>=2) e.printStackTrace();
							return null;
						}
						if (caseInsensitive) match = str.equalsIgnoreCase(m_str);
						else match = str.equals(m_str);
						//if (debug>=3) System.err.println("search ["+m_str+"]["+str+"]");
						break;
					}
					String str = new String(filebyte,offset,filebyte.length-offset);
					if (caseInsensitive) str = str.toLowerCase();
					Matcher m = patternmatcher.matcher(str);
					match = m.matches();
			}// End switch
			if (match) {
				//if (debug>0) System.err.println("match ["+mask+"]["+matchervalue+"]["+m_str+"]");
				if (this.subMatchers.size()>0) {
					for (MimeMagicMatcher mm:subMatchers){
						if (mm.match(filebyte)!=null) return mm.matchermimetype;
					}
				}
				return this.matchermimetype;
			}
		}catch (Exception e){}
		return null;
	}
	//	<------------------------------------------------------------------------->

	//	<------------------------------------------------------------------------->
	/**
	 * Converts a 4 byte array of unsigned bytes to an long
	 * @param b an array containing the 4 unsigned bytes to parse
	 * @param offset the position at which the long coded on 4 unsigned bytes starts
	 * @return a long representing the unsigned INT
	 */
	public static final long unsignedInt(byte[] b, int offset) {
	    long l = 0;
	    l |= b[offset] & 0xFF;
	    l <<= 8;
	    l |= b[offset+1] & 0xFF;
	    l <<= 8;
	    l |= b[offset+2] & 0xFF;
	    l <<= 8;
	    l |= b[offset+3] & 0xFF;
	    return l;
	}
	//	<------------------------------------------------------------------------->
	
	//	<------------------------------------------------------------------------->
	/**
	 * Converts a two byte array to an integer
	 * @param b a byte array of length 2
	 * @param offset the position in the byte array at which the unsigned short starts
	 * @return an integer representing the unsigned short
	 */
	public static final int unsignedShort(byte[] b, int offset) {
	    int i = 0;
	    i |= b[offset] & 0xFF;
	    i <<= 8;
	    i |= b[offset+1] & 0xFF;
	    return i;
	}
	//	<------------------------------------------------------------------------->
}	// End of subclass MagicMatcher
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Try to determine MIME type of given byte array using magic
 * @param data the first file bytes to parse
 * @return String containing MIME type, or null if none
 */
public static String mimeMagic(byte[] data){
	for (MimeMagicMatcher m:magicmatchers){
		String mime = m.match(data);
		if (mime!=null){
			return mime;
		}
		//if (m.match(data)){return m.getMimeType();}
	}
	return null;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Extract <i>magicbytessize</i> from given byte stream and do magic on it
 * @param data byte stream
 * @return String containing MIME type, or null if none
 */
public static String mimeMagic(ExtendedByteArrayOutputStream data){
	int cplength = data.size()<magicbytessize?data.size():magicbytessize;
	byte[] dt = data.getBytes(cplength);
	try{
		return mimeMagic(dt);
	}catch (Exception e){
		//e.printStackTrace();
		//System.err.println(b_data.length + " / " + dt.length + " / "+cplength);
	}
	return null;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Detect MIME type using MimeMagic, or file extension if available
 * If nothing is founded, trust content type provided in response 
 * @param contenttype HTTP content type header
 * @param url requested URL
 * @param data response data
 * @param trustServerPerDefault Trust server for unknown types or not
 * @return founded MIME type, or value initially provided in contenttype if no content type can be determined
 */
public static String detectMime(String contenttype, String url, ExtendedByteArrayOutputStream data, boolean trustServerPerDefault){
	String mimemgc = mimeMagic(data);
	if (mimemgc==null){
		int pathend = url.lastIndexOf("/");
		if (pathend==-1) return contenttype;
		String extension = url.substring(pathend);
		if (extension.indexOf(".") == -1 ) return contenttype;
		mimemgc = getMimeTypeByExtension(extension);
	}
	if (mimemgc != null) return mimemgc;
	if (trustServerPerDefault) return contenttype;
	return UNKNOWN;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Detect MIME type using MimeMagic, or file extension if available
 * If nothing is founded, trust content type provided in response 
 * @param contenttype HTTP content type header
 * @param url requested URL
 * @param data response data
 * @param trustServerPerDefault Trust server for unknown types or not
 * @return founded MIME type, or value initially provided in contenttype if no content type can be determined
 */
public static String detectMime(String contenttype, String url, ByteBuffer data, boolean trustServerPerDefault){
	String mimemgc = mimeMagic(data.array());
	if (mimemgc==null){
		int pathend = url.lastIndexOf("/");
		if (pathend==-1) return contenttype;
		String extension = url.substring(pathend);
		if (extension.indexOf(".") == -1 ) return contenttype;
		mimemgc = getMimeTypeByExtension(extension);
	}
	if (mimemgc != null) return mimemgc;
	if (trustServerPerDefault) return contenttype;
	return UNKNOWN;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Detect MIME type using MimeMagic then file extension
 * @param url requested URL
 * @param data Byte stream containing request/response data
 * @return founded MIME type, or "application/octet-stream" if it cannot be determined
 */
public static String detectMime(String url, ExtendedByteArrayOutputStream data){
	String mimemgc = mimeMagic(data);
	if (mimemgc==null){
		mimemgc = getMimeTypeByExtension(url);
	}
	if (mimemgc != null) return mimemgc;
	return UNKNOWN;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Detect MIME type using MimeMagic then file extension
 * @param url requested URL or file name
 * @param data byte array containing content to analyze
 * @return founded MIME type, or "application/octet-stream" if it cannot be determined
 */

public static String detectMime(String url, byte[] data){
	String mimemgc = mimeMagic(data);
	if (mimemgc==null){
		mimemgc = getMimeTypeByExtension(url);
	}
	if (mimemgc != null) return mimemgc;
	return UNKNOWN;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Read magicbytessize bytes from given file
 * @param filename The file to parse
 * @return byte array containing magicbytessize bytes
 * @throws Exception 
 */
private static byte[] readFile(String filename) throws Exception {
	byte[] data = new byte[magicbytessize];
	File fich = new File(filename);
	BufferedInputStream in = new BufferedInputStream(new FileInputStream(fich));		
	in.read(data);
	in.close();
	return data;
}
//<------------------------------------------------------------------------->

//<------------------------------------------------------------------------->
/**
 * Try to determine MIME type of given file using magic
 * @param file The file to analyze
 * @return MIME type founded, or null if none 
 */
public static String mimeMagic(String file){
	try{
		byte[] data = readFile(file);
		return mimeMagic(data);
	} catch (Exception e){
		return null;
	}
}
//<------------------------------------------------------------------------->




	/**
	 * Return number of bytes used to computes magic
	 * @return number of bytes
	 */
	public static int getMagicBytesSize() {
		return magicbytessize;
	}

	/**
	 * Set number of bytes to use for magic detection
	 * Lowest value is 8, higher is 4096 (default: 128)
	 * @param magicbytessize The bytes to parse for magic 
	 */
	public static void setMagicBytesSize(int magicbytessize) {
		if (magicbytessize<8) magicbytessize = 8;
		if (magicbytessize>4096) magicbytessize = 4096;
		MimeMagic.magicbytessize = magicbytessize;
	}


	/**
	 * Return <i>mime.types</i> file currently in use
	 * @return Current file containing MIME types associated to files extensions 
	 */
	public static String getMimeTypesFile() {
		return _mimefile;
	}

	/**
	 * Set <i>mime.types</i> file to use for MIME detection based on file extension
	 * @param _mimefile file containing MIME types associated to files extensions (Apache server syntax)
	 */
	public static void setMimeTypesFile(String _mimefile) {
		MimeMagic._mimefile = _mimefile;
		reload();
	}

	/**
	 * Return <i>magic.mime</i> file currently in use 
	 * @return Current file containing magic rules and associated MIME types
	 */
	public static String getMimeMagicFile() {
		return _mimemagicfile;
	}

	/**
	 * Set <i>magic.mime</i> file to use for MimeMagic function
	 * @param _mimemagicfile file containing magic rules and associated mime types (UNIX <i>file</i> magic.mime syntax)
	 */
	public static void setMimeMagicFile(String _mimemagicfile) {
		MimeMagic._mimemagicfile = _mimemagicfile;
		reload();
	}
	
	
}

