/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;

import junit.framework.TestCase;

import com.intel.bluetooth.DebugLog.LoggerAppender;

/**
 * @author vlads
 * 
 */
public class NativeDebugTest extends TestCase implements LoggerAppender {

	protected void setUp() throws Exception {
		DebugLog.addAppender(this);
	}

	protected void tearDown() throws Exception {
		DebugLog.removeAppender(this);
	}

	protected boolean needDllWIDCOMM() {
		return false;
	}

	String lastMessage;

	public void testDebug() throws BluetoothStateException {
		BluetoothStack anyStack;
		if (NativeLibLoader.getOS() == NativeLibLoader.OS_MAC_OS_X) {
			anyStack = new BluetoothStackOSX();
		} else if (needDllWIDCOMM()) {
			anyStack = new BluetoothStackWIDCOMM();
		} else {
			anyStack = new BluetoothStackMicrosoft();
		}
		BlueCoveImpl.loadNativeLibraries(anyStack);
		anyStack.enableNativeDebug(DebugLog.class, true);
		DebugLog.setDebugEnabled(true);

		NativeTestInterfaces.testDebug(0, null);
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message"));

		NativeTestInterfaces.testDebug(1, "test-message");
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message[test-message]"));
		lastMessage = null;

		NativeTestInterfaces.testDebug(2, "test-message");
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message[test-message],[test-message]"));
		lastMessage = null;

		NativeTestInterfaces.testDebug(3, "test-message");
		assertNotNull("Debug recived", lastMessage);
		assertTrue("Debug {" + lastMessage + "}", lastMessage.startsWith("message[test-message],[test-message],[3]"));
	}

	public void appendLog(int level, String message, Throwable throwable) {
		lastMessage = message;
	}

}
