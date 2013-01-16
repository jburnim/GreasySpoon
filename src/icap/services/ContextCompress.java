/**----------------------------------------------------------------------------
 * ContextCompress
 * Copyright (C) 2009 Marton Balint
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
 * contact Marton Balint cus at fazekas dot hu
 *---------------------------------------------------------------------------*/
package icap.services;

/* Importalando csomagok */
import icap.IcapServer;
import icap.core.*;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.*;

import tools.logger.Log;
//import magick.*;

/**
 * <p>
 * ContextCompress szolgaltatas a GreasySpoon ICAP szerverhez.<br>
 * <p>
 * Kepes tomoriteni a HTML/CSS/JavaScript kodot a felhasznaloi ugynok es a kliens hosztneve alapjan.<br>
 * A szolgaltatas megvalositja a HTTP-ben hasznlat tomoritesi eljarasokat is, sot kepes a tomoritve kapott tartalmat
 * kitomoriteni, optimalizalni, majd ujra betomoriteni.<br>
 * Minden valasz tomoritesre kerul, ha a kliens tamogatja (Accept-encoding: gzip).<br>  
 * Az ICAP Protocol metodusokat az AbstractService osztaly szolgaltatja.<br>
 * @author cus
 */
public class ContextCompress extends AbstractService {
	
	/* A szolgaltatas hosszu neve, fokent loggolashoz */
	static String servicename = ServicesProperties.getString("ContextCompress.servicename"); 
	
	/* Loggolashoz */
	StringBuilder logstr = new StringBuilder();
	
	/* Megtoretent-e mar az inicializalas */
	private static boolean initialized = false;
			
	/* Tamogatott mime tipusok */
	private static String[] confSupportedContentTypes;
	
	/* GZIP/Deflate tomoritheto mime tipusok */
	private static String[] confCompressibleContentTypes;
	
	/* HTML tomoritheto mime tipusok */
	private static String[] confHtmlContentTypes;
	
	/* Kepkent tomoritheto mime tipusok */
	private static String[] confImageContentTypes;
	
	/* Stiluslapkent tomoritheto mime tipusok */
	private static String[] confCssContentTypes;
	
	/* JavaScriptkent tomoritheto mime tipusok */
	private static String[] confJavaScriptContentTypes;
	
	/* A felhasznaloi ugynokok regularis kifejezese amelyeknel aktivalodik a tomorites */
	private static String confCompressibleUserAgents;
	
	/* A domainek regularis kifejezese aminel aktivalodik a tomorites */
	private static String confCompressibleDomains;

	/* A tomoritett JPEG kepek minosege */
	@SuppressWarnings("unused")
	private static int confImageJpegQuality;

	/* Kulonbozo tomoritest szabalyozo flag-ek */
	private static Map<String,Boolean> confCompressFlags;
	
	
	/**
	 * Letrehozza a szolgatatas szalat, hogy folytassa a szerver kapcsolatot.
	 * @param icapserver ICAP server amelyik a szolgltatast futtatja
	 * @param clientsocket	ICAP kliens socket
	 */
	public ContextCompress(IcapServer icapserver, Socket clientsocket){
		super(icapserver, clientsocket);
		this.server = icapserver;
		this.setSocket(clientsocket);
		if (!initialized){
			initialize(icapserver.serviceconfig);
		}		
	}
	
	/**
	 * Az initialized valtozot hamisra allitja.
	 */
	public static void cleanup(){
		initialized = false;
	}
	
	/**
	 * Visszaadja, hogy milyen fajta modositasokat (keres, valasz) tamogat a szolgaltatas
	 * @see icap.core.AbstractService#getSupportedModes()
	 */
	public VectoringPoint getSupportedModes(){
		return VectoringPoint.REQRESPMOD;
	}
	
