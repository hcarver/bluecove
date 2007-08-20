/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothStackBlueSoleil implements BluetoothStack {

	private boolean initialized = false;

	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_BLUESOLEIL);
	}

	BluetoothStackBlueSoleil() {
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_BLUESOLEIL;
	}

	public native int getLibraryVersion();

	public native int detectBluetoothStack();

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	public native boolean initializeImpl();

	public void initialize() {
		if (!initializeImpl()) {
			DebugLog.fatal("Can't initialize BlueSoleil");
			throw new RuntimeException("BlueSoleil BluetoothStack not found");
		}
		initialized = true;
	}

	private native void uninitialize();

	public void destroy() {
		if (initialized) {
			uninitialize();
			initialized = false;
			DebugLog.debug("BlueSoleil destroyed");
		}
	}

	protected void finalize() {
		destroy();
	}

	public native String getLocalDeviceBluetoothAddress();

	public native String getLocalDeviceName();

	public native int getDeviceClassImpl();

	/**
	 * There are no functions in BlueSoleil stack.
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(getDeviceClassImpl());
	}

	/**
	 * There are no functions to set BlueSoleil stack discoverable status.
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return true;
	}

	native boolean isBlueSoleilStarted(int seconds);

	private native boolean isBluetoothReady(int seconds);

	/**
	 * There are no functions to find BlueSoleil discoverable status.
	 */
	public int getLocalDeviceDiscoverable() {
		if (isBluetoothReady(2)) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	public boolean isLocalDevicePowerOn() {
		return isBluetoothReady(15);
	}

	native int getStackVersionInfo();

	native int getDeviceVersion();

	native int getDeviceManufacturer();

	public String getLocalDeviceProperty(String property) {
		final String TRUE = "true";
		if ("bluetooth.connected.devices.max".equals(property)) {
			return "7";
		}
		if ("bluetooth.sd.trans.max".equals(property)) {
			return "1";
		}
		if ("bluetooth.connected.inquiry.scan".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.page.scan".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.inquiry".equals(property)) {
			return TRUE;
		}

		// service attributes are not supported.
		if ("bluetooth.sd.attr.retrievable.max".equals(property)) {
			return "0";
		}

//		if ("bluecove.radio.version".equals(property)) {
//			return String.valueOf(getDeviceVersion());
//		}
//		if ("bluecove.radio.manufacturer".equals(property)) {
//			return String.valueOf(getDeviceManufacturer());
//		}
		if ("bluecove.stack.version".equals(property)) {
			return String.valueOf(getStackVersionInfo());
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}
// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.deviceInquiryStartedCallback();
		return runDeviceInquiryImpl(startedNotify, accessCode, listener);
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired) {
		DebugLog.debug("deviceDiscoveredCallback", deviceName);
		listener.deviceDiscovered(RemoteDeviceHelper.createRemoteDevice(deviceAddr, deviceName, paired), new DeviceClass(deviceClass));
	}

	public native boolean cancelInquirympl();

	public boolean cancelInquiry(DiscoveryListener listener) {
		return cancelInquirympl();
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		// TODO Properly if possible
		return null;
	}

// --- Service search

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public boolean cancelServiceSearch(int transID) {
		return false;
	}

	private native int runSearchServicesImpl(SearchServicesThread startedNotify, DiscoveryListener listener, byte[] uuidValue, long address, RemoteDevice device) throws BluetoothStateException;

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		UUID uuid = null;
		if ((uuidSet != null) && (uuidSet.length > 0)) {
			uuid = uuidSet[uuidSet.length - 1];
		}
		return runSearchServicesImpl(startedNotify, listener, Utils.UUIDToByteArray(uuid), RemoteDeviceHelper.getAddress(device), device);
	}

	/*
	This is all we have under the BlueSun.
	struct SPPEX_SERVICE_INFO {
		DWORD dwSize;
		DWORD dwSDAPRecordHanlde;
		UUID serviceClassUuid128;
		CHAR szServiceName[MAX_SERVICE_NAME_LENGTH];
		UCHAR ucServiceChannel;
	}
	 */

	public void servicesFoundCallback(SearchServicesThread startedNotify, DiscoveryListener listener, RemoteDevice device, String serviceName, byte[] uuidValue, int channel, long recordHanlde) {
		ServiceRecordImpl record = new ServiceRecordImpl(device, 0);

		UUID uuid = new UUID(Utils.UUIDByteArrayToString(uuidValue), false);

		record.populateRFCOMMAttributes(recordHanlde, channel, uuid, serviceName, BluetoothConsts.obexUUIDs.contains(uuid));
		DebugLog.debug("servicesFoundCallback", record);

		RemoteDevice listedDevice = RemoteDeviceHelper.createRemoteDevice(device);
		RemoteDeviceHelper.setStackAttributes(listedDevice, "RFCOMM_channel" + channel, uuid);

		ServiceRecord[] records = new ServiceRecordImpl[1];
		records[0] = record;
		listener.servicesDiscovered(startedNotify.getTransID(), records);
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		return false;
	}

//	 --- Client RFCOMM connections

	private native long connectionRfOpenImpl(long address, byte[] uuidValue) throws IOException;

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		if (params.authenticate || params.encrypt) {
			throw new IOException("authenticate not supported on BlueSoleil");
		}
		RemoteDevice listedDevice = RemoteDeviceHelper.getCashedDevice(params.address);
		if (listedDevice == null) {
			throw new IOException("Device not discovered");
		}
		UUID uuid = (UUID)RemoteDeviceHelper.getStackAttributes(listedDevice, "RFCOMM_channel" + params.channel);
		if (uuid == null) {
			throw new IOException("Device service not discovered");
		}
		DebugLog.debug("Connect to service UUID", uuid);
		return connectionRfOpenImpl(params.address, Utils.UUIDToByteArray(uuid));
	}

	public native void connectionRfCloseClientConnection(long handle) throws IOException;

	public int getSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	private native long rfServerOpenImpl(byte[] uuidValue, String name, boolean authenticate, boolean encrypt) throws IOException;

	private native int rfServerSCN(long handle) throws IOException;

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
		if (params.authenticate || params.encrypt) {
			throw new IOException("authenticate not supported on BlueSoleil");
		}
		byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
		long handle = rfServerOpenImpl(uuidValue, params.name, params.authenticate, params.encrypt);
		int channel = rfServerSCN(handle);
		DebugLog.debug("serverSCN", channel);
		int serviceRecordHandle = (int)handle;

		serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, params.uuid, params.name, false);

		return handle;
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		if (!acceptAndOpen) {
			throw new ServiceRegistrationException("Not Supported on " + getStackID());
		}
	}

	public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	public native void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;

	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public native void connectionRfFlush(long handle) throws IOException;

	//	---------------------- Client and Server L2CAP connections ----------------------

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams, int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public void l2CloseClientConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams, int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		throw new ServiceRegistrationException("Not Supported on" + getStackID());
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public boolean l2Ready(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public int l2Receive(long handle, byte[] inBuf) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public void l2Send(long handle, byte[] data) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public int l2GetReceiveMTU(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public int l2GetTransmitMTU(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public long l2RemoteAddress(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

}
