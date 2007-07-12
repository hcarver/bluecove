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

public class BluetoothStackMicrosoft extends BluetoothPeer implements BluetoothStack {

	boolean peerInitialized = false;
	
	long localBluetoothAddress = 0;
	
	private DiscoveryListener currentDeviceDiscoveryListener;
	
	BluetoothStackMicrosoft() {
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WINSOCK;
	}
	
	public int getLibraryVersion() {
		return super.getLibraryVersion();
	}
	
	public int detectBluetoothStack() {
		return super.detectBluetoothStack();
	}
	
	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
		super.enableNativeDebug(nativeDebugCallback, on);
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
			super.uninitialize();
		}
	}
	
	public void initialized() throws BluetoothStateException {
		if (!peerInitialized) {
			throw new BluetoothStateException("Bluetooth system is unavailable");
		}
	}

	public String getLocalDeviceBluetoothAddress() {
		try {
			long socket = super.socket(false, false);
			super.bind(socket);
			localBluetoothAddress = super.getsockaddress(socket);
			String address = RemoteDeviceHelper.getBluetoothAddress(localBluetoothAddress);
			super.storesockopt(socket);
			super.close(socket);
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
		return super.getradioname(localBluetoothAddress);
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return super.getpeername(address);
	}
	
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(super.getDeviceClass(localBluetoothAddress));
	}
	
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		switch (mode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			super.setDiscoverable(false);
			break;
		case DiscoveryAgent.GIAC:
			super.setDiscoverable(true);
			break;
		case DiscoveryAgent.LIAC:
			super.setDiscoverable(true);
			// TODO Timer to turn it off
			break;
		}
		return true;
	}

	public boolean isLocalDevicePowerOn() {
		int mode = super.getBluetoothRadioMode();
		return ((mode == BluetoothPeer.BTH_MODE_CONNECTABLE) || (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE));
	}
	
	public int getLocalDeviceDiscoverable() {
		int mode = super.getBluetoothRadioMode();
		if (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}
	
	public String getLocalDeviceProperty(String property) {
		final String TRUE = "true";
		final String FALSE = "false";
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
		if ("bluetooth.connected.page".equals(property)) {
			return TRUE;
		}

		if ("bluetooth.sd.attr.retrievable.max".equals(property)) {
			// TODO what is the real number ?
			return "256";
		}
		if ("bluetooth.master.switch".equals(property)) {
			return FALSE;
		}
		if ("bluetooth.l2cap.receiveMTU.max".equals(property)) {
			return "0";
		}
		
		if ("bluecove.radio.version".equals(property)) {
			return String.valueOf(super.getDeviceVersion(localBluetoothAddress));
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(super.getDeviceManufacturer(localBluetoothAddress));
		}
		return null;
	}
	
	//	 --- Device Inquiry
	
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		if (currentDeviceDiscoveryListener != null) {
			throw new BluetoothStateException();
		}
		currentDeviceDiscoveryListener = listener;
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (currentDeviceDiscoveryListener != listener) {
			return false;
		}
		return super.cancelInquiry();
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		try {
			return super.runDeviceInquiry(startedNotify, accessCode, listener);
		} finally {
			currentDeviceDiscoveryListener = null;
		}
	}
	
	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName) {
		super.deviceDiscoveredCallback(listener, deviceAddr, deviceClass, deviceName);
	}

	//	 --- Service search 
	
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		int[] handles;
		try {
			handles = super.runSearchServices(uuidSet, RemoteDeviceHelper.getAddress(device));
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

		byte[] blob = super.getServiceAttributes(sortIDs, 
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
		long socket = super.socket(authenticate, encrypt);
		super.connect(socket, address, channel);
		return socket;
	}
	
	public void connectionRfCloseClientConnection(long handle) throws IOException {
		super.close(handle);
	}
	
	public long rfServerOpen(UUID uuid, boolean authenticate, boolean encrypt, String name, ServiceRecordImpl serviceRecord) throws IOException {
		/*
		 * open socket
		 */

		long socket = super.socket(authenticate, encrypt);
		super.bind(socket);
		super.listen(socket);

		int channel = super.getsockchannel(socket);
		DebugLog.debug("service channel ", channel);
		
		int serviceRecordHandle = (int)socket; 
		serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, uuid, name, false);

		/*
		 * register service
		 */
		serviceRecord.setHandle(super.registerService(serviceRecord.toByteArray()));
		
		return socket;
	}
	
	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {

		/*
		 * close socket
		 */
		super.close(handle);
		/*
		 * unregister service
		 */
		super.unregisterService(serviceRecord.getHandle());
	}
	
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		return super.accept(handle);
	}
	
	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		super.unregisterService(serviceRecord.getHandle());
		serviceRecord.setHandle(super.registerService(((ServiceRecordImpl) serviceRecord).toByteArray()));
		DebugLog.debug("new serviceRecord", serviceRecord);
	}
	
	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return super.getpeeraddress(handle);
	}
	
	public int connectionRfRead(long handle) throws IOException {
		return super.recv(handle);
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return super.recv(handle, b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return super.recvAvailable(handle);
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		super.send(handle, b);
	}
	
	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		super.send(handle, b, off, len);
	}
	
	public void connectionRfFlush(long handle) throws IOException {
		// TODO are there any flush
	}

}
