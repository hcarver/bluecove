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
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;

/**
 * @author vlads
 * 
 */
class EmulatorSearchServices implements SearchServicesRunnable {

	private EmulatorLocalDevice localDevice;

	private BluetoothStack bluetoothStack;

	EmulatorSearchServices(EmulatorLocalDevice localDevice, BluetoothStack bluetoothStack) {
		this.localDevice = localDevice;
		this.bluetoothStack = bluetoothStack;
	}

	public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device,
			DiscoveryListener listener) throws BluetoothStateException {
		String[] uuidSetStrings = new String[uuidSet.length];
		for (int i = 0; i < uuidSet.length; i++) {
			uuidSetStrings[i] = uuidSet[i].toString();
		}
		sst.searchServicesStartedCallback();
		try {
			long remoteDeviceAddress = RemoteDeviceHelper.getAddress(device);
			long[] handles = localDevice.getDeviceManagerService().searchServices(remoteDeviceAddress, uuidSetStrings);
			if (sst.isTerminated()) {
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			}
			if (handles == null) {
				return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
			}
			if (handles.length != 0) {

				ServiceRecordImpl[] records = new ServiceRecordImpl[handles.length];
				int[] attrIDs = sst.getAttrSet();
				for (int i = 0; i < handles.length; i++) {
					records[i] = new ServiceRecordImpl(bluetoothStack, sst.getDevice(), handles[i]);
					populateServicesRecordAttributeValues(localDevice, records[i], attrIDs, remoteDeviceAddress,
							handles[i]);
				}

				DebugLog.debug("SearchServices finished", sst.getTransID());
				listener.servicesDiscovered(sst.getTransID(), records);
			}

			if (sst.isTerminated()) {
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			} else if (handles.length != 0) {
				return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
			} else {
				return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
			}
		} catch (Throwable e) {
			DebugLog.debug("SearchServices " + sst.getTransID(), e);
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
	}

	static boolean populateServicesRecordAttributeValues(EmulatorLocalDevice localDevice,
			ServiceRecordImpl serviceRecord, int[] attrIDs, long remoteDeviceAddress, long handle) throws IOException {
		byte[] blob = localDevice.getDeviceManagerService().getServicesRecordBinary(remoteDeviceAddress, handle);
		ServiceRecordImpl temp = new ServiceRecordImpl(null, null, handle);
		temp.loadByteArray(blob);
		boolean anyRetrived = false;
		for (int i = 0; i < attrIDs.length; i++) {
			int id = attrIDs[i];
			DataElement element = temp.getAttributeValue(id);
			serviceRecord.populateAttributeValue(id, element);
			if (element != null) {
				anyRetrived = true;
			}
		}
		return anyRetrived;
	}

}
