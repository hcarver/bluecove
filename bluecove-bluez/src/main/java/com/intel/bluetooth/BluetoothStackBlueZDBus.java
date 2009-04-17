/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mark Swanson
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
 *
 * @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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

import org.bluez.BlueZAPI;
import org.bluez.BlueZAPIFactory;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * A Java/DBUS implementation. Property "bluecove.deviceID" or "bluecove.deviceAddress"
 * can be used to select Local Bluetooth device.
 * 
 * bluecove.deviceID: String HCI ID. ID e.g. hci0, hci1, hci2, etc. bluecove.deviceID:
 * String Device number. e.g. 0, 1, 2, etc. bluecove.deviceAddress: String in JSR-82
 * format.
 * 
 * Please help with these questions:
 * 
 * 0. I note that Adapter.java has a bunch of methods commented out. Do you feel these
 * aren't needed to get a bare bones implementation working? I notice that
 * getLocalDeviceDiscoverable() could use adapter.getMode() "discoverable" though I have
 * no idea how to convert that to an int return value... 1.
 * 
 * A: The idea was that I copied all the method descriptors from bluez-d-bus
 * documentation. Some I tested and this is uncommented. Some I'm not sure are implemented
 * as described so I commented out.
 */
class BluetoothStackBlueZDBus implements BluetoothStack, DeviceInquiryRunnable, SearchServicesRunnable {

    // This native lib contains the rfcomm and l2cap linux-specific
    // implementation for this bluez d-bus implementation.
    public static final String NATIVE_BLUECOVE_LIB_BLUEZ = "bluecovez";

    private final static String BLUEZ_DEVICEID_PREFIX = "hci";

    private final static int LISTEN_BACKLOG_RFCOMM = 4;

    private final static int LISTEN_BACKLOG_L2CAP = 4;

    private final int l2cap_receiveMTU_max = 65535;

    private final static Vector<String> devicesUsed = new Vector<String>();

    // Our reusable DBUS connection.
    private DBusConnection dbusConn = null;

    private String deviceID;

    private BlueZAPI blueZ;

    static final int BLUECOVE_DBUS_VERSION = BlueCoveImpl.nativeLibraryVersionExpected;

    /**
     * The parsed long value of the adapter's BT 00:00:... address.
     */
    private long localDeviceBTAddress = -1;

    private Map<String, String> propertiesMap;

    private DiscoveryListener discoveryListener;

    private boolean deviceInquiryCanceled = false;

    private class DiscoveryData {

        public int deviceClass;

        public String name;

        boolean paired;
    }

    BluetoothStackBlueZDBus() {
    }

    public String getStackID() {
        return BlueCoveImpl.STACK_BLUEZ_DBUS;
    }

    public String toString() {
        if (deviceID != null) {
            return getStackID() + ":" + deviceID;
        } else {
            return getStackID();
        }
    }

