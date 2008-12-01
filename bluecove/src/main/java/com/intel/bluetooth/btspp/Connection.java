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
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth.btspp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import com.ibm.oti.connection.CreateConnection;
import com.intel.bluetooth.BluetoothConnectionAccess;
import com.intel.bluetooth.BluetoothConnectionAccessAdapter;
import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * This class is Proxy for btspp (RFCOMM) Connection implementations for IBM J9
 * support
 * <p>
 * You need to configure -Dmicroedition.connection.pkgs=com.intel.bluetooth if
 * not installing bluecove.jar to "%J9_HOME%\lib\jclMidp20\ext\
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 */
public class Connection extends BluetoothConnectionAccessAdapter implements CreateConnection, StreamConnection,
		StreamConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

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

	public void setParameters(String spec, int access, boolean timeout) throws IOException {
		impl = MicroeditionConnector.open(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM + ":" + spec, access, timeout);
	}

	public javax.microedition.io.Connection setParameters2(String spec, int access, boolean timeout) throws IOException {
		setParameters(spec, access, timeout);
		return this;
	}

	public void close() throws IOException {
		impl.close();
	}

	public DataInputStream openDataInputStream() throws IOException {
		return ((InputConnection) impl).openDataInputStream();
	}

	public InputStream openInputStream() throws IOException {
		return ((InputConnection) impl).openInputStream();
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return ((OutputConnection) impl).openDataOutputStream();
	}

	public OutputStream openOutputStream() throws IOException {
		return ((OutputConnection) impl).openOutputStream();
	}

	public StreamConnection acceptAndOpen() throws IOException {
		return ((StreamConnectionNotifier) impl).acceptAndOpen();
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
