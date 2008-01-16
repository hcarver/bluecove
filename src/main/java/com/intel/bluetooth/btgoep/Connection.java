/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
 * 
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