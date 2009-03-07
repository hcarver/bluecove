/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
 *  @author vlads
 *  @version $Id$
 */
package org.bluez.v3;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Vector;

import javax.bluetooth.DiscoveryAgent;

import org.bluez.Adapter;
import org.bluez.BlueZAPI;
import org.bluez.Error.Failed;
import org.bluez.Error.InvalidArguments;
import org.bluez.Error.NoSuchAdapter;
import org.bluez.Error.NotReady;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;

import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.DebugLog;

/**
 * 
 * Access BlueZ v3 over D-Bus
 * 
 */
public class BlueZAPIV3 implements BlueZAPI {

    private DBusConnection dbusConn;

    private ManagerV3 dbusManager;

    private AdapterV3 adapter;

    private Path adapterPath;

    public BlueZAPIV3(DBusConnection dbusConn, ManagerV3 dbusManager) {
        this.dbusConn = dbusConn;
        this.dbusManager = dbusManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#findAdapter(java.lang.String)
     */
    public Path findAdapter(String pattern) throws InvalidArguments {
        String path;
        try {
            path = dbusManager.FindAdapter(pattern);
        } catch (NoSuchAdapter e) {
            return null;
        }
        if (path == null) {
            return null;
        } else {
            return new Path(path);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#defaultAdapter()
     */
    public Path defaultAdapter() throws InvalidArguments {
        String path;
        try {
            path = dbusManager.DefaultAdapter();
        } catch (NoSuchAdapter e) {
            return null;
        }
        if (path == null) {
            return null;
        } else {
            return new Path(path);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapter(int)
     */
    public Path getAdapter(int number) {
        String[] adapters = dbusManager.ListAdapters();
        if (adapters == null) {
            throw null;
        }
        if ((number < 0) || (number >= adapters.length)) {
            throw null;
        }
        return new Path(String.valueOf(adapters[number]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#listAdapters()
     */
    public List<String> listAdapters() {
        List<String> v = new Vector<String>();
        String[] adapters = dbusManager.ListAdapters();
        if (adapters != null) {
            for (int i = 0; i < adapters.length; i++) {
                String adapterId = String.valueOf(adapters[i]);
                final String bluezPath = "/org/bluez/";
                if (adapterId.startsWith(bluezPath)) {
                    adapterId = adapterId.substring(bluezPath.length());
                }
                v.add(adapterId);
            }
        }
        return v;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#selectAdapter(org.freedesktop.dbus.Path)
     */
    public Adapter selectAdapter(Path adapterPath) throws DBusException {
        adapter = dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), AdapterV3.class);
        this.adapterPath = adapterPath;
        return adapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterAddress()
     */
    public String getAdapterAddress() {
        return adapter.GetAddress();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterID()
     */
    public String getAdapterID() {
        final String bluezPath = "/org/bluez/";
        if (adapterPath.getPath().startsWith(bluezPath)) {
            return adapterPath.getPath().substring(bluezPath.length());
        } else {
            return adapterPath.getPath();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterDeviceClass()
     */
    public int getAdapterDeviceClass() {
        int record = 0;
        String major = adapter.GetMajorClass();

        if ("computer".equals(major)) {
            record |= BluetoothConsts.DeviceClassConsts.MAJOR_COMPUTER;
        } else {
            DebugLog.debug("Unknown MajorClass", major);
        }

        String minor = adapter.GetMinorClass();
        if (minor.equals("uncategorized")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_UNCLASSIFIED;
        } else if (minor.equals("desktop")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_DESKTOP;
        } else if (minor.equals("server")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_SERVER;
        } else if (minor.equals("laptop")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_LAPTOP;
        } else if (minor.equals("handheld")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_HANDHELD;
        } else if (minor.equals("palm")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_PALM;
        } else if (minor.equals("wearable")) {
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_WEARABLE;
        } else {
            DebugLog.debug("Unknown MinorClass", minor);
            record |= BluetoothConsts.DeviceClassConsts.COMPUTER_MINOR_UNCLASSIFIED;
        }

        String[] srvc = adapter.GetServiceClasses();
        if (srvc != null) {
            for (int s = 0; s < srvc.length; s++) {
                String serviceClass = srvc[s];
                if (serviceClass.equals("positioning")) {
                    record |= BluetoothConsts.DeviceClassConsts.POSITIONING_SERVICE;
                } else if (serviceClass.equals("networking")) {
                    record |= BluetoothConsts.DeviceClassConsts.NETWORKING_SERVICE;
                } else if (serviceClass.equals("rendering")) {
                    record |= BluetoothConsts.DeviceClassConsts.RENDERING_SERVICE;
                } else if (serviceClass.equals("capturing")) {
                    record |= BluetoothConsts.DeviceClassConsts.CAPTURING_SERVICE;
                } else if (serviceClass.equals("object transfer")) {
                    record |= BluetoothConsts.DeviceClassConsts.OBJECT_TRANSFER_SERVICE;
                } else if (serviceClass.equals("audio")) {
                    record |= BluetoothConsts.DeviceClassConsts.AUDIO_SERVICE;
                } else if (serviceClass.equals("telephony")) {
                    record |= BluetoothConsts.DeviceClassConsts.TELEPHONY_SERVICE;
                } else if (serviceClass.equals("information")) {
                    record |= BluetoothConsts.DeviceClassConsts.INFORMATION_SERVICE;
                } else {
                    DebugLog.debug("Unknown ServiceClasses", serviceClass);
                }
            }
        }

        return record;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterName()
     */
    public String getAdapterName() {
        try {
            return adapter.GetName();
        } catch (NotReady e) {
            return null;
        } catch (Failed e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#isAdapterDiscoverable()
     */
    public boolean isAdapterDiscoverable() {
        return adapter.IsDiscoverable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterDiscoverableTimeout()
     */
    public int getAdapterDiscoverableTimeout() {
        return adapter.GetDiscoverableTimeout().intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#setAdapterDiscoverable(int)
     */
    public boolean setAdapterDiscoverable(int mode) throws DBusException {
        String modeStr;
        switch (mode) {
        case DiscoveryAgent.NOT_DISCOVERABLE:
            modeStr = "connectable";
            break;
        case DiscoveryAgent.GIAC:
            modeStr = "discoverable";
            break;
        case DiscoveryAgent.LIAC:
            modeStr = "limited";
            break;
        default:
            throw new IllegalArgumentException("Invalid discoverable mode");
        }
        adapter.SetMode(modeStr);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterManufacturer()
     */
    public String getAdapterManufacturer() {
        return adapter.GetManufacturer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterRevision()
     */
    public String getAdapterRevision() {
        return adapter.GetRevision();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getAdapterVersion()
     */
    public String getAdapterVersion() {
        return adapter.GetVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#isAdapterPowerOn()
     */
    public boolean isAdapterPowerOn() {
        return !"off".equals(adapter.GetMode());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bluez.BlueZAPI#deviceInquiry(org.bluez.BlueZAPI.DeviceInquiryListener
     * )
     */
    public void deviceInquiry(final DeviceInquiryListener listener) throws DBusException, InterruptedException {

        final Object discoveryCompletedEvent = new Object();

        DBusSigHandler<AdapterV3.DiscoveryCompleted> discoveryCompleted = new DBusSigHandler<AdapterV3.DiscoveryCompleted>() {
            public void handle(AdapterV3.DiscoveryCompleted s) {
                DebugLog.debug("discoveryCompleted.handle()");
                synchronized (discoveryCompletedEvent) {
                    discoveryCompletedEvent.notifyAll();
                }
            }
        };

        DBusSigHandler<AdapterV3.DiscoveryStarted> discoveryStarted = new DBusSigHandler<AdapterV3.DiscoveryStarted>() {
            public void handle(AdapterV3.DiscoveryStarted s) {
                DebugLog.debug("device discovery procedure has been started.");
                //TODO
            }
        };

        DBusSigHandler<AdapterV3.RemoteDeviceFound> remoteDeviceFound = new DBusSigHandler<AdapterV3.RemoteDeviceFound>() {
            public void handle(AdapterV3.RemoteDeviceFound s) {
                listener.deviceDiscovered(s.getDeviceAddress(), null, s.getDeviceClass().intValue(), adapter.HasBonding(s.getDeviceAddress()));
            }
        };

        DBusSigHandler<AdapterV3.RemoteNameUpdated> remoteNameUpdated = new DBusSigHandler<AdapterV3.RemoteNameUpdated>() {
            public void handle(AdapterV3.RemoteNameUpdated s) {
                listener.deviceDiscovered(s.getDeviceAddress(), s.getDeviceName(), -1, false);
            }
        };

        try {
            dbusConn.addSigHandler(AdapterV3.DiscoveryCompleted.class, discoveryCompleted);
            dbusConn.addSigHandler(AdapterV3.DiscoveryStarted.class, discoveryStarted);
            dbusConn.addSigHandler(AdapterV3.RemoteDeviceFound.class, remoteDeviceFound);
            dbusConn.addSigHandler(AdapterV3.RemoteNameUpdated.class, remoteNameUpdated);

            synchronized (discoveryCompletedEvent) {
                adapter.DiscoverDevices();
                listener.deviceInquiryStarted();
                DebugLog.debug("wait for device inquiry to complete...");
                discoveryCompletedEvent.wait();
                adapter.CancelDiscovery();
            }

        } finally {
            dbusConn.removeSigHandler(AdapterV3.RemoteNameUpdated.class, remoteNameUpdated);
            dbusConn.removeSigHandler(AdapterV3.RemoteDeviceFound.class, remoteDeviceFound);
            dbusConn.removeSigHandler(AdapterV3.DiscoveryStarted.class, discoveryStarted);
            dbusConn.removeSigHandler(AdapterV3.DiscoveryCompleted.class, discoveryCompleted);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#deviceInquiryCancel()
     */
    public void deviceInquiryCancel() throws DBusException {
        adapter.CancelDiscovery();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bluez.BlueZAPI#getRemoteDeviceFriendlyName(java.lang.String)
     */
    public String getRemoteDeviceFriendlyName(final String deviceAddress) throws DBusException, IOException {
        final Object discoveryCompletedEvent = new Object();
        final Vector<String> namesFound = new Vector<String>();

        DBusSigHandler<AdapterV3.DiscoveryCompleted> discoveryCompleted = new DBusSigHandler<AdapterV3.DiscoveryCompleted>() {
            public void handle(AdapterV3.DiscoveryCompleted s) {
                DebugLog.debug("discoveryCompleted.handle()");
                synchronized (discoveryCompletedEvent) {
                    discoveryCompletedEvent.notifyAll();
                }
            }
        };

        DBusSigHandler<AdapterV3.RemoteNameUpdated> remoteNameUpdated = new DBusSigHandler<AdapterV3.RemoteNameUpdated>() {
            public void handle(AdapterV3.RemoteNameUpdated s) {
                if (deviceAddress.equals(s.getDeviceAddress())) {
                    if (s.getDeviceName() != null) {
                        namesFound.add(s.getDeviceName());
                        synchronized (discoveryCompletedEvent) {
                            discoveryCompletedEvent.notifyAll();
                        }
                    } else {
                        DebugLog.debug("device name is null");
                    }
                } else {
                    DebugLog.debug("ignore device name " + s.getDeviceAddress() + " " + s.getDeviceName());
                }
            }
        };

        try {
            dbusConn.addSigHandler(AdapterV3.DiscoveryCompleted.class, discoveryCompleted);
            dbusConn.addSigHandler(AdapterV3.RemoteNameUpdated.class, remoteNameUpdated);

            synchronized (discoveryCompletedEvent) {
                adapter.DiscoverDevices();
                DebugLog.debug("wait for device inquiry to complete...");
                try {
                    discoveryCompletedEvent.wait();
                    DebugLog.debug(namesFound.size() + " device name(s) found");
                    if (namesFound.size() == 0) {
                        throw new IOException("Can't retrive device name");
                    }
                    // return the last name found
                    return namesFound.get(namesFound.size() - 1);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
        } finally {
            dbusConn.removeSigHandler(AdapterV3.RemoteNameUpdated.class, remoteNameUpdated);
            dbusConn.removeSigHandler(AdapterV3.DiscoveryCompleted.class, discoveryCompleted);
        }
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#isRemoteDeviceTrusted(java.lang.String)
     */
    public Boolean isRemoteDeviceTrusted(String deviceAddress) throws DBusException {
        return Boolean.valueOf(adapter.HasBonding(deviceAddress));
    }
}
