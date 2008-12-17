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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.RuntimeDetect;
import org.bluecove.tester.util.TimeUtils;

import junit.framework.Assert;
import net.sf.bluecove.tests.RfTrafficGenerator;
import net.sf.bluecove.tests.TwoThreadsPerConnection;
import net.sf.bluecove.tests.RfTrafficGenerator.Config;
import net.sf.bluecove.util.ValueHolder;

public class CommunicationTester extends CommunicationData {

	public static boolean dataOutputStreamFlush = true;

	public static int clientConnectionOpenRetry = 3;

	static void sendString(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringData);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
	}

	static void readString(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringData, got);
	}

	static void sendUTFString(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringUTFData);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
	}

	static void readUTFString(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringUTFData, got);
	}

	static void sendByte(OutputStream os) throws IOException {
		os.write(aKnowndPositiveByte);
		os.write(aKnowndNegativeByte);

		// Test int conversions
		int bp = aKnowndPositiveByte;
		os.write(bp);
		int bn = aKnowndNegativeByte;
		os.write(bn);

		for (int i = 1; i < byteCount; i++) {
			os.write((byte) i);
		}
		for (int i = 0; i < byteAray.length; i++) {
			os.write(byteAray[i]);
		}
		// The byte to be written is the eight low-order bits of the argument b.
		os.write(0xABC);
		os.write(aKnowndPositiveByte);
	}

	static void readByte(InputStream is) throws IOException {
		Assert.assertEquals("positiveByte", aKnowndPositiveByte, (byte) is.read());
		Assert.assertEquals("negativeByte", aKnowndNegativeByte, (byte) is.read());
		Assert.assertEquals("positiveByte written(int)", aKnowndPositiveByte, (byte) is.read());
		Assert.assertEquals("negativeByte written(int)", aKnowndNegativeByte, (byte) is.read());
		for (int i = 1; i < byteCount; i++) {
			byte got = (byte) is.read();
			Assert.assertEquals("t1, byte [" + i + "]", (byte) i, got);
		}
		for (int i = 0; i < byteAray.length; i++) {
			byte got = (byte) is.read();
			Assert.assertEquals("t2, byte [" + i + "]", byteAray[i], got);
		}
		int abc = is.read();
		Assert.assertEquals("written(0xABC)", 0xBC, abc);
		Assert.assertEquals("positiveByte", aKnowndPositiveByte, (byte) is.read());
	}

	static void sendBytes256(OutputStream os) throws IOException {
		// Write all 256 bytes
		int cnt = 0;
		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
			try {
				os.write((byte) i);
				cnt++;
			} catch (IOException e) {
				Logger.debug("Sent only " + cnt + " bytes");
				throw e;
			}
		}
		// Send one more byte to see is 0xFF is not EOF
		os.write(aKnowndPositiveByte);
	}

	static void readBytes256(InputStream is) throws IOException {
		// Read all 256 bytes
		int cnt = 0;
		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
			byte got;
			try {
				got = (byte) is.read();
				cnt++;
			} catch (IOException e) {
				Logger.debug("Received only " + cnt + " bytes");
				throw e;
			}
			Assert.assertEquals("all bytes [" + i + "]", (byte) i, got);
		}
		Assert.assertEquals("conformation that 0xFF is not EOF", aKnowndPositiveByte, (byte) is.read());
	}

	static void sendByteAray(OutputStream os) throws IOException {
		os.write(byteAray);
	}

	static void readByteAray(InputStream is) throws IOException {
		byte[] byteArayGot = new byte[byteAray.length];
		int got = is.read(byteArayGot);
		Assert.assertEquals("byteAray.len", byteAray.length, got);
		for (int i = 0; i < byteAray.length; i++) {
			Assert.assertEquals("byte", byteAray[i], byteArayGot[i]);
		}
	}

	static void sendDataStream(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF(stringData);
		dos.writeInt(1025);
		dos.writeLong(567890025);
		dos.writeBoolean(true);
		dos.writeBoolean(false);
		dos.writeChar('O');
		dos.writeShort(541);
		// CLDC_1_0 dos.writeFloat((float)3.14159);
		// CLDC_1_0 dos.writeDouble(Math.E);
		dos.writeByte(aKnowndPositiveByte);
		dos.writeByte(aKnowndNegativeByte);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
	}

	static void readDataStream(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String got = dis.readUTF();
		Assert.assertEquals("ReadString", stringData, got);
		Assert.assertEquals("ReadInt", 1025, dis.readInt());
		Assert.assertEquals("ReadLong", 567890025, dis.readLong());
		Assert.assertEquals("ReadBoolean", true, dis.readBoolean());
		Assert.assertEquals("ReadBoolean2", false, dis.readBoolean());
		Assert.assertEquals("ReadChar", 'O', dis.readChar());
		Assert.assertEquals("readShort", 541, dis.readShort());
		// CLDC_1_0 Assert.assertEquals("readFloat", (float)3.14159,
		// dis.readFloat(), (float)0.0000001);
		// CLDC_1_0 Assert.assertEquals("readDouble", Math.E, dis.readDouble(),
		// 0.0000000000000001);
		Assert.assertEquals("positiveByte", aKnowndPositiveByte, dis.readByte());
		Assert.assertEquals("negativeByte", aKnowndNegativeByte, dis.readByte());
	}

	private static void sendStreamAvailable(InputStream is, OutputStream os) throws IOException {
		for (int i = 1; i < streamAvailableByteCount; i++) {
			os.write(i);
			if (i % 10 == 0) {
				os.flush();
			}
		}
		// Long test need confirmation
		os.flush();
		byte got = (byte) is.read();
		Assert.assertEquals("Confirmation byte", streamAvailableByteCount, got);
	}

	private static void readStreamAvailable(InputStream is, OutputStream os) throws IOException {
		int available = 0;
		for (int i = 1; i < streamAvailableByteCount; i++) {
			boolean hasData = (available > 0);
			int tryCount = 0;
			while (!hasData) {
				// This blocks on Nokia(Srv) on second call connected to
				// Widcomm(Client)
				try {
					available = is.available();
				} catch (IOException e) {
					Logger.debug("(m1) Received only " + i + " bytes");
					throw e;
				}
				if (available > 0) {
					hasData = true;
				} else if (available < 0) {
					Assert.fail("negative available");
				}
				tryCount++;
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					Assert.fail("Test Interrupted");
				}
				if (tryCount > 70) {
					Assert.fail("Test Available took too long, got " + i + " bytes");
				}
			}

			byte got;
			try {
				got = (byte) is.read();
			} catch (IOException e) {
				Logger.debug("(m2) Received only " + i + " bytes");
				throw e;
			}
			Assert.assertEquals("byte[" + i + "]", i, got);
			available--;
		}
		os.write(streamAvailableByteCount);
		os.flush();
	}

	private static void sendEOF(ConnectionHolderStream c, TestStatus testStatus) throws IOException {
		c.os.write(aKnowndPositiveByte);
		c.os.flush();
		Assert.assertEquals("byte received", aKnowndNegativeByte, (byte) c.is.read());
		c.disconnected();
		c.os.close();
		c.is.close();
		testStatus.streamClosed = true;
	}

	private static void readEOF(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		Assert.assertEquals("byte", aKnowndPositiveByte, (byte) is.read());
		os.write(aKnowndNegativeByte);
		os.flush();
		long startWait = System.currentTimeMillis();
		Assert.assertEquals("EOF expected", -1, is.read());
		testStatus.streamClosed = true;
		Assert.assertFalse("Took too long to close", TimeUtils.since(startWait) > 7 * 1000);
		testStatus.isSuccess = true;
	}

	private static void sendArayEOF(ConnectionHolderStream c, TestStatus testStatus) throws IOException {
		c.os.write(aKnowndPositiveByte);
		c.os.write(aKnowndNegativeByte);
		c.os.flush();
		// Let the server read the message
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			Assert.fail("Test Interrupted");
		}
		c.disconnected();
		c.os.close();
		c.is.close();
		testStatus.streamClosed = true;
	}

	private static void readArayEOF(InputStream is, OutputStream os, TestStatus testStatus) throws IOException {
		byte[] byteArayGot = new byte[3];
		int got = is.read(byteArayGot);
		if (got == 1) {
			int size = is.read(byteArayGot, 1, 2);
			if (size != -1) {
				got += size;
			}
		}
		Assert.assertEquals("byteAray.len", 2, got);
		Assert.assertEquals("byte1", aKnowndPositiveByte, byteArayGot[0]);
		Assert.assertEquals("byte2", aKnowndNegativeByte, byteArayGot[1]);
		int got2 = is.read(byteArayGot);
		Assert.assertEquals("EOF expected", -1, got2);
		testStatus.streamClosed = true;
		testStatus.isSuccess = true;
	}

	private static void sendClosedConnection(ConnectionHolderStream c, TestStatus testStatus) throws IOException {
		c.os.write(aKnowndPositiveByte);
		c.os.write(aKnowndNegativeByte);
		c.os.flush();
		// Let the server read the message
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			Assert.fail("Test Interrupted");
		}
		c.disconnected();
		c.os.close();
		c.is.close();
		testStatus.streamClosed = true;

		try {
			c.os.write(byteAray);
			c.os.flush();
			Assert.fail("Can write to closed OutputStream");
		} catch (IOException ok) {
			testStatus.isSuccess = true;
		}
	}

	private static void readClosedConnection(ConnectionHolderStream c, TestStatus testStatus) throws IOException {
		Assert.assertEquals("byte1", aKnowndPositiveByte, (byte) c.is.read());
		Assert.assertEquals("byte2", aKnowndNegativeByte, (byte) c.is.read());
		testStatus.streamClosed = true;
		try {
			Assert.assertEquals("EOF expected", -1, c.is.read());
		} catch (IOException e) {
			if (RuntimeDetect.isBlueCove) {
				Logger.error("EOF IOException not expected", e);
				Assert.fail("EOF IOException not expected [" + e.toString() + "]");
			}
		}
		c.disconnected();
		try {
			c.os.write(byteAray);
			c.os.flush();
			Assert.fail("Can write to closed BT Connection");
		} catch (IOException ok) {
			testStatus.isSuccess = true;
		}
	}

	private static void serverRemoteDevice(StreamConnection conn, InputStream is, OutputStream os, TestStatus testStatus)
			throws IOException {
		RemoteDevice device = RemoteDevice.getRemoteDevice(conn);
		Logger.debug("is connected to BTAddress " + device.getBluetoothAddress());
		DataInputStream dis = new DataInputStream(is);
		DataOutputStream dos = new DataOutputStream(os);
		String gotBluetoothAddress = dis.readUTF();
		Assert.assertEquals("PairBTAddress", gotBluetoothAddress.toUpperCase(), device.getBluetoothAddress()
				.toUpperCase());

		boolean remoreIsAuthenticated = dis.readBoolean();
		boolean remoreIsEncrypted = dis.readBoolean();

		boolean isAuthenticated = device.isAuthenticated();
		boolean isEncrypted = device.isEncrypted();

		dos.writeBoolean(isAuthenticated);
		dos.writeBoolean(isEncrypted);
		if (dataOutputStreamFlush) {
			dos.flush();
		}

		if (Configuration.authenticate.booleanValue()) {
			if (!isAuthenticated) {
				Logger.error("wrong isAuthenticated " + isAuthenticated);
				testStatus.isError = true;
			} else {
				Logger.debug("isAuthenticated OK " + isAuthenticated);
			}
		} else {
			Logger.debug("isAuthenticated " + isAuthenticated);
		}
		if (remoreIsAuthenticated != isAuthenticated) {
			Logger.error("this Authenticated " + isAuthenticated + " != remote " + remoreIsAuthenticated);
			testStatus.isError = true;
		}

		if (Configuration.encrypt.booleanValue()) {
			if (!isEncrypted) {
				Logger.error("wrong isEncrypted " + isEncrypted);
				testStatus.isError = true;
			} else {
				Logger.debug("isEncrypted OK " + isEncrypted);
			}
		} else if (isEncrypted && (remoreIsEncrypted == isEncrypted)) {
			Logger.debug("isEncrypted OK " + isEncrypted);
		} else {
			Logger.debug("isEncrypted " + isEncrypted);
		}
		if (remoreIsEncrypted != isEncrypted) {
			Logger.error("this Encrypted " + isEncrypted + " != remote " + remoreIsEncrypted);
			testStatus.isError = true;
		}
	}

	private static void clientRemoteDevice(StreamConnection conn, InputStream is, OutputStream os, TestStatus testStatus)
			throws IOException {
		RemoteDevice device = RemoteDevice.getRemoteDevice(conn);
		Logger.debug("is connected toBTAddress " + device.getBluetoothAddress());
		DataOutputStream dos = new DataOutputStream(os);
		DataInputStream dis = new DataInputStream(is);
		dos.writeUTF(LocalDevice.getLocalDevice().getBluetoothAddress());
		if (dataOutputStreamFlush) {
			dos.flush();
		}
		Assert.assertEquals("PairBTAddress", testStatus.pairBTAddress.toUpperCase(), device.getBluetoothAddress()
				.toUpperCase());

		boolean isAuthenticated = device.isAuthenticated();
		boolean isEncrypted = device.isEncrypted();

		dos.writeBoolean(isAuthenticated);
		dos.writeBoolean(isEncrypted);
		if (dataOutputStreamFlush) {
			dos.flush();
		}
		boolean remoreIsAuthenticated = dis.readBoolean();
		boolean remoreIsEncrypted = dis.readBoolean();

		if (Configuration.authenticate.booleanValue() == isAuthenticated) {
			Logger.debug("isAuthenticated OK " + Configuration.authenticate);
		} else if (Configuration.authenticate.booleanValue() && !isAuthenticated) {
			Logger.error("wrong isAuthenticated " + isAuthenticated);
		} else {
			Logger.debug("isAuthenticated OK " + isAuthenticated);
		}
		if (remoreIsAuthenticated != isAuthenticated) {
			Logger.error("this Authenticated " + isAuthenticated + " != remote " + remoreIsAuthenticated);
			testStatus.isError = true;
		}

		if (Configuration.encrypt.booleanValue() == isEncrypted) {
			Logger.debug("isEncrypted OK " + Configuration.encrypt);
		} else if (Configuration.encrypt.booleanValue() && !isEncrypted) {
			Logger.error("wrong isEncrypted " + isEncrypted);
		} else {
			Logger.debug("isEncrypted OK " + isEncrypted);
		}
		if (remoreIsEncrypted != isEncrypted) {
			Logger.error("this Encrypted " + isEncrypted + " != remote " + remoreIsEncrypted);
			testStatus.isError = true;
		}
	}

	static void sendByte4clientToClose(OutputStream os, InputStream is, TestStatus testStatus) throws IOException {
		os.write(aKnowndPositiveByte);
		os.flush();

		TimeUtils.sleep(1 * 1000);

		// Do not send any reply to client.
		testStatus.streamClosed = true;

		long startWait = System.currentTimeMillis();
		// wait for client to close connection, This has been tested in
		// TEST_EOF_READ
		int eof = 0;
		try {
			eof = is.read();
			Assert.assertEquals("EOF expected", -1, eof);
		} catch (IOException e) {
			Logger.debug("OK conn.closed");
		}
		Assert.assertFalse("Took too long to close", TimeUtils.since(startWait) > 7 * 1000);
		testStatus.isSuccess = true;
	}

	static void reciveByteAndCloseStream(boolean testArray, final ConnectionHolderStream c, TestStatus testStatus)
			throws IOException {
		Assert.assertEquals("byte", aKnowndPositiveByte, (byte) c.is.read());
		final ValueHolder whenClose = new ValueHolder();
		final ValueHolder alreadyClose = new ValueHolder(false);
		final ValueHolder whoClose = new ValueHolder();
		final boolean debug = true;
		Runnable r = new Runnable() {
			public void run() {
				TimeUtils.sleep(500);
				c.disconnected();
				if (debug) {
					Logger.debug("try to closed");
				}
				whenClose.valueLong = System.currentTimeMillis();
				whoClose.valueInt = 1;
				try {
					// No effect on Nokia
					c.conn.close();
				} catch (IOException e) {
					Logger.debug("error in conn close", e);
				}
				if (debug) {
					Logger.debug("conn.closed");
				}
				whenClose.valueLong = System.currentTimeMillis();
				TimeUtils.sleep(100);
				if (!alreadyClose.valueBoolean) {
					TimeUtils.sleep(600);
					whenClose.valueLong = System.currentTimeMillis();
					whoClose.valueInt = 2;
					try {
						c.is.close();
					} catch (IOException e) {
						Logger.debug("error in is close", e);
					}
					if (debug) {
						Logger.debug("is.closed");
					}
					whenClose.valueLong = System.currentTimeMillis();
					TimeUtils.sleep(100);
					if (!alreadyClose.valueBoolean) {
						TimeUtils.sleep(600);
						whenClose.valueLong = System.currentTimeMillis();
						whoClose.valueInt = 3;
						try {
							c.os.close();
						} catch (IOException e) {
							Logger.debug("error in os close", e);
						}
						if (debug) {
							Logger.debug("os.closed");
						}
						whenClose.valueLong = System.currentTimeMillis();
					}
				}
				if (debug) {
					Logger.debug("close thread finished");
				}
			}
		};
		Thread t = RuntimeDetect.cldcStub.createNamedThread(r, "ReciveStreamCloser");
		t.start();

		testStatus.streamClosed = true;
		// This will stuck since server is not sending any more data.
		// conn.close() should force read() to throw exception or return -1.
		int eof = 0;
		try {
			if (debug) {
				Logger.debug("try to read EOF");
			}
			// This is function under test
			if (testArray) {
				byte[] buf = new byte[2];
				eof = c.is.read(buf, 0, buf.length);
			} else {
				eof = c.is.read();
			}
			Assert.assertEquals("EOF expected", -1, eof);
			Logger.debug("OK read on conn.closed GOT EOF");
		} catch (IOException e) {
			Logger.debug("OK read on conn.closed throws Exception", e);
		} finally {
			if (debug) {
				Logger.debug("read EOF ends");
			}
		}
		alreadyClose.valueBoolean = true;
		long returenedDelay = System.currentTimeMillis() - whenClose.valueLong;
		if ((returenedDelay < 2 * 1000) || (returenedDelay > -2 * 1000)) {
			testStatus.isSuccess = true;
		} else {
			Assert.fail("Took too long " + (returenedDelay) + " to return");
		}
		switch (whoClose.valueInt) {
		case 1:
			Logger.debug("Closed by StreamConnection.close()");
			break;
		case 2:
			Logger.debug("Closed by InputStream.close()");
			break;
		case 3:
			Logger.debug("Closed by OutputStream.close()");
			break;
		default:
			Assert.fail("Closed by unknown source");
		}
	}

	static void sendByteArayLarge(ConnectionHolderStream c, InputStream is, OutputStream os, int araySize)
			throws IOException {
		long start = System.currentTimeMillis();
		c.setTestTimeOutSec(araySize / 1000);
		byte[] byteArayLarge = new byte[araySize];
		for (int i = 0; i < araySize; i++) {
			byteArayLarge[i] = (byte) (i & 0xF);
		}
		os.write(byteArayLarge);
		c.active();
		os.flush();
		int ok = is.read();
		c.active();
		Assert.assertEquals("confirmation expected", 1, ok);
		Logger.debug("send speed " + TimeUtils.bps(araySize, start));
	}

	static void readByteArayLarge(ConnectionHolderStream c, InputStream is, OutputStream os, int araySize)
			throws IOException {
		long start = System.currentTimeMillis();
		byte[] byteArayGot = new byte[araySize];
		int got = 0;
		c.setTestTimeOutSec(araySize / 1000);
		boolean readInterrupted = true;
		try {
			while (got < araySize) {
				int read = is.read(byteArayGot, got, araySize - got);
				if (read == -1) {
					break;
				}
				got += read;
				c.active();
			}
			readInterrupted = false;
		} finally {
			if (readInterrupted) {
				Logger.debug("Received only " + got);
			}
		}

		Assert.assertEquals("byteArayLarge.len", araySize, got);
		for (int i = 0; i < araySize; i++) {
			Assert.assertEquals("byte", (i & 0xF), byteArayGot[i]);
		}
		os.write(1);
		c.active();
		os.flush();
		Logger.debug("read speed " + TimeUtils.bps(araySize, start));
	}

	public static void runTest(int testType, boolean server, ConnectionHolderStream c, TestStatus testStatus)
			throws IOException {
		InputStream is = c.is;
		OutputStream os = c.os;
		switch (testType) {
		case TEST_STRING:
			testStatus.setName("STRING");
			if (server) {
				CommunicationTester.readString(is);
			} else {
				CommunicationTester.sendString(os);
			}
			break;
		case TEST_STRING_BACK:
			testStatus.setName("STRING_BACK");
			if (!server) {
				CommunicationTester.readString(is);
			} else {
				CommunicationTester.sendString(os);
			}
			break;
		case TEST_BYTE:
			testStatus.setName("BYTE");
			if (server) {
				CommunicationTester.readByte(is);
			} else {
				CommunicationTester.sendByte(os);
			}
			break;
		case TEST_BYTE_BACK:
			testStatus.setName("BYTE_BACK");
			if (!server) {
				CommunicationTester.readByte(is);
			} else {
				CommunicationTester.sendByte(os);
			}
			break;
		case TEST_STRING_UTF:
			testStatus.setName("STRING_UTF");
			if (server) {
				CommunicationTester.readUTFString(is);
			} else {
				CommunicationTester.sendUTFString(os);
			}
			break;
		case TEST_STRING_UTF_BACK:
			testStatus.setName("STRING_UTF_BACK");
			if (!server) {
				CommunicationTester.readUTFString(is);
			} else {
				CommunicationTester.sendUTFString(os);
			}
			break;
		case TEST_BYTE_ARRAY:
			testStatus.setName("BYTE_ARRAY");
			if (server) {
				CommunicationTester.readByteAray(is);
			} else {
				CommunicationTester.sendByteAray(os);
			}
			break;
		case TEST_BYTE_ARRAY_BACK:
			testStatus.setName("BYTE_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readByteAray(is);
			} else {
				CommunicationTester.sendByteAray(os);
			}
			break;
		case TEST_DataStream:
			testStatus.setName("DataStream");
			if (server) {
				CommunicationTester.readDataStream(is);
			} else {
				CommunicationTester.sendDataStream(os);
			}
			break;
		case TEST_DataStream_BACK:
			testStatus.setName("DataStream_BACK");
			if (!server) {
				CommunicationTester.readDataStream(is);
			} else {
				CommunicationTester.sendDataStream(os);
			}
			break;
		case TEST_StreamAvailable:
			testStatus.setName("StreamAvailable");
			if (server) {
				CommunicationTester.readStreamAvailable(is, os);
			} else {
				CommunicationTester.sendStreamAvailable(is, os);
			}
			break;
		case TEST_StreamAvailable_BACK:
			testStatus.setName("StreamAvailable_BACK");
			if (!server) {
				CommunicationTester.readStreamAvailable(is, os);
			} else {
				CommunicationTester.sendStreamAvailable(is, os);
			}
			break;
		case TEST_EOF_READ:
			testStatus.setName("EOF_READ");
			if (server) {
				CommunicationTester.readEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendEOF(c, testStatus);
			}
			break;
		case TEST_EOF_READ_BACK:
			testStatus.setName("EOF_READ_BACK");
			if (!server) {
				CommunicationTester.readEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendEOF(c, testStatus);
			}
			break;
		case TEST_EOF_READ_ARRAY:
			testStatus.setName("EOF_READ_ARRAY");
			if (server) {
				CommunicationTester.readArayEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendArayEOF(c, testStatus);
			}
			break;
		case TEST_EOF_READ_ARRAY_BACK:
			testStatus.setName("EOF_READ_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readArayEOF(is, os, testStatus);
			} else {
				CommunicationTester.sendArayEOF(c, testStatus);
			}
			break;
		case TEST_CONNECTION_INFO:
			testStatus.setName("TEST_CONNECTION_INFO");
			if (server) {
				CommunicationTester.serverRemoteDevice(c.conn, is, os, testStatus);
			} else {
				CommunicationTester.clientRemoteDevice(c.conn, is, os, testStatus);
			}
			break;
		case TEST_CLOSED_CONNECTION:
			testStatus.setName("CLOSED_CONNECTION");
			if (server) {
				CommunicationTester.readClosedConnection(c, testStatus);
			} else {
				CommunicationTester.sendClosedConnection(c, testStatus);
			}
			break;
		case TEST_CLOSED_CONNECTION_BACK:
			testStatus.setName("CLOSED_CONNECTION_BACK");
			if (!server) {
				CommunicationTester.readClosedConnection(c, testStatus);
			} else {
				CommunicationTester.sendClosedConnection(c, testStatus);
			}
			break;
		case TEST_BYTES_256:
			testStatus.setName("BYTES256");
			if (server) {
				CommunicationTester.readBytes256(is);
			} else {
				CommunicationTester.sendBytes256(os);
			}
			break;
		case TEST_BYTES_256_BACK:
			testStatus.setName("BYTES256_BACK");
			if (!server) {
				CommunicationTester.readBytes256(is);
			} else {
				CommunicationTester.sendBytes256(os);
			}
			break;
		case TEST_CAN_CLOSE_READ_ON_CLIENT:
			testStatus.setName("CAN_CLOSE_READ_ON_CLIENT");
			if (server) {
				CommunicationTester.sendByte4clientToClose(os, is, testStatus);
			} else {
				CommunicationTester.reciveByteAndCloseStream(false, c, testStatus);
			}
			break;
		case TEST_CAN_CLOSE_READ_ON_SERVER:
			testStatus.setName("CAN_CLOSE_READ_ON_SERVER");
			if (!server) {
				CommunicationTester.sendByte4clientToClose(os, is, testStatus);
			} else {
				CommunicationTester.reciveByteAndCloseStream(false, c, testStatus);
			}
			break;
		case TEST_CAN_CLOSE_READ_ARRAY_ON_CLIENT:
			testStatus.setName("CAN_CLOSE_READ_ARRAY_ON_CLIENT");
			if (server) {
				CommunicationTester.sendByte4clientToClose(os, is, testStatus);
			} else {
				CommunicationTester.reciveByteAndCloseStream(true, c, testStatus);
			}
			break;
		case TEST_CAN_CLOSE_READ_ARRAY_ON_SERVER:
			testStatus.setName("CAN_CLOSE_READ_ARRAY_ON_SERVER");
			if (!server) {
				CommunicationTester.sendByte4clientToClose(os, is, testStatus);
			} else {
				CommunicationTester.reciveByteAndCloseStream(true, c, testStatus);
			}
			break;
		case TEST_TWO_THREADS_SYNC_BYTES:
			testStatus.setName("TWO_THREADS_SYNC_BYTES");
			TwoThreadsPerConnection.start(c, 1, true);
			break;
		case TEST_TWO_THREADS_SYNC_ARRAYS:
			testStatus.setName("TWO_THREADS_SYNC_ARRAYS");
			TwoThreadsPerConnection.start(c, 64, true);
			break;
		case TEST_TWO_THREADS_BYTES:
			testStatus.setName("TWO_THREADS_BYTES");
			TwoThreadsPerConnection.start(c, 1, false);
			break;
		case TEST_TWO_THREADS_ARRAYS:
			testStatus.setName("TWO_THREADS_ARRAYS");
			TwoThreadsPerConnection.start(c, 64, false);
			break;
		case TEST_8K_PLUS_BYTE_ARRAY:
			testStatus.setName("8K_PLUS_BYTE_ARRAY");
			if (server) {
				CommunicationTester.readByteArayLarge(c, is, os, byteAray8KPlusSize);
			} else {
				CommunicationTester.sendByteArayLarge(c, is, os, byteAray8KPlusSize);
			}
			break;
		case TEST_8K_PLUS_BYTE_ARRAY_BACK:
			testStatus.setName("8K_PLUS_BYTE_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readByteArayLarge(c, is, os, byteAray8KPlusSize);
			} else {
				CommunicationTester.sendByteArayLarge(c, is, os, byteAray8KPlusSize);
			}
			break;
		case TEST_64K_PLUS_BYTE_ARRAY:
			testStatus.setName("64K_PLUS_BYTE_ARRAY");
			if (server) {
				CommunicationTester.readByteArayLarge(c, is, os, byteAray64KPlusSize);
			} else {
				CommunicationTester.sendByteArayLarge(c, is, os, byteAray64KPlusSize);
			}
			break;
		case TEST_64K_PLUS_BYTE_ARRAY_BACK:
			testStatus.setName("64K_PLUS_BYTE_ARRAY_BACK");
			if (!server) {
				CommunicationTester.readByteArayLarge(c, is, os, byteAray64KPlusSize);
			} else {
				CommunicationTester.sendByteArayLarge(c, is, os, byteAray64KPlusSize);
			}
			break;

		case TEST_128K_BYTE_ARRAY_X_10:
			testStatus.setName("128K_BYTE_ARRAY_X_10");
			for (int i = 0; i < 10; i++) {
				if (server) {
					CommunicationTester.readByteArayLarge(c, is, os, byteAray128KSize);
				} else {
					CommunicationTester.sendByteArayLarge(c, is, os, byteAray128KSize);
				}
			}
			break;
		case TEST_128K_BYTE_ARRAY_X_10_BACK:
			testStatus.setName("128K_BYTE_ARRAY_X_10_BACK");
			for (int i = 0; i < 10; i++) {
				if (!server) {
					CommunicationTester.readByteArayLarge(c, is, os, byteAray128KSize);
				} else {
					CommunicationTester.sendByteArayLarge(c, is, os, byteAray128KSize);
				}
			}
			break;

		// ---- TRAFFIC GENERATORS
		case TRAFFIC_GENERATOR_WRITE:
			testStatus.setName("RFgenW");
			if (server) {
				Config cfg = RfTrafficGenerator.getConfig(c, server, "RF write");
				if (cfg != null) {
					RfTrafficGenerator.trafficGeneratorStatusReadStart(c, testStatus);
					RfTrafficGenerator.trafficGeneratorWrite(c, cfg, testStatus);
				}
			} else {
				RfTrafficGenerator.trafficGeneratorClientInit(c);
				RfTrafficGenerator.trafficGeneratorRead(c, server, testStatus);
			}
			break;
		case TRAFFIC_GENERATOR_READ:
			testStatus.setName("RFgenR");
			if (server) {
				RfTrafficGenerator.trafficGeneratorRead(c, server, testStatus);
			} else {
				RfTrafficGenerator.trafficGeneratorClientInit(c);
				Config cfg = RfTrafficGenerator.getConfig(c, server, "RF write");
				if (cfg != null) {
					RfTrafficGenerator.trafficGeneratorStatusReadStart(c, testStatus);
					RfTrafficGenerator.trafficGeneratorWrite(c, cfg, testStatus);
				}
			}
			break;
		case TRAFFIC_GENERATOR_READ_WRITE:
			testStatus.setName("RFgenRW");
			if (!server) {
				RfTrafficGenerator.trafficGeneratorClientInit(c);
			}
			Config cfg = RfTrafficGenerator.getConfig(c, server, "RF write");
			if (cfg != null) {
				RfTrafficGenerator.trafficGeneratorReadStart(c, server, testStatus);
				RfTrafficGenerator.trafficGeneratorWrite(c, cfg, testStatus);
			}
			break;
		case TEST_SERVER_TERMINATE:
			return;
		default:
			Assert.fail("Invalid test#" + testType);
		}
	}
}
