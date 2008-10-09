/**
 *  MicroEmulator
 *  Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
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

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Logger;
import net.sf.bluecove.RemoteDeviceInfo;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.TestResponderClient;
import net.sf.bluecove.TestResponderServer;
import net.sf.bluecove.util.StringUtils;

/**
 * @author vlads
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
