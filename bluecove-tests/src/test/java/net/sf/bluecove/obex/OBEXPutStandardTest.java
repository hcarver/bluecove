/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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

import java.io.ByteArrayOutputStream;
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

/**
 * 
 */
public class OBEXPutStandardTest extends OBEXBaseEmulatorTestCase {

	private byte[] serverData;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverData = null;
	}

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onPut(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				DebugLog.debug("serverRequestHandlerInvocations", serverRequestHandlerInvocations);
				serverHeaders = op.getReceivedHeaders();
				InputStream is = op.openInputStream();
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int data;
				while ((data = is.read()) != -1) {
					buf.write(data);
				}
				serverData = buf.toByteArray();
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	/*
	 * Used for profiling
	 */
	public static void main(String args[]) throws Exception {
		long start = System.currentTimeMillis();
		OBEXPutStandardTest t = new OBEXPutStandardTest();
		t.setUp();
		long setUp = System.currentTimeMillis();
		System.out.println("setUp   : " + (setUp - start));

		t.testPUTOperation();
		// t.testPUTOperationBigData();

		long exec = System.currentTimeMillis();
		System.out.println("exec    : " + (exec - setUp));
		t.tearDown();
		long tearDown = System.currentTimeMillis();
		System.out.println("tearDown: " + (tearDown - exec));
		System.out.println("total   : " + (System.currentTimeMillis() - start));
	}

	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	private void runPUTOperation(boolean flush, int expectedPackets) throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());
		int writePacketsConnect = BlueCoveInternals.getPacketsCountWrite(clientSession);

		HeaderSet hsOperation = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hsOperation.setHeader(HeaderSet.NAME, name);

		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		// Send some text to server
		byte data[] = simpleData;
		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		if (flush) {
			os.flush();
		}
		os.close();

		putOperation.close();

		DebugLog.debug("PUT packets", BlueCoveInternals.getPacketsCountWrite(clientSession) - writePacketsConnect);

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("data", data, serverData);
		assertEquals("invocations", 1, serverRequestHandlerInvocations);

		assertEquals("c.writePackets", expectedPackets, BlueCoveInternals.getPacketsCountWrite(clientSession));
		assertEquals("c.readPackets", expectedPackets, BlueCoveInternals.getPacketsCountRead(clientSession));
		int serverSentPackets = BlueCoveInternals.getPacketsCountWrite(serverAcceptedConnection);
		assertEquals("s.writePackets (" + serverSentPackets + ")", expectedPackets, serverSentPackets);
		int serverReadPackets = BlueCoveInternals.getPacketsCountRead(serverAcceptedConnection);
		assertEquals("s.readPackets (" + serverReadPackets + ")", expectedPackets, serverReadPackets);
	}

	public void testPUTOperation() throws IOException {
		runPUTOperation(false, 1 + 2 + 1);
	}

	public void testPUTOperationFlush() throws IOException {
		runPUTOperation(true, 1 + 2 + 1 + 1);
	}

	public void testPUTOperationNoData() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());
		int writePacketsConnect = BlueCoveInternals.getPacketsCountWrite(clientSession);

		HeaderSet hs = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hs.setHeader(HeaderSet.NAME, name);

		// Create PUT Operation
		Operation putOperation = clientSession.put(hs);

		OutputStream os = putOperation.openOutputStream();
		os.close();

		putOperation.close();

		DebugLog.debug("PUT packets", BlueCoveInternals.getPacketsCountWrite(clientSession) - writePacketsConnect);

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("data", new byte[0], serverData);
		assertEquals("invocations", 1, serverRequestHandlerInvocations);

		int expectedPackets = 1 + 2 + 1;

		assertEquals("c.writePackets", expectedPackets, BlueCoveInternals.getPacketsCountWrite(clientSession));
		assertEquals("c.readPackets", expectedPackets, BlueCoveInternals.getPacketsCountRead(clientSession));
		assertEquals("s.writePackets", expectedPackets, BlueCoveInternals
				.getPacketsCountWrite(serverAcceptedConnection));
		assertEquals("s.readPackets", expectedPackets, BlueCoveInternals.getPacketsCountRead(serverAcceptedConnection));
	}

	public void testPUTOperationBigData() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());
		int writePacketsConnect = BlueCoveInternals.getPacketsCountWrite(clientSession);

		// Create PUT Operation
		Operation putOperation = clientSession.put(null);
		DebugLog.debug("Client PUT Operation started");

		// Send big Data to server
		int length = 0x4001;
		byte data[] = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = (byte) (i & 0xFF);
		}
		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.close();

		putOperation.close();

		DebugLog.debug("PUT packets", BlueCoveInternals.getPacketsCountWrite(clientSession) - writePacketsConnect);

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("data", data, serverData);
		assertEquals("invocations", 1, serverRequestHandlerInvocations);

		int mtu = BlueCoveInternals.getPacketSize(clientSession);
		int dataNeedPackets = length / mtu;
		if ((length % mtu) > 0) {
			dataNeedPackets++;
		}

		assertEquals("writePackets", 1 + longRequestPhasePackets() + dataNeedPackets + 1, BlueCoveInternals
				.getPacketsCountWrite(clientSession));
		assertEquals("readPackets", 1 + longRequestPhasePackets() + dataNeedPackets + 1, BlueCoveInternals
				.getPacketsCountRead(clientSession));
	}
}
