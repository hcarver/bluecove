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
import java.io.IOException;
import java.util.Enumeration;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

public class BluetoothStackMicrosoft extends BluetoothPeer implements BluetoothStack {

	boolean peerInitialized = false;

	long localBluetoothAddress = 0;

	private DiscoveryListener currentDeviceDiscoveryListener;

	private Thread limitedDiscoverableTimer;

	// TODO what is the real number for Attributes retrivable ?
	private final static int ATTR_RETRIEVABLE_MAX = 256;

	BluetoothStackMicrosoft() {
	}

	public String getStackID() {
		return BlueCoveImpl.STACK_WINSOCK;
	}

	public int getLibraryVersion() {
		return super.getLibraryVersion();
	}

	public int detectBluetoothStack() {
		return super.detectBluetoothStack();
	}

	public void enableNativeDebug(Class nativeDebugCallback, boolean on) {
		super.enableNativeDebug(nativeDebugCallback, on);
	}

	public void initialize() {
		try {
			int status = BluetoothPeer.initializationStatus();
			DebugLog.debug("initializationStatus", status);
			if (status == 1) {
				peerInitialized = true;
			}
		} catch (IOException e) {
			DebugLog.fatal("initialization", e);
		}
	}

	public void destroy() {
		if (peerInitialized) {
			peerInitialized = false;
			super.uninitialize();
		}
		cancelLimitedDiscoverableTimer();
	}

	public void initialized() throws BluetoothStateException {
		if (!peerInitialized) {
			throw new BluetoothStateException("Bluetooth system is unavailable");
		}
	}

	public String getLocalDeviceBluetoothAddress() {
		try {
			long socket = super.socket(false, false);
			super.bind(socket);
			localBluetoothAddress = super.getsockaddress(socket);
			String address = RemoteDeviceHelper.getBluetoothAddress(localBluetoothAddress);
			super.storesockopt(socket);
			super.close(socket);
			return address;
		} catch (IOException e) {
			DebugLog.error("get local bluetoothAddress", e);
			return "000000000000";
		}
	}

	public String getLocalDeviceName() {
		if (localBluetoothAddress == 0) {
			getLocalDeviceBluetoothAddress();
		}
		return super.getradioname(localBluetoothAddress);
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		return super.getpeername(address);
	}

	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(super.getDeviceClass(localBluetoothAddress));
	}

	private void cancelLimitedDiscoverableTimer() {
		if (limitedDiscoverableTimer != null) {
			limitedDiscoverableTimer.interrupt();
			limitedDiscoverableTimer = null;
		}
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		switch (mode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			cancelLimitedDiscoverableTimer();
			DebugLog.debug("setDiscoverable(false)");
			super.setDiscoverable(false);
			break;
		case DiscoveryAgent.GIAC:
			cancelLimitedDiscoverableTimer();
			DebugLog.debug("setDiscoverable(true)");
			super.setDiscoverable(true);
			break;
		case DiscoveryAgent.LIAC:
			if (limitedDiscoverableTimer != null) {
				break;
			}
			DebugLog.debug("setDiscoverable(LIAC)");
			super.setDiscoverable(true);
			// Timer to turn it off
			limitedDiscoverableTimer = Utils.schedule(60*1000, new Runnable() {
				public void run() {
					try {
						setDiscoverable(false);
					} catch (BluetoothStateException e) {
						DebugLog.debug("error setDiscoverable", e);
					} finally {
						limitedDiscoverableTimer = null;
					}
				}
			});
			break;
		}
		return true;
	}

	public boolean isLocalDevicePowerOn() {
		int mode = super.getBluetoothRadioMode();
		return ((mode == BluetoothPeer.BTH_MODE_CONNECTABLE) || (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE));
	}

	public int getLocalDeviceDiscoverable() {
		int mode = super.getBluetoothRadioMode();
		if (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE) {
			if (limitedDiscoverableTimer != null) {
				DebugLog.debug("Discoverable = LIAC");
				return DiscoveryAgent.LIAC;
			} else {
				DebugLog.debug("Discoverable = GIAC");
				return DiscoveryAgent.GIAC;
			}
		} else {
			DebugLog.debug("Discoverable = NOT_DISCOVERABLE");
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

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
			return "0";
		}

		if ("bluecove.radio.version".equals(property)) {
			return String.valueOf(super.getDeviceVersion(localBluetoothAddress));
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(super.getDeviceManufacturer(localBluetoothAddress));
		}
		return null;
	}

	//	 --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		initialized();
		if (currentDeviceDiscoveryListener != null) {
			throw new BluetoothStateException();
		}
		currentDeviceDiscoveryListener = listener;
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (currentDeviceDiscoveryListener != listener) {
			return false;
		}
		return super.cancelInquiry();
	}

	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		try {
			return super.runDeviceInquiry(startedNotify, accessCode, listener);
		} finally {
			currentDeviceDiscoveryListener = null;
		}
	}

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired) {
		super.deviceDiscoveredCallback(listener, deviceAddr, deviceClass, deviceName, paired);
	}

	//	 --- Service search

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		int[] handles;
		try {
			handles = super.runSearchServices(uuidSet, RemoteDeviceHelper.getAddress(device));
		} catch (SearchServicesDeviceNotReachableException e) {
			return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
		} catch (SearchServicesTerminatedException e) {
			return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
		} catch (SearchServicesException e) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		}
		if (handles == null) {
			return DiscoveryListener.SERVICE_SEARCH_ERROR;
		} else if (handles.length > 0) {
			ServiceRecord[] records = new ServiceRecordImpl[handles.length];
			boolean hasError = false;
			for (int i = 0; i < handles.length; i++) {
				records[i] = new ServiceRecordImpl(device, handles[i]);
				try {
					if (!records[i].populateRecord(new int[] { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004 })) {
						hasError = true;
					}
					if (attrSet != null) {
						if (!records[i].populateRecord(attrSet)) {
							hasError = true;
						}
					}
				} catch (Exception e) {
					DebugLog.debug("populateRecord error", e);
					hasError = true;
				}
				if (startedNotify.isTerminated()) {
					return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
				}
			}
			listener.servicesDiscovered(startedNotify.getTransID(), records);
			if (hasError) {
				return DiscoveryListener.SERVICE_SEARCH_ERROR;
			} else {
				return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
			}
		} else {
			return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
		}
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
		if (sst != null) {
			sst.setTerminated();
			return true;
		} else {
			return false;
		}
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		if (attrIDs.length > ATTR_RETRIEVABLE_MAX) {
			throw new IllegalArgumentException();
		}
		/*
		 * retrieve SDP blob
		 */
		byte[] blob = super.getServiceAttributes(attrIDs,
				RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice()),
				(int)serviceRecord.getHandle());

		if (blob.length > 0) {
			try {
				boolean anyRetrived = false;
				DataElement element = (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
				for (Enumeration e = (Enumeration) element.getValue(); e.hasMoreElements();) {
					int attrID = (int) ((DataElement) e.nextElement()).getLong();
					serviceRecord.populateAttributeValue(attrID, (DataElement)e.nextElement());
					if (!anyRetrived) {
						for (int i = 0; i < attrIDs.length; i++) {
							if (attrIDs[i] == attrID) {
								anyRetrived = true;
								break;
							}
						}
					}
				}
				return anyRetrived;
			} catch (Exception e) {
				throw new IOException();
			}
		} else {
			return false;
		}
	}

