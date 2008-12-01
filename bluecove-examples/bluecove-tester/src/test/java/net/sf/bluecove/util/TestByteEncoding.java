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
package net.sf.bluecove.util;

import junit.framework.TestCase;

/**
 * 
 */
public class TestByteEncoding extends TestCase {

	private void validateLong(long l) {
		byte[] b = new byte[8];
		IOUtils.long2Bytes(l, 8, b, 0);
		long l2 = IOUtils.bytes2Long(b, 0, 8);
		assertEquals("bytes of " + l, l, l2);
	}

	public void testLongToBytes() {
		validateLong(1);
		validateLong(-1);
		validateLong(0x0100);
		validateLong(-0x0100);
		validateLong(0xF000);
		validateLong(0xF10000);
		validateLong(0xF1000000);
		validateLong(0xBCfffffAffl);
	}
}
