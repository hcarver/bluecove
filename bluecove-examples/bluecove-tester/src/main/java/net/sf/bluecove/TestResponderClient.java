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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;
import org.bluecove.tester.util.RuntimeDetect;
import org.bluecove.tester.util.StringUtils;
import org.bluecove.tester.util.TimeUtils;

import net.sf.bluecove.se.JavaSECommon;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.CollectionUtils;
import net.sf.bluecove.util.CountStatistic;
import net.sf.bluecove.util.IntVar;
import net.sf.bluecove.util.TimeStatistic;

/**
 * 
 */
public class TestResponderClient extends TestResponderCommon implements Runnable, CanShutdown {

	public static int countSuccess = 0;

	public static FailureLog failure = new FailureLog("Client failure");

	public static int discoveryCount = 0;

	public static int connectionCount = 0;

	public static int discoveryDryCount = 0;

	public static int discoverySuccessCount = 0;

	public static long lastSuccessfulDiscovery;

	public static int countConnectionThreads = 0;

	private int connectedConnectionsExpect = 1;

	private int connectionLogPrefixLength = 0;

	private int connectedConnectionsInfo = 1;

	private Vector concurrentConnections = new Vector();

	public static CountStatistic concurrentStatistic = new CountStatistic();

	public static TimeStatistic connectionDuration = new TimeStatistic();

	public static CountStatistic connectionRetyStatistic = new CountStatistic();

	public Thread thread;

	private Object threadLocalBluetoothStack;

	boolean stoped = false;

	TestClientConfig config;

	boolean isRunning = false;

	boolean runStressTest = false;

	TestClientBluetoothInquirer bluetoothInquirer;

	public static Hashtable recentDeviceNames = new Hashtable/* <BTAddress,Name> */();

	boolean configured = false;

	static int sdAttrRetrievableMax = 255;

	public static synchronized void clear() {
		countSuccess = 0;
		failure.clear();
		discoveryCount = 0;
		concurrentStatistic.clear();
		connectionDuration.clear();
	}

	public TestResponderClient() throws BluetoothStateException {
		this(true);
	}

	public TestResponderClient(boolean logLocalDevice) throws BluetoothStateException {

		config = new TestClientConfig();

		if (logLocalDevice) {
			TestResponderCommon.startLocalDevice();
		}
		threadLocalBluetoothStack = Configuration.threadLocalBluetoothStack;

		String v = LocalDevice.getProperty("bluetooth.sd.attr.retrievable.max");
		if (v != null) {
			sdAttrRetrievableMax = Integer.valueOf(v).intValue();
			if ((sdAttrRetrievableMax > 7) && (RuntimeDetect.isJ2ME)) {
				sdAttrRetrievableMax = 7;
			}
		}

	}

	static boolean isMultiProtocol() {
		return ((Configuration.supportL2CAP) && Configuration.testL2CAP.booleanValue() && Configuration.testRFCOMM
				.booleanValue());
	}

