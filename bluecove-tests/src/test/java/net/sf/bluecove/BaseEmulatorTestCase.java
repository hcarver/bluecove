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
package net.sf.bluecove;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * @author vlads
 * 
 */
public abstract class BaseEmulatorTestCase extends TestCase {

	// Use this to debug tests
	protected boolean debug = false;

	protected boolean debugOnInEclipse = true;

	protected Thread testServerThread;

	protected ThreadGroup testServerThreadGroup;

	protected LongRunningTestMonitor monitor;

	/**
	 * Default 30 seconds
	 * 
	 * @return the length of time for test in milliseconds.
	 */
	protected int gracePeriod() {
		return 30 * 1000;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		boolean eclipse = isEclipse();
		if (eclipse && debugOnInEclipse) {
			debug = true;
		}
		if (!eclipse && (gracePeriod() > 0)) {
			monitor = new LongRunningTestMonitor(gracePeriod(), this.getClass().getName() + "." + this.getName());
			monitor.start();
		}
		// Use this to debug tests
		if (debug) {
			BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG, "true");
			BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG_STDOUT, "false");
		} else {
			BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG_LOG4J, "false");
		}
		EmulatorTestsHelper.startInProcessServer();
		Runnable r = createTestServer();
		if (r != null) {
			testServerThread = EmulatorTestsHelper.runNewEmulatorStack(r);
			testServerThread.setName(this.getClass().getSimpleName() + "-ServerThread");
			testServerThreadGroup = testServerThread.getThreadGroup();
		}
		EmulatorTestsHelper.useThreadLocalEmulator();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (testServerThreadGroup != null) {
			testServerThreadGroup.interrupt();
			int count = testServerThreadGroup.activeCount();
			Thread[] active = new Thread[count];
			count = testServerThreadGroup.enumerate(active);
			for (int i = 0; i < count; i++) {
				active[i].join(5000);
			}
		}
		EmulatorTestsHelper.stopInProcessServer();
		if (monitor != null) {
			monitor.finish();
			monitor = null;
		}
	}

	/**
	 * Override if test needs a second Thread with server
	 * 
	 * @return
	 */
	protected Runnable createTestServer() {
		return null;
	}

	static public void assertEquals(String message, byte[] expected, byte[] actual) {
		Assert.assertNotNull(message + " value is null", actual);
		Assert.assertEquals(message + " length", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals(message + " byte [" + i + "]", expected[i], actual[i]);
		}
	}

	static public void assertEquals(String message, int length, byte[] expected, byte[] actual) {
		Assert.assertTrue(message + " expected.length", expected.length >= length);
		Assert.assertTrue(message + " actual.length", actual.length >= length);
		for (int i = 0; i < length; i++) {
			Assert.assertEquals(message + " byte [" + i + "]", expected[i], actual[i]);
		}
	}

	protected String selectService(String uuid) throws BluetoothStateException {
		return selectService(new UUID(uuid, false));
	}

	protected String selectService(UUID uuid) throws BluetoothStateException {
		DiscoveryAgent discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();

		// Find service
		String serverURL = null;
		int retry = 0;
		while ((serverURL == null) && ((retry++) < 3)) {
			serverURL = discoveryAgent.selectService(uuid, ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
		}

		Assert.assertNotNull("service not found", serverURL);

		return serverURL;
	}

	boolean isEclipse() {
		StackTraceElement[] ste = new Throwable().getStackTrace();
		for (StackTraceElement s : ste) {
			if (s.getClassName().startsWith("org.eclipse.jdt")) {
				return true;
			}
		}
		return false;
	}

}
