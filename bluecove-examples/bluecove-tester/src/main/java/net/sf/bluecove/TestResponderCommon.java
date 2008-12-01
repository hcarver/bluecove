/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.LocalDevice;

import junit.framework.Assert;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.StringUtils;

/**
 * 
 */
public class TestResponderCommon {

	public static int receiveMTU_max = L2CAPConnection.DEFAULT_MTU;

	public static void printProperty(String property) {
		String val = LocalDevice.getProperty(property);
		if (val != null) {
			Logger.info(property + ":" + val);
		}
	}

	public static void initLocalDevice() {
		if (!Configuration.isConfigured) {
			try {
				initLocalDeviceConfig();
			} catch (BluetoothStateException e) {
				Logger.error("can't init LocalDevice", e);
			}
		}
	}

	public static void startLocalDevice() throws BluetoothStateException {
		if (!Configuration.initializeLocalDevice) {
			return;
		}
		initLocalDeviceConfig();
	}

	private static void initLocalDeviceConfig() throws BluetoothStateException {
		Configuration.cldcStub.setThreadLocalBluetoothStack(Configuration.threadLocalBluetoothStack);
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
		Logger.info("class:" + BluetoothTypesInfo.toString(localDevice.getDeviceClass()));

		printProperty("bluetooth.api.version");
		printProperty("bluetooth.sd.trans.max");
		printProperty("bluetooth.sd.attr.retrievable.max");
		printProperty("bluetooth.connected.devices.max");
		printProperty("bluetooth.connected.inquiry.scan");
		printProperty("bluetooth.connected.page.scan");
		printProperty("bluetooth.connected.inquiry");
		printProperty("bluetooth.l2cap.receiveMTU.max");

		String bluecoveVersion = LocalDevice.getProperty("bluecove");
		if (StringUtils.isStringSet(bluecoveVersion)) {
			Configuration.isBlueCove = true;

			Logger.info("bluecove:" + bluecoveVersion);
			Logger.info("stack:" + LocalDevice.getProperty("bluecove.stack"));

			Assert.assertNotNull("BT Address is null", localDevice.getBluetoothAddress());
			if (!Configuration.windowsCE) {
				Assert.assertNotNull("BT Name is null", localDevice.getFriendlyName());
			}

			Logger.info("stack version:" + LocalDevice.getProperty("bluecove.stack.version"));
			Logger.info("radio manufacturer:" + LocalDevice.getProperty("bluecove.radio.manufacturer"));
			Logger.info("radio version:" + LocalDevice.getProperty("bluecove.radio.version"));

			Configuration.stackWIDCOMM = StringUtils.equalsIgnoreCase("WIDCOMM", LocalDevice
					.getProperty("bluecove.stack"));
			String featureL2cap = LocalDevice.getProperty("bluecove.feature.l2cap");
			if (featureL2cap == null) {
				Configuration.supportL2CAP = Configuration.stackWIDCOMM || Configuration.macOSx || Configuration.linux;
			} else {
				Configuration.supportL2CAP = "true".equals(featureL2cap);
			}
			String id = LocalDevice.getProperty("bluecove.deviceID");
			if (id != null) {
				Logger.info("bluecove.deviceID:" + id);
			}
			String ids = LocalDevice.getProperty("bluecove.local_devices_ids");
			if (ids != null) {
				Logger.info("local devices ID:" + ids);
			}
		}

		String receiveMTUstr = LocalDevice.getProperty("bluetooth.l2cap.receiveMTU.max");
		if ((receiveMTUstr != null) && (receiveMTUstr.length() > 0)) {
			try {
				int max = Integer.valueOf(receiveMTUstr).intValue();
				if (max < receiveMTU_max) {
					receiveMTU_max = max;
				}
			} catch (NumberFormatException ignore) {
			}
		}
		Configuration.isConfigured = true;
	}

	public static String niceDeviceName(String bluetoothAddress) {
		String w = getWhiteDeviceName(bluetoothAddress);
		if (w == null) {
			w = (String) TestResponderClient.recentDeviceNames.get(bluetoothAddress.toUpperCase());
		}
		return (w != null) ? w : bluetoothAddress;
	}

	public static String getWhiteDeviceName(String bluetoothAddress) {
		if ((bluetoothAddress == null) || (Configuration.testDeviceNames == null)) {
			return null;
		}
		String addr = bluetoothAddress.toUpperCase();
		return (String) Configuration.testDeviceNames.get(addr);
	}

}
