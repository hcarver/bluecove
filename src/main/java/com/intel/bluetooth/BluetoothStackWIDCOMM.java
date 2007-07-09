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

public class BluetoothStackWIDCOMM implements BluetoothStack {

	private boolean initialized = false;
	
	private Vector deviceDiscoveryListeners = new Vector/*<DiscoveryListener>*/();
	
	private Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();
	
	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_WIDCOMM);
	}
	
	BluetoothStackWIDCOMM() {
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WIDCOMM;
	}
	
	public native int getLibraryVersion();
	
	public native int detectBluetoothStack();
	
	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);
	
	
	public void initialize() {
		if (!initializeImpl()) {
			throw new RuntimeException("WIDCOMM BluetoothStack not found");
		}
		initialized = true;
	}
	
	public native boolean initializeImpl();
	
	private native void uninitialize();
	
	public void destroy() {
		if (initialized) {
			uninitialize();
			initialized = false;
			DebugLog.debug("WIDCOMM destroyed");
		}
	}
	
	protected void finalize() {
		destroy();
	}

	public native String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

	public native String getLocalDeviceName();

	
	public native int getDeviceClassImpl();
	
	/**
	 * There are no functions to set WIDCOMM stack 
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(getDeviceClassImpl());
	}

	/**
	 * There are no functions to set WIDCOMM stack discoverable status.
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return true;
	}

	private native boolean isStackServerUp();
	
	/**
	 * There are no functions to find WIDCOMM discoverable status.
	 */
	public int getLocalDeviceDiscoverable() {
		if (isStackServerUp()) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	public native boolean isLocalDevicePowerOn();
	
	private native String getBTWVersionInfo();
	
	private native int getDeviceVersion();
	
	private native int getDeviceManufacturer();
	
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
		
		if ("bluecove.radio.version".equals(property)) {
			return String.valueOf(getDeviceVersion());
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(getDeviceManufacturer());
		}
		if ("bluecove.stack.version".equals(property)) {
			return getBTWVersionInfo();
		}
		
		return null;
	}
	
	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		deviceDiscoveryListeners.addElement(listener);
		deviceDiscoveryListenerReportedDevices.put(listener, new Vector());
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}
	
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		try {
			return runDeviceInquiryImpl(startedNotify, accessCode, listener);
		} finally {
			deviceDiscoveryListeners.removeElement(listener);
			deviceDiscoveryListenerReportedDevices.remove(listener);
		}
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName) {
		DebugLog.debug("deviceDiscoveredCallback deviceName", deviceName);
		Vector reported = (Vector)deviceDiscoveryListenerReportedDevices.get(listener);
		String deviceAddrStr = RemoteDeviceHelper.getBluetoothAddress(deviceAddr);
		for (Enumeration iter = reported.elements(); iter.hasMoreElements();) {
			RemoteDevice device = (RemoteDevice) iter.nextElement();
			if (deviceAddrStr.equalsIgnoreCase(device.getBluetoothAddress())) {
				if (Utils.isStringSet(deviceName)) {
					// Update device name 
					RemoteDeviceHelper.createRemoteDevice(deviceAddr, deviceName);
				}
				return;
			}
			
		}
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(deviceAddr, deviceName);
		reported.addElement(remoteDevice);
		DeviceClass cod = new DeviceClass(deviceClass);
		DebugLog.debug("deviceDiscoveredCallback addtress", remoteDevice.getBluetoothAddress());
		DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
		listener.deviceDiscovered(remoteDevice, cod);			
	}

	private native boolean deviceInquiryCancelImpl();
	
	public boolean cancelInquiry(DiscoveryListener listener) {
		if (!deviceDiscoveryListeners.removeElement(listener)) {
			return false;	
		}
		return deviceInquiryCancelImpl();
	}
	
	public native String getRemoteDeviceFriendlyName(long address) throws IOException;
	
	// --- Service search 
	
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	private native void cancelServiceSearchImpl();
	
	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			sst.setTerminated();
			cancelServiceSearchImpl();
			return true;
		} else {
			return false;
		}
	}

	private native long[] runSearchServicesImpl(SearchServicesThread startedNotify, byte[] uuidValue, long address) throws BluetoothStateException, SearchServicesTerminatedException;

	private native byte[] getServiceAttribute(int attrID, long handle) throws IOException;
	
	private native boolean isServiceRecordDiscoverable(long address, long handle) throws IOException;
	
	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		// Retrive all Records, Filter here in Java
		synchronized (BluetoothStackWIDCOMM.class) {
			byte[] uuidValue = Utils.UUIDToByteArray(BluetoothConsts.L2CAP_PROTOCOL_UUID);
			long[] handles;
			try {
				handles = runSearchServicesImpl(startedNotify, uuidValue, RemoteDeviceHelper.getAddress(device));
			} catch (SearchServicesTerminatedException e) {
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			}
			if (handles == null) {
				return DiscoveryListener.SERVICE_SEARCH_ERROR;
			} else if (handles.length > 0) {

				boolean reqRFCOMM = false;
				// boolean reqL2CAP = false;
				UUID uuidFiler = null;
				// If Search for sepcific service, select its UUID
				for (int u = 0; u < uuidSet.length; u++) {
					if (uuidSet[u].equals(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
						// reqL2CAP = true;
						continue;
					}
					if (uuidSet[u].equals(BluetoothConsts.RFCOMM_PROTOCOL_UUID)) {
						reqRFCOMM = true;
						continue;
					}
					uuidFiler = uuidSet[u];
					break;
				}
				if ((uuidFiler == null) && (reqRFCOMM)) {
					uuidFiler = BluetoothConsts.RFCOMM_PROTOCOL_UUID;
				}

				Vector records = new Vector();
				for (int i = 0; i < handles.length; i++) {
					ServiceRecordImpl sr = new ServiceRecordImpl(device, handles[i]);
					try {
						sr.populateRecord(new int[] { BluetoothConsts.ServiceClassIDList });
						if ((uuidFiler != null) && !sr.hasServiceClassUUID(uuidFiler)) {
							if (BluetoothStackWIDCOMMSDPInputStream.debug) {
								DebugLog.debug("filtered ServiceRecord (" + i + ")", sr);
							}
							continue;
						}
						if (BluetoothStackWIDCOMMSDPInputStream.debug) {
							DebugLog.debug("accepted ServiceRecord (" + i + ")", sr);
						}
						if (!isServiceRecordDiscoverable(RemoteDeviceHelper.getAddress(device), sr.getHandle())) {
							continue;
						}

						records.addElement(sr);
						sr.populateRecord(new int[] { BluetoothConsts.ServiceRecordHandle,
								BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID,
								BluetoothConsts.ProtocolDescriptorList });
						if (attrSet != null) {
							sr.populateRecord(attrSet);
						}
						DebugLog.debug("ServiceRecord (" + i + ") sr.handle", handles[i]);
						DebugLog.debug("ServiceRecord (" + i + ")", sr);
					} catch (Exception e) {
						DebugLog.debug("populateRecord error", e);
					}
					if (startedNotify.isTerminated()) {
						return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
					}
				}
				if (records.size() != 0) {
					ServiceRecord[] fileteredRecords = (ServiceRecord[])Utils.vector2toArray(records, new ServiceRecord[records.size()]);
					listener.servicesDiscovered(startedNotify.getTransID(), fileteredRecords);
					return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
				}
			}
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}
	

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		for (int i = 0; i < attrIDs.length; i++) {
			int id = attrIDs[i];
			try {
				byte[] sdpStruct = getServiceAttribute(id, serviceRecord.getHandle());
				if (sdpStruct != null) {
					if (BluetoothStackWIDCOMMSDPInputStream.debug) {
						DebugLog.debug("decode attribute " + id + " Ox" + Integer.toHexString(id));
					}
					DataElement element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(sdpStruct)))
							.readElement();
					serviceRecord.populateAttributeValue(id, element);
				} else {
					if (BluetoothStackWIDCOMMSDPInputStream.debug) {
						DebugLog.debug("no data for attribute " + id + " Ox" + Integer.toHexString(id));
					}
				}
			} catch (Throwable e) {
				if (BluetoothStackWIDCOMMSDPInputStream.debug) {
					DebugLog.error("error populate attribute " + id + " Ox" + Integer.toHexString(id), e);
				}
			}
		}
		return true;
	}
	
