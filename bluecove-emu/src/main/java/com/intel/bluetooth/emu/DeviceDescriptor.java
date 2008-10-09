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

import java.io.Serializable;

import javax.bluetooth.DiscoveryAgent;

public class DeviceDescriptor implements Serializable {

	private static final long serialVersionUID = 1L;

	private long address;

	private String name;

	private boolean poweredOn = true;

	private int deviceClass;

	private boolean connectable = true;

	private int discoverableMode;

	private long limitedDiscoverableStart = 0;

	public DeviceDescriptor(long address, String name, int deviceClass) {
		super();
		this.address = address;
		this.name = name;
		this.deviceClass = deviceClass;
		this.discoverableMode = DiscoveryAgent.GIAC;
	}

	public long getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public int getDeviceClass() {
		return deviceClass;
	}

	public void setDeviceClass(int deviceClass) {
		this.deviceClass = deviceClass;
	}

	public String toString() {
		return "[address=" + address + "; name=" + name + "; clazz=" + deviceClass + "]";
	}

	public int getDiscoverableMode() {
		return discoverableMode;
	}

	public void setDiscoverableMode(int discoverableMode) {
		this.discoverableMode = discoverableMode;
		if (discoverableMode == DiscoveryAgent.LIAC) {
			limitedDiscoverableStart = System.currentTimeMillis();
		}
	}

	public long getLimitedDiscoverableStart() {
		return limitedDiscoverableStart;
	}

	public boolean isPoweredOn() {
		return this.poweredOn;
	}

	public void setPoweredOn(boolean poweredOn) {
		this.poweredOn = poweredOn;
	}

	public boolean isConnectable() {
		return connectable;
	}

	public void setConnectable(boolean connectable) {
		this.connectable = connectable;
	}

}
