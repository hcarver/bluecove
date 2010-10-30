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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

/**
 *
 * @author Mina Shokry
 */
public class BluetoothStackAndroid implements BluetoothStack {

	private BluetoothAdapter localBluetoothAdapter;

	// TODO what is the real number for Attributes retrievable ?
    private final static int ATTR_RETRIEVABLE_MAX = 256;
	private Map<String, String> propertiesMap;

	private Activity context;
	
	/**
	 * This implementation will turn bluetooth on if it was off, in this case,
	 * bluetooth will be turned off at shutting down stack.
	 */
	private boolean justEnabled;

	private static final int REQUEST_CODE_CHANGE_DISCOVERABLE = 0;

	private Map<DiscoveryListener, DiscoveryBroadcastReceiver> listenerMap;

	private static final UUID UUID_OBEX = new UUID(0x0008);
	private static final UUID UUID_OBEX_OBJECT_PUSH = new UUID(0x1105);
	private static final UUID UUID_OBEX_FILE_TRANSFER = new UUID(0x1106);
	private List<UUID> obexUUIDs;

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
		if (localBluetoothAdapter == null) {
			throw new BluetoothStateException("Bluetooth isn't supported on this device");
		}

		Object contextObject = BlueCoveImpl.getConfigObject(BlueCoveConfigProperties.PROPERTY_ANDROID_CONTEXT);
		if (contextObject == null || !(contextObject instanceof Activity)) {
			throw new BluetoothStateException("Property " + BlueCoveConfigProperties.PROPERTY_ANDROID_CONTEXT
					+ " MUST be correctly set before initializing the stack. Call "
					+ "BlueCoveImpl.setConfigObject(BluecoveConfigProperties.PROPERTY_ANDROID_CONTEXT, <a reference to a context>)"
					+ " before calling LocalDevice.getLocalDevice()");
		}
		context = (Activity) contextObject;

		listenerMap = new HashMap<DiscoveryListener, DiscoveryBroadcastReceiver>();

