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
import java.io.InputStream;
import java.io.OutputStream;

/**
 */
abstract class ConnectionBuffer {

	private final String portID;

	protected final long remoteAddress;

	protected int securityOpt;

	protected final InputStream is;

	protected final OutputStream os;

	protected boolean closed;

	protected ConnectionBuffer connected;

	protected boolean serverSide;

	protected boolean serverAccepted;

	protected MonitorConnectionBuffer monitor;

	protected ConnectionBuffer(long remoteAddress, String portID, InputStream is, OutputStream os) {
		this.remoteAddress = remoteAddress;
		this.portID = portID;
		this.is = is;
		this.os = os;
	}

	void connect(ConnectionBuffer pair) {
		connected = pair;
		pair.connected = this;
	}

	synchronized void accepted() {
		this.serverAccepted = true;
		this.notifyAll();
	}

	boolean isServerAccepted() {
		return serverAccepted;
	}

	long getRemoteAddress() {
		return remoteAddress;
	}

	void setSecurityOpt(int securityOpt) {
		this.securityOpt = securityOpt;
	}

	int getSecurityOpt(int expected) throws IOException {
		return securityOpt;
	}

	boolean encrypt(long remoteAddress, boolean on) throws IOException {
		if (this.remoteAddress != remoteAddress) {
			throw new IllegalArgumentException("Connection not to this device");
		}
		return false;
	}

	synchronized void close() throws IOException {
		closed = true;
		monitor.closedTimeStamp = System.currentTimeMillis();
		try {
			os.close();
		} finally {
			is.close();
		}
		this.notifyAll();
	}

	boolean isServerSide() {
		return serverSide;
	}

	void setServerSide(boolean serverSide) {
		this.serverSide = serverSide;
	}

	void setMonitor(MonitorConnectionBuffer monitor) {
		this.monitor = monitor;
	}

	boolean isClosed() {
		return closed;
	}

	public String getPortID() {
		return this.portID;
	}

}