	public void connectAndTest(String serverURL, String urlArgs, IntVar firstCase, IntVar lastCase,
			TestResponderClientConnection connectionHandler) {
		String deviceAddress = BluetoothTypesInfo.extractBluetoothAddress(serverURL);
		String deviceName = niceDeviceName(deviceAddress);
		long start = System.currentTimeMillis();
		Logger.debug("connect:" + deviceName + " " + serverURL);
		String logPrefix = "";
		if (isMultiProtocol()) {
			deviceName = connectionHandler.protocolID() + " " + deviceName;
		}
		if (connectedConnectionsExpect > 1) {
			logPrefix = "[" + StringUtils.padRight(deviceName, connectionLogPrefixLength, ' ') + "] ";
		}
		for (int testType = firstCase.getValue(); (!stoped) && (runStressTest || testType <= lastCase.getValue()); testType++) {
			Connection conn = null;
			ConnectionHolder c = null;
			TestStatus testStatus = new TestStatus();
			testStatus.pairBTAddress = deviceAddress;
			TestTimeOutMonitor monitor = null;
			long connectionStartTime = 0;
			try {
				if (!runStressTest) {
					Logger.debug(logPrefix + "test #" + testType + " connects");
				} else {
					testType = Configuration.STERSS_TEST_CASE.getValue();
				}
				int connectionOpenTry = 0;
				while ((conn == null) && (!stoped)) {
					try {
						conn = Connector.open(serverURL + urlArgs, Connector.READ_WRITE, true);
					} catch (IOException e) {
						connectionOpenTry++;
						if ((stoped) || (connectionOpenTry > CommunicationTester.clientConnectionOpenRetry)) {
							throw e;
						}
						Logger.debug(logPrefix + "Connector error", e);
						Thread.sleep(Configuration.clientSleepOnConnectionRetry);
						Logger.debug(logPrefix + "connect retry:" + connectionOpenTry);
						String cCount = LocalDevice.getProperty("bluecove.connections");
						if ((cCount != null) && (!"0".equals(cCount))) {
							Logger.debug(logPrefix + "has connections:" + cCount);
						}
					}
				}
				if (stoped) {
					return;
				}
				c = connectionHandler.connected(conn);

				connectionStartTime = System.currentTimeMillis();
				connectionRetyStatistic.add(connectionOpenTry);
				connectionCount++;

				c.registerConcurrent(concurrentConnections);
				c.concurrentNotify();

				if (connectedConnectionsInfo < c.concurrentCount) {
					connectedConnectionsInfo = c.concurrentCount;
					Logger.info(logPrefix + "now connected:" + connectedConnectionsInfo);
					synchronized (TestResponderClient.this) {
						TestResponderClient.this.notifyAll();
					}
				}
				c.active();
				monitor = TestTimeOutMonitor.create(logPrefix + "test #" + testType, c,
						Configuration.clientTestTimeOutSec);
				if (!runStressTest) {
					Logger.debug(logPrefix + "run test #" + testType);
				} else {
					Logger.debug(logPrefix + "connected:" + connectionCount);
					if (connectionCount % 5 == 0) {
						Logger.debug("Test time " + TimeUtils.secSince(start));
					}
				}
				if (testType > Consts.TEST_SERVER_TERMINATE) {
					Configuration.setLastServerURL(serverURL);
				}

				connectionHandler.executeTest(testType, testStatus);

				if (monitor.isShutdownCalled()) {
					failure.addFailure(deviceName + " test #" + testType + " " + testStatus.getName()
							+ " terminated by  by TimeOut");
				} else if (testStatus.isError) {
					failure.addFailure(deviceName + " test #" + testType + " " + testStatus.getName());
				} else if (testStatus.isSuccess) {
					countSuccess++;
					Logger.debug(logPrefix + "test #" + testType + " " + testStatus.getName() + ": OK");
				} else if (testStatus.streamClosed) {
					Logger.debug(logPrefix + "see server log");
				} else {
					connectionHandler.replySuccess(logPrefix, testType, testStatus);
					countSuccess++;
					Logger.debug(logPrefix + "test #" + testType + " " + testStatus.getName() + ": OK");
				}
				if (connectionCount % 5 == 0) {
					Logger.info("*Success:" + countSuccess + " Failure:" + failure.countFailure);
				}
				Configuration.setLastServerURL(serverURL);

				// Delay to see if many connections are made.
				if ((connectedConnectionsExpect > 1) && (connectedConnectionsInfo < connectedConnectionsExpect)) {
					synchronized (TestResponderClient.this) {
						try {
							TestResponderClient.this.wait(3 * 1000);
						} catch (InterruptedException e) {
							break;
						}
					}
					Logger.debug(logPrefix + "concurrentCount " + c.concurrentCount);
				}
			} catch (Throwable e) {
				if (!stoped) {
					if ((monitor != null) && (monitor.isShutdownCalled())) {
						failure.addFailure(deviceName + " test #" + testType + " " + testStatus.getName()
								+ " terminated by  by TimeOut");
					} else if (testType < Consts.TEST_SERVER_TERMINATE) {
						failure.addFailure(deviceName + " test #" + testType + " " + testStatus.getName(), e);
					}
				}
				Logger.error(deviceName + " test #" + testType + " " + testStatus.getName(), e);
			} finally {
				if (connectionStartTime != 0) {
					connectionDuration.add(TimeUtils.since(connectionStartTime));
				}
				if (monitor != null) {
					monitor.finish();
				}
				if (c != null) {
					c.disconnected();
					if (c.concurrentCount != 0) {
						concurrentStatistic.add(c.concurrentCount);
					}
					c.shutdown();
				} else {
					IOUtils.closeQuietly(conn);
				}
			}
			if ((Configuration.clientTestStopOnErrorCount != 0)
					&& (failure.countFailure >= Configuration.clientTestStopOnErrorCount)) {
				shutdown();
			}
			// Let the server restart
			if (!stoped) {
				try {
					Thread.sleep(Configuration.clientSleepBetweenConnections);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		if (!Configuration.clientContinuous.booleanValue()) {
			connectionHandler.sendStopServerCmd(serverURL);
		}
	}

	private boolean connectAndTest(String serverURL) {
		if (BluetoothTypesInfo.isRFCOMM(serverURL)) {
			if (Configuration.testRFCOMM.booleanValue()) {
				TestResponderClientRFCOMM.connectAndTest(this, serverURL);
				return true;
			}
		} else if (BluetoothTypesInfo.isL2CAP(serverURL)) {
			if (Configuration.supportL2CAP) {
				if (Configuration.testL2CAP.booleanValue()) {
					TestResponderClientL2CAP.connectAndTest(this, serverURL);
					return true;
				}
			} else {
				Logger.warn("Can't test L2CAP on this stack");
			}
		} else {
			Logger.warn("No tests for connection type " + serverURL);
		}
		return false;
	}

	private class ClientConnectionTread extends Thread {

		String url;

		boolean started;

		ClientConnectionTread(String url) {
			// CLDC_1_0 super("ClientConnectionTread" +
			// (++countConnectionThreads));
			++countConnectionThreads;

			this.url = url;
		}

		public void run() {
			RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);
			started = connectAndTest(url);
		}
	}

	private void connectAndTest(Vector urls) {
		int numberOfURLs = urls.size();
		if ((!Configuration.clientTestConnectionsMultipleThreads) || (numberOfURLs == 1)) {
			connectedConnectionsExpect = 1;
			connectedConnectionsInfo = 1;
			for (Enumeration en = urls.elements(); en.hasMoreElements();) {
				if (stoped) {
					break;
				}
				String url = (String) en.nextElement();
				connectAndTest(url);
			}
		} else {
			connectedConnectionsExpect = numberOfURLs;
			connectedConnectionsInfo = 1;

			connectionLogPrefixLength = 0;
			for (Enumeration en = urls.elements(); en.hasMoreElements();) {
				String deviceAddress = BluetoothTypesInfo.extractBluetoothAddress((String) en.nextElement());
				String deviceName = niceDeviceName(deviceAddress);
				if (deviceName.length() > connectionLogPrefixLength) {
					connectionLogPrefixLength = deviceName.length();
				}
			}

			if (isMultiProtocol()) {
				connectionLogPrefixLength += 3;
			}

			Logger.debug("start " + numberOfURLs + " threads");
			Vector threads = new Vector();
			for (Enumeration en = urls.elements(); en.hasMoreElements();) {
				ClientConnectionTread t = new ClientConnectionTread((String) en.nextElement());
				t.start();
				threads.addElement(t);
			}
			int connectedConnectionsStartedExpect = 0;
			for (Enumeration en = threads.elements(); en.hasMoreElements();) {
				ClientConnectionTread t = (ClientConnectionTread) en.nextElement();
				if (t.started) {
					connectedConnectionsStartedExpect++;
				}
				try {
					t.join();
				} catch (InterruptedException e) {
					break;
				}
			}
			if (connectedConnectionsInfo < connectedConnectionsStartedExpect) {
				if (!stoped) {
					failure.addFailure("Fails to establish " + connectedConnectionsStartedExpect
							+ " connections same time");
					Logger.error("Fails to establish " + connectedConnectionsStartedExpect + " connections same time");
				}
			} else {
				if (connectedConnectionsInfo > 1) {
					Logger.info("Established " + connectedConnectionsInfo + " connections same time");
				}
			}
		}
	}

	public void configured() {
		synchronized (this) {
			configured = true;
			this.notifyAll();
		}
	}

	public long lastActivityTime() {
		return lastSuccessfulDiscovery;
	}

	public void run() {
		synchronized (this) {
			while (!configured) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					return;
				}
			}
		}
		RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);

