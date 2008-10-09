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
package com.intel.bluetooth.btgoep;

import java.io.IOException;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import com.ibm.oti.connection.CreateConnection;
import com.intel.bluetooth.BluetoothConnectionAccess;
import com.intel.bluetooth.BluetoothConnectionAccessAdapter;
import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * This class is Proxy for btgoep (OBEX over RFCOMM) Connection implementations
 * for IBM J9 support
 * <p>
 * You need to configure -Dmicroedition.connection.pkgs=com.intel.bluetooth if
 * not installing bluecove.jar to "%J9_HOME%\lib\jclMidp20\ext\
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 * @author vlads
 *
 */
public class Connection extends BluetoothConnectionAccessAdapter implements CreateConnection, ClientSession,
		SessionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

	private javax.microedition.io.Connection impl;

	public Connection() {
		impl = null;
	}

	public void setParameters(String spec, int access, boolean timeout) throws IOException {
		impl = MicroeditionConnector.open(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX + ":" + spec, access, timeout);
	}

	public javax.microedition.io.Connection setParameters2(String spec, int access, boolean timeout) throws IOException {
		setParameters(spec, access, timeout);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccessAdapter#getImpl()
	 */
	protected BluetoothConnectionAccess getImpl() {
		return (BluetoothConnectionAccess) impl;
	}

	public void close() throws IOException {
		impl.close();
	}

	public HeaderSet connect(HeaderSet headers) throws IOException {
		return ((ClientSession) impl).connect(headers);
	}

	public HeaderSet createHeaderSet() {
		return ((ClientSession) impl).createHeaderSet();
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		return ((ClientSession) impl).delete(headers);
	}

	public HeaderSet disconnect(HeaderSet headers) throws IOException {
		return ((ClientSession) impl).disconnect(headers);
	}

	public Operation get(HeaderSet headers) throws IOException {
		return ((ClientSession) impl).get(headers);
	}

	public long getConnectionID() {
		return ((ClientSession) impl).getConnectionID();
	}

	public Operation put(HeaderSet headers) throws IOException {
		return ((ClientSession) impl).put(headers);
	}

	public void setAuthenticator(Authenticator auth) {
		((ClientSession) impl).setAuthenticator(auth);

	}

	public void setConnectionID(long id) {
		((ClientSession) impl).setConnectionID(id);
	}

	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException {
		return ((ClientSession) impl).setPath(headers, backup, create);
	}

	public javax.microedition.io.Connection acceptAndOpen(ServerRequestHandler handler) throws IOException {
		return ((SessionNotifier) impl).acceptAndOpen(handler);
	}

	public javax.microedition.io.Connection acceptAndOpen(ServerRequestHandler handler, Authenticator auth)
			throws IOException {
		return ((SessionNotifier) impl).acceptAndOpen(handler, auth);
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