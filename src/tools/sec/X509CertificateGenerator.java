/**----------------------------------------------------------------------------
 * GreasySpoon
 * Copyright (C) 2009 Karel Mittig
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
 * Created	:	20/06/09
 *----------------------------------------------------------------------------------
 */
package tools.sec;

///////////////////////////////////
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;
///////////////////////////////////

/** 
 * Class using SUN proprietary API API to generate X.509 certificates without the need 
 * of keytool, openssl command lines or Bouncycastle API.<br />
 * Use DSA and SHA1 algorithm for certificate and key 
 * @author Karel
 */
@SuppressWarnings("restriction")
public class X509CertificateGenerator {
	
	private final static int KEYLENGTH = 1024;//2*"512 should be enough"... ;-) 
	private final static int VALIDITYINDAYS = 1095; //3 years

	//--------------------------------------------------------------------------------------------------
	/**
	 * Generate X509 certificate for given KeyStore
	 * @param keystore the keystore in which to store certificate
	 * @param cn The server URL or IP
	 * @param adminMail Administrator mail showed in certificate
	 * @param pwd keystore password (also used for keyentry password)
	 * @throws Exception
	 */
	public static void generateKeyFor(KeyStore keystore, String cn,String adminMail,String pwd) throws Exception{

	    CertAndKeyGen cakg = new CertAndKeyGen("DSA", "SHAwithDSA");
        cakg.generate(KEYLENGTH);
       
        X500Name name = new X500Name(
        		cn, //common name of a person, e.g. "Vivette Davis"
        		System.getProperty("user.name"),//organizationUnit - small organization name, e.g. "Purchasing"
                "GreasySpoon",//organizationName - large organization name, e.g. "Onizuka, Inc."
                "Somewhere over",//localityName - locality (city) name, e.g. "Palo Alto"
                "Internet",//stateName - state name, e.g. "California"
                System.getProperty("user.country") //country - two letter country code, e.g. "CH"
                );
        
        X509Certificate certificate = cakg.getSelfCertificate(name,VALIDITYINDAYS*86400);
        certificate.checkValidity();
   	
	    //Add certificate to keystore
	    keystore.setCertificateEntry("gs-ssl", certificate);
		java.security.cert.Certificate[] certs = {certificate};
		keystore.setKeyEntry("gs-ssl", cakg.getPrivateKey(), pwd.toCharArray(),certs);
	}
	//--------------------------------------------------------------------------------------------------

}
