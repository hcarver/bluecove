/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Michael Lifshits
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;

/**
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
