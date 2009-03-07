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
 *  BlueZ Java docs licensed under GNU Free Documentation License, Version 1.1 http://www.fsf.org
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
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * BlueZ V4 D-Bus Device API
 * 
 * Service hierarchy
 * <p>
 * Service org.bluez
 * <p>
 * Interface org.bluez.Device
 * <p>
 * Object path [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX
 * <p>
 * 
 * Created base on D-Bus API description for BlueZ.
 * bluez-4.32/doc/device-api.txt
 */
@DBusInterfaceName("org.bluez.Device")
public interface Device extends DBusInterface, DBusProperties.PropertiesAccess {

    public static enum Properties implements DBusProperties.PropertyEnum {
        /**
         * The Bluetooth device address of the remote device.
         */
        @DBusProperty(type = String.class, access = DBusPropertyAccessType.READONLY)
        Address,

        /**
         * The Bluetooth remote name. This value can not be changed. Use the
         * Alias property instead.
         */
        @DBusProperty(type = String.class, access = DBusPropertyAccessType.READONLY)
        Name,

        /**
         * Proposed icon name according to the freedesktop.org icon naming
         * specification.
         */
        @DBusProperty(type = String.class, access = DBusPropertyAccessType.READONLY)
        Icon,

        /** The Bluetooth class of device of the remote device. */
        @DBusProperty(type = UInt32.class, access = DBusPropertyAccessType.READONLY)
        Class,

        /**
         * List of 128-bit UUIDs that represents the available remote services.
         */
        @DBusProperty(type = String[].class, access = DBusPropertyAccessType.READONLY)
        UUIDs,

        /**
         * Indicates if the remote device is paired.
         */
        @DBusProperty(type = boolean.class, access = DBusPropertyAccessType.READONLY)
        Paired,

        /**
         * Indicates if the remote device is currently connected. A
         * PropertyChanged signal indicate changes to this status.
         */
        @DBusProperty(type = boolean.class, access = DBusPropertyAccessType.READONLY)
        Connected,

        /**
         * Indicates if the remote is seen as trusted. This setting can be
         * changed by the application.
         */
        @DBusProperty(type = boolean.class)
        Trusted,

        /**
         * The name alias for the remote device. The alias can be used to have a
         * different friendly name for the remote device.
         * 
         * In case no alias is set, it will return the remote device name.
         * Setting an empty string as alias will convert it back to the remote
         * device name.
         * 
         * When reseting the alias with an empty string, the emitted
         * PropertyChanged signal will show the remote name again.
         */
        @DBusProperty(type = String.class)
        Alias,

        /**
         * List of device node object paths.
         */
        @DBusProperty(type = Path[].class, access = DBusPropertyAccessType.READONLY)
        Nodes,

        /**
         * The object path of the adapter the device belongs to.
         */
        @DBusProperty(type = Path.class, access = DBusPropertyAccessType.READONLY)
        Adapter,

        /**
         * Set to true if the device only supports the pre-2.1 pairing
         * mechanism. This property is useful in the Adapter.DeviceFound signal
         * to anticipate whether legacy or secure simple pairing will occur.
         * 
         * Note that this property can exhibit false-positives in the case of
         * Bluetooth 2.1 (or newer) devices that have disabled Extend Inquiry
         * Response support.
         */
        @DBusProperty(type = boolean.class, access = DBusPropertyAccessType.READONLY)
        LegacyPairing

    }

    /**
     * This method starts the service discovery to retrieve remote service
     * records. The pattern parameter can be used to specific specific UUIDs.
     * 
     * The return value is a dictionary with the record handles as keys and the
     * service record in XML format as values. The key is uint32 and the value a
     * string for this dictionary.
     */
    Map<UInt32, String> DiscoverServices(String pattern) throws org.bluez.Error.NotReady, org.bluez.Error.Failed, org.bluez.Error.InProgress;

    /**
     * This method will cancel any previous DiscoverServices transaction.
     */
    void CancelDiscovery() throws org.bluez.Error.NotReady, org.bluez.Error.Failed, org.bluez.Error.NotAuthorized;

    /*
     * This method disconnects a specific remote device by terminating the
     * low-level ACL connection. The use of this method should be restricted to
     * administrator use.
     * 
     * A DisconnectRequested signal will be sent and the actual disconnection
     * will only happen 2 seconds later. This enables upper-level applications
     * to terminate their connections gracefully before the ACL connection is
     * terminated.
     */
    void Disconnect() throws org.bluez.Error.NotConnected;

    /**
     * Returns list of device node object paths.
     */
    Object[] ListNodes() throws org.bluez.Error.InvalidArguments, org.bluez.Error.Failed, org.bluez.Error.OutOfMemory;

    /**
     * Creates a persistent device node binding with a remote device. The actual
     * support for the specified UUID depends if the device driver has support
     * for persistent binding. At the moment only RFCOMM TTY nodes are
     * supported.
     */
    Object CreateNode(String uuid) throws org.bluez.Error.InvalidArguments, org.bluez.Error.NotSupported;

    /**
     * Removes a persistent device node binding.
     */
    void RemoveNode(Object node) throws org.bluez.Error.InvalidArguments, org.bluez.Error.DoesNotExist;

    /**
     * This signal indicates a changed value of the given property.
     */
    public class PropertyChanged extends DBusSignal {
        public PropertyChanged(String path, String name, Variant<Object> value) throws DBusException {
            super(path);
        }
    }

    /**
     * This signal will be sent when a low level disconnection to a remote
     * device has been requested. The actual disconnection will happen 2 seconds
     * later.
     */
    public class DisconnectRequested extends DBusSignal {
        public DisconnectRequested(String path) throws DBusException {
            super(path);
        }
    }

    /**
     * Parameter is object path of created device node.
     */
    public class NodeCreated extends DBusSignal {
        public NodeCreated(String path, Path node) throws DBusException {
            super(path, node);
        }
    }

    /**
     * Parameter is object path of removed device node.
     */
    public class NodeRemoved extends DBusSignal {
        public NodeRemoved(String path, Path node) throws DBusException {
            super(path, node);
        }
    }
}