//	 --- Client RFCOMM connections
	
	public native long connectionRfOpenClientConnection(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;
	
	public native void closeRfCommPort(long handle) throws IOException;
	
	public void connectionRfCloseClientConnection(long handle) throws IOException {
		closeRfCommPort(handle);
	}

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;
	
	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public void connectionRfFlush(long handle) throws IOException {
		// TODO are there any flush
	}
	
	private native long rfServerOpenImpl(byte[] uuidValue, byte[] uuidValue2, String name, boolean authenticate, boolean encrypt) throws IOException;
	
	private native int rfServerSCN(long handle) throws IOException;
	
	public long rfServerOpen(UUID uuid, boolean authenticate, boolean encrypt, String name, ServiceRecordImpl serviceRecord) throws IOException {
		byte[] uuidValue = Utils.UUIDToByteArray(uuid);
		long handle = rfServerOpenImpl(uuidValue, Utils.UUIDToByteArray(BluetoothConsts.SERIAL_PORT_UUID), name, authenticate, encrypt);
		int channel = rfServerSCN(handle);
		DebugLog.debug("serverSCN", channel);
		int serviceRecordHandle = (int)handle;
		
		serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, uuid, name);
		
		return handle;
	}
	
	private native void rfServerAddAttribute(long handle, int attrID, short attrType, char[] value) throws ServiceRegistrationException;
	
	private void rfServerAddAttribute(long handle, int attrID, short attrType, String value) throws ServiceRegistrationException {
		char[] cvalue = value.toCharArray();
		rfServerAddAttribute(handle, attrID, attrType, cvalue);
	}
	
	private char[] long2char(long value, int len) {
		char[] cvalue = new char[len];
		long l = value;
		for (int i = len -1; i >= 0; i --) {
			cvalue[i] = (char)(l & 0xFF);
			l >>= 8;
		}
		return cvalue;
	}
	
	private char[] bytes2char(byte[] value, int len) {
		char[] cvalue = new char[len];
		for (int i = 0; i < len;  i ++) {
			cvalue[i] = (char)value[i];
		}
		return cvalue;
	}
	
	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		int[] ids = serviceRecord.getAttributeIDs();
		if ((ids == null) || (ids.length == 0)) {
			return;
		}
		// from WIDCOMM BtIfDefinitions.h
		final short NULL_DESC_TYPE = 0;
		final short UINT_DESC_TYPE = 1;
		final short TWO_COMP_INT_DESC_TYPE = 2;
		final short UUID_DESC_TYPE = 3;
		final short TEXT_STR_DESC_TYPE = 4;
		final short BOOLEAN_DESC_TYPE = 5;
		final short DATA_ELE_SEQ_DESC_TYPE = 6;
		final short DATA_ELE_ALT_DESC_TYPE = 7;
		final short URL_DESC_TYPE = 8;
		
		for (int i = 0; i < ids.length; i++) {
			int id = ids[i];
			switch (id) {
			case BluetoothConsts.ServiceRecordHandle:
			case BluetoothConsts.ServiceClassIDList:
			case BluetoothConsts.ProtocolDescriptorList:
			case BluetoothConsts.AttributeIDServiceName:
				continue;
			}
			
			DataElement d = serviceRecord.getAttributeValue(id);
			switch (d.getDataType()) {
			case DataElement.U_INT_1:
				rfServerAddAttribute(handle, id, UINT_DESC_TYPE, long2char(d.getLong(), 1));
				break;
			case DataElement.U_INT_2:
				rfServerAddAttribute(handle, id, UINT_DESC_TYPE, long2char(d.getLong(), 2));
				break;
			case DataElement.U_INT_4:
				rfServerAddAttribute(handle, id, UINT_DESC_TYPE, long2char(d.getLong(), 4));
				break;
			case DataElement.U_INT_8:
				rfServerAddAttribute(handle, id, UINT_DESC_TYPE, bytes2char((byte[])d.getValue(), 8));
				break;
			case DataElement.U_INT_16:
				rfServerAddAttribute(handle, id, UINT_DESC_TYPE, bytes2char((byte[])d.getValue(), 16));
				break;
			case DataElement.INT_1:
				rfServerAddAttribute(handle, id, TWO_COMP_INT_DESC_TYPE, long2char(d.getLong(), 1));
				break;
			case DataElement.INT_2:
				rfServerAddAttribute(handle, id, TWO_COMP_INT_DESC_TYPE, long2char(d.getLong(), 2));
				break;
			case DataElement.INT_4:
				rfServerAddAttribute(handle, id, TWO_COMP_INT_DESC_TYPE, long2char(d.getLong(), 4));
				break;
			case DataElement.INT_8:
				rfServerAddAttribute(handle, id, TWO_COMP_INT_DESC_TYPE, long2char(d.getLong(), 8));
				break;
			case DataElement.INT_16:
				rfServerAddAttribute(handle, id, TWO_COMP_INT_DESC_TYPE, bytes2char((byte[])d.getValue(), 16));
				break;
			case DataElement.URL:
				rfServerAddAttribute(handle, id, URL_DESC_TYPE, d.getValue().toString());
				break;
			case DataElement.STRING:
				rfServerAddAttribute(handle, id, TEXT_STR_DESC_TYPE, d.getValue().toString());
				break;
			case DataElement.NULL:
				rfServerAddAttribute(handle, id, NULL_DESC_TYPE, "");
				break;
			case DataElement.BOOL:
				rfServerAddAttribute(handle, id, BOOLEAN_DESC_TYPE, d.getBoolean()?"TRUE":"FALSE");
				break;
			case DataElement.UUID:
				rfServerAddAttribute(handle, id, UUID_DESC_TYPE, ((UUID)d.getValue()).toString());
				break;
			case DataElement.DATSEQ:
			case DataElement.DATALT:
				// TODO
			}
		}
	}
	
	public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;
	
	public native void connectionRfCloseServerConnection(long handle) throws IOException;
	
	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		closeRfCommPort(handle);
	}
}
