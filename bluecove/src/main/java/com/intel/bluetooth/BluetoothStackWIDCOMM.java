/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

class BluetoothStackWIDCOMM implements BluetoothStack {

    private static BluetoothStackWIDCOMM singleInstance = null;

    private boolean initialized = false;

    private Vector deviceDiscoveryListeners = new Vector/* <DiscoveryListener> */();

    private Hashtable deviceDiscoveryListenerFoundDevices = new Hashtable();

    private Hashtable deviceDiscoveryListenerReportedDevices = new Hashtable();

    // TODO what is the real number for Attributes retrievable ?
    private final static int ATTR_RETRIEVABLE_MAX = 256;

    private final static int RECEIVE_MTU_MAX = 1024;

    // from WIDCOMM BtIfDefinitions.h
    final static short NULL_DESC_TYPE = 0;

    final static short UINT_DESC_TYPE = 1;

    final static short TWO_COMP_INT_DESC_TYPE = 2;

    final static short UUID_DESC_TYPE = 3;

    final static short TEXT_STR_DESC_TYPE = 4;

    final static short BOOLEAN_DESC_TYPE = 5;

    final static short DATA_ELE_SEQ_DESC_TYPE = 6;

    final static short DATA_ELE_ALT_DESC_TYPE = 7;

    final static short URL_DESC_TYPE = 8;

    BluetoothStackWIDCOMM() {
    }

    public String getStackID() {
        return BlueCoveImpl.STACK_WIDCOMM;
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
        return FEATURE_SERVICE_ATTRIBUTES | FEATURE_L2CAP;
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
        return LibraryInformation.library(BlueCoveImpl.NATIVE_LIB_WIDCOMM);
    }

    public native int getLibraryVersion();

    public native int detectBluetoothStack();

    public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

    public void initialize() throws BluetoothStateException {
        if (singleInstance != null) {
            throw new BluetoothStateException("Only one instance of " + getStackID() + " stack supported");
        }
        if (!initializeImpl()) {
            throw new RuntimeException("WIDCOMM BluetoothStack not found");
        }
        initialized = true;
        singleInstance = this;
    }

    public native boolean initializeImpl();

    private native void uninitialize();

