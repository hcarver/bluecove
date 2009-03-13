/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

/**
 * 
 */
public class BluetoothInquirer implements DiscoveryListener {

	public static final int MAJOR_COMPUTER = 0x0100;

	public static final int MAJOR_PHONE = 0x0200;

	public final UUID L2CAP = new UUID(0x0100);

	public final UUID RFCOMM = new UUID(0x0003);

	public final UUID OBEX = new UUID(0x0008);

	public final UUID OBEX_OBJECT_PUSH = new UUID(0x1105);

	public final UUID OBEX_FILE_TRANSFER = new UUID(0x1106);

	private Main mainInstance;

	boolean inquiring;

	int servicesSearchTransID = 0;

	Vector devices;

	String serviceURL;

	String serviceSearchOn;

	BluetoothInquirer(Main main) {
		mainInstance = main;
		devices = new Vector();
	}

	public boolean startInquiry() {
		devices.removeAllElements();
		inquiring = false;
		try {
			inquiring = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, this);
		} catch (Throwable e) {
			Logger.error(e);
			mainInstance.setStatus("Cannot start inquiry " + e.getMessage());
			return false;
		}
		if (!inquiring) {
			mainInstance.setStatus("Cannot start inquiry");
		}
		return inquiring;
	}

	public boolean startServiceSearch(RemoteDevice device, UUID obexUUID) {
		serviceURL = null;
		try {
			UUID[] searchUuidSet = new UUID[] { /* L2CAP, RFCOMM, */obexUUID };
			int[] attrIDs = new int[] { 0x0100 // Service name
			};
			serviceSearchOn = getFriendlyName(device);
			servicesSearchTransID = LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs,
					searchUuidSet, device, this);
		} catch (Throwable e) {
			Logger.error(e);
			mainInstance.setStatus("Cannot start inquiry " + e.getMessage());
			return false;
		}
		return servicesSearchTransID != 0;
	}

	static String getFriendlyName(RemoteDevice device) {
		try {
			String name = device.getFriendlyName(false);
			if ((name == null) || (name.length() == 0)) {
				return device.getBluetoothAddress();
			} else {
				return name;
			}
		} catch (IOException e) {
			return device.getBluetoothAddress();
		}
	}

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		if ((cod.getMajorDeviceClass() != MAJOR_COMPUTER) && (cod.getMajorDeviceClass() != MAJOR_PHONE)) {
			return;
		}
		devices.add(btDevice);
		try {
			String name = btDevice.getFriendlyName(false);
			mainInstance.setStatus("Found " + name + " " + btDevice.getBluetoothAddress());
		} catch (IOException ioe) {
			mainInstance.setStatus("Found " + btDevice.getBluetoothAddress());
		}
	}

	public void inquiryCompleted(int discType) {
		inquiring = false;
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		for (int i = 0; i < servRecord.length; i++) {
			String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
			if (url == null) {
				continue;
			}
			serviceURL = url;

			mainInstance.setStatus("found service on " + serviceSearchOn);
		}
	}

	public void serviceSearchCompleted(int transID, int respCode) {
		switch (respCode) {
		case SERVICE_SEARCH_ERROR:
			// log.info(" -> Error occurred while processing the service
			// search");
			mainInstance.setStatus("Error on " + serviceSearchOn);
			break;
		case SERVICE_SEARCH_TERMINATED:
			break;
		case SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
			// log.info(" -> Device not reachable");
			mainInstance.setStatus(serviceSearchOn + " not reachable");
			break;
		default:
			if (serviceURL == null) {
				// log.info(" -> OBEX Object Push service not found");
				mainInstance.setStatus(serviceSearchOn + " no services");
			}
		}
		servicesSearchTransID = 0;
	}

	public boolean searching() {
		return servicesSearchTransID != 0;
	}

	public String findOBEX(String btAddress) {
		RemoteDevice device = new RemoteDeviceExt(btAddress);
		this.serviceURL = null;
		if (!startServiceSearch(device, OBEX_OBJECT_PUSH)) {
			mainInstance.setStatus("Cannot start inquiry");
		} else {
			while (searching()) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			}
		}
		if (this.serviceURL == null) {
			if (!startServiceSearch(device, OBEX_FILE_TRANSFER)) {
				mainInstance.setStatus("Cannot start inquiry");
			} else {
				while (searching()) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
			}
		}
		return this.serviceURL;
	}

	/**
	 * 
	 */
	public void shutdown() {
		try {
			if (inquiring) {
				LocalDevice.getLocalDevice().getDiscoveryAgent().cancelInquiry(this);
			}
		} catch (BluetoothStateException ignore) {
		}
	}

}
