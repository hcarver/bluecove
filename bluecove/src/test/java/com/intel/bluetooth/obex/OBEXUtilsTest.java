/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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

import junit.framework.TestCase;

public class OBEXUtilsTest extends TestCase {
	
	public void testBytesLoHi() {
		assertEquals("loByte", (byte)0x77, OBEXUtils.loByte(0x1077));
		assertEquals("loByte", (byte)0x81, OBEXUtils.loByte(0x1081));
		
		assertEquals("hiByte", (byte)0x10, OBEXUtils.hiByte(0x1077));
		assertEquals("hiByte", (byte)0x95, OBEXUtils.hiByte(0x9581));
	}
	
	private void validateBytesToShort(int i) {
		byte[] b = new byte[2];
		b[0] = OBEXUtils.hiByte(i);
		b[1] = OBEXUtils.loByte(i);
		int r = OBEXUtils.bytesToShort(b[0], b[1]);
		assertEquals("short from bytes", i, r);
	}
	
	public void testBytesToShort() {
		validateBytesToShort(0x0A);
		validateBytesToShort(0x80);
		validateBytesToShort(0xF7);
		validateBytesToShort(0x0100);
		validateBytesToShort(0x0102);
		validateBytesToShort(0xFB00);
		validateBytesToShort(0xBCEF);
	}
	
	public void testUTF() throws IOException {
		String value = "BlueCove";
		assertEquals("UTF16 ASCI String", value, new String(OBEXUtils.getUTF16BytesSimple(value), "UTF-16BE"));
		assertEquals("UTF16 ASCI String", value, OBEXUtils.newStringUTF16Simple(OBEXUtils.getUTF16Bytes(value)));
		
		value = "\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";
		assertEquals("UTF16 Rus String", value, new String(OBEXUtils.getUTF16BytesSimple(value), "UTF-16BE"));
		assertEquals("UTF16 Rus String", value, OBEXUtils.newStringUTF16Simple(OBEXUtils.getUTF16Bytes(value)));
	}

}
