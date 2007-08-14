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

import javax.bluetooth.DataElement;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;

import com.intel.bluetooth.BluetoothConsts;

/**
 * @author vlads
 *
 */
class OBEXTCPServiceRecordImpl implements ServiceRecord {

	private String host;
	
	private String port;
	
	OBEXTCPServiceRecordImpl(ServerSocketConnection notifier) {
		try {
			port = String.valueOf(notifier.getLocalPort());
			host = notifier.getLocalAddress();
		} catch (IOException e) {
			host = null;
		}
	}
	
	OBEXTCPServiceRecordImpl(SocketConnection connection) {
		try {
			port = String.valueOf(connection.getPort());
			host = connection.getAddress();
		} catch (IOException e) {
			host = null;
		}
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getConnectionURL(int, boolean)
	 */
	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster) {
		if (host == null) {
			return null;
		}
		return BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX + "://" + host + ":" + port;
	}
	
	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getAttributeIDs()
	 */
	public int[] getAttributeIDs() {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getAttributeValue(int)
	 */
	public DataElement getAttributeValue(int attrID) {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#getHostDevice()
	 */
	public RemoteDevice getHostDevice() {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#populateRecord(int[])
	 */
	public boolean populateRecord(int[] attrIDs) throws IOException {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#setAttributeValue(int, javax.bluetooth.DataElement)
	 */
	public boolean setAttributeValue(int attrID, DataElement attrValue) {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.ServiceRecord#setDeviceServiceClasses(int)
	 */
	public void setDeviceServiceClasses(int classes) {
		throw new IllegalArgumentException("Not a Bluetooth ServiceRecord");
	}

}
