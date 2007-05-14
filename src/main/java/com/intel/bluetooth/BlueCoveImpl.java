/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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

/**
 *
 * Singleton class used as holder for BluetoothPeer instead of LocalDevice
 *
 * All you need to do is initialize BlueCoveImpl inside Privileged context.
 *
 * n/a: Also this class hold Secirity Context to enable work in webstart applications.
 *
 * @author vlads
 *
 */
public class BlueCoveImpl {

	public static final String version = "2.0.0-SNAPSHOT";
	
	public static final String STACK_WIDCOMM = "widcomm";
	
	public static final String STACK_WINSOCK = "winsock";
	
	private BluetoothPeer bluetoothPeer;

	private BluetoothStack bluetoothStack;
	
    /**
     * Allow default initialization.
     * In Secure environment instance() should be called initialy from secure contex.
     */
    private static class SingletonHolder {
        private static BlueCoveImpl instance = new BlueCoveImpl();
    }

	private BlueCoveImpl() {
		bluetoothPeer = new BluetoothPeer();
		
		String stack = System.getProperty("bluecove.stack");
		if (stack == null) {
			//stack = "WIDCOMM";
		}
		if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
			bluetoothStack = new BluetoothStackWIDCOMM();
			stack = STACK_WIDCOMM;
		} else {
			bluetoothStack = new BluetoothStackMicrosoft();
			stack = STACK_WINSOCK;
		}
		System.out.println("BlueCove version " + version + " on " + stack);
	}

    public static BlueCoveImpl instance() {
		return SingletonHolder.instance;
    }

    public BluetoothPeer getBluetoothPeer() {
		return bluetoothPeer;
	}

	public BluetoothStack getBluetoothStack() {
		return bluetoothStack;
	}

}
