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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothStackToshiba implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

	private boolean initialized = false;

	private Vector deviceDiscoveryListeners = new Vector/* <DiscoveryListener> */();
	
	private Hashtable deviceDiscoveryListenerFoundDevices = new Hashtable();

	private Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();

	private final static int ATTR_RETRIEVABLE_MAX = 0xFFFF;

	private final static int RECEIVE_MTU_MAX = 1024;

	// FIXME
	private String getBTWVersionInfo()
	{
		return "";
	}

	// FIXME
	private int getDeviceVersion()
	{
		return 0;
	}

	// FIXME
	private int getDeviceManufacturer()
	{
		return 0;
	}

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

	public RemoteDevice[] retrieveDevices(int option) {
		return null;
	}

	public Boolean isRemoteDeviceTrusted(long address) {
		return null;
	}

	public Boolean isRemoteDeviceAuthenticated(long address) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice (long)
	 */
	public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
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
		return true;
	}

	public String getLocalDeviceProperty(String property) {
		// Copied directly from WIDCOMM: probably needs to be changed
		if ("bluetooth.connected.devices.max".equals(property)) {
			return "7";
		}
		if ("bluetooth.sd.trans.max".equals(property)) {
			return "1";
		}
		if ("bluetooth.connected.inquiry.scan".equals(property)) {
			return BlueCoveImpl.TRUE;
		}
		if ("bluetooth.connected.page.scan".equals(property)) {
			return BlueCoveImpl.TRUE;
		}
		if ("bluetooth.connected.inquiry".equals(property)) {
			return BlueCoveImpl.TRUE;
		}
		if ("bluetooth.connected.page".equals(property)) {
			return BlueCoveImpl.TRUE;
		}

		if ("bluetooth.sd.attr.retrievable.max".equals(property)) {
			return String.valueOf(ATTR_RETRIEVABLE_MAX);
		}
		if ("bluetooth.master.switch".equals(property)) {
			return BlueCoveImpl.FALSE;
		}
		if ("bluetooth.l2cap.receiveMTU.max".equals(property)) {
			return String.valueOf(RECEIVE_MTU_MAX);
		}

		if ("bluecove.radio.version".equals(property)) {
			return String.valueOf(getDeviceVersion());
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(getDeviceManufacturer());
		}
		if ("bluecove.stack.version".equals(property)) {
			return getBTWVersionInfo();
		}
		// Some Hack and testing functions, not documented
		if (property.startsWith("bluecove.nativeFunction:")) {
			String functionDescr = property.substring(property.indexOf(':') + 1, property.length());
			int paramIdx = functionDescr.indexOf(':');
			if (paramIdx == -1) {
				throw new RuntimeException("Invalid native function " + functionDescr + "; arguments expected");
			}
			String function = functionDescr.substring(0, paramIdx);
			long address = RemoteDeviceHelper.getAddress(functionDescr.substring(function.length() + 1, functionDescr
					.length()));
			if ("getRemoteDeviceVersionInfo".equals(function)) {
				return getRemoteDeviceVersionInfo(address);
			} else if ("cancelSniffMode".equals(function)) {
				return String.valueOf(cancelSniffMode(address));
			} else if ("setSniffMode".equals(function)) {
				return String.valueOf(setSniffMode(address));
			} else if ("getRemoteDeviceRSSI".equals(function)) {
				return String.valueOf(getRemoteDeviceRSSI(address));
			} else if ("getRemoteDeviceLinkMode".equals(function)) {
				if (isRemoteDeviceConnected(address)) {
					return getRemoteDeviceLinkMode(address);
				} else {
					return "disconnected";
				}
			}
		}
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

	// ---------------------- Remote Device authentication

	public boolean authenticateRemoteDevice(long address) throws IOException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#authenticateRemoteDevice(long,
	 *      java.lang.String)
	 */
	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
		return false;
	}

	// --- Some testing functions accessible by LocalDevice.getProperty

	// FIXME ALL
	
	public boolean isRemoteDeviceConnected(long address)
	{
		return true;
	}

	public String getRemoteDeviceLinkMode(long address)
	{
		return "";
	}

	public String getRemoteDeviceVersionInfo(long address)
	{
		return "";
	}

	public boolean setSniffMode(long address)
	{
		return false;
	}

	public boolean cancelSniffMode(long address)
	{
		return false;
	}

	public int getRemoteDeviceRSSI(long address)
	{
		return 0;
	}


	// ---------------------- Device Inquiry

	private native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode,
			DiscoveryListener listener) throws BluetoothStateException;

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		deviceDiscoveryListeners.addElement(listener);
		if (BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_INQUIRY_REPORT_ASAP, false)) {
			deviceDiscoveryListenerFoundDevices.put(listener, new Hashtable());
		}
		deviceDiscoveryListenerReportedDevices.put(listener, new Vector());
		return DeviceInquiryThread.startInquiry(this, this, accessCode, listener);
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			int discType = runDeviceInquiryImpl(startedNotify, accessCode, listener);
			if (discType == DiscoveryListener.INQUIRY_COMPLETED) {
				// Report found devices if any not reported
				Hashtable previouslyFound = (Hashtable) deviceDiscoveryListenerFoundDevices.get(listener);
				if (previouslyFound != null) {
					Vector reported = (Vector) deviceDiscoveryListenerReportedDevices.get(listener);
					for (Enumeration en = previouslyFound.keys(); en.hasMoreElements();) {
						RemoteDevice remoteDevice = (RemoteDevice) en.nextElement();
						if (reported.contains(remoteDevice)) {
							continue;
						}
						reported.addElement(remoteDevice);
						Integer deviceClassInt = (Integer) previouslyFound.get(remoteDevice);
						DeviceClass deviceClass = new DeviceClass(deviceClassInt.intValue());
						listener.deviceDiscovered(remoteDevice, deviceClass);
						// If cancelInquiry has been called
						if (!deviceDiscoveryListeners.contains(listener)) {
							return DiscoveryListener.INQUIRY_TERMINATED;
						}
					}
				}
			}
			return discType;
		} finally {
			deviceDiscoveryListeners.removeElement(listener);
			deviceDiscoveryListenerFoundDevices.remove(listener);
			deviceDiscoveryListenerReportedDevices.remove(listener);
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
		// Copied directly from WIDCOMM driver
		DebugLog.debug("deviceDiscoveredCallback deviceName", deviceName);
		if (!deviceDiscoveryListeners.contains(listener)) {
			return;
		}
		// Update name if name retrieved
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, deviceAddr, deviceName, paired);
		Vector reported = (Vector) deviceDiscoveryListenerReportedDevices.get(listener);
		if (reported == null || (reported.contains(remoteDevice))) {
			return;
		}
		// See -Dbluecove.inquiry.report_asap=false
		Hashtable previouslyFound = (Hashtable) deviceDiscoveryListenerFoundDevices.get(listener);
		if (previouslyFound != null) {
			Integer deviceClassInt = (Integer) previouslyFound.get(remoteDevice);
			if (deviceClassInt == null) {
				previouslyFound.put(remoteDevice, new Integer(deviceClass));
			} else if (deviceClass != 0) {
				previouslyFound.put(remoteDevice, new Integer(deviceClass));
			}
		} else {
			DeviceClass cod = new DeviceClass(deviceClass);
			reported.addElement(remoteDevice);
			DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
			DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
			listener.deviceDiscovered(remoteDevice, cod);
		}
	}

	private native boolean deviceInquiryCancelImpl();

	public boolean cancelInquiry(DiscoveryListener listener) {
		// no further deviceDiscovered() events will occur for this inquiry
		if (!deviceDiscoveryListeners.removeElement(listener)) {
			return false;
		}
		return deviceInquiryCancelImpl();
	}

	/**
	 * get device name while discovery running. Device may not report its name first time while discovering.
	 * 
	 * @param address
	 * @return name
	 */
	private native String getRemoteDeviceFriendlyNameImpl(long address);

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return getRemoteDeviceFriendlyNameImpl(address);
	}

	// ---------------------- Service search

	private native short connectSDPImpl(long address);

	private native void disconnectSDPImpl(short cid);

	private native long[] searchServicesImpl(SearchServicesThread startedNotify, short cid, byte[][] uuidSet);

	private native byte[] populateWorkerImpl(short cid, long handle, int[] attrSet);

	private boolean setAttributes(ServiceRecordImpl serviceRecord, int[] attrIDs, byte[] bytes) {
		boolean anyRetrived = false;

		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		// Need to rename this class
		BluetoothStackWIDCOMMSDPInputStream btis = null;
		try {
			btis = new BluetoothStackWIDCOMMSDPInputStream(bais);
		}
		catch (Exception e) {
		}

		for (int i = 0; i < attrIDs.length; i++) {
			int id = attrIDs[i];
			try {
				if (BluetoothStackWIDCOMMSDPInputStream.debug) {
					DebugLog.debug("decode attribute " + id + " Ox" + Integer.toHexString(id));
				}
				DataElement element = btis.readElement();

				// Do special case conversion for only one element in the
				// list.
				if (id == BluetoothConsts.ProtocolDescriptorList) {
					Enumeration protocolsSeqEnum = (Enumeration) element.getValue();
					if (protocolsSeqEnum.hasMoreElements()) {
						DataElement protocolElement = (DataElement) protocolsSeqEnum.nextElement();
						if (protocolElement.getDataType() != DataElement.DATSEQ) {
							DataElement newMainSeq = new DataElement(DataElement.DATSEQ);
							newMainSeq.addElement(element);
							element = newMainSeq;
						}
					}
				}

				serviceRecord.populateAttributeValue(id, element);
				anyRetrived = true;
			} catch (Throwable e) {
				if (BluetoothStackWIDCOMMSDPInputStream.debug) {
					DebugLog.error("error populate attribute " + id + " Ox" + Integer.toHexString(id), e);
				}
			}
		}
		return anyRetrived;
	}


	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		short cid;
		try {
			cid = connectSDPImpl(RemoteDeviceHelper.getAddress(device.getBluetoothAddress()));
		}
		catch (Exception e) {
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		}

		byte[][] uuidBytes = new byte[uuidSet.length][16];
		for (int i = 0; i < uuidSet.length; i++) {
			uuidBytes[i] = new byte[16];
			String full = uuidSet[i].toString();
			for (int j = 0; j < 16; j++) {
				String sub = full.substring(j*2, j*2+2).toUpperCase();
				uuidBytes[i][j] = (byte)Integer.parseInt(sub, 16);
			}
		}

		long[] handles;

		try {
			handles = searchServicesImpl(startedNotify, cid, uuidBytes);
		}
		catch (Exception e) {
			disconnectSDPImpl(cid);
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}

		if (handles.length <= 0) {
			disconnectSDPImpl(cid);
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}

		ServiceRecordImpl[] records = new ServiceRecordImpl[handles.length];

		for (int i = 0; i < handles.length; i++) {
			records[i] = new ServiceRecordImpl(this, device, handles[i]);
			byte[] bytes;
			try {
				bytes = populateWorkerImpl(cid, handles[i], attrSet);
			}
			catch (Exception e) {
				disconnectSDPImpl(cid);
				return DiscoveryListener.SERVICE_SEARCH_ERROR;
			}
			if (bytes != null) {
				setAttributes(records[i], attrSet, bytes);
			}
		}

		listener.servicesDiscovered(startedNotify.getTransID(), records);

		disconnectSDPImpl(cid);

		return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
	}

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, this, attrSet, uuidSet, device, listener);
	}

	public boolean cancelServiceSearch(int transID) {
		// No service search cancel on Toshiba
		return false;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		if (attrIDs.length > 0xFFFF) {
			throw new IllegalArgumentException();
		}

		short cid;
		try {
			cid = connectSDPImpl(RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice().getBluetoothAddress()));
		}
		catch (Exception e) {
			return false;
		}

		byte[] bytes;

		try {
			bytes = populateWorkerImpl(cid, serviceRecord.getHandle(), attrIDs);
		}
		catch (Exception e) {
			disconnectSDPImpl(cid);
			return false;
		}

		if (bytes == null) {
			return false;
		}

		boolean ret;

		ret = setAttributes(serviceRecord, attrIDs, bytes);

		disconnectSDPImpl(cid);

		return ret;
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
	 *      int, int)
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
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams,
	 *      int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long,
	 *      com.intel.bluetooth.ServiceRecordImpl, boolean)
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
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long,
	 *      com.intel.bluetooth.ServiceRecordImpl)
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
