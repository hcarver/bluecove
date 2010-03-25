/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Eric Wagner
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;

class BluetoothStackOSX implements BluetoothStack, BluetoothStackExtension {

    public static final boolean debug = false;

    private static BluetoothStackOSX singleInstance = null;

    // TODO what is the real number for Attributes retrivable ?
    private final static int ATTR_RETRIEVABLE_MAX = 256;

    private final Vector deviceDiscoveryListeners = new Vector/* <DiscoveryListener> */();

    private final Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();

    private int receive_mtu_max = -1;

    private int localDeviceSupportedSoftwareVersion;

    private long lastDeviceDiscoveryTime = 0;

    private int localDeviceServiceClasses = 0;

    private Thread localDeviceServiceClassMaintainer = null;

    private static final int BLUETOOTH_SOFTWARE_VERSION_2_0_0 = 20000;

    BluetoothStackOSX() {

    }

    // ---------------------- Library initialization

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
        return LibraryInformation.library(BlueCoveImpl.NATIVE_LIB_OSX);
    }

    public String getStackID() {
        return BlueCoveImpl.STACK_OSX;
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
        if (localDeviceSupportedSoftwareVersion >= BLUETOOTH_SOFTWARE_VERSION_2_0_0) {
            return FEATURE_L2CAP | FEATURE_SERVICE_ATTRIBUTES | FEATURE_SET_DEVICE_SERVICE_CLASSES | (isLocalDeviceFeatureRSSI() ? FEATURE_RSSI : 0);
        } else {
            return FEATURE_L2CAP | FEATURE_SERVICE_ATTRIBUTES;
        }
    }

    public native int getLibraryVersion();

    public native int detectBluetoothStack();

    private native boolean initializeImpl();

    public void initialize() throws BluetoothStateException {
        if (singleInstance != null) {
            throw new BluetoothStateException("Only one instance of " + getStackID() + " stack supported");
        }

        String sysVersion = System.getProperty("os.version");
        String jreDataModel = System.getProperty("sun.arch.data.model");
        boolean osIsLeopard = (sysVersion != null) && sysVersion.startsWith("10.5");
        boolean jreIs64Bit = "64".equals(jreDataModel);
        if (osIsLeopard && jreIs64Bit) {
            throw new BluetoothStateException("Mac OS X 10.5 not supported with a 64 bit JRE");
        }

        localDeviceSupportedSoftwareVersion = getLocalDeviceSupportedSoftwareVersion();
        DebugLog.debug("localDeviceSupportedSoftwareVersion", localDeviceSupportedSoftwareVersion);
        if (!initializeImpl()) {
            throw new BluetoothStateException("OS X BluetoothStack not found");
        }
        singleInstance = this;
    }

    public void destroy() {
        if (localDeviceSupportedSoftwareVersion >= BLUETOOTH_SOFTWARE_VERSION_2_0_0) {
            setLocalDeviceServiceClassesImpl(0);
        }
        singleInstance = null;
    }

    public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
     */
    public boolean isCurrentThreadInterruptedCallback() {
        return UtilsJavaSE.isCurrentThreadInterrupted();
    }

    // ---------------------- LocalDevice

    public native String getLocalDeviceBluetoothAddress() throws BluetoothStateException;

    public native String getLocalDeviceName();

    private native int getDeviceClassImpl();

    public DeviceClass getLocalDeviceClass() {
        return new DeviceClass(getDeviceClassImpl());
    }

    private native boolean setLocalDeviceServiceClassesImpl(int classOfDevice);

    private class MaintainDeviceServiceClassesThread extends Thread {

        MaintainDeviceServiceClassesThread() {
            super("MaintainDeviceServiceClassesThread");
        }

        public void run() {
            boolean updated = true;
            while (true) {
                try {
                    int delay = 1000 * 120;
                    if (!updated) {
                        delay = 1000;
                    }
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
                if (localDeviceServiceClasses != 0) {
                    updated = setLocalDeviceServiceClassesImpl(localDeviceServiceClasses);
                } else if (!updated) {
                    updated = true;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
     */
    public synchronized void setLocalDeviceServiceClasses(int classOfDevice) {
        if (localDeviceSupportedSoftwareVersion < BLUETOOTH_SOFTWARE_VERSION_2_0_0) {
            return;
        }
        if (classOfDevice != localDeviceServiceClasses) {
            setLocalDeviceServiceClassesImpl(classOfDevice);
        }
        localDeviceServiceClasses = classOfDevice;
        if ((classOfDevice != 0) && (localDeviceServiceClassMaintainer == null)) {
            localDeviceServiceClassMaintainer = new MaintainDeviceServiceClassesThread();
            UtilsJavaSE.threadSetDaemon(localDeviceServiceClassMaintainer);
            localDeviceServiceClassMaintainer.start();
        }
    }

    public native boolean isLocalDevicePowerOn();

    private native boolean isLocalDeviceFeatureSwitchRoles();

    private native boolean isLocalDeviceFeatureParkMode();

    private native boolean isLocalDeviceFeatureRSSI();

    private native int getLocalDeviceL2CAPMTUMaximum();

    private native int getLocalDeviceSupportedSoftwareVersion();

    private native String getLocalDeviceSoftwareVersionInfo();

    private native int getLocalDeviceManufacturer();

    private native String getLocalDeviceVersion();

    public String getLocalDeviceProperty(String property) {
        if (BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX.equals(property)) {
            return isLocalDeviceFeatureParkMode() ? "255" : "7";
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX.equals(property)) {
            return "7";
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY_SCAN.equals(property)) {
            return BlueCoveImpl.TRUE;
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE_SCAN.equals(property)) {
            return BlueCoveImpl.TRUE;
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY.equals(property)) {
            return BlueCoveImpl.TRUE;
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE.equals(property)) {
            return BlueCoveImpl.TRUE;
        }

        if (BluetoothConsts.PROPERTY_BLUETOOTH_SD_ATTR_RETRIEVABLE_MAX.equals(property)) {
            return String.valueOf(ATTR_RETRIEVABLE_MAX);
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_MASTER_SWITCH.equals(property)) {
            // return isLocalDeviceFeatureSwitchRoles() ? TRUE : FALSE;
            return BlueCoveImpl.FALSE;
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX.equals(property)) {
            return String.valueOf(receiveMTUMAX());
        }

        if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_RADIO_VERSION.equals(property)) {
            return getLocalDeviceVersion();
        }
        if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_RADIO_MANUFACTURER.equals(property)) {
            return String.valueOf(getLocalDeviceManufacturer());
        }
        if (BlueCoveLocalDeviceProperties.LOCAL_DEVICE_PROPERTY_STACK_VERSION.equals(property)) {
            return getLocalDeviceSoftwareVersionInfo();
        }

        return null;
    }

    private int receiveMTUMAX() {
        if (receive_mtu_max < 0) {
            receive_mtu_max = getLocalDeviceL2CAPMTUMaximum();
        }
        return receive_mtu_max;
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

    private void verifyDeviceReady() throws BluetoothStateException {
        if (!isLocalDevicePowerOn()) {
            throw new BluetoothStateException("Bluetooth Device is not ready");
        }
    }

    private native boolean retrieveDevicesImpl(int option, RetrieveDevicesCallback retrieveDevicesCallback);

    public RemoteDevice[] retrieveDevices(int option) {
        final Vector devices = new Vector();
        RetrieveDevicesCallback retrieveDevicesCallback = new RetrieveDevicesCallback() {
            public void deviceFoundCallback(long deviceAddr, int deviceClass, String deviceName, boolean paired) {
                DebugLog.debug("device found", deviceAddr);
                RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackOSX.this, deviceAddr, deviceName, paired);
                if (!devices.contains(remoteDevice)) {
                    devices.add(remoteDevice);
                }
            }
        };
        if (retrieveDevicesImpl(option, retrieveDevicesCallback)) {
            return RemoteDeviceHelper.remoteDeviceListToArray(devices);
        } else {
            return null;
        }
    }

    private native boolean isRemoteDeviceTrustedImpl(long address);

    public Boolean isRemoteDeviceTrusted(long address) {
        return new Boolean(isRemoteDeviceTrustedImpl(address));
    }

    private native boolean isRemoteDeviceAuthenticatedImpl(long address);

    public Boolean isRemoteDeviceAuthenticated(long address) {
        return new Boolean(isRemoteDeviceAuthenticatedImpl(address));
    }

    private native int readRemoteDeviceRSSIImpl(long address) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStackExtension#readRemoteDeviceRSSI(long)
     */
    public int readRemoteDeviceRSSI(long address) throws IOException {
        return readRemoteDeviceRSSIImpl(address);
    }

    // ---------------------- Remote Device authentication

    private native boolean authenticateRemoteDeviceImpl(long address) throws IOException;

    public boolean authenticateRemoteDevice(long address) throws IOException {
        return authenticateRemoteDeviceImpl(address);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#authenticateRemoteDevice(long,
     * java.lang.String)
     */
    public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice (long)
     */
    public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    // ---------------------- Device Inquiry

    public native String getRemoteDeviceFriendlyName(long address) throws IOException;

    private native int runDeviceInquiryImpl(DeviceInquiryRunnable inquiryRunnable, DeviceInquiryThread startedNotify, int accessCode, int duration,
            DiscoveryListener listener) throws BluetoothStateException;

    public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
        // Inquiries are throttled if they are called too quickly in succession.
        // e.g. JSR-82 TCK
        long sinceDiscoveryLast = System.currentTimeMillis() - lastDeviceDiscoveryTime;
        long acceptableInterval = 7 * 1000;
        if (sinceDiscoveryLast < acceptableInterval) {
            try {
                Thread.sleep(acceptableInterval - sinceDiscoveryLast);
            } catch (InterruptedException e) {
                throw new BluetoothStateException();
            }
        }

        deviceDiscoveryListeners.addElement(listener);
        deviceDiscoveryListenerReportedDevices.put(listener, new Vector());
        DeviceInquiryRunnable inquiryRunnable = new DeviceInquiryRunnable() {

            public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
                try {
                    return runDeviceInquiryImpl(this, startedNotify, accessCode, DeviceInquiryThread.getConfigDeviceInquiryDuration(), listener);
                } finally {
                    lastDeviceDiscoveryTime = System.currentTimeMillis();
                    deviceDiscoveryListeners.removeElement(listener);
                    deviceDiscoveryListenerReportedDevices.remove(listener);
                }
            }

            public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired) {
                if (!deviceDiscoveryListeners.contains(listener)) {
                    return;
                }
                // Update name if name retrieved
                RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackOSX.this, deviceAddr, deviceName, paired);
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
        };
        return DeviceInquiryThread.startInquiry(this, inquiryRunnable, accessCode, listener);
    }

    private native boolean deviceInquiryCancelImpl();

    public boolean cancelInquiry(DiscoveryListener listener) {
        // no further deviceDiscovered() events will occur for this inquiry
        if (!deviceDiscoveryListeners.removeElement(listener)) {
            return false;
        }
        return deviceInquiryCancelImpl();
    }

    // ---------------------- Service search

    private native int runSearchServicesImpl(long address, int transID) throws BluetoothStateException, SearchServicesException;

    public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {

        SearchServicesRunnable searchRunnable = new SearchServicesRunnable() {

            public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
                    throws BluetoothStateException {
                // OS X will retrieve all Records, we filter here in Java
                sst.searchServicesStartedCallback();
                int recordsSize;
                try {
                    recordsSize = runSearchServicesImpl(RemoteDeviceHelper.getAddress(device), sst.getTransID());
                } catch (SearchServicesDeviceNotReachableException e) {
                    return DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;
                } catch (SearchServicesTerminatedException e) {
                    return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
                } catch (SearchServicesException e) {
                    return DiscoveryListener.SERVICE_SEARCH_ERROR;
                }
                if (sst.isTerminated()) {
                    return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
                }
                if (recordsSize == 0) {
                    return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
                }
                Vector records = new Vector();
                int[] uuidFilerAttrIDs = new int[] { BluetoothConsts.ServiceClassIDList, BluetoothConsts.ProtocolDescriptorList };
                int[] requiredAttrIDs = new int[] { BluetoothConsts.ServiceRecordHandle, BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID };
                nextRecord: for (int i = 0; i < recordsSize; i++) {
                    ServiceRecordImpl sr = new ServiceRecordImpl(BluetoothStackOSX.this, device, i);
                    try {
                        sr.populateRecord(uuidFilerAttrIDs);
                        // Apply JSR-82 filter, all UUID should be present
                        for (int u = 0; u < uuidSet.length; u++) {
                            if (!((sr.hasServiceClassUUID(uuidSet[u]) || sr.hasProtocolClassUUID(uuidSet[u])))) {
                                if (debug) {
                                    DebugLog.debug("filtered ServiceRecord (" + i + ")", sr);
                                }
                                continue nextRecord;
                            }
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

                    if (sst.isTerminated()) {
                        DebugLog.debug("SERVICE_SEARCH_TERMINATED " + sst.getTransID());
                        return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
                    }
                }
                if (records.size() != 0) {
                    DebugLog.debug("SERVICE_SEARCH_COMPLETED " + sst.getTransID());
                    ServiceRecord[] fileteredRecords = (ServiceRecord[]) Utils.vector2toArray(records, new ServiceRecord[records.size()]);
                    listener.servicesDiscovered(sst.getTransID(), fileteredRecords);
                    return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
                } else {
                    return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
                }
            }

        };
        return SearchServicesThread.startSearchServices(this, searchRunnable, attrSet, uuidSet, device, listener);
    }

    private native void cancelServiceSearchImpl(int transID);

    public boolean cancelServiceSearch(int transID) {
        SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
        if (sst != null) {
            synchronized (this) {
                if (!sst.isTerminated()) {
                    sst.setTerminated();
                    cancelServiceSearchImpl(transID);
                    return true;
                }
            }
        }
        return false;
    }

    private native byte[] getServiceAttributeImpl(long address, long serviceRecordIndex, int attrID);

    public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
        if (attrIDs.length > ATTR_RETRIEVABLE_MAX) {
            throw new IllegalArgumentException();
        }
        boolean anyRetrived = false;
        long address = RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice());
        for (int i = 0; i < attrIDs.length; i++) {
            int id = attrIDs[i];
            try {
                byte[] blob = getServiceAttributeImpl(address, serviceRecord.getHandle(), id);
                if (blob != null) {
                    DataElement element = (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
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

    // ---------------------- Client RFCOMM connections

    private native long connectionRfOpenClientConnectionImpl(long address, int channel, boolean authenticate, boolean encrypt, int timeout) throws IOException;

    public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
        if (params.encrypt) {
            throw new BluetoothConnectionException(BluetoothConnectionException.SECURITY_BLOCK, "encrypt mode not supported");
        }
        Object lock = RemoteDeviceHelper.createRemoteDevice(this, params.address, null, false);
        synchronized (lock) {
            return connectionRfOpenClientConnectionImpl(params.address, params.channel, params.authenticate, params.encrypt, params.timeout);
        }
    }

    public native void connectionRfCloseClientConnection(long handle) throws IOException;

    public native int rfGetSecurityOpt(long handle, int expected) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
     */
    public boolean rfEncrypt(long address, long handle, boolean on) throws IOException {
        return false;
    }

    // ---------------------- Server RFCOMM connections

    private native long rfServerCreateImpl(byte[] uuidValue, boolean obexSrv, String name, boolean authenticate, boolean encrypt) throws IOException;

    private native int rfServerGetChannelID(long handle) throws IOException;

    private native void rfServerCloseImpl(long handle) throws IOException;

    public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
        verifyDeviceReady();
        if (params.encrypt) {
            throw new BluetoothConnectionException(BluetoothConnectionException.SECURITY_BLOCK, "encrypt mode not supported");
        }
        byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
        long handle = rfServerCreateImpl(uuidValue, params.obex, params.name, params.authenticate, params.encrypt);
        boolean success = false;
        try {
            int channel = rfServerGetChannelID(handle);
            serviceRecord.populateRFCOMMAttributes(handle, channel, params.uuid, params.name, params.obex);
            success = true;
        } finally {
            if (!success) {
                rfServerCloseImpl(handle);
            }
        }
        return handle;
    }

    public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
        rfServerCloseImpl(handle);
    }

    public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        sdpServiceUpdateServiceRecord(handle, 'R', serviceRecord);
    }

    public native long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException;

    public void connectionRfCloseServerConnection(long handle) throws IOException {
        connectionRfCloseClientConnection(handle);
    }

    private native void sdpServiceUpdateServiceRecordPublish(long handle, char handleType) throws ServiceRegistrationException;

    private native void sdpServiceAddAttribute(long handle, char handleType, int attrID, int attrType, long numberValue, byte[] arrayValue)
            throws ServiceRegistrationException;

    private native void sdpServiceSequenceAttributeStart(long handle, char handleType, int attrID, int attrType) throws ServiceRegistrationException;

    private native void sdpServiceSequenceAttributeEnd(long handle, char handleType, int attrID) throws ServiceRegistrationException;

    private void sdpServiceAddAttribute(long handle, char handleType, int attrID, DataElement element) throws ServiceRegistrationException {
        int type = element.getDataType();
        switch (type) {
        case DataElement.NULL:
            sdpServiceAddAttribute(handle, handleType, attrID, type, 0, null);
            break;
        case DataElement.BOOL:
            sdpServiceAddAttribute(handle, handleType, attrID, type, element.getBoolean() ? 1 : 0, null);
            break;
        case DataElement.U_INT_1:
        case DataElement.INT_1:
        case DataElement.U_INT_2:
        case DataElement.INT_2:
        case DataElement.U_INT_4:
        case DataElement.INT_4:
        case DataElement.INT_8:
            sdpServiceAddAttribute(handle, handleType, attrID, type, element.getLong(), null);
            break;
        case DataElement.U_INT_8:
        case DataElement.U_INT_16:
        case DataElement.INT_16:
            sdpServiceAddAttribute(handle, handleType, attrID, type, 0, (byte[]) element.getValue());
            break;
        case DataElement.UUID:
            sdpServiceAddAttribute(handle, handleType, attrID, type, 0, Utils.UUIDToByteArray((UUID) element.getValue()));
            break;
        case DataElement.STRING:
            byte[] bs = Utils.getUTF8Bytes((String) element.getValue());
            sdpServiceAddAttribute(handle, handleType, attrID, type, 0, bs);
            break;
        case DataElement.URL:
            byte[] bu = Utils.getASCIIBytes((String) element.getValue());
            sdpServiceAddAttribute(handle, handleType, attrID, type, 0, bu);
            break;
        case DataElement.DATSEQ:
        case DataElement.DATALT:
            sdpServiceSequenceAttributeStart(handle, handleType, attrID, type);
            for (Enumeration e = (Enumeration) element.getValue(); e.hasMoreElements();) {
                DataElement child = (DataElement) e.nextElement();
                sdpServiceAddAttribute(handle, handleType, -1, child);
            }
            sdpServiceSequenceAttributeEnd(handle, handleType, attrID);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    private void sdpServiceUpdateServiceRecord(long handle, char handleType, ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
        int[] ids = serviceRecord.getAttributeIDs();
        if ((ids == null) || (ids.length == 0)) {
            return;
        }
        for (int i = 0; i < ids.length; i++) {
            int attrID = ids[i];
            switch (attrID) {
            case BluetoothConsts.ServiceRecordHandle:
                continue;
            case BluetoothConsts.ProtocolDescriptorList:
            case BluetoothConsts.AttributeIDServiceName:
                continue;
            }
            sdpServiceAddAttribute(handle, handleType, attrID, serviceRecord.getAttributeValue(attrID));
        }
        sdpServiceUpdateServiceRecordPublish(handle, handleType);
    }

    // ---------------------- Shared Client and Server RFCOMM connections

    public void connectionRfFlush(long handle) throws IOException {
        // TODO Auto-generated method stub
    }

    public native int connectionRfRead(long handle) throws IOException;

    public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

    public native int connectionRfReadAvailable(long handle) throws IOException;

    public void connectionRfWrite(long handle, int b) throws IOException {
        byte buf[] = new byte[1];
        buf[0] = (byte) (b & 0xFF);
        connectionRfWrite(handle, buf, 0, 1);
    }

    public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

    public native long getConnectionRfRemoteAddress(long handle) throws IOException;

    // ---------------------- Client and Server L2CAP connections

    private void validateMTU(int receiveMTU, int transmitMTU) {
        if (receiveMTU > receiveMTUMAX()) {
            throw new IllegalArgumentException("invalid ReceiveMTU value " + receiveMTU);
        }
    }

    private native long l2OpenClientConnectionImpl(long address, int channel, boolean authenticate, boolean encrypt, int receiveMTU, int transmitMTU,
            int timeout) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth
     * .BluetoothConnectionParams, int, int)
     */
    public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
        validateMTU(receiveMTU, transmitMTU);
        if (params.encrypt) {
            throw new BluetoothConnectionException(BluetoothConnectionException.SECURITY_BLOCK, "encrypt mode not supported");
        }
        Object lock = RemoteDeviceHelper.createRemoteDevice(this, params.address, null, false);
        synchronized (lock) {
            return l2OpenClientConnectionImpl(params.address, params.channel, params.authenticate, params.encrypt, receiveMTU, transmitMTU, params.timeout);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
     */
    public native void l2CloseClientConnection(long handle) throws IOException;

    private native long l2ServerOpenImpl(byte[] uuidValue, boolean authenticate, boolean encrypt, String name, int receiveMTU, int transmitMTU, int assignPsm)
            throws IOException;

    public native int l2ServerPSM(long handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @seecom.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.
     * BluetoothConnectionNotifierParams, int, int, com.intel.bluetooth.ServiceRecordImpl)
     */
    public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException {
        verifyDeviceReady();
        validateMTU(receiveMTU, transmitMTU);
        if (params.encrypt) {
            throw new BluetoothConnectionException(BluetoothConnectionException.SECURITY_BLOCK, "encrypt mode not supported");
        }
        byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
        long handle = l2ServerOpenImpl(uuidValue, params.authenticate, params.encrypt, params.name, receiveMTU, transmitMTU, params.bluecove_ext_psm);

        int channel = l2ServerPSM(handle);

        int serviceRecordHandle = (int) handle;

        serviceRecord.populateL2CAPAttributes(serviceRecordHandle, channel, params.uuid, params.name);

        return handle;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long,
     * com.intel.bluetooth.ServiceRecordImpl, boolean)
     */
    public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        sdpServiceUpdateServiceRecord(handle, 'L', serviceRecord);
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

    private native void l2ServerCloseImpl(long handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long,
     * com.intel.bluetooth.ServiceRecordImpl)
     */
    public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
        l2ServerCloseImpl(handle);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2GetSecurityOpt(long, int)
     */
    public native int l2GetSecurityOpt(long handle, int expected) throws IOException;

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
     * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
     */
    public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
        return false;
    }
}
