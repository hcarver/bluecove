/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.RemoteDeviceHelper;

/**
 * 
 */
class DeviceSDP {

	private long address;

	private Hashtable<Long, ServicesDescriptor> services = new Hashtable<Long, ServicesDescriptor>();

	DeviceSDP(long address) {
		this.address = address;
	}

	synchronized void updateServiceRecord(long handle, ServicesDescriptor sdpData) {
		Long key = new Long(handle);
		boolean update = (services.get(key) != null);
		services.put(key, sdpData);

		String[] serviceUuidSet = sdpData.getUuidSet();
		for (int i = 0; i < serviceUuidSet.length; i++) {
			DebugLog.debug((update ? "Update" : "Create") + " Srv on "
					+ RemoteDeviceHelper.getBluetoothAddress(address) + " " + handle + " " + i + " "
					+ serviceUuidSet[i]);
		}
	}

	synchronized void removeServiceRecord(long handle) {
		ServicesDescriptor srv = (ServicesDescriptor) services.remove(new Long(handle));
		if (srv != null) {
			DebugLog.debug("Remove Srv on " + RemoteDeviceHelper.getBluetoothAddress(address) + " " + handle);
		}
	}

	ServicesDescriptor getServicesDescriptor(long handle) {
		return (ServicesDescriptor) services.get(new Long(handle));
	}

	synchronized long[] searchServices(String[] uuidSet) {
		Vector<Long> handles = new Vector<Long>();

		for (Enumeration<Long> iterator = services.keys(); iterator.hasMoreElements();) {
			Long key = (Long) iterator.nextElement();
			ServicesDescriptor service = (ServicesDescriptor) services.get(key);
			String[] serviceUuidSet = service.getUuidSet();
			// No duplicate values in any set!
			int match = 0;
			for (int i = 0; i < serviceUuidSet.length; i++) {
				for (int k = 0; k < uuidSet.length; k++) {
					if (uuidSet[k].equals(serviceUuidSet[i])) {
						match++;
						break;
					}
				}
			}
			if (match == uuidSet.length) {
				handles.addElement(key);
			}
		}

		long[] h = new long[handles.size()];
		int i = 0;
		for (Enumeration<Long> e = handles.elements(); e.hasMoreElements();) {
			h[i++] = ((Long) e.nextElement()).intValue();
		}
		return h;
	}
}
