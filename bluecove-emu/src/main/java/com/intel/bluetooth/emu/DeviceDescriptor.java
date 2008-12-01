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
