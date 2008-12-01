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
package com.intel.bluetooth;

import java.io.IOException;

/**
 * 
 */
class EmulatorRFCOMMClient extends EmulatorLinkedConnection {

	EmulatorRFCOMMClient(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);

	}

	void connect(BluetoothConnectionParams params) throws IOException {
		connectVerify(params);
		this.connectionHandle = localDevice.getDeviceManagerService().rfConnect(localDevice.getAddress(),
				params.address, params.channel, params.authenticate, params.encrypt, params.timeout);
		this.remoteAddress = params.address;
	}

	int read() throws IOException {
		byte buf[] = new byte[1];
		int len = read(buf, 0, 1);
		if (len == -1) {
			return -1;
		}
		return buf[0] & 0xFF;
	}

	int read(byte[] b, int off, int len) throws IOException {
		byte buf[] = localDevice.getDeviceManagerService().rfRead(localDevice.getAddress(), this.connectionHandle, len);
		if (buf == null) {
			return -1;
		}
		System.arraycopy(buf, 0, b, off, buf.length);
		return buf.length;
	}

	int available() throws IOException {
		return localDevice.getDeviceManagerService().rfAvailable(localDevice.getAddress(), this.connectionHandle);
	}

	void write(int b) throws IOException {
		byte buf[] = new byte[1];
		buf[0] = (byte) (b & 0xFF);
		write(buf, 0, 1);
	}

	void write(byte[] b, int off, int len) throws IOException {
		byte buf[];
		if ((b.length == len) && (off == 0)) {
			buf = b;
		} else {
			buf = new byte[len];
			System.arraycopy(b, off, buf, 0, len);
		}
		localDevice.getDeviceManagerService().rfWrite(localDevice.getAddress(), this.connectionHandle, buf);
	}

	void flush() throws IOException {
	}

}
