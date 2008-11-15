/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothStackMicrosoft implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

	private static final int BTH_MODE_POWER_OFF = 1;

	private static final int BTH_MODE_CONNECTABLE = 2;

	private static final int BTH_MODE_DISCOVERABLE = 3;

	private static BluetoothStackMicrosoft singleInstance = null;

	private boolean peerInitialized = false;

	private boolean windowsCE;

	private long localBluetoothAddress = 0;

	private DiscoveryListener currentDeviceDiscoveryListener;

	private Thread limitedDiscoverableTimer;

	// TODO what is the real number for Attributes retrievable ?
	private final static int ATTR_RETRIEVABLE_MAX = 256;

	private Hashtable deviceDiscoveryDevices;

	BluetoothStackMicrosoft() {
	}

	// ---------------------- Library initialization

	public String getStackID() {
		return BlueCoveImpl.STACK_WINSOCK;
	}

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
		return LibraryInformation.library(BlueCoveImpl.NATIVE_LIB_MS);
	}

	public String toString() {
		return getStackID();
	}

	public native int getLibraryVersion();

	public native int detectBluetoothStack();

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	private static native int initializationStatus() throws IOException;

	private native void uninitialize();

	private native boolean isWindowsCE();

	public void initialize() throws BluetoothStateException {
		if (singleInstance != null) {
			throw new BluetoothStateException("Only one instance of " + getStackID() + " stack supported");
		}
		try {
			int status = initializationStatus();
			DebugLog.debug("initializationStatus", status);
			if (status == 1) {
				peerInitialized = true;
			}
			windowsCE = isWindowsCE();
			singleInstance = this;
		} catch (BluetoothStateException e) {
			throw e;
		} catch (IOException e) {
			DebugLog.fatal("initialization", e);
			throw new BluetoothStateException(e.getMessage());
		}
	}

	public void destroy() {
		if (singleInstance != this) {
			throw new RuntimeException("Destroy invalid instance");
		}
		if (peerInitialized) {
			peerInitialized = false;
			uninitialize();
		}
		cancelLimitedDiscoverableTimer();
		singleInstance = null;
	}

	private void initialized() throws BluetoothStateException {
		if (!peerInitialized) {
			throw new BluetoothStateException("Bluetooth system is unavailable");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	public int getFeatureSet() {
		return FEATURE_SERVICE_ATTRIBUTES | (windowsCE ? 0 : FEATURE_SET_DEVICE_SERVICE_CLASSES);
	}

	// ---------------------- LocalDevice

	private native int getDeviceClass(long address);

	private native void setDiscoverable(boolean on) throws BluetoothStateException;

	private native int getBluetoothRadioMode();

	private native String getradioname(long address);

	private native int getDeviceVersion(long address);

	private native int getDeviceManufacturer(long address);

	public String getLocalDeviceBluetoothAddress() {
		try {
			long socket = socket(false, false);
			bind(socket);
			localBluetoothAddress = getsockaddress(socket);
			String address = RemoteDeviceHelper.getBluetoothAddress(localBluetoothAddress);
			storesockopt(socket);
			close(socket);
			return address;
		} catch (IOException e) {
			DebugLog.error("get local bluetoothAddress", e);
			return "000000000000";
		}
	}

	public String getLocalDeviceName() {
		if (localBluetoothAddress == 0) {
			getLocalDeviceBluetoothAddress();
		}
		return getradioname(localBluetoothAddress);
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return getpeername(address);
	}

	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(getDeviceClass(localBluetoothAddress));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		// Done in rfServerUpdateServiceRecord
	}

	private void cancelLimitedDiscoverableTimer() {
		if (limitedDiscoverableTimer != null) {
			limitedDiscoverableTimer.interrupt();
			limitedDiscoverableTimer = null;
		}
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		switch (mode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			cancelLimitedDiscoverableTimer();
			DebugLog.debug("setDiscoverable(false)");
			setDiscoverable(false);
			return (DiscoveryAgent.NOT_DISCOVERABLE == getLocalDeviceDiscoverable());
		case DiscoveryAgent.GIAC:
			cancelLimitedDiscoverableTimer();
			DebugLog.debug("setDiscoverable(true)");
			setDiscoverable(true);
			return (DiscoveryAgent.GIAC == getLocalDeviceDiscoverable());
		case DiscoveryAgent.LIAC:
			cancelLimitedDiscoverableTimer();
			DebugLog.debug("setDiscoverable(LIAC)");
			setDiscoverable(true);
			if (!(DiscoveryAgent.GIAC == getLocalDeviceDiscoverable())) {
				return false;
			}
			// Timer to turn it off
			limitedDiscoverableTimer = Utils.schedule(60 * 1000, new Runnable() {
				public void run() {
					try {
						setDiscoverable(false);
					} catch (BluetoothStateException e) {
						DebugLog.debug("error setDiscoverable", e);
					} finally {
						limitedDiscoverableTimer = null;
					}
				}
			});
			return true;
		}
		return false;
	}

	public boolean isLocalDevicePowerOn() {
		int mode = getBluetoothRadioMode();
		if (mode == BTH_MODE_POWER_OFF) {
			return false;
		}
		return ((mode == BTH_MODE_CONNECTABLE) || (mode == BTH_MODE_DISCOVERABLE));
	}

	public int getLocalDeviceDiscoverable() {
		int mode = getBluetoothRadioMode();
		if (mode == BTH_MODE_DISCOVERABLE) {
			if (limitedDiscoverableTimer != null) {
				DebugLog.debug("Discoverable = LIAC");
				return DiscoveryAgent.LIAC;
			} else {
				DebugLog.debug("Discoverable = GIAC");
				return DiscoveryAgent.GIAC;
			}
		} else {
			DebugLog.debug("Discoverable = NOT_DISCOVERABLE");
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	public String getLocalDeviceProperty(String property) {
		if ("bluetooth.connected.devices.max".equals(property)) {
			return "7";
		}
		if ("bluetooth.sd.trans.max".equals(property)) {
			return "7";
		}
		if ("bluetooth.connected.inquiry.scan".equals(property)) {
			return BlueCoveImpl.TRUE;
		}
		if ("bluetooth.connected.page.scan".equals(property)) {
			return BlueCoveImpl.TRUE;
		}
		if ("bluetooth.connected.inquiry".equals(property)) {
			return BlueCoveImpl.TRUE;
		}
		if ("bluetooth.connected.page".equals(property)) {
			return BlueCoveImpl.TRUE;
		}

		if ("bluetooth.sd.attr.retrievable.max".equals(property)) {
			return String.valueOf(ATTR_RETRIEVABLE_MAX);
		}
		if ("bluetooth.master.switch".equals(property)) {
			return BlueCoveImpl.FALSE;
		}
		if ("bluetooth.l2cap.receiveMTU.max".equals(property)) {
			return "0";
		}

		if ("bluecove.radio.version".equals(property)) {
			return String.valueOf(getDeviceVersion(localBluetoothAddress));
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(getDeviceManufacturer(localBluetoothAddress));
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return UtilsJavaSE.isCurrentThreadInterrupted();
	}

	private native boolean retrieveDevicesImpl(int option, RetrieveDevicesCallback retrieveDevicesCallback);

	public RemoteDevice[] retrieveDevices(int option) {
		if (windowsCE) {
			return null;
		}
		final Vector devices = new Vector();
		RetrieveDevicesCallback retrieveDevicesCallback = new RetrieveDevicesCallback() {
			public void deviceFoundCallback(long deviceAddr, int deviceClass, String deviceName, boolean paired) {
				DebugLog.debug("device found", deviceAddr);
				RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackMicrosoft.this,
						deviceAddr, deviceName, paired);
				devices.add(remoteDevice);
			}
		};
		if (retrieveDevicesImpl(option, retrieveDevicesCallback)) {
			return RemoteDeviceHelper.remoteDeviceListToArray(devices);
		} else {
			return null;
		}
	}

	private native boolean isRemoteDeviceTrustedImpl(long address);

	public Boolean isRemoteDeviceTrusted(long address) {
		if (windowsCE) {
			return null;
		}
		return new Boolean(isRemoteDeviceTrustedImpl(address));
	}

	private native boolean isRemoteDeviceAuthenticatedImpl(long address);

	public Boolean isRemoteDeviceAuthenticated(long address) {
		if (windowsCE) {
			return null;
		}
		return new Boolean(isRemoteDeviceAuthenticatedImpl(address));
	}

	private native boolean authenticateRemoteDeviceImpl(long address, String passkey) throws IOException;

	public boolean authenticateRemoteDevice(long address) throws IOException {
		return authenticateRemoteDeviceImpl(address, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#authenticateRemoteDevice(long, java.lang.String)
	 */
	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
		return authenticateRemoteDeviceImpl(address, passkey);
	}

	private native void removeAuthenticationWithRemoteDeviceImpl(long address) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice (long)
	 */
	public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
		removeAuthenticationWithRemoteDeviceImpl(address);
	}

	// ---------------------- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		if (currentDeviceDiscoveryListener != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		currentDeviceDiscoveryListener = listener;
		return DeviceInquiryThread.startInquiry(this, this, accessCode, listener);
	}

	/*
	 * cancel current inquiry (if any)
	 */
	private native boolean cancelInquiry();

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (currentDeviceDiscoveryListener != listener) {
			return false;
		}
		// no further deviceDiscovered() events will occur for this inquiry
		currentDeviceDiscoveryListener = null;
		return cancelInquiry();
	}

	/*
	 * perform synchronous inquiry
	 */
	private native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, int duration,
			DiscoveryListener listener) throws BluetoothStateException;

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			deviceDiscoveryDevices = new Hashtable();
			int discType = runDeviceInquiryImpl(startedNotify, accessCode, DeviceInquiryThread
					.getConfigDeviceInquiryDuration(), listener);
			if (discType == DiscoveryListener.INQUIRY_COMPLETED) {
				for (Enumeration en = deviceDiscoveryDevices.keys(); en.hasMoreElements();) {
					RemoteDevice remoteDevice = (RemoteDevice) en.nextElement();
					DeviceClass deviceClass = (DeviceClass) deviceDiscoveryDevices.get(remoteDevice);
					listener.deviceDiscovered(remoteDevice, deviceClass);
					// If cancelInquiry has been called
					if (currentDeviceDiscoveryListener == null) {
						return DiscoveryListener.INQUIRY_TERMINATED;
					}
				}
			}
			return discType;
		} finally {
			deviceDiscoveryDevices = null;
			currentDeviceDiscoveryListener = null;
		}
	}

	/**
	 * This is called when all device discoved by stack. To avoid problems with getpeername we will postpone the calls
	 * to User deviceDiscovered function until runDeviceInquiry is finished.
	 */
	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, deviceAddr, deviceName, paired);
		if ((currentDeviceDiscoveryListener == null) || (deviceDiscoveryDevices == null)
				|| (currentDeviceDiscoveryListener != listener)) {
			return;
		}
		DeviceClass cod = new DeviceClass(deviceClass);
		DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
		DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
		deviceDiscoveryDevices.put(remoteDevice, cod);
	}

	// ---------------------- Service search

	/*
	 * perform synchronous service discovery
	 */
	public native int[] runSearchServices(UUID[] uuidSet, long address) throws SearchServicesException;

	/*
	 * get service attributes
	 */
	public native byte[] getServiceAttributes(int[] attrIDs, long address, int handle) throws IOException;

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, this, attrSet, uuidSet, device, listener);
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		int[] handles;
		try {
			handles = runSearchServices(uuidSet, RemoteDeviceHelper.getAddress(device));
		} catch (SearchServicesDeviceNotReachableException e) {
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		} catch (SearchServicesTerminatedException e) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		} catch (SearchServicesException e) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
		if (handles == null) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		} else if (handles.length > 0) {
			ServiceRecord[] records = new ServiceRecordImpl[handles.length];
			int[] requiredAttrIDs = new int[] { BluetoothConsts.ServiceRecordHandle,
					BluetoothConsts.ServiceClassIDList, BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID,
					BluetoothConsts.ProtocolDescriptorList };
			boolean hasError = false;
			for (int i = 0; i < handles.length; i++) {
				records[i] = new ServiceRecordImpl(this, device, handles[i]);
				try {
					records[i].populateRecord(requiredAttrIDs);
					if (attrSet != null) {
						records[i].populateRecord(attrSet);
					}
				} catch (Exception e) {
					DebugLog.debug("populateRecord error", e);
					hasError = true;
				}
				if (startedNotify.isTerminated()) {
					return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
				}
			}
			listener.servicesDiscovered(startedNotify.getTransID(), records);
			if (hasError) {
				return DiscoveryListener.SERVICE_SEARCH_ERROR;
			} else {
				return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
			}
		} else {
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			return sst.setTerminated();
		} else {
			return false;
		}
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		if (attrIDs.length > ATTR_RETRIEVABLE_MAX) {
			throw new IllegalArgumentException();
		}
		/*
		 * retrieve SDP blob
		 */
		byte[] blob = getServiceAttributes(attrIDs, RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice()),
				(int) serviceRecord.getHandle());

		if (blob.length > 0) {
			try {
				boolean anyRetrived = false;
				DataElement element = (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
				for (Enumeration e = (Enumeration) element.getValue(); e.hasMoreElements();) {
					int attrID = (int) ((DataElement) e.nextElement()).getLong();
					serviceRecord.populateAttributeValue(attrID, (DataElement) e.nextElement());
					if (!anyRetrived) {
						for (int i = 0; i < attrIDs.length; i++) {
							if (attrIDs[i] == attrID) {
								anyRetrived = true;
								break;
							}
						}
					}
				}
				return anyRetrived;
			} catch (IOException e) {
				throw e;
			} catch (Throwable e) {
				throw new IOException();
			}
		} else {
			return false;
		}
	}

	/*
	 * socket operations
	 */
	private native long socket(boolean authenticate, boolean encrypt) throws IOException;

	private native long getsockaddress(long socket) throws IOException;

	private native void storesockopt(long socket);

	private native int getsockchannel(long socket) throws IOException;

	private native void connect(long socket, long address, int channel) throws IOException;

	private native void bind(long socket) throws IOException;

	private native void listen(long socket) throws IOException;

	private native long accept(long socket) throws IOException;

	private native int recvAvailable(long socket) throws IOException;

	private native int recv(long socket) throws IOException;

	private native int recv(long socket, byte[] b, int off, int len) throws IOException;

	private native void send(long socket, int b) throws IOException;

	private native void send(long socket, byte[] b, int off, int len) throws IOException;

	private native void close(long socket) throws IOException;

	private native String getpeername(long address) throws IOException;

	private native long getpeeraddress(long socket) throws IOException;

	// ---------------------- Client RFCOMM connections

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth .BluetoothConnectionParams,
	 * int, int)
	 */
	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		long socket = socket(params.authenticate, params.encrypt);
		boolean success = false;
		try {
			connect(socket, params.address, params.channel);
			success = true;
		} finally {
			if (!success) {
				close(socket);
			}
		}
		return socket;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		close(handle);
	}

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		/*
		 * open socket
		 */
		long socket = socket(params.authenticate, params.encrypt);
		boolean success = false;
		try {

			synchronized (this) {
				bind(socket);
			}
			listen(socket);

			int channel = getsockchannel(socket);
			DebugLog.debug("service channel ", channel);

			long serviceRecordHandle = socket;
			serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, params.uuid, params.name, params.obex);

			/*
			 * register service
			 */
			serviceRecord.setHandle(registerService(serviceRecord.toByteArray(), serviceRecord.deviceServiceClasses));

			success = true;
		} finally {
			if (!success) {
				try {
					close(socket);
				} catch (IOException e) {
					DebugLog.debug("close on failure", e);
				}
			}
		}
		return socket;
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		try {
			/*
			 * close socket
			 */
			close(handle);
		} finally {
			/*
			 * unregister service
			 */
			unregisterService(serviceRecord.getHandle());
		}
	}

	/*
	 * register service
	 */
	private native long registerService(byte[] record, int classOfDevice) throws ServiceRegistrationException;

	/*
	 * unregister service
	 */
	private native void unregisterService(long handle) throws ServiceRegistrationException;

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		return accept(handle);
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		unregisterService(serviceRecord.getHandle());
		byte[] blob;
		try {
			blob = serviceRecord.toByteArray();
		} catch (IOException e) {
			throw new ServiceRegistrationException(e.toString());
		}
		serviceRecord.setHandle(registerService(blob, serviceRecord.deviceServiceClasses));
		DebugLog.debug("new serviceRecord", serviceRecord);
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return getpeeraddress(handle);
	}

	public int connectionRfRead(long handle) throws IOException {
		return recv(handle);
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return recv(handle, b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return recvAvailable(handle);
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		send(handle, b);
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		send(handle, b, off, len);
	}

	public void connectionRfFlush(long handle) throws IOException {
		// TODO are there any flush
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		return false;
	}

	// ---------------------- Client and Server L2CAP connections

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth .BluetoothConnectionParams,
	 * int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public void l2CloseClientConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth. BluetoothConnectionNotifierParams, int,
	 * int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl,
	 * boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		throw new ServiceRegistrationException("Not Supported on" + getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection (long)
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetSecurityOpt(long, int)
	 */
	public int l2GetSecurityOpt(long handle, int expected) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public boolean l2Ready(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public int l2Receive(long handle, byte[] inBuf) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public void l2Send(long handle, byte[] data) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public int l2GetReceiveMTU(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public int l2GetTransmitMTU(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public long l2RemoteAddress(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}
}
