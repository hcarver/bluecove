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
 *  @version $Id: NativeExceptionTest.java 1570 2008-01-16 22:15:56Z skarzhevskyy $
 */
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.BluetoothStateException;

import junit.framework.AssertionFailedError;

/**
 * @author vlads
 * 
 */
public class NativeExceptionTest extends NativeTestCase {

	private void verify(int ntype, Throwable e) {
		try {
			BluetoothStackBlueZNativeTests.testThrowException(ntype);
			fail("Should raise an Exception " + e);
		} catch (Throwable t) {
			if (t instanceof AssertionFailedError) {
				throw (AssertionFailedError) t;
			}
			assertEquals("Exception class", e.getClass().getName(), t.getClass().getName());
			assertEquals("Exception message", e.getMessage(), t.getMessage());
			if (t instanceof BluetoothConnectionException) {
				assertEquals("Exception getStatus", ((BluetoothConnectionException) e).getStatus(),
						((BluetoothConnectionException) t).getStatus());
			}
		}
	}

	public void testExceptions() {
		verify(0, new Exception("0"));
		verify(1, new Exception("1[str]"));
		verify(2, new IOException("2"));
		verify(3, new IOException("3[str]"));
		verify(4, new BluetoothStateException("4"));
		verify(5, new BluetoothStateException("5[str]"));
		verify(6, new RuntimeException("6"));
		verify(7, new BluetoothConnectionException(1, "7"));
		verify(8, new BluetoothConnectionException(2, "8[str]"));
	}

	public void testThrowTwoExceptions() {
		// Throw Exception two times in a row. Second Exception ignored
		verify(22, new Exception("22.1"));
	}
}
