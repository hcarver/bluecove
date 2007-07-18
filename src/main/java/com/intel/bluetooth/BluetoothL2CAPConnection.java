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

/**
 * @author vlads
 *
 */
public class BluetoothL2CAPConnection implements L2CAPConnection, BluetoothConnectionAccess {

	protected long handle;
	
	protected int receiveMTU = DEFAULT_MTU;
	
	protected int transmitMTU = DEFAULT_MTU;
	
	protected BluetoothL2CAPConnection(long handle) {
		this.handle = handle;
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteAddress()
	 */
	public long getRemoteAddress() throws IOException {
		return BlueCoveImpl.instance().getBluetoothStack().getConnectionRfRemoteAddress(handle);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
	 */
	public int getReceiveMTU() throws IOException {
		return receiveMTU;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
	 */
	public int getTransmitMTU() throws IOException {
		return transmitMTU;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#ready()
	 */
	public boolean ready() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#receive(byte[])
	 */
	public int receive(byte[] inBuf) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#send(byte[])
	 */
	public void send(byte[] data) throws IOException {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
