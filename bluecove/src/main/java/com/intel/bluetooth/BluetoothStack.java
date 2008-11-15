/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
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

/**
 * New native stack support should ONLY implement this interface. No other classes should ideally be changed except
 * BlueCoveImpl where the instance of new class should be created.
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 * @author vlads
 * 
 */
public interface BluetoothStack {

	public static final int FEATURE_L2CAP = 1;

	public static final int FEATURE_SERVICE_ATTRIBUTES = 1 << 1;

	public static final int FEATURE_SET_DEVICE_SERVICE_CLASSES = 1 << 2;

	public static class LibraryInformation {

		public String libraryName;

		/**
		 * Class ClassLoader of which to use for loading library as resource. May be null.
		 */
		public Class stackClass;

		public LibraryInformation(String libraryName) {
			this.libraryName = libraryName;
		}

		public static LibraryInformation[] library(String libraryName) {
			return new LibraryInformation[] { new LibraryInformation(libraryName) };
		}
	}

	// ---------------------- Library initialization

	/**
	 * Used by library initialization to detect if shared library already loaded. The caller with catch
	 * UnsatisfiedLinkError and will load libraries returned by requireNativeLibraries().
	 */
	public boolean isNativeCodeLoaded();

	/**
	 * List the native libraries that need to be loaded.
	 * 
	 * @see java.lang.System#loadLibrary(java.lang.String)
	 * @return array of library names used by implementation.
	 */
	public LibraryInformation[] requireNativeLibraries();

	/**
	 * Used to verify native library version. versionMajor1 * 1000000 + versionMajor2 * 10000 + versionMinor * 100 +
	 * versionBuild
	 * 
	 * @return Version number in decimal presentation. e.g. 2030407 for version 2.3.4 build 7
	 */
	public int getLibraryVersion() throws BluetoothStateException;

	/**
	 * Used if OS Supports multiple Bluetooth stacks 0x01 winsock; 0x02 widcomm; 0x04 bluesoleil; 0x08 BlueZ; 0x10 OS X
	 * stack;
	 * 
	 * @return stackID
	 */
	public int detectBluetoothStack();

	/**
	 * 
	 * @param nativeDebugCallback
	 *            DebugLog.class
	 * @param on
	 */
	public void enableNativeDebug(Class nativeDebugCallback, boolean on);

	/**
	 * Call is made when we want to use this stack.
	 */
	public void initialize() throws BluetoothStateException;

	public void destroy();

	public String getStackID();

	/**
	 * Called from long running native code to see if thread interrupted. If yes InterruptedIOException would be thrown.
	 * 
	 * @return true if interrupted
	 */
	public boolean isCurrentThreadInterruptedCallback();

	/**
	 * @return implemented features, see FEATURE_* constants
	 */
	public int getFeatureSet();

	// ---------------------- LocalDevice

	/**
	 * Retrieves the Bluetooth address of the local device.
	 * 
	 * @see javax.bluetooth.LocalDevice#getBluetoothAddress()
	 */
	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

	/**
	 * Retrieves the name of the local device.
	 * 
	 * @see javax.bluetooth.LocalDevice#getFriendlyName()
	 */
	public String getLocalDeviceName();

	/**
	 * Retrieves the class of the local device.
	 * 
	 * @see javax.bluetooth.LocalDevice#getDeviceClass()
	 */
	public DeviceClass getLocalDeviceClass();

	/**
	 * Implementation for local device service class
	 * 
	 * @see javax.bluetooth.ServiceRecord#setDeviceServiceClasses(int) and
	 * @see javax.bluetooth.LocalDevice#updateRecord(javax.bluetooth.ServiceRecord)
	 * @param classOfDevice
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice);

	/**
	 * @see javax.bluetooth.LocalDevice#setDiscoverable(int)
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException;

	/**
	 * @see javax.bluetooth.LocalDevice#getDiscoverable()
	 */
	public int getLocalDeviceDiscoverable();

	/**
	 * @see javax.bluetooth.LocalDevice#isPowerOn()
	 */
	public boolean isLocalDevicePowerOn();

	/**
	 * @see javax.bluetooth.LocalDevice#getProperty(String)
	 */
	public String getLocalDeviceProperty(String property);

	// ---------------------- Remote Device authentication

