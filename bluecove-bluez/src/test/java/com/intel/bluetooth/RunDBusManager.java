/**
 *  BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.util.HashMap;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;

import org.bluez.Adapter;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;

/**
 * 
 */
public class RunDBusManager {

//	static {
//		System.getProperties().put("bluecove.debug", "true");
//	}
//
//	public static void main(String[] args) {
//		try {
//			BluetoothStack anyStack = new BluetoothStackBlueZDBus();
//			BlueCoveImpl.loadNativeLibraries(anyStack);
//		} catch (BluetoothStateException e) {
//			throw new Error(e);
//		}
//
//		DBusConnection conn = null;
//		try {
//			conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
//
//			Manager manager = (Manager) conn.getRemoteObject("org.bluez", "/org/bluez", Manager.class);
//
//			// System.out.println("InterfaceVersion " +
//			// manager.InterfaceVersion());
//
//			String defaultAdapter = manager.DefaultAdapter();
//			System.out.println("DefaultAdapter " + defaultAdapter);
//
//			String[] adapters = manager.ListAdapters();
//			for (String adapter : adapters) {
//				System.out.println(" adapter " + adapter);
//			}
//
//			// String[] services = manager.ListServices();
//			// for (String service: services) {
//			// System.out.println(" service " + service);
//			// }
//
//			Adapter adapter = (Adapter) conn.getRemoteObject("org.bluez", defaultAdapter, Adapter.class);
//			System.out.println("DefaultAdapter address " + adapter.GetAddress());
//
//			final Object discoveryCompletedEvent = new Object();
//
//			DBusSigHandler<Adapter.DiscoveryCompleted> discoveryCompleted = new DBusSigHandler<Adapter.DiscoveryCompleted>() {
//				public void handle(Adapter.DiscoveryCompleted s) {
//					synchronized (discoveryCompletedEvent) {
//						discoveryCompletedEvent.notifyAll();
//					}
//				}
//			};
//			conn.addSigHandler(Adapter.DiscoveryCompleted.class, discoveryCompleted);
//
//			DBusSigHandler<Adapter.DiscoveryStarted> discoveryStarted = new DBusSigHandler<Adapter.DiscoveryStarted>() {
//				public void handle(Adapter.DiscoveryStarted s) {
//					System.out.println("device discovery procedure has been started.");
//				}
//			};
//			conn.addSigHandler(Adapter.DiscoveryStarted.class, discoveryStarted);
//
//			final Map<String, Adapter.RemoteDeviceFound> devicesDiscovered = new HashMap<String, Adapter.RemoteDeviceFound>();
//			DBusSigHandler<Adapter.RemoteDeviceFound> remoteDeviceFound = new DBusSigHandler<Adapter.RemoteDeviceFound>() {
//				public void handle(Adapter.RemoteDeviceFound s) {
//					if (!devicesDiscovered.containsKey(s.address)) {
//						System.out.println("device found " + s.address);
//					}
//					devicesDiscovered.put(s.address, s);
//				}
//			};
//			conn.addSigHandler(Adapter.RemoteDeviceFound.class, remoteDeviceFound);
//
//			DBusSigHandler<Adapter.RemoteNameUpdated> remoteNameUpdated = new DBusSigHandler<Adapter.RemoteNameUpdated>() {
//				public void handle(Adapter.RemoteNameUpdated s) {
//					System.out.println("device name found " + s.address + " " + s.name);
//				}
//			};
//			conn.addSigHandler(Adapter.RemoteNameUpdated.class, remoteNameUpdated);
//
//			synchronized (discoveryCompletedEvent) {
//				adapter.DiscoverDevices();
//				System.out.println("wait for device inquiry to complete...");
//				discoveryCompletedEvent.wait();
//				System.out.println(devicesDiscovered.size() + " device(s) found");
//			}
//
//		} catch (Throwable e) {
//			System.out.println(e);
//			e.printStackTrace();
//		} finally {
//			if (conn != null) {
//				conn.disconnect();
//			}
//		}
//
//	}
//
}
