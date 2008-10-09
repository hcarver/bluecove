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
class BluetoothL2CAPServerConnection extends BluetoothL2CAPConnection implements BluetoothServerConnection {

	/**
	 * @param handle
	 * @throws IOException
	 */
	protected BluetoothL2CAPServerConnection(BluetoothStack bluetoothStack, long handle, int securityOpt)
			throws IOException {
		super(bluetoothStack, handle);
		boolean initOK = false;
		try {
			this.securityOpt = securityOpt;
			RemoteDeviceHelper.connected(this);
			initOK = true;
		} finally {
			if (!initOK) {
				try {
					bluetoothStack.l2CloseServerConnection(this.handle);
				} catch (IOException e) {
					DebugLog.error("close error", e);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothL2CAPConnection#closeConnectionHandle(long)
	 */
	void closeConnectionHandle(long handle) throws IOException {
		RemoteDeviceHelper.disconnected(this);
		bluetoothStack.l2CloseServerConnection(handle);
	}

}
