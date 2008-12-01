/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

import java.io.IOException;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 *
 * 
 */
public class OBEXAuthenticationTest extends TestCase {

	static public void assertEquals(String message, byte[] expected, byte[] actual) {
		if (expected == null) {
			Assert.assertNull(message, actual);
			return;
		}
		Assert.assertEquals(message + " byte[].length", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals(message + " byte[" + i + "]", expected[i], actual[i]);
		}
	}

	private byte[] md5digest(String md5text) {
		byte[] digest = new byte[0x10];
		for (int i = 0; i < 0x10; i++) {
			digest[i] = (byte) Integer.parseInt(md5text.substring((i * 2), ((i + 1) * 2)), 0x10);
		}
		return digest;
	}

	public void testMD5DigestWrapper() {
		MD5DigestWrapper md5 = new MD5DigestWrapper();
		byte[] data = new byte[] { 'a' };
		byte[] digestExpected = md5digest("0cc175b9c0f1b6a831c399e269772661");
		md5.update(data);
		byte[] digest = md5.digest();
		assertEquals("md5 digest.length", 0x10, digest.length);
		assertEquals("md5 digest", digestExpected, digest);

		md5 = new MD5DigestWrapper();

		md5.update(new byte[] { 'a' });
		md5.update(new byte[] { 'b' });
		md5.update(new byte[] { 'c' });
		digestExpected = md5digest("900150983cd24fb0d6963f7d28e17f72");
		digest = md5.digest();
		assertEquals("md5 digest.length", 0x10, digest.length);
		assertEquals("md5 digest", digestExpected, digest);
	}

	static public void assertEquals(String message, OBEXAuthentication.Challenge expected,
			OBEXAuthentication.Challenge actual) {
		assertEquals(message + " realm", expected.getRealm(), actual.getRealm());
		assertEquals(message + " userID", expected.isUserIdRequired(), actual.isUserIdRequired());
		assertEquals(message + " access", expected.isFullAccess(), actual.isFullAccess());
		assertEquals(message + " nonce", expected.nonce, actual.nonce);
	}

	public void testChallenge() throws IOException {
		byte[] digest = md5digest("0cc175b9c0f1b6a831c399e269772661");
		OBEXAuthentication.Challenge c1 = new OBEXAuthentication.Challenge("realm", true, true, digest);
		OBEXAuthentication.Challenge c2 = new OBEXAuthentication.Challenge(c1.write());
		assertEquals("Challenge write 1", c1, c2);

		c1 = new OBEXAuthentication.Challenge("realm2", false, true, digest);
		c2 = new OBEXAuthentication.Challenge(c1.write());
		assertEquals("Challenge write 2", c1, c2);

		c1 = new OBEXAuthentication.Challenge(null, false, false, digest);
		c2 = new OBEXAuthentication.Challenge(c1.write());
		assertEquals("Challenge write 3", c1, c2);
	}

	static public void assertEquals(String message, OBEXAuthentication.DigestResponse expected,
			OBEXAuthentication.DigestResponse actual) {
		assertEquals(message + " requestDigest", expected.requestDigest, actual.requestDigest);
		assertEquals(message + " userName", expected.userName, actual.userName);
		assertEquals(message + " nonce", expected.nonce, actual.nonce);
	}

	public void testDigestResponse() throws IOException {
		byte[] digest1 = md5digest("0cc175b9c0f1b6a831c399e269772661");
		byte[] digest2 = md5digest("900150983cd24fb0d6963f7d28e17f72");
		OBEXAuthentication.DigestResponse expected = new OBEXAuthentication.DigestResponse();
		OBEXAuthentication.DigestResponse actual = new OBEXAuthentication.DigestResponse();
		expected.nonce = digest1;
		expected.requestDigest = digest2;
		actual.read(expected.write());
		assertEquals("DigestResponse 1", expected, actual);

		expected = new OBEXAuthentication.DigestResponse();
		actual = new OBEXAuthentication.DigestResponse();
		expected.nonce = digest2;
		expected.userName = new byte[] { 'b', 's' };
		expected.requestDigest = digest1;
		actual.read(expected.write());
		assertEquals("DigestResponse 2", expected, actual);
	}
}