    public void destroy() {
        if (singleInstance != this) {
            throw new RuntimeException("Destroy invalid instance");
        }
        if (initialized) {
            uninitialize();
            initialized = false;
            DebugLog.debug("WIDCOMM destroyed");
        }
        singleInstance = null;
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

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#setLocalDeviceServiceClasses(int)
     */
    public void setLocalDeviceServiceClasses(int classOfDevice) {
        throw new NotSupportedRuntimeException(getStackID());
    }

    /**
     * There are no functions to set WIDCOMM stack discoverable status.
     * 
     * @return <code>true</code> if the request succeeded, otherwise
     *         <code>false</code> if the request failed because the BCC denied
     *         the request; <code>false</code> if the Bluetooth system does not
     *         support the access mode specified in <code>mode</code>
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

    private synchronized void verifyDeviceReady() throws BluetoothStateException {
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
        if (BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX.equals(property)) {
            return "7";
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX.equals(property)) {
            return "1";
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
            return BlueCoveImpl.FALSE;
        }
        if (BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX.equals(property)) {
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
            long address = RemoteDeviceHelper.getAddress(functionDescr.substring(function.length() + 1, functionDescr.length()));
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.intel.bluetooth.BluetoothStack#isCurrentThreadInterruptedCallback()
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

    // ---------------------- Remote Device authentication

    public boolean authenticateRemoteDevice(long address) throws IOException {
        return false;
    }

    private native boolean authenticateRemoteDeviceImpl(long address, String passkey) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#authenticateRemoteDevice(long,
     * java.lang.String)
     */
    public boolean authenticateRemoteDevice(long address, String passkey) throws IOException {
        return authenticateRemoteDeviceImpl(address, passkey);
    }

    private native void removeAuthenticationWithRemoteDeviceImpl(long address) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.intel.bluetooth.BluetoothStack#removeAuthenticationWithRemoteDevice
     * (long)
     */
    public void removeAuthenticationWithRemoteDevice(long address) throws IOException {
        removeAuthenticationWithRemoteDeviceImpl(address);
    }

    // --- Some testing functions accessible by LocalDevice.getProperty

    private native boolean isRemoteDeviceConnected(long address);

    private native String getRemoteDeviceLinkMode(long address);

    private native String getRemoteDeviceVersionInfo(long address);

    private native boolean setSniffMode(long address);

    private native boolean cancelSniffMode(long address);

    private native int getRemoteDeviceRSSI(long address);

    // --- Device Inquiry

    private native int runDeviceInquiryImpl(DeviceInquiryRunnable inquiryRunnable, DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
            throws BluetoothStateException;

    public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
        deviceDiscoveryListeners.addElement(listener);
        if (BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_INQUIRY_REPORT_ASAP, false)) {
            deviceDiscoveryListenerFoundDevices.put(listener, new Hashtable());
        }
        deviceDiscoveryListenerReportedDevices.put(listener, new Vector());
        DeviceInquiryRunnable inquiryRunnable = new DeviceInquiryRunnable() {
            public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
                try {
                    int discType = runDeviceInquiryImpl(this, startedNotify, accessCode, listener);
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

            /*
             * This function may trigger multiple times per inquiry - even
             * multiple times per device - once for the address alone, and once
             * for the address and the user-friendly name.
             */
            public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName, boolean paired) {
                DebugLog.debug("deviceDiscoveredCallback deviceName", deviceName);
                if (!deviceDiscoveryListeners.contains(listener)) {
                    return;
                }
                // Update name if name retrieved
                RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(BluetoothStackWIDCOMM.this, deviceAddr, deviceName, paired);
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

    native String getRemoteDeviceFriendlyName(long address, int majorDeviceClass, int minorDeviceClass) throws IOException;

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

    private native long[] runSearchServicesImpl(SearchServicesThread startedNotify, byte[] uuidValue, long address) throws BluetoothStateException,
            SearchServicesTerminatedException;

    public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {

        SearchServicesRunnable searchRunnable = new SearchServicesRunnable() {

            public int runSearchServices(SearchServicesThread sst, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
                    throws BluetoothStateException {
                // Retrieve all Records, Filter here in Java
                synchronized (BluetoothStackWIDCOMM.class) {
                    byte[] uuidValue = Utils.UUIDToByteArray(BluetoothConsts.L2CAP_PROTOCOL_UUID);
                    for (int u = 0; u < uuidSet.length; u++) {
                        if (uuidSet[u].equals(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
                            continue;
                        } else if (uuidSet[u].equals(BluetoothConsts.RFCOMM_PROTOCOL_UUID)) {
                            uuidValue = Utils.UUIDToByteArray(uuidSet[u]);
                            continue;
                        } else {
                            // Look for the most specific UUID
                            uuidValue = Utils.UUIDToByteArray(uuidSet[u]);
                            break;
                        }
                    }
                    long[] handles;
                    try {
                        handles = runSearchServicesImpl(sst, uuidValue, RemoteDeviceHelper.getAddress(device));
                    } catch (SearchServicesTerminatedException e) {
                        DebugLog.debug("SERVICE_SEARCH_TERMINATED");
                        return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
                    }
                    if (handles == null) {
                        DebugLog.debug("SERVICE_SEARCH_ERROR");
                        return DiscoveryListener.SERVICE_SEARCH_ERROR;
                    } else if (handles.length > 0) {
                        Vector records = new Vector();
                        int[] uuidFilerAttrIDs = new int[] { BluetoothConsts.ServiceClassIDList, BluetoothConsts.ProtocolDescriptorList };
                        int[] requiredAttrIDs = new int[] { BluetoothConsts.ServiceRecordHandle, BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID };
                        nextRecord: for (int i = 0; i < handles.length; i++) {
                            ServiceRecordImpl sr = new ServiceRecordImpl(BluetoothStackWIDCOMM.this, device, handles[i]);
                            try {
                                sr.populateRecord(uuidFilerAttrIDs);
                                // Apply JSR-82 filter, all UUID should be present
                                for (int u = 0; u < uuidSet.length; u++) {
                                    if (!((sr.hasServiceClassUUID(uuidSet[u]) || sr.hasProtocolClassUUID(uuidSet[u])))) {
                                        if (BluetoothStackWIDCOMMSDPInputStream.debug) {
                                            DebugLog.debug("filtered ServiceRecord (" + i + ")", sr);
                                        }
                                        continue nextRecord;
                                    }
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
                            if (sst.isTerminated()) {
                                DebugLog.debug("SERVICE_SEARCH_TERMINATED");
                                return DiscoveryListener.SERVICE_SEARCH_TERMINATED;
                            }
                        }
                        if (records.size() != 0) {
                            DebugLog.debug("SERVICE_SEARCH_COMPLETED");
                            ServiceRecord[] fileteredRecords = (ServiceRecord[]) Utils.vector2toArray(records, new ServiceRecord[records.size()]);
                            listener.servicesDiscovered(sst.getTransID(), fileteredRecords);
                            return DiscoveryListener.SERVICE_SEARCH_COMPLETED;
                        }
                    }
                    DebugLog.debug("SERVICE_SEARCH_NO_RECORDS");
                    return DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;
                }
            }
        };
        return SearchServicesThread.startSearchServices(this, searchRunnable, attrSet, uuidSet, device, listener);
    }

    /**
     * Only one concurrent ServiceSearch supported
     */
    private native void cancelServiceSearchImpl();

    public boolean cancelServiceSearch(int transID) {
        SearchServicesThread sst = SearchServicesThread.getServiceSearchThread(transID);
        if (sst != null) {
            synchronized (this) {
                if (!sst.isTerminated()) {
                    sst.setTerminated();
                    cancelServiceSearchImpl();
                    return true;
                }
            }
        }
        return false;
    }

    private native byte[] getServiceAttribute(int attrID, long handle) throws IOException;

    private native boolean isServiceRecordDiscoverable(long address, long handle) throws IOException;

    public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
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
                    DataElement element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(sdpStruct))).readElement();

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

    private native long connectionRfOpenClientConnectionImpl(long address, int channel, boolean authenticate, boolean encrypt, int timeout) throws IOException;

    public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
        verifyDeviceReady();
        return connectionRfOpenClientConnectionImpl(params.address, params.channel, params.authenticate, params.encrypt, params.timeout);
    }

    private native void closeRfCommPortImpl(long handle) throws IOException;

    public void connectionRfCloseClientConnection(long handle) throws IOException {
        closeRfCommPortImpl(handle);
    }

    public native long getConnectionRfRemoteAddress(long handle) throws IOException;

    public native int connectionRfRead(long handle) throws IOException;

    public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

    public native int connectionRfReadAvailable(long handle) throws IOException;

    private native void connectionRfWriteImpl(long handle, byte[] b, int off, int len) throws IOException;

    public void connectionRfWrite(long handle, int b) throws IOException {
        byte buf[] = new byte[1];
        buf[0] = (byte) (b & 0xFF);
        connectionRfWriteImpl(handle, buf, 0, 1);
    }

    public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
        // Max in WIDCOMM is 64K that will cause ACCESS_VIOLATION.
        final int maxNativeBuffer = 0x10000 - 1;
        if (len < maxNativeBuffer) {
            connectionRfWriteImpl(handle, b, off, len);
        } else {
            int done = 0;
            while (done < len) {
                int l = len - done;
                if (l > maxNativeBuffer) {
                    l = maxNativeBuffer;
                }
                connectionRfWriteImpl(handle, b, off + done, l);
                done += maxNativeBuffer;
            }
        }
    }

    public void connectionRfFlush(long handle) throws IOException {
        // TODO are there any flush
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

    private native synchronized long rfServerOpenImpl(byte[] uuidValue, byte[] uuidValue2, boolean obexSrv, String name, boolean authenticate, boolean encrypt)
            throws IOException;

    private native int rfServerSCN(long handle) throws IOException;

    public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord) throws IOException {
        verifyDeviceReady();
        byte[] uuidValue = Utils.UUIDToByteArray(params.uuid);
        byte[] uuidValue2 = params.obex ? null : Utils.UUIDToByteArray(BluetoothConsts.SERIAL_PORT_UUID);
        long handle = rfServerOpenImpl(uuidValue, uuidValue2, params.obex, params.name, params.authenticate, params.encrypt);
        int channel = rfServerSCN(handle);
        DebugLog.debug("serverSCN", channel);
        long serviceRecordHandle = handle;

        serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, params.uuid, params.name, params.obex);

        return handle;
    }

    private native void sdpServiceAddAttribute(long handle, char handleType, int attrID, short attrType, byte[] value) throws ServiceRegistrationException;

    private byte[] long2byte(long value, int len) {
        byte[] cvalue = new byte[len];
        long l = value;
        for (int i = len - 1; i >= 0; i--) {
            cvalue[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return cvalue;
    }

    public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        sdpServiceUpdateServiceRecord(handle, 'r', serviceRecord);
    }

    private byte[] sdpServiceSequenceAttribute(Enumeration en) throws ServiceRegistrationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SDPOutputStream sdpOut = new SDPOutputStream(out);
        try {
            while (en.hasMoreElements()) {
                sdpOut.writeElement((DataElement) en.nextElement());
            }
        } catch (IOException e) {
            throw new ServiceRegistrationException(e.getMessage());
        }
        return out.toByteArray();
    }

    private native void sdpServiceAddServiceClassIdList(long handle, char handleType, byte[][] uuidValues) throws ServiceRegistrationException;

    private void sdpServiceUpdateServiceRecord(long handle, char handleType, ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
        int[] ids = serviceRecord.getAttributeIDs();
        if ((ids == null) || (ids.length == 0)) {
            return;
        }
        // Update the records that can be update only once
        DataElement serviceClassIDList = serviceRecord.getAttributeValue(BluetoothConsts.ServiceClassIDList);
        if (serviceClassIDList.getDataType() != DataElement.DATSEQ) {
            throw new ServiceRegistrationException("Invalid serviceClassIDList");
        }
        Enumeration en = (Enumeration) serviceClassIDList.getValue();
        Vector uuids = new Vector();
        while (en.hasMoreElements()) {
            DataElement u = (DataElement) en.nextElement();
            if (u.getDataType() != DataElement.UUID) {
                throw new ServiceRegistrationException("Invalid serviceClassIDList element " + u);
            }
            uuids.add(u.getValue());
        }
        if (uuids.size() > 0) {
            byte[][] uuidValues = new byte[uuids.size()][];
            for (int u = 0; u < uuidValues.length; u++) {
                uuidValues[u] = Utils.UUIDToByteArray((UUID) uuids.elementAt(u));
            }
            sdpServiceAddServiceClassIdList(handle, handleType, uuidValues);
        }

        // Update all other records
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
                sdpServiceAddAttribute(handle, handleType, id, URL_DESC_TYPE, Utils.getASCIIBytes(d.getValue().toString()));
                break;
            case DataElement.STRING:
                sdpServiceAddAttribute(handle, handleType, id, TEXT_STR_DESC_TYPE, Utils.getUTF8Bytes(d.getValue().toString()));
                break;
            case DataElement.NULL:
                sdpServiceAddAttribute(handle, handleType, id, NULL_DESC_TYPE, null);
                break;
            case DataElement.BOOL:
                sdpServiceAddAttribute(handle, handleType, id, BOOLEAN_DESC_TYPE, new byte[] { (byte) (d.getBoolean() ? 1 : 0) });
                break;
            case DataElement.UUID:
                sdpServiceAddAttribute(handle, handleType, id, UUID_DESC_TYPE, BluetoothStackWIDCOMMSDPInputStream.getUUIDHexBytes((UUID) d.getValue()));
                break;
            case DataElement.DATSEQ:
                sdpServiceAddAttribute(handle, handleType, id, DATA_ELE_SEQ_DESC_TYPE, sdpServiceSequenceAttribute((Enumeration) d.getValue()));
                break;
            case DataElement.DATALT:
                sdpServiceAddAttribute(handle, handleType, id, DATA_ELE_ALT_DESC_TYPE, sdpServiceSequenceAttribute((Enumeration) d.getValue()));
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

    private native long l2OpenClientConnectionImpl(long address, int channel, boolean authenticate, boolean encrypt, int receiveMTU, int transmitMTU,
            int timeout) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth
     * .BluetoothConnectionParams, int, int)
     */
    public long l2OpenClientConnection(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
        verifyDeviceReady();
        validateMTU(receiveMTU, transmitMTU);
        return l2OpenClientConnectionImpl(params.address, params.channel, params.authenticate, params.encrypt, receiveMTU, transmitMTU, params.timeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
     */
    public native void l2CloseClientConnection(long handle) throws IOException;

    private native synchronized long l2ServerOpenImpl(byte[] uuidValue, boolean authenticate, boolean encrypt, String name, int receiveMTU, int transmitMTU,
            int assignPsm) throws IOException;

    public native int l2ServerPSM(long handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @seecom.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.
     * BluetoothConnectionNotifierParams, int, int,
     * com.intel.bluetooth.ServiceRecordImpl)
     */
    public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU, ServiceRecordImpl serviceRecord) throws IOException {
        verifyDeviceReady();
        validateMTU(receiveMTU, transmitMTU);
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
        sdpServiceUpdateServiceRecord(handle, 'l', serviceRecord);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection
     * (long)
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
    public int l2GetSecurityOpt(long handle, int expected) throws IOException {
        return expected;
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

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2Encrypt(long,long,boolean)
     */
    public boolean l2Encrypt(long address, long handle, boolean on) throws IOException {
        return false;
    }
}
