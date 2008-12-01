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
package com.intel.bluetooth;

import javax.bluetooth.UUID;

public class NativeConversionTest extends NativeTestCase {

	private void verifyUUID(final String uuidString) {
		UUID uuid = new UUID(uuidString, false);
		byte[] uuidValue = NativeTestInterfaces.testUUIDConversion(Utils.UUIDToByteArray(uuid));
		UUID uuid2 = new UUID(Utils.UUIDByteArrayToString(uuidValue), false);
		assertEquals("UUID converted by native code", uuid, uuid2);
	}
	
	public void testNativeUUID() {
		verifyUUID("B10C0BE1111111111111111111110001");
		verifyUUID("B1011114111115111111117111110001");
		verifyUUID("27012f0c68af4fbf8dbe6bbaf7ab651b");
	}
}
