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

package tools.httpserver;

/**
 * HTTP constants used to describe requests and access rights
 */
public interface HttpConstants {

	/** Access rights that can be attached to a user connection to administration server */
	public enum RIGHTS {
		/**Administration rights => everything allowed*/
	ADMIN, 
	/**User rights => standard features allowed*/
	USER, 
	/**Noen rights => readonly */
	NONE};
	
	/**HTTP request methods*/
	public enum METHOD {
		/**GET request*/
		GET, 
		/**POST request*/
		POST, 
		/**Others requests types: HEAD, PUT,... (not handled for now)*/
		OTHER};
}
