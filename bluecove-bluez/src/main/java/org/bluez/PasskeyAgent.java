/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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

/**
 * 
 * Service unique name; Interface org.bluez.PasskeyAgent; Object path freely
 * definable
 * 
 */
public interface PasskeyAgent {
/*
	String Request(String path, String address, boolean numeric)

	This method gets called when the service daemon
	needs to get the passkey for an authentication. The
	return value is actual passkey.

	The first argument contains the path of the local
	adapter and the second one the remote address. The
	third argument signals if a numeric PIN code is
	expected or not. The default is a 1 to 16 byte PIN
	code in UTF-8 format.

	throws Error.Rejected
	                 Error.Canceled

void Confirm(String path, String address, String value)

	This method gets called when the service daemon
	needs to verify a passkey. The verification is
	done by showing the value to the passkey agent
	and returning means a successful confirmation.
	In case the values don't match an error must
	be returned.

	throws Error.Rejected
	                 Error.Canceled

void Display(String path, String address, String value)

	This method gets called when the service daemon
	needs to display the passkey value. No return
	value is needed. A successful paring will be
	indicated by the Complete method and a failure
	will be signaled with Cancel.

void Keypress(String path, String address)

	This method indicates keypresses from the remote
	device. This can happen when pairing with a keyboard.

void Complete(String path, String address)

	This method gets called to indicate that the
	authentication has been completed.

void Cancel(String path, String address)

	This method gets called to indicate that the
	authentication request failed before a reply was
	returned by the Request method.

void Release()

	This method gets called when the service daemon
	unregisters a passkey agent. An agent can use
	it to do cleanup tasks. There is no need to
	unregister the agent, because when this method
	gets called it has already been unregistered.
*/	
}
