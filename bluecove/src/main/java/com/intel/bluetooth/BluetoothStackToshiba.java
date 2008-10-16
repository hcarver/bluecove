/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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

class BluetoothStackToshiba implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

	private boolean initialized = false;

	BluetoothStackToshiba() {

	}

	// ---------------------- Library initialization

	public String getStackID() {
		return BlueCoveImpl.STACK_TOSHIBA;
	}

	public String toString() {
		return getStackID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
	 */
	public int getFeatureSet() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isNativeCodeLoaded()
	 */
	public native boolean isNativeCodeLoaded();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#requireNativeLibraries()
	 */
	public LibraryInformation[] requireNativeLibraries() {
		return LibraryInformation.library(BlueCoveImpl.NATIVE_LIB_TOSHIBA);
	}

	public native int getLibraryVersion();

	public native int detectBluetoothStack();

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	private native boolean initializeImpl() throws BluetoothStateException;

	private native void destroyImpl();

	public void initialize() throws BluetoothStateException {
		if (!initializeImpl()) {
			throw new BluetoothStateException("TOSHIBA BluetoothStack not found");
		}
		initialized = true;
	}

	public void destroy() {
		if (initialized) {
			destroyImpl();
			initialized = false;
			DebugLog.debug("TOSHIBA destroyed");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return UtilsJavaSE.isCurrentThreadInterrupted();
	}

	// ---------------------- LocalDevice

	public native String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

	public DeviceClass getLocalDeviceClass() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
	 */
	public void setLocalDeviceServiceClasses(int classOfDevice) {
		// TODO Auto-generated method stub
	}

	public String getLocalDeviceName() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isLocalDevicePowerOn() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getLocalDeviceProperty(String property) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getLocalDeviceDiscoverable() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		// TODO Auto-generated method stub
		return false;
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	// ---------------------- Remote Device authentication

	public boolean authenticateRemoteDevice(long address) throws IOException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#authenticateRemoteDevice(long, java.lang.String)
	 */
	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice (long)
	 */
	public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	// ---------------------- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return DeviceInquiryThread.startInquiry(this, this, accessCode, listener);
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
		// TODO Auto-generated method stub
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		// TODO Auto-generated method stub
		return false;
	}

	// ---------------------- Service search

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, this, attrSet, uuidSet, device, listener);
	}

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean cancelServiceSearch(int transID) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	// ---------------------- Client RFCOMM connections

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		return false;
	}

	// ---------------------- Server RFCOMM connections

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub

	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		// TODO Auto-generated method stub

	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub
	}

	// ---------------------- Shared Client and Server RFCOMM connections

	public void connectionRfFlush(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	public int connectionRfRead(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		// TODO Auto-generated method stub

	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	// ---------------------- Client and Server L2CAP connections

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams,
	 * int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public void l2CloseClientConnection(long handle) throws IOException {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams, int,
	 * int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl,
	 * boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub

	}

	public int l2GetSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public boolean l2Ready(long handle) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public int l2Receive(long handle, byte[] inBuf) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public void l2Send(long handle, byte[] data) throws IOException {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public int l2GetReceiveMTU(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public int l2GetTransmitMTU(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public long l2RemoteAddress(long handle) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
	 */
	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

}
