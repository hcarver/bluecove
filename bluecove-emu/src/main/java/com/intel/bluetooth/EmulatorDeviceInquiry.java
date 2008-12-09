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
package com.intel.bluetooth;

import java.util.Random;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;

import com.intel.bluetooth.emu.DeviceDescriptor;

/**
 * 
 */
class EmulatorDeviceInquiry implements DeviceInquiryRunnable {

	private static final int DISCOVERY_DURATION_ALWAYS = 200;

	private static final int DISCOVERY_DURATION_MINIMUM = 500;

	private EmulatorLocalDevice localDevice;

	private BluetoothStack bluetoothStack;

	private DiscoveryListener discoveryListener;

	private boolean deviceInquiryCanceled = false;

	private Object canceledEvent = new Object();

	private static Random rnd;

	EmulatorDeviceInquiry(EmulatorLocalDevice localDevice, BluetoothStack bluetoothStack,
			DiscoveryListener discoveryListener) {
		this.localDevice = localDevice;
		this.bluetoothStack = bluetoothStack;
		this.discoveryListener = discoveryListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.DeviceInquiryRunnable#runDeviceInquiry(com.intel.bluetooth.DeviceInquiryThread, int,
	 * javax.bluetooth.DiscoveryListener)
	 */
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			DeviceDescriptor[] devices = localDevice.getDeviceManagerService().getDiscoveredDevices(
					localDevice.getAddress());
			startedNotify.deviceInquiryStartedCallback();
			long start = System.currentTimeMillis();
			// Find first device
			while (devices.length == 0) {
				if (!randomWait(start, -1)) {
					break;
				}
				devices = updateDiscoveredDevices(devices);
			}
			// Report devices and find a new one
			int reportedIndex = 0;
			while ((randomWait(start, reportedIndex)) || (reportedIndex < devices.length)) {
				if (deviceInquiryCanceled) {
					return DiscoveryListener.INQUIRY_TERMINATED;
				}
				if (reportedIndex < devices.length) {
					DeviceDescriptor d = devices[reportedIndex];
					// Device may be updated.
					d = localDevice.getDeviceManagerService().getDeviceDescriptor(d.getAddress());
					reportedIndex++;
					RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(bluetoothStack, d.getAddress(), d
							.getName(), false);
					DeviceClass cod = new DeviceClass(d.getDeviceClass());
					DebugLog.debug("deviceDiscovered address", remoteDevice.getBluetoothAddress());
					DebugLog.debug("deviceDiscovered deviceClass", cod);
					listener.deviceDiscovered(remoteDevice, cod);
				}
				devices = updateDiscoveredDevices(devices);
			}

			if (deviceInquiryCanceled) {
				return DiscoveryListener.INQUIRY_TERMINATED;
			}
			return DiscoveryListener.INQUIRY_COMPLETED;
		} finally {
			bluetoothStack.cancelInquiry(discoveryListener);
		}
	}

	private DeviceDescriptor[] updateDiscoveredDevices(DeviceDescriptor[] devices) {
		DeviceDescriptor[] newDevices = localDevice.getDeviceManagerService().getDiscoveredDevices(
				localDevice.getAddress());
		Vector<DeviceDescriptor> discoveredDevice = new Vector<DeviceDescriptor>();
		// Old device unchanged
		for (int i = 0; i < devices.length; i++) {
			discoveredDevice.addElement(devices[i]);
		}
		// Append new devices if any
		newDevicesLoop: for (int i = 0; i < newDevices.length; i++) {
			for (int k = 0; k < devices.length; k++) {
				if (newDevices[i].getAddress() == devices[k].getAddress()) {
					continue newDevicesLoop;
				}
			}
			discoveredDevice.addElement(newDevices[i]);
		}
		return (DeviceDescriptor[]) discoveredDevice.toArray(new DeviceDescriptor[discoveredDevice.size()]);
	}

	private boolean randomWait(long start, int device) {
		long duration = localDevice.getConfiguration().getDeviceInquiryDuration() * 1000;
		if (duration <= 0) {
			duration = DISCOVERY_DURATION_MINIMUM;
		}
		long now = System.currentTimeMillis();
		if ((duration == 0) || (now > start + duration)) {
			return false;
		}
		long timeout = duration / 7;
		if (localDevice.getConfiguration().isDeviceInquiryRandomDelay()) {
			if (rnd == null) {
				rnd = new Random();
			}
			long timeleft = start + duration - now;
			if (timeleft > 0) {
				timeout = rnd.nextInt((int) timeleft);
			}
		}
		if (device == 0) {
			timeout += DISCOVERY_DURATION_ALWAYS;
		}

		// Limit wait till the end of the duration period
		if (now + timeout > start + duration) {
			timeout = start + duration - now;
		}
		if (timeout <= 0) {
			return true;
		}
		synchronized (canceledEvent) {
			try {
				canceledEvent.wait(timeout);
			} catch (InterruptedException e) {
				deviceInquiryCanceled = true;
			}
		}
		return true;
	}

	boolean cancelInquiry(DiscoveryListener listener) {
		if (discoveryListener != listener) {
			return false;
		}
		deviceInquiryCanceled = true;
		synchronized (canceledEvent) {
			canceledEvent.notifyAll();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.DeviceInquiryRunnable#deviceDiscoveredCallback(javax.bluetooth.DiscoveryListener, long,
	 * int, java.lang.String, boolean)
	 */
	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
	}

}
