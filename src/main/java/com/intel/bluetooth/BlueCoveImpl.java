/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package com.intel.bluetooth;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;

/**
 * 
 * Singleton class used as holder for BluetoothStack.
 * 
 * All you need to do is initialize BlueCoveImpl inside Privileged context.
 * <p>
 * If automatic Bluetooth Stack detection is not enough Java System property
 * "bluecove.stack" can be used to force desired Stack Initialization. Values
 * "widcomm", "bluesoleil" or "winsock". By default winsock is selected if
 * available.
 * <p>
 * Another property "bluecove.stack.first" is used optimize stack detection. If
 * -Dbluecove.stack.first=widcomm then widcomm (bluecove.dll) stack is loaded
 * first and if not available then BlueCove will switch to winsock. By default
 * intelbth.dll is loaded first.
 * <p>
 * If multiple stacks are detected they are selected in following order:
 * "winsock", "widcomm", "bluesoleil". Since BlueCove v2.0.1
 * "bluecove.stack.first" will alter the order of stack selection.
 * <p>
 * If System property is not an option (e.g. when running in Webstart) create
 * text file "bluecove.stack" or "bluecove.stack.first" containing stack name
 * and add this file to BlueCove or Application jar. (Since v2.0.1)
 * <p>
 * Use `LocalDevice.getProperty("bluecove.stack")` to find out which stack is
 * used.
 * 
 * @author vlads
 * 
 */
public class BlueCoveImpl {

	public static final int versionMajor1 = 2;

	public static final int versionMajor2 = 0;

	public static final int versionMinor = 3;

	public static final int versionBuild = 0;

	public static final String versionSufix = "-SNAPSHOT"; // SNAPSHOT

	public static final String version = String.valueOf(versionMajor1) + "." + String.valueOf(versionMajor2) + "."
			+ String.valueOf(versionMinor) + versionSufix;

	private static final int nativeLibraryVersionExpected = versionMajor1 * 1000000 + versionMajor2 * 10000
			+ versionMinor * 100 + versionBuild;

	public static final String STACK_WINSOCK = "winsock";

	public static final String STACK_WIDCOMM = "widcomm";

	public static final String STACK_BLUESOLEIL = "bluesoleil";

	public static final String STACK_TOSHIBA = "toshiba";

	public static final String STACK_BLUEZ = "bluez";

	public static final String STACK_OSX = "mac";

	public static final String STACK_EMULATOR = "emulator";

	// We can't use the same DLL on windows for all implementations.
	// Since WIDCOMM need to be compile /MD using VC6 and winsock /MT using
	// VC2005
	// This variable can be used to simplify development/test builds
	private static final boolean oneDLLbuild = false;

	public static final String NATIVE_LIB_MS = "intelbth";

	public static final String NATIVE_LIB_WIDCOMM = oneDLLbuild ? NATIVE_LIB_MS : "bluecove";

	public static final String NATIVE_LIB_TOSHIBA = "bluecove";

	public static final String NATIVE_LIB_BLUEZ = "bluecove";

	public static final String NATIVE_LIB_OSX = "bluecove";

	/**
	 * To work on BlueSoleil version 2.3 we need to compile C++ code /MT the
	 * same as winsock.
	 */
	public static final String NATIVE_LIB_BLUESOLEIL = NATIVE_LIB_MS;

	private BluetoothStack bluetoothStack;

	static final int BLUECOVE_STACK_DETECT_MICROSOFT = 1;

	static final int BLUECOVE_STACK_DETECT_WIDCOMM = 1 << 1;

	static final int BLUECOVE_STACK_DETECT_BLUESOLEIL = 1 << 2;

	static final int BLUECOVE_STACK_DETECT_TOSHIBA = 1 << 3;

	static final int BLUECOVE_STACK_DETECT_OSX = 1 << 4;

	public static final int BLUECOVE_STACK_DETECT_BLUEZ = 1 << 5;

	static final int BLUECOVE_STACK_DETECT_EMULATOR = 1 << 6;

	private static Hashtable configProperty = new Hashtable();

	private static final String FQCN = BlueCoveImpl.class.getName();

	private static final Vector fqcnSet = new Vector();

	/* The context to be used when loading native DLL */
	private Object accessControlContext;

	private boolean shutdownHookCreated;

	private static BlueCoveImpl instance;

	static {
		fqcnSet.addElement(FQCN);
	}

	/**
	 * bluetoothStack.destroy(); May stuck forever. Exit JVM anyway after
	 * timeout.
	 */
	private class AsynchronousShutdownThread extends Thread {

