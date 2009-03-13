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
import java.io.InterruptedIOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;
import org.bluecove.tester.util.RuntimeDetect;
import org.bluecove.tester.util.TimeUtils;

import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.CollectionUtils;

/**
 *
 */
public class TestResponderServerL2CAP extends Thread {

	private Object threadLocalBluetoothStack;

	private L2CAPConnectionNotifier serverConnection;

	private boolean isStoped = false;

	private boolean isRunning = false;

	private Vector concurrentConnectionsThreads = new Vector();

	private class ServerConnectionTread extends Thread {

		L2CAPConnection channel;

		boolean isRunning;

		ServerConnectionTread(L2CAPConnection channel) {
			this.channel = channel;
			this.isRunning = true;
			synchronized (concurrentConnectionsThreads) {
				concurrentConnectionsThreads.addElement(this);
			}
		}

		public void run() {
			try {
				receive(channel);
				if ((!isStoped) && (isRunning)) {
					try {
						Thread.sleep(Configuration.serverSleepB4ClosingConnection);
					} catch (InterruptedException e) {
					}
				}
			} finally {
				this.isRunning = false;
				IOUtils.closeQuietly(channel);
				synchronized (concurrentConnectionsThreads) {
					concurrentConnectionsThreads.removeElement(this);
				}
			}
		}

		void shutdown() {
			if (isRunning) {
				IOUtils.closeQuietly(channel);
			}
		}
	}

	private TestResponderServerL2CAP() {
		threadLocalBluetoothStack = Configuration.threadLocalBluetoothStack;
	}

	public static TestResponderServerL2CAP startServer() {
		TestResponderServerL2CAP srv = new TestResponderServerL2CAP();
		srv.start();
		return srv;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void run() {
		isStoped = false;
		try {
			runCreateConnectionNotifier();
		} catch (Throwable e) {
			Logger.error("L2CAP Server start error", e);
			isStoped = true;
			return;
		}
		try {
			runAcceptAndOpen();
		} catch (Throwable e) {
			Logger.error("L2CAP Server run error", e);
		} finally {
			close();
			Logger.info("L2CAP Server finished! " + TimeUtils.timeNowToString());
			isRunning = false;
		}
	}

	private void runCreateConnectionNotifier() throws IOException {
		RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);
		StringBuffer url = new StringBuffer(BluetoothTypesInfo.PROTOCOL_SCHEME_L2CAP);
		url.append("://localhost:").append(Configuration.blueCoveL2CAPUUID());
		url.append(";name=").append(Consts.RESPONDER_SERVERNAME).append("_l2");
		if (Configuration.useShortUUID.booleanValue()) {
			url.append("s");
		}
		url.append(Configuration.serverURLParams());
		url.append(";TransmitMTU=").append(TestResponderCommon.receiveMTU_max);
		url.append(";ReceiveMTU=").append(TestResponderCommon.receiveMTU_max);
		if ((RuntimeDetect.isBlueCove) && (Configuration.bluecovepsm != null)
				&& (Configuration.bluecovepsm.length() > 0)) {
			url.append(";bluecovepsm=").append(Configuration.bluecovepsm);
		}
		serverConnection = (L2CAPConnectionNotifier) Connector.open(url.toString());
		if (Configuration.testServiceAttributes.booleanValue()) {
			ServiceRecord record = LocalDevice.getLocalDevice().getRecord(serverConnection);
			if (record == null) {
				Logger.warn("Bluetooth ServiceRecord is null");
			} else {
				TestResponderServer.buildServiceRecord(record);
				try {
					LocalDevice.getLocalDevice().updateRecord(record);
					Logger.debug("L2CAP ServiceRecord updated");
				} catch (Throwable e) {
					Logger.error("L2CAP Service Record update error", e);
				}
			}
		}
	}

	private void runAcceptAndOpen() {
		int errorCount = 0;
		isRunning = true;
		boolean showServiceRecordOnce = true;
		serviceRunLoop: while (!isStoped) {
			L2CAPConnection channel;
			try {
				Logger.info("Accepting L2CAP connections");
				if (showServiceRecordOnce) {
					Logger.debug("L2Url:"
							+ LocalDevice.getLocalDevice().getRecord(serverConnection).getConnectionURL(
									Configuration.getRequiredSecurity(), false));
					showServiceRecordOnce = false;
				}
				channel = serverConnection.acceptAndOpen();
			} catch (InterruptedIOException e) {
				isStoped = true;
				break;
			} catch (Throwable e) {
				Logger.error("acceptAndOpen ", e);
				if (!(isStoped) && (errorCount > 3)) {
					isStoped = true;
					Logger.error("L2CAP Server stoped, too many errors");
				}
				if (isStoped) {
					break serviceRunLoop;
				}
				errorCount++;
				continue;
			}
			errorCount = 0;
			Logger.info("Received L2CAP connection");
			if (!Configuration.serverAcceptWhileConnected.booleanValue()) {
				try {
					receive(channel);
					if (!isStoped) {
						try {
							Thread.sleep(Configuration.serverSleepB4ClosingConnection);
						} catch (InterruptedException e) {
						}
					}
				} finally {
					IOUtils.closeQuietly(channel);
				}
			} else {
				ServerConnectionTread t = new ServerConnectionTread(channel);
				t.start();
			}
		}
	}

