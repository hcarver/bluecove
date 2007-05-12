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
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

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

	//	 --- Device Inquiry
	
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		BlueCoveImpl.instance().getBluetoothPeer().initialized();
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		return BlueCoveImpl.instance().getBluetoothPeer().cancelInquiry();
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return BlueCoveImpl.instance().getBluetoothPeer().runDeviceInquiry(startedNotify, accessCode, listener);
	}

	//	 --- Service search 
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		int[] handles = BlueCoveImpl.instance().getBluetoothPeer().runSearchServices(startedNotify, uuidSet, Long.parseLong(device.getBluetoothAddress(), 16));
		if (handles == null) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		} else if (handles.length > 0) {
			ServiceRecord[] records = new ServiceRecordImpl[handles.length];

			for (int i = 0; i < handles.length; i++) {
				records[i] = new ServiceRecordImpl(device, handles[i]);
				try {
					records[i].populateRecord(new int[] { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004 });
					if (attrSet != null) {
						records[i].populateRecord(attrSet);
					}
				} catch (Exception e) {
				}
			}
			listener.servicesDiscovered(0, records);
			return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
		} else {
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}
	
	public boolean cancelServiceSearch(int transID) {
		if (NotImplementedError.enabled) {
			throw new NotImplementedError();
		} else {
			return false;
		}
	}

}
