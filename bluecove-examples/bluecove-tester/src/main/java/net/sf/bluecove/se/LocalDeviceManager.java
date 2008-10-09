/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.se;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Logger;
import net.sf.bluecove.TestResponderServer;

import com.intel.bluetooth.BlueCoveImpl;

/**
 * @author vlads
 * 
 */
public class LocalDeviceManager {

	private static Object threadLocalBluetoothStackWINSOCK;

	private static Object threadLocalBluetoothStackWIDCOMM;

	private static Object threadLocalBluetoothStack0;

	private static Object threadLocalBluetoothStack1;

	public static void setNotDiscoverable() {
		TestResponderServer.setNotDiscoverable();
		getDiscoverable();
	}

	public static void setDiscoverableGIAC() {
		TestResponderServer.setDiscoverable(DiscoveryAgent.GIAC);
		getDiscoverable();
	}

	public static void setDiscoverableLIAC() {
		TestResponderServer.setDiscoverable(DiscoveryAgent.LIAC);
		getDiscoverable();
	}

	public static void getDiscoverable() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			int mode = localDevice.getDiscoverable();
			Logger.debug("LocalDevice " + localDevice.getBluetoothAddress() + " Discoverable "
					+ TestResponderServer.discoverableModeString(mode));
		} catch (Throwable e) {
			Logger.error("getDiscoverable error", e);
		}
	}

	public static void setThreadLocalBluetoothStack(Object id) {
		if (id != null) {
			try {
				BlueCoveImpl.setThreadBluetoothStackID(id);
			} catch (Throwable e) {
				Logger.error("error", e);
			}
		}
	}

	public static void shutdown() {
		BlueCoveImpl.shutdown();
		Logger.info("shutdown finished");
	}

	public static void shutdownThreadLocal() {
		if (Configuration.threadLocalBluetoothStack != null) {
			BlueCoveImpl.shutdownThreadBluetoothStack();
			Configuration.threadLocalBluetoothStack = null;
			Logger.info("ThreadLocal shutdown finished");
		} else {
			Logger.info("no ThreadLocal stack");
		}
	}

	public static void setUseWINSOCK() {
		if (threadLocalBluetoothStackWINSOCK == null) {
			try {
				BlueCoveImpl.useThreadLocalBluetoothStack();
				BlueCoveImpl.setThreadBluetoothStackID(null);
				BlueCoveImpl.setConfigProperty("bluecove.stack", "winsock");
				threadLocalBluetoothStackWINSOCK = BlueCoveImpl.getThreadBluetoothStackID();
			} catch (Throwable e) {
				Logger.error("error", e);
				return;
			}
		}
		Logger.info("will use stack " + threadLocalBluetoothStackWINSOCK);
		Configuration.threadLocalBluetoothStack = threadLocalBluetoothStackWINSOCK;
		setThreadLocalBluetoothStack(Configuration.threadLocalBluetoothStack);
	}

	public static void setUseWIDCOMM() {
		if (threadLocalBluetoothStackWIDCOMM == null) {
			try {
				BlueCoveImpl.useThreadLocalBluetoothStack();
				BlueCoveImpl.setThreadBluetoothStackID(null);
				BlueCoveImpl.setConfigProperty("bluecove.stack", "widcomm");
				threadLocalBluetoothStackWIDCOMM = BlueCoveImpl.getThreadBluetoothStackID();
			} catch (Throwable e) {
				Logger.error("error", e);
				return;
			}
		}
		Logger.info("will use stack " + threadLocalBluetoothStackWIDCOMM);
		Configuration.threadLocalBluetoothStack = threadLocalBluetoothStackWIDCOMM;
		setThreadLocalBluetoothStack(Configuration.threadLocalBluetoothStack);
	}

	public static void setUseDevice0() {
		if (threadLocalBluetoothStack0 == null) {
			try {
				BlueCoveImpl.useThreadLocalBluetoothStack();
				BlueCoveImpl.setThreadBluetoothStackID(null);
				BlueCoveImpl.setConfigProperty("bluecove.deviceID", "0");
				threadLocalBluetoothStack0 = BlueCoveImpl.getThreadBluetoothStackID();
			} catch (Throwable e) {
				Logger.error("error", e);
				return;
			}
		}
		Logger.info("will use stack " + threadLocalBluetoothStack0);
		Configuration.threadLocalBluetoothStack = threadLocalBluetoothStack0;
		setThreadLocalBluetoothStack(Configuration.threadLocalBluetoothStack);
	}

	public static void setUseDevice1() {
		if (threadLocalBluetoothStack1 == null) {
			try {
				BlueCoveImpl.useThreadLocalBluetoothStack();
				BlueCoveImpl.setThreadBluetoothStackID(null);
				BlueCoveImpl.setConfigProperty("bluecove.deviceID", "1");
				threadLocalBluetoothStack1 = BlueCoveImpl.getThreadBluetoothStackID();
			} catch (Throwable e) {
				Logger.error("error", e);
				return;
			}
		}
		Logger.info("will use stack " + threadLocalBluetoothStack1);
		Configuration.threadLocalBluetoothStack = threadLocalBluetoothStack1;
		setThreadLocalBluetoothStack(Configuration.threadLocalBluetoothStack);
	}

}