	/**
	 * Inicaliazalja a szolgaltatast.
	 * @param serviceconfig A szolgaltatas parameterei a konfiguracios fajlban
	 */
	private static synchronized void initialize(Properties serviceconfig) {
		if (initialized) return;
		ServicesProperties.refresh();
		System.err.println("ContextCompress config: " + serviceconfig.toString());
		String defaultUserAgentPattern = "(.*(AvantGo|DoCoMo|UP\\.Browser|EudoraWeb|Windows CE|Minimo|NetFront|Xiino|iPhone|Opera Mobi|BlackBerry|Opera Mini|Symbian ?OS|[cC]onfiguration\\/CLDC-|Vodafone|PalmSource).*|Nokia.*|SEC.*|SAMSUNG.*|SonyEricsson.*|(MOT|mot)-.*)";
		String defaultCompressibleDomains = "((apn-.*|gprs.*)\\.vodafone\\.hu|.*gprs\\.p(annon)?gsm\\.hu|.*\\.pool\\.t-umts\\.hu)\\.?";
		confSupportedContentTypes = serviceconfig.getProperty("supportedcontenttypes", "text/ application/x-javascript application/javascript image/ /xml +xml application/json").split("\\s+");
		confCompressibleContentTypes = serviceconfig.getProperty("compressiblecontenttypes", "text/ application/x-javascript application/javascript /xml +xml application/json").split("\\s+");
		confHtmlContentTypes = serviceconfig.getProperty("htmlcontenttypes", "text/html").split("\\s+");
		confImageContentTypes = serviceconfig.getProperty("imagecontenttypes", "image/").split("\\s+");		
		confCssContentTypes = serviceconfig.getProperty("csscontenttypes", "text/css").split("\\s+");		
		confJavaScriptContentTypes = serviceconfig.getProperty("javascriptcontenttypes", "text/javascript application/x-javascript application/javascript").split("\\s+");
		confCompressibleUserAgents = serviceconfig.getProperty("compressibleuseragents", defaultUserAgentPattern);
		confCompressibleDomains = serviceconfig.getProperty("compressibledomains", defaultCompressibleDomains);
		confImageJpegQuality = Integer.parseInt(serviceconfig.getProperty("imagejpegquality", "15"));
		String[] flags = serviceconfig.getProperty("compressflags", "http html html-il-java html-il-css image css javascript").split("\\s+");
		confCompressFlags = new HashMap<String,Boolean> ();
		for(String flag:flags) {
			confCompressFlags.put(flag, true);
		}
		try {
			("").matches(confCompressibleUserAgents);
		} catch (PatternSyntaxException e) {
			System.err.println("ContextCompress UserAgent regexp: Invalid pattern, using default!");
			confCompressibleUserAgents = defaultUserAgentPattern;			
		}
		try {
			("").matches(confCompressibleDomains);
		} catch (PatternSyntaxException e) {
			System.err.println("ContextCompress CompressibleDomains regexp: Invalid pattern, using default!");
			confCompressibleDomains = defaultCompressibleDomains;		
		}
		initialized = true;
	}
	
	/**
	 * Visszaadja a szolgaltatas szoveges leirasat.
	 * @see icap.core.AbstractService#getDescription()
	 */
	public String getDescription(){
		return ServicesProperties.getString("ContextCompress.description"); 
	}

	/**
	 * Eloallitja az ICAP szerver valaszat.
	 * @see icap.core.AbstractService#getResponse(java.io.ByteArrayOutputStream)
	 */
	public int getResponse(ByteArrayOutputStream bas)  {
		if (Log.isEnable()) logstr.setLength(0);
		bas.reset();
		long timing = System.nanoTime();
		int returncode=-1;
		try{
			switch (this.getType()){
				case REQMOD:
					if (Log.isEnable()) this.logstr.append("HTTP/").append(this.httpmethod).append(" ").append(this.getAbsolutePath());
					returncode = getReqmodResponse(bas);
					break;
				case RESPMOD: 
					if (Log.isEnable()) logstr.append("HTTP/").append(this.rescode).append(" ").append(this.getAbsolutePath());
					returncode = getRespModResponse(bas);
					break;
				default: break;
			}
			if (returncode!=-1){
				timing = (System.nanoTime()-timing)/1000000;
				if (Log.isEnable()) Log.access(logstr.insert(0,String.format("%1$-4s [%2$-7s] [%3$-10s] ICAP/%4$-3s ", timing, type.toString(), servicename, returncode)));
				return returncode;
			}
		} catch (Exception e){
			Log.trace(Log.INFO,"ContextCompress Exception while compressing", e);
		}
		/* Ha ide jut a futas, akkor hiba keletkezett, adjunk vissza 500-as kodot */
		try{
			bas.reset();
			bas.write(_500SERVERERROR);
		} catch (Exception e){
			/* Ha itt is hiba volt, akkor se csinaljunk semmit */
		}
		if (Log.isEnable()) Log.access(logstr.insert(0,"["+type.toString()+"] ["+servicename+"] ICAP/500 "));  
		return 500;
	}
	
