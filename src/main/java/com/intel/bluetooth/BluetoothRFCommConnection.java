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
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.StreamConnection;

/**
 * All StreamConnections have one underlying InputStream and one OutputStream.
 * Opening a DataInputStream counts as opening an InputStream and opening a
 * DataOutputStream counts as opening an OutputStream. Trying to open another
 * InputStream or OutputStream causes an IOException. Trying to open the
 * InputStream or OutputStream after they have been closed causes an
 * IOException.
 * <p>
 * The methods of StreamConnection are not synchronized. The only stream method
 * that can be called safely in another thread is close.
 * 
 * 
 */
abstract class BluetoothRFCommConnection implements StreamConnection, BluetoothConnectionAccess {

	protected BluetoothStack bluetoothStack;

	protected volatile long handle;

	private BluetoothRFCommInputStream in;

	private BluetoothRFCommOutputStream out;

	private boolean isClosed;

	protected int securityOpt;

	RemoteDevice remoteDevice;

	protected BluetoothRFCommConnection(BluetoothStack bluetoothStack, long handle) {
		this.bluetoothStack = bluetoothStack;
		this.handle = handle;
		this.isClosed = false;
	}

	abstract void closeConnectionHandle(long handle) throws IOException;

	/**
	 * Close the connection.
	 * <p>
	 * Streams derived from the connection may be open when close() method is
	 * called. Any open streams will cause the connection to be held open until
	 * they themselves are closed. In this latter case access to the open
	 * streams is permitted, but access to the connection is not.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	void streamClosed() throws IOException {
		// Closing streams does not close connection
		if (!isClosed) {
			return;
		}

		// Any open streams will cause the connection to be held open
		if ((in != null) && (!in.isClosed())) {
			return;
		}

		if ((out != null) && (!out.isClosed())) {
			return;
		}

		shutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#shutdown()
	 */
	public void shutdown() throws IOException {
		if (handle != 0) {
			DebugLog.debug("closing RFCOMM Connection", handle);
			// close() can be called safely in another thread
			long synchronizedHandle;
			synchronized (this) {
				synchronizedHandle = handle;
				handle = 0;
			}
			if (synchronizedHandle != 0) {
				closeConnectionHandle(synchronizedHandle);
			}
		}
	}

	/**
	 * Open and return an input stream for a connection.
	 * <p>
	 * Trying to open another InputStream or OutputStream causes an IOException.
	 * <p>
	 * Trying to open the InputStream or OutputStream after they have been
	 * closed causes an IOException.
	 * 
	 * @return An input stream
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public InputStream openInputStream() throws IOException {
		if (isClosed) {
			throw new IOException("RFCOMM Connection is already closed");
		} else {
			if (in == null) {
				in = new BluetoothRFCommInputStream(this);
				return in;
			} else if (in.isClosed()) {
				throw new IOException("Stream cannot be reopened");
			} else {
				throw new IOException("Another InputStream already opened");
			}
		}
	}

	/**
	 * Open and return an data input stream for a connection.
	 * <p>
	 * Opening a DataInputStream counts as opening an InputStream
	 * <p>
	 * Trying to open another InputStream or OutputStream causes an IOException.
	 * <p>
	 * Trying to open the InputStream or OutputStream after they have been
	 * closed causes an IOException.
	 * 
	 * @return An input stream
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	/**
	 * Open and return an output stream for a connection.
	 * <p>
	 * Trying to open another InputStream or OutputStream causes an IOException.
	 * <p>
	 * Trying to open the InputStream or OutputStream after they have been
	 * closed causes an IOException.
	 * 
	 * @return An output stream
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public OutputStream openOutputStream() throws IOException {
		if (isClosed) {
			throw new IOException("RFCOMM Connection is already closed");
		} else {
			if (out == null) {
				out = new BluetoothRFCommOutputStream(this);
				return out;
			} else if (out.isClosed()) {
				throw new IOException("Stream cannot be reopened");
			} else {
				throw new IOException("Another OutputStream already opened");
			}
		}
	}

	/**
	 * Open and return an data output stream for a connection.
	 * <p>
	 * Opening a DataOutputStream counts as opening an OutputStream
	 * <p>
	 * Trying to open another InputStream or OutputStream causes an IOException.
	 * <p>
	 * Trying to open the InputStream or OutputStream after they have been
	 * closed causes an IOException.
	 * 
	 * @return An output stream
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	/**
	 * Close the connection.
	 * <p>
	 * When a connection has been closed, access to any of its methods except
	 * this close() will cause an an IOException to be thrown. Closing an
	 * already closed connection has no effect. Streams derived from the
	 * connection may be open when method is called. Any open streams will cause
	 * the connection to be held open until they themselves are closed. In this
	 * latter case access to the open streams is permitted, but access to the
	 * connection is not.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public void close() throws IOException {
		if (isClosed) {
			return;
		}
		isClosed = true;
		streamClosed();
	}

	protected void finalize() {
		try {
			close();
		} catch (IOException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#isClosed()
	 */
	public boolean isClosed() {
		return isClosed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#markAuthenticated()
	 */
	public void markAuthenticated() {
		if (this.securityOpt == ServiceRecord.NOAUTHENTICATE_NOENCRYPT) {
			this.securityOpt = ServiceRecord.AUTHENTICATE_NOENCRYPT;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getSecurityOpt()
	 */
	public int getSecurityOpt() {
		try {
			this.securityOpt = bluetoothStack.rfGetSecurityOpt(this.handle, this.securityOpt);
		} catch (IOException notChanged) {
		}
		return this.securityOpt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#encrypt(boolean)
	 */
	public boolean encrypt(long address, boolean on) throws IOException {
		if (isClosed) {
			throw new IOException("RFCOMM Connection is already closed");
		}
		boolean changed = bluetoothStack.rfEncrypt(address, this.handle, on);
		if (changed) {
			if (on) {
				this.securityOpt = ServiceRecord.AUTHENTICATE_ENCRYPT;
			} else {
				this.securityOpt = ServiceRecord.AUTHENTICATE_NOENCRYPT;
			}
		}
		return changed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteAddress()
	 */
	public long getRemoteAddress() throws IOException {
		if (isClosed) {
			throw new IOException("Connection closed");
		}
		return bluetoothStack.getConnectionRfRemoteAddress(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteDevice()
	 */
	public RemoteDevice getRemoteDevice() {
		return this.remoteDevice;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#setRemoteDevice(javax.bluetooth.RemoteDevice)
	 */
	public void setRemoteDevice(RemoteDevice remoteDevice) {
		this.remoteDevice = remoteDevice;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getBluetoothStack()
	 */
	public BluetoothStack getBluetoothStack() {
		return bluetoothStack;
	}
}