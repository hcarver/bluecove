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

import java.io.IOException;

/**
 * 
 */
public class MonitorService implements MonitorItem {

	private static final long serialVersionUID = 1L;

	private long device;

	private String portId;

	private long internalHandle;

	private boolean listening;

	private int connectionCount;

	private int sdpAttributes;

	private int sdpSize;

	private String displayName;

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	public long getDevice() {
		return device;
	}

	public boolean isListening() {
		return listening;
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public String getPortId() {
		return portId;
	}

	public long getInternalHandle() {
		return internalHandle;
	}

	public int getSdpAttributes() {
		return sdpAttributes;
	}

	public int getSdpSize() {
		return sdpSize;
	}

	public String getDisplayName() {
		return displayName;
	}
}
