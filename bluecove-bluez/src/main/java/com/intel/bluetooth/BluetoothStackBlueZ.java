/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mark Swanson
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
 *
 * @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

import org.bluez.Adapter;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;

import cx.ath.matthew.unix.UnixSocket;

/**
 * A Java/DBUS implementation. Property "bluecove.deviceID" or "bluecove.deviceAddress" can be used to select Local
 * Bluetooth device. Formats: 1. bluecove.deviceID: String. F.E. hci0, hci1, hci2, etc. 2. bluecove.deviceAddress:
 * String. F.E. 00:00:00:00:00:00
 * 
 * Please help with these questions: 0. I note that Adapter.java has a bunch of methods commented out. Do you feel these
 * aren't needed to get a bare bones implementation working? I notice that getLocalDeviceDiscoverable() could use
 * adapter.getMode() "discoverable" though I have no idea how to convert that to an int return value... 1. In order to
 * find the device ID I need to parse the String bluecove presents - which seems to be "/org/bluez/hci". I think the
 * constant: CONST_ADAPTER_PREFIX = "/org/bluez/hci" should be provided in Adapter.java. Or, Adapter.java needs to
 * provide some way of converting "/org/bluez/hci0" to the device ID "0". Thoughts?
 * 
 * 2. public DeviceClass getLocalDeviceClass() - I can't construct a DeviceClass because I don't have a record. How do I
 * make a record?
 * 
 * 3. getLocalDeviceName() dbus uniqueName or adapter.getAddress()?
 * 
 * A: Regarding "discoverable" string See in bluez sources what kind of stirrings it accepts then use
 * DiscoveryAgent.NOT_DISCOVERABLE DiscoveryAgent.GIAC, DiscoveryAgent.LIAC:
 * 
 * A: The idea was that I copied all the method descriptors from bluez-d-bus documentation. Some I tested and this is
 * uncommented . Some I'm not sure are implemented as described so I commented out.
 */