//	 --- Client RFCOMM connections

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams, int, int)
	 */
	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		long socket = super.socket(params.authenticate, params.encrypt);
		try {
			super.connect(socket, params.address, params.channel);
		} catch (IOException e) {
			super.close(socket);
			throw e;
		}
		return socket;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		super.close(handle);
	}

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
		/*
		 * open socket
		 */
		long socket = super.socket(params.authenticate, params.encrypt);

		try {
			super.bind(socket);
			super.listen(socket);

			int channel = super.getsockchannel(socket);
			DebugLog.debug("service channel ", channel);

			int serviceRecordHandle = (int) socket;
			serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, params.uuid, params.name, false);

			/*
			 * register service
			 */
			serviceRecord.setHandle(super.registerService(serviceRecord.toByteArray()));

		} catch (IOException e) {
			super.close(socket);
			throw e;
		}
		return socket;
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {

		/*
		 * close socket
		 */
		super.close(handle);
		/*
		 * unregister service
		 */
		super.unregisterService(serviceRecord.getHandle());
	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		return super.accept(handle);
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		super.unregisterService(serviceRecord.getHandle());
		serviceRecord.setHandle(super.registerService(((ServiceRecordImpl) serviceRecord).toByteArray()));
		DebugLog.debug("new serviceRecord", serviceRecord);
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		connectionRfCloseClientConnection(handle);
	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		return super.getpeeraddress(handle);
	}

	public int connectionRfRead(long handle) throws IOException {
		return super.recv(handle);
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		return super.recv(handle, b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		return super.recvAvailable(handle);
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		super.send(handle, b);
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		super.send(handle, b, off, len);
	}

	public void connectionRfFlush(long handle) throws IOException {
		// TODO are there any flush
	}

//	---------------------- Client and Server L2CAP connections ----------------------

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams, int, int)
	 */
	public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
	 */
	public void l2CloseClientConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams, int, int, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl, boolean)
	 */
	public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		throw new ServiceRegistrationException("Not Supported on" + getStackID());
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
	 */
	public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
	 */
	public void l2CloseServerConnection(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
	 */
	public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
	 */
	public boolean l2Ready(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
	 */
	public int l2Receive(long handle, byte[] inBuf) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
	 */
	public void l2Send(long handle, byte[] data) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
	 */
	public int l2GetReceiveMTU(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
	 */
	public int l2GetTransmitMTU(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
	 */
	public long l2RemoteAddress(long handle) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}
}
