/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

/**
 * @author vlads
 * 
 */
public class MonitoringServiceImpl implements MonitoringService {

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
