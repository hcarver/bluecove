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
import javax.bluetooth.UUID;

public class BluetoothPeer {

	public static int BTH_MODE_POWER_OFF = 1;
	
	public static int BTH_MODE_CONNECTABLE = 2;
	
	public static int BTH_MODE_DISCOVERABLE = 3;
	
	public static boolean peerInitialized;
	
	static {
		NativeLibLoader.isAvailable();
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
	
	public native int initializationStatus() throws IOException;
	
	public native void enableNativeDebug(boolean on);
	
	public static void nativeDebugCallback(String fileName, int lineN, String message) {
		if (fileName.startsWith(".\\")) {
			fileName = fileName.substring(2);
		}
		DebugLog.debugNative(fileName + ":" + lineN, message);
	}
	
	public native int getDeviceClass(long address);
	
	public native void setDiscoverable(boolean on) throws BluetoothStateException;
	
	public native int getBluetoothRadioMode();
	
	/*
	 * perform synchronous inquiry
	 */

	public native int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener);

	/*
	 * cancel current inquiry (if any)
	 */

	public native boolean cancelInquiry();

	/*
	 * perform synchronous service discovery
	 */

	public native int[] runSearchServices(UUID[] uuidSet, long address) throws SearchServicesException;

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
	
	public native void storesockopt(int socket);

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
	
	public native int getDeviceVersion(long address);
	
	public native int getDeviceManufacturer(long address);
	
	// internal test function
	
	public static native byte[] testUUIDConversion(byte[] uuidValue);
}