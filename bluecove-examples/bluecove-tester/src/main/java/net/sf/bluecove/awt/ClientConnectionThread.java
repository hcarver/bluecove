/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove.awt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.bluetooth.L2CAPConnection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.ConnectionHolder;
import net.sf.bluecove.ConnectionHolderL2CAP;
import net.sf.bluecove.ConnectionHolderStream;
import net.sf.bluecove.Logger;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.IOUtils;
import net.sf.bluecove.util.StringUtils;
import net.sf.bluecove.util.TimeUtils;

/**
 *
 * 
 */
public class ClientConnectionThread extends Thread {

	Object threadLocalBluetoothStack;

	private static int connectionCount = 0;

	private String serverURL;

	private ConnectionHolder c;

	private boolean stoped = false;

	boolean isRunning = false;

	boolean isConnecting = false;

	long receivedCount = 0;

	long reportedSize = 0;

	long receivedPacketsCount = 0;

	boolean rfcomm;

	public static final int interpretDataChar = 0;

	public static final int interpretDataCharArray = 1;

	public static final int interpretDataStats = 2;

	public static final int interpretDataStatsArray = 3;

	public static final int interpretIgnore = 4;

	int interpretData = interpretDataChar;

	long reported = 0;

	String logPrefix = "";

	private StringBuffer dataBuf = new StringBuffer();

	private boolean binaryData = false;

	private FileOutputStream fileOut;

	ClientConnectionThread(String serverURL) {
		super("ClientConnectionThread" + (++connectionCount));
		this.serverURL = serverURL;
	}

	String getLocalBluetoothId() {
		if (threadLocalBluetoothStack == null) {
			return "";
		} else {
			return threadLocalBluetoothStack.toString();
		}
	}

