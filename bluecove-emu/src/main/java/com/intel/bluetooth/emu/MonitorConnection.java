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

/**
 * @author vlads
 * 
 */
public class MonitorConnection implements MonitorItem {

	private static final long serialVersionUID = 1L;

	private long connectionId;

	private long connectedTimeStamp;

	private long clientDevice;

	private long serverDevice;

	private String portId;

	private MonitorConnectionBuffer clientBuffer;

	private MonitorConnectionBuffer serverBuffer;

	MonitorConnection(long clientDevice, long serverDevice, String portId, long connectionId) {
		this.clientDevice = clientDevice;
		this.serverDevice = serverDevice;
		this.portId = portId;
		this.connectionId = connectionId;
		this.connectedTimeStamp = System.currentTimeMillis();
		this.clientBuffer = new MonitorConnectionBuffer();
		this.serverBuffer = new MonitorConnectionBuffer();
	}

	public long getConnectedTimeStamp() {
		return connectedTimeStamp;
	}

	public long getClientDevice() {
		return clientDevice;
	}

	public long getServerDevice() {
		return serverDevice;
	}

	public String getPortId() {
		return portId;
	}

	public MonitorConnectionBuffer getClientBuffer() {
		return clientBuffer;
	}

	public MonitorConnectionBuffer getServerBuffer() {
		return serverBuffer;
	}

	public long getConnectionId() {
		return this.connectionId;
	}

}
