package com.intel.bluetooth;

/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2010 Mina Shokry
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



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

/**
 *
 * @author Mina Shokry
 */
public class BluetoothStackAndroid implements BluetoothStack {

	private BluetoothAdapter localBluetoothAdapter;

	/**
	 * This implementation will turn bluetooth on if it was off, in this case,
	 * bluetooth will be turned off at shutting down stack.
	 */
	private boolean justEnabled;

	public boolean isNativeCodeLoaded() {
		return true;
	}

	public LibraryInformation[] requireNativeLibraries() {
		return null;
	}

	public int getLibraryVersion() throws BluetoothStateException {
		return BlueCoveImpl.nativeLibraryVersionExpected;
	}

	public int detectBluetoothStack() {
		return BlueCoveImpl.BLUECOVE_STACK_DETECT_ANDROID_2_X;
	}

	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void initialize() throws BluetoothStateException {
		localBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(localBluetoothAdapter == null) {
			throw new BluetoothStateException("Bluetooth isn't supported on this device");
		}
		try {
			if(!localBluetoothAdapter.isEnabled()) {
				if(localBluetoothAdapter.enable()) {
					justEnabled = true;
				}
			}
		} catch (Exception ex) {
			BluetoothStateException bluetoothStateException = new BluetoothStateException(ex.toString());
			throw bluetoothStateException;
		}
	}

	public void destroy() {
		if(justEnabled) {
			localBluetoothAdapter.disable();
		}
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_ANDROID_2_X;
	}

	public boolean isCurrentThreadInterruptedCallback() {
		return UtilsJavaSE.isCurrentThreadInterrupted();
	}

	public int getFeatureSet() {
		return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP | FEATURE_RSSI;
	}

	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		return localBluetoothAdapter.getAddress().replace(":", "");
	}

	public String getLocalDeviceName() {
		return localBluetoothAdapter.getName();
	}

	public DeviceClass getLocalDeviceClass() {
		return null;
	}

	public void setLocalDeviceServiceClasses(int classOfDevice) {
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return false;
	}

	public int getLocalDeviceDiscoverable() {
		switch (localBluetoothAdapter.getScanMode()) {
			case BluetoothAdapter.SCAN_MODE_NONE:
				return DiscoveryAgent.NOT_DISCOVERABLE;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
				return DiscoveryAgent.LIAC;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
				return DiscoveryAgent.GIAC;
			default:
				throw new RuntimeException("Unexpected scan mode returned: " + localBluetoothAdapter.getScanMode());
		}
	}

	public boolean isLocalDevicePowerOn() {
		return localBluetoothAdapter.isEnabled();
	}

	public String getLocalDeviceProperty(String property) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean authenticateRemoteDevice(long address) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		String addressString = getAddressAsString(address);
		return localBluetoothAdapter.getRemoteDevice(addressString).getName();
	}

	private String getAddressAsString(long address) {
		String addressHex = Long.toHexString(address);
		StringBuilder buffer = new StringBuilder("000000000000".substring(addressHex.length()) + addressHex);
		for(int index = 2; index < buffer.length(); index += 3) {
			buffer.insert(index, ':');
		}
		return buffer.toString();
	}

	private long getAddressAsLong(String address) {
		return Long.parseLong(address.replace(":", ""), 16);
	}

	public RemoteDevice[] retrieveDevices(int option) {
		Set<BluetoothDevice> bondedDevices = localBluetoothAdapter.getBondedDevices();
		RemoteDevice[] devices = new RemoteDevice[bondedDevices.size()];
		int index = 0;
		Iterator<BluetoothDevice> iterator = bondedDevices.iterator();
		while(iterator.hasNext()) {
			BluetoothDevice device = iterator.next();
			devices[index++] = RemoteDeviceHelper.createRemoteDevice(this,
				getAddressAsLong(device.getAddress()), device.getName(),
				device.getBondState() == BluetoothDevice.BOND_BONDED);
		}
		return devices;
	}

	public Boolean isRemoteDeviceTrusted(long address) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Boolean isRemoteDeviceAuthenticated(long address) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean cancelServiceSearch(int transID) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int connectionRfRead(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void connectionRfFlush(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void l2CloseClientConnection(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void l2CloseServerConnection(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int l2GetSecurityOpt(long handle, int expected) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int l2GetTransmitMTU(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int l2GetReceiveMTU(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean l2Ready(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int l2Receive(long handle, byte[] inBuf) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void l2Send(long handle, byte[] data, int transmitMTU) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long l2RemoteAddress(long handle) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
