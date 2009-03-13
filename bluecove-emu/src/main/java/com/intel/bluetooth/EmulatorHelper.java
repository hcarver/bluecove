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

import java.util.HashMap;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.DeviceManagerService;
import com.intel.bluetooth.rmi.Client;

class EmulatorHelper {

	private static Map<EmulatorLocalDevice, EmulatorCommandReceiver> receivers = new HashMap<EmulatorLocalDevice, EmulatorCommandReceiver>();

	static DeviceManagerService getService() {
		String host = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_HOST);
		String port = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT);
		boolean isMaster = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_RMI_REGISTRY,
				false);
		return (DeviceManagerService) Client.getService(DeviceManagerService.class, isMaster, host, port);
	}

	static EmulatorLocalDevice createNewLocalDevice() throws BluetoothStateException {
		DeviceDescriptor deviceDescriptor;
		DeviceManagerService service;
		try {
			service = getService();
			deviceDescriptor = service.createNewDevice(BlueCoveImpl
					.getConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ID), BlueCoveImpl
					.getConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS));
		} catch (RuntimeException e) {
			throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
		}
		EmulatorLocalDevice device = new EmulatorLocalDevice(service, deviceDescriptor);
		EmulatorCommandReceiver receiver = new EmulatorCommandReceiver(device);
		receivers.put(device, receiver);
		receiver.setDaemon(true);
		receiver.start();
		return device;
	}

	static void releaseDevice(EmulatorLocalDevice device) {
		EmulatorCommandReceiver receiver = receivers.remove(device);
		if (receiver != null) {
			receiver.shutdownReceiver();
		}
		device.getDeviceManagerService().releaseDevice(device.getAddress());
		device.destroy();
	}

}
