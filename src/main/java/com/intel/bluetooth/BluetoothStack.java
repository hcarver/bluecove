/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
import javax.microedition.io.Connection;

/**
 * New native stack support should ONLY implement this interface. No other
 * classes should ideally be changed except BlueCoveImpl where the instance of
 * new class should be created.
 * 
 * @author vlads
 * 
 */
public interface BluetoothStack {

	public static final int FEATURE_L2CAP = 1;

	public static final int FEATURE_SERVICE_ATTRIBUTES = 1 << 1;

	public static final int FEATURE_SET_DEVICE_SERVICE_CLASSES = 1 << 2;

	// ---------------------- Library initialization

	/**
	 * Used to verify native library version. versionMajor1 * 1000000 +
	 * versionMajor2 * 10000 + versionMinor * 100 + versionBuild
	 * 
	 * @return Version number in decimal presentation. e.g. 2030407 for version
	 *         2.3.4 build 7
	 */
	public int getLibraryVersion() throws BluetoothStateException;

	/**
	 * Used if OS Supports multiple Bluetooth stacks 0x01 winsock; 0x02 widcomm;
	 * 0x04 bluesoleil; 0x08 BlueZ; 0x10 OS X stack;
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
	 * Called from long running native code to see if thread interrupted. If yes
	 * InterruptedIOException would be thrown.
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

	// ---------------------- Device Inquiry

	/**
	 * called by JSR-82 code Device Inquiry
	 * 
	 * @see javax.bluetooth.DiscoveryAgent#startInquiry(int,
	 *      javax.bluetooth.DiscoveryListener)
	 */
	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	/**
	 * called by JSR-82 code Device Inquiry
	 * 
	 * @see javax.bluetooth.DiscoveryAgent#cancelInquiry(javax.bluetooth.DiscoveryListener)
	 */
	public boolean cancelInquiry(DiscoveryListener listener);

	/**
	 * called by implementation when device name is unknown or
	 * <code>alwaysAsk</code> is <code>true</code>
	 * 
	 * @see javax.bluetooth.RemoteDevice#getFriendlyName(boolean)
	 */
	public String getRemoteDeviceFriendlyName(long address) throws IOException;

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
	 * Called by ServiceRecord.populateRecord(int[] attrIDs) during Service
	 * search.
	 * 
	 * @see javax.bluetooth.ServiceRecord#populateRecord(int[])
	 */
	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException;

	// ---------------------- Client and Server RFCOMM connections

	/**
	 * Used to create handle for {@link BluetoothRFCommClientConnection}
	 * 
	 * @see javax.microedition.io.Connector#open(String, int, boolean)
	 */
	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException;

	/**
	 * @param handle
	 * @param expected
	 *            Value specified when connection was open
	 *            ServiceRecord.xxAUTHENTICATE_xxENCRYPT
	 * @return expected if not implemented by stack
	 * @throws IOException
	 * 
	 * @see javax.bluetooth.RemoteDevice#isAuthenticated()
	 * @see javax.bluetooth.RemoteDevice#isEncrypted()
	 */
	public int rfGetSecurityOpt(long handle, int expected) throws IOException;

	/**
	 * @see BluetoothRFCommClientConnection
	 * @see BluetoothRFCommConnection#close()
	 * @see BluetoothRFCommConnection#closeConnectionHandle(long)
	 */
	public void connectionRfCloseClientConnection(long handle) throws IOException;

	/**
	 * @see BluetoothRFCommServerConnection
	 * @see #connectionRfCloseClientConnection(long)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void connectionRfCloseServerConnection(long handle) throws IOException;

	/**
	 * Used to create handle for {@link BluetoothRFCommConnectionNotifier}
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
	 * Used to create handle for {@link BluetoothRFCommServerConnection}
	 * 
	 * @see BluetoothRFCommConnectionNotifier#acceptAndOpen()
	 * @see javax.microedition.io.StreamConnectionNotifier#acceptAndOpen()
	 */
	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

	/**
	 * @see BluetoothConnectionNotifierBase#close()
	 * @see javax.microedition.io.Connection#close()
	 */
	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException;

	/**
	 * @see javax.bluetooth.RemoteDevice#getRemoteDevice(Connection)
	 */
	public long getConnectionRfRemoteAddress(long handle) throws IOException;

	/**
	 * @see java.io.InputStream#read()
	 * @see BluetoothRFCommInputStream#read()
	 */
	public int connectionRfRead(long handle) throws IOException;

	/**
	 * @see java.io.InputStream#read(byte[],int,int)
	 * @see BluetoothRFCommInputStream#read(byte[],int,int)
	 */
	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	/**
	 * @see java.io.InputStream#available()
	 * @see BluetoothRFCommInputStream#available()
	 */
	public int connectionRfReadAvailable(long handle) throws IOException;

	/**
	 * @see BluetoothRFCommOutputStream#write(int)
	 */
	public void connectionRfWrite(long handle, int b) throws IOException;

	/**
	 * @see BluetoothRFCommOutputStream#write(byte[], int, int)
	 */
	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	/**
	 * @see BluetoothRFCommOutputStream#flush()
	 */
	public void connectionRfFlush(long handle) throws IOException;

	// ---------------------- Client and Server L2CAP connections

	/**
	 * Used to create handle for {@link BluetoothL2CAPClientConnection}
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException;

	/**
	 * Closing {@link  BluetoothL2CAPClientConnection}
	 * 
	 * @see javax.microedition.io.Connection#close()
	 */
	public void l2CloseClientConnection(long handle) throws IOException;

	/**
	 * Used to create handle for {@link BluetoothL2CAPConnectionNotifier}
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
	 * Used to create handle for {@link BluetoothL2CAPServerConnection}
	 * 
	 * @see BluetoothL2CAPConnectionNotifier#acceptAndOpen()
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

	/**
	 * Closing {@link  BluetoothL2CAPServerConnection}
	 * 
	 * @see #l2CloseClientConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException;

	/**
	 * @see BluetoothConnectionNotifierBase#close()
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
	 * @see javax.bluetooth.RemoteDevice#getRemoteDevice(Connection)
	 */
	public long l2RemoteAddress(long handle) throws IOException;
}
