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
class EmulatorRFCOMMService extends EmulatorServiceConnection {

	private int channel;

	EmulatorRFCOMMService(EmulatorLocalDevice localDevice, long handle, int channel) {
		super(localDevice, handle);
		this.channel = channel;
	}

	void open(BluetoothConnectionNotifierParams params) throws IOException {
		this.params = params;
		localDevice.getDeviceManagerService().rfOpenService(localDevice.getAddress(), this.channel);
	}

	/**
	 * 
	 * @return connectionHandle on server
	 * @throws IOException
	 */
	long accept() throws IOException {
		return localDevice.getDeviceManagerService().rfAccept(localDevice.getAddress(), this.channel,
				this.params.authenticate, this.params.encrypt);
	}

	int getChannel() {
		return channel;
	}

	void close(ServiceRecordImpl serviceRecord) throws IOException {
		localDevice.getDeviceManagerService().removeServiceRecord(localDevice.getAddress(), serviceRecord.getHandle());
		localDevice.getDeviceManagerService().rfCloseService(localDevice.getAddress(), channel);
	}

}
