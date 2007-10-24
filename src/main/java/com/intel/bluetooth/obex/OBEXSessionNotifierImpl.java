/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
