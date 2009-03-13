/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Michael Lifshits
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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

import java.io.IOException;

/**
 * 
 */
public class MonitorDevice implements MonitorItem {

	private static final long serialVersionUID = 1L;

	private transient Device device;

	protected DeviceDescriptor deviceDescriptor;

	private boolean hasServices;

	private boolean listening;

	private Long[] connectedTo;

	protected MonitorDevice() {

	}

	MonitorDevice(Device device) {
		this.device = device;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		updateFields();
		out.defaultWriteObject();
	}

	protected void updateFields() {
		if (device != null) {
			deviceDescriptor = device.getDescriptor();
			hasServices = device.isHasServices();
			listening = device.isListening();
			connectedTo = device.getConnectedTo();
		}
	}

	public DeviceDescriptor getDeviceDescriptor() {
		return deviceDescriptor;
	}

	public boolean isHasServices() {
		return hasServices;
	}

	public boolean isListening() {
		return listening;
	}

	public Long[] getConnectedTo() {
		return connectedTo;
	}

}
