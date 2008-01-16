/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 * @author vlads
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

	static void register(BluetoothConnectionNotifierServiceRecordAccess notifier, ServiceRecordImpl serviceRecord) {
		serviceRecordsMap.put(serviceRecord, notifier);
	}

	static void unregister(ServiceRecordImpl serviceRecord) {
		serviceRecordsMap.remove(serviceRecord);
	}

	static int getDeviceServiceClasses() {
		int deviceServiceClasses = 0;
		for (Enumeration en = serviceRecordsMap.keys(); en.hasMoreElements();) {
			ServiceRecordImpl serviceRecord = (ServiceRecordImpl) en.nextElement();
			deviceServiceClasses |= serviceRecord.deviceServiceClasses;
		}
		return deviceServiceClasses;
	}

	public static void updateServiceRecord(ServiceRecord srvRecord) throws ServiceRegistrationException {
		BluetoothConnectionNotifierServiceRecordAccess owner = (BluetoothConnectionNotifierServiceRecordAccess) serviceRecordsMap
				.get(srvRecord);
		if (owner == null) {
			throw new IllegalArgumentException("Service record is not registered");
		}
		owner.updateServiceRecord(false);
	}
}