	void receive(L2CAPConnection channel) {
		try {
			int receiveLengthMax = channel.getReceiveMTU();
			byte[] buffer = new byte[receiveLengthMax];

			int receivedLength = channel.receive(buffer);

			if (receivedLength == 0) {
				Logger.debug("a zero length L2CAP packet is received");
			} else {
				Logger.debug("received L2CAP packet", buffer, 0, receivedLength);
				processData(channel, buffer, receivedLength);
			}

		} catch (Throwable e) {
			if (isStoped) {
				return;
			}
			Logger.error("L2CAP receive", e);
		}
	}

	private void processData(L2CAPConnection channel, byte[] buffer, int receivedLength) throws IOException {
		if ((receivedLength < 3) || (buffer[0] != Consts.SEND_TEST_START)) {
			Logger.debug("not a test client connected, will echo");
			runEcho(channel, buffer, receivedLength);
			return;
		}
		int testType = buffer[1];
		TestStatus testStatus = new TestStatus(testType);
		ConnectionHolderL2CAP c = new ConnectionHolderL2CAP(channel);

		TestTimeOutMonitor monitorConnection = TestTimeOutMonitor.create("test" + testType, c,
				Configuration.serverTestTimeOutSec);

		byte[] initialData = new byte[receivedLength - CommunicationTesterL2CAP.INITIAL_DATA_PREFIX_LEN];
		System.arraycopy(buffer, CommunicationTesterL2CAP.INITIAL_DATA_PREFIX_LEN, initialData, 0, receivedLength
				- CommunicationTesterL2CAP.INITIAL_DATA_PREFIX_LEN);

		try {
			CommunicationTesterL2CAP.runTest(testType, true, c, initialData, testStatus);

			TestResponderServer.countSuccess++;

			Logger.debug("Test# " + testType + " " + testStatus.getName() + " ok");
		} catch (Throwable e) {
			if (!isStoped) {
				TestResponderServer.failure.addFailure("test " + testType + " " + testStatus.getName(), e);
			}
			Logger.error("Test# " + testType + " " + testStatus.getName() + " error", e);
		} finally {
			monitorConnection.finish();
		}
	}

	private void runEcho(L2CAPConnection channel, byte[] buffer, int receivedLength) throws IOException {
		RemoteDevice device = RemoteDevice.getRemoteDevice(channel);
		boolean authorized = false;
		try {
			authorized = device.isAuthorized(channel);
		} catch (Throwable blucoveIgnoe) {
		}
		Logger.debug("connected:" + device.getBluetoothAddress() + (device.isAuthenticated() ? " Auth" : "")
				+ (authorized ? " Authz" : "") + (device.isEncrypted() ? " Encr" : ""));
		Logger.debug("ReceiveMTU=" + channel.getReceiveMTU() + " TransmitMTU=" + channel.getTransmitMTU());

		echo(channel, buffer, receivedLength);
		mainLoop: while (true) {
			while (!channel.ready()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break mainLoop;
				}
			}
			int receiveMTU = channel.getReceiveMTU();
			byte[] data = new byte[receiveMTU];
			int length = channel.receive(data);
			echo(channel, data, length);
		}
	}

	private void echo(L2CAPConnection channel, byte[] buffer, int receivedLength) throws IOException {
		boolean cBufHasBinary = false;
		int messageLength = receivedLength;
		for (int k = 0; k < receivedLength; k++) {
			char c = (char) buffer[k];
			if (c < ' ') {
				if ((c == '\n') && (k == receivedLength - 1)) {
					messageLength = receivedLength - 1;
					break;
				}
				cBufHasBinary = true;
				break;
			}
		}
		String message;
		if (messageLength != 0) {
			message = new String(buffer, 0, messageLength);
		} else {
			message = "";
		}
		StringBuffer buf = new StringBuffer(message);
		if (cBufHasBinary) {
			buf.append(" [");
			for (int k = 0; k < receivedLength; k++) {
				buf.append(Integer.toHexString(buffer[k])).append(' ');
			}
			buf.append("]");
		}
		buf.append(" (").append(receivedLength).append(")");
		Logger.debug("|" + buf.toString());

		byte[] reply = new byte[receivedLength];
		if (receivedLength != 0) {
			System.arraycopy(buffer, 0, reply, 0, receivedLength);
		}
		channel.send(reply);
	}

	void close() {
		try {
			if (serverConnection != null) {
				serverConnection.close();
			}
			Logger.debug("L2CAP ServerConnection closed");
		} catch (Throwable e) {
			Logger.error("L2CAP Server stop error", e);
		}
	}

	void closeServer() {
		isStoped = true;
		close();
	}

	public void closeServerClientConnections() {
		Vector copy = CollectionUtils.copy(concurrentConnectionsThreads);
		for (Enumeration iter = copy.elements(); iter.hasMoreElements();) {
			ServerConnectionTread t = (ServerConnectionTread) iter.nextElement();
			t.shutdown();
		}
	}

	public int countClientConnections() {
		int count = 0;
		synchronized (concurrentConnectionsThreads) {
			for (Enumeration iter = concurrentConnectionsThreads.elements(); iter.hasMoreElements();) {
				ServerConnectionTread t = (ServerConnectionTread) iter.nextElement();
				if (t.isRunning) {
					count++;
				}
			}
		}
		return count;
	}
}
