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
package net.sf.bluecove;

import java.util.Hashtable;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import net.sf.bluecove.util.TimeStatistic;

/**
 * @author vlads
 * 
 */
public class RemoteDeviceInfo {

	public static Hashtable devices = new Hashtable();

	public static Hashtable services = new Hashtable();

	public String name;

	public RemoteDevice remoteDevice;

	public int discoveredCount;

	public long discoveredFirstTime;

	public long discoveredLastTime;

	private TimeStatistic deviceDiscovery = new TimeStatistic();

	public static TimeStatistic deviceInquiryDuration = new TimeStatistic();

	private TimeStatistic serviceSearch = new TimeStatistic();

	public static TimeStatistic allServiceSearch = new TimeStatistic();

	public long serviceDiscoveredFirstTime;

	public long serviceDiscoveredLastTime;

	public TimeStatistic serviceDiscovered = new TimeStatistic();

	public long variableData;

	public long variableDataCheckLastTime;

	public boolean variableDataUpdated = false;

	public static synchronized void clear() {
		devices = new Hashtable();
		services = new Hashtable();
		allServiceSearch.clear();
		deviceInquiryDuration.clear();
	}

	public static synchronized RemoteDeviceInfo getDevice(RemoteDevice remoteDevice) {
		String addr = remoteDevice.getBluetoothAddress().toUpperCase();
		RemoteDeviceInfo devInfo = (RemoteDeviceInfo) devices.get(addr);
		if (devInfo == null) {
			devInfo = new RemoteDeviceInfo();
			devInfo.name = TestResponderClient.niceDeviceName(addr);
			devInfo.remoteDevice = remoteDevice;
			devices.put(addr, devInfo);
		}
		return devInfo;
	}

	public static synchronized void deviceFound(RemoteDevice remoteDevice) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		long now = System.currentTimeMillis();
		if (devInfo.discoveredCount == 0) {
			devInfo.discoveredFirstTime = now;
			devInfo.deviceDiscovery.add(0);
		} else {
			devInfo.deviceDiscovery.add(now - devInfo.discoveredLastTime);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.discoveredCount++;
		devInfo.discoveredLastTime = now;
	}

	public static synchronized void deviceServiceFound(RemoteDevice remoteDevice, long variableData) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		long now = System.currentTimeMillis();
		if (devInfo.serviceDiscovered.count == 0) {
			devInfo.serviceDiscoveredFirstTime = now;
			devInfo.serviceDiscovered.add(0);
		} else {
			devInfo.serviceDiscovered.add(now - devInfo.serviceDiscoveredLastTime);
		}
		devInfo.remoteDevice = remoteDevice;
		devInfo.serviceDiscoveredLastTime = now;
		// if (variableData != 0) {
		// long frequencyMSec = now - devInfo.variableDataCheckLastTime;
		// if ((devInfo.variableData != 0) && (frequencyMSec > 1000 * 120)) {
		// devInfo.variableDataCheckLastTime = now;
		// boolean er = false;
		// if (variableData == devInfo.variableData) {
		// Logger.warn("not updated " + variableData);
		// TestResponderClient.failure.addFailure("not updated " + variableData
		// + " on " + devInfo.name);
		// er = true;
		// }
		// if (!er) {
		// devInfo.variableDataUpdated = true;
		// Logger.info("Var info updated, " + variableData);
		// }
		// } else if (devInfo.variableData != variableData) {
		// if (devInfo.variableData == 0) {
		// Logger.info("Var info set, " + variableData);
		// } else {
		// Logger.info("Var info updated, " + variableData);
		// }
		// devInfo.variableData = variableData;
		// devInfo.variableDataCheckLastTime = now;
		// devInfo.variableDataUpdated = true;
		// }
		// }
	}

	public static synchronized void searchServices(RemoteDevice remoteDevice, boolean found, long servicesSearch) {
		RemoteDeviceInfo devInfo = getDevice(remoteDevice);
		devInfo.serviceSearch.add(servicesSearch);
		allServiceSearch.add(servicesSearch);
	}

	public static void discoveryInquiryFinished(long discoveryInquiry) {
		deviceInquiryDuration.add(discoveryInquiry);
	}

	public static long allAvgDeviceInquiryDurationSec() {
		return deviceInquiryDuration.avgSec();
	}

	public static long allAvgServiceSearchDurationSec() {
		return allServiceSearch.avgSec();
	}

	public long avgDiscoveryFrequencySec() {
		return deviceDiscovery.avgSec();
	}

	public long avgServiceDiscoveryFrequencySec() {
		return serviceDiscovered.avgSec();
	}

	public long avgServiceSearchDurationSec() {
		return serviceSearch.durationMaxSec();
	}

	public long serviceSearchSuccessPrc() {
		if ((serviceSearch.count) == 0) {
			return 0;
		}
		return (100 * serviceDiscovered.count) / (serviceSearch.count);
	}

	public static void saveServiceURL(ServiceRecord serviceRecord) {
		services.put(serviceRecord.getConnectionURL(Configuration.getRequiredSecurity(), false), serviceRecord);
	}
}
