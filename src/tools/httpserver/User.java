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
 * Simple user account class
 */
public class User implements HttpConstants{
	private String _pwd;
	private String _login;
	private RIGHTS _rights;
	
	/**
	 * Create a new user account
	 * @param login user login
	 * @param pwd user password
	 * @param rights associated account rights
	 */
	public User(String login, String pwd, RIGHTS rights){
		this._login = login;
		this._pwd = pwd;
		this._rights = rights;
	}

	/**
	 * @return User Login
	 */
	public  String getLogin() {
		return _login;
	}

	/**
	 * @param login Update user login with given value
	 */
	public  void setLogin(String login) {
		this._login = login;
	}

	/**
	 * @return User unencrypted password
	 */
	public  String getPwd() {
		return _pwd;
	}

	/**
	 * Update user password with new value.<br>
	 * Password must be provided in unencrypted format
	 * @param pwd Update user password with new value
	 */
	public  void setPwd(String pwd) {
		this._pwd = pwd;
	}

	/**
	 * @return User rights
	 */
	public RIGHTS getRights() {
		return _rights;
	}

	/**
	 * @param rights Update user rights
	 */
	public  void setRights(RIGHTS rights) {
		this._rights = rights;
	}

}
