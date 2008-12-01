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

import javax.bluetooth.BluetoothConnectionException;

/**
 * 
 */
abstract class EmulatorLinkedConnection extends EmulatorConnection {

	protected long remoteAddress;

	EmulatorLinkedConnection(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);
	}

	void connectVerify(BluetoothConnectionParams params) throws IOException {
		if (params.address == localDevice.getAddress()) {
			throw new BluetoothConnectionException(BluetoothConnectionException.FAILED_NOINFO,
					"Can't connect to local device");
		}
		if ((params.encrypt) && (!localDevice.getConfiguration().isLinkEncryptionSupported())) {
			throw new BluetoothConnectionException(BluetoothConnectionException.SECURITY_BLOCK,
					"encrypt mode not supported");
		}
	}

	void connect(long remoteAddress, long connectionHandle) throws IOException {
		this.connectionHandle = connectionHandle;
		this.remoteAddress = remoteAddress;
	}

	int getSecurityOpt(int expected) throws IOException {
		return localDevice.getDeviceManagerService().getSecurityOpt(localDevice.getAddress(), this.connectionHandle,
				expected);
	}

	boolean encrypt(long address, boolean on) throws IOException {
		return localDevice.getDeviceManagerService().encrypt(localDevice.getAddress(), this.connectionHandle, address,
				on);
	}

	long getRemoteAddress() throws IOException {
		return remoteAddress;
	}

	void close() throws IOException {
		localDevice.getDeviceManagerService().closeConnection(localDevice.getAddress(), this.connectionHandle);
	}
}
