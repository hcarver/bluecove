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

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryListener;

public class BluetoothStackMicrosoft implements BluetoothStack {

	long bluetoothAddress = 0;
	
	BluetoothStackMicrosoft() {
		
	}
	
	public String getLocalDeviceBluetoothAddress() {
		BluetoothPeer bluetoothPeer = BlueCoveImpl.instance().getBluetoothPeer();
		String address;
		try {
			int socket = bluetoothPeer.socket(false, false);
			bluetoothPeer.bind(socket);
			bluetoothAddress = bluetoothPeer.getsockaddress(socket);
			address = Long.toHexString(bluetoothAddress);
			bluetoothPeer.close(socket);
		} catch (IOException e) {
			DebugLog.error("get local bluetoothAddress", e);
			address = "";
		}
		return "000000000000".substring(address.length()) + address;
	}

	public String getLocalDeviceName() {
		if (bluetoothAddress == 0) {
			getLocalDeviceBluetoothAddress();
		}
		return BlueCoveImpl.instance().getBluetoothPeer().getradioname(bluetoothAddress);
	}

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		BlueCoveImpl.instance().getBluetoothPeer().startInquiry(accessCode, listener);;
		return true;
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		return BlueCoveImpl.instance().getBluetoothPeer().cancelInquiry();
	}

	/**
	 * Not used now.
	 */
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return 0;
	}
}