	/**
	 * Megnezi, hogy a MIME tipus a megadott lehetosegek kozott van-e.
	 * @param contenttype A MIME tipus, amit ellnorizni kell
	 * @param mimeTypesToCheck A lehetseges MIME tipusok
	 * @return Igazat ad vissza, ha a MIME tipus megfelel valamelyik lehetseges MIME tipusnak
	 */
	public static boolean isMimeTypeSupported(String contenttype,String[] mimeTypesToCheck){
		boolean ismimesupported = false;
		for (String ct:mimeTypesToCheck) {
			if (ct.equals("*")) return true;
			if (contenttype.contains(ct)){
				ismimesupported = true;
				break;
			}
		}
		return ismimesupported;
	}
	
	/**
	 * Eloallitja az ICAP keres-modositas valaszt, vagyis a modositott HTTP kerest.
	 * @param bas A stream, ahova a modositott keres kerul
	 * @return	ICAP valasz statuszkod
	 * @throws Exception
	 */
	public synchronized int getReqmodResponse(ByteArrayOutputStream bas) throws Exception {
		
		/* Toltsuk le a keres teljes tartalmat */	
        if (i_req_body > 0){
        	this.getAllBody();
        }
        
        /* Adjuk meg az X-Context-Compress fejlecet, hogy a valaszt ez alapjan tudja 
         * a proxy cache-elni illetve mi is ez alapjan fogjuk vagy nem fogjuk majd a 
         * valaszt tomoriteni.
         */
        boolean comp;
        updateHttpHeader("X-Context-Compress", (comp=getContextCompression())?"true":"false", false);

		if (Log.isEnable()) logstr.append(" [compression: ").append(comp?"on":"off").append("]");
        
		/* Hozzuk letre az ICAP valaszt */
		StringBuilder sb = new StringBuilder();
		
		/* Hozzaadjuk az ICAP hostot, majd a HTTP fejlecet a 0. pozicioba */
		sb.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF);//.append("Server: ICAP-Server-Software/1.0\r\n"); 
		
		/* A keres valassza valt, (HTTP/xxx -vel kezdodik GET/POST/.... helyett) */
		boolean turnedIntoResponse = reqHeader.subSequence(0, 5).equals("HTTP/"); 
		
		sb.append(server.icaphost).append(turnedIntoResponse?"Encapsulated: res-hdr=0":"Encapsulated: req-hdr=0"); 
		
		if (reqBody!=null && reqBody.size()>0){
			sb.append(turnedIntoResponse?", res-body=":", req-body="); /* Ha van body, megadjuk */ 
		} else {
			sb.append(", null-body="); /* Ha nincs, null-bodyt hasznalunk */
		}
		sb.append(reqHeader.length()).append(CRLF);
		sb.append("Cache-Control: no-cache").append(CRLF); 

		/* Megadjuk, hogy perzisztens-e a kapcsolat */
		if (server.useKeepAliveConnections()) {
			sb.append(HEAD_CONNECTION_KEEPALIVE);
		} else {
			sb.append(HEAD_CONNECTION_CLOSED);
			this.closeConnection();
		}
		
		/* ICAP fejlec vege */
		sb.append(CRLF);

		/* A teljes HTTP fejlec */ 
		sb.append(reqHeader);
		
		bas.write(sb.toString().getBytes());

		/* A body resz, chunked megoldassal kodolva */
		if (reqBody!=null && reqBody.size()>0) {
			bas.write((Integer.toHexString(reqBody.size())+CRLF).getBytes());
			reqBody.writeTo(bas);
			bas.write(CRLF.getBytes());
			bas.write(ENDCHUNK);
		}
	