	/**
	 * Attempts to authenticate RemoteDevice. Return <code>false</code> if the stack does not support authentication.
	 * 
	 * @see javax.bluetooth.RemoteDevice#authenticate()
	 */
	public boolean authenticateRemoteDevice(long address) throws IOException;

	/**
	 * Sends an authentication request to a remote Bluetooth device. Non JSR-82,
	 * 
	 * @param address
	 *            Remote Device address
	 * @param passkey
	 *            A Personal Identification Number (PIN) to be used for device authentication.
	 * @return <code>true</code> if authentication is successful; otherwise <code>false</code>
	 * @throws IOException
	 *             if there are error during authentication.
	 */
	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException;

	/**
	 * Removes authentication between local and remote bluetooth devices. Non JSR-82,
	 * 
	 * @param address
	 *            Remote Device address authentication.
	 * @throws IOException
	 *             if there are error during authentication.
	 */

	public void removeAuthenticationWithRemoteDevice(long address) throws IOException;

	// ---------------------- Device Inquiry

	/**
	 * called by JSR-82 code Device Inquiry
	 * 
	 * @see javax.bluetooth.DiscoveryAgent#startInquiry(int, javax.bluetooth.DiscoveryListener)
	 */
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	/**
	 * called by JSR-82 code Device Inquiry
	 * 
	 * @see javax.bluetooth.DiscoveryAgent#cancelInquiry(javax.bluetooth.DiscoveryListener)
	 */
	public boolean cancelInquiry(DiscoveryListener listener);

	/**
	 * called by implementation when device name is unknown or <code>alwaysAsk</code> is <code>true</code>
	 * 
	 * @see javax.bluetooth.RemoteDevice#getFriendlyName(boolean)
	 */
	public String getRemoteDeviceFriendlyName(long address) throws IOException;

	/**
	 * @see javax.bluetooth.DiscoveryAgent#retrieveDevices(int)
	 * @return null if not implemented
	 */
	public RemoteDevice[] retrieveDevices(int option);

	/**
	 * @see javax.bluetooth.RemoteDevice#isTrustedDevice()
	 * @return null if not implemented
	 */
	public Boolean isRemoteDeviceTrusted(long address);

	/**
	 * @see javax.bluetooth.RemoteDevice#isAuthenticated()
	 * @return null if not implemented
	 */
	public Boolean isRemoteDeviceAuthenticated(long address);

	// ---------------------- Service search

	/**
	 * called by JSR-82 code Service search
	 * 
	 * @see javax.bluetooth.DiscoveryAgent#searchServices(int[],UUID[],javax.bluetooth.RemoteDevice,
	 *      javax.bluetooth.DiscoveryListener)
	 */
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException;

	/**
	 * called by JSR-82 code Service search
	 * 
	 * @see javax.bluetooth.DiscoveryAgent#cancelServiceSearch(int)
	 */
	public boolean cancelServiceSearch(int transID);

	/**
	 * Called by ServiceRecord.populateRecord(int[] attrIDs) during Service search.
	 * 
	 * @see javax.bluetooth.ServiceRecord#populateRecord(int[])
	 */
	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException;

	// ---------------------- Client and Server RFCOMM connections

	/**
	 * Used to create handle for {@link com.intel.bluetooth.BluetoothRFCommClientConnection}
	 * 
	 * @see javax.microedition.io.Connector#open(String, int, boolean)
	 */
	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException;

	/**
	 * @param handle
	 * @param expected
	 *            Value specified when connection was open ServiceRecord.xxAUTHENTICATE_xxENCRYPT
	 * @return expected if not implemented by stack
	 * @throws IOException
	 * 
	 * @see javax.bluetooth.RemoteDevice#isAuthenticated()
	 * @see javax.bluetooth.RemoteDevice#isEncrypted()
	 */
	public int rfGetSecurityOpt(long handle, int expected) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothRFCommClientConnection
	 * @see com.intel.bluetooth.BluetoothRFCommConnection#close()
	 * @see com.intel.bluetooth.BluetoothRFCommConnection#closeConnectionHandle(long)
	 */
	public void connectionRfCloseClientConnection(long handle) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothRFCommServerConnection
	 * @see #connectionRfCloseClientConnection(long)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void connectionRfCloseServerConnection(long handle) throws IOException;

	/**
	 * Used to create handle for {@link com.intel.bluetooth.BluetoothRFCommConnectionNotifier}
	 * 
	 * @see javax.microedition.io.Connector#open(String, int, boolean)
	 */
	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException;

