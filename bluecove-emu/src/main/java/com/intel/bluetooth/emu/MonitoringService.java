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

/**
 * @author vlads
 * 
 */
public interface MonitoringService {

	public List<MonitorDevice> getDevices();

	public List<MonitorService> getServices();

	public List<MonitorConnection> getConnections();

	public void setDevicePower(long address, boolean on);

	public void setDeviceDiscoverable(long address, int mode);

	public void createThreadDumpFile(long address);

	public void shutdownJVM(long address);

	public void connectionDellayDelivery(long address, long connectionId, int msecDelay);

	public void connectionBreak(long address, long connectionId);

}
