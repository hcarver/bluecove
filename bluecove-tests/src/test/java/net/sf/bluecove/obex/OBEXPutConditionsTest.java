/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.obex.BlueCoveInternals;
import com.intel.bluetooth.obex.BlueCoveOBEX;

/**
 * 
 */
public class OBEXPutConditionsTest extends OBEXBaseEmulatorTestCase {

	private int serverDataLength;

	private byte[] serverData;

	private static long LENGTH_NO_DATA = 0xffffffffl;

	private volatile int serverResponseCode = ResponseCodes.OBEX_HTTP_OK;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverDataLength = -1;
		serverData = null;
		serverResponseCode = 0;
	}

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onPut(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				DebugLog.debug("==TEST== serverRequestHandlerInvocations", serverRequestHandlerInvocations);
				if (serverRequestHandlerInvocations > 1) {
					return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
				}
				serverHeaders = op.getReceivedHeaders();
				Long dataLength = (Long) serverHeaders.getHeader(HeaderSet.LENGTH);
				if (dataLength == null) {
					serverResponseCode = ResponseCodes.OBEX_HTTP_LENGTH_REQUIRED;
					return serverResponseCode;
				}
				long length = dataLength.longValue();
				int len = (int) length;
				if (length != LENGTH_NO_DATA) {
					InputStream is = op.openInputStream();
					serverData = new byte[len];
					int got = 0;
					// read fully
					while (got < len) {
						int rc = is.read(serverData, got, len - got);
						if (rc < 0) {
							break;
						}
						got += rc;
					}
					is.close();
					serverDataLength = got;
				}
				op.close();
				serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
				return serverResponseCode;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	private void runPUTOperation(boolean flush, long length, byte[] data1, byte[] data2, int expectedPackets)
			throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());
		int writePacketsConnect = BlueCoveInternals.getPacketsCountWrite(clientSession);

		HeaderSet hsOperation = clientSession.createHeaderSet();

		hsOperation.setHeader(HeaderSet.LENGTH, new Long(length));

		DebugLog.debug("==TEST== client start put");
		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		DebugLog.debug("==TEST== client openOutputStream");
		OutputStream os = putOperation.openOutputStream();
		os.write(data1);
		if (flush) {
			DebugLog.debug("==TEST== client flush 1");
			os.flush();
		}
		if (data2 != null) {
			os.write(data2);
			if (flush) {
				DebugLog.debug("test client flush 2");
				os.flush();
			}
		}
		DebugLog.debug("==TEST== client OutputStream close");
		os.close();

		int responseCode = putOperation.getResponseCode();
		DebugLog.debug0x("==TEST== Client ResponseCode " + BlueCoveOBEX.obexResponseCodes(responseCode) + " = ",
				responseCode);

		DebugLog.debug("==TEST== client Operation close");
		putOperation.close();

		DebugLog.debug("==TEST== client PUT packets", BlueCoveInternals.getPacketsCountWrite(clientSession)
				- writePacketsConnect);

		DebugLog.debug("==TEST== client Session disconnect");
		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("invocations", 1, serverRequestHandlerInvocations);
		assertEquals("LENGTH", new Long(length), serverHeaders.getHeader(HeaderSet.LENGTH));
		assertEquals("data.length", data1.length, serverDataLength);

		assertEquals("ResponseCodes." + BlueCoveOBEX.obexResponseCodes(serverResponseCode), serverResponseCode,
				responseCode);

		assertEquals("c.writePackets", expectedPackets, BlueCoveInternals.getPacketsCountWrite(clientSession));
		assertEquals("c.readPackets", expectedPackets, BlueCoveInternals.getPacketsCountRead(clientSession));
		assertEquals("s.writePackets", expectedPackets, BlueCoveInternals
				.getPacketsCountWrite(getServerAcceptedConnection()));
		assertEquals("s.readPackets", expectedPackets, BlueCoveInternals
				.getPacketsCountRead(getServerAcceptedConnection()));
		assertServerErrors();
	}

	/**
	 * Verify that server can read data without getting to the exact end of file in InputStream
	 */
	public void testPUTOperationComplete() throws IOException {
		byte data[] = simpleData;
		int expectedPackets = 1 + 2 + 1;
		runPUTOperation(false, data.length, data, null, expectedPackets);

		assertEquals("data", data, serverData);
	}

	public void testPUTOperationCompleteBigData() throws IOException {
		int mtu = 0x400; // OBEX_DEFAULT_MTU
		// Send big Data to server
		int length = 0x4001;
		byte data[] = makeTestData(length);

		int dataNeedPackets = length / mtu;
		if ((length % mtu) > 0) {
			dataNeedPackets++;
		}
		int expectedPackets = 1 + 1 + dataNeedPackets + 1;

		runPUTOperation(false, data.length, data, null, expectedPackets);

		assertEquals("data", data, serverData);
	}

	/**
	 * Verify that call to flush do not cause double invocation for onPut
	 */
	public void testPUTOperationCompleteFlush() throws IOException {
		byte data[] = simpleData;

		int expectedPackets = 1 + 2 + 1 + 1;
		runPUTOperation(true, data.length, data, null, expectedPackets);

		assertEquals("data", data, serverData);
	}

	public void testPUTOperationCompleteFlushBigData() throws IOException {
		int mtu = 0x400; // OBEX_DEFAULT_MTU
		// Send big Data to server
		int length = 0x4001;
		byte data[] = makeTestData(length);

		int dataNeedPackets = length / mtu;
		if ((length % mtu) > 0) {
			dataNeedPackets++;
		}
		int expectedPackets = 1 + 1 + dataNeedPackets + 1 + 1;

		runPUTOperation(true, data.length, data, null, expectedPackets);

		assertEquals("data", data, serverData);
	}

	public void testPUTOperationSendMore() throws IOException {
		byte data[] = simpleData;

		int expectedPackets = 1 + 2 + 1;
		runPUTOperation(false, data.length, data, "More".getBytes("iso-8859-1"), expectedPackets);

		assertEquals("data", data, serverData);
	}

	public void testPUTOperationSendMoreBigData() throws IOException {
		int mtu = 0x400; // OBEX_DEFAULT_MTU
		// Send big Data to server
		int length = 0x4001;
		byte data[] = makeTestData(length);

		byte data2[] = makeTestData(mtu * 3 + 10);
		length += data2.length;

		int dataNeedPackets = length / mtu;
		if ((length % mtu) > 0) {
			dataNeedPackets++;
		}

		int expectedPackets = 1 + 1 + dataNeedPackets + 1;

		runPUTOperation(false, data.length, data, data2, expectedPackets);

		assertEquals("data", data, serverData);
	}

	public void testPUTOperationSendLess() throws IOException {
		byte data[] = simpleData;
		int less = 4;

		int expectedPackets = 1 + 2 + 1;
		runPUTOperation(false, data.length + less, data, null, expectedPackets);

		assertEquals("data", data.length, data, serverData);
	}

	public void testPUTOperationSendLessBigData() throws IOException {
		int mtu = 0x400; // OBEX_DEFAULT_MTU
		// Send big Data to server
		byte data[] = makeTestData(0x4001);

		int less = mtu * 3 + 11;

		int length = data.length;

		int dataNeedPackets = length / mtu;
		if ((length % mtu) > 0) {
			dataNeedPackets++;
		}
		int expectedPackets = 1 + 1 + dataNeedPackets + 1;

		runPUTOperation(false, length + less, data, null, expectedPackets);

		assertEquals("data", data.length, data, serverData);
	}

	/**
	 * No data in Operation OutputStream
	 */
	public void testPUTOperationNoData() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());
		int writePacketsConnect = BlueCoveInternals.getPacketsCountWrite(clientSession);

		HeaderSet hs = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hs.setHeader(HeaderSet.NAME, name);
		hs.setHeader(HeaderSet.LENGTH, new Long(LENGTH_NO_DATA));

		// Create PUT Operation
		Operation putOperation = clientSession.put(hs);

		OutputStream os = putOperation.openOutputStream();
		os.close();

		int responseCode = putOperation.getResponseCode();
		DebugLog.debug0x("==TEST== Client ResponseCode " + BlueCoveOBEX.obexResponseCodes(responseCode) + " = ",
				responseCode);

		putOperation.close();

		DebugLog.debug("PUT packets", BlueCoveInternals.getPacketsCountWrite(clientSession) - writePacketsConnect);

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertNull("data", serverData);
		assertEquals("invocations", 1, serverRequestHandlerInvocations);

		assertEquals("ResponseCodes." + BlueCoveOBEX.obexResponseCodes(serverResponseCode), serverResponseCode,
				responseCode);

		int expectedPackets = 1 + 2 + 1;

		assertEquals("c.writePackets", expectedPackets, BlueCoveInternals.getPacketsCountWrite(clientSession));
		assertEquals("c.readPackets", expectedPackets, BlueCoveInternals.getPacketsCountRead(clientSession));
		assertEquals("s.writePackets", expectedPackets, BlueCoveInternals
				.getPacketsCountWrite(getServerAcceptedConnection()));
		assertEquals("s.readPackets", expectedPackets, BlueCoveInternals
				.getPacketsCountRead(getServerAcceptedConnection()));
		assertServerErrors();
	}
}
