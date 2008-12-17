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
package net.sf.bluecove.tests;

import java.io.IOException;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;
import org.bluecove.tester.util.RuntimeDetect;
import org.bluecove.tester.util.TimeUtils;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.ConnectionHolderStream;
import net.sf.bluecove.TestStatus;

/**
 *
 * 
 */
public class RfTrafficGenerator {

	final static int sequenceSizeMin = 18;

	public static final byte END_MARKER = -1;

	public static class Config {

		int sequenceSleep;

		int sequenceSize;

		int durationMSec = 0;

		boolean init(ConnectionHolderStream c, boolean server, String messagePrefix) throws IOException {
			if (server) {
				sequenceSleep = c.is.read();
				if (sequenceSleep == -1) {
					Logger.debug("EOF received");
					return false;
				}
				sequenceSize = c.is.read();
				if (sequenceSize == -1) {
					Logger.debug("EOF received");
					return false;
				}
				durationMSec = c.is.read();
				if (durationMSec == -1) {
					Logger.debug("EOF received");
					return false;
				}
			} else {
				sequenceSize = Configuration.tgSize & 0xFF;
				sequenceSleep = Configuration.tgSleep & 0xFF;
				durationMSec = Configuration.tgDurationMin;
			}
			sequenceSleep = sequenceSleep * 10;
			if (sequenceSize < sequenceSizeMin) {
				sequenceSize = sequenceSizeMin;
			}
			switch (sequenceSize) {
			case 251:
				// 1K
				sequenceSize = 0x400;
				break;
			case 252:
				// 2K
				sequenceSize = 0x800;
				break;
			case 253:
				// 3K
				sequenceSize = 0xC00;
				break;
			case 254:
				// 4K
				sequenceSize = 0x1000;
				break;
			case 255:
				// 5K
				sequenceSize = 0x1400;
				break;
			}
			Logger.debug(messagePrefix + " size selected " + sequenceSize + " byte");
			Logger.debug(messagePrefix + " duration " + durationMSec + " minutes");
			durationMSec *= 60000;

			return true;
		}
	}

	public static Config getConfig(ConnectionHolderStream c, boolean server, String messagePrefix) throws IOException {
		Config cf = new Config();
		if (cf.init(c, server, messagePrefix)) {
			return cf;
		} else {
			return null;
		}
	}

	public static void trafficGeneratorClientInit(ConnectionHolderStream c) throws IOException {
		byte sequenceSleep = (byte) (Configuration.tgSleep & 0xFF);
		byte sequenceSize = (byte) (Configuration.tgSize & 0xFF);
		byte durationMin = (byte) (Configuration.tgDurationMin & 0xFF);
		c.os.write(sequenceSleep);
		c.os.write(sequenceSize);
		c.os.write(durationMin);
		c.os.flush();
	}

	public static void trafficGeneratorWrite(ConnectionHolderStream c, Config cfg, TestStatus testStatus)
			throws IOException {
		if (cfg.sequenceSleep > 0) {
			Logger.debug("RF write sleep selected " + cfg.sequenceSleep + " msec");
		} else {
			Logger.debug("RF write no sleep");
		}
		long sequenceSentCount = 0;
		int reportedSize = 0;

		// Create test data
		byte[] data = new byte[cfg.sequenceSize];
		for (int i = 1; i < cfg.sequenceSize; i++) {
			data[i] = (byte) i;
		}

		long start = System.currentTimeMillis();
		long reported = start;
		try {
			mainLoop: do {
				IOUtils.long2Bytes(sequenceSentCount, 8, data, 0);
				long sendTime = System.currentTimeMillis();
				IOUtils.long2Bytes(sendTime, 8, data, 8);
				boolean finalArray = false;
				if ((cfg.durationMSec != 0) && (sendTime > start + cfg.durationMSec)) {
					finalArray = true;
					data[cfg.sequenceSize - 2] = END_MARKER;
					data[cfg.sequenceSize - 1] = END_MARKER;
				}

				c.os.write(data);
				sequenceSentCount++;
				reportedSize += cfg.sequenceSize;
				c.active();
				long now = System.currentTimeMillis();
				if (now - reported > 5 * 1000) {
					Logger.debug("RF Sent " + sequenceSentCount + " array(s) " + TimeUtils.bps(reportedSize, reported));
					reported = now;
					reportedSize = 0;
				}
				if (finalArray) {
					break;
				}
				if (cfg.sequenceSleep > 0) {
					try {
						Thread.sleep(cfg.sequenceSleep);
					} catch (InterruptedException e) {
						break mainLoop;
					}
					c.active();
				}
			} while (c.isConnectionOpen() && (!testStatus.isRunCompleate()));
		} finally {
			testStatus.setRunCompleate();

			String m;
			m = "RF Total " + sequenceSentCount + " array(s)";
			testStatus.addReplyMessage(m);
			Logger.debug(m);

			long totalB = sequenceSentCount * cfg.sequenceSize;
			m = "RF Total " + (totalB / 1024) + " KBytes";
			testStatus.addReplyMessage(m);
			Logger.debug(m);

			m = "RF Total write speed " + TimeUtils.bps(totalB, start);
			testStatus.addReplyMessage(m);
			Logger.debug(m);
		}
	}

