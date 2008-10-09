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

import java.util.Random;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;

import com.intel.bluetooth.emu.DeviceDescriptor;

/**
 * @author vlads
 * 
 */
class EmulatorDeviceInquiry implements DeviceInquiryRunnable {

	private static final int MIN_DISCOVERY_DURATION = 200;

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
	 * @see com.intel.bluetooth.DeviceInquiryRunnable#runDeviceInquiry(com.intel.bluetooth.DeviceInquiryThread,
	 *      int, javax.bluetooth.DiscoveryListener)
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
			duration = MIN_DISCOVERY_DURATION;
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
	 * @see com.intel.bluetooth.DeviceInquiryRunnable#deviceDiscoveredCallback(javax.bluetooth.DiscoveryListener,
	 *      long, int, java.lang.String, boolean)
	 */
	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
	}

}
