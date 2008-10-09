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
import java.util.Vector;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.Connection;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.obex.Authenticator;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.Utils;

/**
 * SessionNotifier implementation. See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 *
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 * @author vlads
 *
 */
public class OBEXSessionNotifierImpl implements SessionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

	private StreamConnectionNotifier notifier;

	private OBEXConnectionParams obexConnectionParams;

	private static final String FQCN = OBEXSessionNotifierImpl.class.getName();

	private static final Vector fqcnSet = new Vector();

	static {
		fqcnSet.addElement(FQCN);
	}

	/**
	 * Applications should not used this function.
	 *
	 * @exception Error
	 *                if called from outside of BlueCove internal code.
	 */
	public OBEXSessionNotifierImpl(StreamConnectionNotifier notifier, OBEXConnectionParams obexConnectionParams)
			throws IOException, Error {
		Utils.isLegalAPICall(fqcnSet);
		this.notifier = notifier;
		this.obexConnectionParams = obexConnectionParams;
	}

	public Connection acceptAndOpen(ServerRequestHandler handler) throws IOException {
		return acceptAndOpen(handler, null);
	}

	public Connection acceptAndOpen(ServerRequestHandler handler, Authenticator auth) throws IOException {
		if (notifier == null) {
			throw new IOException("Session closed");
		}
		if (handler == null) {
			throw new NullPointerException("handler is null");
		}
		return new OBEXServerSessionImpl(notifier.acceptAndOpen(), handler, auth, obexConnectionParams);
	}

	public void close() throws IOException {
		StreamConnectionNotifier n = this.notifier;
		this.notifier = null;
		if (n != null) {
			n.close();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		if (notifier instanceof ServerSocketConnection) {
			return new OBEXTCPServiceRecordImpl((ServerSocketConnection) notifier);
		}
		if (!(notifier instanceof BluetoothConnectionNotifierServiceRecordAccess)) {
			throw new IllegalArgumentException("connection is not a Bluetooth notifier");
		}
		return ((BluetoothConnectionNotifierServiceRecordAccess) notifier).getServiceRecord();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#updateServiceRecord(boolean)
	 */
	public void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		if (!(notifier instanceof BluetoothConnectionNotifierServiceRecordAccess)) {
			throw new IllegalArgumentException("connection is not a Bluetooth notifier");
		}
		((BluetoothConnectionNotifierServiceRecordAccess) notifier).updateServiceRecord(acceptAndOpen);
	}

}