class BluetoothStackBlueZ implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

	public static final String NATIVE_BLUECOVE_LIB_BLUEZ = "bluecove";

	// Our reusable DBUS connection.
	DBusConnection dbusConn = null;

	// Our reusable default host adapter.
	Adapter adapter = null;

	// The current Manager.
	Manager dbusManager = null;

	static final int BLUECOVE_DBUS_VERSION = 2000300;

	// private int deviceID;

	// private int deviceDescriptor;

	/**
	 * The parsed long value of the adapter's BT 00:00:... address.
	 */
	private long localDeviceBTAddress = -1;

	private long sdpSesion;

	private int registeredServicesCount = 0;

	private Map<String, String> propertiesMap;

	private DiscoveryListener discoveryListener;

	// Prevent the device from been discovered twice
	private Vector<RemoteDevice> discoveredDevices;

	private boolean deviceInquiryCanceled = false;

	public static String CONST_ADAPTER_PREFIX = "/org/bluez/hci";

	// This native lib contains the rfcomm and l2cap linux-specific
	// implementation for this bluez d-bus implementation.
	public static String NATIVE_BLUEZ_LINUX_LIB = "bluecovebluez";

	private class DiscoveryData {
		public DeviceClass deviceClass;

		public String name;
	}

	// Different signal handlers get different device attributes
	// so we cache the data until device discovery is finished
	// and then create the RemoteDevice objects.
	private Map<Long, DiscoveryData> address2DiscoveryData;

	BluetoothStackBlueZ() {
	}

	public String getStackID() {
		DebugLog.debug("getStackID()");
		return BlueCoveImpl.STACK_BLUEZ;
	}

	// --- Library initialization

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isNativeCodeLoaded()
	 */
	public native boolean isNativeCodeLoaded();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#requireNativeLibraries()
	 */
	public LibraryInformation[] requireNativeLibraries() {
		LibraryInformation unixSocketLib = new LibraryInformation("unix-java");
		unixSocketLib.stackClass = UnixSocket.class;
		return new LibraryInformation[] { new LibraryInformation(NATIVE_BLUECOVE_LIB_BLUEZ), unixSocketLib };
	}

	public native int getLibraryVersionNative();

	public int getLibraryVersion() throws BluetoothStateException {
		DebugLog.debug("for breakpoint");
		return BLUECOVE_DBUS_VERSION;
	}

	public int detectBluetoothStack() {
		DebugLog.debug("detectBluetoothStack()");
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_BLUEZ;
	}

	/**
	 * Returns the device ID F.E. the '0' from /org/bluez/hci0 that corresponds to the given findLocalDeviceBTAddress.
	 * 
	 * @param id
	 *            If this is >= 0, we just return the id. I think this parameter had more meaning in the native
	 *            implementation. In this Java implementation the 'native' ID is the same as the device ID.
	 * @param findLocalDeviceBTAddress
	 *            The parsed long value of the device address F.E. Long.parseLong("00:03:C9:55:DF:3E").
	 * @return
	 * @throws BluetoothStateException
	 */
	private int nativeGetDeviceID(int id, long findLocalDeviceBTAddress) throws BluetoothStateException {
		// private native int nativeGetDeviceID(int id, long
		// findLocalDeviceBTAddress) throws BluetoothStateException;
		if (id >= 0)
			return id; // 
		String hexAddress = toHexString(findLocalDeviceBTAddress);
		// String hexAddress = "00:03:C9:55:DF:3E";
		DebugLog.debug("Trying to find adapter using hexAddress " + hexAddress);
		String adapter = dbusManager.FindAdapter(hexAddress);
		if (adapter != null) {
			DebugLog.debug("Found adapter:" + adapter + " using hexAddress " + hexAddress);
			// TODO..
			String idStr = adapter.substring(CONST_ADAPTER_PREFIX.length());
			return Integer.parseInt(idStr);
		}
		DebugLog.error("Failed to find adapter with hexAddress:" + hexAddress);
		throw new BluetoothStateException("Failed to get adapter with address " + hexAddress);
	}

	private Adapter getAdapterUsingDeviceID(String deviceID) throws BluetoothStateException {
		// String hexAddress = toHexString(findLocalDeviceBTAddress);
		// String hexAddress = "00:03:C9:55:DF:3E";
		DebugLog.debug("Trying to find adapter using deviceID " + deviceID);
		String adapterName = dbusManager.FindAdapter(deviceID);
		if (adapterName != null) {
			try {
				Adapter foundAdapter = (Adapter) dbusConn.getRemoteObject("org.bluez", adapterName, Adapter.class);
				DebugLog.debug("Found adapter using deviceID " + deviceID);
				return foundAdapter;
			} catch (DBusException ex) {
				DebugLog.error("initialize() getRemoteObject() failed to get the default dbus adapter " + adapterName
						+ ".", ex);
				throw new BluetoothStateException(ex.getMessage());
			}
		}
		DebugLog.error("Failed to find adapter with device ID " + deviceID);
		throw new BluetoothStateException("Failed to find adapter with device ID " + deviceID);
	}

	/**
	 * Returns a colon formatted BT address. F.E. 00:01:C2:51:D1:31
	 * 
	 * @param l
	 *            The long address format to be converted to a string.
	 * @return Note: can be optimized - was playing around with the formats required by bluecove.
	 */
	private String toHexString(long l) {
		StringBuffer buf = new StringBuffer();
		String lo = Integer.toHexString((int) l);
		if (l > 0xffffffffl) {
			String hi = Integer.toHexString((int) (l >> 32));
			buf.append(hi);
		}
		buf.append(lo);
		StringBuffer result = new StringBuffer();
		int prependZeros = 12 - buf.length();
		for (int i = 0; i < prependZeros; ++i)
			result.append("0");
		result.append(buf.toString());
		StringBuffer hex = new StringBuffer();
		for (int i = 0; i < 12; i += 2) {
			hex.append(result.substring(i, i + 2));
			if (i < 10)
				hex.append(":");
		}
		return hex.toString();
	}

	// private native int nativeOpenDevice(int deviceID) throws
	// BluetoothStateException;
	/*
	 * private int nativeOpenDevice(int deviceID) throws BluetoothStateException { // For now just return the deviceID.
	 * return deviceID; //throw new BluetoothStateException("nativeOpenDevice() Not supported yet."); }
	 */

	public void initialize() throws BluetoothStateException {
		DebugLog.debug("initialize()");
		try {
			dbusConn = DBusConnection.getConnection(DBusConnection.SYSTEM);
		} catch (DBusException ex) {
			DebugLog.error("initialize() failed to get the dbus connection.", ex);
			throw new BluetoothStateException(ex.getMessage());
		}
		try {
			dbusManager = (Manager) dbusConn.getRemoteObject("org.bluez", "/org/bluez", Manager.class);
		} catch (DBusException ex) {
			DebugLog.error("initialize() failed to get the dbus manager.", ex);
			throw new BluetoothStateException(ex.getMessage());
		}
		// DebugLog.debug("initialize() InterfaceVersion " +
		// dbusManager.InterfaceVersion());

		String defaultAdapterName = dbusManager.DefaultAdapter();
		DebugLog.debug("initialize() defaultAdapterName:" + defaultAdapterName);

		try {
			adapter = (Adapter) dbusConn.getRemoteObject("org.bluez", defaultAdapterName, Adapter.class);
		} catch (DBusException ex) {
			DebugLog.error("initialize() getRemoteObject() failed to get the default dbus adapter "
					+ defaultAdapterName + ".", ex);
			throw new BluetoothStateException(ex.getMessage());
		}
		DebugLog.debug("initialize() found the dbus adapter " + adapter.GetAddress());

		// If the user specifies a specific deviceID then we try to find it.
		String deviceIDStr = BlueCoveImpl.getConfigProperty("bluecove.deviceID");
		if (deviceIDStr != null && deviceIDStr.length() > 0) {
			Adapter foundAdapter = getAdapterUsingDeviceID(deviceIDStr);
			if (foundAdapter != null)
				adapter = foundAdapter;
		} else {
			// If the user specifies a specific BT address then we try to find
			// it.
			String hexAddressStr = BlueCoveImpl.getConfigProperty("bluecove.deviceAddress");
			if (hexAddressStr != null && hexAddressStr.length() > 0) {
				Adapter foundAdapter = getAdapterUsingDeviceID(hexAddressStr);
				if (foundAdapter != null)
					adapter = foundAdapter;
				/*
				 * long findLocalDeviceBTAddress = Long.parseLong(adapter.GetAddress().replaceAll(":", ""), 16); if
				 * (deviceAddressStr != null) { findLocalDeviceBTAddress =
				 * Long.parseLong(deviceAddressStr.replaceAll(":", ""), 16); } DebugLog.debug("initialize() get deviceID
				 * from findLocalDeviceBTAddress:" + findLocalDeviceBTAddress); deviceID = nativeGetDeviceID(findID,
				 * findLocalDeviceBTAddress); DebugLog.debug("initialize() localDeviceID", deviceID);
				 */
			}
		}
		localDeviceBTAddress = convertBTAddress(adapter.GetAddress());
		// deviceDescriptor = nativeOpenDevice(deviceID);

		address2DiscoveryData = new HashMap<Long, DiscoveryData>();

		propertiesMap = new TreeMap<String, String>();
		propertiesMap.put("bluetooth.api.version", "1.1");
		// required or service discovery will fail with NPE
		propertiesMap.put("bluetooth.sd.trans.max", "1");
	}

	// private native void nativeCloseDevice(int deviceDescriptor);
	/*
	 * private void nativeCloseDevice(int deviceDescriptor) { //conn.getConnection(arg0) throw new
	 * UnsupportedOperationException("nativeCloseDevice() Not supported yet."); }
	 */

	public void destroy() {
		DebugLog.debug("destroy()");
		if (sdpSesion != 0) {
			try {
				long s = sdpSesion;
				sdpSesion = 0;
				closeSDPSessionImpl(s, true);
			} catch (ServiceRegistrationException ignore) {
			}
		}
		adapter.CancelDiscovery(); // needed?
		dbusConn.disconnect();
		// nativeCloseDevice(deviceDescriptor);
	}

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		// DebugLog.debug("isCurrentThreadInterruptedCallback()");
		return Thread.interrupted();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	public int getFeatureSet() {
		DebugLog.debug("getFeatureSet()");
		return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
	}

	// --- LocalDevice

	/**
	 * I don't know what this is supposed to do - just returning the deviceDescriptor - which is really just the
	 * deviceID.
	 */
	/*
	 * private long getLocalDeviceBluetoothAddressImpl(int deviceDescriptor) throws BluetoothStateException { //private
	 * native long getLocalDeviceBluetoothAddressImpl(int deviceDescriptor) throws BluetoothStateException; return
	 * deviceDescriptor; //throw new BluetoothStateException
	 * ("getLocalDeviceBluetoothAddressImpl() Not supported yet."); }
	 */

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		String result = adapter.GetAddress();
		DebugLog.debug("getLocalDeviceBluetoothAddress():" + result);
		// return
		// RemoteDeviceHelper.getBluetoothAddress(getLocalDeviceBluetoothAddressImpl(deviceDescriptor));
		// I don't know if this is supposed to return the hci deviceID or the
		// hex address.
		return result.replaceAll(":", "");
	}

	// private native int nativeGetDeviceClass(int deviceDescriptor);
	private int nativeGetDeviceClass(int deviceDescriptor) {

		throw new UnsupportedOperationException("nativeGetDeviceClass() Not supported yet.");
	}

	public DeviceClass getLocalDeviceClass() {
		DebugLog.debug("getLocalDeviceClass()");
		// How am I supposed to determine this?
		// DeviceClass deviceClass = new DeviceClass();
		DeviceClass deviceClass = null;

		/*
		 * int record = nativeGetDeviceClass(deviceDescriptor); if (record == 0xff000000) { // could not be determined
		 * return null; } return new DeviceClass(record);
		 */
		return deviceClass;
	}

	// private native String nativeGetDeviceName(int deviceDescriptor);
	/*
	 * private String nativeGetDeviceName(int deviceDescriptor) {
	 * 
	 * throw new UnsupportedOperationException("nativeGetDeviceName() Not supported yet." ); }
	 */

	public String getLocalDeviceName() {
		DebugLog.debug("getLocalDeviceName()");
		return dbusConn.getUniqueName();
		// adapter.GetAddress(); // or should we use the adapter address?
		// return nativeGetDeviceName(deviceDescriptor);
	}

	public boolean isLocalDevicePowerOn() {
		DebugLog.debug("isLocalDevicePowerOn()");
		// Have no idea how turn on and off device on BlueZ, as well to how to
		// detect this condition.
		return true;
	}

	public String getLocalDeviceProperty(String property) {
		DebugLog.debug("getLocalDeviceProperty() property:" + property + ", value:" + propertiesMap.get(property));
		return (String) propertiesMap.get(property);
	}

	// private native int nativeGetLocalDeviceDiscoverable(int
	// deviceDescriptor);
	/*
	 * private int nativeGetLocalDeviceDiscoverable(int deviceDescriptor) {
	 * 
	 * throw newUnsupportedOperationException( "nativeGetLocalDeviceDiscoverable() Not supported yet."); }
	 */

	public int getLocalDeviceDiscoverable() {
		DebugLog.debug("getLocalDeviceDiscoverable()");

		throw new UnsupportedOperationException("getLocalDeviceDiscoverable() Not supported yet.");
		// return nativeGetLocalDeviceDiscoverable(deviceDescriptor);
	}

	// private native int nativeSetLocalDeviceDiscoverable(int deviceDescriptor,
	// int mode);
	/*
	 * private int nativeSetLocalDeviceDiscoverable(int deviceDescriptor, int mode) {
	 * 
	 * throw newUnsupportedOperationException( "nativeSetLocalDeviceDiscoverable() Not supported yet."); }
	 */

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		DebugLog.debug("setLocalDeviceDiscoverable()");
		throw new UnsupportedOperationException("setLocalDeviceDiscoverable(int mode) Not supported yet.");
		/*
		 * int error = nativeSetLocalDeviceDiscoverable(deviceDescriptor, mode); if (error != 0) { throw
		 * newBluetoothStateException( "Unable to change discovery mode. It may be because you aren't root" ); } return
		 * true;
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		DebugLog.debug("setLocalDeviceServiceClasses()");
		throw new NotSupportedRuntimeException(getStackID());
	}

	public boolean authenticateRemoteDevice(long address) throws IOException {
		// TODO
		return false;
	}

	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice (long)
	 */
	public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
		// TODO
		throw new NotSupportedIOException(getStackID());
	}

	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		DebugLog.debug("startInquiry()");
		if (discoveryListener != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		discoveryListener = listener;
		discoveredDevices = new Vector<RemoteDevice>();
		deviceInquiryCanceled = false;
		return DeviceInquiryThread.startInquiry(this, this, accessCode, listener);
	}

	// private native int runDeviceInquiryImpl(DeviceInquiryThread
	// startedNotify, int deviceID, int deviceDescriptor,
	// int accessCode, int inquiryLength, int maxResponses, DiscoveryListener
	// listener)
	// throws BluetoothStateException;
	private int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, int inquiryLength,
			int maxResponses, final DiscoveryListener listener) throws BluetoothStateException {

		try {
			final Object discoveryCompletedEvent = new Object();

			DBusSigHandler<Adapter.DiscoveryCompleted> discoveryCompleted = new DBusSigHandler<Adapter.DiscoveryCompleted>() {
				public void handle(Adapter.DiscoveryCompleted s) {
					DebugLog.debug("discoveryCompleted.handle()");
					synchronized (discoveryCompletedEvent) {
						discoveryCompletedEvent.notifyAll();
					}
				}
			};
			dbusConn.addSigHandler(Adapter.DiscoveryCompleted.class, discoveryCompleted);

			DBusSigHandler<Adapter.DiscoveryStarted> discoveryStarted = new DBusSigHandler<Adapter.DiscoveryStarted>() {
				public void handle(Adapter.DiscoveryStarted s) {
					DebugLog.debug("device discovery procedure has been started.");
				}
			};
			dbusConn.addSigHandler(Adapter.DiscoveryStarted.class, discoveryStarted);

			final Map<String, Adapter.RemoteDeviceFound> devicesDiscovered = new HashMap<String, Adapter.RemoteDeviceFound>();
			DBusSigHandler<Adapter.RemoteDeviceFound> remoteDeviceFound = new DBusSigHandler<Adapter.RemoteDeviceFound>() {
				public void handle(Adapter.RemoteDeviceFound s) {
					if (devicesDiscovered.containsKey(s.address))
						return;
					if (s.address.equals("00:0A:95:31:C7:60"))
						return; // ignore springer
					// dbus doesn't give us the remote device name so we
					// can't create the RemoteDevice here as we can never set
					// the device name later during remoteNameUpdated.
					DebugLog.debug("device found " + s.address + " , name:" + s.getName() + ", destination:"
							+ s.getDestination() + ", interface:" + s.getInterface() + ", path:" + s.getPath()
							+ ", sig:" + s.getSig() + ", source:" + s.getSource() + ", device class:"
							+ s.deviceClass.intValue());
					devicesDiscovered.put(s.address, s);
					DeviceClass deviceClass = new DeviceClass(s.deviceClass.intValue());
					long longAddress = convertBTAddress(s.address);
					DiscoveryData discoveryData = address2DiscoveryData.get(longAddress);
					if (discoveryData == null) {
						discoveryData = new DiscoveryData();
						address2DiscoveryData.put(longAddress, discoveryData);
					}
					discoveryData.deviceClass = deviceClass;
				}
			};
			dbusConn.addSigHandler(Adapter.RemoteDeviceFound.class, remoteDeviceFound);

			DBusSigHandler<Adapter.RemoteNameUpdated> remoteNameUpdated = new DBusSigHandler<Adapter.RemoteNameUpdated>() {
				public void handle(Adapter.RemoteNameUpdated s) {
					DebugLog.debug("deviceNameUpdated() " + s.address + " " + s.name);

					if (s.address.equals("00:0A:95:31:C7:60")) {
						DebugLog.debug("Ignoring springer. address:" + s.address);
						return; // ignore springer
					}
					long longAddress = convertBTAddress(s.address);
					DiscoveryData discoveryData = address2DiscoveryData.get(longAddress);
					if (discoveryData == null) {
						discoveryData = new DiscoveryData();
						address2DiscoveryData.put(longAddress, discoveryData);
					}
					discoveryData.name = s.name;
				}
			};
			dbusConn.addSigHandler(Adapter.RemoteNameUpdated.class, remoteNameUpdated);

			synchronized (discoveryCompletedEvent) {
				adapter.DiscoverDevices();
				startedNotify.deviceInquiryStartedCallback();
				DebugLog.debug("wait for device inquiry to complete...");
				try {
					discoveryCompletedEvent.wait();
					DebugLog.debug(devicesDiscovered.size() + " device(s) found");

					boolean paired = true; // TODO: how do I choose?

					for (Long address : address2DiscoveryData.keySet()) {
						DiscoveryData discoveryData = address2DiscoveryData.get(address);
						if (discoveryData.name.contains("springer")) {
							DebugLog.debug("Ignoring springer.");
							continue;
						}
						RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackBlueZ.this,
								address, discoveryData.name, paired);
						listener.deviceDiscovered(remoteDevice, discoveryData.deviceClass);
					}
					// adapter.CancelDiscovery(); // not authorized
					// No, the SearchServicesThread will notify the listener.
					// listener.inquiryCompleted(DiscoveryListener.INQUIRY_COMPLETED);
					DebugLog.debug("CancelDiscovery()");
					return DiscoveryListener.INQUIRY_COMPLETED;
				} catch (InterruptedException e) {
					DebugLog.error("Discovery interrupted.");
					return DiscoveryListener.INQUIRY_TERMINATED;
				} catch (Exception ex) {
					DebugLog.error("Discovery process failed", ex);
					throw new BluetoothStateException("Device Inquiry failed:" + ex.getMessage());
				}
			}
		} catch (DBusException e) {
			DebugLog.error("Discovery dbus problem", e);
			throw new BluetoothStateException("Device Inquiry failed:" + e.getMessage());
		}
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		DebugLog.debug("runDeviceInquiry()");
		try {
			int discType = runDeviceInquiryImpl(startedNotify, accessCode, 8, 20, listener);
			if (deviceInquiryCanceled) {
				return DiscoveryListener.INQUIRY_TERMINATED;
			}
			return discType;
		} finally {
			discoveryListener = null;
			discoveredDevices = null;
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
		DebugLog.debug("deviceDiscoveredCallback()");
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, deviceAddr, deviceName, paired);
		if (deviceInquiryCanceled || (discoveryListener == null) || (discoveredDevices == null)
				|| (discoveredDevices.contains(remoteDevice))) {
			return;
		}
		discoveredDevices.addElement(remoteDevice);
		DeviceClass cod = new DeviceClass(deviceClass);
		DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
		DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
		listener.deviceDiscovered(remoteDevice, cod);
	}

	// private native boolean deviceInquiryCancelImpl(int deviceDescriptor);
	/*
	 * private boolean deviceInquiryCancelImpl(int deviceDescriptor) { return adapter.CancelDiscovery(); }
	 */

	public boolean cancelInquiry(DiscoveryListener listener) {
		DebugLog.debug("cancelInquiry()");
		// TODO: why == ?
		if (discoveryListener != null && discoveryListener == listener) {
			deviceInquiryCanceled = true;
			// return deviceInquiryCancelImpl(deviceDescriptor);
			adapter.CancelDiscovery();
			return true; // TODO: how could the be true or false?
		}
		return false;
	}

	// private native String getRemoteDeviceFriendlyNameImpl(int
	// deviceDescriptor, long remoteAddress) throws IOException;
	/*
	 * private String getRemoteDeviceFriendlyNameImpl(int deviceDescriptor, long remoteAddress) throws IOException {
	 * 
	 * throw new IOException("getRemoteDeviceFriendlyNameImpl() Not supported yet."); }
	 */

	public String getRemoteDeviceFriendlyName(long anAddress) throws IOException {
		DebugLog.debug("getRemoteDeviceFriendlyName()");

		RemoteDevice remoteDevice = RemoteDeviceHelper.getCashedDevice(this, anAddress);
		return remoteDevice.getFriendlyName(false);
	}

	public RemoteDevice[] retrieveDevices(int option) {
		return null;
	}

	public Boolean isRemoteDeviceTrusted(long address) {
		return null;
	}

	public Boolean isRemoteDeviceAuthenticated(long address) {
		return null;
	}

	// --- Service search

	/**
	 * Starts searching for services.
	 * 
	 * @return transId
	 */
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			DebugLog.debug("searchServices() device:" + device.getFriendlyName(false));
			return SearchServicesThread.startSearchServices(this, this, attrSet, uuidSet, device, listener);
		} catch (Exception ex) {
			DebugLog.debug("searchServices() failed", ex);
			throw new BluetoothStateException("searchServices() failed: " + ex.getMessage());
		}
	}

	/**
	 * Finds services. Implements interface: SearchServicesRunnable
	 * 
	 * @param sst
	 * @param localDeviceBTAddress
	 * @param uuidValues
	 * @param remoteDeviceAddress
	 * @return
	 * @throws SearchServicesException
	 */
	private int runSearchServicesImpl(SearchServicesThread sst, long localDeviceBTAddress, byte[][] uuidValues,
			long remoteDeviceAddress) throws SearchServicesException {
		DebugLog.debug("runSearchServicesImpl()");
		// TODO: services discovery

		// 1. uuidValues need to be converted somehow to a match String.
		// http://wiki.bluez.org/wiki/HOWTO/DiscoveringServices states:
		// "Currently, the BlueZ D-Bus API supports only a single pattern."
		// So, instead we match everything and do our own matching further
		// down.
		String match = "";
		for (int j = 0; j < uuidValues.length; ++j) {
			// I expect something like this:
			// 000000020000100080000002ee000002
			BigInteger bi = new BigInteger(uuidValues[j]);
			String hex = bi.abs().toString(16);
			// if (hex.length() == 33)
			// hex = hex.substring(1); // minus sign
			// DebugLog.debug("service uuid:" + hex);
			UUID protocolUUID = new UUID(hex, false);
		}

		// 2. I assume the current adapter is to be used atm.

		// MUST use ':' format. F.E. 00:00:00:00:00:00
		String hexAddress = toHexString(remoteDeviceAddress);
		DebugLog.debug("runSearchServicesImpl() hexAddress:" + hexAddress);
		UInt32[] serviceHandles = null;
		try {
			serviceHandles = adapter.GetRemoteServiceHandles(hexAddress, match);
		} catch (Throwable t) {
			DebugLog.debug("GetRemoteServiceHandles() failed:", t);
			throw new SearchServicesException("GetRemoteServiceHandles() failed:" + t.getMessage());
		}
		DebugLog.debug("GetRemoteServiceHandles() done.");

		if (serviceHandles == null && serviceHandles.length == 0)
			return DiscoveryListener.SERVICE_SEARCH_COMPLETED;

		DebugLog.debug("Found serviceHandles:" + serviceHandles.length);
		RemoteDevice remoteDevice = RemoteDeviceHelper.getCashedDevice(this, remoteDeviceAddress);
		for (int i = 0; i < serviceHandles.length; ++i) {
			UInt32 handle = serviceHandles[i];
			try {
				byte[] serviceRecordBytes = adapter.GetRemoteServiceRecord(hexAddress, handle);
				ServiceRecordImpl serviceRecordImpl = new ServiceRecordImpl(this, remoteDevice, handle.intValue());
				serviceRecordImpl.loadByteArray(serviceRecordBytes);
				for (int j = 0; j < uuidValues.length; ++j) {
					// I expect something like this:
					// 000000020000100080000002ee000002
					BigInteger bi = new BigInteger(uuidValues[j]);
					String hex = bi.abs().toString(16);
					UUID protocolUUID = new UUID(hex, false);
					if (!serviceRecordImpl.hasServiceClassUUID(protocolUUID)) {
						DebugLog.debug("ignoring service uuid:" + hex);
						continue;
					}
					DebugLog.debug("found service uuid:" + hex);
					sst.addServicesRecords(serviceRecordImpl);
				}
				/*
				 * int[] attributeIDs = serviceRecordImpl.getAttributeIDs(); for (int j=0; j < attributeIDs.length; ++j)
				 * { DataElement dataElement = serviceRecordImpl.getAttributeValue(attributeIDs[j]);
				 * DebugLog.debug("dataElement:" + ((Object)dataElement).toString()); }
				 */
			} catch (IOException e) {
				DebugLog.debug("Failed to load serviceRecordBytes", e);
				// TODO: Is there any logical reason to parse other records?
				// throw new SearchServicesException("runSearchServicesImpl()
				// failed to parse the service record.");
			}
		}

		return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
	}

	public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device,
			DiscoveryListener listener) throws BluetoothStateException {
		DebugLog.debug("runSearchServices()");
		sst.searchServicesStartedCallback();
		try {
			byte[][] uuidValues = new byte[uuidSet.length][];
			for (int i = 0; i < uuidSet.length; i++) {
				uuidValues[i] = Utils.UUIDToByteArray(uuidSet[i]);
			}
			long btAddress = Long.parseLong(adapter.GetAddress().replaceAll(":", ""), 16);
			int respCode = runSearchServicesImpl(sst, btAddress, uuidValues, RemoteDeviceHelper.getAddress(device));
			if ((respCode != DiscoveryListener.SERVICE_SEARCH_ERROR) && (sst.isTerminated())) {
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			} else if (respCode == DiscoveryListener.SERVICE_SEARCH_COMPLETED) {
				Vector<ServiceRecord> records = sst.getServicesRecords();
				if (records.size() != 0) {
					DebugLog.debug("SearchServices finished", sst.getTransID());
					ServiceRecord[] servRecordArray = (ServiceRecord[]) Utils.vector2toArray(records,
							new ServiceRecord[records.size()]);
					listener.servicesDiscovered(sst.getTransID(), servRecordArray);
				} else
					listener.servicesDiscovered(sst.getTransID(), new ServiceRecord[0]);
				if (records.size() != 0) {
					return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
				} else {
					return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
				}
			} else {
				return respCode;
			}
		} catch (SearchServicesDeviceNotReachableException e) {
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		} catch (SearchServicesTerminatedException e) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		} catch (SearchServicesException e) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
	}

	public boolean serviceDiscoveredCallback(SearchServicesThread sst, long sdpSession, long handle) {
		DebugLog.debug("serviceDiscoveredCallback()");
		if (sst.isTerminated()) {
			return true;
		}
		ServiceRecordImpl servRecord = new ServiceRecordImpl(this, sst.getDevice(), handle);
		int[] attrIDs = sst.getAttrSet();
		long remoteDeviceAddress = RemoteDeviceHelper.getAddress(sst.getDevice());
		populateServiceRecordAttributeValuesImpl(remoteDeviceAddress, sdpSession, handle, attrIDs, servRecord);
		sst.addServicesRecords(servRecord);
		return false;
	}

	public boolean cancelServiceSearch(int transID) {
		DebugLog.debug("cancelServiceSearch()");
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			return sst.setTerminated();
		} else {
			return false;
		}
	}

	// private native boolean populateServiceRecordAttributeValuesImpl(long
	// localDeviceBTAddress,
	// long remoteDeviceAddress, long sdpSession, long handle, int[] attrIDs,
	// ServiceRecordImpl serviceRecord);
	private boolean populateServiceRecordAttributeValuesImpl(long remoteDeviceAddress, long sdpSession, long handle,
			int[] attrIDs, ServiceRecordImpl serviceRecord) {
		throw new UnsupportedOperationException("populateServiceRecordAttributeValuesImpl() Not supported yet.");
	}

	private long convertBTAddress(String anAddress) {
		long btAddress = Long.parseLong(anAddress.replaceAll(":", ""), 16);
		return btAddress;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		DebugLog.debug("populateServicesRecordAttributeValues()");
		long remoteDeviceAddress = RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
		return populateServiceRecordAttributeValuesImpl(remoteDeviceAddress, 0, serviceRecord.getHandle(), attrIDs,
				serviceRecord);
	}

	// --- SDP Server

	// private native long openSDPSessionImpl() throws
	// ServiceRegistrationException;
	private long openSDPSessionImpl() throws ServiceRegistrationException {
		throw new ServiceRegistrationException("openSDPSessionImpl() Not supported yet.");
	}

	private synchronized long getSDPSession() throws ServiceRegistrationException {
		if (this.sdpSesion == 0) {
			sdpSesion = openSDPSessionImpl();
			DebugLog.debug("created SDPSession", sdpSesion);
		}
		return sdpSesion;
	}

	// private native void closeSDPSessionImpl(long sdpSesion, boolean quietly)
	// throws ServiceRegistrationException;
	private void closeSDPSessionImpl(long sdpSesion, boolean quietly) throws ServiceRegistrationException {

		throw new ServiceRegistrationException("closeSDPSessionImpl() Not supported yet.");
	}

	// private native long registerSDPServiceImpl(long sdpSesion, long
	// localDeviceBTAddress, byte[] record)
	// throws ServiceRegistrationException;
	private long registerSDPServiceImpl(long sdpSesion, byte[] record) throws ServiceRegistrationException {

		throw new ServiceRegistrationException("registerSDPServiceImpl() Not supported yet.");
	}

	// private native void updateSDPServiceImpl(long sdpSesion, long
	// localDeviceBTAddress, long handle, byte[] record)
	// throws ServiceRegistrationException;
	private void updateSDPServiceImpl(long sdpSesion, long handle, byte[] record) throws ServiceRegistrationException {

		throw new ServiceRegistrationException("updateSDPServiceImpl() Not supported yet.");
	}

	// private native void unregisterSDPServiceImpl(long sdpSesion, long
	// localDeviceBTAddress, long handle, byte[] record)
	// throws ServiceRegistrationException;
	private void unregisterSDPServiceImpl(long sdpSesion, long handle, byte[] record)
			throws ServiceRegistrationException {

		throw new ServiceRegistrationException("unregisterSDPServiceImpl() Not supported yet.");
	}

	private byte[] getSDPBinary(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		byte[] blob;
		try {
			blob = serviceRecord.toByteArray();
		} catch (IOException e) {
			throw new ServiceRegistrationException(e.toString());
		}
		return blob;
	}

	private synchronized void registerSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		long handle = registerSDPServiceImpl(getSDPSession(), getSDPBinary(serviceRecord));
		serviceRecord.setHandle(handle);
		serviceRecord.populateAttributeValue(BluetoothConsts.ServiceRecordHandle, new DataElement(DataElement.U_INT_4,
				handle));
		registeredServicesCount++;
	}

	private void updateSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		updateSDPServiceImpl(getSDPSession(), serviceRecord.getHandle(), getSDPBinary(serviceRecord));
	}

	private synchronized void unregisterSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		try {
			unregisterSDPServiceImpl(getSDPSession(), serviceRecord.getHandle(), getSDPBinary(serviceRecord));
		} finally {
			registeredServicesCount--;
			if (registeredServicesCount <= 0) {
				registeredServicesCount = 0;
				DebugLog.debug("closeSDPSession", sdpSesion);
				long s = sdpSesion;
				sdpSesion = 0;
				closeSDPSessionImpl(s, false);
			}
		}
	}

	// --- Client RFCOMM connections

	private native long connectionRfOpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel,
			boolean authenticate, boolean encrypt, int timeout) throws IOException;

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		DebugLog.debug("connectionRfOpenClientConnection()");
		return connectionRfOpenClientConnectionImpl(this.localDeviceBTAddress, params.address, params.channel,
				params.authenticate, params.encrypt, params.timeout);
	}

	public native void connectionRfCloseClientConnection(long handle) throws IOException;

	public native int rfGetSecurityOptImpl(long handle) throws IOException;

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return rfGetSecurityOptImpl(handle);
	}

	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		// TODO
		return false;
	}

	private native long rfServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			boolean encrypt, boolean master, boolean timeouts, int backlog) throws IOException;

	private native int rfServerGetChannelIDImpl(long handle) throws IOException;

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		final int listen_backlog = 1;
		long socket = rfServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate,
				params.encrypt, params.master, params.timeouts, listen_backlog);
		boolean success = false;
		try {
			int channel = rfServerGetChannelIDImpl(socket);
			serviceRecord.populateRFCOMMAttributes(0, channel, params.uuid, params.name, params.obex);
			registerSDPRecord(serviceRecord);
			success = true;
			return socket;
		} finally {
			if (!success) {
				rfServerCloseImpl(socket, true);
			}
		}
	}

	private native void rfServerCloseImpl(long handle, boolean quietly) throws IOException;

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		try {
			unregisterSDPRecord(serviceRecord);
		} finally {
			rfServerCloseImpl(handle, false);
		}
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		updateSDPRecord(serviceRecord);
	}

	// public native long rfServerAcceptAndOpenRfServerConnection(long handle)
	// throws IOException;
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		throw new IOException("rfServerAcceptAndOpenRfServerConnection() Not supported yet.");
	}

	public void connectionRfCloseServerConnection(long clientHandle) throws IOException {
		connectionRfCloseClientConnection(clientHandle);
	}

	// --- Shared Client and Server RFCOMM connections

	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public native void connectionRfFlush(long handle) throws IOException;

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;

	// --- Client and Server L2CAP connections

	private native long l2OpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel,
			boolean authenticate, boolean encrypt, int receiveMTU, int transmitMTU, int timeout) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth .BluetoothConnectionParams,
	 * int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {

		return l2OpenClientConnectionImpl(localDeviceBTAddress, params.address, params.channel, params.authenticate,
				params.encrypt, receiveMTU, transmitMTU, params.timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public native void l2CloseClientConnection(long handle) throws IOException;

	private native long l2ServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			boolean encrypt, boolean master, boolean timeouts, int backlog, int receiveMTU, int transmitMTU)
			throws IOException;

	public native int l2ServerGetPSMImpl(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth. BluetoothConnectionNotifierParams, int,
	 * int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		final int listen_backlog = 1;
		long socket = l2ServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate,
				params.encrypt, params.master, params.timeouts, listen_backlog, receiveMTU, transmitMTU);
		boolean success = false;
		try {
			int channel = l2ServerGetPSMImpl(socket);
			serviceRecord.populateL2CAPAttributes(0, channel, params.uuid, params.name);
			registerSDPRecord(serviceRecord);
			success = true;
			return socket;
		} finally {
			if (!success) {
				l2ServerCloseImpl(socket, true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl,
	 * boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		updateSDPRecord(serviceRecord);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection (long)
	 */
	public native long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		l2CloseClientConnection(handle);
	}

	private native void l2ServerCloseImpl(long handle, boolean quietly) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		try {
			unregisterSDPRecord(serviceRecord);
		} finally {
			l2ServerCloseImpl(handle, false);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public native boolean l2Ready(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	// public native int l2Receive(long handle, byte[] inBuf) throws
	// IOException;
	public int l2Receive(long handle, byte[] inBuf) throws IOException {

		throw new IOException("l2Receive() Not supported yet.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public native void l2Send(long handle, byte[] data) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public native int l2GetReceiveMTU(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public native int l2GetTransmitMTU(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public native long l2RemoteAddress(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetSecurityOpt(long, int)
	 */
	public native int l2GetSecurityOpt(long handle, int expected) throws IOException;

	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
		// TODO
		return false;
	}

}