		obexUUIDs = new ArrayList<UUID>();
		obexUUIDs.add(UUID_OBEX);
		obexUUIDs.add(UUID_OBEX_OBJECT_PUSH);
		obexUUIDs.add(UUID_OBEX_FILE_TRANSFER);
		String obexUUIDsProperty = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_ANDROID_OBEX_UUIDS);
		if (obexUUIDsProperty != null) {
			String[] uuids = obexUUIDsProperty.split(",");
			for (String uuid : uuids) {
				try {
					UUID jsr82UUID = new UUID(uuid, false);
					obexUUIDs.add(jsr82UUID);
				} catch (Exception ex) {
					// ignore wrong values.
				}
			}
		}

		try {
			if (!localBluetoothAdapter.isEnabled()) {
				if (localBluetoothAdapter.enable()) {
					justEnabled = true;
				}
			}

			final String TRUE = "true";
			final String FALSE = "false";
			propertiesMap = new HashMap<String, String>();
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX, "7");
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX, "7");
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY_SCAN, TRUE);
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE_SCAN, TRUE);
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY, TRUE);
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE, TRUE);
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_ATTR_RETRIEVABLE_MAX, String.valueOf(ATTR_RETRIEVABLE_MAX));
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_MASTER_SWITCH, FALSE);
			propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX, "0");

		} catch (Exception ex) {
			BluetoothStateException bluetoothStateException = new BluetoothStateException(ex.toString());
			throw bluetoothStateException;
		}
	}

	public void destroy() {
		if (justEnabled) {
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
		// TODO: which of these is really available?
		return FEATURE_SERVICE_ATTRIBUTES /*| FEATURE_L2CAP*/ | FEATURE_RSSI;
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
		// TODO: not yet implemented
	}

	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

		int androidMode = 0;
		switch (mode) {
			case DiscoveryAgent.GIAC:
				androidMode = BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
				break;
			case DiscoveryAgent.LIAC:
				androidMode = BluetoothAdapter.SCAN_MODE_CONNECTABLE;
				break;
			case DiscoveryAgent.NOT_DISCOVERABLE:
				androidMode = BluetoothAdapter.SCAN_MODE_NONE;
				break;
			// any other value is invalid and this was previously checked for in
			// implementation of LocalDevice.setDiscoverable()
		}
		intent.putExtra(BluetoothAdapter.EXTRA_SCAN_MODE, androidMode);

		int duration;
		if (mode == DiscoveryAgent.NOT_DISCOVERABLE) {
			duration = 0;
		} else {
			duration = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_ANDROID_DISCOVERABLE_DURATION, 120);
		}
		intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);

		context.startActivityForResult(intent, REQUEST_CODE_CHANGE_DISCOVERABLE);
		
		// TODO: return appropriate value according to whether mode was changed or not.
		return true;
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
		return propertiesMap.get(property);
	}

	public boolean authenticateRemoteDevice(long address) throws IOException {
		return false;
	}

	public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
		return false;
	}

	public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
		throw new NotSupportedIOException(getStackID());
	}

	public boolean startInquiry(int accessCode, final DiscoveryListener listener) throws BluetoothStateException {
		DiscoveryBroadcastReceiver discoveryBroadcastReceiver = new DiscoveryBroadcastReceiver(listener);
		listenerMap.put(listener, discoveryBroadcastReceiver);

		IntentFilter deviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		IntentFilter discoveryFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

		context.registerReceiver(discoveryBroadcastReceiver, deviceFound);
		context.registerReceiver(discoveryBroadcastReceiver, discoveryFinished);

		return localBluetoothAdapter.startDiscovery();
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		DiscoveryBroadcastReceiver discoveryBroadcastReceiver = listenerMap.get(listener);
		discoveryBroadcastReceiver.cancelled = true;
		return localBluetoothAdapter.cancelDiscovery();
	}

	public String getRemoteDeviceFriendlyName(long address) throws IOException {
		String addressString = getAddressAsString(address).toUpperCase();
		return localBluetoothAdapter.getRemoteDevice(addressString).getName();
	}

	private String getAddressAsString(long address) {
		String addressHex = Long.toHexString(address);
		return formatAddressInAndroid(addressHex);
	}

	private String formatAddressInAndroid(String bluetoothAddress) {
		StringBuilder buffer = new StringBuilder("000000000000".substring(bluetoothAddress.length()) + bluetoothAddress);
		for (int index = 2; index < buffer.length(); index += 3) {
			buffer.insert(index, ':');
		}
		return buffer.toString().toUpperCase();
	}

	private long getAddressAsLong(String address) {
		return Long.parseLong(address.replace(":", ""), 16);
	}

	public RemoteDevice[] retrieveDevices(int option) {
		Set<BluetoothDevice> bondedDevices = localBluetoothAdapter.getBondedDevices();
		RemoteDevice[] devices = new RemoteDevice[bondedDevices.size()];
		int index = 0;
		Iterator<BluetoothDevice> iterator = bondedDevices.iterator();
		while (iterator.hasNext()) {
			BluetoothDevice device = iterator.next();
			devices[index++] = RemoteDeviceHelper.createRemoteDevice(this,
					getAddressAsLong(device.getAddress()), device.getName(),
					device.getBondState() == BluetoothDevice.BOND_BONDED);
		}
		return devices;
	}

	public Boolean isRemoteDeviceTrusted(long address) {
		return null;
	}

	public Boolean isRemoteDeviceAuthenticated(long address) {
		return null;
	}

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice remoteDevice, DiscoveryListener listener) throws BluetoothStateException {
		if (uuidSet.length != 1) {
			throw new BluetoothStateException("Searching for services with more than one UUID isn't supported on Android");
		}
		SearchServicesRunnable searchServicesRunnable = new SearchServicesRunnable() {
			public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice remoteDevice, DiscoveryListener listener) throws BluetoothStateException {
				try {
					sst.searchServicesStartedCallback();
					for (UUID jsr82UUID : uuidSet) {
						java.util.UUID javaUUID = createJavaUUID(jsr82UUID);
						String addressInAndroidFormat = formatAddressInAndroid(remoteDevice.getBluetoothAddress());
						BluetoothDevice device = localBluetoothAdapter.getRemoteDevice(addressInAndroidFormat);
						BluetoothSocket socket = device.createRfcommSocketToServiceRecord(javaUUID);
						if (socket != null) {
							boolean obex = obexUUIDs.contains(jsr82UUID);
							listener.servicesDiscovered(sst.getTransID(), new ServiceRecord[] {createServiceRecord(remoteDevice, socket, jsr82UUID, obex)});
						}
						socket.close();
					}
					return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
				} catch (IOException ex) {
					return DiscoveryListener.SERVICE_SEARCH_ERROR;
				}
			}
		};

		return SearchServicesThread.startSearchServices(this, searchServicesRunnable, attrSet, uuidSet, remoteDevice, listener);
	}

	public boolean cancelServiceSearch(int transID) {
		SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
        if (sst != null) {
            return sst.setTerminated();
        } else {
            return false;
        }
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		// TODO: is this correct and best thing we can do?
		return false;
	}

	public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
		AndroidBluetoothConnectionParams androidParams = (AndroidBluetoothConnectionParams) params;
		BluetoothDevice bluetoothDevice = localBluetoothAdapter.getRemoteDevice(getAddressAsString(androidParams.address));
		UUID jsr82UUID = new UUID(androidParams.serviceUUID, false);
		BluetoothSocket socket = bluetoothDevice.createRfcommSocketToServiceRecord(createJavaUUID(jsr82UUID));
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.createConnection(socket);
		return bluetoothConnection.getHandle();
	}

	public int rfGetSecurityOpt(long handle, int expected) throws IOException {
		// TODO: is this correct?
		return ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
	}

	public void connectionRfCloseClientConnection(long handle) throws IOException {
		AndroidBluetoothConnection.getBluetoothConnection(handle).close();
	}

	public void connectionRfCloseServerConnection(long handle) throws IOException {
		AndroidBluetoothConnection.getBluetoothConnection(handle).close();
	}

	public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
		java.util.UUID javaUUID = createJavaUUID(params.uuid);
		BluetoothServerSocket serverSocket = localBluetoothAdapter.listenUsingRfcommWithServiceRecord(params.name, javaUUID);
		AndroidBluetoothConnection connection = AndroidBluetoothConnection.createServerConnection(serverSocket);

		return connection.getHandle();
	}

	public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		BluetoothServerSocket serverSocket = bluetoothConnection.getServerSocket();
		BluetoothSocket socket = serverSocket.accept();
		AndroidBluetoothConnection connection = AndroidBluetoothConnection.createConnection(socket);
		return connection.getHandle();
	}

	public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		BluetoothServerSocket serverSocket = bluetoothConnection.getServerSocket();
		serverSocket.close();

	}

	public long getConnectionRfRemoteAddress(long handle) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		String address = bluetoothConnection.getSocket().getRemoteDevice().getAddress();
		return getAddressAsLong(address);
	}

	public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
		// TODO: is this correct?
		return false;
	}

	public int connectionRfRead(long handle) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		InputStream inputStream = bluetoothConnection.getInputStream();
		return inputStream.read();
	}

	public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		InputStream inputStream = bluetoothConnection.getInputStream();
		return inputStream.read(b, off, len);
	}

	public int connectionRfReadAvailable(long handle) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		InputStream inputStream = bluetoothConnection.getInputStream();
		return inputStream.available();
	}

	public void connectionRfWrite(long handle, int b) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		OutputStream outputStream = bluetoothConnection.getOutputStream();
		outputStream.write(b);
	}

	public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		OutputStream outputStream = bluetoothConnection.getOutputStream();
		outputStream.write(b, off, len);
	}

	public void connectionRfFlush(long handle) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = AndroidBluetoothConnection.getBluetoothConnection(handle);
		OutputStream outputStream = bluetoothConnection.getOutputStream();
		outputStream.flush();
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

	private class DiscoveryBroadcastReceiver extends BroadcastReceiver {
		private DiscoveryListener discoveryListener;
		private boolean cancelled;

		public DiscoveryBroadcastReceiver(DiscoveryListener discoveryListener) {
			this.discoveryListener = discoveryListener;
			cancelled = false;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				BluetoothClass bluetoothClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
				RemoteDevice remoteDevice = createRemoteDevice(bluetoothDevice);
				DeviceClass deviceClass = createDeviceClass(bluetoothClass);
				discoveryListener.deviceDiscovered(remoteDevice, deviceClass);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				context.unregisterReceiver(this);
				discoveryListener.inquiryCompleted(cancelled ?
					DiscoveryListener.INQUIRY_TERMINATED :
					DiscoveryListener.INQUIRY_COMPLETED);
			}
		}
	}

	private RemoteDevice createRemoteDevice(BluetoothDevice bluetoothDevice) {
		RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this,
				getAddressAsLong(bluetoothDevice.getAddress()), bluetoothDevice.getName(),
				bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED);
		return remoteDevice;
	}
	
	private DeviceClass createDeviceClass(BluetoothClass bluetoothClass) {
		int record = bluetoothClass.getDeviceClass();
		for (int service = 0x4000; service  < 0x800000; service <<= 1) {
			if (bluetoothClass.hasService(service)) {
				record |= service;
			}
		}
		DeviceClass deviceClass = new DeviceClass(record);
		return deviceClass;
	}

	private java.util.UUID createJavaUUID(UUID jsr82UUID) {
		String uuidString = jsr82UUID.toString();
		String part1 = uuidString.substring(0, 8);
		String part2 = uuidString.substring(8, 16);
		String part3 = uuidString.substring(16, 24);
		String part4 = uuidString.substring(24, 32);

		long part1Long = Long.parseLong(part1, 16);
		long part2Long = Long.parseLong(part2, 16);
		long part3Long = Long.parseLong(part3, 16);
		long part4Long = Long.parseLong(part4, 16);

		long mostSigBits = (part1Long << 32) | part2Long;
		long leastSigBits = (part3Long << 32) | part4Long;
		
		java.util.UUID javaUUID = new java.util.UUID(mostSigBits, leastSigBits);

		return javaUUID;
	}

	private ServiceRecord createServiceRecord(RemoteDevice remoteDevice, BluetoothSocket socket, UUID uuid, boolean obex) {
		ServiceRecord record = new AndroidServiceRecord(this, remoteDevice, socket, uuid, obex);

		return record;
	}
}
