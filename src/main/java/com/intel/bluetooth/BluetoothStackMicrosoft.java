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

class BluetoothStackMicrosoft implements BluetoothStack {

    private static final int BTH_MODE_POWER_OFF = 1;

    private static final int BTH_MODE_CONNECTABLE = 2;

    private static final int BTH_MODE_DISCOVERABLE = 3;

    boolean peerInitialized = false;

    long localBluetoothAddress = 0;

    private DiscoveryListener currentDeviceDiscoveryListener;

    private Thread limitedDiscoverableTimer;

    // TODO what is the real number for Attributes retrievable ?
    private final static int ATTR_RETRIEVABLE_MAX = 256;

    private final static boolean postponeDeviceDiscoveryReport = true;

    private Vector deviceDiscoveryReportedDevices = new Vector/* <ReportedDevice> */();

    private static class ReportedDevice {

        RemoteDevice remoteDevice;

        DeviceClass deviceClass;
    }

    static {
        NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_MS);
    }

    BluetoothStackMicrosoft() {
    }

    // ---------------------- Library initialization

    public String getStackID() {
        return BlueCoveImpl.STACK_WINSOCK;
    }

    public native int getLibraryVersion();

    public native int detectBluetoothStack();

    public native void enableNativeDebug(Class nativeDebugCallback, boolean on);

    static native int initializationStatus() throws IOException;

    native void uninitialize();

    public void initialize() {
        try {
            int status = initializationStatus();
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
            uninitialize();
        }
        cancelLimitedDiscoverableTimer();
    }

    public void initialized() throws BluetoothStateException {
        if (!peerInitialized) {
            throw new BluetoothStateException("Bluetooth system is unavailable");
        }
    }

    // ---------------------- LocalDevice

    private native int getDeviceClass(long address);

    private native void setDiscoverable(boolean on) throws BluetoothStateException;

    private native int getBluetoothRadioMode();

    private native String getradioname(long address);

    private native int getDeviceVersion(long address);

    private native int getDeviceManufacturer(long address);

    public String getLocalDeviceBluetoothAddress() {
        try {
            long socket = socket(false, false);
            bind(socket);
            localBluetoothAddress = getsockaddress(socket);
            String address = RemoteDeviceHelper.getBluetoothAddress(localBluetoothAddress);
            storesockopt(socket);
            close(socket);
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
        return getradioname(localBluetoothAddress);
    }

    public String getRemoteDeviceFriendlyName(long address) throws IOException {
        return getpeername(address);
    }

    public DeviceClass getLocalDeviceClass() {
        return new DeviceClass(getDeviceClass(localBluetoothAddress));
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
            setDiscoverable(false);
            break;
        case DiscoveryAgent.GIAC:
            cancelLimitedDiscoverableTimer();
            DebugLog.debug("setDiscoverable(true)");
            setDiscoverable(true);
            break;
        case DiscoveryAgent.LIAC:
            if (limitedDiscoverableTimer != null) {
                break;
            }
            DebugLog.debug("setDiscoverable(LIAC)");
            setDiscoverable(true);
            // Timer to turn it off
            limitedDiscoverableTimer = Utils.schedule(60 * 1000, new Runnable() {
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
        int mode = getBluetoothRadioMode();
        if (mode == BTH_MODE_POWER_OFF) {
            return false;
        }
        return ((mode == BTH_MODE_CONNECTABLE) || (mode == BTH_MODE_DISCOVERABLE));
    }

    public int getLocalDeviceDiscoverable() {
        int mode = getBluetoothRadioMode();
        if (mode == BTH_MODE_DISCOVERABLE) {
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
            return String.valueOf(getDeviceVersion(localBluetoothAddress));
        }
        if ("bluecove.radio.manufacturer".equals(property)) {
            return String.valueOf(getDeviceManufacturer(localBluetoothAddress));
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

    // ---------------------- Device Inquiry

    /**
     * This is called when all device discoved by stack. To avoid problems with getpeername we will postpone the calls
     * to User deviceDiscovered function until runDeviceInquiry is finished.
     */
    public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
            String deviceName, boolean paired) {
        RemoteDevice remoteDevice = RemoteDeviceHelper.createRemoteDevice(this, deviceAddr, deviceName, paired);
        if ((currentDeviceDiscoveryListener == null) || (currentDeviceDiscoveryListener != listener)) {
            return;
        }
        DeviceClass cod = new DeviceClass(deviceClass);
        DebugLog.debug("deviceDiscoveredCallback address", remoteDevice.getBluetoothAddress());
        DebugLog.debug("deviceDiscoveredCallback deviceClass", cod);
        if (postponeDeviceDiscoveryReport) {
            ReportedDevice rd = new ReportedDevice();
            rd.deviceClass = cod;
            rd.remoteDevice = remoteDevice;
            deviceDiscoveryReportedDevices.addElement(rd);
        } else {
            listener.deviceDiscovered(remoteDevice, cod);
        }
    }

    public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
        initialized();
        if (currentDeviceDiscoveryListener != null) {
            throw new BluetoothStateException("Another inquiry already running");
        }
        currentDeviceDiscoveryListener = listener;
        return DeviceInquiryThread.startInquiry(this, accessCode, listener);
    }

    /*
     * cancel current inquiry (if any)
     */
    private native boolean cancelInquiry();

    public boolean cancelInquiry(DiscoveryListener listener) {
        if (currentDeviceDiscoveryListener != listener) {
            return false;
        }
        // no further deviceDiscovered() events will occur for this inquiry
        currentDeviceDiscoveryListener = null;
        return cancelInquiry();
    }

    /*
     * perform synchronous inquiry
     */
    private native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode,
            DiscoveryListener listener) throws BluetoothStateException;

    public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
            throws BluetoothStateException {
        try {
            if (postponeDeviceDiscoveryReport) {
                deviceDiscoveryReportedDevices.removeAllElements();
            }
            int discType = runDeviceInquiryImpl(startedNotify, accessCode, listener);
            if ((discType == DiscoveryListener.INQUIRY_COMPLETED) && (postponeDeviceDiscoveryReport)) {
                for (Enumeration en = deviceDiscoveryReportedDevices.elements(); en.hasMoreElements();) {
                    ReportedDevice rd = (ReportedDevice) en.nextElement();
                    listener.deviceDiscovered(rd.remoteDevice, rd.deviceClass);
                }
            }
            return discType;
        } finally {
            currentDeviceDiscoveryListener = null;
        }
    }

    // ---------------------- Service search

    /*
     * perform synchronous service discovery
     */
    public native int[] runSearchServices(UUID[] uuidSet, long address) throws SearchServicesException;

    /*
     * get service attributes
     */
    public native byte[] getServiceAttributes(int[] attrIDs, long address, int handle) throws IOException;

    public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener)
            throws BluetoothStateException {
        return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
    }

    public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet,
            RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
        startedNotify.searchServicesStartedCallback();
        int[] handles;
        try {
            handles = runSearchServices(uuidSet, RemoteDeviceHelper.getAddress(device));
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
                records[i] = new ServiceRecordImpl(this, device, handles[i]);
                try {
                    records[i].populateRecord(new int[] { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004 });
                    if (attrSet != null) {
                        records[i].populateRecord(attrSet);
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

    public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs)
            throws IOException {
        if (attrIDs.length > ATTR_RETRIEVABLE_MAX) {
            throw new IllegalArgumentException();
        }
        /*
         * retrieve SDP blob
         */
        byte[] blob = getServiceAttributes(attrIDs, RemoteDeviceHelper.getAddress(serviceRecord.getHostDevice()),
                (int) serviceRecord.getHandle());

        if (blob.length > 0) {
            try {
                boolean anyRetrived = false;
                DataElement element = (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
                for (Enumeration e = (Enumeration) element.getValue(); e.hasMoreElements();) {
                    int attrID = (int) ((DataElement) e.nextElement()).getLong();
                    serviceRecord.populateAttributeValue(attrID, (DataElement) e.nextElement());
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
            } catch (IOException e) {
                throw e;
            } catch (Throwable e) {
                throw new IOException();
            }
        } else {
            return false;
        }
    }

    /*
     * socket operations
     */
    private native long socket(boolean authenticate, boolean encrypt) throws IOException;

    private native long getsockaddress(long socket) throws IOException;

    private native void storesockopt(long socket);

    private native int getsockchannel(long socket) throws IOException;

    private native void connect(long socket, long address, int channel) throws IOException;

    // public native int getSecurityOptImpl(long handle) throws IOException;
    public int getSecurityOpt(long handle, int expected) throws IOException {
        return expected;
    }

    private native void bind(long socket) throws IOException;

    private native void listen(long socket) throws IOException;

    private native long accept(long socket) throws IOException;

    private native int recvAvailable(long socket) throws IOException;

    private native int recv(long socket) throws IOException;

    private native int recv(long socket, byte[] b, int off, int len) throws IOException;

    private native void send(long socket, int b) throws IOException;

    private native void send(long socket, byte[] b, int off, int len) throws IOException;

    private native void close(long socket) throws IOException;

    private native String getpeername(long address) throws IOException;

    private native long getpeeraddress(long socket) throws IOException;

    // ---------------------- Client RFCOMM connections

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2OpenClientConnection(com.intel.bluetooth.BluetoothConnectionParams,
     *      int, int)
     */
    public long connectionRfOpenClientConnection(BluetoothConnectionParams params) throws IOException {
        long socket = socket(params.authenticate, params.encrypt);
        boolean success = false;
        try {
            connect(socket, params.address, params.channel);
            success = true;
        } finally {
            if (!success) {
                close(socket);
            }
        }
        return socket;
    }

    public void connectionRfCloseClientConnection(long handle) throws IOException {
        close(handle);
    }

    public long rfServerOpen(BluetoothConnectionNotifierParams params, ServiceRecordImpl serviceRecord)
            throws IOException {
        /*
         * open socket
         */
        long socket = socket(params.authenticate, params.encrypt);
        boolean success = false;
        try {

            bind(socket);
            listen(socket);

            int channel = getsockchannel(socket);
            DebugLog.debug("service channel ", channel);

            long serviceRecordHandle = socket;
            serviceRecord.populateRFCOMMAttributes(serviceRecordHandle, channel, params.uuid, params.name, params.obex);

            /*
             * register service
             */
            serviceRecord.setHandle(registerService(serviceRecord.toByteArray()));

            success = true;
        } finally {
            if (!success) {
                close(socket);
            }
        }
        return socket;
    }

    public void rfServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
        try {
            /*
             * close socket
             */
            close(handle);
        } finally {
            /*
             * unregister service
             */
            unregisterService(serviceRecord.getHandle());
        }
    }

    /*
     * register service
     */
    private native long registerService(byte[] record) throws ServiceRegistrationException;

    /*
     * unregister service
     */
    private native void unregisterService(long handle) throws ServiceRegistrationException;

    public long rfServerAcceptAndOpenRfServerConnection(long handle) throws IOException {
        return accept(handle);
    }

    public void rfServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
            throws ServiceRegistrationException {
        unregisterService(serviceRecord.getHandle());
        byte[] blob;
        try {
            blob = serviceRecord.toByteArray();
        } catch (IOException e) {
            throw new ServiceRegistrationException(e.toString());
        }
        serviceRecord.setHandle(registerService(blob));
        DebugLog.debug("new serviceRecord", serviceRecord);
    }

    public void connectionRfCloseServerConnection(long handle) throws IOException {
        connectionRfCloseClientConnection(handle);
    }

    public long getConnectionRfRemoteAddress(long handle) throws IOException {
        return getpeeraddress(handle);
    }

    public int connectionRfRead(long handle) throws IOException {
        return recv(handle);
    }

    public int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException {
        return recv(handle, b, off, len);
    }

    public int connectionRfReadAvailable(long handle) throws IOException {
        return recvAvailable(handle);
    }

    public void connectionRfWrite(long handle, int b) throws IOException {
        send(handle, b);
    }

    public void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException {
        send(handle, b, off, len);
    }

    public void connectionRfFlush(long handle) throws IOException {
        // TODO are there any flush
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
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2CloseClientConnection(long)
     */
    public void l2CloseClientConnection(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerOpen(com.intel.bluetooth.BluetoothConnectionNotifierParams, int,
     *      int, com.intel.bluetooth.ServiceRecordImpl)
     */
    public long l2ServerOpen(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU,
            ServiceRecordImpl serviceRecord) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerUpdateServiceRecord(long, com.intel.bluetooth.ServiceRecordImpl,
     *      boolean)
     */
    public void l2ServerUpdateServiceRecord(long handle, ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
            throws ServiceRegistrationException {
        throw new ServiceRegistrationException("Not Supported on" + getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerAcceptAndOpenServerConnection(long)
     */
    public long l2ServerAcceptAndOpenServerConnection(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2CloseServerConnection(long)
     */
    public void l2CloseServerConnection(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2ServerClose(long, com.intel.bluetooth.ServiceRecordImpl)
     */
    public void l2ServerClose(long handle, ServiceRecordImpl serviceRecord) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2Ready(long)
     */
    public boolean l2Ready(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2receive(long, byte[])
     */
    public int l2Receive(long handle, byte[] inBuf) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2send(long, byte[])
     */
    public void l2Send(long handle, byte[] data) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2GetReceiveMTU(long)
     */
    public int l2GetReceiveMTU(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2GetTransmitMTU(long)
     */
    public int l2GetTransmitMTU(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothStack#l2RemoteAddress(long)
     */
    public long l2RemoteAddress(long handle) throws IOException {
        throw new NotSupportedIOException(getStackID());
    }
}
