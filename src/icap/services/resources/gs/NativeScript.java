/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008,2009 Karel Mittig
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
 * Created on 30 June 2009
 *-----------------------------------------------------------------------------*/
package icap.services.resources.gs;

////////////////////////////////
//Import
import tools.logger.Log;
////////////////////////////////


/**
 * Native Script skeleton for Java native scripts
 * @author Karel
 */
public abstract class NativeScript {

	String nativeScriptName = "";
	HttpMessage httpmessage;
//	<------------------------------------------------------------------------->    
	/**
	 * Default constructor. MUST be instantiate by implementing classes
	 * using super() constructor in order to ensure class initialization.
	 */
	public NativeScript(){} 
//	<------------------------------------------------------------------------->
	
	/**
	 * Entry method that must be implemented by native scripts in order to be called
	 * @param httpMessage The HTTP Message (either request or response) to process
	 */
	public abstract void main(HttpMessage httpMessage);
	
//	<------------------------------------------------------------------------->
	/**
	 * Simple method, used to trace some script variables in service log
	 * @param s
	 */
	public void debug(String s){
		if (Log.fine()) if (s!= null && s.length()>0) Log.service(Log.FINE, String.format("%1$-20s  trace log [%2$s]", nativeScriptName,s));
	}
//	<------------------------------------------------------------------------->

}
