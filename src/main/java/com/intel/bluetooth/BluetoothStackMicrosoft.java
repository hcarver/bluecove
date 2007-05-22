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
import javax.bluetooth.UUID;

public class BluetoothStackMicrosoft implements BluetoothStack {

	boolean peerInitialized = false;
	
	long bluetoothAddress = 0;
	
	BluetoothStackMicrosoft() {
		initialize();
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WINSOCK;
	}
	
	private void initialize() {
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
		
	}
	
	public void initialized() throws BluetoothStateException {
		if (!peerInitialized) {
			throw new BluetoothStateException("Bluetooth system is unavailable");
		}
	}

	public String getLocalDeviceBluetoothAddress() {
		BluetoothPeer bluetoothPeer = BlueCoveImpl.instance().getBluetoothPeer();
		String address;
		try {
			int socket = bluetoothPeer.socket(false, false);
			bluetoothPeer.bind(socket);
			bluetoothAddress = bluetoothPeer.getsockaddress(socket);
			address = Long.toHexString(bluetoothAddress);
			bluetoothPeer.storesockopt(socket);
			bluetoothPeer.close(socket);
		} catch (IOException e) {
			DebugLog.error("get local bluetoothAddress", e);
			address = "";
		}
		return "000000000000".substring(address.length()) + address;
	}

	public String getLocalDeviceName() {
		if (bluetoothAddress == 0) {
			getLocalDeviceBluetoothAddress();
		}
		return BlueCoveImpl.instance().getBluetoothPeer().getradioname(bluetoothAddress);
	}

	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(BlueCoveImpl.instance().getBluetoothPeer().getDeviceClass(bluetoothAddress));
	}
	
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		switch (mode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			BlueCoveImpl.instance().getBluetoothPeer().setDiscoverable(false);
			break;
		case DiscoveryAgent.GIAC:
			BlueCoveImpl.instance().getBluetoothPeer().setDiscoverable(true);
			break;
		case DiscoveryAgent.LIAC:
			BlueCoveImpl.instance().getBluetoothPeer().setDiscoverable(true);
			// TODO Timer to turn it off
			break;
		}
		return true;
	}

	public boolean isLocalDevicePowerOn() {
		int mode = BlueCoveImpl.instance().getBluetoothPeer().getBluetoothRadioMode();
		return ((mode == BluetoothPeer.BTH_MODE_CONNECTABLE) || (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE));
	}
	
	public int getLocalDeviceDiscoverable() {
		int mode = BlueCoveImpl.instance().getBluetoothPeer().getBluetoothRadioMode();
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
			return String.valueOf(BlueCoveImpl.instance().getBluetoothPeer().getDeviceVersion(bluetoothAddress));
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(BlueCoveImpl.instance().getBluetoothPeer().getDeviceManufacturer(bluetoothAddress));
		}
		return null;
	}
	
	//	 --- Device Inquiry
	
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		return BlueCoveImpl.instance().getBluetoothPeer().cancelInquiry();
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return BlueCoveImpl.instance().getBluetoothPeer().runDeviceInquiry(startedNotify, accessCode, listener);
	}

	//	 --- Service search 
	
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		int[] handles;
		try {
			handles = BlueCoveImpl.instance().getBluetoothPeer().runSearchServices(uuidSet, Long.parseLong(device.getBluetoothAddress(), 16));
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

			for (int i = 0; i < handles.length; i++) {
				records[i] = new ServiceRecordImpl(device, handles[i]);
				try {
					records[i].populateRecord(new int[] { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004 });
					if (attrSet != null) {
						records[i].populateRecord(attrSet);
					}
				} catch (Exception e) {
				}
			}
			listener.servicesDiscovered(startedNotify.getTransID(), records);
			return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
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
		}

		/*
		 * retrieve SDP blob
		 */

		byte[] blob = BlueCoveImpl.instance().getBluetoothPeer().getServiceAttributes(sortIDs,
				 		Long.parseLong(serviceRecord.getHostDevice().getBluetoothAddress(), 16),
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
		BluetoothPeer peer = BlueCoveImpl.instance().getBluetoothPeer();
		int socket = peer.socket(authenticate, encrypt);
		peer.connect(socket, address, channel);
		return socket;
	}
	
	public void connectionRfCloseClientConnection(long handle) throws IOException {
		BlueCoveImpl.instance().getBluetoothPeer().close((int)handle);
	}
	
	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return BlueCoveImpl.instance().getBluetoothPeer().getpeeraddress((int)handle);
	}
	
	public int connectionRfRead(long handle) throws IOException {
		return BlueCoveImpl.instance().getBluetoothPeer().recv((int)handle);
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return BlueCoveImpl.instance().getBluetoothPeer().recv((int)handle, b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return (int)BlueCoveImpl.instance().getBluetoothPeer().recvAvailable((int)handle);
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		BlueCoveImpl.instance().getBluetoothPeer().send((int)handle, b);
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		BlueCoveImpl.instance().getBluetoothPeer().send((int)handle, b, off, len);
	}
}
