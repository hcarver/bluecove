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
 *  @author vlads
 *  @version $Id$
 */
package org.bluez;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * This interface provides methods to list the available adapters, retrieve the
 * default local adapter path and list/activate services. There is just one
 * Manager object instance provided in the path "/org/bluez".
 * 
 * Service org.bluez; Interface org.bluez.Manager; Object path /org/bluez
 * 
 * Created base on D-Bus API description for BlueZ.
 * bluez-utils-3.17/hcid/dbus-api.txt
 * 
 */
public interface Manager extends DBusInterface {

	/**
	 * @deprecated in BlueZ 4
	 * @return the current interface version. At the moment only version 0 is
	 *         supported.
	 */
	@Deprecated
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
	 * @return returns Object[] in BlueZ 4
	 */
	String[] ListAdapters() throws Error.InvalidArguments, Error.Failed, Error.OutOfMemory;

	/**
	 * @deprecated in BlueZ 4
	 * 
	 *             Returns object path for the specified service. Valid patterns
	 *             are the unqiue identifier or a bus name.
	 * 
	 * @param pattern
	 * @return
	 */
	@Deprecated
	String FindService(String pattern) throws Error.InvalidArguments, Error.NoSuchService;

	/**
	 * @deprecated in BlueZ 4 Returns list of object paths of current services.
	 * 
	 * @return
	 * @throws Error.InvalidArguments
	 */
	@Deprecated
	String[] ListServices() throws Error.InvalidArguments;

	/**
	 * @deprecated in BlueZ 4 Returns the unqiue bus id of the specified
	 *             service. Valid patterns are the same as for FindService(). If
	 *             the service is not running it will be started.
	 * 
	 * @param pattern
	 * @return
	 */
	@Deprecated
	String ActivateService(String pattern);

	/**
	 * Parameter is object path of added adapter.
	 * 
	 * @author vlads
	 * 
	 */
	public class AdapterAdded extends DBusSignal {
		public AdapterAdded(String path) throws DBusException {
			super(path);
		}
	}

	// void AdapterRemoved(String path)
	//
	// Parameter is object path of removed adapter.
	//

	// void DefaultAdapterChanged(String path)
	//
	// Parameter is object path of the new default adapter.
	//

	// void ServiceAdded(String path)
	//
	// Parameter is object path of registered service agent.
	//

	// void ServiceRemoved(String path)
	//
	// Parameter is object path of unregistered service agent.
}
