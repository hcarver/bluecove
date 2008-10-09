/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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
package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;

/**
 * @author vlads
 * 
 */
public class EmulatorTestsHelper {

	private static int threadNumber;

	private static synchronized int nextThreadNum() {
		return threadNumber++;
	}

	/**
	 * Start air simulator server as in process server.
	 */
	public static void startInProcessServer() {
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
	}

	/**
	 * Shutdown all running Stacks and air simulator server.
	 */
	public static void stopInProcessServer() {
		BlueCoveImpl.shutdown();
		EmulatorHelper.getService().shutdown();
	}

	/**
	 * API that enables the use of Multiple Bluetooth Adapters in parallel in
	 * the same JVM. Each thread can call this function to initialize new
	 * adapter.
	 * 
	 * @see com.intel.bluetooth.BlueCoveImpl#useThreadLocalBluetoothStack()
	 * @throws BluetoothStateException
	 *             if the Bluetooth system emulator could not be initialized
	 */
	public static void useThreadLocalEmulator() throws BluetoothStateException {
		useThreadLocalEmulator(null, null);
	}

	/**
	 * API that enables the use of Multiple Bluetooth Adapters in parallel in
	 * the same JVM. Each thread can call this function to initialize new
	 * adapter.
	 * 
	 * @see com.intel.bluetooth.BlueCoveImpl#useThreadLocalBluetoothStack()
	 * 
	 * @param deviceID
	 *            select bluetooth adapter by its system ID, can be
	 *            <code>null</code>
	 * @param localAddress
	 *            select bluetooth adapter by its Address, can be
	 *            <code>null</code>
	 * @throws BluetoothStateException
	 *             if the Bluetooth system emulator could not be initialized
	 */
	public static void useThreadLocalEmulator(String deviceID, String localAddress) throws BluetoothStateException {
		BlueCoveImpl.useThreadLocalBluetoothStack();
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK, BlueCoveImpl.STACK_EMULATOR);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT, "0");
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ID, deviceID);
		BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ADDRESS, localAddress);
		BlueCoveImpl.getThreadBluetoothStackID();
	}

	private static class RunBefore implements Runnable {

		private Runnable runnable;

		private Object startedEvent = new Object();

		private boolean started = false;

		private BluetoothStateException startException;

		RunBefore(Runnable runnable) {
			this.runnable = runnable;
		}

		public void run() {
			try {
				useThreadLocalEmulator();
			} catch (BluetoothStateException e) {
				startException = e;
			} finally {
				started = true;
				synchronized (startedEvent) {
					startedEvent.notifyAll();
				}
			}
			runnable.run();
		}
	}

	/**
	 * Helper function to execute code using different Bluetooth address
	 * 
	 * @param runnable
	 *            to be executed using another stack
	 * @return created and running Thread that will execute runnable in a new
	 *         ThreadGroup
	 * @throws BluetoothStateException
	 *             if the Bluetooth system emulator could not be initialized
	 */
	public static Thread runNewEmulatorStack(Runnable runnable) throws BluetoothStateException {
		RunBefore r = new RunBefore(runnable);
		int id = nextThreadNum();
		ThreadGroup g = new ThreadGroup("TestHelperThreadGroup-" + id);
		Thread t = new Thread(g, r, "TestHelperThread-" + id);
		synchronized (r.startedEvent) {
			t.start();
			while (!r.started) {
				try {
					r.startedEvent.wait();
				} catch (InterruptedException e) {
					throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()),
							e);
				}
				if (r.startException != null) {
					throw r.startException;
				}
			}
		}
		return t;
	}
}
