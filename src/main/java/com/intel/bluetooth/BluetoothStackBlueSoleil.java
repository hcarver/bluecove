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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class BluetoothStackBlueSoleil implements BluetoothStack {

	private boolean initialized = false;
	
	private Hashtable devices = new Hashtable();
	
	private Hashtable connectionHandles = new Hashtable();
	
	BluetoothStackBlueSoleil() {
		initialize();
		initialized = true;
	}
	
	public String getStackID() {
		return BlueCoveImpl.STACK_BLUESOLEIL;
	}
	
	private native void initialize();
	
	private native void uninitialize();
	
	public void destroy() {
		if (initialized) {
			for(Enumeration en = connectionHandles.keys(); en.hasMoreElements(); ) {
				Long handle = (Long)en.nextElement();
				try {
					long[] handles =(long[])connectionHandles.get(handle);
					connectionRfCloseImpl(handles[0], handles[1]);
				} catch (Throwable e) {
				} 
			}
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

	/**
	 * @todo
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(BlueCoveImpl.instance().getBluetoothPeer().getDeviceClass(0));
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
		listener.deviceDiscovered(new RemoteDeviceImpl(deviceAddr, deviceName), new DeviceClass(deviceClass));			
	}

	public native boolean cancelInquirympl();
	
	public boolean cancelInquiry(DiscoveryListener listener) {
		return cancelInquirympl();
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
		return runSearchServicesImpl(startedNotify, listener, Utils.UUIDToByteArray(uuid), ((RemoteDeviceImpl)device).getAddress(), device);
	}

	/*
	This is all we have under the Blue Sun.
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
		
		Long address = new Long(((RemoteDeviceImpl)device).getAddress());
		RemoteDeviceImpl listedDevice = (RemoteDeviceImpl)devices.get(address);
		if (listedDevice == null) {
			devices.put(address, device);
			listedDevice = (RemoteDeviceImpl)device;
		}
		listedDevice.setStackAttributes("RFCOMM_channel" + channel, uuidValue);
		
		ServiceRecord[] records = new ServiceRecordImpl[1];
		records[0] = record;
		listener.servicesDiscovered(startedNotify.getTransID(), records);
	}
	
	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		return false;
	}
	
//	 --- Client RFCOMM connections
	
	private native long[] connectionRfOpenImpl(long address, byte[] uuidValue) throws IOException;
	
	public long connectionRfOpenClientConnection(long address, int channel, boolean authenticate, boolean encrypt) throws IOException {
		Long addressLong = new Long(address);
		RemoteDeviceImpl listedDevice = (RemoteDeviceImpl)devices.get(addressLong);
		if (listedDevice == null) {
			throw new IOException("Device not discovered");
		}
		byte[] uuidValue = (byte[])listedDevice.getStackAttributes("RFCOMM_channel" + channel);
		if (uuidValue == null) {
			throw new IOException("Device service not discovered");
		}
		
		long[] handles = connectionRfOpenImpl(address, uuidValue);
		connectionHandles.put(new Long(handles[0]), handles);
		
		return handles[0];
	}
	
	private native void connectionRfCloseImpl(long comHandle, long connectionHandle) throws IOException;
	
	public void connectionRfCloseClientConnection(long handle) throws IOException {
		long[] handles =(long[])connectionHandles.remove(new Long(handle));
		if (handles == null) {
			throw new IOException("handle not found");
		}
		connectionRfCloseImpl(handles[0], handles[1]);
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		// TODO
	}
	
	public native long getConnectionRfRemoteAddress(long handle) throws IOException;
	
	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

}
