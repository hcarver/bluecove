/**
 *  BlueCove - Java library for Bluetooth
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
 *  @version $Id$
 */
package com.intel.bluetooth.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import com.intel.bluetooth.DebugLog;

/**
 * This class provides a stand-alone test for Blue Cove
 * 
 */
public class SimpleDiscovery {

	public static void main(String[] args) {

		EnvSettings.setSystemProperties();

		LocalDevice l;
		try {
			l = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e) {
			System.err.println("Cannot get local device: " + e);
			return;
		}

		System.out.println("Local btaddr is " + l.getBluetoothAddress());
		System.out.println("Local name is " + l.getFriendlyName());

		String bluecoveVersion = LocalDevice.getProperty("bluecove");
		if (bluecoveVersion != null) {
			System.out.println("bluecove:" + bluecoveVersion);
			System.out.println("stack:" + LocalDevice.getProperty("bluecove.stack"));
			System.out.println("stack version:" + LocalDevice.getProperty("bluecove.stack.version"));
			System.out.println("radio manufacturer:" + LocalDevice.getProperty("bluecove.radio.manufacturer"));
			System.out.println("radio version:" + LocalDevice.getProperty("bluecove.radio.version"));
		}

		BluetoothInquirer bi = new BluetoothInquirer();

		while (true) {

			System.out.println("Starting inquiry");

			if (!bi.startInquiry()) {
				System.out.println("Cannot start inquiry, Exit ...");
				break;
			}
			while (bi.inquiring) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}

			System.out.println("Testing getFriendlyName");
			for (Iterator i = bi.devices.iterator(); i.hasNext();) {
				RemoteDevice btDevice = (RemoteDevice) i.next();
				try {
					System.out.println("Found " + btDevice.getBluetoothAddress() + " : "
							+ btDevice.getFriendlyName(true));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class BluetoothInquirer implements DiscoveryListener {

		boolean inquiring;

		List devices;

		public boolean startInquiry() {
			inquiring = false;
			devices = new Vector();
			try {
				inquiring = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, this);
			} catch (BluetoothStateException e) {
				System.err.println("Cannot start inquiry: " + e);
				return false;
			}
			return inquiring;
		}

		public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
			DebugLog.debug("deviceDiscovered");
			devices.add(btDevice);
			StringBuffer name;
			try {
				DebugLog.debug("call getFriendlyName");
				name = new StringBuffer(btDevice.getFriendlyName(false));
			} catch (IOException ioe) {
				ioe.printStackTrace();
				name = new StringBuffer();
			}
			while (name.length() < 20)
				name.append(' ');
			System.out.println("Found " + btDevice.getBluetoothAddress() + " : " + name + " : " + cod);
		}

		public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		}

		public void serviceSearchCompleted(int transID, int respCode) {
		}

		public void inquiryCompleted(int discType) {
			inquiring = false;
		}

	}
}
