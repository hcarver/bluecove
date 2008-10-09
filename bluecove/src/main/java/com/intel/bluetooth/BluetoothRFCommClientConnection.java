/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;

/**
 * @author vlads
 *
 */
class BluetoothRFCommClientConnection extends BluetoothRFCommConnection {

	public BluetoothRFCommClientConnection(BluetoothStack bluetoothStack, BluetoothConnectionParams params)
			throws IOException {
		super(bluetoothStack, bluetoothStack.connectionRfOpenClientConnection(params));
		boolean initOK = false;
		try {
			this.securityOpt = bluetoothStack.rfGetSecurityOpt(this.handle, Utils.securityOpt(params.authenticate,
					params.encrypt));
			RemoteDeviceHelper.connected(this);
			initOK = true;
		} finally {
			if (!initOK) {
				try {
					bluetoothStack.connectionRfCloseClientConnection(this.handle);
				} catch (IOException e) {
					DebugLog.error("close error", e);
				}
			}
		}
	}

	void closeConnectionHandle(long handle) throws IOException {
		RemoteDeviceHelper.disconnected(this);
		bluetoothStack.connectionRfCloseClientConnection(handle);
	}
}