		Logger.debug(config.logID + "Client started..." + TimeUtils.timeNowToString());
		isRunning = true;
		try {
			bluetoothInquirer = new TestClientBluetoothInquirer(config);
			Switcher.clientStarted(this);

			int startTry = 0;
			if (config.connectURL != null) {
				if (!config.connectURL.equals("")) {
					bluetoothInquirer.serverURLs.addElement(config.connectURL);
				}
			} else if (config.connectDevice != null) {
				Configuration.clientContinuousDiscoveryDevices = false;
				bluetoothInquirer.devices.addElement(new RemoteDeviceIheritance(config.connectDevice));
			}

			while (!stoped) {
				if ((config.connectURL != null) && (config.connectURL.equals(""))) {
					try {
						bluetoothInquirer.serverURLs.removeAllElements();
						DiscoveryAgent discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
						UUID uuid = (config.searchOnlyBluecoveUuid) ? Configuration.blueCoveUUID()
								: Configuration.discoveryUUID;
						String url = discoveryAgent.selectService(uuid, Configuration.getRequiredSecurity(), false);
						if (url != null) {
							Logger.debug(config.logID + "selectService service found " + url);
							bluetoothInquirer.serverURLs.addElement(url);
						} else {
							Logger.debug(config.logID + "selectService service not found");
						}
					} catch (BluetoothStateException e) {
						Logger.error(config.logID + "Cannot selectService", e);
					}
				} else if ((!bluetoothInquirer.hasServers())
						|| (Configuration.clientContinuousDiscovery.booleanValue() && (config.connectURL == null))
						|| (!Configuration.clientTestConnections)) {
					if (!bluetoothInquirer.runDeviceInquiry()) {
						if (stoped) {
							break;
						}
						startTry++;
						try {
							Thread.sleep(Configuration.clientSleepOnDeviceInquiryError);
						} catch (Exception e) {
							break;
						}
						if (startTry < 3) {
							continue;
						}
						Switcher.yield(this);
					} else {
						startTry = 0;
					}
					while (bluetoothInquirer.inquiring) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
							break;
						}
					}
				}
				if ((Configuration.clientTestConnections) && (bluetoothInquirer.hasServers())) {
					discoveryDryCount = 0;
					discoverySuccessCount++;
					lastSuccessfulDiscovery = System.currentTimeMillis();
					if (!config.discoveryOnce) {
						connectAndTest(bluetoothInquirer.serverURLs);
					}
				} else {
					discoveryDryCount++;
					if ((discoveryDryCount % 5 == 0) && (lastSuccessfulDiscovery != 0)) {
						Logger.debug(config.logID + "No services " + discoveryDryCount + " times for "
								+ TimeUtils.secSince(lastSuccessfulDiscovery) + " " + discoverySuccessCount);
					}
				}
				Logger.info(config.logID + "*Success:" + countSuccess + " Failure:" + failure.countFailure);
				if ((countSuccess + failure.countFailure > 0) && (!Configuration.clientContinuous.booleanValue())) {
					break;
				}
				if (stoped || config.discoveryOnce || config.connectOnce) {
					break;
				}
				Switcher.yield(this);
			}
		} catch (Throwable e) {
			if (!stoped) {
				Logger.error(config.logID + "client error ", e);
			}
		} finally {
			Switcher.clientEnds(this);
			config.connectURL = null;
			isRunning = false;
			Logger.info(config.logID + "Client finished! " + TimeUtils.timeNowToString());
			Switcher.yield(this);
		}
	}

	public boolean isAnyServiceFound() {
		if (bluetoothInquirer != null) {
			return bluetoothInquirer.anyServicesFound;
		} else {
			return false;
		}
	}

	public void shutdown() {
		Logger.info(config.logID + "shutdownClient");
		stoped = true;
		if (bluetoothInquirer != null) {
			bluetoothInquirer.shutdown();
		}
		if (RuntimeDetect.cldcStub != null) {
			RuntimeDetect.cldcStub.interruptThread(thread);
		}
		Vector concurrentConnectionsCopy = CollectionUtils.copy(concurrentConnections);
		for (Enumeration iter = concurrentConnectionsCopy.elements(); iter.hasMoreElements();) {
			ConnectionHolder t = (ConnectionHolder) iter.nextElement();
			t.shutdown();
		}
	}

	public static void main(String[] args) {
		JavaSECommon.initOnce();
		try {
			(new TestResponderClient()).run();
			// System.exit(0);
		} catch (Throwable e) {
			Logger.error("start error ", e);
		}
	}

}