	/**
	 * @see javax.bluetooth.LocalDevice#updateRecord(javax.bluetooth.ServiceRecord)
	 */
	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException;

	/**
	 * Used to create handle for {@link com.intel.bluetooth.BluetoothRFCommServerConnection}
	 * 
	 * @see com.intel.bluetooth.BluetoothRFCommConnectionNotifier#acceptAndOpen()
	 * @see javax.microedition.io.StreamConnectionNotifier#acceptAndOpen()
	 */
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#close()
	 * @see javax.microedition.io.Connection#close()
	 */
	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;

	/**
	 * @see javax.bluetooth.RemoteDevice#getRemoteDevice(javax.microedition.io.Connection)
	 */
	public long getConnectionRfRemoteAddress(long handle) throws IOException;

	/**
	 * @see javax.bluetooth.RemoteDevice#encrypt(javax.microedition.io.Connection, boolean)
	 */
	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException;

	/**
	 * @see java.io.InputStream#read()
	 * @see com.intel.bluetooth.BluetoothRFCommInputStream#read()
	 */
	public int connectionRfRead(long handle) throws IOException;

	/**
	 * @see java.io.InputStream#read(byte[],int,int)
	 * @see com.intel.bluetooth.BluetoothRFCommInputStream#read(byte[],int,int)
	 */
	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	/**
	 * @see java.io.InputStream#available()
	 * @see com.intel.bluetooth.BluetoothRFCommInputStream#available()
	 */
	public int connectionRfReadAvailable(long handle) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothRFCommOutputStream#write(int)
	 */
	public void connectionRfWrite(long handle, int b) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothRFCommOutputStream#write(byte[], int, int)
	 */
	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothRFCommOutputStream#flush()
	 */
	public void connectionRfFlush(long handle) throws IOException;

	// ---------------------- Client and Server L2CAP connections

	/**
	 * Used to create handle for {@link com.intel.bluetooth.BluetoothL2CAPClientConnection}
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException;

	/**
	 * Closing {@link com.intel.bluetooth.BluetoothL2CAPClientConnection}
	 * 
	 * @see javax.microedition.io.Connection#close()
	 */
	public void l2CloseClientConnection(long handle) throws IOException;

	/**
	 * Used to create handle for {@link com.intel.bluetooth.BluetoothL2CAPConnectionNotifier}
	 * 
	 * @see javax.microedition.io.Connector#open(String, int, boolean)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException;

	/**
	 * @see javax.bluetooth.LocalDevice#updateRecord(javax.bluetooth.ServiceRecord)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException;

	/**
	 * Used to create handle for {@link com.intel.bluetooth.BluetoothL2CAPServerConnection}
	 * 
	 * @see com.intel.bluetooth.BluetoothL2CAPConnectionNotifier#acceptAndOpen()
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

	/**
	 * Closing {@link com.intel.bluetooth.BluetoothL2CAPServerConnection}
	 * 
	 * @see #l2CloseClientConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException;

	/**
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#close()
	 * @see javax.microedition.io.Connection#close()
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;

	/**
	 * @see #rfGetSecurityOpt(long, int)
	 */
	public int l2GetSecurityOpt(long handle, int expected) throws IOException;

	/**
	 * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
	 */
	public int l2GetTransmitMTU(long handle) throws IOException;

	/**
	 * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
	 */
	public int l2GetReceiveMTU(long handle) throws IOException;

	/**
	 * @see javax.bluetooth.L2CAPConnection#ready()
	 */
	public boolean l2Ready(long handle) throws IOException;

	/**
	 * @see javax.bluetooth.L2CAPConnection#receive(byte[])
	 */
	public int l2Receive(long handle, byte[] inBuf) throws IOException;

	/**
	 * @see javax.bluetooth.L2CAPConnection#send(byte[])
	 */
	public void l2Send(long handle, byte[] data) throws IOException;

	/**
	 * @see javax.bluetooth.RemoteDevice#getRemoteDevice(javax.microedition.io.Connection)
	 */
	public long l2RemoteAddress(long handle) throws IOException;

	/**
	 * @see javax.bluetooth.RemoteDevice#encrypt(javax.microedition.io.Connection, boolean)
	 */
	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException;
}
