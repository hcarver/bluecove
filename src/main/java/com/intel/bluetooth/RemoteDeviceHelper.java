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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connection;

/**
 * @author vlads
 *
 * Instance of RemoteDevice can be created by User.
 * BlueCove only use RemoteDeviceHelper class to create RemoteDevice instances. 
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
		
		public String toString() {
			return super.getBluetoothAddress();
		}
	}
	
	private static Hashtable devicesCashed = new Hashtable();
	
	private RemoteDeviceHelper() {
		
	}

	private static RemoteDeviceWithExtendedInfo getCashedDeviceWithExtendedInfo(long address) {
		Object key = new Long(address);
		return (RemoteDeviceWithExtendedInfo)devicesCashed.get(key);
	}
	
	static RemoteDevice getCashedDevice(long address) {
		return getCashedDeviceWithExtendedInfo(address);
	}

	static RemoteDevice createRemoteDevice(long address, String name) {
		RemoteDeviceWithExtendedInfo dev = getCashedDeviceWithExtendedInfo(address);
		if (dev == null) {
			dev = new RemoteDeviceWithExtendedInfo(address, name);
			devicesCashed.put(new Long(address), dev);
			DebugLog.debug0x("new devicesCashed", address);
		} else if (!Utils.isStringSet(dev.name)) {
			// New name found
			dev.name = name;
		} else if (Utils.isStringSet(name)) {
			// Update name if changed
			dev.name = name;
		}
		return dev;
	}
	
	static RemoteDevice createRemoteDevice(RemoteDevice device) {
		if (device instanceof RemoteDeviceWithExtendedInfo) {
			return device;
		} else {
			return createRemoteDevice(getAddress(device), null);
		}
	}
	
	public static String getFriendlyName(RemoteDevice device, long address, boolean alwaysAsk) throws IOException {
		String name = null;
		if (!(device instanceof RemoteDeviceWithExtendedInfo)) {
			device = createRemoteDevice(device);
		}
		name = ((RemoteDeviceWithExtendedInfo)device).name;
		if (alwaysAsk || (!Utils.isStringSet(name))) {
			name = BlueCoveImpl.instance().getBluetoothStack().getRemoteDeviceFriendlyName(address);
			if (Utils.isStringSet(name)) {
				((RemoteDeviceWithExtendedInfo)device).name = name;
			} else {
				throw new IOException("Can't query remote device");
			}
		}
		return name;
	}
	
	public static RemoteDevice getRemoteDevice(Connection conn) throws IOException {
		if (!(conn instanceof BluetoothConnectionAccess)) {
			throw new IllegalArgumentException("Not a Bluetooth connection " + conn.getClass().getName());
		}
		return createRemoteDevice(((BluetoothConnectionAccess)conn).getRemoteAddress(), null);
	}
	
	public static RemoteDevice[] retrieveDevices(int option) {
		switch (option) {
		case DiscoveryAgent.CACHED:
		case DiscoveryAgent.PREKNOWN:
			RemoteDevice[] devices = new RemoteDevice[devicesCashed.size()];
			int i = 0;
			for(Enumeration en = devicesCashed.elements(); en.hasMoreElements(); ) {
				devices[i++] = (RemoteDevice)en.nextElement();
			}
			return devices; 
		default:
			throw new IllegalArgumentException("invalid option");
		}
	}

	public static String getBluetoothAddress(String address) {
		String s = address.toUpperCase();
		return "000000000000".substring(s.length()) + s;
	}
	
	static String getBluetoothAddress(long address) {
		return getBluetoothAddress(Utils.toHexString(address));
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
	
	static void setStackAttributes(RemoteDevice device, Object key, Object value) {
		RemoteDeviceWithExtendedInfo devInfo = (RemoteDeviceWithExtendedInfo)createRemoteDevice(device);
		devInfo.setStackAttributes(key, value);
	}
	
	static Object getStackAttributes(RemoteDevice device, Object key) {
		RemoteDeviceWithExtendedInfo devInfo = null;
		if (device instanceof RemoteDeviceWithExtendedInfo) {
			devInfo = (RemoteDeviceWithExtendedInfo)device;
		} else {
			devInfo = getCashedDeviceWithExtendedInfo(getAddress(device));
		}
		
		if (devInfo != null) {
			return devInfo.getStackAttributes(key);
		} else {
			return null;
		}
	}

}
