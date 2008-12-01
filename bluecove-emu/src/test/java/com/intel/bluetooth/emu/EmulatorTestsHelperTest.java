/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.bluetooth.LocalDevice;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * 
 */
public class EmulatorTestsHelperTest extends TestCase {

	private static final String localAddress = "CAFE00000000";

	protected void tearDown() throws Exception {
		super.tearDown();
		EmulatorTestsHelper.stopInProcessServer();
	}

	public void testInitialization() throws Exception {

		EmulatorTestsHelper.startInProcessServer();
		EmulatorTestsHelper.useThreadLocalEmulator();

		Assert.assertFalse("Slect different address for tests", localAddress.equals(LocalDevice.getLocalDevice()
				.getBluetoothAddress()));

		// This remove all device and clear properties
		EmulatorTestsHelper.stopInProcessServer();

		// Start again and verify that previous stack was shuted down
		EmulatorTestsHelper.useThreadLocalEmulator(null, localAddress);

		Assert.assertEquals("Local bt address ", localAddress, LocalDevice.getLocalDevice().getBluetoothAddress());

	}

	private class ClientRunnable implements Runnable {

		HashSet<String> uniqueAddresses;

		boolean ok = false;

		Thread thread;

		public void run() {
			String addr;
			try {
				addr = LocalDevice.getLocalDevice().getBluetoothAddress();
			} catch (Throwable e) {
				Assert.fail(e.getMessage());
				return;
			}
			Assert.assertFalse("duplicateAddress", uniqueAddresses.contains(addr));
			uniqueAddresses.add(addr);
			ok = true;

		}
	}

	public void testRunNew() throws Exception {
		HashSet<String> uniqueAddresses = new HashSet<String>();
		List<ClientRunnable> clients = new Vector<ClientRunnable>();
		for (int i = 0; i < 20; i++) {
			ClientRunnable c = new ClientRunnable();
			c.uniqueAddresses = uniqueAddresses;
			c.thread = EmulatorTestsHelper.runNewEmulatorStack(c);
			clients.add(c);
		}
		for (ClientRunnable c : clients) {
			c.thread.join();
			Assert.assertTrue("Client Failed", c.ok);
		}
	}
}
