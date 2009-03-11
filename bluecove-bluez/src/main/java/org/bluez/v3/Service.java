/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2009 Vlad Skarzhevskyy
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
package org.bluez.v3;

import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * The service interfaces aim provide an easy way to develop Bluetooth services. For
 * remote service search(handles/records) check the Adapter interface. This section is
 * restricted to local interactions only. There are two interfaces related to Bluetooth
 * services:
 * <p>
 * <ul>
 * <li>Service interface: retrieve information about the registered service. eg: name,
 * description, status, Bus id, ...</li>
 * <li>Database interface: manage SDP records and service connection authorizations</li>
 * <ul>
 * 
 * BlueZ services can be classified as external and internal. Internal are services
 * registered automatically when the system starts. External are services running in the
 * standalone mode where the user start the service and ask for registration. Once the
 * service is registered, an object instance will be available, the methods provided are
 * described below.
 * 
 * 
 * Service org.bluez;
 * <p>
 * Interface org.bluez.Service;
 * <p>
 * Object path path from org.bluez.Manager.ListServices()
 * 
 * Created base on D-Bus API description for BlueZ bluez-utils-3.36/hcid/dbus-api.txt
 * 
 * @since BlueZ 3.8
 */
@DBusInterfaceName("org.bluez.Service")
public interface Service extends DBusInterface {

    /**
     * Returns the service properties.
     * @since BlueZ 3.10
     */
    Map GetInfo();

    /**
     * This method returns the service identifier.
     */
    String GetIdentifier();

    /**
     * This method returns the service name.
     */
    String GetName();

    /**
     * This method returns the service description.
     */
    String GetDescription();

    /**
     * Returns the unique bus name of the service if it has been started. [experimental]
     * @since BlueZ 3.10
     */
    String GetBusName() throws org.bluez.Error.NotAvailable;

    /**
     * This method tells the system to start the service.
     */
    void Start();

    /**
     * This method tells the system to stop the service.
     */
    void Stop();

    /**
     * Returns true if the service has been started and is currently active. Otherwise, it
     * returns false.
     */
    boolean IsRunning();

    /**
     * Returns true if the service was registered using the Database.RegisterService
     * method instead of a .service file. The Start and Stop methods are not applicable to
     * external services and will return an error.
     */
    boolean IsExternal();

    /**
     * Returns a list of remote devices that are trusted for the service. [experimental]
     */
    String[] ListTrusts();

    /**
     * Marks the user as trusted. [experimental]
     */
    void SetTrusted(String address) throws org.bluez.Error.InvalidArguments, org.bluez.Error.AlreadyExists;

    /**
     * Returns true if the user is trusted or false otherwise. The address parameter must
     * match one of the current users of the service.
     * 
     * [experimental]
     */
    boolean IsTrusted(String address) throws org.bluez.Error.InvalidArguments;

    /**
     * Marks the user as not trusted.
     * 
     * [experimental]
     */
    void RemoveTrust(String address) throws org.bluez.Error.InvalidArguments, org.bluez.Error.DoesNotExist;

    //===================== Signals =====================

    /**
     * The object path of this signal contains which service was started.
     */
    public class Started extends DBusSignal {

        public Started(String path) throws DBusException {
            super(path);
        }
    }

    /**
     * The object path of this signal contains which service was stopped.
     */
    public class Stopped extends DBusSignal {

        public Stopped(String path) throws DBusException {
            super(path);
        }
    }

    /**
     * Sent when SetTrusted() is called.
     * 
     * @since BlueZ 3.10
     */
    public class TrustAdded extends DBusSignal {

        private final String address;

        public TrustAdded(String path, String address) throws DBusException {
            super(path, address);
            this.address = address;
        }

        public String getDeviceAddress() {
            return address;
        }
    }

    /**
     * Sent when RemoveTrust() is called.
     * 
     * @since BlueZ 3.10
     */
    public class TrustRemoved extends DBusSignal {

        private final String address;

        public TrustRemoved(String path, String address) throws DBusException {
            super(path, address);
            this.address = address;
        }

        public String getDeviceAddress() {
            return address;
        }
    }
}
