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

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;

/**
 * BlueZ V4 D-Bus Agent API
 * 
 * Agent hierarchy
 * <p>
 * Service unique name
 * <p>
 * Interface org.bluez.Adapter
 * <p>
 * Object path freely definable
 * <p>
 * 
 * Created base on D-Bus API description for BlueZ. bluez-4.32/doc/agent-api.txt
 * 
 */
@DBusInterfaceName("org.bluez.Agent")
public interface Agent extends DBusInterface {

    /**
     * This method gets called when the service daemon unregisters the agent. An
     * agent can use it to do cleanup tasks. There is no need to unregister the
     * agent, because when this method gets called it has already been
     * unregistered.
     */
    void Release();

    /**
     * This method gets called when the service daemon needs to get the passkey
     * for an authentication.
     * 
     * The return value should be a string of 1-16 characters length. The string
     * can be alphanumeric.
     */
    String RequestPinCode(Path device) throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

    /**
     * This method gets called when the service daemon needs to get the passkey
     * for an authentication.
     * 
     * The return value should be a numeric value between 0-999999.
     */
    UInt32 RequestPasskey(Path device) throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

    /**
     * This method gets called when the service daemon needs to display a
     * passkey for an authentication.
     * 
     * The entered parameter indicates the number of already typed keys on the
     * remote side.
     * 
     * An empty reply should be returned. When the passkey needs no longer to be
     * displayed, the Cancel method of the agent will be called.
     * 
     * During the pairing process this method might be called multiple times to
     * update the entered value.
     */
    void DisplayPasskey(Path device, UInt32 passkey, byte entered);

    /**
     * This method gets called when the service daemon needs to confirm a
     * passkey for an authentication.
     * 
     * To confirm the value it should return an empty reply or an error in case
     * the passkey is invalid.
     */
    void RequestConfirmation(Path device, UInt32 passkey) throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

    /**
     * This method gets called when the service daemon needs to authorize a
     * connection/service request.
     */
    void Authorize(Path device, String uuid) throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

    /**
     * This method gets called if a mode change is requested that needs to be
     * confirmed by the user. An example would be leaving flight mode.
     */
    void ConfirmModeChange(String mode) throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

    /**
     * This method gets called to indicate that the agent request failed before
     * a reply was returned.
     */
    void Cancel();
}
