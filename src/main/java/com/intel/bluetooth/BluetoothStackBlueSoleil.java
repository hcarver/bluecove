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

public class BluetoothStackBlueSoleil implements BluetoothStack {

	private boolean initialized = false;
	
	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_WC_BS);
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
	 * @todo
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(getDeviceClassImpl());
	}

	/**
	 * @todo
	 * There are no functions to set BlueSoleil stack discoverable status.
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return true;
	}

	native boolean isBlueSoleilStarted(int seconds);
	
	private native boolean isBluetoothReady(int seconds);
	
	/**
	 * @todo
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
	
// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}
	
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.deviceInquiryStartedCallback();
		return runDeviceInquiryImpl(startedNotify, accessCode, listener);
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName) {
		DebugLog.debug("deviceDiscoveredCallback", deviceName);
		listener.deviceDiscovered(RemoteDeviceHelper.createRemoteDevice(deviceAddr, deviceName), new DeviceClass(deviceClass));			
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
			uuid = uuidSet[uuidSet.length -1];
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
	
	void servicesFoundCallback(SearchServicesThread startedNotify, DiscoveryListener listener, RemoteDevice device, String serviceName, byte[] uuidValue, int channel, int recordHanlde) {
		ServiceRecordImpl record = new ServiceRecordImpl(device, 0);

		UUID uuid = new UUID(Utils.UUIDByteArrayToString(uuidValue), false);
		
		record.populateRFCOMMAttributes(recordHanlde, channel, uuid, serviceName);
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
	
	public long connectionRfOpenClientConnection(long address, int channel, boolean authenticate, boolean encrypt) throws IOException {
		RemoteDevice listedDevice = RemoteDeviceHelper.getCashedDevice(address);
		if (listedDevice == null) {
			throw new IOException("Device not discovered");
		}
		UUID uuid = (UUID)RemoteDeviceHelper.getStackAttributes(listedDevice, "RFCOMM_channel" + channel);
		if (uuid == null) {
			throw new IOException("Device service not discovered");
		}
		DebugLog.debug("Connect to service UUID", uuid);
		return connectionRfOpenImpl(address, Utils.UUIDToByteArray(uuid));
	}
	
	public native void connectionRfCloseClientConnection(long handle) throws IOException;
	
	private native long rfServerOpenImpl(byte[] uuidValue, String name, boolean authenticate, boolean encrypt) throws IOException;
	
	private native int rfServerSCN(long handle) throws IOException;
	
	public long rfServerOpen(UUID uuid, boolean authenticate, boolean encrypt, String name, ServiceRecordImpl serviceRecord) throws IOException {
		byte[] uuidValue = Utils.UUIDToByteArray(uuid);
		long handle = rfServerOpenImpl(uuidValue, name, authenticate, encrypt);
		int channel = rfServerSCN(handle);
		DebugLog.debug("serverSCN", channel);
		int serviceRecordHandle = (int)handle;
		
		serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, uuid, name);
		
		return handle;

	}
	
	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		throw new NotImplementedError();
	}
	
	public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;
	
	public void connectionRfCloseServerConnection(long handle) throws IOException {
		throw new NotImplementedError();
	}
	
	public native void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;
	
	public native long getConnectionRfRemoteAddress(long handle) throws IOException;
	
	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;
	
	public native void connectionRfFlush(long handle) throws IOException;

}
