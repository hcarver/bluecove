/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package com.intel.bluetooth.obex;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author vlads
 *
 */
class OBEXAuthentication {

	private static byte[] privateKey;
	
	private static long uniqueTimestamp = 0;
	
	static byte[] createChallenge(String realm, boolean userID, boolean access) {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		
		buf.write(0x00); // Tag
		buf.write(0x10); // Len
		buf.write(createNonce(), 0, 0x10);
		
		byte options = (byte)((userID ? 1 : 0) | (access ? 2 : 0));
		buf.write(0x01); // Tag
		buf.write(0x01); // Len
		buf.write(options);
		
		if (realm != null) {
			byte realmArray[];
			byte charSetCode;
			try {
				realmArray = realm.getBytes("UTF-16BE");
				charSetCode = -1; // 0xFF; Unicode
			} catch (UnsupportedEncodingException e) {
				try {
					realmArray = realm.getBytes("iso-8859-1");
				} catch (UnsupportedEncodingException e1) {
					realmArray = new byte[0];
				}
				charSetCode = 1; // iso-8859-1
			}
			buf.write(0x02); // Tag
			buf.write(realmArray.length + 1); // Len
			buf.write(charSetCode);
			buf.write(realmArray, 0, realmArray.length);
		}
		
		return buf.toByteArray();
	}
	
	private static synchronized byte[] createNonce() {
		MD5DigestWrapper md5 = new MD5DigestWrapper();
		md5.update(createTimestamp());
		md5.update(getPrivateKey());
		return md5.digest();
	}
	
	private static synchronized byte[] getPrivateKey() {
		if (privateKey != null) {
			return privateKey;
		}
		MD5DigestWrapper md5 = new MD5DigestWrapper();
		md5.update(createTimestamp());
		privateKey = md5.digest();
		return privateKey;
	}
	
	private static synchronized byte[] createTimestamp() {
		long t = System.currentTimeMillis();
		if (t <= uniqueTimestamp) {
			t = uniqueTimestamp + 1; 
		}
		uniqueTimestamp = t;
		byte[] buf = new byte[8];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = (byte)(t >> (buf.length - 1 << 3));
			t <<= 8;
		}
		return buf;
	}
	
}
