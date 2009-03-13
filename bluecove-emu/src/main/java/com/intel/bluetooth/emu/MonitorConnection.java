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

/**
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