	public void run() {
		try {
			rfcomm = BluetoothTypesInfo.isRFCOMM(serverURL);
			if (!rfcomm && !BluetoothTypesInfo.isL2CAP(serverURL)) {
				Logger.error(logPrefix + "unsupported connection type " + serverURL);
				return;
			}
			Configuration.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);
			Connection conn = null;
			try {
				isConnecting = true;
				Logger.debug(logPrefix + "Connecting:" + serverURL + " ...");
				conn = Connector.open(serverURL);
			} catch (IOException e) {
				Logger.error(logPrefix + "Connection error", e);
				return;
			} finally {
				isConnecting = false;
			}
			if (rfcomm) {
				ConnectionHolderStream cs = new ConnectionHolderStream((StreamConnection) conn);
				c = cs;
				cs.is = cs.conn.openInputStream();
				cs.os = cs.conn.openOutputStream();
				isRunning = true;
				while (!stoped) {
					if (interpretData == interpretIgnore) {
						Thread.sleep(777);
					} else if ((interpretData == interpretDataCharArray) || (interpretData == interpretDataStatsArray)) {
						byte b[] = new byte[0xFF];
						int readLen = cs.is.read(b);
						if (readLen == -1) {
							Logger.debug(logPrefix + "EOF recived");
							break;
						}
						receivedCount += readLen;
						for (int k = 0; k < readLen; k++) {
							printdataReceivedRFCOMM(b[k]);
						}
					} else {
						int data = cs.is.read();
						if (data == -1) {
							Logger.debug(logPrefix + "EOF recived");
							break;
						}
						receivedCount++;
						printdataReceivedRFCOMM(data);
					}
				}
				if (dataBuf.length() > 0) {
					Logger.debug(logPrefix + "cc:" + StringUtils.toBinaryText(dataBuf));
				}
			} else { // l2cap
				ConnectionHolderL2CAP lc = new ConnectionHolderL2CAP((L2CAPConnection) conn);
				isRunning = true;
				c = lc;
				while (!stoped) {
					if (interpretData == interpretIgnore) {
						Thread.sleep(777);
					} else {
						if ((interpretData != interpretDataCharArray) || (interpretData != interpretDataStatsArray)) {
							while ((!lc.channel.ready()) && (!stoped)) {
								Thread.sleep(100);
							}
						}
						if (stoped) {
							break;
						}
						int receiveMTU = lc.channel.getReceiveMTU();
						byte[] data = new byte[receiveMTU];
						int length = lc.channel.receive(data);
						receivedCount += length;
						receivedPacketsCount++;
						printdataReceivedL2CAP(data, length);
					}
				}
			}
		} catch (IOException e) {
			if (!stoped) {
				Logger.error(logPrefix + "Communication error", e);
			} else {
				Logger.debug(logPrefix + "communication stopped", e);
			}
		} catch (Throwable e) {
			Logger.error(logPrefix + "Error", e);
		} finally {
			isRunning = false;
			if (c != null) {
				c.shutdown();
			}
			closeFile();
		}
	}

	private void printdataReceivedRFCOMM(int data) {
		switch (interpretData) {
		case interpretDataChar:
		case interpretDataCharArray:
			char c = (char) data;
			if ((!binaryData) && (c < ' ')) {
				binaryData = true;
			}
			dataBuf.append(c);
			if (((!binaryData) && (c == '\n')) || (dataBuf.length() > 32)) {
				Logger.debug("cc:" + StringUtils.toBinaryText(dataBuf));
				dataBuf = new StringBuffer();
			}
			break;
		case interpretDataStatsArray:
		case interpretDataStats:
			long now = System.currentTimeMillis();
			if (now - reported > 5 * 1000) {
				int size = (int) (receivedCount - reportedSize);
				reportedSize = receivedCount;
				Logger.debug(logPrefix + "Received " + receivedCount + " bytes " + TimeUtils.bps(size, reported));
				reported = now;
			}
			break;
		}
		synchronized (this) {
			if (fileOut != null) {
				try {
					fileOut.write((char) data);
				} catch (IOException e) {
					Logger.debug(logPrefix + "file write error", e);
					closeFile();
				}
			}
		}
	}

	private void printdataReceivedL2CAP(byte[] data, int length) {
		switch (interpretData) {
		case interpretDataCharArray:
		case interpretDataChar:
			int messageLength = length;
			if ((length > 0) && (data[length - 1] == '\n')) {
				messageLength = length - 1;
			}
			StringBuffer buf = new StringBuffer();
			buf.append(logPrefix).append("cc:");
			if (messageLength != 0) {
				buf.append(StringUtils.toBinaryText(new StringBuffer(new String(data, 0, messageLength))));
			}
			buf.append(" (").append(length).append(")");
			Logger.debug(buf.toString());
			break;
		case interpretDataStatsArray:
		case interpretDataStats:
			long now = System.currentTimeMillis();
			if (now - reported > 5 * 1000) {
				int size = (int) (receivedCount - reportedSize);
				reportedSize = receivedCount;
				Logger.debug(logPrefix + "Received " + receivedPacketsCount + " packet(s), " + receivedCount
						+ " bytes " + TimeUtils.bps(size, reported));
				reported = now;
			}
			break;
		}
		synchronized (this) {
			if (fileOut != null) {
				try {
					fileOut.write(data, 0, length);
				} catch (IOException e) {
					Logger.debug(logPrefix + "file write error", e);
					closeFile();
				}
			}
		}
	}

	public void shutdown() {
		stoped = true;
		if (c != null) {
			c.shutdown();
		}
		c = null;
		closeFile();
	}

	private synchronized void closeFile() {
		if (fileOut != null) {
			try {
				fileOut.flush();
			} catch (IOException ignore) {
			}
			IOUtils.closeQuietly(fileOut);
			fileOut = null;
		}
	}

	public void updateDataReceiveType(int type, boolean saveToFile) {
		interpretData = type;

		if ((!saveToFile) && (fileOut != null)) {
			closeFile();
		} else if ((saveToFile) && (fileOut == null)) {
			SimpleDateFormat fmt = new SimpleDateFormat("MM-dd_HH-mm-ss");
			File file = new File("data-" + BluetoothTypesInfo.extractBluetoothAddress(serverURL)
					+ fmt.format(new Date()) + ".bin");
			try {
				fileOut = new FileOutputStream(file);
				Logger.info(logPrefix + "saving data to file " + file.getAbsolutePath());
			} catch (IOException e) {
			}
		}
	}

	public void send(final byte data[]) {
		Thread t = new Thread("ClientConnectionSendThread" + (++connectionCount)) {
			public void run() {
				try {
					if (rfcomm) {
						((ConnectionHolderStream) c).os.write(data);
					} else {
						((ConnectionHolderL2CAP) c).channel.send(data);
					}
					Logger.debug(logPrefix + "data " + data.length + " sent");
				} catch (IOException e) {
					Logger.error(logPrefix + "Communication error", e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
}
