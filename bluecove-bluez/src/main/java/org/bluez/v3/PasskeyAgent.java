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
 * Interface org.bluez.PasskeyAgent;
 * <p>
 * Object path freely definable
 * 
 * Created base on D-Bus API description for BlueZ bluez-utils-3.36/hcid/dbus-api.txt
 */
@DBusInterfaceName("org.bluez.PasskeyAgent")
public interface PasskeyAgent extends DBusInterface {

	/**
	 * This method gets called when the service daemon needs to get the passkey for an authentication. The return value
	 * is actual passkey. It is a 1 to 16 byte PIN code in UTF-8 format.
	 * 
	 * The first argument contains the path of the local adapter and the second one the remote address.
	 */
	public String Request(String path, String address) throws org.bluez.Error.Rejected, org.bluez.Error.Canceled;

	/**
	 * This method gets called to indicate that the authentication request failed before a reply was returned by the
	 * Request method.
	 */
	public void Cancel(String path, String address);

	/**
	 * This method gets called when the service daemon unregisters a passkey agent. An agent can use it to do cleanup
	 * tasks. There is no need to unregister the agent, because when this method gets called it has already been
	 * unregistered.
	 */
	public void Release();
}
