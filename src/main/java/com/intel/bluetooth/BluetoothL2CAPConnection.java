/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

	protected volatile long handle;
	
	protected int securityOpt;
	
	RemoteDevice remoteDevice;
	
	protected boolean closing = false;
	
	protected BluetoothL2CAPConnection(long handle) {
		this.handle = handle;
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteAddress()
	 */
	public long getRemoteAddress() throws IOException {
		if (closing) {
			throw new IOException("Connection closed");
		}
		return BlueCoveImpl.instance().getBluetoothStack().l2RemoteAddress(handle);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
	 */
	public int getReceiveMTU() throws IOException {
		if (closing) {
			throw new IOException("Connection closed");
		}
		return BlueCoveImpl.instance().getBluetoothStack().l2GetReceiveMTU(handle);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
	 */
	public int getTransmitMTU() throws IOException {
		if (closing) {
			throw new IOException("Connection closed");
		}
		return BlueCoveImpl.instance().getBluetoothStack().l2GetTransmitMTU(handle);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#ready()
	 */
	public boolean ready() throws IOException {
		if (closing) {
			throw new IOException("Connection closed");
		}
		return BlueCoveImpl.instance().getBluetoothStack().l2Ready(handle);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#receive(byte[])
	 */
	public int receive(byte[] inBuf) throws IOException {
		if (closing) {
			throw new IOException("Connection closed");
		}
		if (inBuf == null) {
			throw new NullPointerException ("inBuf is null");
		}
		return BlueCoveImpl.instance().getBluetoothStack().l2Receive(handle, inBuf);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#send(byte[])
	 */
	public void send(byte[] data) throws IOException {
		if (closing) {
			throw new IOException("Connection closed");
		}
		if (data == null) {
			throw new NullPointerException ("data is null");
		}
		BlueCoveImpl.instance().getBluetoothStack().l2Send(handle, data);
	}

	abstract void closeConnectionHandle(long handle) throws IOException;
	
	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (!closing) {
			closing = true;
			long h = handle;
			handle = 0;
			closeConnectionHandle(h);
		}
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

	public RemoteDevice getRemoteDevice() {
		return this.remoteDevice;
	}

	public void setRemoteDevice(RemoteDevice remoteDevice) {
		this.remoteDevice = remoteDevice;
	}

}
