/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.bluetooth.LocalDevice;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * @author vlads
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
