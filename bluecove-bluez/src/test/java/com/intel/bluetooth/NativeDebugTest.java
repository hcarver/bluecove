/**
 *  BlueCove BlueZ module - Java library for Bluetooth on Linux
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

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.DebugLog.LoggerAppender;

/**
 * 
 */
public class NativeDebugTest extends NativeTestCase implements LoggerAppender {

	protected void setUp() throws Exception {
		DebugLog.addAppender(this);
	}

	protected void tearDown() throws Exception {
		DebugLog.removeAppender(this);
	}

	private String lastMessage;

	public void testDebug() throws BluetoothStateException {
		BluetoothStack anyStack = new BluetoothStackBlueZDBus();
		BlueCoveImpl.loadNativeLibraries(anyStack);

		anyStack.enableNativeDebug(DebugLog.class, true);
		DebugLog.setDebugEnabled(true);

		BluetoothStackBlueZDBusNativeTests.testDebug(0, null);
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message"));

		BluetoothStackBlueZDBusNativeTests.testDebug(1, "test-message");
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message[test-message]"));
		lastMessage = null;

		BluetoothStackBlueZDBusNativeTests.testDebug(2, "test-message");
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message[test-message],[test-message]"));
		lastMessage = null;

		BluetoothStackBlueZDBusNativeTests.testDebug(3, "test-message");
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message[test-message],[test-message],[3]"));
	}

	public void appendLog(int level, String message, Throwable throwable) {
		lastMessage = message;
	}

}
