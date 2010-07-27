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
 *  =======================================================================================
 *
 *  BlueZ docs licensed under GNU Free Documentation License, Version 1.1 http://www.fsf.org
 *  Copyright (C) 2004-2008  Marcel Holtmann <marcel@holtmann.org>
 *  Copyright (C) 2005-2006  Johan Hedberg <johan.hedberg@nokia.com>
 *  Copyright (C) 2005-2006  Claudio Takahasi <claudio.takahasi@indt.org.br>
 *  Copyright (C) 2006-2007  Luiz von Dentz <luiz.dentz@indt.org.br>
 *
 *  @author vlads
 *  @version $Id$
 */
package org.bluez.v4;

import java.util.Map;

import org.bluez.dbus.DBusProperties;
import org.bluez.dbus.DBusProperties.DBusProperty;
import org.bluez.dbus.DBusProperties.DBusPropertyAccessType;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * BlueZ V4 D-Bus Adapter API
 *
 * Adapter hierarchy
 * <p>
 * Service org.bluez
 * <p>
 * Interface org.bluez.Adapter
 * <p>
 * Object path [variable prefix]/{hci0,hci1,...}
 * <p>
 *
 * Created base on D-Bus API description for BlueZ. bluez-4.32/doc/adapter-api.txt
 */
@DBusInterfaceName("org.bluez.Adapter")
public interface Adapter extends org.bluez.Adapter, DBusProperties.PropertiesAccess {

    public static enum Properties implements DBusProperties.PropertyEnum {

        /**
         * The Bluetooth device address. Example: "00:11:22:33:44:55"
         */
        @DBusProperty(type = String.class, access = DBusPropertyAccessType.READONLY)
        Address,

        /**
         * The Bluetooth friendly name. This value can be changed and a PropertyChanged
         * signal will be emitted.
         */
        @DBusProperty(type = String.class)
        Name,

        /**
         * The Bluetooth class of device.
         *
         * @since BlueZ 4.34
         */
        @DBusProperty(type = UInt32.class, access = DBusPropertyAccessType.READONLY)
        Class,

        /**
         * Switch an adapter on or off. This will also set the appropriate connectable
         * state.
         */
        @DBusProperty(type = boolean.class)
        Powered,

        /**
         * Switch an adapter to discoverable or non-discoverable to either make it visible
         * or hide it. This is a global setting and should only be used by the settings
         * application.
         *
         * If the DiscoverableTimeout is set to a non-zero value then the system will set
         * this value back to false after the timer expired.
         *
         * In case the adapter is switched off, setting this value will fail.
         *
         * When changing the Powered property the new state of this property will be
         * updated via a PropertyChanged signal.
         */
        @DBusProperty(type = boolean.class)
        Discoverable,

        /**
         * Switch an adapter to pairable or non-pairable. This is a global setting and
         * should only be used by the settings application.
         *
         * Note that this property only affects incoming pairing requests.
         */
        @DBusProperty(type = boolean.class)
        Pairable,

        /**
         * The pairable timeout in seconds. A value of zero means that the timeout is
         * disabled and it will stay in pairable mode forever.
         */
        @DBusProperty(type = UInt32.class)
        PaireableTimeout,

        /**
         * The discoverable timeout in seconds. A value of zero means that the timeout is
         * disabled and it will stay in discoverable/limited mode forever.
         *
         * The default value for the discoverable timeout should be 180 seconds (3
         * minutes).
         */
        @DBusProperty(type = UInt32.class)
        DiscoverableTimeout,

        /**
         * Indicates that a device discovery procedure is active.
         */
        @DBusProperty(type = boolean.class, access = DBusPropertyAccessType.READONLY)
        Discovering,

        /**
         * List of device object paths.
         */
        @DBusProperty(type = Path[].class, access = DBusPropertyAccessType.READONLY)
        Devices
    }

    /**
     * This method will request a client session that provides operational Bluetooth. A
     * possible mode change must be confirmed by the user via the agent.
     */
    void RequestSession() throws org.bluez.Error.Rejected;

    /**
     * Release a previous requested session.
     */

    void ReleaseSession() throws org.bluez.Error.DoesNotExist;

    /**
     * This method starts the device discovery session. This includes an inquiry procedure
     * and remote device name resolving. Use StopDiscovery to release the sessions
     * acquired.
     *
     * This process will start emitting DeviceFound and PropertyChanged "Discovering"
     * signals.
     */
    void StartDiscovery() throws org.bluez.Error.NotReady, org.bluez.Error.Failed;

    /**
     * This method will cancel any previous StartDiscovery transaction.
     *
     * Note that a discovery procedure is shared between all discovery sessions thus
     * calling StopDiscovery will only release a single session.
     */
    void StopDiscovery() throws org.bluez.Error.NotReady, org.bluez.Error.Failed, org.bluez.Error.NotAuthorized;

