/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package org.bluez;

import org.freedesktop.dbus.DBusInterface;

/**
 * pin_helper concept has been removed starting with bluez-utils 3.X. and has
 * been replaced with a feature called passkey agents. An application that wants
 * to handle passkey requests must use the "hcid" security interface to register
 * a passkey agent. Currently, two types of passkey agents are supported:
 * default and device specific. A "specific" passkey agent handles all passkey
 * requests for a given remote device while a default handles all requests for
 * which a specific agent was not found. "specific" passkey agents are useful to
 * address pre-defined passkey values or environments where the user interaction
 * is not allowed/difficult.
 * <p>
 * When the CreateBonding method is called the "hcid" daemon will verify if
 * there is a link key stored in the file system. If it is available an error is
 * returned, and if not, a D-Bus message is sent to the registered passkey agent
 * asking for a passkey.
 * <p>
 * Each Passkey Agent is represented by a D-Bus object path. The "hcid"
 * distinguishes the agents based on their unique bus names and their object
 * paths.
 * 
 */
public interface Security extends DBusInterface {
	/*
	void RegisterDefaultPasskeyAgent(String path)

	This registers the default passkey agent. It can
	register a passkey for all adapters or for a
	specific device depending on with object path has
	been used.

	The path parameter defines the object path of the
	passkey agent that will be called when a passkey
	needs to be entered.

	If an application disconnects from the bus all
	registered passkey agent will be removed.

	throws Error.AlreadyExists

void UnregisterDefaultPasskeyAgent(String path)

	This unregisters a default passkey agent that has
	been previously registered. The object path and
	the path parameter must match the same values that
	has been used on registration.

	throws Error.DoesNotExist

void RegisterPasskeyAgent(String path, String address)

	This registers the application passkey agent that
	will be used for any application specific passkey
	tasks.

	The path parameter defines the object path of the
	passkey agent that will be called when a passkey
	needs to be entered. The address defines the remote
	device that it will answer passkey requests for.

	If an application disconnects from the bus all
	registered passkey agent will be removed. It will
	also be unregistered after a timeout and if the
	pairing succeeds or fails. The application has to
	take care of that it reregisters the passkey agent.

	throws Error.AlreadyExists

void UnregisterPasskeyAgent(String path, String address)

	This unregisters a passkey agent that has been
	previously registered. The object path and the path
	and address parameter must match the same values
	that has been used on registration.

	The method is actually only needed if an application
	wants to removed the passkey agent and don't wanna
	wait for the automatic timeout.

	throws Error.DoesNotExist

void RegisterDefaultAuthorizationAgent(String path)

	This registers the default authorization agent. It can
	register an authorization agent for all adapters or
	for a specific one depending on which object path has
	been used.

	The path parameter defines the object path of the
	authorization agent that will be called when an
	authorization request needs to be answered.

void UnregisterDefaultAuthorizationAgent(String path)

	This unregisters a default authorization agent that has
	been previously registered. The path parameter must
	match the same value that has been used on
	registration.
	*/
}
