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

import javax.bluetooth.ServiceRecord;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import com.ibm.oti.connection.CreateConnection;
import com.intel.bluetooth.BluetoothConnectionAccess;
import com.intel.bluetooth.BluetoothStreamServiceRecordAccess;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * This class is Proxy for different Connection implementations for IBM J9 support
 * 
 * @author vlads
 *
 */
public class Connection implements CreateConnection, StreamConnection, StreamConnectionNotifier, BluetoothStreamServiceRecordAccess, BluetoothConnectionAccess {

	javax.microedition.io.Connection impl;
	
	public final static String PROTOCOL = "btspp";
	
	public Connection() {
		
	}
	
	public void setParameters(String spec, int access, boolean timeout) throws IOException {
		impl = MicroeditionConnector.open(PROTOCOL + ":" + spec, access, timeout);
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

	public ServiceRecord getServiceRecord() {
		return ((BluetoothStreamServiceRecordAccess)impl).getServiceRecord();
	}

	public long getRemoteAddress() throws IOException {
		return ((BluetoothConnectionAccess)impl).getRemoteAddress();
	}

}