    /**
     * Returns the object path of device for given address.
     *
     * The device object needs to be first created via CreateDevice or CreatePairedDevice.
     */
    Path FindDevice(String address) throws org.bluez.Error.DoesNotExist, org.bluez.Error.InvalidArguments;

    /**
     * Returns list of device object paths.
     */
    Path[] ListDevices() throws org.bluez.Error.InvalidArguments, org.bluez.Error.Failed, org.bluez.Error.OutOfMemory;

    /**
     * Creates a new object path for a remote device. This method will connect to the
     * remote device and retrieve all SDP records.
     *
     * If the object for the remote device already exists this method will fail.
     */
    Path CreateDevice(String address) throws org.bluez.Error.InvalidArguments, org.bluez.Error.Failed;

    /**
     * Creates a new object path for a remote device. This method will connect to the
     * remote device and retrieve all SDP records and then initiate the pairing.
     *
     * If previously CreateDevice was used successfully, this method will only initiate
     * the pairing.
     *
     * Compared to CreateDevice this method will fail if the pairing already exists, but
     * not if the object path already has been created. This allows applications to use
     * CreateDevice first and the if needed use CreatePairedDevice to initiate pairing.
     *
     * The agent object path is assumed to reside within the process (D-Bus connection
     * instance) that calls this method. No separate registration procedure is needed for
     * it and it gets automatically released once the pairing operation is complete.
     *
     * The capability parameter is the same as for the RegisterAgent method.
     */
    Path CreatePairedDevice(String address, Path agent, String capability) throws org.bluez.Error.InvalidArguments, org.bluez.Error.Failed;

    /**
     * Aborts either a CreateDevice call or a CreatePairedDevice call.
     */
    void CancelDeviceCreation(String address) throws org.bluez.Error.InvalidArguments, org.bluez.Error.NotInProgress;

    /**
     * This removes the remote device object at the given path. It will remove also the
     * pairing information.
     */
    void RemoveDevice(Path device) throws org.bluez.Error.InvalidArguments, org.bluez.Error.Failed;

    /**
     * This registers the adapter wide agent.
     *
     * The object path defines the path the of the agent that will be called when user
     * input is needed.
     *
     * If an application disconnects from the bus all of its registered agents will be
     * removed.
     *
     * The capability parameter can have the values "DisplayOnly", "DisplayYesNo",
     * "KeyboardOnly" and "NoInputNoOutput" which reflects the input and output
     * capabilities of the agent. If an empty string is used it will fallback to
     * "DisplayYesNo".
     */
    void RegisterAgent(Path agent, String capability) throws org.bluez.Error.InvalidArguments, org.bluez.Error.AlreadyExists;

    /**
     * This unregisters the agent that has been previously registered. The object path
     * parameter must match the same value that has been used on registration.
     */
    void UnregisterAgent(Path agent) throws org.bluez.Error.DoesNotExist;

    /**
     * This signal indicates a changed value of the given property.
     */
    public class PropertyChanged extends DBusSignal {
        public PropertyChanged(String path, String name, Variant<Object> value) throws DBusException {
            super(path);
        }
    }

    /**
     * This signal will be send every time an inquiry result has been found by the service
     * daemon. In general they only appear during a device discovery.
     *
     * The dictionary can contain basically the same values that we be returned by the
     * GetProperties method from the org.bluez.Device interface. In addition there can be
     * values for the RSSI and the TX power level.
     */
    public class DeviceFound extends DBusSignal {

        private final String address;

        private final Map<String, Variant<?>> deviceProperties;

        public DeviceFound(String path, String address, Map<String, Variant<?>> deviceProperties) throws DBusException {
            super(path, address, deviceProperties);
            this.address = address;
            this.deviceProperties = deviceProperties;
        }

        /**
         * @return the address
         */
        public String getDeviceAddress() {
            return address;
        }

        /**
         * @see org.bluez.v4.Device.Properties
         * @return the deviceProperties
         */
        public Map<String, Variant<?>> getDeviceProperties() {
            return deviceProperties;
        }
    }

    /**
     * This signal will be send when an inquiry session for a periodic discovery finishes
     * and previously found devices are no longer in range or visible.
     */
    public class DeviceDisappeared extends DBusSignal {
        public DeviceDisappeared(String path, String address) throws DBusException {
            super(path, address);
        }
    }

    /**
     * Parameter is object path of created device.
     */
    public class DeviceCreated extends DBusSignal {
        public DeviceCreated(String path, Path device) throws DBusException {
            super(path, device);
        }
    }

    /**
     * Parameter is object path of removed device.
     */
    public class DeviceRemoved extends DBusSignal {
        public DeviceRemoved(String path, Path device) throws DBusException {
            super(path, device);
        }
    }
}
