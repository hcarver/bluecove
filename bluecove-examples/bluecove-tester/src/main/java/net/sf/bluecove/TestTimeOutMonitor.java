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
package net.sf.bluecove;

/**
 * @author vlads
 * 
 */
public class TestTimeOutMonitor implements Runnable {

	private boolean testFinished = false;

	private boolean shutdownCalled = false;

	private CanShutdown testThread;

	private String name;

	int gracePeriodSeconds = 0;

	private Thread monitorThread;

	private TestTimeOutMonitor(String name, CanShutdown testThread, int gracePeriodSeconds) {
		this.name = name;
		this.testThread = testThread;
		this.gracePeriodSeconds = gracePeriodSeconds;
	}

	public static TestTimeOutMonitor create(String name, CanShutdown testThread, int gracePeriodSeconds) {
		TestTimeOutMonitor monitor = new TestTimeOutMonitor(name, testThread, gracePeriodSeconds);

		if (gracePeriodSeconds != 0) {
			monitor.monitorThread = Configuration.cldcStub.createNamedThread(monitor, name + "Monitor");
			monitor.monitorThread.start();
		}
		return monitor;
	}

	private int getTestGracePeriod() {
		if (testThread instanceof ConnectionHolder) {
			return ((ConnectionHolder) testThread).getTestTimeOutSec();
		} else {
			return 0;
		}
	}

	public void run() {
		if (gracePeriodSeconds == 0) {
			return;
		}

		while ((!testFinished)
				&& (System.currentTimeMillis() < (testThread.lastActivityTime() + (getTestGracePeriod() + this.gracePeriodSeconds) * 1000))) {
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				return;
			}
		}

		if (!testFinished) {
			shutdownCalled = true;
			Logger.info("shutdown " + name + " by TimeOut");
			testThread.shutdown();
		}
	}

	public void finish() {
		testFinished = true;
	}

	public boolean isShutdownCalled() {
		return shutdownCalled;
	}
}