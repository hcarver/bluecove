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
