/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Hakan Lager
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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

public class BluetoothStackBlueZ implements BluetoothStack {

	//Used mainly in Unit Tests
	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_BLUEZ);
	}

	BluetoothStackBlueZ() {

	}

	//---------------------- Library initialization ----------------------
	
	public String getStackID() {
		return BlueCoveImpl.STACK_BLUEZ;
	}

	public int getLibraryVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int detectBluetoothStack() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void initialize() {
		// TODO Auto-generated method stub
	}

	public void destroy() {
		// TODO Auto-generated method stub

	}

	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
		// TODO Auto-generated method stub

	}

	//---------------------- LocalDevice ----------------------
	
	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public DeviceClass getLocalDeviceClass() {
		// TODO Auto-generated method stub
		return null;
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


	//---------------------- Device Inquiry ----------------------

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName) {
		// TODO Auto-generated method stub
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		// TODO Auto-generated method stub
		return false;
	}

	//---------------------- Service search ---------------------- 

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
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

	//---------------------- Client RFCOMM connections ----------------------

	public long connectionRfOpenClientConnection(long address, int channel, boolean authenticate, boolean encrypt)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	//---------------------- Server RFCOMM connections ----------------------

	public long rfServerOpen(UUID uuid, boolean authenticate, boolean encrypt, String name,
			ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub

	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord)
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

	//---------------------- Shared Client and Server RFCOMM connections ----------------------

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

}
