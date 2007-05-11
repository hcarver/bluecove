/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class BluetoothPeer {

	public static int BTH_MODE_POWER_OFF = 1;
	
	public static int BTH_MODE_CONNECTABLE = 2;
	
	public static int BTH_MODE_DISCOVERABLE = 3;
	
	public static boolean peerInitialized;
	
	static {
		NativeLibLoader.isAvailable();
	}

	class InquiryThread extends Thread {
		private int accessCode;

		private DiscoveryListener listener;

		public InquiryThread(int accessCode, DiscoveryListener listener) {
			this.accessCode = accessCode;
			this.listener = listener;
		}

		public void run() {
			listener.inquiryCompleted(doInquiry(accessCode, listener));
		}
	}

	class SearchServicesThread extends Thread {
		private int[] attrSet;

		private UUID[] uuidSet;

		private RemoteDevice device;

		private DiscoveryListener listener;

		public SearchServicesThread(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) {
			this.attrSet = attrSet;
			this.uuidSet = uuidSet;
			this.device = device;
			this.listener = listener;
		}

		public void run() {
			int[] handles = getServiceHandles(uuidSet, Long.parseLong(device.getBluetoothAddress(), 16));

			if (handles == null) {
				listener.serviceSearchCompleted(0, DiscoveryListener.SERVICE_SEARCH_ERROR);
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

				listener.servicesDiscovered(0, records);
				listener.serviceSearchCompleted(0, DiscoveryListener.SERVICE_SEARCH_COMPLETED);
			} else {
				listener.serviceSearchCompleted(0, DiscoveryListener.SERVICE_SEARCH_NO_RECORDS);
			}
		}
	}
	
	/**
	 * This is implementation specific class, only BlueCoveImpl can create this class
	 *
	 */
	BluetoothPeer() {
		try {
			int status = initializationStatus();
			DebugLog.debug("initializationStatus", status);
			if (DebugLog.isDebugEnabled()) {
				enableNativeDebug(true);
			}
			if (status == 1) {
				peerInitialized = true;
			}
		} catch (IOException e) {
			DebugLog.fatal("initialization", e);
		}
	}

	public void initialized() throws BluetoothStateException {
		if (!peerInitialized) {
			throw new BluetoothStateException("Bluetooth system is unavailable");
		}
	}
	
	public void startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		(new InquiryThread(accessCode, listener)).start();
	}

	public void startSearchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		(new SearchServicesThread(attrSet, uuidSet, device, listener)).start();
	}

	public native int initializationStatus() throws IOException;
	
	public native void enableNativeDebug(boolean on);
	
	public static void nativeDebugCallback(int lineN, String message) {
		DebugLog.debugNative("intelbth.cpp:" + lineN, message);
	}
	
	public native int getDeviceClass();
	
	public native void setDiscoverable(boolean on) throws BluetoothStateException;
	
	public native int getBluetoothRadioMode();
	
	/*
	 * perform synchronous inquiry
	 */

	public native int doInquiry(int accessCode, DiscoveryListener listener);

	/*
	 * cancel current inquiry (if any)
	 */

	public native boolean cancelInquiry();

	/*
	 * perform synchronous service discovery
	 */

	public native int[] getServiceHandles(UUID[] uuidSet, long address);

	/*
	 * get service attributes
	 */

	public native byte[] getServiceAttributes(int[] attrIDs, long address, int handle) throws IOException;

	/*
	 * register service
	 */

	public native long registerService(byte[] record) throws IOException;

	/*
	 * unregister service
	 */

	public native void unregisterService(long handle) throws IOException;

	/*
	 * socket operations
	 */

	public native int socket(boolean authenticate, boolean encrypt) throws IOException;

	public native long getsockaddress(int socket) throws IOException;

	public native int getsockchannel(int socket) throws IOException;

	public native void connect(int socket, long address, int channel) throws IOException;

	public native void bind(int socket) throws IOException;
	
	public native void listen(int socket) throws IOException;

	public native int accept(int socket) throws IOException;

	public native long recvAvailable(int socket) throws IOException;
	
	public native int recv(int socket) throws IOException;

	public native int recv(int socket, byte[] b, int off, int len) throws IOException;

	public native void send(int socket, int b) throws IOException;

	public native void send(int socket, byte[] b, int off, int len) throws IOException;

	public native void close(int socket) throws IOException;

	public native String getpeername(long address) throws IOException;

	public native long getpeeraddress(int socket) throws IOException;

	public native String getradioname(long address);
}