		/* Minden kesz, minden jo, 200-as kod */
		return 200;
	}
	
	/**
	 * Ez a fuggveny vegzi annak meghatarozasat, hogy kell-e a szerver valaszat tomoriteni a kliens szamara.
	 * Vagyis itt tortenik meg a kontextusinformaciok feldolgozasa, es annak eldontese, hogy szukseg van-e
	 * a szerver majdani valaszanak tomoritesere.
	 * @return igaz erteket ad vissza, ha a tomorites aktiv, hamisat egyebkent
	 */
	public synchronized boolean getContextCompression() {
		String useragent = getReqHeader("user-agent");
		if (useragent.matches(confCompressibleUserAgents)) {
				return true;
		}
		String userip = this.getIcapHeader("x-client-ip");
		try {
			InetAddress ipaddr = InetAddress.getByName(userip);
			String hostname = ipaddr.getCanonicalHostName();
			if (hostname.matches(confCompressibleDomains)) {
				return true;
			}
		} catch (UnknownHostException e) {
		}
		return false;
	}
	
	/** 
	 * Eloallitja az ICAP valasz-modositas valaszt, vagyis a modositott HTTP valaszt.
	 * @param bas A stream ahova a modositott valasz kerul
	 * @return	ICAP valasz statuszkod
	 * @throws Exception 
	 */
	public synchronized int getRespModResponse(ByteArrayOutputStream bas) throws Exception {

		String contenttype = this.getRespHeader("content-type");
		boolean disablecompression = false;

		if (contenttype!=null) {
			contenttype = contenttype.toLowerCase();
		} else { /* Az RFC 2616 szerint ha nincs megadva content type, akkor legyen octet-stream */
			contenttype = "application/octet-stream";
		}
		if (Log.finest()) Log.trace(Log.FINEST,"Content-type = "+contenttype); 
		if (Log.isEnable()) logstr.append(" [").append(contenttype).append("]");
		
		if (contenttype==null){ /* Ennek soha nem kene megtortennie */
			bas.write(server._204NOCONTENT);
			if (Log.isEnable()) logstr.append(" [no mime-type]");
			return 204;
		}
		
		/* Ellenorizzuk, hogy tamogatjuk-e a MIME tipust. Ha nem, adjunk vissza 204-et (nocontent) */
		if (!isMimeTypeSupported(contenttype, confSupportedContentTypes)){
			bas.write(server._204NOCONTENT);
			if (Log.isEnable()) logstr.append(" [unsupported mime-type]");
			return 204;
		}

		/* Letoltjuk a teljes valaszt */
		this.getAllBody();

		boolean bodyavailable = true;
		if (resBody==null) bodyavailable = false;
				
		/* Beallitjuk a Vary headert, hogy tudassuk a proxyval, hogy mas hosztoknak mas valasz jarhat */
		updateHttpHeader("Vary", "x-context-compress", true);
		
		disablecompression = (this.getReqHeader("x-context-compress").indexOf("true") == -1); 
		
		if (Log.isEnable()) logstr.append(" [compression: ").append(disablecompression?"off":"on").append("]");
		
		if (!disablecompression && bodyavailable) {

			/* Kitomoritjuk a tartalmat, ha tomoritve volt */
			boolean initiallyGzipped = isCompressed();
			if ( initiallyGzipped ) resBody = uncompress(resBody);
			
			if (isMimeTypeSupported(contenttype, confImageContentTypes)) {
				if (confCompressFlags.containsKey("image")) {
					byte[] binaryContent = this.resBody.toByteArray();
					/* Csak akkor tomoritunk at, ha 200 byte-nal nagyobb a kep */
					if (binaryContent.length > 200) {
						/* Az ImageMagick sajnos nem thread-safe, igy szinkronizalni kell */
						/*synchronized (imagemagicklock) {
							MagickImage image = new MagickImage();
							ImageInfo info = new ImageInfo();
							try {
								image.blobToImage(info, binaryContent);
								info.setMagick("JPG");
								info.setQuality(confImageJpegQuality);
								byte[] imageBytes = image.imageToBlob(info); 
								if (binaryContent.length > imageBytes.length) {
									updateContentType("image/jpeg");
									this.resBody.reset();
									this.resBody.write(imageBytes);
								}
							} catch (MagickException e) {
								bas.write(server._204NOCONTENT);
								return 204;
							}
						}*/
					}
				}
			} else {
				
				String content, encoding = "ISO-8859-1";

				if (isMimeTypeSupported(contenttype, confHtmlContentTypes)) {
					try { /* Meghatarozzuk a forras karakterkeszletet */
						encoding = this.getEncoding(contenttype);
						if (Log.isEnable()) logstr.append(" [encoding/").append(encoding).append("]");
					} catch (Exception e) { /* Ha nem sikerult, akkor inkabb modositatlanul adjuk vissza */
						if (Log.isEnable()) logstr.append(" [unknown encoding]");
						if (initiallyGzipped) this.resBody = compress(this.resBody);
						return unchangedResponse(bas);
					}
				}
				content = new String (this.resBody.toByteArray(),encoding);

				/* Meghatarozzuk a tartalom hash-et, hogy csak akkor modositsuk, ha modosult */
				int initialcontenthash = content==null?0:content.hashCode();
				
				if (isMimeTypeSupported(contenttype, confHtmlContentTypes)) {
					if (confCompressFlags.containsKey("html")) {
						content = cleanupHTML(content);
					}
				} else if (isMimeTypeSupported(contenttype, confCssContentTypes)) {
					if (confCompressFlags.containsKey("css")) {
						content = cleanupCSS(content);
					}
				} else if (isMimeTypeSupported(contenttype, confJavaScriptContentTypes)) {
					if (confCompressFlags.containsKey("javascript")) {
						content = cleanupJavaScript(content);
					}
				}
				
				/* Megnezzuk, hogy modosult-e a tartalom, ha igen frissitjuk azt */ 
				if (initialcontenthash != (content==null?0:content.hashCode())) {
					this.resBody.reset(); /* A regi tartalom kitorlese */
					if (encoding!=null) {
						if (content !=null) this.resBody.write(content.getBytes(encoding));
					} else {
						if (content !=null) this.resBody.write(content.getBytes());	
					}
				}
			}

			/* Tomoritsuk a valaszt, ha a bongeszo tamogatja */
			if ( initiallyGzipped ) {
				long timing = System.currentTimeMillis();
				this.resBody = compress(this.resBody);
				if (Log.isEnable()) this.logstr.append(" [gzip:"+(System.currentTimeMillis()-timing)+"]"); 
			} else if ( confCompressFlags.containsKey("http") ) {
				if (isMimeTypeSupported(contenttype, confCompressibleContentTypes)) {
					long timing = System.currentTimeMillis();
					this.resBody = compress(this.resBody);
					if (Log.isEnable()) this.logstr.append(" [gzip-opt:"+(System.currentTimeMillis()-timing)+"]"); 
				}
			}
		}
				
		/* Generaljuk le a valaszt */
		updateContentLength(bodyavailable?this.resBody.size():0);

		StringBuilder sb = new StringBuilder();
		sb.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF);//.append("Server: ICAP-Server-Software/1.0\r\n"); 
		sb.append("Encapsulated: res-hdr=0, res-body=").append(this.resHeader.length()).append(CRLF); 
		sb.append(CRLF).append(this.resHeader);
		bas.write(sb.toString().getBytes());
		
		/* Adjuk hozza a tartalmat */
		if (this.resBody!=null && this.resBody.size()>0) {
			bas.write((Integer.toHexString(this.resBody.size())+CRLF).getBytes());
			this.resBody.writeTo(bas);
			bas.write(CRLF.getBytes());
		}
		bas.write(ENDCHUNK);

		/* Minden kesz, 200-as kod */
		return 200;

	}
	
	/**
	 * 200 OK uzenetet ad vissza modositas nelkuli valasszal
	 * @param bas ByteArrayOutputStream ami a valaszt tartalmazza majd
	 * @return 200
	 * @throws Exception
	 */
	public int unchangedResponse(ByteArrayOutputStream bas) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF);//.append("Server: ICAP-Server-Software/1.0\r\n"); 
		sb.append("Encapsulated: res-hdr=0, res-body=").append(this.resHeader.length()).append(CRLF); 
		sb.append(CRLF).append(this.resHeader);
		bas.write(sb.toString().getBytes());
		if (this.resBody!=null) {
			bas.write((Integer.toHexString(this.resBody.size())+CRLF).getBytes());
			this.resBody.writeTo(bas);
			bas.write(CRLF.getBytes());
		}
		bas.write(ENDCHUNK);
		return 200;
	}
	
	/**
	 * A HTTP fejlecek kozul frissit egyet.
	 * @param header A frissitendo http fejlec neve
	 * @param value A HTTP fejlec uj erteke
	 * @param add Ha igaz, akkor ha a fejlec eddig is letezett, akkor az uj ertek vesszovel lesz a regi ertek utan beillesztve 
	 */
	private void updateHttpHeader(String header, String value, boolean add) {
		String normalheader = header;
		header = header.toLowerCase();
		switch (this.type){
		case REQMOD:
			if (this.httpReqHeaders.containsKey(header)){
				int pos1 = this.reqHeader.toString().toLowerCase().indexOf(header) + header.length() + 1;
				int pos2 = this.reqHeader.indexOf("\r\n", pos1);
				if (add) {
					this.reqHeader = this.reqHeader.replace(pos2,pos2, "," + value + "");				
				} else {
					this.reqHeader = this.reqHeader.replace(pos1,pos2, " " + value + "");
				}
			} else {
				this.reqHeader.insert(this.reqHeader.length()-2, normalheader+": " + value + "\r\n");
			}
			break;
		case RESPMOD:
			if (this.httpRespHeaders.containsKey(header)) {
				int pos1 = this.resHeader.toString().toLowerCase().indexOf(header) + header.length() + 1;
				int pos2 = this.resHeader.indexOf("\r\n", pos1);
				if (add) {
					this.resHeader = this.resHeader.replace(pos2,pos2, ","+value+"");					
				} else {
					this.resHeader = this.resHeader.replace(pos1,pos2, " "+value+"");					
				}
			} else {
				this.resHeader.insert(this.resHeader.length()-2, normalheader+": " + value+ "\r\n");
			}
			break;
		default:
		}
	}


	/********************************************************
	 * Itt jonnek az altalanos segedfuggvenyek (statikusak) *
	 ********************************************************
	 **/

