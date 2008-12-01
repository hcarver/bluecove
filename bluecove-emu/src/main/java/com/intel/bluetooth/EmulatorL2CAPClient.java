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
 * @author vlads
 * 
 */
class EmulatorL2CAPClient extends EmulatorLinkedConnection {

	private int receiveMTU;

	private int transmitMTU;

	EmulatorL2CAPClient(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);
	}

	void connect(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
		connectVerify(params);
		this.connectionHandle = localDevice.getDeviceManagerService().l2Connect(localDevice.getAddress(),
				params.address, params.channel, params.authenticate, params.encrypt, receiveMTU, params.timeout);
		this.remoteAddress = params.address;
		this.receiveMTU = receiveMTU;
		this.transmitMTU = transmitMTU;
		int remoteDeviceReceiveMTU = localDevice.getDeviceManagerService().l2RemoteDeviceReceiveMTU(
				localDevice.getAddress(), this.connectionHandle);
		if (this.transmitMTU == -1) {
			this.transmitMTU = remoteDeviceReceiveMTU;
		} else if (this.transmitMTU < remoteDeviceReceiveMTU) {
			// Ok use selected
		} else {
			this.transmitMTU = remoteDeviceReceiveMTU;
		}
	}

	void connect(long remoteAddress, long connectionHandle, int receiveMTU, int transmitMTU) throws IOException {
		super.connect(remoteAddress, connectionHandle);
		this.receiveMTU = receiveMTU;
		this.transmitMTU = transmitMTU;
	}

	int getReceiveMTU() throws IOException {
		return receiveMTU;
	}

	int getTransmitMTU() throws IOException {
		return transmitMTU;
	}

	boolean ready() throws IOException {
		return localDevice.getDeviceManagerService().l2Ready(localDevice.getAddress(), this.connectionHandle);
	}

	int receive(byte[] inBuf) throws IOException {
		byte[] packetData = localDevice.getDeviceManagerService().l2Receive(localDevice.getAddress(),
				this.connectionHandle, this.receiveMTU);
		int length = packetData.length;
		if (length > inBuf.length) {
			length = inBuf.length;
		}
		System.arraycopy(packetData, 0, inBuf, 0, length);
		return length;
	}

	void send(byte[] data) throws IOException {
		if (data.length > transmitMTU) {
			byte[] b = new byte[transmitMTU];
			System.arraycopy(data, 0, b, 0, transmitMTU);
			data = b;
		}
		localDevice.getDeviceManagerService().l2Send(localDevice.getAddress(), this.connectionHandle, data);
	}

}
