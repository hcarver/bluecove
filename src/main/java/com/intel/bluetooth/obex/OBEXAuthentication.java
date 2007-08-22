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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.obex.Authenticator;
import javax.obex.PasswordAuthentication;
import javax.obex.ServerRequestHandler;

/**
 * @author vlads
 *
 */
class OBEXAuthentication {

	private static byte[] privateKey;
	
	private static long uniqueTimestamp = 0;
	
	private static final byte column[] = { ':' };
	
	private static class Challenge {
		
		String realm; 
		
		boolean userID;
		
		boolean access;
		
		byte nonce[];
		
		Challenge(byte data[]) throws IOException {
			this.read(data);
		}
		
		Challenge(String realm, boolean userID, boolean access, byte[] nonce) {
			this.realm = realm;
			this.userID = userID;
			this.access = access;
			this.nonce = nonce;
		}

		
		byte[] write() {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			
			buf.write(0x00); // Tag
			buf.write(0x10); // Len
			buf.write(nonce, 0, 0x10);
			
			byte options = (byte)((userID ? 1 : 0) | (access ? 2 : 0));
			buf.write(0x01); // Tag
			buf.write(0x01); // Len
			buf.write(options);
			
			if (realm != null) {
				byte realmArray[];
				byte charSetCode;
				try {
					realmArray = OBEXUtils.getUTF16Bytes(realm);
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
		
		void read(byte data[]) throws IOException {
			for (int i = 0; i < data.length;) {
				int tag = data[i] & 0xFF;
				int len = data[i + 1] & 0xFF;
				i += 2;
				switch(tag) {
				case 0:
					if (len != 0x10) {
						throw new IOException("OBEX Digest Challenge error in tag Nonce");
					}
					nonce = new byte[0x10];
	                System.arraycopy(data, i, nonce, 0, 0x10);
	                break;
				case 1:
					byte options = data[i];
					userID = ((options & 1) != 0);
					access = ((options & 2) != 0);
					break;
				case 2:
					int charSetCode = data[i] & 0xFF;
					byte chars[] = new byte[len - 1];
					System.arraycopy(data, i + 1, chars, 0, chars.length);
					if (charSetCode == 0xFF) {
						realm = OBEXUtils.newStringUTF16(chars);
					} else if (charSetCode == 0) {
						realm = new String(chars, "ASCII");
					} else if (charSetCode <= 9) {
						realm = new String(chars, "ISO-8859-" + charSetCode);
					} else {
                        throw new UnsupportedEncodingException();
					}
					break;
				}
				i += len;
			}
		}
	
	}
	
	private static class DigestResponse {
		
		byte requestDigest[];
		
		byte userName[];
		
		byte nonce[];
		
		byte[] write() {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			
			buf.write(0x00); // Tag
			buf.write(0x10); // Len
			buf.write(requestDigest, 0, 0x10);
			
			if (userName != null) {
				buf.write(0x01); // Tag
				buf.write(userName.length); // Len
				buf.write(userName, 0, userName.length);
			}
			
			buf.write(0x02); // Tag
			buf.write(0x10); // Len
			buf.write(nonce, 0, 0x10);
			
			return buf.toByteArray();			
		}
		
		void read(byte data[])  throws IOException {
			for (int i = 0; i < data.length;) {
				int tag = data[i] & 0xFF;
				int len = data[i + 1] & 0xFF;
				i += 2;
				switch(tag) {
				case 0:
					if (len != 0x10) {
						throw new IOException("OBEX Digest Response error in tag request-digest");
					}
					requestDigest = new byte[0x10];
	                System.arraycopy(data, i, requestDigest, 0, 0x10);
	                break;
				case 1:
					userName = new byte[len];
					System.arraycopy(data, i, userName, 0, userName.length);
					break;
				case 2:
					if (len != 0x10) {
						throw new IOException("OBEX Digest Response error in tag Nonce");
					}
					nonce = new byte[0x10];
	                System.arraycopy(data, i, nonce, 0, 0x10);
	                break;
				}
				i += len;
			}
		}
	}
	
	static byte[] createChallenge(String realm, boolean userID, boolean access) {
		Challenge challenge = new Challenge(realm, userID, access, createNonce());
		return challenge.write();
	}

	static void handleAuthenticationResponse(OBEXHeaderSetImpl incomingHeaders, Authenticator authenticator, ServerRequestHandler serverHandler) throws IOException {
		for (Enumeration iter = incomingHeaders.getAuthenticationResponses(); iter.hasMoreElements();) {
			byte[] authResponse = (byte[]) iter.nextElement();
			DigestResponse dr = new DigestResponse();
			dr.read(authResponse);
			byte[] password = authenticator.onAuthenticationResponse(dr.userName);
			if (password == null) {
				throw new IOException("authentication request failed");
			}
			MD5DigestWrapper md5 = new MD5DigestWrapper();
			md5.update(dr.nonce);
			md5.update(column);
			md5.update(password);
			if (!equals(dr.requestDigest, md5.digest())) {
				if (serverHandler != null) {
					serverHandler.onAuthenticationFailure(dr.userName);
				} else {
					throw new IOException ("Authentication failure");
				}
			}
		}
	}
	
	public static void handleAuthenticationChallenge(OBEXHeaderSetImpl incomingHeaders, OBEXHeaderSetImpl replyHeaders, Authenticator authenticator) throws IOException {
		for (Enumeration iter = incomingHeaders.getAuthenticationChallenges(); iter.hasMoreElements();) {
			byte[] authChallenge = (byte[]) iter.nextElement();
			Challenge challenge = new Challenge(authChallenge);
			PasswordAuthentication pwd = authenticator.onAuthenticationChallenge(challenge.realm, challenge.userID, challenge.access);
			DigestResponse dr = new DigestResponse();
			dr.nonce = challenge.nonce;
			if (challenge.userID) {
				dr.userName = pwd.getUserName();
			}
			MD5DigestWrapper md5 = new MD5DigestWrapper();
			md5.update(dr.nonce);
			md5.update(column);
			md5.update(pwd.getPassword());
			dr.requestDigest = md5.digest();
			replyHeaders.addAuthenticationResponse(dr.write());
		}
	}
	
	private static synchronized byte[] createNonce() {
		MD5DigestWrapper md5 = new MD5DigestWrapper();
		md5.update(createTimestamp());
		md5.update(column);
		md5.update(getPrivateKey());
		return md5.digest();
	}
	
	private static boolean equals(byte[] digest1, byte[] digest2) {
		for (int i = 0; i < 0x10; i++) {
			if (digest1[i] != digest2[i]) {
				return false;
			}
		}
		return true;
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
