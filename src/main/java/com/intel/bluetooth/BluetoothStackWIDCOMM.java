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
import javax.bluetooth.UUID;

public class BluetoothStackWIDCOMM implements BluetoothStack {

	private boolean initialized = false;
	
	private Vector deviceDiscoveryListeners = new Vector/*<DiscoveryListener>*/();
	
	private Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();
	
	BluetoothStackWIDCOMM() {
		if (!initialize()) {
			throw new RuntimeException("WIDCOMM BluetoothStack not found");
		}
		initialized = true;
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WIDCOMM;
	}
	public native boolean initialize();
	
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

	/**
	 * @todo
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(BlueCoveImpl.instance().getBluetoothPeer().getDeviceClass(0));
	}

	/**
	 * @todo
	 * There are no functions to set WIDCOMM stack discoverable status.
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return true;
	}

	private native boolean isStackServerUp();
	
	/**
	 * @todo
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
		for (Enumeration iter = reported.elements(); iter.hasMoreElements();) {
			RemoteDeviceImpl device = (RemoteDeviceImpl) iter.nextElement();
			if (device.getAddress() == deviceAddr) {
				return;
			}
			
		}
		RemoteDeviceImpl remoteDevice = new RemoteDeviceImpl(deviceAddr, deviceName);
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
	
	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		// Retrive all Records, Filter here in Java
		byte[] uuidValue = Utils.UUIDToByteArray(BluetoothConsts.L2CAP_PROTOCOL_UUID);
		long[] handles;
		try {
			handles = runSearchServicesImpl(startedNotify, uuidValue, ((RemoteDeviceImpl) device).getAddress());
		} catch (SearchServicesTerminatedException e) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		}
		if (handles == null) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		} else if (handles.length > 0) {

			boolean reqRFCOMM = false;
			//boolean reqL2CAP = false;
			UUID uuidFiler = null;
			// If Search for sepcific service, select its UUID
			for (int u = 0; u < uuidSet.length; u++) {
				if (uuidSet[u].equals(BluetoothConsts.L2CAP_PROTOCOL_UUID))  {
					//reqL2CAP = true;
					continue;
				}
				if (uuidSet[u].equals(BluetoothConsts.RFCOMM_PROTOCOL_UUID))  {
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
					sr.populateRecord(new int[] {BluetoothConsts.ServiceClassIDList});
					if ((uuidFiler != null) && !sr.hasServiceClassUUID(uuidFiler)) {
						continue;
					}
					records.addElement(sr);
					sr.populateRecord(new int[] { BluetoothConsts.ServiceRecordHandle,
							BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID,
							BluetoothConsts.ProtocolDescriptorList });
					if (attrSet != null) {
						sr.populateRecord(attrSet);
					}
				} catch (Exception e) {
					DebugLog.debug("populateRecord error", e);
				}
				if (startedNotify.isTerminated()) {
					return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
				}
			}
			ServiceRecord[] fileteredRecords = (ServiceRecord[])records.toArray(new ServiceRecord[records.size()]);  
			listener.servicesDiscovered(startedNotify.getTransID(), fileteredRecords);
			return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
		} else {
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}
	
	private native byte[] getServiceAttribute(int attrID, long handle) throws IOException;
	

	// Simple test
//	private native int getServiceAttributeRFCommScn(long handle) throws IOException;
//	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
//		UUID uuid = new UUID("B1011111111111111111111111110001", false);
//		int channel = getServiceAttributeRFCommScn(serviceRecord.getHandle());
//		serviceRecord.populateRFCOMMAttributes(0, channel, uuid, "");
//		return true;
//	}
	
	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		for (int i = 0; i < attrIDs.length; i++) {
			try {
				byte[] sdpStruct = getServiceAttribute(attrIDs[i], serviceRecord.getHandle());
				if (sdpStruct != null) {
					//DebugLog.debug("decode attribute", attrIDs[i]);
					DataElement element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(sdpStruct)))
							.readElement();
					serviceRecord.populateAttributeValue(attrIDs[i], element);
				} else {
					//DebugLog.debug("no data for attribute", attrIDs[i]);
				}
			} catch (Throwable e) {
				DebugLog.debug("populateServicesRecord attribute " + attrIDs[i] + " error", e);
			}
		}
		return true;
	}
	
//	 --- Client RFCOMM connections
	
	public native long connectionRfOpenClientConnection(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;
	
	public native void connectionRfCloseClientConnection(long handle) throws IOException;

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;
	
	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		// TODO
	}
}
