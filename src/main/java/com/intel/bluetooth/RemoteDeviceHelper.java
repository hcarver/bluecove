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
 * Instance of RemoteDevice can be created by User.
 * BlueCove only use RemoteDeviceHelper class. 
 */
public abstract class RemoteDeviceHelper {
	
	private static class RemoteDeviceWithExtendedInfo extends RemoteDevice {
		
		private String name;
		
		private long addressLong;
		
		private Hashtable stackAttributes;
		
		private RemoteDeviceWithExtendedInfo(long address, String name) {
			super(RemoteDeviceHelper.getBluetoothAddress(address));
			this.name = name;
			this.addressLong = address;
		}
		
		private void setStackAttributes(Object key, Object value) {
			if (stackAttributes == null) {
				stackAttributes = new Hashtable();
			}
			if (value == null) {
				stackAttributes.remove(key);
			} else {
				stackAttributes.put(key, value);
			}
		}
		
		private Object getStackAttributes(Object key) {
			if (stackAttributes == null) {
				return null;
			}
			return stackAttributes.get(key);
		}
	}
	
	private RemoteDeviceHelper() {
		
	}
	
	static RemoteDevice createRemoteDevice(long address, String name) {
		RemoteDevice dev = new RemoteDeviceWithExtendedInfo(address, name);
		return dev;
	}
	
	public static String getFriendlyName(RemoteDevice device, long address, boolean alwaysAsk) throws IOException {
		String name = null;
		if (device instanceof RemoteDeviceWithExtendedInfo) {
			name = ((RemoteDeviceWithExtendedInfo)device).name;
		} else {
			// TODO
		}
		if (alwaysAsk || name == null || name.equals("")) {
			name = BlueCoveImpl.instance().getBluetoothStack().getRemoteDeviceFriendlyName(address);
		}
		return name;
	}
	
	public static RemoteDevice getRemoteDevice(Connection conn) throws IOException {
		if (!(conn instanceof BluetoothRFCommConnection)) {
			throw new IllegalArgumentException("Not a Bluetooth connection");
		}
		return createRemoteDevice(((BluetoothRFCommConnection)conn).getRemoteAddress(), null);
	}

	static String getBluetoothAddress(long address) {
		return Long.toHexString(address).toUpperCase();
	}
	
	public static long getAddress(String bluetoothAddress) {
		return Long.parseLong(bluetoothAddress, 16);
	}
	
	static long getAddress(RemoteDevice device) {
		if (device instanceof RemoteDeviceWithExtendedInfo) {
			return ((RemoteDeviceWithExtendedInfo)device).addressLong;
		} else {
			return getAddress(device.getBluetoothAddress());
		}
	}
	
	private static RemoteDeviceWithExtendedInfo getCashedDevice(RemoteDevice device) {
		return null;
	}
	
	static void setStackAttributes(RemoteDevice device, Object key, Object value) {
	}
	
	static Object getStackAttributes(RemoteDevice device, Object key) {
		RemoteDeviceWithExtendedInfo devInfo = null;
		if (device instanceof RemoteDeviceWithExtendedInfo) {
			devInfo = (RemoteDeviceWithExtendedInfo)device;
		} else {
			devInfo = getCashedDevice(device);
		}
		
		if (devInfo != null) {
			return devInfo.getStackAttributes(key);
		} else {
			return null;
		}
	}

}
