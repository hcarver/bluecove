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

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

/**
 * 
 */
public class OBEXGetHeaderTest extends OBEXBaseEmulatorTestCase {

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
				int responsecode = ResponseCodes.OBEX_HTTP_OK;

				if (params.equals("OBEX_HTTP_NOT_MODIFIED")) {
					responsecode = ResponseCodes.OBEX_HTTP_NOT_MODIFIED;
				}

				op.close();
				return responsecode;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.OBEXBaseEmulatorTestCase#createRequestHandler()
	 */
	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	private void runGETHeader(String testParams, int expectServerDataResponseCode) throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hs = clientSession.createHeaderSet();
		hs.setHeader(OBEX_HDR_USER, testParams);

		// Create GET Operation
		Operation get = clientSession.get(hs);

		assertEquals("ResponseCode", expectServerDataResponseCode, get.getResponseCode());

		get.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("invocations", 1, serverRequestHandlerInvocations);
	}

	/**
	 * duplicate for TCK test
	 * com.motorola.tck.tests.api.javax.obex.ClientSession.getTests.ClientSession9003
	 * 
	 * Tests that server waits for FINAL packet in request before sending reply.
	 * e.g waits for request to end.
	 */

	public void testGETNoStreamJustHeader() throws IOException {
		runGETHeader("OBEX_HTTP_NOT_MODIFIED", ResponseCodes.OBEX_HTTP_NOT_MODIFIED);
	}
}
