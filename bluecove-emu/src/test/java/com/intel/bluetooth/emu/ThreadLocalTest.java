/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * 
 */
public class ThreadLocalTest extends TestCase {

	private static final String localAddress0 = "CAFE00000000";

	private static final String localAddress1 = "CAFE00000001";

	private static String address1;

	protected void tearDown() throws Exception {
		super.tearDown();
		EmulatorTestsHelper.stopInProcessServer();
	}

	public void testSingleThread() throws Exception {
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS, localAddress0);
		Assert.assertEquals("Local bt address ", localAddress0, LocalDevice.getLocalDevice().getBluetoothAddress());
		BlueCoveImpl.useThreadLocalBluetoothStack();

		Thread t1 = new Thread() {
			public void run() {
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS, localAddress1);
				try {
					address1 = LocalDevice.getLocalDevice().getBluetoothAddress();
				} catch (BluetoothStateException e) {
					address1 = null;
				}
			}
		};
		t1.start();
		t1.join();
		Assert.assertEquals("Second address", localAddress1, address1);
		Assert.assertEquals("First is not broken", localAddress0, LocalDevice.getLocalDevice().getBluetoothAddress());
	}

	public void testConfigProperty() throws Exception {
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS, localAddress0);
		Assert.assertEquals("Local bt address ", localAddress0, LocalDevice.getLocalDevice().getBluetoothAddress());
		BlueCoveImpl.useThreadLocalBluetoothStack();

		Thread t1 = new Thread() {
			public void run() {
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
				try {
					address1 = LocalDevice.getLocalDevice().getBluetoothAddress();
				} catch (BluetoothStateException e) {
					address1 = null;
				}
			}
		};
		t1.start();
		t1.join();
		Assert.assertNotNull("Second address", address1);
		Assert.assertEquals("First is not broken", localAddress0, LocalDevice.getLocalDevice().getBluetoothAddress());
	}

	public void testAlreadyInitialized() throws Exception {
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
		Assert.assertNotNull("Local bt address ", LocalDevice.getLocalDevice().getBluetoothAddress());

		try {
			BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
			Assert.fail("Should thow AlreadyInitialized");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testDuplicateAddress() throws Exception {
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS, localAddress0);
		Assert.assertEquals("Local bt address ", localAddress0, LocalDevice.getLocalDevice().getBluetoothAddress());
		BlueCoveImpl.useThreadLocalBluetoothStack();
		address1 = "X";
		Thread t1 = new Thread() {
			public void run() {
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
				BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS, localAddress0);
				try {
					address1 = LocalDevice.getLocalDevice().getBluetoothAddress();
				} catch (BluetoothStateException e) {
					address1 = null;
				}
			}
		};
		t1.start();
		t1.join();
		Assert.assertNull("Second not avalable", address1);
		Assert.assertEquals("First is not broken", localAddress0, LocalDevice.getLocalDevice().getBluetoothAddress());
	}
}
