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

import java.io.IOException;

import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.BluetoothStateException;

import junit.framework.AssertionFailedError;

/**
 *
 *
 */
public class NativeExceptionTest extends NativeTestCase {

	private void verify(int ntype, Throwable e) {
		try {
			NativeTestInterfaces.testThrowException(ntype);
			 fail("Should raise an Exception " + e); 
		} catch (Throwable t) {
			if (t instanceof AssertionFailedError) {
				throw (AssertionFailedError)t;
			}
			assertEquals("Exception class", e.getClass().getName(), t.getClass().getName());
			assertEquals("Exception message", e.getMessage(), t.getMessage());
			if (t instanceof BluetoothConnectionException ) {
				assertEquals("Exception getStatus", ((BluetoothConnectionException)e).getStatus(), ((BluetoothConnectionException)t).getStatus());	
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
		//	Throw Exception two times in a row. Second Exception ignored
		verify(22, new Exception("22.1"));
	}
}
