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
package com.intel.bluetooth.btl2cap;

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import com.ibm.oti.connection.CreateConnection;
import com.intel.bluetooth.BluetoothConnectionAccess;
import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * This class is Proxy for btl2cap (L2CAP) Connection implementations for IBM J9 support
 * 
 * @author vlads
 *
 */
public class Connection implements CreateConnection, L2CAPConnection, L2CAPConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess, BluetoothConnectionAccess{

	javax.microedition.io.Connection impl;
	
	public Connection() {
		
	}

	/* (non-Javadoc)
	 * @see com.ibm.oti.connection.CreateConnection#setParameters(java.lang.String, int, boolean)
	 */
	public void setParameters(String spec, int access, boolean timeout) throws IOException {
		impl = MicroeditionConnector.open(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM + ":" + spec, access, timeout);
	}

	/* (non-Javadoc)
	 * @see com.ibm.oti.connection.CreateConnection#setParameters2(java.lang.String, int, boolean)
	 */
	public void setParameters2(String spec, int access, boolean timeout) throws IOException {
		setParameters(spec, access, timeout);
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		impl.close();
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#getReceiveMTU()
	 */
	public int getReceiveMTU() throws IOException {
		return ((L2CAPConnection)impl).getReceiveMTU();
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#getTransmitMTU()
	 */
	public int getTransmitMTU() throws IOException {
		return ((L2CAPConnection)impl).getTransmitMTU();
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#ready()
	 */
	public boolean ready() throws IOException {
		return ((L2CAPConnection)impl).ready();
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#receive(byte[])
	 */
	public int receive(byte[] inBuf) throws IOException {
		return ((L2CAPConnection)impl).receive(inBuf);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnection#send(byte[])
	 */
	public void send(byte[] data) throws IOException {
		((L2CAPConnection)impl).send(data);
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public L2CAPConnection acceptAndOpen() throws IOException {
		return ((L2CAPConnectionNotifier)impl).acceptAndOpen();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		return ((BluetoothConnectionNotifierServiceRecordAccess)impl).getServiceRecord();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteAddress()
	 */
	public long getRemoteAddress() throws IOException {
		return ((BluetoothConnectionAccess)impl).getRemoteAddress();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteDevice()
	 */
	public RemoteDevice getRemoteDevice() {
		return ((BluetoothConnectionAccess)impl).getRemoteDevice();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getSecurityOpt()
	 */
	public int getSecurityOpt() {
		return ((BluetoothConnectionAccess)impl).getSecurityOpt();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#setRemoteDevice(javax.bluetooth.RemoteDevice)
	 */
	public void setRemoteDevice(RemoteDevice remoteDevice) {
		((BluetoothConnectionAccess)impl).setRemoteDevice(remoteDevice);
	}

}
