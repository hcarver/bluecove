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

package com.intel.bluetooth.test;

public abstract class EnvSettings {

	public static void setSystemProperties() {
		
		//System.getProperties().put("bluecove.debug", "true");
		
		// Used to avoid refresh in Eclipse during development
		//System.getProperties().put("bluecove.native.path", "./src/main/resources");
	}
	
	public static boolean isTestAddress(String bluetoothAddress) {
		// Only one device during development tests
		if (false) {
			//return bluetoothAddress.equalsIgnoreCase("0019639c4007");
			//return bluetoothAddress.equalsIgnoreCase("00123755ae71");
		}
		return true;
	}
}