//	<------------------------------------------------------------------------------------------>
	/**
	 * Javascriptet tisztit meg a felesleges szokozoktol, kommentektol.
	 * @param content A megtisztitando JavaScript
	 * @return A tisztitott JavaScript 
	 */
	private static String cleanupJavaScript(String content) {
		Reader reader = new StringReader(content);
		Writer writer = new StringWriter();		
		try {
			JavaScriptCompressor javasc = new JavaScriptCompressor(reader, new ErrorReporter() {
                public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
                	return;
                }
                public void error(String message, String sourceName,int line, String lineSource, int lineOffset) {
                	throw new EvaluatorException(message);
                }
                public EvaluatorException runtimeError(String message, String sourceName,int line, String lineSource, int lineOffset) {
                    return new EvaluatorException(message);
                }
            });
			javasc.compress(writer, -1, false, false, !confCompressFlags.containsKey("javascript-sc"), !confCompressFlags.containsKey("javascript-mo"));
		} catch (Exception e) {
			return content;
		}
		return writer.toString();				
	}
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * CSS-t tisztit meg a felesleges szokozoktol, kommentektol.
	 * @param content A megtisztitando CSS
	 * @return A tisztitott CSS
	 */
	private static String cleanupCSS(String content) {
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
	/* A HTML whitespace karakterei */
	private static String WHITESPACE =  " \r\n\t";
//	<------------------------------------------------------------------------------------------>
	
//	<------------------------------------------------------------------------------------------>
	/**
	 * A whitespace karakterekt vonja ossze a content stringben.
	 * @param content A karakterlanc ami a bemenetet tartalmazza
	 * @param last Megadja, hogy a bemenet elejen maradjon-e whitespace
	 * @return A bemenet immaron a whitespace karakterek osszevonasaval
	 */
	private static String cleanupWhitespace(String content, boolean last) {
		String ret = content.replaceAll("["+WHITESPACE+"]+", " ");
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
	private static String cleanupHTML(String content) {
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
					if (tepos != -1 && confCompressFlags.containsKey("html-il-java")) {
						result.append(cleanupHTMLTag(currenttag) + cleanupJavaScript(content.substring(tbpos+currenttag.length(), tepos)) +  "</script>");
					} else {
						result.append(content.substring(tbpos, tepos == -1 ? content.length(): tepos+telen));
					}
				} else if (currenttag.length() >= 6 && currenttag.substring(0, 6).equalsIgnoreCase("<style") && scriptLevel == 0 && styleLevel == 0) {
					telen = 8;
					tepos = indexOfRegexp(content, "\\<\\/[Ss][Tt][Yy][Ll][Ee]\\>", tbpos);
					if (tepos != -1 && confCompressFlags.containsKey("html-il-css")) {
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