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
package com.intel.bluetooth;

import java.io.IOException;

/**
 * 
 */
class EmulatorL2CAPService extends EmulatorServiceConnection {

	private int receiveMTU;

	private int transmitMTU;

	int pcm;

	EmulatorL2CAPService(EmulatorLocalDevice localDevice, long handle, int pcm) {
		super(localDevice, handle);
		this.pcm = pcm;
	}

	public int getPcm() {
		return this.pcm;
	}

	int getReceiveMTU() throws IOException {
		return receiveMTU;
	}

	int getTransmitMTU() throws IOException {
		return transmitMTU;
	}

	public void open(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU) throws IOException {
		this.params = params;
		this.receiveMTU = receiveMTU;
		this.transmitMTU = transmitMTU;
		localDevice.getDeviceManagerService().l2OpenService(localDevice.getAddress(), this.pcm);
	}

	/**
	 * 
	 * @return connectionHandle on server
	 * @throws IOException
	 */
	public long accept() throws IOException {
		return localDevice.getDeviceManagerService().l2Accept(localDevice.getAddress(), this.pcm,
				this.params.authenticate, this.params.encrypt, this.receiveMTU);
	}

	public void close(ServiceRecordImpl serviceRecord) throws IOException {
		localDevice.getDeviceManagerService().removeServiceRecord(localDevice.getAddress(), serviceRecord.getHandle());
		localDevice.getDeviceManagerService().l2CloseService(localDevice.getAddress(), this.pcm);
	}
}
