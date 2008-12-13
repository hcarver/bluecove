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

/**
 * 
 */
public class OBEXGetStandardTest extends OBEXBaseEmulatorTestCase {

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onGet(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				serverHeaders = op.getReceivedHeaders();
				String params = (String) serverHeaders.getHeader(OBEX_HDR_USER);
				if (params == null) {
					params = "";
				}

				HeaderSet hs = createHeaderSet();
				hs.setHeader(HeaderSet.LENGTH, new Long(simpleData.length));
				op.sendHeaders(hs);

				OutputStream os = op.openOutputStream();
				os.write(simpleData);
				if (params.contains("flush")) {
					os.flush();
				}
				DebugLog.debug("==TEST== Server close io");
				os.close();

				DebugLog.debug("==TEST== Server close Operation");
				op.close();
				DebugLog.debug("==TEST== Server returns OBEX_HTTP_OK");
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	private void runGETOperation(String testParams) throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hs = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hs.setHeader(HeaderSet.NAME, name);
		hs.setHeader(OBEX_HDR_USER, testParams);

		DebugLog.debug("==TEST== Client Create GET Operation");
		Operation getOp = clientSession.get(hs);

		DebugLog.debug("==TEST== Client getReceivedHeaders");
		HeaderSet headers = getOp.getReceivedHeaders();

		DebugLog.debug("==TEST== Client openInputStream");
		InputStream is = getOp.openInputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int data;
		while ((data = is.read()) != -1) {
			buf.write(data);
		}
		byte serverData[] = buf.toByteArray();

		DebugLog.debug("==TEST== Client close io");
		is.close();
		DebugLog.debug("==TEST== Client getResponseCode");
		int responseCode = getOp.getResponseCode();
		DebugLog.debug0x("==TEST== Client ResponseCode = ", responseCode);
		// assertEquals("ResponseCodes.OBEX_HTTP_OK", ResponseCodes.OBEX_HTTP_OK, responseCode);

		getOp.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("LENGTH", new Long(serverData.length), headers.getHeader(HeaderSet.LENGTH));
		assertEquals("data", simpleData, serverData);
		assertEquals("invocations", 1, serverRequestHandlerInvocations);
	}

	public void testGETOperation() throws IOException {
		runGETOperation("");
	}

	public void xtestGETOperationFlush() throws IOException {
		runGETOperation("flush");
	}
}
