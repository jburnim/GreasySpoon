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
 * Created:	27 janv. 2006
 *---------------------------------------------------------------------------*/
package icap.services;


///////////////////////////////////
//Import
import icap.IcapServer;
import icap.core.*;
import java.io.ByteArrayOutputStream;
import java.net.*;
import tools.logger.Log;
///////////////////////////////////

/**
 * <p>
 * Simple ICAP REQMOD and RESPMOD Echo Service<br>
 * <p>
 * Parse iCAP message from an iCAP client and return 200 OK with unmodified response<br>
 * Can be used as skeleton or example for new REQMOD AND/OR RESPMOD services<br>
 */
public class EchoService extends AbstractService {

//	<------------------------------------------------------------------------->
	/**
	 * Service thread creation to manage ICAP transaction
	 * @param _server ICAP server
	 * @param clientsocket socket with ICAP client
	 */
	public EchoService(IcapServer _server, Socket clientsocket){
		super(_server, clientsocket);
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * @see icap.core.AbstractService#getSupportedModes()
	 */
	public VectoringPoint getSupportedModes(){
		return VectoringPoint.REQRESPMOD;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * @see icap.core.AbstractService#getDescription()
	 */
	public String getDescription(){
		return "Simple ECHO service. Retrieves HTTP response header and body and returns a 200 OK with received content";
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->   
	/**
	 * Main abstractservice method implementation
	 * ICAP request has been parsed => generate response
	 * @return ICAP response code
	 **/
	public int getResponse(ByteArrayOutputStream bas)  {
		bas.reset();
		try{
			switch (this.getType()){
				case REQMOD: return getReqmodResponse(bas);
				case RESPMOD: return getRespModResponse(bas);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		//bug: on retourne une erreur
		try{
			bas.reset();
			bas.write(("ICAP/1.0 500 Server Error\r\n\r\n").getBytes());
		} catch (Exception e){}
		return 500;
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->
	/**
	 * Echo service in Response MODE
	 * @param bas ByteArrayOutputStream used to manipulate data
	 * @return ICAP response code
	 * @throws Exception
	 */
	public int getRespModResponse(ByteArrayOutputStream bas) throws Exception {
		this.getAllBody();
	
		//----------------------------
		// Here is where you can do content modifications
		//Do any response header modification you want
		/*if (this.getAbsolutePath().toLowerCase().startsWith("http://sample.test.com/")){
			if (resHeader.indexOf("Last-Modified: ")!=-1) {
				int p1 = resHeader.indexOf("Last-Modified: ");
				int p2 = resHeader.indexOf("\r\n", p1)+2;
				resHeader.delete(p1, p2);
			}
		}*/
		
		//Do any response body modification you want
		/*if (this.getReqUrl().trim().equals("http://sample.test.com/")){
			String newbody = resBody.toString();
			newbody = newbody.replace("content to rewrite","rewritten content");
			resBody.reset();
			resBody.write(newbody.getBytes());
			//update content length to match modifications
			updateContentLength(resBody.size());
		}*/
		//----------------------------
		
		//Response will be an ICAP 200 OK message
		StringBuilder icapresponse = new StringBuilder();
		icapresponse.append("ICAP/1.0 200 OK\r\n").append(server.getISTAG()).append(CRLF);
		
		//Set if connection will use keep-alive
		if (server.useKeepAliveConnections()) {
			icapresponse.append(HEAD_CONNECTION_KEEPALIVE);
		} else {
			icapresponse.append(HEAD_CONNECTION_CLOSED);
			this.closeConnection();
		}
		
		//Add ICAP header for HTTP response
		icapresponse.append("Encapsulated: res-hdr=0, res-body=").append(this.resHeader.length()).append(CRLF);

		//Add HTTP response header
		icapresponse.append("\r\n").append(this.resHeader);

		bas.write(icapresponse.toString().getBytes());

		//Add HTTP response body (if any)
		if (resBody.size()>0) {	//Return body in a single chunk
			bas.write((Integer.toHexString(resBody.size())+CRLF).getBytes()); // put chunk size(body length)
			resBody.writeTo(bas);//write body
			bas.write(CRLF.getBytes());//add chunk trailing CRLF
		}
		//Close ICAP response
		bas.write(ENDCHUNK);//close ICAP response
		return 200; //that's done
	}
//	<------------------------------------------------------------------------->

//	<------------------------------------------------------------------------->   
	/**
	 * Echo service in Request MODE
	 * @param bas ByteArrayOutputStream used to manipulate data
	 * @return ICAP response code
	 * @throws Exception
	 */
	public int getReqmodResponse(ByteArrayOutputStream bas) throws Exception {



		//Read request body (if any)
		if (i_req_body>0 && preview!=0){
			this.getAllBody();
		}

		//----------------------------
		// Here is where you can do content modifications
		//Do any response header modification you want
		/*if (this.getAbsolutePath().toLowerCase().startsWith("http://sample.test.com/")){
			if (reqHeader.indexOf("Cookie: ")!=-1) {
				int p1 = reqHeader.indexOf("Cookie: ");
				int p2 = reqHeader.indexOf("\r\n", p1)+2;
				resHeader.delete(p1, p2);
			}
		}*/
		
		//Do any response body modification you want
		/*if (reqBody!=null && this.getReqUrl().trim().equals("http://sample.test.com/")){
			String newbody = reqBody.toString();
			newbody = newbody.replace("content to rewrite","rewritten content");
			reqBody.reset();
			reqBody.write(newbody.getBytes());
			//update content length to match modifications
			updateContentLength(reqBody.size());
		}*/
		//----------------------------
		
		//Start building an ICAP 200 OK response
		StringBuilder response = new StringBuilder();
		response.append(_200_OK);
		response.append(server.getISTAG()).append(CRLF);

		//Append ICAP host header, then HTTP header in position zero (static)
		response.append(server.icaphost).append("Encapsulated: req-hdr=0");
		
		//Finish ICAP header relative to HTTP message
		if (reqBody!=null && reqBody.size()>0){
			response.append(", req-body="); //Body available =>tag req-body
		} else {
			response.append(", null-body="); //orelse => null-body
		}
		//Add HTTP request header size
		response.append(reqHeader.length()).append(CRLF);

		//Set if connection will use keep-alive
		if (server.useKeepAliveConnections()) {
			response.append(HEAD_CONNECTION_KEEPALIVE);
		} else {
			response.append(HEAD_CONNECTION_CLOSED);
			this.closeConnection();
		}

		//End of ICAP header
		response.append(CRLF);

		//Append HTTP request header
		response.append(reqHeader);

		//flush response
		bas.write(response.toString().getBytes());

		//If body is available, append it as a single chunk
		if (reqBody.size()>0) {
			bas.write((Integer.toHexString(reqBody.size())+CRLF).getBytes());
			reqBody.writeTo(bas);
			bas.write(CRLF.getBytes());
			bas.write(ENDCHUNK);
		}
		if (Log.finest()) Log.trace(Log.FINEST,bas.toString()+"---------------------");

		//All done
		return 200;
	}
//	<------------------------------------------------------------------------->


}
