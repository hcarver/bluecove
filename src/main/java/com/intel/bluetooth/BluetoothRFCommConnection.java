/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package com.intel.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;

public abstract class BluetoothRFCommConnection implements StreamConnection, BluetoothConnectionAccess {
	
	protected long handle;

	volatile protected BluetoothInputStream in;

	volatile protected BluetoothOutputStream out;

	protected boolean closing;

	protected int securityOpt;
	
	RemoteDevice remoteDevice;
	
	protected BluetoothRFCommConnection(long handle) {
		this.handle = handle;
	}

	abstract void closeConnectionHandle(long handle) throws IOException;
	
	/**
	 * Implemenation specific
	 * The idea is to Close Connection 
	 * 1. When in and out was closed
	 * 2. When StreamConnection.close() called.	(This was not closing connection in BlueCove v1.2.3)
	 * 
	 * Also Connection.close() will close Input/OutputStream
	 * @throws IOException
	 */
	void closeConnection() throws IOException {
		if ((in == null && out == null) || (closing)) {
			closing = true;
			if (handle != 0) {
				synchronized (this) {
					long h = handle;
					handle = 0;
					if (h != 0) {
						closeConnectionHandle(h);
					}
				}
			}
			// This will call this function again but will do nothing.
			// Function is not synchronized so we catch NullPointerException
			if (in != null) {
				try {
					in.close();
				} catch (NullPointerException ignore) {
				}
				in = null;
			}
			if (out != null) {
				try {
					out.close();
				} catch (NullPointerException ignore) {
				}
				out = null;
			}
		}
	}
	
	public long getRemoteAddress() throws IOException {
		return BlueCoveImpl.instance().getBluetoothStack().getConnectionRfRemoteAddress(handle);
	}

	/*
	 * Open and return an input stream for a connection. Returns: An input
	 * stream Throws: IOException - If an I/O error occurs
	 */
	public InputStream openInputStream() throws IOException {
		if (closing) {
			throw new IOException();
		} else {
			if (in == null) {
				in = new BluetoothInputStream(this);
			}
			return in;
		}
	}

	/*
	 * Open and return a data input stream for a connection. Returns: An input
	 * stream Throws: IOException - If an I/O error occurs
	 */

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	/*
	 * Open and return an output stream for a connection. Returns: An output
	 * stream Throws: IOException - If an I/O error occurs
	 */

	public OutputStream openOutputStream() throws IOException {
		if (closing) {
			throw new IOException();
		} else {
			if (out == null) {
				out = new BluetoothOutputStream(this);
			}
			return out;
		}
	}

	/*
	 * Open and return a data output stream for a connection. Returns: An output
	 * stream Throws: IOException - If an I/O error occurs
	 */

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	/*
	 * Close the connection. When a connection has been closed, access to any of
	 * its methods except this close() will cause an an IOException to be
	 * thrown. Closing an already closed connection has no effect. Streams
	 * derived from the connection may be open when method is called. Any open
	 * streams will cause the connection to be held open until they themselves
	 * are closed. In this latter case access to the open streams is permitted,
	 * but access to the connection is not.
	 * 
	 * Throws: IOException - If an I/O error occurs
	 */

	public void close() throws IOException {
		closing = true;
		closeConnection();
	}

	protected void finalize() {
		try {
			close();
		} catch (IOException e) {
		}
	}

	public int getSecurityOpt() {
		return this.securityOpt;
	}
}