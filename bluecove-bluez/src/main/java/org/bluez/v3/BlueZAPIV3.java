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
package org.bluez.v3;

import java.util.List;
import java.util.Vector;

import org.bluez.Adapter;
import org.bluez.BlueZAPI;
import org.bluez.Error.InvalidArguments;
import org.bluez.Error.NoSuchAdapter;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * 
 * Access BlueZ v3 over D-Bus
 * 
 */
public class BlueZAPIV3 implements BlueZAPI {

	private DBusConnection dbusConn;

	private ManagerV3 dbusManager;

	private AdapterV3 adapter;

	private Path adapterPath;

	public BlueZAPIV3(DBusConnection dbusConn, ManagerV3 dbusManager) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#findAdapter(java.lang.String)
	 */
	public Path findAdapter(String pattern) throws InvalidArguments {
		String path;
		try {
			path = dbusManager.FindAdapter(pattern);
		} catch (NoSuchAdapter e) {
			return null;
		}
		if (path == null) {
			return null;
		} else {
			return new Path(path);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#defaultAdapter()
	 */
	public Path defaultAdapter() throws InvalidArguments {
		String path;
		try {
			path = dbusManager.DefaultAdapter();
		} catch (NoSuchAdapter e) {
			return null;
		}
		if (path == null) {
			return null;
		} else {
			return new Path(path);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapter(int)
	 */
	public Path getAdapter(int number) {
		String[] adapters = dbusManager.ListAdapters();
		if (adapters == null) {
			throw null;
		}
		if ((number < 0) || (number >= adapters.length)) {
			throw null;
		}
		return new Path(String.valueOf(adapters[number]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#listAdapters()
	 */
	public List<String> listAdapters() {
		List<String> v = new Vector<String>();
		String[] adapters = dbusManager.ListAdapters();
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
		adapter = dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), AdapterV3.class);
		this.adapterPath = adapterPath;
		return adapter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapterAddress()
	 */
	public String getAdapterAddress() {
		return adapter.GetAddress();
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