	public static void trafficGeneratorStatusReadStart(final ConnectionHolderStream c, final TestStatus testStatus) {
		Runnable r = new Runnable() {
			public void run() {
				try {
					trafficGeneratorStatusRead(c, testStatus);
				} catch (IOException e) {
					Logger.error("reader", e);
				}
			}
		};
		Thread t = RuntimeDetect.cldcStub.createNamedThread(r, "RFtgStatusReciver");
		t.start();
	}

	public static void trafficGeneratorStatusRead(ConnectionHolderStream c, final TestStatus testStatus)
			throws IOException {
		try {
			int endMakerStatus = 0;
			byte[] byteAray = new byte[1];
			while (c.isConnectionOpen() && (!testStatus.isRunCompleate())) {
				if (c.is.available() > 0) {
					int read = c.is.read(byteAray);
					if (read == -1) {
						testStatus.setStreamClosed();
						break;
					}
					endMakerStatus = detectEndMaker(endMakerStatus, byteAray, read);
					if (endMakerStatus > 0) {
						break;
					}
					c.active();
				} else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		} finally {
			testStatus.setRunCompleate();
		}
	}

	public static void trafficGeneratorReadStart(final ConnectionHolderStream c, final boolean server,
			final TestStatus testStatus) {
		Runnable r = new Runnable() {
			public void run() {
				try {
					trafficGeneratorRead(c, server, testStatus);
				} catch (IOException e) {
					Logger.error("reader", e);
				}
			}
		};
		Thread t = RuntimeDetect.cldcStub.createNamedThread(r, "RFtgReciver");
		t.start();
	}

	/**
	 * Expects 2 END_MARKER bytes
	 */
	public static int detectEndMaker(int endMakerStatus, byte[] byteAray, int len) {
		if (len > 0) {
			if (byteAray[len - 1] == END_MARKER) {
				if (len == 1) {
					if (endMakerStatus < 0) {
						return 1;
					} else {
						return -1;
					}
				} else if (byteAray[len - 2] == END_MARKER) {
					return 1;
				}
			}
			return 0;
		} else {
			return endMakerStatus;
		}
	}

	public static void trafficGeneratorRead(ConnectionHolderStream c, boolean server, final TestStatus testStatus)
			throws IOException {
		Config cf = new Config();
		if (!cf.init(c, server, "RF read")) {
			return;
		}
		long totalSize = 0;
		long sequenceReceivedCount = 0;
		long start = System.currentTimeMillis();
		long reported = start;
		long reportedSize = 0;
		byte[] byteAray = new byte[cf.sequenceSize];
		int endMakerStatus = 0;
		try {
			while (c.isConnectionOpen() && (!testStatus.isRunCompleate())) {
				int read = c.is.read(byteAray);
				if (read == -1) {
					testStatus.setStreamClosed();
					break;
				}
				endMakerStatus = detectEndMaker(endMakerStatus, byteAray, read);
				if (endMakerStatus > 0) {
					break;
				}
				sequenceReceivedCount++;
				totalSize += read;
				reportedSize += read;
				c.active();
				long now = System.currentTimeMillis();
				if (now - reported > 5 * 1000) {
					Logger.debug("RF Received " + sequenceReceivedCount + " array(s) "
							+ TimeUtils.bps(reportedSize, reported));
					reported = now;
					reportedSize = 0;
				}
			}
		} finally {
			testStatus.setRunCompleate();
			String m;
			m = "RF Total " + sequenceReceivedCount + " array(s)";
			testStatus.addReplyMessage(m);
			Logger.debug(m);

			m = "RF Total " + (totalSize / 1024) + " KBytes";
			testStatus.addReplyMessage(m);
			Logger.debug(m);

			m = "RF Total read speed " + TimeUtils.bps(totalSize, start);
			testStatus.addReplyMessage(m);
			Logger.debug(m);
		}

	}

}
