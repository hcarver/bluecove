/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Eric Wagner
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

class BluetoothStackOSX implements BluetoothStack {

	public static final boolean debug = true;

	// TODO what is the real number for Attributes retrivable ?
	private final static int ATTR_RETRIEVABLE_MAX = 256;

	private Vector deviceDiscoveryListeners = new Vector/* <DiscoveryListener> */();

	private Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();

	// Used mainly in Unit Tests
	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_OSX);
	}

	BluetoothStackOSX() {

	}

	// ---------------------- Library initialization ----------------------

	public String getStackID() {
		return BlueCoveImpl.STACK_OSX;
	}

	public native int getLibraryVersion();

	public native int detectBluetoothStack();

	private native boolean initializeImpl();

	public void initialize() {
		if (!initializeImpl()) {
			throw new RuntimeException("OS X BluetoothStack not found");
		}
	}

	public void destroy() {
		// TODO Auto-generated method stub
	}

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}

	// ---------------------- LocalDevice ----------------------

	public native String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

	public native String getLocalDeviceName();

	private native int getDeviceClassImpl();

	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(getDeviceClassImpl());
	}

	public native boolean isLocalDevicePowerOn();

	private native boolean isLocalDeviceFeatureSwitchRoles();

	private native boolean isLocalDeviceFeatureParkMode();

	private native int getLocalDeviceL2CAPMTUMaximum();

	private native String getLocalDeviceSoftwareVersionInfo();

	private native int getLocalDeviceManufacturer();

	private native String getLocalDeviceVersion();

	public String getLocalDeviceProperty(String property) {
		final String TRUE = "true";
		final String FALSE = "false";
		if ("bluetooth.connected.devices.max".equals(property)) {
			return isLocalDeviceFeatureParkMode() ? "255" : "7";
		}
		if ("bluetooth.sd.trans.max".equals(property)) {
			return "1";
		}
		if ("bluetooth.connected.inquiry.scan".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.page.scan".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.inquiry".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.page".equals(property)) {
			return TRUE;
		}

		if ("bluetooth.sd.attr.retrievable.max".equals(property)) {
			return String.valueOf(ATTR_RETRIEVABLE_MAX);
		}
		if ("bluetooth.master.switch".equals(property)) {
			return isLocalDeviceFeatureSwitchRoles() ? TRUE : FALSE;
		}
		if ("bluetooth.l2cap.receiveMTU.max".equals(property)) {
			return String.valueOf(getLocalDeviceL2CAPMTUMaximum());
		}

		if ("bluecove.radio.version".equals(property)) {
			return getLocalDeviceVersion();
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(getLocalDeviceManufacturer());
		}
		if ("bluecove.stack.version".equals(property)) {
			return getLocalDeviceSoftwareVersionInfo();
		}

		return null;
	}

	private native boolean getLocalDeviceDiscoverableImpl();

	public int getLocalDeviceDiscoverable() {
		if (getLocalDeviceDiscoverableImpl()) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	/**
	 * There are no functions to set OS X stack Discoverable status.
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		if (getLocalDeviceDiscoverable() == mode) {
			return true;
		}
		return false;
	}

	// ---------------------- Device Inquiry ----------------------

	public native String getRemoteDeviceFriendlyName(long address) throws IOException;

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		deviceDiscoveryListeners.addElement(listener);
		deviceDiscoveryListenerReportedDevices.put(listener, new Vector());
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException;

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			return runDeviceInquiryImpl(startedNotify, accessCode, listener);
		} finally {
			deviceDiscoveryListeners.removeElement(listener);
			deviceDiscoveryListenerReportedDevices.remove(listener);
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
		if (!deviceDiscoveryListeners.contains(listener)) {
			return;
		}
		// Update name if name retrieved
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, deviceAddr, deviceName, paired);
		Vector reported = (Vector) deviceDiscoveryListenerReportedDevices.get(listener);
		if (reported == null || (reported.contains(remoteDevice))) {
			return;
		}
		reported.addElement(remoteDevice);
		DeviceClass cod = new DeviceClass(deviceClass);
		DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
		DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
		listener.deviceDiscovered(remoteDevice, cod);
	}

	private native boolean deviceInquiryCancelImpl();

	public boolean cancelInquiry(DiscoveryListener listener) {
		// no further deviceDiscovered() events will occur for this inquiry
		if (!deviceDiscoveryListeners.removeElement(listener)) {
			return false;
		}
		return deviceInquiryCancelImpl();
	}

	// ---------------------- Service search ----------------------

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	/**
	 * 
	 * @param address
	 *            Bluetooth device address
	 * @return number of service records found on device
	 */
	private native int runSearchServicesImpl(long address, int transID) throws BluetoothStateException,
			SearchServicesException;

	private native void cancelServiceSearchImpl(int transID);

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			sst.setTerminated();
			synchronized (this) {
				cancelServiceSearchImpl(transID);
			}
			return true;
		} else {
			return false;
		}
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		// Retrieve all Records, Filter here in Java
		startedNotify.searchServicesStartedCallback();
		int recordsSize;
		try {
			recordsSize = runSearchServicesImpl(RemoteDeviceHelper.getAddress(device), startedNotify.getTransID());
		} catch (SearchServicesDeviceNotReachableException e) {
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		} catch (SearchServicesTerminatedException e) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		} catch (SearchServicesException e) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
		if (startedNotify.isTerminated()) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		}
		if (recordsSize == 0) {
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
		Vector records = new Vector();
		int[] uuidFilerAttrIDs = new int[] { BluetoothConsts.ServiceClassIDList, BluetoothConsts.ProtocolDescriptorList };
		int[] requiredAttrIDs = new int[] { BluetoothConsts.ServiceRecordHandle, BluetoothConsts.ServiceRecordState,
				BluetoothConsts.ServiceID };
		nextRecord: for (int i = 0; i < recordsSize; i++) {
			ServiceRecordImpl sr = new ServiceRecordImpl(this, device, i);
			try {
				sr.populateRecord(uuidFilerAttrIDs);
				filterUUID: for (int u = 0; u < uuidSet.length; u++) {
					if ((sr.hasServiceClassUUID(uuidSet[u]) || sr.hasProtocolClassUUID(uuidSet[u]))) {
						break filterUUID;
					}
					if (debug) {
						DebugLog.debug("filtered ServiceRecord (" + i + ")", sr);
					}
					continue nextRecord;
				}
				if (debug) {
					DebugLog.debug("accepted ServiceRecord (" + i + ")", sr);
				}
				records.addElement(sr);
				sr.populateRecord(requiredAttrIDs);
				if (attrSet != null) {
					sr.populateRecord(attrSet);
				}
				DebugLog.debug("ServiceRecord (" + i + ")", sr);
			} catch (Exception e) {
				DebugLog.debug("populateRecord error", e);
			}

			if (startedNotify.isTerminated()) {
				DebugLog.debug("SERVICE_SEARCH_TERMINATED");
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			}
		}
		if (records.size() != 0) {
			DebugLog.debug("SERVICE_SEARCH_COMPLETED");
			ServiceRecord[] fileteredRecords = (ServiceRecord[]) Utils.vector2toArray(records,
					new ServiceRecord[records.size()]);
			listener.servicesDiscovered(startedNotify.getTransID(), fileteredRecords);
			return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
		} else {
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}

	private native DataElement getServiceAttributeImpl(long address, long serviceRecordIndex, int attrID);

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		if (attrIDs.length > ATTR_RETRIEVABLE_MAX) {
			throw new IllegalArgumentException();
		}
		boolean anyRetrived = false;
		long address = RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
		for (int i = 0; i < attrIDs.length; i++) {
			int id = attrIDs[i];
			try {
				DataElement element = getServiceAttributeImpl(address, serviceRecord.getHandle(), id);
				if (element != null) {
					serviceRecord.populateAttributeValue(id, element);
					anyRetrived = true;
					if (debug) {
						DebugLog.debug("data for attribute " + id + " Ox" + Integer.toHexString(id) + " " + element);
					}
				} else {
					if (debug) {
						DebugLog.debug("no data for attribute " + id + " Ox" + Integer.toHexString(id));
					}
				}
			} catch (Throwable e) {
				if (debug) {
					DebugLog.error("error populate attribute " + id + " Ox" + Integer.toHexString(id), e);
				}
			}
		}
		return anyRetrived;
	}

	// ---------------------- Client RFCOMM connections ----------------------

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		// TODO Auto-generated method stub

	}

	public int getSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	// ---------------------- Server RFCOMM connections ----------------------

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
	// ----------------------

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
	// ----------------------

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
}
