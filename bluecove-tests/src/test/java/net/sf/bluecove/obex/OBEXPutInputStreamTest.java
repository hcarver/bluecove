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
import com.intel.bluetooth.obex.BlueCoveOBEX;

/**
 * see TCK InputStream2032 and the OBEXTCKAgentApp
 */
public class OBEXPutInputStreamTest extends OBEXBaseEmulatorTestCase {

	private byte[] serverReceiveData;

	private final byte[] serverReplyData = "Ask for data!".getBytes();;

	private final int serverResponseCode = ResponseCodes.OBEX_HTTP_OK;

	private IOException serverIOException;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverReceiveData = null;
		serverIOException = null;
	}

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onPut(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				DebugLog.debug("==TEST== serverRequestHandlerInvocations", serverRequestHandlerInvocations);
				serverHeaders = op.getReceivedHeaders();

				InputStream is = op.openInputStream();
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int data;
				while ((data = is.read()) != -1) {
					buf.write(data);
				}
				serverReceiveData = buf.toByteArray();

				OutputStream os = op.openOutputStream();
				os.write(serverReplyData);
				os.close();

				DebugLog.debug("==TEST== Server close Operation");
				op.close();
				DebugLog.debug("==TEST== Server returns " + BlueCoveOBEX.obexResponseCodes(serverResponseCode));
				return serverResponseCode;
			} catch (IOException e) {
				serverIOException = e;
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	public void testOutputStream() throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hs = clientSession.createHeaderSet();
		String name = "HelloOutputStream.txt";
		hs.setHeader(HeaderSet.NAME, name);

		// Create PUT Operation
		Operation putOp = clientSession.put(hs);

		// Send some text to server
		OutputStream os = putOp.openOutputStream();
		os.write(simpleData);
		os.close();

		InputStream is = putOp.openInputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int data;
		while ((data = is.read()) != -1) {
			buf.write(data);
		}
		byte serverRepliedData[] = buf.toByteArray();

		is.close();

		int responseCode = putOp.getResponseCode();
		DebugLog.debug0x("==TEST== Client ResponseCode " + BlueCoveOBEX.obexResponseCodes(responseCode) + " = ",
				responseCode);

		putOp.close();

		clientSession.disconnect(null);

		clientSession.close();

		if (serverIOException != null) {
			throw serverIOException;
		}
		assertEquals("invocations", 1, serverRequestHandlerInvocations);
		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("data", simpleData, serverReceiveData);
		assertEquals("data in responce", serverReplyData, serverRepliedData);

		assertEquals("ResponseCodes." + BlueCoveOBEX.obexResponseCodes(serverResponseCode), serverResponseCode,
				responseCode);
		assertServerErrors();
	}

}
