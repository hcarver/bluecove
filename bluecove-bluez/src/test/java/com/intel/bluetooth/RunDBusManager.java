package com.intel.bluetooth;

/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
import java.util.HashMap;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;

import org.bluez.Adapter;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;

/**
 * @author vlads
 * 
 */
public class RunDBusManager {

	static {
		System.getProperties().put("bluecove.debug", "true");
	}

	public static void main(String[] args) {
		try {
			BluetoothStack anyStack = new BluetoothStackBlueZ();
			BlueCoveImpl.loadNativeLibraries(anyStack);
		} catch (BluetoothStateException e) {
			throw new Error(e);
		}

		DBusConnection conn = null;
		try {
			conn = DBusConnection.getConnection(DBusConnection.SYSTEM);

			Manager manager = (Manager) conn.getRemoteObject("org.bluez", "/org/bluez", Manager.class);

			// System.out.println("InterfaceVersion " +
			// manager.InterfaceVersion());

			String defaultAdapter = manager.DefaultAdapter();
			System.out.println("DefaultAdapter " + defaultAdapter);

			String[] adapters = manager.ListAdapters();
			for (String adapter : adapters) {
				System.out.println(" adapter " + adapter);
			}

			// String[] services = manager.ListServices();
			// for (String service: services) {
			// System.out.println(" service " + service);
			// }

			Adapter adapter = (Adapter) conn.getRemoteObject("org.bluez", defaultAdapter, Adapter.class);
			System.out.println("DefaultAdapter address " + adapter.GetAddress());

			final Object discoveryCompletedEvent = new Object();

			DBusSigHandler<Adapter.DiscoveryCompleted> discoveryCompleted = new DBusSigHandler<Adapter.DiscoveryCompleted>() {
				public void handle(Adapter.DiscoveryCompleted s) {
					synchronized (discoveryCompletedEvent) {
						discoveryCompletedEvent.notifyAll();
					}
				}
			};
			conn.addSigHandler(Adapter.DiscoveryCompleted.class, discoveryCompleted);

			DBusSigHandler<Adapter.DiscoveryStarted> discoveryStarted = new DBusSigHandler<Adapter.DiscoveryStarted>() {
				public void handle(Adapter.DiscoveryStarted s) {
					System.out.println("device discovery procedure has been started.");
				}
			};
			conn.addSigHandler(Adapter.DiscoveryStarted.class, discoveryStarted);

			final Map<String, Adapter.RemoteDeviceFound> devicesDiscovered = new HashMap<String, Adapter.RemoteDeviceFound>();
			DBusSigHandler<Adapter.RemoteDeviceFound> remoteDeviceFound = new DBusSigHandler<Adapter.RemoteDeviceFound>() {
				public void handle(Adapter.RemoteDeviceFound s) {
					if (!devicesDiscovered.containsKey(s.address)) {
						System.out.println("device found " + s.address);
					}
					devicesDiscovered.put(s.address, s);
				}
			};
			conn.addSigHandler(Adapter.RemoteDeviceFound.class, remoteDeviceFound);

			DBusSigHandler<Adapter.RemoteNameUpdated> remoteNameUpdated = new DBusSigHandler<Adapter.RemoteNameUpdated>() {
				public void handle(Adapter.RemoteNameUpdated s) {
					System.out.println("device name found " + s.address + " " + s.name);
				}
			};
			conn.addSigHandler(Adapter.RemoteNameUpdated.class, remoteNameUpdated);

			synchronized (discoveryCompletedEvent) {
				adapter.DiscoverDevices();
				System.out.println("wait for device inquiry to complete...");
				discoveryCompletedEvent.wait();
				System.out.println(devicesDiscovered.size() + " device(s) found");
			}

		} catch (Throwable e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

	}

}
