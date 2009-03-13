/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
 * 
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package net.sf.bluecove;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.RuntimeDetect;

/**
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
			monitor.monitorThread = RuntimeDetect.cldcStub.createNamedThread(monitor, name + "Monitor");
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