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

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;

/**
 * Service unique name;
 * <p>
 * Interface org.bluez.AuthorizationAgent;
 * <p>
 * Object path freely definable
 * 
 * Created base on D-Bus API description for BlueZ bluez-utils-3.36/hcid/dbus-api.txt
 */
@DBusInterfaceName("org.bluez.AuthorizationAgent")
public interface AuthorizationAgent extends DBusInterface {

	/**
	 * This method gets called when the service daemon wants to get an authorization for accessing a service. This
	 * method should return if the remote user is granted access or an error otherwise.
	 * 
	 * The adapter_path parameter is the object path of the local adapter. The address, service_path and action
	 * parameters correspond to the remote device address, the object path of the service and the uuid of the profile.
	 */
	void Authorize(String adapter_path, String address, String service_path, String uuid)
			throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

	/**
	 * This method cancels a previous authorization request. The adapter_path, address, service_path and uuid parameters
	 * must match the same values that have been used when the Authorize() method was called.
	 */
	void Cancel(String adapter_path, String address, String service_path, String uuid);

	/**
	 * This method gets called when the service daemon unregisters an authorization agent. An agent can use it to do
	 * cleanup tasks. There is no need to unregister the agent, because when this method gets called it has already been
	 * unregistered.
	 */
	void Release();

}
