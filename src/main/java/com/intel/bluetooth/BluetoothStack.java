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

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

public interface BluetoothStack {

	//---------------------- Library initialization ----------------------
	
	/**
	 * Used to verify native library version.
	 * versionMajor * 10000 + versionMinor * 100 + versionBuild  
	 * @return Version number in decimal presentation. e.g. 20304  for version 2.3.4
	 */
	public int getLibraryVersion();
	
	/**
	 * Used if OS Supports multiple Bluetooth stacks  
	 * 0x01 winsock
	 * 0x02 widcomm
	 * 0x04 bluesoleil
	 * 0x08 BlueZ
	 * 0x10 OS X stack?
	 * 
	 * @return stackID
	 */
	public int detectBluetoothStack();
	
	/**
	 * 
	 * @param nativeDebugCallback  DebugLog.class
	 * @param on
	 */
	public void enableNativeDebug(Class nativeDebugCallback, boolean on);
	
	/**
	 * Call is made when we want to use this stack.
	 */
	public void initialize();
	
	public void destroy();
	
	public String getStackID();
	
	//---------------------- LocalDevice ----------------------
	
	/**
	 * Retrieves the Bluetooth address of the local device.
	 */
	public String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

	/**
	 *  Retrieves the name of the local device. 
	 */
	public String getLocalDeviceName();

	public String getRemoteDeviceFriendlyName(long address) throws IOException;
	
	public DeviceClass getLocalDeviceClass();
	
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException;

	public int getLocalDeviceDiscoverable();
	
	public boolean isLocalDevicePowerOn();
	
	public String getLocalDeviceProperty(String property);
	
	//---------------------- Device Inquiry ----------------------
	
	/**
	 * called by JSR-82 code Device Inquiry
	 */
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException;
	
	/**
	 * called by JSR-82 code Device Inquiry
	 */
	public boolean cancelInquiry(DiscoveryListener listener);
	
	/**
	 * Common synchronous method called by DeviceInquiryThread. 
	 * Should throw BluetoothStateException only if it can't start Inquiry
	 */
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired);
	
	//---------------------- Service search ---------------------- 
	
	/**
	 * called by JSR-82 code Service search
	 */
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException;

	/**
	 * called by JSR-82 code Service search
	 */
	public boolean cancelServiceSearch(int transID);
	
	/**
	 * Common synchronous method called by SearchServicesThread. 
	 * Should throw BluetoothStateException only if it can't start Search
	 */
	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException;
	
	/**
	 * Called by ServiceRecord.populateRecord(int[] attrIDs) during Service search
	 */
	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException;
	
	//---------------------- Client and Server RFCOMM connections ----------------------
	
	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException;
	
	/**
	 * @param handle
	 * @param expected  Value specified when connection was open ServiceRecord.xxAUTHENTICATE_xxENCRYPT
	 * @return expected if not implemented by stack
	 * @throws IOException
	 */
	public int getSecurityOpt(long handle, int expected) throws IOException;
	
	public void connectionRfCloseClientConnection(long handle) throws IOException;
	
	public void connectionRfCloseServerConnection(long handle) throws IOException;
	
	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException;
	
	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException;
	
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;
	
	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;
	
	public long getConnectionRfRemoteAddress(long handle) throws IOException;

	public int connectionRfRead(long handle) throws IOException;
	
	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;
	
	public int connectionRfReadAvailable(long handle) throws IOException;
	
	public void connectionRfWrite(long handle, int b) throws IOException;
	
	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;
	
	public void connectionRfFlush(long handle) throws IOException;

	//	---------------------- Client and Server L2CAP connections ----------------------
	
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException;
	
	public void l2CloseClientConnection(long handle) throws IOException;
	
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException;
	
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException;
	
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;
	
	public void l2CloseServerConnection(long handle) throws IOException;
	
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;
	
	public int l2GetTransmitMTU(long handle) throws IOException;
	
	public int l2GetReceiveMTU(long handle) throws IOException;
	
	public boolean l2Ready(long handle) throws IOException;
	
	public int l2Receive(long handle, byte[] inBuf) throws IOException;
	
	public void l2Send(long handle, byte[] data) throws IOException;
	
	public long l2RemoteAddress(long handle) throws IOException;
}
