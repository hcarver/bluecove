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
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.RemoteDevice;

/**
 * @author vlads
 * 
 */
abstract class BluetoothL2CAPConnection implements L2CAPConnection, BluetoothConnectionAccess {

	protected BluetoothStack bluetoothStack;

	protected volatile long handle;

	protected int securityOpt;

	private RemoteDevice remoteDevice;

	private boolean isClosed;

	protected BluetoothL2CAPConnection(BluetoothStack bluetoothStack, long handle) {
		this.bluetoothStack = bluetoothStack;
		this.handle = handle;
		this.isClosed = false;
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
		return bluetoothStack.l2RemoteAddress(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
	 */
	public int getReceiveMTU() throws IOException {
		if (isClosed) {
			throw new IOException("Connection closed");
		}
		return bluetoothStack.l2GetReceiveMTU(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
	 */
	public int getTransmitMTU() throws IOException {
		if (isClosed) {
			throw new IOException("Connection closed");
		}
		return bluetoothStack.l2GetTransmitMTU(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.bluetooth.L2CAPConnection#ready()
	 */
	public boolean ready() throws IOException {
		if (isClosed) {
			throw new IOException("Connection closed");
		}
		return bluetoothStack.l2Ready(handle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.bluetooth.L2CAPConnection#receive(byte[])
	 */
	public int receive(byte[] inBuf) throws IOException {
		if (isClosed) {
			throw new IOException("Connection closed");
		}
		if (inBuf == null) {
			throw new NullPointerException("inBuf is null");
		}
		return bluetoothStack.l2Receive(handle, inBuf);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.bluetooth.L2CAPConnection#send(byte[])
	 */
	public void send(byte[] data) throws IOException {
		if (isClosed) {
			throw new IOException("Connection closed");
		}
		if (data == null) {
			throw new NullPointerException("data is null");
		}
		bluetoothStack.l2Send(handle, data);
	}

	abstract void closeConnectionHandle(long handle) throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (isClosed) {
			return;
		}

		isClosed = true;
		DebugLog.debug("closing L2CAP Connection");

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

	protected void finalize() {
		try {
			close();
		} catch (IOException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getSecurityOpt()
	 */
	public int getSecurityOpt() {
		return this.securityOpt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#encrypt(boolean)
	 * @see javax.bluetooth.RemoteDevice#encrypt(Connection , boolean)
	 */
	public boolean encrypt(boolean on) {
		return false;
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
