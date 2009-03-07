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
package org.bluez.v3;

import org.bluez.Error;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.UInt32;

@DBusInterfaceName("org.bluez.Manager")
public interface ManagerV3 extends Manager {

	/**
	 * Deprecated in BlueZ 4
	 * 
	 * @return the current interface version. At the moment only version 0 is supported.
	 */
	public UInt32 InterfaceVersion() throws Error.InvalidArguments;

	/**
	 * Returns object path for the default adapter.
	 * 
	 * @return returns Object in BlueZ 4
	 */
	String DefaultAdapter() throws Error.InvalidArguments, Error.NoSuchAdapter;

	/**
	 * Returns object path for the specified adapter.
	 * 
	 * @param pattern
	 *            "hci0" or "00:11:22:33:44:55"
	 * @return returns Object in BlueZ 4
	 */
	String FindAdapter(String pattern) throws Error.InvalidArguments, Error.NoSuchAdapter;

	/**
	 * Returns list of adapter object paths under /org/bluez
	 * 
	 * @return returns path list
	 */
	String[] ListAdapters() throws Error.InvalidArguments, Error.Failed, Error.OutOfMemory;
}