		Object monitor = new Object();

		AsynchronousShutdownThread() {
			super("BluecoveAsynchronousShutdownThread");
		}

		public void run() {
			synchronized (monitor) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					return;
				}
			}
			if (bluetoothStack != null) {
				bluetoothStack.destroy();
				bluetoothStack = null;
			}
			System.out.println("BlueCove stack shutdown completed");
			synchronized (monitor) {
				monitor.notifyAll();
			}
		}
	}

	private class ShutdownHookThread extends Thread {

		private Object monitor;

		ShutdownHookThread(Object monitor) {
			super("BluecoveShutdownHookThread");
			this.monitor = monitor;
		}

		public void run() {
			synchronized (monitor) {
				monitor.notifyAll();
				if (bluetoothStack != null) {
					try {
						monitor.wait(7000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	/**
	 * Applications should not used this function. Allow default initialization.
	 * In Secure environment instance() should be called initially from secure
	 * context.
	 * 
	 * @return Instance of the class, getBluetoothStack() can be called.
	 */
	public static synchronized BlueCoveImpl instance() {
		if (instance == null) {
			instance = new BlueCoveImpl();
		}
		return instance;
	}

	private BlueCoveImpl() {
		try {
			accessControlContext = AccessController.getContext();
		} catch (Throwable javaME) {
		}
		// Initialization in WebStart.
		DebugLog.isDebugEnabled();
	}

	static int getNativeLibraryVersion() {
		return nativeLibraryVersionExpected;
	}

	private synchronized void createShutdownHook() {
		if (shutdownHookCreated) {
			return;
		}
		shutdownHookCreated = true;
		AsynchronousShutdownThread shutdownHookThread = new AsynchronousShutdownThread();
		UtilsJavaSE.threadSetDaemon(shutdownHookThread);
		shutdownHookThread.start();
		try {
			// since Java 1.3
			UtilsJavaSE.runtimeAddShutdownHook(new ShutdownHookThread(shutdownHookThread.monitor));
		} catch (Throwable java12) {
		}
	}

	private int getStackId(String stack) {
		if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_WIDCOMM;
		} else if (STACK_BLUESOLEIL.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_BLUESOLEIL;
		} else if (STACK_TOSHIBA.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_TOSHIBA;
		} else if (STACK_WINSOCK.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_MICROSOFT;
		} else if (STACK_BLUEZ.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_BLUEZ;
		} else if (STACK_WINSOCK.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_OSX;
		} else if (STACK_EMULATOR.equalsIgnoreCase(stack)) {
			return BLUECOVE_STACK_DETECT_EMULATOR;
		} else {
			return 0;
		}
	}

	private Class loadStackClass(String classPropertyName, String classNameDefault) throws BluetoothStateException {
		String className = getConfigProperty(classPropertyName);
		if (className == null) {
			className = classNameDefault;
		}
		try {
			Class c = Class.forName(className);
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			DebugLog.error(className, e);
		}
		throw new BluetoothStateException("BlueCove " + className + " not available");
	}

	private BluetoothStack newStackInstance(Class ctackClass) throws BluetoothStateException {
		String className = ctackClass.getName();
		try {
			return (BluetoothStack) ctackClass.newInstance();
		} catch (InstantiationException e) {
			DebugLog.error(className, e);
		} catch (IllegalAccessException e) {
			DebugLog.error(className, e);
		}
		throw new BluetoothStateException("BlueCove " + className + " can't instantiate");
	}

	private BluetoothStack loadStack(String classPropertyName, String classNameDefault) throws BluetoothStateException {
		return newStackInstance(loadStackClass(classPropertyName, classNameDefault));
	}

	private void detectStack() throws BluetoothStateException {

		BluetoothStack detectorStack = null;

		String stackFirstDetector = getConfigProperty("bluecove.stack.first");

		String stackSelected = getConfigProperty("bluecove.stack");

		if (stackFirstDetector == null) {
			stackFirstDetector = stackSelected;
		}
		if (STACK_EMULATOR.equals(stackSelected)) {
			detectorStack = loadStack("bluecove.emulator.class", "com.intel.bluetooth.BluetoothEmulator");
		} else {
			switch (NativeLibLoader.getOS()) {
			case NativeLibLoader.OS_LINUX:
				Class stackClass = loadStackClass("bluecove.bluez.class", "com.intel.bluetooth.BluetoothStackBlueZ");
				if (!NativeLibLoader.isAvailable(NATIVE_LIB_BLUEZ, stackClass)) {
					throw new BluetoothStateException("BlueCove not available");
				}
				detectorStack = newStackInstance(stackClass);
				stackSelected = detectorStack.getStackID();
				break;
			case NativeLibLoader.OS_MAC_OS_X:
				if (!NativeLibLoader.isAvailable(NATIVE_LIB_OSX)) {
					throw new BluetoothStateException("BlueCove not available");
				}
				detectorStack = new BluetoothStackOSX();
				stackSelected = detectorStack.getStackID();
				break;
			case NativeLibLoader.OS_WINDOWS:
			case NativeLibLoader.OS_WINDOWS_CE:
				detectorStack = createDetectorOnWindows(stackFirstDetector);
				if (DebugLog.isDebugEnabled()) {
					detectorStack.enableNativeDebug(DebugLog.class, true);
				}
				break;
			default:
				throw new BluetoothStateException("BlueCove not available");

			}
		}

		int libraryVersion = detectorStack.getLibraryVersion();
		if (nativeLibraryVersionExpected != libraryVersion) {
			DebugLog.fatal("BlueCove native library version mismatch " + libraryVersion + " expected "
					+ nativeLibraryVersionExpected);
			throw new BluetoothStateException("BlueCove native library version mismatch");
		}

		if (stackSelected == null) {
			// auto detect
			int aval = detectorStack.detectBluetoothStack();
			DebugLog.debug("BluetoothStack detected", aval);
			int detectorID = getStackId(detectorStack.getStackID());
			if ((aval & detectorID) != 0) {
				stackSelected = detectorStack.getStackID();
			} else if ((aval & BLUECOVE_STACK_DETECT_MICROSOFT) != 0) {
				stackSelected = STACK_WINSOCK;
			} else if ((aval & BLUECOVE_STACK_DETECT_WIDCOMM) != 0) {
				stackSelected = STACK_WIDCOMM;
			} else if ((aval & BLUECOVE_STACK_DETECT_BLUESOLEIL) != 0) {
				stackSelected = STACK_BLUESOLEIL;
			} else if ((aval & BLUECOVE_STACK_DETECT_TOSHIBA) != 0) {
				stackSelected = STACK_TOSHIBA;
			} else if ((aval & BLUECOVE_STACK_DETECT_OSX) != 0) {
				stackSelected = STACK_OSX;
			} else {
				DebugLog.fatal("BluetoothStack not detected");
				throw new BluetoothStateException("BluetoothStack not detected");
			}
		} else {
			DebugLog.debug("BluetoothStack selected", stackSelected);
		}

		stackSelected = setBluetoothStack(stackSelected, detectorStack);

		copySystemProperty();
		System.out.println("BlueCove version " + version + " on " + stackSelected);
	}

	/**
	 * API that can be used to configure BlueCove properties instead of System
	 * properties. Should be used before stack initialized. If <code>null</code>
	 * is passed as the <code>value</code> then the property will be removed.
	 * 
	 * @param key
	 * @param value
	 * 
	 * @exception IllegalArgumentException
	 *                if the stack already initialized.
	 */
	public static void setConfigProperty(String key, String value) {
		if (instance != null) {
			throw new IllegalArgumentException("BlueCove Stack already initialized");
		}
		if (value == null) {
			configProperty.remove(key);
		} else {
			configProperty.put(key, value);
		}
	}

	static String getConfigProperty(String key) {
		String value = (String) configProperty.get(key);
		if (value == null) {
			try {
				value = System.getProperty(key);
			} catch (SecurityException webstart) {
			}
		}
		if (value == null) {
			value = Utils.getResourceProperty(BlueCoveImpl.class, key);
		}
		return value;
	}

	void copySystemProperty() {
		if (bluetoothStack != null) {
			UtilsJavaSE.setSystemProperty("bluetooth.api.version", "1.1");
			UtilsJavaSE.setSystemProperty("obex.api.version", "1.1");
			String[] property = { "bluetooth.master.switch", "bluetooth.sd.attr.retrievable.max",
					"bluetooth.connected.devices.max", "bluetooth.l2cap.receiveMTU.max", "bluetooth.sd.trans.max",
					"bluetooth.connected.inquiry.scan", "bluetooth.connected.page.scan", "bluetooth.connected.inquiry",
					"bluetooth.connected.page" };
			for (int i = 0; i < property.length; i++) {
				UtilsJavaSE.setSystemProperty(property[i], bluetoothStack.getLocalDeviceProperty(property[i]));
			}
		}
	}

	private BluetoothStack createDetectorOnWindows(String stackFirst) throws BluetoothStateException {
		if (stackFirst != null) {
			DebugLog.debug("detector stack", stackFirst);
			if (STACK_WIDCOMM.equalsIgnoreCase(stackFirst)) {
				if ((NativeLibLoader.isAvailable(NATIVE_LIB_WIDCOMM))) {
					return new BluetoothStackWIDCOMM();
				}
			} else if (STACK_BLUESOLEIL.equalsIgnoreCase(stackFirst)) {
				if (NativeLibLoader.isAvailable(NATIVE_LIB_BLUESOLEIL)) {
					return new BluetoothStackBlueSoleil();
				}
			} else if (STACK_WINSOCK.equalsIgnoreCase(stackFirst)) {
				if (NativeLibLoader.isAvailable(NATIVE_LIB_MS)) {
					return new BluetoothStackMicrosoft();
				}
			} else if (STACK_TOSHIBA.equalsIgnoreCase(stackFirst)) {
				if (NativeLibLoader.isAvailable(NATIVE_LIB_TOSHIBA)) {
					return new BluetoothStackToshiba();
				}
			} else {
				throw new IllegalArgumentException("Invalid BlueCove detector stack [" + stackFirst + "]");
			}
		}
		if (NativeLibLoader.isAvailable(NATIVE_LIB_MS)) {
			return new BluetoothStackMicrosoft();
		} else if (NativeLibLoader.isAvailable(NATIVE_LIB_WIDCOMM)) {
			return new BluetoothStackWIDCOMM();
		} else {
			throw new BluetoothStateException("BlueCove not available");
		}
	}

	public String setBluetoothStack(String stack) throws BluetoothStateException {
		return setBluetoothStack(stack, null);
	}

	private synchronized String setBluetoothStack(String stack, BluetoothStack detectorStack)
			throws BluetoothStateException {
		if (bluetoothStack != null) {
			bluetoothStack.destroy();
			bluetoothStack = null;
		}
		BluetoothStack newStack;
		if ((detectorStack != null) && (detectorStack.getStackID()).equalsIgnoreCase(stack)) {
			newStack = detectorStack;
		} else if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
			newStack = new BluetoothStackWIDCOMM();
		} else if (STACK_BLUESOLEIL.equalsIgnoreCase(stack)) {
			newStack = new BluetoothStackBlueSoleil();
		} else if (STACK_TOSHIBA.equalsIgnoreCase(stack)) {
			newStack = new BluetoothStackToshiba();
		} else {
			newStack = new BluetoothStackMicrosoft();
		}
		int libraryVersion = newStack.getLibraryVersion();
		if (nativeLibraryVersionExpected != libraryVersion) {
			DebugLog.fatal("BlueCove native library version mismatch " + libraryVersion + " expected "
					+ nativeLibraryVersionExpected);
			throw new BluetoothStateException("BlueCove native library version mismatch");
		}

		if (DebugLog.isDebugEnabled()) {
			newStack.enableNativeDebug(DebugLog.class, true);
		}
		newStack.initialize();
		createShutdownHook();
		bluetoothStack = newStack;
		return bluetoothStack.getStackID();
	}

	public void enableNativeDebug(boolean on) {
		if (bluetoothStack != null) {
			bluetoothStack.enableNativeDebug(DebugLog.class, on);
		}
	}

	/**
	 * Applications should not used this function.
	 * 
	 * @return current BluetoothStack implementation
	 * @throws BluetoothStateException
	 *             when BluetoothStack not detected. If one connected the
	 *             hardware later, BlueCove would be able to recover and start
	 *             correctly
	 * @exception Error
	 *                if called from outside of BlueCove internal code.
	 */
	public synchronized BluetoothStack getBluetoothStack() throws BluetoothStateException {
		Utils.isLegalAPICall(fqcnSet);
		if (bluetoothStack == null) {
			if (accessControlContext == null) {
				detectStack();
			} else {
				detectStackPrivileged();
			}
			if (bluetoothStack == null) {
				throw new BluetoothStateException("BlueCove not available");
			}
		}
		return bluetoothStack;
	}

	private void detectStackPrivileged() throws BluetoothStateException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BluetoothStateException {
					detectStack();
					return null;
				}
			}, (AccessControlContext) accessControlContext);
		} catch (PrivilegedActionException e) {
			if (e.getCause() instanceof IOException) {
				throw (BluetoothStateException) e.getCause();
			}
			throw new BluetoothStateException(e.toString());
		}
	}

}
