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

class BluetoothStackWIDCOMM implements BluetoothStack {

	private boolean initialized = false;

	private Vector deviceDiscoveryListeners = new Vector/* <DiscoveryListener> */();

	private Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();

	// TODO what is the real number for Attributes retrievable ?
	private final static int ATTR_RETRIEVABLE_MAX = 256;

	private final static int RECEIVE_MTU_MAX = 1024;

	static {
		NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_WIDCOMM);
	}

	BluetoothStackWIDCOMM() {
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WIDCOMM;
	}

	public native int getLibraryVersion();

	public native int detectBluetoothStack();

	public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

	public void initialize() {
		if (!initializeImpl()) {
			throw new RuntimeException("WIDCOMM BluetoothStack not found");
		}
		initialized = true;
	}

	public native boolean initializeImpl();

	private native void uninitialize();

	public void destroy() {
		if (initialized) {
			uninitialize();
			initialized = false;
			DebugLog.debug("WIDCOMM destroyed");
		}
	}

	protected void finalize() {
		destroy();
	}

	public native String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

	public native String getLocalDeviceName();

	private native int getDeviceClassImpl();

	/**
	 * There are no functions to set WIDCOMM stack
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(getDeviceClassImpl());
	}

	/**
	 * There are no functions to set WIDCOMM stack discoverable status.
	 * 
	 * @return <code>true</code> if the request succeeded, otherwise
	 *         <code>false</code> if the request failed because the BCC denied
	 *         the request; <code>false</code> if the Bluetooth system does
	 *         not support the access mode specified in <code>mode</code>
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		int curentMode = getLocalDeviceDiscoverable();
		if (curentMode == mode) {
			return true;
		} else {
			return false;
		}
	}

	private native boolean isStackServerUp();

	private void verifyDeviceReady() throws BluetoothStateException {
		if (!isLocalDevicePowerOn()) {
			throw new BluetoothStateException("Bluetooth Device is not ready");
		}
	}

	public native boolean isLocalDeviceDiscoverable();

	public int getLocalDeviceDiscoverable() {
		if (isStackServerUp() && isLocalDeviceDiscoverable()) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	public native boolean isLocalDevicePowerOn();

	private native String getBTWVersionInfo();

	private native int getDeviceVersion();

	private native int getDeviceManufacturer();

	public String getLocalDeviceProperty(String property) {
		final String TRUE = "true";
		final String FALSE = "false";
		if ("bluetooth.connected.devices.max".equals(property)) {
			return "7";
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
			return FALSE;
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

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
	 */
	public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}

	// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		deviceDiscoveryListeners.addElement(listener);
		deviceDiscoveryListenerReportedDevices.put(listener, new Vector());
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException {
		try {
			return runDeviceInquiryImpl(startedNotify, accessCode, listener);
		} finally {
			deviceDiscoveryListeners.removeElement(listener);
			deviceDiscoveryListenerReportedDevices.remove(listener);
		}
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired) {
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

	native String getRemoteDeviceFriendlyName(long address, int majorDeviceClass, int minorDeviceClass)
			throws IOException;

	/**
	 * get device name while discovery running. Device may not report its name
	 * first time while discovering.
	 * 
	 * @param address
	 * @return name
	 */
	native String peekRemoteDeviceFriendlyName(long address);

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		if (deviceDiscoveryListeners.size() != 0) {
			// discovery running
			return peekRemoteDeviceFriendlyName(address);
		} else {
			// Another way to get name is to run deviceInquiry
			DiscoveryListener listener = new DiscoveryListenerAdapter();
			if (startInquiry(DiscoveryAgent.GIAC, listener)) {
				String name = peekRemoteDeviceFriendlyName(address);
				cancelInquiry(listener);
				return name;
			}
		}
		return null;
	}

	// --- Service search

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
			throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	private native void cancelServiceSearchImpl();

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			sst.setTerminated();
			cancelServiceSearchImpl();
			return true;
		} else {
			return false;
		}
	}

	private native long[] runSearchServicesImpl(SearchServicesThread startedNotify, byte[] uuidValue, long address)
			throws BluetoothStateException, SearchServicesTerminatedException;

	private native byte[] getServiceAttribute(int attrID, long handle) throws IOException;

	private native boolean isServiceRecordDiscoverable(long address, long handle) throws IOException;

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
			RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		// Retrieve all Records, Filter here in Java
		synchronized (BluetoothStackWIDCOMM.class) {
			byte[] uuidValue = Utils.UUIDToByteArray(BluetoothConsts.L2CAP_PROTOCOL_UUID);
			long[] handles;
			try {
				handles = runSearchServicesImpl(startedNotify, uuidValue, RemoteDeviceHelper.getAddress(device));
			} catch (SearchServicesTerminatedException e) {
				DebugLog.debug("SERVICE_SEARCH_TERMINATED");
				return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
			}
			if (handles == null) {
				DebugLog.debug("SERVICE_SEARCH_ERROR");
				return DiscoveryListener.SERVICE_SEARCH_ERROR;
			} else if (handles.length > 0) {

				boolean reqRFCOMM = false;
				// boolean reqL2CAP = false;
				UUID uuidFiler = null;
				// If Search for specific service, select its UUID
				for (int u = 0; u < uuidSet.length; u++) {
					if (uuidSet[u].equals(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
						// reqL2CAP = true;
						continue;
					}
					if (uuidSet[u].equals(BluetoothConsts.RFCOMM_PROTOCOL_UUID)) {
						reqRFCOMM = true;
						continue;
					}
					uuidFiler = uuidSet[u];
					break;
				}
				if ((uuidFiler == null) && (reqRFCOMM)) {
					uuidFiler = BluetoothConsts.RFCOMM_PROTOCOL_UUID;
				} else if (BluetoothStackWIDCOMMSDPInputStream.debug) {
					DebugLog.debug("uuidFiler selected", uuidFiler);
				}

				Vector records = new Vector();
				int[] uuidFilerAttrIDs = new int[] { BluetoothConsts.ServiceClassIDList,
						BluetoothConsts.ProtocolDescriptorList };
				int[] requiredAttrIDs = new int[] { BluetoothConsts.ServiceRecordHandle,
						BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID };
				for (int i = 0; i < handles.length; i++) {
					ServiceRecordImpl sr = new ServiceRecordImpl(this, device, handles[i]);
					try {
						sr.populateRecord(uuidFilerAttrIDs);
						if ((uuidFiler != null)
								&& !(sr.hasServiceClassUUID(uuidFiler) || sr.hasProtocolClassUUID(uuidFiler))) {
							if (BluetoothStackWIDCOMMSDPInputStream.debug) {
								DebugLog.debug("filtered ServiceRecord (" + i + ")", sr);
							}
							continue;
						}
						if (BluetoothStackWIDCOMMSDPInputStream.debug) {
							DebugLog.debug("accepted ServiceRecord (" + i + ")", sr);
						}
						if (!isServiceRecordDiscoverable(RemoteDeviceHelper.getAddress(device), sr.getHandle())) {
							continue;
						}

						records.addElement(sr);
						sr.populateRecord(requiredAttrIDs);
						if (attrSet != null) {
							sr.populateRecord(attrSet);
						}
						DebugLog.debug("ServiceRecord (" + i + ") sr.handle", handles[i]);
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
				}
			}
			DebugLog.debug("SERVICE_SEARCH_NO_RECORDS");
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
			throws IOException {
		if (attrIDs.length > ATTR_RETRIEVABLE_MAX) {
			throw new IllegalArgumentException();
		}
		boolean anyRetrived = false;
		for (int i = 0; i < attrIDs.length; i++) {
			int id = attrIDs[i];
			try {
				byte[] sdpStruct = getServiceAttribute(id, serviceRecord.getHandle());
				if (sdpStruct != null) {
					if (BluetoothStackWIDCOMMSDPInputStream.debug) {
						DebugLog.debug("decode attribute " + id + " Ox" + Integer.toHexString(id));
					}
					DataElement element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(sdpStruct)))
							.readElement();

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
				} else {
					if (BluetoothStackWIDCOMMSDPInputStream.debug) {
						DebugLog.debug("no data for attribute " + id + " Ox" + Integer.toHexString(id));
					}
				}
			} catch (Throwable e) {
				if (BluetoothStackWIDCOMMSDPInputStream.debug) {
					DebugLog.error("error populate attribute " + id + " Ox" + Integer.toHexString(id), e);
				}
			}
		}
		return anyRetrived;
	}

	// --- Client RFCOMM connections

	private native long connectionRfOpenClientConnectionImpl(long address, int channel, boolean authenticate,
			boolean encrypt, int timeout) throws IOException;

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		verifyDeviceReady();
		return connectionRfOpenClientConnectionImpl(params.address, params.channel, params.authenticate,
				params.encrypt, params.timeout);
	}

	private native void closeRfCommPortImpl(long handle) throws IOException;

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		closeRfCommPortImpl(handle);
	}

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;

	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

	public void connectionRfFlush(long handle) throws IOException {
		// TODO are there any flush
	}

	public int getSecurityOpt(long handle, int expected) throws IOException {
		return expected;
	}

	private native long rfServerOpenImpl(byte[] uuidValue, byte[] uuidValue2, boolean obexSrv, String name,
			boolean authenticate, boolean encrypt) throws IOException;

	private native int rfServerSCN(long handle) throws IOException;

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
			throws IOException {
		verifyDeviceReady();
		byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
		byte[] uuidValue2 = params.obex ? null : Utils.UUIDToByteArray(BluetoothConsts.SERIAL_PORT_UUID);
		long handle = rfServerOpenImpl(uuidValue, uuidValue2, params.obex, params.name, params.authenticate,
				params.encrypt);
		int channel = rfServerSCN(handle);
		DebugLog.debug("serverSCN", channel);
		long serviceRecordHandle = handle;

		serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, params.uuid, params.name, params.obex);

		return handle;
	}

	private native void sdpServiceAddAttribute(long handle, char handleType, int attrID, short attrType, byte[] value)
			throws ServiceRegistrationException;

	private byte[] long2byte(long value, int len) {
		byte[] cvalue = new byte[len];
		long l = value;
		for (int i = len - 1; i >= 0; i--) {
			cvalue[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return cvalue;
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		sdpServiceUpdateServiceRecord(handle, 'r', serviceRecord);
	}

	private byte[] sdpServiceSequenceAttribute(Enumeration en) throws ServiceRegistrationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SDPOutputStream sdpOut = new SDPOutputStream(out);
		try {
			for (; en.hasMoreElements();) {
				sdpOut.writeElement((DataElement) en.nextElement());
			}
		} catch (IOException e) {
			throw new ServiceRegistrationException(e.getMessage());
		}
		return out.toByteArray();
	}

	private void sdpServiceUpdateServiceRecord(long handle, char handleType, ServiceRecordImpl serviceRecord)
			throws ServiceRegistrationException {
		int[] ids = serviceRecord.getAttributeIDs();
		if ((ids == null) || (ids.length == 0)) {
			return;
		}
		// from WIDCOMM BtIfDefinitions.h
		final short NULL_DESC_TYPE = 0;
		final short UINT_DESC_TYPE = 1;
		final short TWO_COMP_INT_DESC_TYPE = 2;
		final short UUID_DESC_TYPE = 3;
		final short TEXT_STR_DESC_TYPE = 4;
		final short BOOLEAN_DESC_TYPE = 5;
		final short DATA_ELE_SEQ_DESC_TYPE = 6;
		final short DATA_ELE_ALT_DESC_TYPE = 7;
		final short URL_DESC_TYPE = 8;

		for (int i = 0; i < ids.length; i++) {
			int id = ids[i];
			switch (id) {
			case BluetoothConsts.ServiceRecordHandle:
			case BluetoothConsts.ServiceClassIDList:
			case BluetoothConsts.ProtocolDescriptorList:
			case BluetoothConsts.AttributeIDServiceName:
				continue;
			}

			DataElement d = serviceRecord.getAttributeValue(id);
			switch (d.getDataType()) {
			case DataElement.U_INT_1:
				sdpServiceAddAttribute(handle, handleType, id, UINT_DESC_TYPE, long2byte(d.getLong(), 1));
				break;
			case DataElement.U_INT_2:
				sdpServiceAddAttribute(handle, handleType, id, UINT_DESC_TYPE, long2byte(d.getLong(), 2));
				break;
			case DataElement.U_INT_4:
				sdpServiceAddAttribute(handle, handleType, id, UINT_DESC_TYPE, long2byte(d.getLong(), 4));
				break;
			case DataElement.U_INT_8:
				sdpServiceAddAttribute(handle, handleType, id, UINT_DESC_TYPE, (byte[]) d.getValue());
				break;
			case DataElement.U_INT_16:
				sdpServiceAddAttribute(handle, handleType, id, UINT_DESC_TYPE, (byte[]) d.getValue());
				break;
			case DataElement.INT_1:
				sdpServiceAddAttribute(handle, handleType, id, TWO_COMP_INT_DESC_TYPE, long2byte(d.getLong(), 1));
				break;
			case DataElement.INT_2:
				sdpServiceAddAttribute(handle, handleType, id, TWO_COMP_INT_DESC_TYPE, long2byte(d.getLong(), 2));
				break;
			case DataElement.INT_4:
				sdpServiceAddAttribute(handle, handleType, id, TWO_COMP_INT_DESC_TYPE, long2byte(d.getLong(), 4));
				break;
			case DataElement.INT_8:
				sdpServiceAddAttribute(handle, handleType, id, TWO_COMP_INT_DESC_TYPE, long2byte(d.getLong(), 8));
				break;
			case DataElement.INT_16:
				sdpServiceAddAttribute(handle, handleType, id, TWO_COMP_INT_DESC_TYPE, (byte[]) d.getValue());
				break;
			case DataElement.URL:
				sdpServiceAddAttribute(handle, handleType, id, URL_DESC_TYPE, Utils.getASCIIBytes(d.getValue()
						.toString()));
				break;
			case DataElement.STRING:
				sdpServiceAddAttribute(handle, handleType, id, TEXT_STR_DESC_TYPE, Utils.getUTF8Bytes(d.getValue()
						.toString()));
				break;
			case DataElement.NULL:
				sdpServiceAddAttribute(handle, handleType, id, NULL_DESC_TYPE, null);
				break;
			case DataElement.BOOL:
				sdpServiceAddAttribute(handle, handleType, id, BOOLEAN_DESC_TYPE,
						new byte[] { (byte) (d.getBoolean() ? 1 : 0) });
				break;
			case DataElement.UUID:
				sdpServiceAddAttribute(handle, handleType, id, UUID_DESC_TYPE, BluetoothStackWIDCOMMSDPInputStream
						.getUUIDHexBytes((UUID) d.getValue()));
				break;
			case DataElement.DATSEQ:
				sdpServiceAddAttribute(handle, handleType, id, DATA_ELE_SEQ_DESC_TYPE,
						sdpServiceSequenceAttribute((Enumeration) d.getValue()));
				break;
			case DataElement.DATALT:
				sdpServiceAddAttribute(handle, handleType, id, DATA_ELE_ALT_DESC_TYPE,
						sdpServiceSequenceAttribute((Enumeration) d.getValue()));
				break;
			default:
				throw new ServiceRegistrationException("Invalid " + d.getDataType());
			}
		}
	}

	public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

	public native void connectionRfCloseServerConnection(long handle) throws IOException;

	private native void rfServerCloseImpl(long handle) throws IOException;

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		rfServerCloseImpl(handle);
	}

	// ---------------------- Client and Server L2CAP connections

	private void validateMTU(int receiveMTU, int transmitMTU) {
		if (receiveMTU > RECEIVE_MTU_MAX) {
			throw new IllegalArgumentException("invalid ReceiveMTU value " + receiveMTU);
		}
		// if (transmitMTU > RECEIVE_MTU_MAX) {
		// throw new IllegalArgumentException("invalid TransmitMTU value " +
		// transmitMTU);
		// }
		// int min = L2CAPConnection.DEFAULT_MTU;
		// if ((receiveMTU > L2CAPConnection.MINIMUM_MTU) && (receiveMTU < min))
		// {
		// min = receiveMTU;
		// }
		// if ((transmitMTU > L2CAPConnection.MINIMUM_MTU) && (transmitMTU <
		// min)) {
		// min = transmitMTU;
		// }
		// return min;
	}

	private native long l2OpenClientConnectionImpl(long address, int channel, boolean authenticate, boolean encrypt,
			int receiveMTU, int transmitMTU, int timeout) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams,
	 *      int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU)
			throws IOException {
		verifyDeviceReady();
		validateMTU(receiveMTU, transmitMTU);
		return l2OpenClientConnectionImpl(params.address, params.channel, params.authenticate, params.encrypt,
				receiveMTU, transmitMTU, params.timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public native void l2CloseClientConnection(long handle) throws IOException;

	private native long l2ServerOpenImpl(byte[] uuidValue, boolean authenticate, boolean encrypt, String name,
			int receiveMTU, int transmitMTU, int assignPsm) throws IOException;

	public native int l2ServerPSM(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams,
	 *      int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
			ServiceRecordImpl serviceRecord) throws IOException {
		verifyDeviceReady();
		validateMTU(receiveMTU, transmitMTU);
		byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
		long handle = l2ServerOpenImpl(uuidValue, params.authenticate, params.encrypt, params.name, receiveMTU,
				transmitMTU, params.bluecove_ext_psm);

		int channel = l2ServerPSM(handle);

		int serviceRecordHandle = (int) handle;

		serviceRecord.populateL2CAPAttributes(serviceRecordHandle, channel, params.uuid, params.name);

		return handle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long,
	 *      com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		sdpServiceUpdateServiceRecord(handle, 'l', serviceRecord);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	public native long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public native void l2CloseServerConnection(long handle) throws IOException;

	private native void l2ServerCloseImpl(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long,
	 *      com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		l2ServerCloseImpl(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public native int l2GetReceiveMTU(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public native int l2GetTransmitMTU(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public native boolean l2Ready(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public native int l2Receive(long handle, byte[] inBuf) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public native void l2Send(long handle, byte[] data) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public native long l2RemoteAddress(long handle) throws IOException;
}
