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
package com.intel.bluetooth.btl2cap;

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;

import com.ibm.oti.connection.CreateConnection;
import com.intel.bluetooth.BluetoothConnectionAccess;
import com.intel.bluetooth.BluetoothConnectionAccessAdapter;
import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * This class is Proxy for btl2cap (L2CAP) Connection implementations for IBM J9
 * support
 * <p>
 * You need to configure -Dmicroedition.connection.pkgs=com.intel.bluetooth if
 * not installing bluecove.jar to "%J9_HOME%\lib\jclMidp20\ext\
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 * @author vlads
 *
 */
public class Connection extends BluetoothConnectionAccessAdapter implements CreateConnection, L2CAPConnection,
		L2CAPConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

	javax.microedition.io.Connection impl;

	public Connection() {
		impl = null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccessAdapter#getImpl()
	 */
	protected BluetoothConnectionAccess getImpl() {
		return (BluetoothConnectionAccess) impl;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.oti.connection.CreateConnection#setParameters(java.lang.String,
	 *      int, boolean)
	 */
	public void setParameters(String spec, int access, boolean timeout) throws IOException {
		impl = MicroeditionConnector.open(BluetoothConsts.PROTOCOL_SCHEME_L2CAP + ":" + spec, access, timeout);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.oti.connection.CreateConnection#setParameters2(java.lang.String,
	 *      int, boolean)
	 */
	public javax.microedition.io.Connection setParameters2(String spec, int access, boolean timeout) throws IOException {
		setParameters(spec, access, timeout);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		impl.close();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
	 */
	public int getReceiveMTU() throws IOException {
		return ((L2CAPConnection) impl).getReceiveMTU();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
	 */
	public int getTransmitMTU() throws IOException {
		return ((L2CAPConnection) impl).getTransmitMTU();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.bluetooth.L2CAPConnection#ready()
	 */
	public boolean ready() throws IOException {
		return ((L2CAPConnection) impl).ready();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.bluetooth.L2CAPConnection#receive(byte[])
	 */
	public int receive(byte[] inBuf) throws IOException {
		return ((L2CAPConnection) impl).receive(inBuf);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.bluetooth.L2CAPConnection#send(byte[])
	 */
	public void send(byte[] data) throws IOException {
		((L2CAPConnection) impl).send(data);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public L2CAPConnection acceptAndOpen() throws IOException {
		return ((L2CAPConnectionNotifier) impl).acceptAndOpen();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		return ((BluetoothConnectionNotifierServiceRecordAccess) impl).getServiceRecord();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#updateServiceRecord(boolean)
	 */
	public void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		((BluetoothConnectionNotifierServiceRecordAccess) impl).updateServiceRecord(acceptAndOpen);
	}

}
