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
import java.util.Random;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.CollectionUtils;
import BluetoothTCKAgent.System;

/**
 * 
 */
public class Switcher implements Runnable {

	public static TestResponderClient client;

	private static Vector runningClients = new Vector();

	public static TestResponderServer server;

	private static Vector runningServers = new Vector();

	public static int clientStartCount = 0;

	public static int serverStartCount = 0;

	private boolean stoped = false;

	public Thread thread;

	boolean isRunning = false;

	private static Switcher instance;

	Random random = new Random();

	private static Thread tckRFCOMMThread;

	private static Thread tckL2CALthread;

	private static Thread tckGOEPThread;

	private static Thread tckOBEXThread;

	public Switcher() {
		instance = this;
	}

	public static synchronized void clear() {
		clientStartCount = 0;
		serverStartCount = 0;
	}

	public static void yield(TestResponderClient client) {
		if (instance != null) {
			clientShutdown();
			synchronized (instance) {
				instance.notifyAll();
			}
		}
	}

	public static void yield(TestResponderServer server) {
		if (instance != null) {
			while (server.hasRunningConnections()) {
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}
			}
			serverShutdown();
		}
	}

	public static boolean isRunning() {
		return (instance != null) && instance.isRunning;
	}

	public static boolean isRunningClient() {
		return ((client != null) && client.isRunning) || (!runningClients.isEmpty());
	}

	public static boolean isRunningServer() {
		return isTCKRunning() || ((server != null) && server.isRunning());
	}

	public static boolean isRunningServerClients() {
		return ((server != null) && (server.countClientConnections() > 0));
	}

	public void run() {
		Logger.debug("Switcher started...");
		isRunning = true;
		try {
			if (!isRunningClient()) {
				startClient();
			}
			while (!stoped) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						break;
					}
				}
				if (stoped) {
					break;
				}
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}
				if (stoped) {
					break;
				}
				startServer();
				if (stoped) {
					break;
				}
				try {
					int sec = randomTTL(30, Configuration.serverMAXTimeSec);
					Logger.info("switch to client in " + sec + " sec");
					Thread.sleep(sec * 1000);
				} catch (Exception e) {
					break;
				}

				yield(server);
				if (stoped) {
					break;
				}
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					break;
				}
				if (stoped) {
					break;
				}
				startClient();

			}
		} finally {
			isRunning = false;
			Logger.info("Switcher finished!");
		}
	}

	public int randomTTL(int min, int max) {
		int d = random.nextInt() % (max - min);
		if (d < 0) {
			d = -d;
		}
		return min + d;
	}

	public void shutdown() {
		Logger.info("shutdownSwitcher");
		stoped = true;
		interruptThread(thread);
		synchronized (this) {
			notifyAll();
		}
		instance = null;
	}

	public static void clientStarted(CanShutdown t) {
		runningClients.addElement(t);
	}

	public static void clientEnds(CanShutdown t) {
		runningClients.removeElement(t);
	}

	public static void interruptThread(Thread thread) {
		if (Configuration.cldcStub != null) {
			Configuration.cldcStub.interruptThread(thread);
		}
	}

	public static Thread createThreadByName(String className) {
		try {
			Class c = Class.forName(className);
			return (Thread) c.newInstance();
		} catch (Throwable e) {
			Logger.debug(className, e);
			return null;
		}
	}

	public static void startTCKAgent() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			Logger.info("address:" + localDevice.getBluetoothAddress());
			Logger.info("name:" + localDevice.getFriendlyName());
			Logger.info("class:" + BluetoothTypesInfo.toString(localDevice.getDeviceClass()));
			localDevice.setDiscoverable(DiscoveryAgent.GIAC);
		} catch (BluetoothStateException e) {
			Logger.error("start", e);
		}

		if (Configuration.likedTCKAgent) {
			tckRFCOMMThread = new BluetoothTCKAgent.RFCOMMThread("RFCOMMThread");
			if (tckRFCOMMThread == null) {
				Logger.info("Due to the License we do not include the TCK agent in distribution");
			} else {
				tckRFCOMMThread.start();

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

				try {
					tckOBEXThread = new OBEXTCKAgent.OBEXTCKAgentApp("10", Configuration.testServerOBEX_TCP
							.booleanValue() ? "tcpobex" : "btgoep");
					if (tckOBEXThread != null) {
						tckOBEXThread.start();
					}
				} catch (Throwable e) {
					Logger.debug("Fail to start OBEX srv", e);
				}
			}
		}
	}

	public static boolean isTCKRunning() {
		return (tckRFCOMMThread != null) || (tckL2CALthread != null) || (tckGOEPThread != null)
				|| (tckOBEXThread != null);
	}

	static void stopTCK() {
		interruptThread(tckRFCOMMThread);
		tckRFCOMMThread = null;

		interruptThread(tckL2CALthread);
		tckL2CALthread = null;

		interruptThread(tckGOEPThread);
		tckGOEPThread = null;

		interruptThread(tckOBEXThread);
		tckOBEXThread = null;
	}

	public static TestResponderClient createClient() {
		return createClient(false);
	}

	public static TestResponderClient createClient(boolean force) {
		try {
			TestResponderClient c;
			if ((client == null) || (force)) {
				c = new TestResponderClient(!force);
				client = c;
			} else {
				c = client;
			}
			if (!c.isRunning) {
				c.config.logID = "";
				c.configured = false;
				c.config.discoveryOnce = false;
				c.config.connectDevice = null;
				c.config.searchServiceRetry = true;
				c.config.useDiscoveredDevices = false;
				c.config.searchOnlyBluecoveUuid = Configuration.searchOnlyBluecoveUuid;
				String name = "Client" + clientStartCount++;
				c.thread = Configuration.cldcStub.createNamedThread(c, name);
				c.thread.start();
				return c;
			} else {
				if (Configuration.isJ2ME) {
					BlueCoveTestMIDlet.message("Warn", "Client is already Running");
				} else {
					Logger.warn("Client is already Running");
				}
				return null;
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
			return null;
		}
	}

	public static void startTwoClients() {
		try {
			client = new TestResponderClient();
			client.configured = false;
			client.config.discoveryOnce = false;
			client.config.useDiscoveredDevices = false;
			client.config.searchOnlyBluecoveUuid = Configuration.searchOnlyBluecoveUuid;
			client.thread = new Thread(client);
			client.configured();

			TestResponderClient client2 = new TestResponderClient();
			client2.configured = false;
			client2.config.discoveryOnce = false;
			client2.config.useDiscoveredDevices = false;
			client2.config.searchOnlyBluecoveUuid = Configuration.searchOnlyBluecoveUuid;
			client2.thread = new Thread(client2);
			client2.configured();

			client.thread.start();
			client2.thread.start();
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}

	public static void startDiscovery() {
		TestResponderClient client = createClient();
		if (client != null) {
			client.config.discoveryOnce = true;
			client.config.useDiscoveredDevices = false;
			client.config.searchOnlyBluecoveUuid = Configuration.discoverySearchOnlyBluecoveUuid;
			client.configured();
		}
	}

	public static void startServicesSearch() {
		TestResponderClient client = createClient();
		if (client != null) {
			client.config.discoveryOnce = true;
			client.config.useDiscoveredDevices = true;
			client.config.searchOnlyBluecoveUuid = Configuration.discoverySearchOnlyBluecoveUuid;
			client.configured();
		}
	}

	public static void startClient() {
		try {
			createClient();
			if (client != null) {
				client.configured();
			}
		} catch (Throwable e) {
			Logger.error("startClient", e);
		}
	}

	public static int runClient() {
		createClient();
		if (client != null) {
			client.config.connectOnce = true;
			client.configured();
			try {
				client.thread.join();
			} catch (InterruptedException e) {
				return 2;
			}
			if (TestResponderClient.failure.countFailure > 0) {
				return 2;
			} else if (TestResponderClient.countSuccess == 0) {
				return 3;
			}
			return 1;
		} else {
			return 2;
		}
	}

	public static void startClientStress() {
		if ((client != null) && client.isRunning) {
			Logger.warn("Client is already Running");
			return;
		}
		createClient();
		if (client != null) {
			client.runStressTest = true;
			client.configured();
		}
	}

	public static void startClientLastURl() {
		if (Configuration.storage == null) {
			Logger.warn("no storage");
			return;
		}
		if ((client != null) && client.isRunning) {
			Logger.warn("Client is already Running");
			return;
		}
		String lastURL = Configuration.getLastServerURL();
		if (lastURL != null) {
			createClient();
			if (client != null) {
				client.config.connectURL = lastURL;
				client.configured();
			}
		} else {
			Logger.warn("no recent Connections");
		}
	}

	public static void startClientSelectService() {
		if ((client != null) && client.isRunning) {
			Logger.warn("Client is already Running");
			return;
		}
		createClient();
		if (client != null) {
			client.config.connectURL = "";
			client.configured();
		}
	}

	public static void startClientLastDevice() {
		if (Configuration.storage == null) {
			Logger.warn("no storage");
			return;
		}
		if ((client != null) && client.isRunning) {
			Logger.warn("Client is already Running");
			return;
		}
		String lastURL = Configuration.getLastServerURL();
		if (lastURL != null) {
			createClient();
			if (client != null) {
				client.config.connectDevice = BluetoothTypesInfo.extractBluetoothAddress(lastURL);
				client.configured();
			}
		} else {
			Logger.warn("no recent Connections");
		}
	}

	public static void clientShutdown() {
		if (client != null) {
			client.shutdown();
			client = null;
		}
		Vector runningClientsCopy = CollectionUtils.copy(runningClients);
		for (Enumeration iter = runningClientsCopy.elements(); iter.hasMoreElements();) {
			CanShutdown t = (CanShutdown) iter.nextElement();
			t.shutdown();
		}
	}

	public static void startServer() {
		try {
			if (server == null) {
				server = new TestResponderServer();
			}
			if (!server.isRunning()) {
				String name = "Server" + serverStartCount++;
				server.thread = Configuration.cldcStub.createNamedThread(server, name);
				server.thread.start();
			} else {
				if (Configuration.canCloseServer) {
					if (Configuration.isJ2ME) {
						BlueCoveTestMIDlet.message("Warn", "Server is already running");
					} else {
						Logger.warn("Server is already running");
					}
				} else {
					serverStartCount++;
					server.updateServiceRecord();
					TestResponderServer.setDiscoverable();
				}
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}

	public static void serverShutdown() {
		if (Configuration.canCloseServer) {
			serverShutdownForce();
		} else {
			TestResponderServer.setNotDiscoverable();
		}
		stopTCK();
	}

	public static void serverShutdownOnExit() {
		serverShutdownForce();
	}

	public static void serverShutdownForce() {
		if (server != null) {
			server.shutdown();
			server = null;
		}
	}

	public static void closeServerClientConnections() {
		if (server != null) {
			server.closeServerClientConnections();
		}
	}

}
