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

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * @author vlads
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
