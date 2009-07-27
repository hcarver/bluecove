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

import org.bluez.Error;
import org.bluez.dbus.DBusProperties;
import org.bluez.dbus.DBusProperties.DBusProperty;
import org.bluez.dbus.DBusProperties.DBusPropertyAccessType;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * BlueZ V4 D-Bus Manager API
 * 
 * Manager hierarchy
 * <p>
 * Service org.bluez
 * <p>
 * Interface org.bluez.Manager
 * <p>
 * Object path /
 * <p>
 * 
 * Created base on D-Bus API description for BlueZ. bluez-4.32/doc/manager-api.txt
 */
@DBusInterfaceName("org.bluez.Manager")
public interface Manager extends org.bluez.Manager, DBusProperties.PropertiesAccess {

    public static enum Properties implements DBusProperties.PropertyEnum {

        /**
         * List of adapter object paths.
         */
        @DBusProperty(type = Path[].class, access = DBusPropertyAccessType.READONLY)
        Adapters
    }

    /**
     * Returns object path for the default adapter.
     * 
     * @return returns Object Path
     */
    Path DefaultAdapter() throws Error.InvalidArguments, Error.NoSuchAdapter;

    /**
     * Returns object path for the specified adapter.
     * 
     * @param pattern
     *            "hci0" or "00:11:22:33:44:55"
     * @return returns Object Path
     */
    Path FindAdapter(String pattern) throws Error.InvalidArguments, Error.NoSuchAdapter;

    /**
     * Returns list of adapter object paths under /org/bluez
     * 
     * @return returns Path[]
     */
    Path[] ListAdapters() throws Error.InvalidArguments, Error.Failed, Error.OutOfMemory;

    /**
     * This signal indicates a changed value of the given property.
     */
    @DBusInterfaceName("org.bluez.Manager.AdapterAdded")
    public class PropertyChanged extends DBusSignal {
        public PropertyChanged(String path, String name, Variant<Object> value) throws DBusException {
            super(path);
        }
    }

    /**
     * Parameter is object path of added adapter.
     */
    @DBusInterfaceName("org.bluez.Manager.AdapterAdded")
    public class AdapterAdded extends DBusSignal {
        public AdapterAdded(String path, Path adapter) throws DBusException {
            super(path, adapter);
        }
    }

    /**
     * Parameter is object path of removed adapter.
     */
    @DBusInterfaceName("org.bluez.Manager.AdapterAdded")
    public class AdapterRemoved extends DBusSignal {
        public AdapterRemoved(String path, Path adapter) throws DBusException {
            super(path, adapter);
        }
    }

    /**
     * Parameter is object path of the new default adapter.
     * 
     * In case all adapters are removed this signal will not be emitted. The
     * AdapterRemoved signal has to be used to detect that no default adapter is
     * selected or available anymore.
     */
    @DBusInterfaceName("org.bluez.Manager.AdapterAdded")
    public class DefaultAdapterChanged extends DBusSignal {
        public DefaultAdapterChanged(String path, Path adapter) throws DBusException {
            super(path, adapter);
        }
    }

}
