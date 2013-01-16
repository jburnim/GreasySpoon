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
package icap.services;


///////////////////////////////////
// Imports
import icap.IcapServer;
import icap.core.*;
import java.io.ByteArrayOutputStream;
import java.net.*;
import tools.logger.Log;
///////////////////////////////////

/**
 * <p>
 * Simple ICAP RESPMOD Echo Service using Preview<br>
 * <p>
 * Parse iCAP message from an iCAP client, return 204 for byte content
 * and return 200 OK with unmodified response otherwise<br>
 * Can be used as skeleton or example for new RESPMOD services<br>
 */
public class PreviewService extends AbstractService {
    /** service name (mainly appears in logs)*/
    String servicename = "preview";
    
//  <------------------------------------------------------------------------->        
    /**
     * Service thread creation 
     * @param server The ICAP server managing service 
     * @param clientsocket The ICAP client socket
     */
    public PreviewService(IcapServer server, Socket clientsocket){
        super(server, clientsocket);
    }
//  <------------------------------------------------------------------------->

//  <------------------------------------------------------------------------->
    /**
     * @see icap.core.AbstractService#getDescription()
     */
    public String getDescription(){
    	return "Simple ECHO service. Retrieves HTTP response header usiong 0 preview and returns a 204 no content";
    }
//  <------------------------------------------------------------------------->


//	<------------------------------------------------------------------------->
	/**
	 * @see icap.core.AbstractService#getSupportedModes()
	 */
	public VectoringPoint getSupportedModes(){
		return VectoringPoint.RESPMOD;
	}
//	<------------------------------------------------------------------------->

    
//  <------------------------------------------------------------------------->
	StringBuilder logstr = new StringBuilder();
    /**
     * IcapReaderThread implementation
     * @see icap.core.AbstractService#getResponse(java.io.ByteArrayOutputStream)
     */
    public int getResponse(ByteArrayOutputStream bas)  {
       bas.reset();
       logstr.setLength(0);
	   if (Log.isEnable()) logstr.append("HTTP/").append(this.rescode).append(" ").append(this.getAbsolutePath());
       try{
    	   
			bas.write(this.server._204NOCONTENT);
			if (Log.isEnable()) Log.access(logstr.insert(0,"["+type.toString()+"] ["+servicename+"] ICAP/204 ").append(" [unsupported mime-type]"));
			return 204;
    	   /*
    	    * For those who want to go further, read body
    	    * 204 are no more possible after
    	    * */
			/*
           this.getAllBody();
           if (debug>2) System.out.println("///////////////////////////////////////////////////");
           
           StringBuilder sb = new StringBuilder();
           sb.append("ICAP/1.0 200 OK\r\nISTAG: \"001-000-000004\"\r\n");//.append("Server: ICAP-Server-Software/1.0\r\n");
           sb.append("Encapsulated: res-hdr=0, res-body=").append(this.resHeader.length()).append(CRLF);
           sb.append("\r\n").append(this.resHeader);
           
           if (debug>2) System.out.println("THREAD ["+id+"] RESPONSE:\n---------------------\n"+new String(sb)+"\r\n[....BODY...]\r\n---------------------");
           
           bas.write(sb.toString().getBytes());

           if (resBody.size()>0) {
        	   bas.write((Integer.toHexString(resBody.size())+CRLF).getBytes());
        	   resBody.writeTo(bas);
        	   bas.write(CRLF.getBytes());
           }
           bas.write(ENDCHUNK);
           
           return bas;
    	    */
       } catch (Exception e){
           //e.printStackTrace();
       }
       //bug: let's return an error
       try{
    	   bas.reset();
    	   bas.write(Icap._500SERVERERROR);
       } catch (Exception e){
    	   //nothing more to try
       }
       if (Log.isEnable()) Log.access(logstr.insert(0,"["+type.toString()+"] ["+type.toString()+"] ["+servicename+"] ICAP/500 "));
       return 500;
    }
//  <------------------------------------------------------------------------->
    
    
    
}
