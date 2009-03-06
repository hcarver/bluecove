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

import java.util.List;
import java.util.Vector;

import org.bluez.Adapter;
import org.bluez.BlueZAPI;
import org.bluez.Error.InvalidArguments;
import org.bluez.Error.NoSuchAdapter;
import org.bluez.dbus.DBusProperties;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * Access BlueZ v4 over D-Bus
 */
public class BlueZAPIV4 implements BlueZAPI {

	private DBusConnection dbusConn;

	private ManagerV4 dbusManager;

	private AdapterV4 adapter;

	private Path adapterPath;

	public BlueZAPIV4(DBusConnection dbusConn, ManagerV4 dbusManager) {
		this.dbusConn = dbusConn;
		this.dbusManager = dbusManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#findAdapter(java.lang.String)
	 */
	public Path findAdapter(String pattern) throws InvalidArguments {
		try {
			return dbusManager.FindAdapter(pattern);
		} catch (NoSuchAdapter e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#defaultAdapter()
	 */
	public Path defaultAdapter() throws InvalidArguments {
		try {
			return dbusManager.DefaultAdapter();
		} catch (NoSuchAdapter e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapter(int)
	 */
	public Path getAdapter(int number) {
		Path[] adapters = dbusManager.ListAdapters();
		if (adapters == null) {
			throw null;
		}
		if ((number < 0) || (number >= adapters.length)) {
			throw null;
		}
		return adapters[number];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#listAdapters()
	 */
	public List<String> listAdapters() {
		List<String> v = new Vector<String>();
		Path[] adapters = dbusManager.ListAdapters();
		if (adapters != null) {
			for (int i = 0; i < adapters.length; i++) {
				String adapterId = String.valueOf(adapters[i]);
				final String bluezPath = "/org/bluez/";
				if (adapterId.startsWith(bluezPath)) {
					adapterId = adapterId.substring(bluezPath.length());
				}
				v.add(adapterId);
			}
		}
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#selectAdapter(org.freedesktop.dbus.Path)
	 */
	public Adapter selectAdapter(Path adapterPath) throws DBusException {
		adapter = dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), AdapterV4.class);
		this.adapterPath = adapterPath;
		return adapter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapterAddress()
	 */
	public String getAdapterAddress() {
		return DBusProperties.getStringValue(adapter, AdapterV4.Properties.Address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapterID()
	 */
	public String getAdapterID() {
		final String bluezPath = "/org/bluez/";
		if (adapterPath.getPath().startsWith(bluezPath)) {
			return adapterPath.getPath().substring(bluezPath.length());
		} else {
			return adapterPath.getPath();
		}
	}

}
