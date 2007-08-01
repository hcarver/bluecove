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

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.obex.Authenticator;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.NotImplementedIOException;

public class OBEXSessionNotifierImpl implements SessionNotifier, BluetoothConnectionNotifierServiceRecordAccess {
	
	private StreamConnectionNotifier notifier;
	
	public OBEXSessionNotifierImpl(StreamConnectionNotifier notifier) throws IOException {
		if (false) {
			throw new NotImplementedIOException();
		}
		this.notifier = notifier;
	}
	
	public Connection acceptAndOpen(ServerRequestHandler handler) throws IOException {
		return acceptAndOpen(handler, null);
	}

	public Connection acceptAndOpen(ServerRequestHandler handler, Authenticator auth) throws IOException {
		if (notifier == null) {
            throw new IOException("Session closed");
		}
		return new OBEXServerSessionImpl(notifier.acceptAndOpen(), handler, auth);
	}

	public void close() throws IOException {
		if (this.notifier != null) {
			this.notifier.close();
			this.notifier = null;
		}
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		if (!(notifier instanceof BluetoothConnectionNotifierServiceRecordAccess)) {
			throw new IllegalArgumentException("connection is not a Bluetooth notifier");
		}
		return ((BluetoothConnectionNotifierServiceRecordAccess) notifier).getServiceRecord();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#updateServiceRecord(boolean)
	 */
	public void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		if (!(notifier instanceof BluetoothConnectionNotifierServiceRecordAccess)) {
			throw new IllegalArgumentException("connection is not a Bluetooth notifier");
		}
		((BluetoothConnectionNotifierServiceRecordAccess) notifier).updateServiceRecord(acceptAndOpen);
	}

}
