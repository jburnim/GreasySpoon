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
 * Created	:	27 janv. 2006
 *---------------------------------------------------------------------------*/
package icap.core;


/**
 * Generic interface used to define ICAP constants
 */
public interface Icap {
    
	/** Defines ICAP requests type that can be received */
    public enum TYPE {
    	/**EMPTY Request*/
    	EMPTY,
    	/**Invalid Request*/
    	INVALID, 
    	/**OPTIONS Request*/
    	OPTIONS, 
    	/**REQMOD Request*/
    	REQMOD, 
    	/**RESPMOD Request*/
    	RESPMOD}
    
	/** Vectoring points that can be supported by a service: REQMOD, RESPMOD or both */
	public enum VectoringPoint {
		/**Support of REQMOD only*/
		REQMOD, 
		/**Support of RESPMOD only*/
		RESPMOD, 
		/**Support of both REQMOD and RESPMOD*/
		REQRESPMOD}
	
	/** Defines ICAP client brand to allow custom implementation/optimisations*/
	public enum ClientBrand {
		/**Network Appliance proxy*/
		NETAPP, 
		/**Unknown/unspecific implementation*/
		OTHER
		}
	
    
    /**Simple <CR><LF>*/
    public final static String CRLF = "\r\n";
    /** <CR><LF> bytes*/
	/**Simple \r\n in byte format*/
    public final static byte[] CRLF_b = "\r\n".getBytes();
    
    /** CRLF size*/
    final static int CRSIZE = CRLF.length();

    /**ICAP chunk trailer (0 chunck + 2* CRLF)*/
    final static byte[] ENDCHUNK = ("0"+CRLF+CRLF).getBytes();
    
	/** ICAP 200 starting header*/
	final static String _200_OK = ("ICAP/1.0 200 OK"+CRLF);
	/** ICAP 204 starting header*/
	final static String _204_NOCONTENT = ("ICAP/1.0 204 No Content"+CRLF);
	/** ICAP 500 error message (directly converted into bytes for fast processing)*/
	final static byte[] _500SERVERERROR = ("ICAP/1.0 500 Server Error"+CRLF+CRLF).getBytes();
	/** ICAP 504 Service Timeout error message (directly converted into bytes for fast processing)*/
	final static byte[] _504SERVERERROR = ("ICAP/1.0 504 Service timeout"+CRLF+CRLF).getBytes();
	/**Bad Request - the request could not be understood by the server due to malformed syntax.*/
	/** ICAP 400 error message (directly converted into bytes for fast processing)*/
	final static byte[] _400CLIENTERROR = ("ICAP/1.0 400 Bad Request"+CRLF+CRLF).getBytes();
	
	/** Keep-alive header*/
	final static String HEAD_CONNECTION_KEEPALIVE = "Connection: keep-alive" + CRLF;
	/** Connection close header*/
	final static String HEAD_CONNECTION_CLOSED = "Connection: close"+ CRLF;
	
}
