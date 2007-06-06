/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import javax.bluetooth.UUID;

import junit.framework.TestCase;

public class ConversionTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		
		//System.getProperties().put("bluecove.debug", "true");
		System.getProperties().put("bluecove.native.path", "./src/main/resources");
	}
	
	private void verifyUUID(final String uuidString) {
		UUID uuid = new UUID(uuidString, false);
		byte[] uuidValue = BluetoothPeer.testUUIDConversion(Utils.UUIDToByteArray(uuid));
		UUID uuid2 = new UUID(Utils.UUIDByteArrayToString(uuidValue), false);
		assertEquals("UUID converted by native code", uuid, uuid2);
	}
	
	public void testNativeUUID() {
		verifyUUID("B1011114111115111111117111110001");
		verifyUUID("27012f0c68af4fbf8dbe6bbaf7ab651b");
	}
}
