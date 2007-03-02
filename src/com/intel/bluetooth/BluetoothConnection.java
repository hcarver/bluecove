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

import javax.bluetooth.LocalDevice;
import javax.microedition.io.StreamConnection;

public class BluetoothConnection implements StreamConnection {
	
	int socket;

	long address;

	BluetoothInputStream in;

	BluetoothOutputStream out;

	private boolean closing;

	private boolean closed;

	synchronized void closeSocket() throws IOException {
		if (in == null && out == null && closing && !closed) {
			(LocalDevice.getLocalDevice()).getBluetoothPeer().close(socket);
			closed = true;
		}
	}

	public BluetoothConnection(long address, int channel, boolean authenticate,
			boolean encrypt) throws IOException {
		this.address = address;
		BluetoothPeer peer = (LocalDevice.getLocalDevice()).getBluetoothPeer();

		socket = peer.socket(authenticate, encrypt);

		peer.connect(socket, address, channel);
	}

	/** Construct BluetoothConnection with pre-existing socket */
	protected BluetoothConnection(int socket) {
		this.socket = socket;
		try {
			this.address = LocalDevice.getLocalDevice().getBluetoothPeer()
					.getsockaddress(socket);
		} catch (IOException e) {
		}
	}

	public long getAddress() {
		return address;
	}

	public long getRemoteAddress() throws IOException {
		return LocalDevice.getLocalDevice().getBluetoothPeer().getpeeraddress(
				socket);
	}

	/*
	 * Open and return an input stream for a connection. Returns: An input
	 * stream Throws: IOException - If an I/O error occurs
	 */
	public InputStream openInputStream() throws IOException {
		if (closing)
			throw new IOException();
		else {
			if (in == null)
				in = new BluetoothInputStream(this);

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
		if (closing)
			throw new IOException();
		else {
			if (out == null)
				out = new BluetoothOutputStream(this);

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

		closeSocket();
	}

	protected void finalize() {
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();

			close();
		} catch (IOException e) {
		}
	}
}