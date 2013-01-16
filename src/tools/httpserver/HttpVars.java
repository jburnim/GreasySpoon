/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2008-2009 Karel Mittig
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
 * Created	:	13 feb. 2006
 *---------------------------------------------------------------------------*/
package tools.httpserver;

///////////////////////////////////
// 	Import
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
///////////////////////////////////

/**
 * HTTP tools
 */
public class HttpVars {

    static SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z",Locale.US);
    
    static{formatter.setTimeZone(TimeZone.getTimeZone("GMT"));}
    /**DAY, dd-MMM-YYYY HH:MM:SS GMT
     * Generate a HTTP compatible date value corresponding to given absolute time given
     * The expiration date is set by using the format "expires=<date>", where <date> is 
     * the expiration date in Greenwich Mean Time (GMT). If the expiration date is not set,
     * the cookie expires after the Internet session ends. Otherwise, the cookie is persisted
     * in the cache until the expiration date. The date must follow the format
     * DAY, dd-MMM-YYYY HH:MM:SS GMT
     * , where DAY is the day of the week (Sun, Mon, Tue, Wed, Thu, Fri, Sat),
     * dd is the day in the month (such as 01 for the first day of the month),
     * MMM is the three-letter abbreviation for the month (Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec),
     * YYYY is the year, 
     * HH is the hour value in military time (22 would be 10:00 P.M., for example),
     * MM is the minute value, and SS is the second value.
     * @param absoluteTime absolute time (in ms)
     * @return Date on HTTP standard format ("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US)
     */
    public static String getHTTPDate(long absoluteTime){
        Date date = new Date(absoluteTime);
        return formatter.format(date);
    }
    
}
