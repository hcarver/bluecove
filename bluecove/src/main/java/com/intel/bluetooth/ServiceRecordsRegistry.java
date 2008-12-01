/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;

/**
 * Maps ServiceRecord to ConnectionNotifier.
 *
 * Used by ServiceRecordsRegistry.updateServiceRecord().
 *
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 */
public abstract class ServiceRecordsRegistry {

	/**
	 * Used to find ConnectionNotifier by ServiceRecord returned by
	 * LocalDevice.getRecord()
	 */
	// <ServiceRecordImpl, BluetoothConnectionNotifierServiceRecordAccess>
	private static Hashtable serviceRecordsMap = new Hashtable();

	private ServiceRecordsRegistry() {

	}

	static synchronized void register(BluetoothConnectionNotifierServiceRecordAccess notifier,
			ServiceRecordImpl serviceRecord) {
		serviceRecordsMap.put(serviceRecord, notifier);
	}

	static synchronized void unregister(ServiceRecordImpl serviceRecord) {
		serviceRecordsMap.remove(serviceRecord);
	}

	static synchronized int getDeviceServiceClasses() {
		int deviceServiceClasses = 0;
		for (Enumeration en = serviceRecordsMap.keys(); en.hasMoreElements();) {
			ServiceRecordImpl serviceRecord = (ServiceRecordImpl) en.nextElement();
			deviceServiceClasses |= serviceRecord.deviceServiceClasses;
		}
		return deviceServiceClasses;
	}

	public static void updateServiceRecord(ServiceRecord srvRecord) throws ServiceRegistrationException {
		BluetoothConnectionNotifierServiceRecordAccess owner;
		synchronized (ServiceRecordsRegistry.class) {
			owner = (BluetoothConnectionNotifierServiceRecordAccess) serviceRecordsMap.get(srvRecord);
		}
		if (owner == null) {
			throw new IllegalArgumentException("Service record is not registered");
		}
		owner.updateServiceRecord(false);
	}
}
