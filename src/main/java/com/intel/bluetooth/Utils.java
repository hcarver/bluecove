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

public abstract class Utils {

	public static byte[] UUIDToByteArray(String uuidStringValue) {
		byte[] uuidValue = new byte[16];
		for (int i = 0; i < 16; i++) {
			uuidValue[i] = (byte) Integer.parseInt(uuidStringValue.substring(i * 2, i * 2 + 2), 16);
		}
		return uuidValue;
	}
	
	public static byte[] UUIDToByteArray(UUID uuid) {
		return UUIDToByteArray(uuid.toString());
	}
	
	public static String UUIDByteArrayToString(byte[] uuidValue) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < uuidValue.length; i++) {
			buf.append(Integer.toHexString(uuidValue[i] >> 4 & 0xf));
			buf.append(Integer.toHexString(uuidValue[i] & 0xf));
		}
		return buf.toString();
	}
	
	public static int UUIDTo16Bit(UUID uuid) {
		if (uuid == null) {
			return -1;
		}
		String str = uuid.toString().toUpperCase();
		int shortIdx = str.indexOf(BluetoothConsts.SHORT_UUID_BASE);
		if ((shortIdx != -1) && (shortIdx + BluetoothConsts.SHORT_UUID_BASE.length() == str.length())) {
			// This is short 16-bit UUID
			return Integer.parseInt(str.substring(0, shortIdx), 16);
		}
		return -1;
	}
	
	public static boolean is16Bit(UUID uuid) {
		return (UUIDTo16Bit(uuid) != -1);
	}
	
	public static void j2meUsagePatternDellay() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	}
}
