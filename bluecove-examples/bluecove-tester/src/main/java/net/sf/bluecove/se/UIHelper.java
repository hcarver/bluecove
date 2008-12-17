/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.se;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.StringUtils;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.RemoteDeviceInfo;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.TestResponderClient;
import net.sf.bluecove.TestResponderServer;

/**
 * 
 */
public class UIHelper {

	public static String getMainWindowTitle() {
		String title = "BlueCove tester";
		String bluecoveVersion = LocalDevice.getProperty("bluecove");
		if (StringUtils.isStringSet(bluecoveVersion)) {
			title += " " + bluecoveVersion;
			if (Configuration.threadLocalBluetoothStack != null) {
				title += " on [" + Configuration.threadLocalBluetoothStack.toString() + ", ...]";
			} else {
				String stack = LocalDevice.getProperty("bluecove.stack");
				if (StringUtils.isStringSet(stack)) {
					if (stack.equals("emulator") || stack.equals("bluez")) {
						try {
							stack += ":" + LocalDevice.getLocalDevice().getBluetoothAddress();
						} catch (BluetoothStateException ignore) {
						}
					}
					title += " on [" + stack + "]";
				} else {
					title += " on [winsock]";
				}
			}
		}
		return title;
	}

	public static void printFailureLog() {
		if (TestResponderClient.countSuccess + TestResponderClient.failure.countFailure != 0) {
			Logger.info("*Client Success:" + TestResponderClient.countSuccess + " Failure:"
					+ TestResponderClient.failure.countFailure);
			Logger.debug("Client avg conn concurrent " + TestResponderClient.concurrentStatistic.avg());
			Logger.debug("Client max conn concurrent " + TestResponderClient.concurrentStatistic.max());
			Logger.debug("Client avg conn time " + TestResponderClient.connectionDuration.avg() + " msec");
			Logger.debug("Client avg conn retry " + TestResponderClient.connectionRetyStatistic.avgPrc());

			TestResponderClient.failure.writeToLog();
		}

		if (TestResponderServer.countSuccess + TestResponderServer.failure.countFailure != 0) {
			Logger.info("*Server Success:" + TestResponderServer.countSuccess + " Failure:"
					+ TestResponderServer.failure.countFailure);
			Logger.debug("Server avg conn concurrent " + TestResponderServer.concurrentStatistic.avg());
			Logger.debug("Server avg conn time " + TestResponderServer.connectionDuration.avg() + " msec");

			TestResponderServer.failure.writeToLog();
		}
	}

	public static void clearStats() {
		TestResponderClient.clear();
		TestResponderServer.clear();
		Switcher.clear();
		RemoteDeviceInfo.clear();
	}

	public static void configurationForSpeedTest(int testNumber, boolean l2cap) {
		Configuration.tgSize = 251;
		Configuration.tgSleep = 0;
		Configuration.clientContinuous.setValue(false);
		Configuration.TEST_CASE_FIRST.setValue(testNumber);
		Configuration.TEST_CASE_LAST.setValue(testNumber);
		Configuration.TEST_CASE_L2CAP_FIRST.setValue(testNumber);
		Configuration.TEST_CASE_L2CAP_LAST.setValue(testNumber);
		Configuration.testL2CAP.setValue(l2cap);
		Configuration.testRFCOMM.setValue(!l2cap);
	}

}
