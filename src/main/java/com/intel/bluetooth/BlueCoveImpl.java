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

	public static final int versionMajor = 2;
	
	public static final int versionMinor = 0;
	
	public static final int versionBuild = 0;
	
	public static final String version = String.valueOf(versionMajor) + "." + String.valueOf(versionMinor) + "." + String.valueOf(versionBuild) + "-SNAPSHOT";

	public static final int nativeLibraryVersionExpected = versionMajor * 10000 + versionMinor * 100 + versionBuild;
	
	public static final String STACK_WINSOCK = "winsock";
	
	public static final String STACK_WIDCOMM = "widcomm";
	
	public static final String STACK_BLUESOLEIL = "bluesoleil";
	
	// We can't use the same DLL on windows for all implemenations.
	
	public static final String NATIVE_LIB_MS = "intelbth";
	
	public static final String NATIVE_LIB_WC_BS = "bluecove";
	
	private BluetoothStack bluetoothStack;
	
    /**
     * Allow default initialization.
     * In Secure environment instance() should be called initialy from secure contex.
     */
    private static class SingletonHolder {
        private static BlueCoveImpl instance = new BlueCoveImpl();
    }

	private BlueCoveImpl() {
		
		BluetoothStack detectorStack;
		if (NativeLibLoader.isAvailable(NATIVE_LIB_MS)) {
			detectorStack = new BluetoothStackMicrosoft();
		} else if (NativeLibLoader.isAvailable(NATIVE_LIB_WC_BS)) {
			detectorStack = new BluetoothStackWIDCOMM();
		} else {
			return;
		}
		
		int libraryVersion = detectorStack.getLibraryVersion();
		if (nativeLibraryVersionExpected != libraryVersion) {
			DebugLog.fatal("BlueCove native library version mismatch " + libraryVersion + " expected " + nativeLibraryVersionExpected);
			return;
		}
		
		String stack = System.getProperty("bluecove.stack");
		if (stack == null) {
			//auto detect
			int aval = detectorStack.detectBluetoothStack();
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
		
		// bluetoothStack.destroy(); May stuck in WIDCOMM forever. Exit JVM anyway.
		final ShutdownHookThread shutdownHookThread = new ShutdownHookThread();
		shutdownHookThread.start();
		
		Runnable r = new Runnable() {
			public void run() {
				synchronized (shutdownHookThread) {
					shutdownHookThread.notifyAll();
					try {
						shutdownHookThread.wait(7000);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		
		try {
			// since Java 1.3
			Runtime.getRuntime().addShutdownHook(new Thread(r));
		} catch (Throwable java12) {
		}
		
		System.out.println("BlueCove version " + version + " on " + stack);
	}
	
	private class ShutdownHookThread extends Thread {
		
		ShutdownHookThread() {
			super("BluecoveShutdownHookThread");
			this.setDaemon(true);
		}
		
		public void run() {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					return;
				}
			}
			bluetoothStack.destroy();
			System.out.println("BlueCove stack shutdown completed");
			synchronized (this) {
				this.notifyAll();
			}
		}
		
	}

    public static BlueCoveImpl instance() {
		return SingletonHolder.instance;
    }

    public String setBluetoothStack(String stack) {
    	if (bluetoothStack != null) {
    		bluetoothStack.destroy();
    		bluetoothStack = null;
    	}
    	BluetoothStack newStack;
    	if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
    		newStack = new BluetoothStackWIDCOMM();
			stack = STACK_WIDCOMM;
		} else if (STACK_BLUESOLEIL.equalsIgnoreCase(stack)) {
			newStack = new BluetoothStackBlueSoleil();
			stack = STACK_BLUESOLEIL;
		} else {
			newStack = new BluetoothStackMicrosoft();
			stack = STACK_WINSOCK;
		}
    	int libraryVersion = newStack.getLibraryVersion();
		if (nativeLibraryVersionExpected != libraryVersion) {
			DebugLog.fatal("BlueCove native library version mismatch " + libraryVersion + " expected " + nativeLibraryVersionExpected);
			return null;
		}
		
    	if (DebugLog.isDebugEnabled()) {
    		newStack.enableNativeDebug(DebugLog.class, true);
		}
    	newStack.initialize();
    	bluetoothStack = newStack;
    	return stack;
    }
    
    public void enableNativeDebug(boolean on) {
    	if (bluetoothStack != null) {
    		bluetoothStack.enableNativeDebug(DebugLog.class, on);
    	}
    }
    
	public BluetoothStack getBluetoothStack() {
		if (bluetoothStack == null) {
			throw new Error("BlueCove not avalable");
		}
		return bluetoothStack;
	}

}
