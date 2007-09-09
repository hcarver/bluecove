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
package com.intel.bluetooth.btspp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import com.ibm.oti.connection.CreateConnection;
import com.intel.bluetooth.BluetoothConnectionAccess;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * This class is Proxy for btspp (RFCOMM) Connection implementations for IBM J9 support
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 * @author vlads
 *
 */
public class Connection implements CreateConnection, StreamConnection, StreamConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess, BluetoothConnectionAccess {

	javax.microedition.io.Connection impl;
	
	public Connection() {
		impl = null;
	}
	
	public void setParameters(String spec, int access, boolean timeout) throws IOException {
		impl = MicroeditionConnector.open(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM + ":" + spec, access, timeout);
	}

	public javax.microedition.io.Connection setParameters2(String spec, int access, boolean timeout) throws IOException {
		setParameters(spec, access, timeout);
		return this;
	}
	
	public void close() throws IOException {
		impl.close();
	}

	public DataInputStream openDataInputStream() throws IOException {
		return ((InputConnection)impl).openDataInputStream();
	}

	public InputStream openInputStream() throws IOException {
		return ((InputConnection)impl).openInputStream();
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return ((OutputConnection)impl).openDataOutputStream();
	}

	public OutputStream openOutputStream() throws IOException {
		return ((OutputConnection)impl).openOutputStream();
	}

	public StreamConnection acceptAndOpen() throws IOException {
		return ((StreamConnectionNotifier)impl).acceptAndOpen();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		return ((BluetoothConnectionNotifierServiceRecordAccess)impl).getServiceRecord();
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#updateServiceRecord(boolean)
	 */
	public void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		((BluetoothConnectionNotifierServiceRecordAccess)impl).updateServiceRecord(acceptAndOpen);
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
