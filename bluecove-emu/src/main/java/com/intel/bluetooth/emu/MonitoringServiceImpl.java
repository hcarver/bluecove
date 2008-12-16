/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

/**
 * 
 */
class MonitoringServiceImpl implements MonitoringService {

	private static Map<MonitorConnection, Object> connections = new WeakHashMap<MonitorConnection, Object>();

	private static Map<MonitorService, Object> servicess = new WeakHashMap<MonitorService, Object>();

	static void registerService(MonitorService monitorService) {
		servicess.put(monitorService, null);
	}

	static void registerConnection(MonitorConnection monitorConnection) {
		connections.put(monitorConnection, null);
	}

	public List<MonitorDevice> getDevices() {
		return DeviceManagerServiceImpl.getMonitorDevices();
	}

	public List<MonitorService> getServices() {
		List<MonitorService> r = new Vector<MonitorService>();
		r.addAll(servicess.keySet());
		return r;
	}

	public List<MonitorConnection> getConnections() {
		List<MonitorConnection> r = new Vector<MonitorConnection>();
		r.addAll(connections.keySet());
		return r;
	}

	public void setDevicePower(long address, boolean on) {
		Device d = DeviceManagerServiceImpl.getDevice(address);
		if (d != null) {
			d.setDevicePower(on);
		}
	}

	public void setDeviceDiscoverable(long address, int mode) {
		Device d = DeviceManagerServiceImpl.getDevice(address);
		if (d != null) {
			d.getDescriptor().setDiscoverableMode(mode);
		}
	}

	public void createThreadDumpFile(long address) {
		Device d = DeviceManagerServiceImpl.getDevice(address);
		if (d != null) {
			d.putCommand(new DeviceCommand(DeviceCommand.DeviceCommandType.createThreadDumpFile));
		}
	}

	public void shutdownJVM(long address) {
		Device d = DeviceManagerServiceImpl.getDevice(address);
		if (d != null) {
			d.putCommand(new DeviceCommand(DeviceCommand.DeviceCommandType.shutdownJVM));
		}
	}

	public void connectionDellayDelivery(long address, long connectionId, int msecDelay) {
		// TODO
	}

	public void connectionBreak(long address, long connectionId) {
		// TODO
	}

}
