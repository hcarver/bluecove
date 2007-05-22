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

	public static final String STACK_WINSOCK = "winsock";
	
	public static final String STACK_WIDCOMM = "widcomm";
	
	public static final String STACK_BLUESOLEIL = "bluesoleil";
	
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
			//auto detect
			int aval = BluetoothPeer.detectBluetoothStack();
			DebugLog.debug("BluetoothStack detected", aval);
			if ((aval & 1) != 0) {
				stack = STACK_WINSOCK;
			} else if ((aval & 2) != 0) {
				stack = STACK_WIDCOMM;
			} else if ((aval & 4) != 0) {
				stack = STACK_BLUESOLEIL;
			} else {
				DebugLog.fatal("BluetoothStack not detected");
				throw new RuntimeException("BluetoothStack not detected");
			}
		}
		
		stack = setBluetoothStack(stack);
		
		Runnable r = new Runnable() {
			public void run() {
				bluetoothStack.destroy();
				System.out.println("BlueCove stack shutdown completed");
			}
		};
		
		try {
			// since Java 1.3
			Runtime.getRuntime().addShutdownHook(new Thread(r));
		} catch (Throwable java12) {
		}
		
		System.out.println("BlueCove version " + version + " on " + stack);
	}

    public static BlueCoveImpl instance() {
		return SingletonHolder.instance;
    }

    public String setBluetoothStack(String stack) {
    	if (bluetoothStack != null) {
    		bluetoothStack.destroy();
    		bluetoothStack = null;
    	}
    	if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
			bluetoothStack = new BluetoothStackWIDCOMM();
			stack = STACK_WIDCOMM;
		} else if (STACK_BLUESOLEIL.equalsIgnoreCase(stack)) {
			bluetoothStack = new BluetoothStackBlueSoleil();
			stack = STACK_BLUESOLEIL;
		} else {
			bluetoothStack = new BluetoothStackMicrosoft();
			stack = STACK_WINSOCK;
		}
    	return stack;
    }
    
    public BluetoothPeer getBluetoothPeer() {
		return bluetoothPeer;
	}

	public BluetoothStack getBluetoothStack() {
		return bluetoothStack;
	}

}
