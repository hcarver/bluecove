/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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

import junit.framework.Assert;
import junit.framework.TestCase;

public class DeviceManagerTest extends TestCase {

	DeviceManagerServiceImpl deviceManager;

	public void setUp() throws Exception {
		deviceManager = new DeviceManagerServiceImpl();
	}

	public void tearDown() throws Exception {
		deviceManager.shutdown();
	}

	public void testCreateNewDevice() throws Exception {
		HashSet<Long> uniqueAddresses = new HashSet<Long>();

		for (int i = 0; i < 20; i++) {
			DeviceDescriptor descriptor = deviceManager.createNewDevice(null, null);
			long addr = descriptor.getAddress();
			Assert.assertFalse("duplicateAddress", uniqueAddresses.contains(addr));
			uniqueAddresses.add(addr);
		}
	}

}
