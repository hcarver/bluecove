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
 *  @author vlads
 *  @version $Id$
 */
package org.bluez.v4;

import org.bluez.Adapter;
import org.bluez.dbus.DBusProperties;
import org.freedesktop.dbus.DBusInterfaceName;

@DBusInterfaceName("org.bluez.Adapter")
public interface AdapterV4 extends Adapter, DBusProperties.PropertiesAccess {

	public static enum Properties implements DBusProperties.Property {

		/**
		 * The Bluetooth device address. Example: "00:11:22:33:44:55"
		 */
		Address,

		/**
		 * The Bluetooth friendly name. This value can be changed and a PropertyChanged signal will be emitted.
		 * 
		 * [readwrite]
		 */
		Name;

		//
		// boolean Powered [readwrite]
		//
		// Switch an adapter on or off. This will also set the
		// appropiate connectable state.
		//
		// boolean Discoverable [readwrite]
		//
		// Switch an adapter to discoverable or non-discoverable
		// to either make it visible or hide it. This is a global
		// setting and should only be used by the settings
		// application.
		//
		// If the DiscoverableTimeout is set to a non-zero
		// value then the system will set this value back to
		// false after the timer expired.
		//
		// In case the adapter is switched off, setting this
		// value will fail.
		//
		// When changing the Powered property the new state of
		// this property will be updated via a PropertyChanged
		// signal.
		//
		// boolean Pairable [readwrite]
		//
		// Switch an adapter to pairable or non-pairable. This is
		// a global setting and should only be used by the
		// settings application.
		//
		// Note that this property only affects incoming pairing
		// requests.
		//
		// uint32 PaireableTimeout [readwrite]
		//
		// The pairable timeout in seconds. A value of zero
		// means that the timeout is disabled and it will stay in
		// pareable mode forever.
		//
		// uint32 DiscoverableTimeout [readwrite]
		//
		// The discoverable timeout in seconds. A value of zero
		// means that the timeout is disabled and it will stay in
		// discoverable/limited mode forever.
		//
		// The default value for the discoverable timeout should
		// be 180 seconds (3 minutes).
		//
		// boolean Discovering [readonly]
		//
		// Indicates that a device discovery procedure is active.
	}

}
