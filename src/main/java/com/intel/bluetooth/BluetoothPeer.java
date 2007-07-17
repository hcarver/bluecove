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
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

abstract class BluetoothPeer implements BluetoothStack {

	public static int BTH_MODE_POWER_OFF = 1;
	
	public static int BTH_MODE_CONNECTABLE = 2;
	
	public static int BTH_MODE_DISCOVERABLE = 3;
	
	public static boolean peerInitialized;
	
	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_MS);
	}

	/**
	 * This is implementation specific class, only BlueCoveImpl can create this class
	 *
	 */
	BluetoothPeer() {
	}
	
	public native int getLibraryVersion();
	
	public native int detectBluetoothStack();
	
	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);
	
	static native int initializationStatus() throws IOException;
	
	native void uninitialize();
	
	public native int getDeviceClass(long address);
	
	public native void setDiscoverable(boolean on) throws BluetoothStateException;
	
	public native int getBluetoothRadioMode();
	
	/*
	 * perform synchronous inquiry
	 */
	public native int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired) {
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(deviceAddr, deviceName, paired);
		DeviceClass cod = new DeviceClass(deviceClass);
		DebugLog.debug("deviceDiscoveredCallback addtress", remoteDevice.getBluetoothAddress());
		DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
		listener.deviceDiscovered(remoteDevice, cod);
	}
	
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
	public native long registerService(byte[] record) throws ServiceRegistrationException;

	/*
	 * unregister service
	 */
	public native void unregisterService(long handle) throws ServiceRegistrationException;

	/*
	 * socket operations
	 */
	public native long socket(boolean authenticate, boolean encrypt) throws IOException;

	public native long getsockaddress(long socket) throws IOException;
	
	public native void storesockopt(long socket);

	public native int getsockchannel(long socket) throws IOException;

	public native void connect(long socket, long address, int channel) throws IOException;

	//public native int getSecurityOptImpl(long handle) throws IOException;
	public int getSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}
	
	public native void bind(long socket) throws IOException;
	
	public native void listen(long socket) throws IOException;

	public native long accept(long socket) throws IOException;

	public native int recvAvailable(long socket) throws IOException;
	
	public native int recv(long socket) throws IOException;

	public native int recv(long socket, byte[] b, int off, int len) throws IOException;

	public native void send(long socket, int b) throws IOException;

	public native void send(long socket, byte[] b, int off, int len) throws IOException;

	public native void close(long socket) throws IOException;

	public native String getpeername(long address) throws IOException;

	public native long getpeeraddress(long socket) throws IOException;

	public native String getradioname(long address);
	
	public native int getDeviceVersion(long address);
	
	public native int getDeviceManufacturer(long address);
	
	// internal test function
	public static native byte[] testUUIDConversion(byte[] uuidValue);

	public static native long testReceiveBufferCreate(int size);

	public static native void testReceiveBufferClose(long bufferHandler);

	public static native int testReceiveBufferWrite(long bufferHandler, byte[] send);

	public static native int testReceiveBufferRead(long bufferHandler, byte[] rcv);
	
	public static native int testReceiveBufferRead(long bufferHandler);
	
	public static native int testReceiveBufferAvailable(long bufferHandler);
	
	public static native boolean testReceiveBufferIsOverflown(long bufferHandler);
	
	public static native boolean testReceiveBufferIsCorrupted(long bufferHandler);
	
	public static native void testThrowException(int type) throws Exception;
	
	public static native void testDebug(String message);
}