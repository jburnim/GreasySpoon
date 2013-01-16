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
 * Created	:	20/06/09
 *----------------------------------------------------------------------------------
 */
package tools.httpserver;

///////////////////////////////////
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import tools.sec.X509CertificateGenerator;
///////////////////////////////////


/**
 * SSL server Factory, used to create SSL server from raw certificate (without JVM options)
 * Supports keystore/SSL certificate creation, loading and saving 
 * @author Karel MITTIG
 */
public class SslSocketFactory {
	
	static KeyStore ks;
	static String KEYSTORETYPE = "JKS";

//-----------------------------------------------------------------------------
	/**
	 * Create a new SSL Server socket
	 * @param keystoreName The keystore containing SSL certificate
	 * @param masterpwd Password to access to the Keystore
	 * @return A SSL server socket instance
	 * @throws Exception
	 */
	public static ServerSocket createServerSocket(String keystoreName, String masterpwd) throws Exception{
			
		File f = new File(keystoreName);
		if (!f.exists()){
			ks = createKeyStore(keystoreName, masterpwd);
		} else {
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks = loadKeyStore(keystoreName, masterpwd);
		}

	// KeyManager's decide which key material to use.
	KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
	kmf.init(ks, masterpwd.toCharArray());

	SSLContext sslContext = SSLContext.getInstance("TLS");
	sslContext.init(kmf.getKeyManagers(), null, null);
	return sslContext.getServerSocketFactory().createServerSocket();
}
//-----------------------------------------------------------------------------
	
//-----------------------------------------------------------------------------
	/**
	 * Delete and recreate the keystore
	 * To call when changing Listening IP or admin password
	 * @param keystoreName
	 * @param masterpwd
	 */
	public static void resetKeystore(String keystoreName, String masterpwd) {
		try {
			File f = new File(keystoreName);
			if (!f.exists()){
				return;
			}
			f.delete();
			ks = createKeyStore(keystoreName, masterpwd);
		} catch (Exception e){
			e.printStackTrace();
		}
}
//-----------------------------------------------------------------------------
	
	
//-----------------------------------------------------------------------------
/**
 * Create a new Keystore with given password
 * @param keystoreName The key store to create
 * @param pwd password to assign to the key store
 * @return KeyStore created key store
 * @throws Exception
 */
public static KeyStore createKeyStore(String keystoreName, String pwd) throws Exception {
	String cn = HttpServer.bounded_ip.equals("")?InetAddress.getLocalHost().getHostName():HttpServer.bounded_ip;
	KeyStore _ks = KeyStore.getInstance(KEYSTORETYPE);
	_ks.load(null, pwd.toCharArray());
	X509CertificateGenerator.generateKeyFor(_ks,cn, HttpServer.adminmail,pwd);
	saveKeystore(_ks,keystoreName,pwd);
	return _ks;
}
//-----------------------------------------------------------------------------
	
//-----------------------------------------------------------------------------
/**
 * Load keystore information 
 * @param keyStoreName The keystore to read
 * @param keyStorePwd Keystore password
 * @return Keystore keystore object
 * @throws Exception
 */
public static KeyStore loadKeyStore(String keyStoreName, String keyStorePwd) throws Exception {
    KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);
    // get user password and file input stream
    char[] password = keyStorePwd.toCharArray();
    java.io.FileInputStream fis = new java.io.FileInputStream(keyStoreName);
    ks.load(fis, password);
    fis.close();
    return ks;
}
//-----------------------------------------------------------------------------	



//-----------------------------------------------------------------------------
/**
 * store the keystore to disk
 * @param ks Keystore to save
 * @param keystoreName name of the file to create/overwrite
 * @param pwd Password for the keystore
 * @throws Exception
 */
public static void saveKeystore(KeyStore ks, String keystoreName, String pwd) throws Exception{
	FileOutputStream fos = new java.io.FileOutputStream(keystoreName);
	ks.store(fos, pwd.toCharArray());
	fos.close();
}
//-----------------------------------------------------------------------------

}
