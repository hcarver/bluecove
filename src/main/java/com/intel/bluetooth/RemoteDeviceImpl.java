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
import java.util.Hashtable;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connection;

/**
 * @author vlads
 *
 * BlueCove only creates this class. Instance of RemoteDevice should not be created
 */
public class RemoteDeviceImpl extends RemoteDevice {
	
	String name;
	
	private long address;
	
	private Hashtable stackAttributes;
	
	protected RemoteDeviceImpl(long address, String name) {
		super(Long.toHexString(address));
		this.name = name;
		this.address = address;
	}
	
	public String getFriendlyName(boolean alwaysAsk) throws IOException {
		if (alwaysAsk || name == null || name.equals("")) {
			name = BlueCoveImpl.instance().getBluetoothStack().getRemoteDeviceFriendlyName(address);
		}
		return name;
	}
	
	public static RemoteDevice getRemoteDevice(Connection conn) throws IOException {
		if (!(conn instanceof BluetoothRFCommConnection)) {
			throw new IllegalArgumentException("Not a Bluetooth connection");
		}
		return new RemoteDeviceImpl(((BluetoothRFCommConnection)conn).getRemoteAddress(), null);
	}

	public long getAddress() {
		return address;
	}
	
	void setStackAttributes(Object key, Object value) {
		if (stackAttributes == null) {
			stackAttributes = new Hashtable();
		}
		if (value == null) {
			stackAttributes.remove(key);
		} else {
			stackAttributes.put(key, value);
		}
	}
	
	Object getStackAttributes(Object key) {
		if (stackAttributes == null) {
			return null;
		}
		return stackAttributes.get(key);
	}
}
