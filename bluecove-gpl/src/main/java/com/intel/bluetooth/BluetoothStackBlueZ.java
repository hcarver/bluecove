/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

/**
 * Property "bluecove.deviceID" or "bluecove.deviceAddress" can be used to select Local Bluetooth device.
 * 
 */
class BluetoothStackBlueZ implements BluetoothStack {

	public static final String NATIVE_BLUECOVE_LIB_BLUEZ = "bluecove";

	static final int NATIVE_LIBRARY_VERSION = BlueCoveImpl.nativeLibraryVersionExpected;

	// TODO what is the real number for Attributes retrievable ?
	private final static int ATTR_RETRIEVABLE_MAX = 256;

	private final static int LISTEN_BACKLOG_RFCOMM = 4;

	private final static int LISTEN_BACKLOG_L2CAP = 4;

	private final static Vector devicesUsed = new Vector();

	private final static String BLUEZ_DEVICEID_PREFIX = "BlueZ";

	private int deviceID = -1;

	private int deviceDescriptor;

	private long localDeviceBTAddress;

	private long sdpSesion;

	private int registeredServicesCount = 0;

	private Hashtable/* <String,String> */propertiesMap;

	private DiscoveryListener discoveryListener;

	// Prevent the device from been discovered twice
	private Vector/* <RemoteDevice> */discoveredDevices;

	private boolean deviceInquiryCanceled = false;

	private final int l2cap_receiveMTU_max = 65535;

	BluetoothStackBlueZ() {
	}

	// --- Library initialization

	public String getStackID() {
		return BlueCoveImpl.STACK_BLUEZ;
	}

	public String toString() {
		if (deviceID >= 0) {
			return getStackID() + ":" + deviceID;
		} else {
			return getStackID();
		}
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
		return LibraryInformation.library(NATIVE_BLUECOVE_LIB_BLUEZ);
	}

	public native int getLibraryVersionNative();

	public int getLibraryVersion() throws BluetoothStateException {
		int version = getLibraryVersionNative();
		if (version != NATIVE_LIBRARY_VERSION) {
			DebugLog.fatal("BlueCove native library version mismatch " + version + " expected "
					+ NATIVE_LIBRARY_VERSION);
			throw new BluetoothStateException("BlueCove native library version mismatch");
		}
		return version;
	}

	public int detectBluetoothStack() {
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_BLUEZ;
	}

	private native int nativeGetDeviceID(int findNumber, int findBlueZDeviceID, long findLocalDeviceBTAddress)
			throws BluetoothStateException;

	private native int nativeOpenDevice(int deviceID) throws BluetoothStateException;