    // --- Library initialization

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
        try {
            LibraryInformation unixSocketLib = new LibraryInformation("unix-java", false);
            unixSocketLib.stackClass = cx.ath.matthew.unix.UnixSocket.class;
            return new LibraryInformation[] { new LibraryInformation(NATIVE_BLUECOVE_LIB_BLUEZ), unixSocketLib };
        } catch (NoClassDefFoundError e) {
            // dubs may use different UnixSocket implementation
            return new LibraryInformation[] { new LibraryInformation(NATIVE_BLUECOVE_LIB_BLUEZ) };
        }
    }

    private native int getLibraryVersionNative();

    public int getLibraryVersion() throws BluetoothStateException {
        int version = getLibraryVersionNative();
        if (version != BLUECOVE_DBUS_VERSION) {
            DebugLog.fatal("BlueCove native library version mismatch " + version + " expected " + BLUECOVE_DBUS_VERSION);
            throw new BluetoothStateException("BlueCove native library version mismatch");
        }
        return version;
    }

    public int detectBluetoothStack() {
        return BlueCoveImpl.BLUECOVE_STACK_DETECT_BLUEZ;
    }

    /**
     * Returns a colon formatted BT address required by BlueZ. e.g. 00:01:C2:51:D1:31
     * 
     * @param l
     *            The long address to be converted to a string.
     * @return Note: can be optimized - was playing around with the formats required by
     *         BlueZ.
     */
    private String toHexString(long l) {
        StringBuffer buf = new StringBuffer();
        String lo = Integer.toHexString((int) l);
        if (l > 0xffffffffl) {
            String hi = Integer.toHexString((int) (l >> 32));
            buf.append(hi);
        }
        buf.append(lo);
        StringBuffer result = new StringBuffer();
        int prependZeros = 12 - buf.length();
        for (int i = 0; i < prependZeros; ++i) {
            result.append("0");
        }
        result.append(buf.toString());
        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < 12; i += 2) {
            hex.append(result.substring(i, i + 2));
            if (i < 10) {
                hex.append(":");
            }
        }
        return hex.toString().toUpperCase(Locale.ENGLISH);
    }

    private long convertBTAddress(String anAddress) {
        long btAddress = Long.parseLong(anAddress.replaceAll(":", ""), 16);
        return btAddress;
    }

    public void initialize() throws BluetoothStateException {
        boolean intialized = false;
        try {
            try {
                dbusConn = DBusConnection.getConnection(DBusConnection.SYSTEM);
            } catch (DBusException e) {
                DebugLog.error("Failed to get the dbus connection", e);
                throw new BluetoothStateException(e.getMessage());
            }
            try {
                blueZ = BlueZAPIFactory.getBlueZAPI(dbusConn);
            } catch (DBusException e) {
                DebugLog.error("Failed to get bluez dbus manager", e);
                throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException("Can't access BlueZ D-Bus"), e);
            }

            Path adapterPath;
            // If the user specifies a specific deviceID then we try to find it.
            String findID = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ID);
            String deviceAddressStr = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS);
            if (findID != null) {
                if (findID.startsWith(BLUEZ_DEVICEID_PREFIX)) {
                    adapterPath = blueZ.findAdapter(findID);
                    if (adapterPath == null) {
                        throw new BluetoothStateException("Can't find '" + findID + "' adapter");
                    }
                } else {
                    int findNumber = Integer.parseInt(findID);
                    adapterPath = blueZ.getAdapter(findNumber);
                    if (adapterPath == null) {
                        throw new BluetoothStateException("Can't find adapter #" + findID);
                    }
                }
            } else if (deviceAddressStr != null) {
                String pattern = toHexString(Long.parseLong(deviceAddressStr, 0x10));
                adapterPath = blueZ.findAdapter(pattern);
                if (adapterPath == null) {
                    throw new BluetoothStateException("Can't find adapter with address '" + deviceAddressStr + "'");
                }
            } else {
                adapterPath = blueZ.defaultAdapter();
                if (adapterPath == null) {
                    throw new BluetoothStateException("Can't find default adapter");
                }
            }
            try {
                blueZ.selectAdapter(adapterPath);
            } catch (DBusException e) {
                throw new BluetoothStateException(adapterPath + " " + e.getMessage());
            }
            localDeviceBTAddress = convertBTAddress(blueZ.getAdapterAddress());
            deviceID = blueZ.getAdapterID();
            if (devicesUsed.contains(deviceID)) {
                throw new BluetoothStateException("LocalDevice " + deviceID + " alredy in use");
            }
            propertiesMap = new TreeMap<String, String>();
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX, "7");
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX, "7");
            propertiesMap.put(BlueCoveLocalDeviceProperties.LOCAL_DEVICE_PROPERTY_DEVICE_ID, deviceID);

            final String TRUE = "true";
            final String FALSE = "false";
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY_SCAN, TRUE);
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE_SCAN, TRUE);
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY, TRUE);
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE, TRUE);
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_ATTR_RETRIEVABLE_MAX, String.valueOf(256));
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_MASTER_SWITCH, FALSE);
            propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX, String.valueOf(l2cap_receiveMTU_max));

            intialized = true;
        } finally {
            if (!intialized) {
                if (dbusConn != null) {
                    dbusConn.disconnect();
                }
                dbusConn = null;
            }
        }
    }

    public void destroy() {
        DebugLog.debug("destroy()");
        if (deviceID != null) {
            devicesUsed.removeElement(deviceID);
            deviceID = null;
        }
        if (dbusConn != null) {
            dbusConn.disconnect();
            dbusConn = null;
        }
    }

    @SuppressWarnings("unchecked")
    public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
     */
    public boolean isCurrentThreadInterruptedCallback() {
        // DebugLog.debug("isCurrentThreadInterruptedCallback()");
        return Thread.interrupted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#getFeatureSet()
     */
    public int getFeatureSet() {
        return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
    }

    // --- LocalDevice

    public String getLocalDeviceBluetoothAddress() throws BluetoothStateException {
        return RemoteDeviceHelper.getBluetoothAddress(localDeviceBTAddress);
    }

    public DeviceClass getLocalDeviceClass() {
        try {
            int record = blueZ.getAdapterDeviceClass();
            if (DiscoveryAgent.LIAC == getLocalDeviceDiscoverable()) {
                record |= BluetoothConsts.DeviceClassConsts.LIMITED_DISCOVERY_SERVICE;
            }
            return new DeviceClass(record);
        } catch (DBusExecutionException e) {
            DebugLog.error("getLocalDeviceClass", e);
            return null;
        }
    }

    /**
     * Retrieves the name of the local device.
     * 
     * @see javax.bluetooth.LocalDevice#getFriendlyName()
     */
    public String getLocalDeviceName() {
        return blueZ.getAdapterName();
    }

    public boolean isLocalDevicePowerOn() {
        return blueZ.isAdapterPowerOn();
    }

    public String getLocalDeviceProperty(String property) {
        if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_DEVICES_LIST.equals(property)) {
            StringBuffer b = new StringBuffer();
            for (String adapterId : blueZ.listAdapters()) {
                if (b.length() > 0) {
                    b.append(',');
                }
                b.append(adapterId);
            }
            return b.toString();
        } else if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_RADIO_VERSION.equals(property)) {
            return blueZ.getAdapterVersion() + "; HCI " + blueZ.getAdapterRevision();
        } else if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_RADIO_MANUFACTURER.equals(property)) {
            return blueZ.getAdapterManufacturer();
        } else {
            return propertiesMap.get(property);
        }
    }

    public int getLocalDeviceDiscoverable() {
        if (blueZ.isAdapterDiscoverable()) {
            int timeout = blueZ.getAdapterDiscoverableTimeout();
            if (timeout == 0) {
                return DiscoveryAgent.GIAC;
            } else {
                return DiscoveryAgent.LIAC;
            }
        } else {
            return DiscoveryAgent.NOT_DISCOVERABLE;
        }
    }

    public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
        if (getLocalDeviceDiscoverable() == mode) {
            return true;
        }
        try {
            return blueZ.setAdapterDiscoverable(mode);
        } catch (DBusException e) {
            throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
        } catch (DBusExecutionException e) {
            throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
     */
    public void setLocalDeviceServiceClasses(int classOfDevice) {
        DebugLog.debug("setLocalDeviceServiceClasses()");
        throw new NotSupportedRuntimeException(getStackID());
    }

    public boolean authenticateRemoteDevice(long address) throws IOException {
        try {
            blueZ.authenticateRemoteDevice(toHexString(address));
            return true;
        } catch (Throwable e) {
            DebugLog.error("Error creating bonding", e);
            return false;
        }
    }

    public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
        try {
            return blueZ.authenticateRemoteDevice(toHexString(address), passkey);
        } catch (Throwable e) {
            throw (IOException) UtilsJavaSE.initCause(new IOException(e.getMessage()), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice (long)
     */
    public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
        try {
            blueZ.removeAuthenticationWithRemoteDevice(toHexString(address));
        } catch (Throwable e) {
            throw (IOException) UtilsJavaSE.initCause(new IOException(e.getMessage()), e);
        }
    }

    // --- Device Inquiry

    public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
        DebugLog.debug("startInquiry()");
        if (discoveryListener != null) {
            throw new BluetoothStateException("Another inquiry already running");
        }
        discoveryListener = listener;
        deviceInquiryCanceled = false;
        return DeviceInquiryThread.startInquiry(this, this, accessCode, listener);
    }

    public int runDeviceInquiry(final DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
        DebugLog.debug("runDeviceInquiry()");
        try {

            // Different signal handlers get different device attributes
            // so we cache the data until device discovery is finished
            // and then create the RemoteDevice objects.
            final Map<Long, DiscoveryData> address2DiscoveryData = new HashMap<Long, DiscoveryData>();

            BlueZAPI.DeviceInquiryListener bluezDiscoveryListener = new BlueZAPI.DeviceInquiryListener() {

                public void deviceInquiryStarted() {
                    startedNotify.deviceInquiryStartedCallback();

                }

                public void deviceDiscovered(String deviceAddr, String deviceName, int deviceClass, boolean paired) {
                    long longAddress = convertBTAddress(deviceAddr);
                    DiscoveryData discoveryData = address2DiscoveryData.get(longAddress);
                    if (discoveryData == null) {
                        discoveryData = new DiscoveryData();
                        address2DiscoveryData.put(longAddress, discoveryData);
                    }
                    if (deviceName != null) {
                        discoveryData.name = deviceName;
                    }
                    if (deviceClass >= 0) {
                        discoveryData.deviceClass = deviceClass;
                    }
                }
            };

            try {
                blueZ.deviceInquiry(bluezDiscoveryListener);
            } catch (Throwable e) {
                if (deviceInquiryCanceled) {
                    return DiscoveryListener.INQUIRY_TERMINATED;
                } else {
                    DebugLog.error("deviceInquiry error", e);
                    throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
                }
            }

            for (Long address : address2DiscoveryData.keySet()) {
                DiscoveryData discoveryData = address2DiscoveryData.get(address);
                if (discoveryData.name == null) {
                    try {
                        discoveryData.name = blueZ.getRemoteDeviceFriendlyName(toHexString(address));
                    } catch (Throwable e) {
                        DebugLog.error("can't get device name", e);
                    }
                    if (discoveryData.name == null) {
                        discoveryData.name = "";
                    }
                }
                RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackBlueZDBus.this, address, discoveryData.name,
                        discoveryData.paired);
                listener.deviceDiscovered(remoteDevice, new DeviceClass(discoveryData.deviceClass));
                if (deviceInquiryCanceled) {
                    break;
                }
            }

            if (deviceInquiryCanceled) {
                return DiscoveryListener.INQUIRY_TERMINATED;
            } else {
                return DiscoveryListener.INQUIRY_COMPLETED;
            }
        } finally {
            discoveryListener = null;
        }
    }

    public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired) {
        // Not used here since there are no native callbacks
    }

    public boolean cancelInquiry(DiscoveryListener listener) {
        DebugLog.debug("cancelInquiry()");
        if (discoveryListener != null && discoveryListener == listener) {
            deviceInquiryCanceled = true;
            try {
                blueZ.deviceInquiryCancel();
                return true;
            } catch (Throwable e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Contact the remote device
     */
    public String getRemoteDeviceFriendlyName(final long deviceAddress) throws IOException {
        // return adapter.GetRemoteName(toHexString(anAddress));
        // For JSR-82 GetRemoteName can't be since it use cash.

        if (discoveryListener != null) {
            throw new IOException("DeviceInquiry alredy running");
        }
        try {
            return blueZ.getRemoteDeviceFriendlyName(toHexString(deviceAddress));
        } catch (DBusExecutionException e) {
            throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
        } catch (DBusException e) {
            throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), e);
        }
    }

    public RemoteDevice[] retrieveDevices(int option) {
        List<String> preKnownDevices = blueZ.retrieveDevices((DiscoveryAgent.PREKNOWN == option));
        if (preKnownDevices == null) {
            return null;
        }
        final Vector<RemoteDevice> devices = new Vector<RemoteDevice>();
        for (String addres : preKnownDevices) {
            devices.add(RemoteDeviceHelper.createRemoteDevice(this, convertBTAddress(addres), null, true));
        }
        return RemoteDeviceHelper.remoteDeviceListToArray(devices);
    }

    public Boolean isRemoteDeviceTrusted(long address) {
        try {
            return blueZ.isRemoteDeviceTrusted(toHexString(address));
        } catch (DBusExecutionException e) {
            DebugLog.error("isRemoteDeviceTrusted", e);
            return Boolean.FALSE;
        } catch (DBusException e) {
            DebugLog.error("isRemoteDeviceTrusted", e);
            return Boolean.FALSE;
        }
    }

    public Boolean isRemoteDeviceAuthenticated(long address) {
        try {
            return Boolean.valueOf(blueZ.isRemoteDeviceConnected(toHexString(address)) && blueZ.isRemoteDeviceTrusted(toHexString(address)));
        } catch (DBusExecutionException e) {
            DebugLog.error("isRemoteDeviceAuthenticated", e);
            return Boolean.FALSE;
        } catch (DBusException e) {
            DebugLog.error("isRemoteDeviceAuthenticated", e);
            return false;
        }
    }

    // --- Service search

    /**
     * Starts searching for services.
     * 
     * @return transId
     */
    public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
        try {
            DebugLog.debug("searchServices() device", device.getBluetoothAddress());
            return SearchServicesThread.startSearchServices(this, this, attrSet, uuidSet, device, listener);
        } catch (Exception ex) {
            DebugLog.debug("searchServices() failed", ex);
            throw new BluetoothStateException("searchServices() failed: " + ex.getMessage());
        }
    }

    private int getRemoteServices(SearchServicesThread sst, UUID[] uuidSet, RemoteDevice remoteDevice) {
        Map<Integer, String> xmlRecords;
        try {
            xmlRecords = blueZ.getRemoteDeviceServices(toHexString(RemoteDeviceHelper.getAddress(remoteDevice)));
        } catch (DBusException e) {
            DebugLog.error("get Service records failed", e);
            return DiscoveryListener.SERVICE_SEARCH_ERROR;
        } catch (DBusExecutionException e) {
            DebugLog.error("get Service records failed", e);
            return DiscoveryListener.SERVICE_SEARCH_ERROR;
        }
        if (xmlRecords == null) {
            return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
        }
        nextRecord: for (Map.Entry<Integer, String> record : xmlRecords.entrySet()) {
            DebugLog.debug("pars service record", record.getValue());
            ServiceRecordImpl sr = new ServiceRecordImpl(this, remoteDevice, record.getKey().intValue());
            Map<Integer, DataElement> elements;
            try {
                elements = BlueZServiceRecordXML.parsXMLRecord(record.getValue());
            } catch (IOException e) {
                DebugLog.error("Error parsing service record", e);
                continue nextRecord;
            }
            for (Map.Entry<Integer, DataElement> element : elements.entrySet()) {
                sr.populateAttributeValue(element.getKey().intValue(), element.getValue());
            }
            for (int u = 0; u < uuidSet.length; u++) {
                if (!((sr.hasServiceClassUUID(uuidSet[u])) || (sr.hasProtocolClassUUID(uuidSet[u])))) {
                    DebugLog.debug("ignoring service", sr);
                    continue nextRecord;
                }
            }
            DebugLog.debug("found service");
            sst.addServicesRecords(sr);
        }
        return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
    }

    @SuppressWarnings("unchecked")
    public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
            throws BluetoothStateException {
        DebugLog.debug("runSearchServices()");
        sst.searchServicesStartedCallback();

        int respCode = getRemoteServices(sst, uuidSet, device);
        DebugLog.debug("SearchServices finished", sst.getTransID());
        Vector<ServiceRecord> records = sst.getServicesRecords();
        if (records.size() != 0) {
            ServiceRecord[] servRecordArray = (ServiceRecord[]) Utils.vector2toArray(records, new ServiceRecord[records.size()]);
            listener.servicesDiscovered(sst.getTransID(), servRecordArray);
        }
        if ((respCode != DiscoveryListener.SERVICE_SEARCH_ERROR) && (sst.isTerminated())) {
            return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
        } else if (respCode == DiscoveryListener.SERVICE_SEARCH_COMPLETED) {
            if (records.size() != 0) {
                return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
            } else {
                return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
            }
        } else {
            return respCode;
        }
    }

    public boolean cancelServiceSearch(int transID) {
        DebugLog.debug("cancelServiceSearch()");
        SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
        if (sst != null) {
            return sst.setTerminated();
        } else {
            return false;
        }
    }

    public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
        DebugLog.debug("populateServicesRecordAttributeValues()");
        long remoteDeviceAddress = RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
        throw new UnsupportedOperationException("populateServicesRecordAttributeValues Not supported yet.");
    }

    // --- SDP Server

    private synchronized void registerSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
        long handle;
        try {
            handle = blueZ.registerSDPRecord(BlueZServiceRecordXML.exportXMLRecord(serviceRecord));
        } catch (Throwable e) {
            throw (ServiceRegistrationException) UtilsJavaSE.initCause(new ServiceRegistrationException(e.getMessage()), e);
        }
        serviceRecord.setHandle(handle);
        serviceRecord.populateAttributeValue(BluetoothConsts.ServiceRecordHandle, new DataElement(DataElement.U_INT_4, handle));
    }

    private synchronized void updateSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
        try {
            blueZ.updateSDPRecord(serviceRecord.getHandle(), BlueZServiceRecordXML.exportXMLRecord(serviceRecord));
        } catch (Throwable e) {
            throw (ServiceRegistrationException) UtilsJavaSE.initCause(new ServiceRegistrationException(e.getMessage()), e);
        }
    }

    private synchronized void unregisterSDPRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
        try {
            blueZ.unregisterSDPRecord(serviceRecord.getHandle());
        } catch (Throwable e) {
            throw (ServiceRegistrationException) UtilsJavaSE.initCause(new ServiceRegistrationException(e.getMessage()), e);
        }
    }

    // --- Client RFCOMM connections

    private native long connectionRfOpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel, boolean authenticate, boolean encrypt,
            int timeout) throws IOException;

    public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
        return connectionRfOpenClientConnectionImpl(this.localDeviceBTAddress, params.address, params.channel, params.authenticate, params.encrypt,
                params.timeout);
    }

    public native void connectionRfCloseClientConnection(long handle) throws IOException;

    public native int rfGetSecurityOptImpl(long handle) throws IOException;

    public int rfGetSecurityOpt(long handle, int expected) throws IOException {
        return rfGetSecurityOptImpl(handle);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
     */
    public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
        // TODO
        return false;
    }

    private native long rfServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate, boolean encrypt, boolean master, boolean timeouts,
            int backlog) throws IOException;

    private native int rfServerGetChannelIDImpl(long handle) throws IOException;

    public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
        long socket = rfServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate, params.encrypt, params.master, params.timeouts,
                LISTEN_BACKLOG_RFCOMM);
        boolean success = false;
        try {
            int channel = rfServerGetChannelIDImpl(socket);
            serviceRecord.populateRFCOMMAttributes(0, channel, params.uuid, params.name, params.obex);
            registerSDPRecord(serviceRecord);
            success = true;
            return socket;
        } finally {
            if (!success) {
                rfServerCloseImpl(socket, true);
            }
        }
    }

    private native void rfServerCloseImpl(long handle, boolean quietly) throws IOException;

    public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
        try {
            unregisterSDPRecord(serviceRecord);
        } finally {
            rfServerCloseImpl(handle, false);
        }
    }

    public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        updateSDPRecord(serviceRecord);
    }

    public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

    public void connectionRfCloseServerConnection(long clientHandle) throws IOException {
        connectionRfCloseClientConnection(clientHandle);
    }

    // --- Shared Client and Server RFCOMM connections

    public int connectionRfRead(long handle) throws IOException {
        byte[] data = new byte[1];
        int size = connectionRfRead(handle, data, 0, 1);
        if (size == -1) {
            return -1;
        }
        return 0xFF & data[0];
    }

    public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

    public native int connectionRfReadAvailable(long handle) throws IOException;

    public native void connectionRfWrite(long handle, int b) throws IOException;

    public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

    public native void connectionRfFlush(long handle) throws IOException;

    public native long getConnectionRfRemoteAddress(long handle) throws IOException;

    // --- Client and Server L2CAP connections

    private void validateMTU(int receiveMTU, int transmitMTU) {
        if (receiveMTU > l2cap_receiveMTU_max) {
            throw new IllegalArgumentException("invalid ReceiveMTU value " + receiveMTU);
        }
    }

    private native long l2OpenClientConnectionImpl(long localDeviceBTAddress, long address, int channel, boolean authenticate, boolean encrypt, int receiveMTU,
            int transmitMTU, int timeout) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth
     * .BluetoothConnectionParams, int, int)
     */
    public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
        validateMTU(receiveMTU, transmitMTU);
        return l2OpenClientConnectionImpl(this.localDeviceBTAddress, params.address, params.channel, params.authenticate, params.encrypt, receiveMTU,
                transmitMTU, params.timeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
     */
    public native void l2CloseClientConnection(long handle) throws IOException;

    private native long l2ServerOpenImpl(long localDeviceBTAddress, boolean authorize, boolean authenticate, boolean encrypt, boolean master, boolean timeouts,
            int backlog, int receiveMTU, int transmitMTU, int assignPsm) throws IOException;

    public native int l2ServerGetPSMImpl(long handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @seecom.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.
     * BluetoothConnectionNotifierParams, int, int, com.intel.bluetooth.ServiceRecordImpl)
     */
    public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException {
        validateMTU(receiveMTU, transmitMTU);
        long socket = l2ServerOpenImpl(this.localDeviceBTAddress, params.authorize, params.authenticate, params.encrypt, params.master, params.timeouts,
                LISTEN_BACKLOG_L2CAP, receiveMTU, transmitMTU, params.bluecove_ext_psm);
        boolean success = false;
        try {
            int channel = l2ServerGetPSMImpl(socket);
            serviceRecord.populateL2CAPAttributes(0, channel, params.uuid, params.name);
            registerSDPRecord(serviceRecord);
            success = true;
            return socket;
        } finally {
            if (!success) {
                l2ServerCloseImpl(socket, true);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long,
     * com.intel.bluetooth.ServiceRecordImpl, boolean)
     */
    public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        updateSDPRecord(serviceRecord);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection
     * (long)
     */
    public native long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
     */
    public void l2CloseServerConnection(long handle) throws IOException {
        l2CloseClientConnection(handle);
    }

    private native void l2ServerCloseImpl(long handle, boolean quietly) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long,
     * com.intel.bluetooth.ServiceRecordImpl)
     */
    public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
        try {
            unregisterSDPRecord(serviceRecord);
        } finally {
            l2ServerCloseImpl(handle, false);
        }
    }

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
    public native void l2Send(long handle, byte[] data, int transmitMTU) throws IOException;

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
     * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
     */
    public native long l2RemoteAddress(long handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2GetSecurityOpt(long, int)
     */
    public native int l2GetSecurityOpt(long handle, int expected) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
     */
    public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
        // TODO
        return false;
    }

}