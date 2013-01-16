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
 * Created on 15 mars 2005
 *-----------------------------------------------------------------------------*/
package tools.sec;

////////////////////////////////
// Import
import javax.crypto.*;
import java.security.spec.*;
import javax.crypto.spec.*;
import tools.general.Base64;
////////////////////////////////

/**
 * Basic class to encrypt and decode a String using DES 
 * and based on a literal key
 * Here is an example that uses the class:
 *   try {
 *      // Create encrypter/decrypter class
 *      Encryption encrypter = new Encryption("My Pass Phrase!");
 *      // Encrypt
 *      String encrypted = encrypter.encrypt("Don't tell anybody!");
 *      // Decrypt
 *      String decrypted = encrypter.decrypt(encrypted);
 *  } catch (Exception e) {
 *  }
 */
public class Encryption {
        Cipher ecipher;
        Cipher dcipher;
    
        // 8-byte Salt
        byte[] salt = {
            (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
            (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
        };
    
        // Iteration count
        int iterationCount = 19;

    /**
     * Create a new instance of encryption class 
     * @param litteralKey the key used for encryption and decryption
     */
    public Encryption(String litteralKey) {
            try {
                // Create the key
                KeySpec keySpec = new PBEKeySpec(litteralKey.toCharArray(), salt, iterationCount);
                SecretKey key = SecretKeyFactory.getInstance(
                    "PBEWithMD5AndDES").generateSecret(keySpec);
                ecipher = Cipher.getInstance(key.getAlgorithm());
                dcipher = Cipher.getInstance(key.getAlgorithm());
    
                // Prepare the parameter to the ciphers
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
    
                // Create the ciphers
                ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            } catch (java.security.InvalidAlgorithmParameterException e) {//do nothing
            } catch (java.security.spec.InvalidKeySpecException e) {//do nothing
            } catch (javax.crypto.NoSuchPaddingException e) {//do nothing
            } catch (java.security.NoSuchAlgorithmException e) {//do nothing
            } catch (java.security.InvalidKeyException e) {//do nothing
            }
        }

        /**
         * Encrypt given message with provided litteral key
         * @param str The message to encrypt
         * @return encrypted message
         */
        public String encrypt(String str) {
            try {
                // Encode the string into bytes using utf-8
                byte[] utf8 = str.getBytes("UTF8");
    
                // Encrypt
                byte[] enc = ecipher.doFinal(utf8);
    
                // Encode bytes to base64 to get a string
                //return new sun.misc.BASE64Encoder().encode(enc);
                return new String(Base64.encode(enc));
            } catch (javax.crypto.BadPaddingException e) {//do nothing
            } catch (IllegalBlockSizeException e) {//do nothing
            } catch (Exception e) {//do nothing
            }
            return null;
        }
    
        /**
         * Decrypt given message using literal key
         * @param str the String to decrypt
         * @return decrypted String
         */
        public String decrypt(String str) {
            try {
                // Decode base64 to get bytes
                //byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
                byte[] dec = Base64.decode(str);
    
                // Decrypt
                byte[] utf8 = dcipher.doFinal(dec);
    
                // Decode using utf-8
                return new String(utf8, "UTF8");
            } catch (Exception e) {
                try{dcipher.doFinal();}catch (Exception e2){//do nothing
                }
            }
            return null;
        }
    }
    

