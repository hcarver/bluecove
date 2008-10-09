/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
import java.io.InterruptedIOException;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

class BluetoothRFCommConnectionNotifier extends BluetoothConnectionNotifierBase implements StreamConnectionNotifier {

	private int rfcommChannel = -1;

	public BluetoothRFCommConnectionNotifier(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params)
			throws IOException {
		super(bluetoothStack, params);

		this.handle = bluetoothStack.rfServerOpen(params, serviceRecord);

		this.rfcommChannel = serviceRecord.getChannel(BluetoothConsts.RFCOMM_PROTOCOL_UUID);

		this.serviceRecord.attributeUpdated = false;

		this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);

		this.connectionCreated();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#stackServerClose(long)
	 */
	protected void stackServerClose(long handle) throws IOException {
		bluetoothStack.rfServerClose(handle, serviceRecord);
	}

	/*
	 * Returns a StreamConnection that represents a server side socket
	 * connection. Returns: A socket to communicate with a client. Throws:
	 * IOException - If an I/O error occurs.
	 */

	public StreamConnection acceptAndOpen() throws IOException {
		if (closed) {
			throw new IOException("Notifier is closed");
		}
		updateServiceRecord(true);
		try {
			long clientHandle = bluetoothStack.rfServerAcceptAndOpenRfServerConnection(handle);
			int clientSecurityOpt = bluetoothStack.rfGetSecurityOpt(clientHandle, this.securityOpt);
			return new BluetoothRFCommServerConnection(bluetoothStack, clientHandle, clientSecurityOpt);
		} catch (InterruptedIOException e) {
			throw e;
		} catch (IOException e) {
			if (closed) {
				throw new InterruptedIOException("Notifier has been closed; " + e.getMessage());
			}
			throw e;
		}
	}

	protected void validateServiceRecord(ServiceRecord srvRecord) {
		if (this.rfcommChannel != serviceRecord.getChannel(BluetoothConsts.RFCOMM_PROTOCOL_UUID)) {
			throw new IllegalArgumentException("Must not change the RFCOMM server channel number");
		}
		super.validateServiceRecord(srvRecord);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#updateStackServiceRecord(com.intel.bluetooth.ServiceRecordImpl,
	 *      boolean)
	 */
	protected void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		bluetoothStack.rfServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
	}

}