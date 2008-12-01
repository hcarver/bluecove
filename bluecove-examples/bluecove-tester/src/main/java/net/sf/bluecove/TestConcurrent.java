/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

import java.util.Enumeration;
import java.util.Vector;

/**
 * 
 */
public class TestConcurrent {

	private static class ServicesSearchClientsThread implements CanShutdown, Runnable {

		Thread thread;

		private boolean stoped = false;

		private long active;

		public void run() {
			if (RemoteDeviceInfo.devices.size() < 2) {
				Logger.info("Need more Devices, have " + RemoteDeviceInfo.devices.size());
				return;
			}
			try {
				Switcher.clientStarted(this);
				while (!stoped) {
					Vector clients = new Vector();
					int ccount = 1;
					Logger.info("Starting Concurrent ServicesSearch " + RemoteDeviceInfo.devices.size());
					for (Enumeration iter = RemoteDeviceInfo.devices.elements(); iter.hasMoreElements() && (!stoped);) {
						RemoteDeviceInfo dev = (RemoteDeviceInfo) iter.nextElement();

						TestResponderClient client = Switcher.createClient(true);

						client.searchServiceRetry = false;
						client.discoveryOnce = true;
						client.useDiscoveredDevices = false;
						client.connectDevice = dev.remoteDevice.getBluetoothAddress();
						client.searchOnlyBluecoveUuid = Configuration.discoverySearchOnlyBluecoveUuid;
						client.logID = "CSS-" + ccount + " ";
						ccount++;

						client.configured();

						clients.addElement(client);

						active = System.currentTimeMillis();
					}

					for (Enumeration en = clients.elements(); en.hasMoreElements() && (!stoped);) {
						TestResponderClient c = (TestResponderClient) en.nextElement();
						try {
							c.thread.join();
						} catch (InterruptedException e) {
							break;
						}
					}

					int cerviceFound = 0;
					for (Enumeration en = clients.elements(); en.hasMoreElements() && (!stoped);) {
						TestResponderClient c = (TestResponderClient) en.nextElement();
						if (c.isAnyServiceFound()) {
							cerviceFound++;
						} else {
							Logger.debug("No srvc on " + TestResponderClient.niceDeviceName(c.connectDevice));
						}
					}

					Logger.info("Services found on " + cerviceFound + " from " + clients.size());

					if (!Configuration.clientContinuousServicesSearch) {
						break;
					}

				}
			} finally {
				Switcher.clientEnds(this);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.sf.bluecove.CanShutdown#lastActivityTime()
		 */
		public long lastActivityTime() {
			return active;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.sf.bluecove.CanShutdown#shutdown()
		 */
		public void shutdown() {
			stoped = true;
			if (Configuration.cldcStub != null) {
				Configuration.cldcStub.interruptThread(thread);
			}
		}

	}

	public static void startConcurrentServicesSearchClients() {
		ServicesSearchClientsThread cclient = new ServicesSearchClientsThread();
		cclient.thread = Configuration.cldcStub.createNamedThread(cclient, "ConcurrentSS");
		cclient.thread.start();
	}
}
