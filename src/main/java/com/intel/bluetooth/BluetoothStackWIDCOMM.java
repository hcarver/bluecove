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

import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;

public class BluetoothStackWIDCOMM implements BluetoothStack {

	private Vector deviceDiscoveryListeners = new Vector/*<DiscoveryListener>*/();
	
	BluetoothStackWIDCOMM() {
	}
	
	public native String getLocalDeviceBluetoothAddress();

	public native String getLocalDeviceName();

	// ---------- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		deviceDiscoveryListeners.addElement(listener);
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}
	
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		try {
			return runDeviceInquiryImpl(startedNotify, accessCode, listener);
		} finally {
			deviceDiscoveryListeners.removeElement(listener);
		}
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName) {
		DebugLog.debug("deviceDiscoveredCallback", deviceName);
		listener.deviceDiscovered(new RemoteDeviceImpl(deviceAddr, deviceName), new DeviceClass(deviceClass));			
	}

	private native boolean deviceInquiryCancelImpl();
	
	public boolean cancelInquiry(DiscoveryListener listener) {
		if (!deviceDiscoveryListeners.removeElement(listener)) {
			return false;	
		}
		return deviceInquiryCancelImpl();
	}

}
