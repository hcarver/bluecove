/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth.obex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;

import javax.obex.Authenticator;
import javax.obex.PasswordAuthentication;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.DebugLog;

class OBEXAuthentication {

	private static byte[] privateKey;

	private static long uniqueTimestamp = 0;

	private static final byte COLUMN[] = { ':' };

	static class Challenge {

		private String realm;

		private boolean isUserIdRequired;

		private boolean isFullAccess;

		byte nonce[];

		Challenge(byte data[]) throws IOException {
			this.read(data);
		}

		Challenge(String realm, boolean isUserIdRequired, boolean isFullAccess, byte[] nonce) {
			this.realm = realm;
			this.isUserIdRequired = isUserIdRequired;
			this.isFullAccess = isFullAccess;
			this.nonce = nonce;
		}

		byte[] write() {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();

			buf.write(0x00); // Tag
			buf.write(0x10); // Len
			buf.write(nonce, 0, 0x10);

			byte options = (byte) ((isUserIdRequired ? 1 : 0) | ((!isFullAccess) ? 2 : 0));
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
			DebugLog.debug("authChallenge", data);
			for (int i = 0; i < data.length;) {
				int tag = data[i] & 0xFF;
				int len = data[i + 1] & 0xFF;
				i += 2;
				switch (tag) {
				case 0:
					if (len != 0x10) {
						throw new IOException("OBEX Digest Challenge error in tag Nonce");
					}
					nonce = new byte[0x10];
					System.arraycopy(data, i, nonce, 0, 0x10);
					break;
				case 1:
					byte options = data[i];
					DebugLog.debug("authChallenge options", options);
					isUserIdRequired = ((options & 1) != 0);
					isFullAccess = ((options & 2) == 0);
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
						DebugLog.error("Unsupported charset code " + charSetCode + " in Challenge");
						// throw new UnsupportedEncodingException("charset code
						// " + charSetCode);
						// BUG on SE K790a
						realm = new String(chars, 0, len - 1, "ASCII");
					}
					break;
				default:
					DebugLog.error("invalid authChallenge tag " + tag);
				}
				i += len;
			}
		}

		public boolean isUserIdRequired() {
			return isUserIdRequired;
		}

		public boolean isFullAccess() {
			return isFullAccess;
		}

		public String getRealm() {
			return realm;
		}

	}

	static class DigestResponse {

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

		void read(byte data[]) throws IOException {
			for (int i = 0; i < data.length;) {
				int tag = data[i] & 0xFF;
				int len = data[i + 1] & 0xFF;
				i += 2;
				switch (tag) {
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

	static byte[] createChallenge(String realm, boolean isUserIdRequired, boolean isFullAccess) {
		Challenge challenge = new Challenge(realm, isUserIdRequired, isFullAccess, createNonce());
		return challenge.write();
	}

	static boolean handleAuthenticationResponse(OBEXHeaderSetImpl incomingHeaders, Authenticator authenticator,
			ServerRequestHandler serverHandler, Vector authChallengesSent) throws IOException {
		if (!incomingHeaders.hasAuthenticationResponses()) {
			return false;
		}
		for (Enumeration iter = incomingHeaders.getAuthenticationResponses(); iter.hasMoreElements();) {
			byte[] authResponse = (byte[]) iter.nextElement();
			DigestResponse dr = new DigestResponse();
			dr.read(authResponse);
			DebugLog.debug("got nonce", dr.nonce);

			// Verify that we did sent the Challenge that triggered this Responses
			Challenge challengeSent = null;
			for (Enumeration challengeIter = authChallengesSent.elements(); challengeIter.hasMoreElements();) {
				Challenge c = (Challenge) challengeIter.nextElement();
				if (equals(c.nonce, dr.nonce)) {
					challengeSent = c;
					break;
				}
			}
			if (challengeSent == null) {
				throw new IOException("Authentication response for unknown challenge");
			}

			byte[] password = authenticator.onAuthenticationResponse(dr.userName);
			if (password == null) {
				throw new IOException("Authentication request failed, password is not supplied");
			}
			// DebugLog.debug("authenticate using password", new String(password));
			// DebugLog.debug("password used", password);
			MD5DigestWrapper md5 = new MD5DigestWrapper();
			md5.update(dr.nonce);
			md5.update(COLUMN);
			md5.update(password);
			byte[] claulated = md5.digest();
			if (!equals(dr.requestDigest, claulated)) {
				DebugLog.debug("got digest", dr.requestDigest);
				DebugLog.debug("  expected", claulated);
				if (serverHandler != null) {
					serverHandler.onAuthenticationFailure(dr.userName);
				} else {
					throw new IOException("Authentication failure");
				}
			} else {
				return true;
			}
		}
		return false;
	}

	static void handleAuthenticationChallenge(OBEXHeaderSetImpl incomingHeaders, OBEXHeaderSetImpl replyHeaders,
			Authenticator authenticator) throws IOException {
		if (!incomingHeaders.hasAuthenticationChallenge()) {
			return;
		}
		for (Enumeration iter = incomingHeaders.getAuthenticationChallenges(); iter.hasMoreElements();) {
			byte[] authChallenge = (byte[]) iter.nextElement();
			Challenge challenge = new Challenge(authChallenge);
			PasswordAuthentication pwd = authenticator.onAuthenticationChallenge(challenge.getRealm(), challenge
					.isUserIdRequired(), challenge.isFullAccess());
			DigestResponse dr = new DigestResponse();
			dr.nonce = challenge.nonce;
			DebugLog.debug("got nonce", dr.nonce);
			if (challenge.isUserIdRequired()) {
				dr.userName = pwd.getUserName();
			}
			MD5DigestWrapper md5 = new MD5DigestWrapper();
			md5.update(dr.nonce);
			md5.update(COLUMN);
			md5.update(pwd.getPassword());
			dr.requestDigest = md5.digest();
			// DebugLog.debug("password", new String(pwd.getPassword()));
			// DebugLog.debug("password used", pwd.getPassword());
			DebugLog.debug("send digest", dr.requestDigest);
			replyHeaders.addAuthenticationResponse(dr.write());
		}
	}

	private static synchronized byte[] createNonce() {
		MD5DigestWrapper md5 = new MD5DigestWrapper();
		md5.update(createTimestamp());
		md5.update(COLUMN);
		md5.update(getPrivateKey());
		return md5.digest();
	}

	static boolean equals(byte[] digest1, byte[] digest2) {
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
			buf[i] = (byte) (t >> (buf.length - 1 << 3));
			t <<= 8;
		}
		return buf;
	}

}
