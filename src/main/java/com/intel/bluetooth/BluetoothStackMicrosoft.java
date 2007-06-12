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

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

public class BluetoothStackMicrosoft implements BluetoothStack {

	boolean peerInitialized = false;
	
	long localBluetoothAddress = 0;
	
	private BluetoothPeer bluetoothPeer;
	
	BluetoothStackMicrosoft() {
		bluetoothPeer = new BluetoothPeer();
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WINSOCK;
	}
	
	public int getLibraryVersion() {
		return bluetoothPeer.getLibraryVersion();
	}
	
	public int detectBluetoothStack() {
		return bluetoothPeer.detectBluetoothStack();
	}
	
	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
		bluetoothPeer.enableNativeDebug(nativeDebugCallback, on);
	}
	
	public void initialize() {
		try {
			int status = BluetoothPeer.initializationStatus();
			DebugLog.debug("initializationStatus", status);
			if (status == 1) {
				peerInitialized = true;
			}
		} catch (IOException e) {
			DebugLog.fatal("initialization", e);
		}
	}
	
	public void destroy() {
		if (peerInitialized) {
			peerInitialized = false;
			bluetoothPeer.uninitialize();
		}
	}
	
	public void initialized() throws BluetoothStateException {
		if (!peerInitialized) {
			throw new BluetoothStateException("Bluetooth system is unavailable");
		}
	}

	public String getLocalDeviceBluetoothAddress() {
		String address;
		try {
			int socket = bluetoothPeer.socket(false, false);
			bluetoothPeer.bind(socket);
			localBluetoothAddress = bluetoothPeer.getsockaddress(socket);
			address = Long.toHexString(localBluetoothAddress);
			bluetoothPeer.storesockopt(socket);
			bluetoothPeer.close(socket);
		} catch (IOException e) {
			DebugLog.error("get local bluetoothAddress", e);
			address = "";
		}
		return "000000000000".substring(address.length()) + address;
	}

	public String getLocalDeviceName() {
		if (localBluetoothAddress == 0) {
			getLocalDeviceBluetoothAddress();
		}
		return bluetoothPeer.getradioname(localBluetoothAddress);
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return bluetoothPeer.getpeername(address);
	}
	
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(bluetoothPeer.getDeviceClass(localBluetoothAddress));
	}
	
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		switch (mode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			bluetoothPeer.setDiscoverable(false);
			break;
		case DiscoveryAgent.GIAC:
			bluetoothPeer.setDiscoverable(true);
			break;
		case DiscoveryAgent.LIAC:
			bluetoothPeer.setDiscoverable(true);
			// TODO Timer to turn it off
			break;
		}
		return true;
	}

	public boolean isLocalDevicePowerOn() {
		int mode = bluetoothPeer.getBluetoothRadioMode();
		return ((mode == BluetoothPeer.BTH_MODE_CONNECTABLE) || (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE));
	}
	
	public int getLocalDeviceDiscoverable() {
		int mode = bluetoothPeer.getBluetoothRadioMode();
		if (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}
	
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
			return String.valueOf(bluetoothPeer.getDeviceVersion(localBluetoothAddress));
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(bluetoothPeer.getDeviceManufacturer(localBluetoothAddress));
		}
		return null;
	}
	
	//	 --- Device Inquiry
	
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		return bluetoothPeer.cancelInquiry();
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return bluetoothPeer.runDeviceInquiry(startedNotify, accessCode, listener);
	}

	//	 --- Service search 
	
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		int[] handles;
		try {
			handles = bluetoothPeer.runSearchServices(uuidSet, RemoteDeviceHelper.getAddress(device));
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
			boolean hasError = false;
			for (int i = 0; i < handles.length; i++) {
				records[i] = new ServiceRecordImpl(device, handles[i]);
				try {
					if (!records[i].populateRecord(new int[] { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004 })) {
						hasError = true;
					}
					if (attrSet != null) {
						if (!records[i].populateRecord(attrSet)) {
							hasError = true;
						}
					}
				} catch (Exception e) {
					DebugLog.debug("populateRecord error", e);
					hasError = true;
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
		if (NotImplementedError.enabled) {
			throw new NotImplementedError();
		} else {
			return false;
		}
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		/*
		 * copy and sort attrIDs (required by MS Bluetooth)
		 */

		int[] sortIDs = new int[attrIDs.length];

		System.arraycopy(attrIDs, 0, sortIDs, 0, attrIDs.length);

		for (int i = 0; i < sortIDs.length; i++) {
			for (int j = 0; j < sortIDs.length - i - 1; j++) {
				if (sortIDs[j] > sortIDs[j + 1]) {
					int temp = sortIDs[j];
					sortIDs[j] = sortIDs[j + 1];
					sortIDs[j + 1] = temp;
				}
			}
		}

		/*
		 * check for duplicates
		 */

		for (int i = 0; i < sortIDs.length - 1; i++) {
			if (sortIDs[i] == sortIDs[i + 1]) {
				throw new IllegalArgumentException();
			}
			DebugLog.debug("query for ", sortIDs[i]);
		}
		DebugLog.debug("query for ", sortIDs[sortIDs.length - 1]);

		/*
		 * retrieve SDP blob
		 */

		byte[] blob = bluetoothPeer.getServiceAttributes(sortIDs, 
				RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice()),
				(int)serviceRecord.getHandle());

		if (blob.length > 0) {
			try {
				DataElement element = (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
				for (Enumeration e = (Enumeration) element.getValue(); e.hasMoreElements();) {
					serviceRecord.populateAttributeValue((int) ((DataElement) e.nextElement()).getLong(), (DataElement)e.nextElement());
				}
				return true;
			} catch (Exception e) {
				throw new IOException();
			}
		} else {
			return false;
		}
	}

//	 --- Client RFCOMM connections
	
	public long connectionRfOpenClientConnection(long address, int channel, boolean authenticate, boolean encrypt) throws IOException {
		int socket = bluetoothPeer.socket(authenticate, encrypt);
		bluetoothPeer.connect(socket, address, channel);
		return socket;
	}
	
	public void connectionRfCloseClientConnection(long handle) throws IOException {
		bluetoothPeer.close((int)handle);
	}
	
	public long rfServerOpen(UUID uuid, boolean authenticate, boolean encrypt, String name, ServiceRecordImpl serviceRecord) throws IOException {
		/*
		 * open socket
		 */

		int socket = bluetoothPeer.socket(authenticate, encrypt);
		bluetoothPeer.bind(socket);
		bluetoothPeer.listen(socket);

		int channel = bluetoothPeer.getsockchannel(socket);
		DebugLog.debug("service channel ", channel);
		
		int serviceRecordHandle = socket; 
		serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, uuid, name);

		/*
		 * register service
		 */
		serviceRecord.setHandle(bluetoothPeer.registerService(serviceRecord.toByteArray()));
		
		return socket;
	}
	
	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {

		/*
		 * close socket
		 */
		bluetoothPeer.close((int)handle);
		/*
		 * unregister service
		 */
		bluetoothPeer.unregisterService(serviceRecord.getHandle());
	}
	
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		return bluetoothPeer.accept((int)handle);
	}
	
	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		bluetoothPeer.unregisterService(serviceRecord.getHandle());
		serviceRecord.setHandle(bluetoothPeer.registerService(((ServiceRecordImpl) serviceRecord).toByteArray()));
		DebugLog.debug("new serviceRecord", serviceRecord);
	}
	
	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return bluetoothPeer.getpeeraddress((int)handle);
	}
	
	public int connectionRfRead(long handle) throws IOException {
		return bluetoothPeer.recv((int)handle);
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return bluetoothPeer.recv((int)handle, b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return (int)bluetoothPeer.recvAvailable((int)handle);
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		bluetoothPeer.send((int)handle, b);
	}
	
	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		bluetoothPeer.send((int)handle, b, off, len);
	}
	
	public void connectionRfFlush(long handle) throws IOException {
		// TODO are there any flush
	}

}