	public void initialize() throws BluetoothStateException {
		long findLocalDeviceBTAddress = -1;
		String findID = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ID);
		int findNumber = -1;
		int findBlueZDeviceID = -1;
		if (findID != null) {
			if (findID.startsWith(BLUEZ_DEVICEID_PREFIX)) {
				findBlueZDeviceID = Integer.parseInt(findID.substring(BLUEZ_DEVICEID_PREFIX.length()));
			} else {
				findNumber = Integer.parseInt(findID);
			}
		}
		String deviceAddressStr = BlueCoveImpl
				.getConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS);
		if (deviceAddressStr != null) {
			findLocalDeviceBTAddress = Long.parseLong(deviceAddressStr, 0x10);
		}
		int foundDeviceID = nativeGetDeviceID(findNumber, findBlueZDeviceID, findLocalDeviceBTAddress);
		if (devicesUsed.contains(new Long(foundDeviceID))) {
			throw new BluetoothStateException("LocalDevice " + foundDeviceID + " alredy in use");
		}

		this.deviceID = foundDeviceID;
		DebugLog.debug("localDeviceID", deviceID);
		deviceDescriptor = nativeOpenDevice(deviceID);
		localDeviceBTAddress = getLocalDeviceBluetoothAddressImpl(deviceDescriptor);
		propertiesMap = new Hashtable/* <String,String> */();
		final String TRUE = "true";
		final String FALSE = "false";
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX, "7");
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX, "7");
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY_SCAN, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE_SCAN, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_ATTR_RETRIEVABLE_MAX, String
				.valueOf(ATTR_RETRIEVABLE_MAX));
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_MASTER_SWITCH, FALSE);
		propertiesMap
				.put(BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX, String.valueOf(l2cap_receiveMTU_max));
		// propertiesMap.put("bluecove.radio.version", );
		// propertiesMap.put("bluecove.radio.manufacturer", );
		// propertiesMap.put("bluecove.stack.version", );
		propertiesMap.put(BlueCoveLocalDeviceProperties.LOCAL_DEVICE_PROPERTY_DEVICE_ID, String.valueOf(deviceID));

		devicesUsed.addElement(new Long(deviceID));
	}

	private native void nativeCloseDevice(int deviceDescriptor);

	public void destroy() {
		if (sdpSesion != 0) {
			try {
				long s = sdpSesion;
				sdpSesion = 0;
				closeSDPSessionImpl(s, true);
			} catch (ServiceRegistrationException ignore) {
			}
		}
		nativeCloseDevice(deviceDescriptor);
		if (deviceID >= 0) {
			devicesUsed.removeElement(new Long(deviceID));
			deviceID = -1;
		}
	}

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return UtilsJavaSE.isCurrentThreadInterrupted();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	public int getFeatureSet() {
		return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
	}

	// --- LocalDevice

	private native long getLocalDeviceBluetoothAddressImpl(int deviceDescriptor) throws BluetoothStateException;

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		return RemoteDeviceHelper.getBluetoothAddress(getLocalDeviceBluetoothAddressImpl(deviceDescriptor));
	}

	private native int nativeGetDeviceClass(int deviceDescriptor);

	public DeviceClass getLocalDeviceClass() {
		int record = nativeGetDeviceClass(deviceDescriptor);
		if (record == 0xff000000) {
			// could not be determined
			return null;
		}
		return new DeviceClass(record);
	}

	private native String nativeGetDeviceName(int deviceDescriptor);

	public String getLocalDeviceName() {
		return nativeGetDeviceName(deviceDescriptor);
	}

	public boolean isLocalDevicePowerOn() {
		// Have no idea how turn on and off device on BlueZ, as well to how to
		// detect this condition.
		return true;
	}

	private native int[] getLocalDevicesID();

	public String getLocalDeviceProperty(String property) {
		if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_DEVICES_LIST.equals(property)) {
			int[] ids = getLocalDevicesID();
			StringBuffer b = new StringBuffer();
			if (ids != null) {
				for (int i = 0; i < ids.length; i++) {
					if (i != 0) {
						b.append(',');
					}
					b.append(BLUEZ_DEVICEID_PREFIX);
					b.append(String.valueOf(ids[i]));
				}
			}
			return b.toString();
		}
		// Some Hack and testing functions, not documented
		if (property.startsWith("bluecove.nativeFunction:")) {
			String functionDescr = property.substring(property.indexOf(':') + 1, property.length());
			int paramIdx = functionDescr.indexOf(':');
			if (paramIdx == -1) {
				throw new RuntimeException("Invalid native function " + functionDescr + "; arguments expected");
			}
			String function = functionDescr.substring(0, paramIdx);
			long address = RemoteDeviceHelper.getAddress(functionDescr.substring(function.length() + 1, functionDescr
					.length()));
			if ("getRemoteDeviceVersionInfo".equals(function)) {
				return getRemoteDeviceVersionInfo(address);
			} else if ("getRemoteDeviceRSSI".equals(function)) {
				return String.valueOf(getRemoteDeviceRSSI(address));
			}
			return null;
		}
		return (String) propertiesMap.get(property);
	}

	private native int nativeGetLocalDeviceDiscoverable(int deviceDescriptor);

	public int getLocalDeviceDiscoverable() {
		return nativeGetLocalDeviceDiscoverable(deviceDescriptor);
	}

	private native int nativeSetLocalDeviceDiscoverable(int deviceDescriptor, int mode);

	/**
	 * From JSR-82 docs
	 * 
	 * @return <code>true</code> if the request succeeded, otherwise <code>false</code> if the request failed because
	 *         the BCC denied the request; <code>false</code> if the Bluetooth system does not support the access mode
	 *         specified in <code>mode</code>
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		int curentMode = getLocalDeviceDiscoverable();
		if (curentMode == mode) {
			return true;
		} else {
			int error = nativeSetLocalDeviceDiscoverable(deviceDescriptor, mode);
			if (error != 0) {
				DebugLog.error("Unable to change discovery mode. It may be because you aren't root; " + error);
				return false;
			}
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		throw new NotSupportedRuntimeException(getStackID());
	}

	// --- Remote Device authentication

	public boolean authenticateRemoteDevice(long address) throws IOException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#authenticateRemoteDevice(long, java.lang.String)
	 */
	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
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

	// --- Some testing functions accessible by LocalDevice.getProperty

	private native String getRemoteDeviceVersionInfoImpl(int deviceDescriptor, long address);

	public String getRemoteDeviceVersionInfo(long address) {
		return getRemoteDeviceVersionInfoImpl(this.deviceDescriptor, address);
	}

	private native int getRemoteDeviceRSSIImpl(int deviceDescriptor, long address);

	public int getRemoteDeviceRSSI(long address) {
		return getRemoteDeviceRSSIImpl(this.deviceDescriptor, address);
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

	// --- Device Inquiry

	private native int runDeviceInquiryImpl(DeviceInquiryRunnable inquiryRunnable, DeviceInquiryThread startedNotify,
			int deviceID, int deviceDescriptor, int accessCode, int inquiryLength, int maxResponses,
			DiscoveryListener listener) throws BluetoothStateException;

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		if (discoveryListener != null) {
			throw new BluetoothStateException("Another inquiry already running");
		}
		discoveryListener = listener;
		discoveredDevices = new Vector();
		deviceInquiryCanceled = false;
		DeviceInquiryRunnable inquiryRunnable = new DeviceInquiryRunnable() {

			public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
					throws BluetoothStateException {
				try {
					int discType = runDeviceInquiryImpl(this, startedNotify, deviceID, deviceDescriptor, accessCode, 8,
							20, listener);
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
				RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackBlueZ.this, deviceAddr,
						deviceName, paired);
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
		};
		return DeviceInquiryThread.startInquiry(this, inquiryRunnable, accessCode, listener);
	}

	private native boolean deviceInquiryCancelImpl(int deviceDescriptor);

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (discoveryListener != null && discoveryListener == listener) {
			deviceInquiryCanceled = true;
			return deviceInquiryCancelImpl(deviceDescriptor);
		}
		return false;
	}

	private native String getRemoteDeviceFriendlyNameImpl(int deviceDescriptor, long remoteAddress) throws IOException;

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return getRemoteDeviceFriendlyNameImpl(deviceDescriptor, address);
	}

	// --- Service search

	private native int runSearchServicesImpl(SearchServicesThread sst, long localDeviceBTAddress, byte[][] uuidValues,
			long remoteDeviceAddress) throws SearchServicesException;

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {

		SearchServicesRunnable searchRunnable = new SearchServicesRunnable() {

			public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device,
					DiscoveryListener listener) throws BluetoothStateException {
				sst.searchServicesStartedCallback();
				try {
					byte[][] uuidValues = new byte[uuidSet.length][];
					for (int i = 0; i < uuidSet.length; i++) {
						uuidValues[i] = Utils.UUIDToByteArray(uuidSet[i]);
					}
					int respCode = runSearchServicesImpl(sst, localDeviceBTAddress, uuidValues, RemoteDeviceHelper
							.getAddress(device));
					if ((respCode != DiscoveryListener.SERVICE_SEARCH_ERROR) && (sst.isTerminated())) {
						return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
					} else if (respCode == DiscoveryListener.SERVICE_SEARCH_COMPLETED) {
						Vector records = sst.getServicesRecords();
						if (records.size() != 0) {
							DebugLog.debug("SearchServices finished", sst.getTransID());
							ServiceRecord[] servRecordArray = (ServiceRecord[]) Utils.vector2toArray(records,
									new ServiceRecord[records.size()]);
							listener.servicesDiscovered(sst.getTransID(), servRecordArray);
						}
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
		};
		return SearchServicesThread.startSearchServices(this, searchRunnable, attrSet, uuidSet, device, listener);
	}

	public boolean serviceDiscoveredCallback(SearchServicesThread sst, long sdpSession, long handle) {
		if (sst.isTerminated()) {
			return true;
		}
		ServiceRecordImpl servRecord = new ServiceRecordImpl(this, sst.getDevice(), handle);
		int[] attrIDs = sst.getAttrSet();
		long remoteDeviceAddress = RemoteDeviceHelper.getAddress(sst.getDevice());
		populateServiceRecordAttributeValuesImpl(this.localDeviceBTAddress, remoteDeviceAddress, sdpSession, handle,
				attrIDs, servRecord);
		sst.addServicesRecords(servRecord);
		return false;
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			return sst.setTerminated();
		} else {
			return false;
		}
	}

	private native boolean populateServiceRecordAttributeValuesImpl(long localDeviceBTAddress,
			long remoteDeviceAddress, long sdpSession, long handle, int[] attrIDs, ServiceRecordImpl serviceRecord);

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		long remoteDeviceAddress = RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
		return populateServiceRecordAttributeValuesImpl(this.localDeviceBTAddress, remoteDeviceAddress, 0,
				serviceRecord.getHandle(), attrIDs, serviceRecord);
	}

	// --- SDP Server

	private native long openSDPSessionImpl() throws ServiceRegistrationException;

	private synchronized long getSDPSession() throws ServiceRegistrationException {
		if (this.sdpSesion == 0) {
			sdpSesion = openSDPSessionImpl();
			DebugLog.debug("created SDPSession", sdpSesion);
		}
		return sdpSesion;
	}

	private native void closeSDPSessionImpl(long sdpSesion, boolean quietly) throws ServiceRegistrationException;

	private native long registerSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, byte[] record)
			throws ServiceRegistrationException;

	private native void updateSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, long handle, byte[] record)
			throws ServiceRegistrationException;

	private native void unregisterSDPServiceImpl(long sdpSesion, long localDeviceBTAddress, long handle, byte[] record)
			throws ServiceRegistrationException;

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
		long handle = registerSDPServiceImpl(getSDPSession(), this.localDeviceBTAddress, getSDPBinary(serviceRecord));
		serviceRecord.setHandle(handle);
		serviceRecord.populateAttributeValue(BluetoothConsts.ServiceRecordHandle, new DataElement(DataElement.U_INT_4,
				handle));
		registeredServicesCount++;
	}

	private void updateSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		updateSDPServiceImpl(getSDPSession(), this.localDeviceBTAddress, serviceRecord.getHandle(),
				getSDPBinary(serviceRecord));
	}

	private synchronized void unregisterSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		try {
			unregisterSDPServiceImpl(getSDPSession(), this.localDeviceBTAddress, serviceRecord.getHandle(),
					getSDPBinary(serviceRecord));
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
		return connectionRfOpenClientConnectionImpl(localDeviceBTAddress, params.address, params.channel,
				params.authenticate, params.encrypt, params.timeout);
	}

	public native void connectionRfCloseClientConnection(long handle) throws IOException;

	public native int rfGetSecurityOptImpl(long handle) throws IOException;

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return rfGetSecurityOptImpl(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		return false;
	}

	private native long rfServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			boolean encrypt, boolean master, boolean timeouts, int backlog) throws IOException;

	private native int rfServerGetChannelIDImpl(long handle) throws IOException;

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		long socket = rfServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate,
				params.encrypt, params.master, params.timeouts, LISTEN_BACKLOG_RFCOMM);
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

	public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

	public void connectionRfCloseServerConnection(long clientHandle) throws IOException {
		connectionRfCloseClientConnection(clientHandle);
	}

	// --- Shared Client and Server RFCOMM connections

	public int connectionRfRead(long handle) throws IOException {
		byte[] data = new byte[1];
		int size = connectionRfRead(handle, data, 0, 1);
		if (size == -1) {
			return -1;
		}
		return 0xFF & data[0];
	}

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public native void connectionRfFlush(long handle) throws IOException;

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;

	// --- Client and Server L2CAP connections

	private void validateMTU(int receiveMTU, int transmitMTU) {
		if (receiveMTU > l2cap_receiveMTU_max) {
			throw new IllegalArgumentException("invalid ReceiveMTU value " + receiveMTU);
		}
	}

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
		validateMTU(receiveMTU, transmitMTU);
		return l2OpenClientConnectionImpl(this.localDeviceBTAddress, params.address, params.channel,
				params.authenticate, params.encrypt, receiveMTU, transmitMTU, params.timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public native void l2CloseClientConnection(long handle) throws IOException;

	private native long l2ServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate,
			boolean encrypt, boolean master, boolean timeouts, int backlog, int receiveMTU, int transmitMTU,
			int assignPsm) throws IOException;

	public native int l2ServerGetPSMImpl(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth. BluetoothConnectionNotifierParams, int,
	 * int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		validateMTU(receiveMTU, transmitMTU);
		long socket = l2ServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate,
				params.encrypt, params.master, params.timeouts, LISTEN_BACKLOG_L2CAP, receiveMTU, transmitMTU,
				params.bluecove_ext_psm);
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
	public native int l2Receive(long handle, byte[] inBuf) throws IOException;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
		return false;
	}
}
