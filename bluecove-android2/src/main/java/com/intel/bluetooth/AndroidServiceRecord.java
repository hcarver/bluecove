/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2010 Mina Shokry
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

import android.bluetooth.BluetoothSocket;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;

/**
 *
 * @author Mina Shokry
 */
public class AndroidServiceRecord extends ServiceRecordImpl {
	private BluetoothSocket socket;
	private boolean obex;
	private UUID uuid;

	public AndroidServiceRecord(BluetoothStack bluetoothStack, RemoteDevice device, BluetoothSocket socket, UUID uuid, boolean obex) {
		super(bluetoothStack, device, 0);
		this.socket = socket;
		this.obex = obex;
		this.uuid = uuid;
	}

	@Override
	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster) {
		StringBuilder buf = new StringBuilder();

		buf.append(obex ? BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX : BluetoothConsts.PROTOCOL_SCHEME_RFCOMM);
		buf.append("://");

		buf.append(socket.getRemoteDevice().getAddress().replace(":", ""));
		buf.append(":");

		buf.append(uuid.toString());

		switch (requiredSecurity) {
		case NOAUTHENTICATE_NOENCRYPT:
			buf.append(";authenticate=false;encrypt=false");
			break;
		case AUTHENTICATE_NOENCRYPT:
			buf.append(";authenticate=true;encrypt=false");
			break;
		case AUTHENTICATE_ENCRYPT:
			buf.append(";authenticate=true;encrypt=true");
			break;
		default:
			throw new IllegalArgumentException();
		}

		if (mustBeMaster) {
			buf.append(";master=true");
		} else {
			buf.append(";master=false");
		}

		buf.append(";android=true");

		return buf.toString();
	}
}
