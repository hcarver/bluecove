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
package org.bluecove.tester.tck;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.ThreadUtils;

import BluetoothTCKAgent.System;

public class TckStarter {

	private static Thread tckRFCOMMThread;

	private static Thread tckL2CALthread;

	private static Thread tckGOEPThread;

	private static Thread tckOBEXThread;

	static boolean startCommong() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			Logger.info("address:" + localDevice.getBluetoothAddress());
			Logger.info("name:" + localDevice.getFriendlyName());
			localDevice.setDiscoverable(DiscoveryAgent.GIAC);
			return true;
		} catch (BluetoothStateException e) {
			Logger.error("start", e);
			return false;
		}
	}

	static void startBluetoothTCK() {
		if (!startCommong()) {
			return;
		}
		try {
			tckRFCOMMThread = new BluetoothTCKAgent.RFCOMMThread("RFCOMMThread");
			tckRFCOMMThread.start();
		} catch (Throwable e) {
			Logger.debug("Fail to start RFCOMM", e);
		}
		try {
			String agentMtu = System.getProperty("bluetooth.agent_mtu");
			String timeout = System.getProperty("timeout");
			tckL2CALthread = new BluetoothTCKAgent.L2CAPThread("L2CAPThread", agentMtu, timeout);
			if (tckL2CALthread != null) {
				tckL2CALthread.start();
			}
		} catch (Throwable e) {
			Logger.debug("Fail to start L2CAP", e);
		}

		try {
			tckGOEPThread = new BluetoothTCKAgent.GOEPThread("GOEPThread");
			if (tckGOEPThread != null) {
				tckGOEPThread.start();
			}
		} catch (Throwable e) {
			Logger.debug("Fail to start GOEP srv", e);
		}
	}

	static void startObexTCK(boolean tcp) {
		if (!startCommong()) {
			return;
		}
		try {
			tckOBEXThread = new OBEXTCKAgent.OBEXTCKAgentApp("10", tcp ? "tcpobex" : "btgoep");
			if (tckOBEXThread != null) {
				tckOBEXThread.start();
			}
		} catch (Throwable e) {
			Logger.debug("Fail to start OBEX srv", e);
		}
	}

	static void stopAgent() {
		ThreadUtils.interruptThread(tckRFCOMMThread);
		tckRFCOMMThread = null;

		ThreadUtils.interruptThread(tckL2CALthread);
		tckL2CALthread = null;

		ThreadUtils.interruptThread(tckGOEPThread);
		tckGOEPThread = null;

		ThreadUtils.interruptThread(tckOBEXThread);
		tckOBEXThread = null;
	}

}
