/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.obex;

import java.io.IOException;

import javax.bluetooth.DataElement;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;

import com.intel.bluetooth.BluetoothConsts;

/**
 * @author vlads
 *
 */
class OBEXTCPServiceRecordImpl implements ServiceRecord {

	private String host;

	private String port;

	OBEXTCPServiceRecordImpl(ServerSocketConnection notifier) {
		try {
			port = String.valueOf(notifier.getLocalPort());
			host = notifier.getLocalAddress();
		} catch (IOException e) {
			host = null;
		}
	}

	OBEXTCPServiceRecordImpl(SocketConnection connection) {
		try {
			port = String.valueOf(connection.getPort());
			host = connection.getAddress();
		} catch (IOException e) {
			host = null;
		}
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getConnectionURL(int, boolean)
	 */
	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster) {
		if (host == null) {
			return null;
		}
		return BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX + "://" + host + ":" + port;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getAttributeIDs()
	 */
	public int[] getAttributeIDs() {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getAttributeValue(int)
	 */
	public DataElement getAttributeValue(int attrID) {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getHostDevice()
	 */
	public RemoteDevice getHostDevice() {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#populateRecord(int[])
	 */
	public boolean populateRecord(int[] attrIDs) throws IOException {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#setAttributeValue(int, javax.bluetooth.DataElement)
	 */
	public boolean setAttributeValue(int attrID, DataElement attrValue) {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#setDeviceServiceClasses(int)
	 */
	public void setDeviceServiceClasses(int classes) {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

}
