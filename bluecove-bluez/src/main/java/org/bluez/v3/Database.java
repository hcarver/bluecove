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

import org.bluez.Error;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.UInt32;

/**
 * SDP queries involves transactions between an SDP server and an SDP client. The server
 * maintains a list of service records that describe the characteristics of services
 * associated with the server.
 * <p>
 * The database interface provides methods to manage local service records(SDP Server).
 * All of the information about a service that is maintained by an SDP server is contained
 * within a single service record. A service record is basically a list of service
 * attributes. Each service attribute describes a single characteristic of a service such
 * as the service type, name, description, ...
 * <p>
 * Client methods are available at Adapter interface. See GetRemoteService?{handles,
 * record} methods for more information how retrieve remote service handles/records.
 * 
 * Record ownership and life-cycle
 * <p>
 * This section is applied for record registered by D-Bus methods only.
 * <p>
 * Only the record owner can update or remove the record If the owner exits, the record is
 * automatically removed from the database
 * 
 * 
 * Service org.bluez
 * <p>
 * Interface org.bluez.Database
 * <p>
 * Object path /org/bluez   (NOT On /org/bluez/{hci0,hci1,...})
 * <p>
 */
@DBusInterfaceName("org.bluez.Database")
public interface Database extends DBusInterface {

    /**
     * This method registers a new service specified by its unique identifier. This is
     * only needed for services that are not started through the Bluetooth daemon.
     */

    void RegisterService(String identifier, String name, String description);

    /**
     * This method unregisters a service specified by its unique identifier.
     */
    void UnregisterService(String identifier);

    /**
     * Adds a new service record and returns the assigned record handle.
     */
    UInt32 AddServiceRecord(byte[] b) throws Error.InvalidArguments, Error.Failed;

    /**
     * Adds a new service record and returns the assigned record handle.
     */
    UInt32 AddServiceRecordFromXML(String record) throws Error.InvalidArguments, Error.Failed;

    /**
     * Updates a given service record.
     */
    void UpdateServiceRecord(UInt32 handle, byte[] sdprecord) throws Error.InvalidArguments, Error.NotAvailable, Error.Failed;

    /**
     * Updates a given service record provided in the XML format.
     * 
     * @param handle
     * @param record
     */
    void UpdateServiceRecordFromXML(UInt32 handle, String sdprecordXML) throws Error.InvalidArguments, Error.NotAvailable, Error.Failed;

    /**
     * Remove a service record identified by its handle.
     * <p>
     * It is only possible to remove service records that where added by the current
     * connection.
     * 
     * @param handle
     */
    void RemoveServiceRecord(UInt32 handle) throws Error.InvalidArguments, Error.NotAuthorized, Error.DoesNotExist, Error.Failed;

    /**
     * This method gets called when a service wants to check if a remote device is
     * authorized to perform some action. The authorization request is forwarded to an
     * authorization agent.
     * <p>
     * The address parameter is the Bluetooth address of the remote device and the uuid is
     * the identifier of the profile requesting the authorization. This parameter can also
     * be left blank.
     */
    void RequestAuthorization(String address, String uuid);

    /**
     * This method cancels an authorization process requested by a previous call to
     * RequestAuthorization(). The address and uuid parameters must match.
     */
    void CancelAuthorizationRequest(String address, String uuid);

}
