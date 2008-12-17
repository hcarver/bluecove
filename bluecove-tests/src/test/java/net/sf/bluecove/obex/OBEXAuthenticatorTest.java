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
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.PasswordAuthentication;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.obex.BlueCoveInternals;
import com.intel.bluetooth.obex.BlueCoveOBEX;

public class OBEXAuthenticatorTest extends OBEXBaseEmulatorTestCase {

	private String userName = "bob";

	private String userPasswordSufix = "_Pwd";

	private int serverResponseCode;

	private final int testHeaderID = 0xF0;

	private enum When {
		Never, onConnect, onPut
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
	}

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onConnect(HeaderSet request, HeaderSet reply) {
			try {
				Long when = (Long) request.getHeader(testHeaderID);
				if (When.onConnect.ordinal() == when) {
					reply.createAuthenticationChallenge("SrvAuth", true, true);
					return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
				}
			} catch (IOException e) {
				DebugLog.error("==TEST== Server", e);
				return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}
			return ResponseCodes.OBEX_HTTP_OK;
		}

		@Override
		public int onPut(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				DebugLog.debug("==TEST== serverRequestHandlerInvocations", serverRequestHandlerInvocations);
				serverHeaders = op.getReceivedHeaders();

				Long when = (Long) serverHeaders.getHeader(testHeaderID);
				if (When.onPut.ordinal() == when) {
					HeaderSet reply = createHeaderSet();
					reply.createAuthenticationChallenge("SrvAuth", true, true);
					op.sendHeaders(reply);
					return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
				}

				InputStream is = op.openInputStream();
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int data;
				while ((data = is.read()) != -1) {
					buf.write(data);
				}
				DebugLog.debug("==TEST== Server close Operation");
				op.close();
				serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
				DebugLog.debug("==TEST== Server returns " + BlueCoveOBEX.obexResponseCodes(serverResponseCode));
				return serverResponseCode;
			} catch (IOException e) {
				DebugLog.error("==TEST== Server", e);
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	private class RequestAuthenticator implements Authenticator {

		public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired,
				boolean isFullAccess) {
			DebugLog.debug("==TEST== Server challenge " + (isUserIdRequired ? "U" : "") + (isFullAccess ? "A" : "")
					+ " " + description);
			return new PasswordAuthentication(userName.getBytes(), ((new String("userName") + userPasswordSufix))
					.getBytes());
		}

		public byte[] onAuthenticationResponse(byte[] userName) {
			DebugLog.debug("==TEST== Server authenticate " + new String(userName));
			return (new String(userName) + userPasswordSufix).getBytes();
		}

	}

	@Override
	protected Authenticator getServerAuthenticator() {
		return new RequestAuthenticator();
	}

	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	private When now = When.Never;

	private When authenticatorCalled = When.Never;

	private void runPUTOperation(final When whenChallenge) throws IOException {

		// TODO
		ingoreServerErrors();

		now = When.Never;

		authenticatorCalled = When.Never;

		Authenticator auth = new Authenticator() {

			public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired,
					boolean isFullAccess) {
				authenticatorCalled = now;
				DebugLog.debug("==TEST== Client challenge " + (isUserIdRequired ? "U" : "") + (isFullAccess ? "A" : "")
						+ " " + description);
				return new PasswordAuthentication(userName.getBytes(), ((new String("userName") + userPasswordSufix))
						.getBytes());
			}

			public byte[] onAuthenticationResponse(byte[] userName) {
				return null;
			}
		};

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));

		clientSession.setAuthenticator(auth);
		HeaderSet hsConnect = clientSession.createHeaderSet();
		hsConnect.setHeader(testHeaderID, new Long(whenChallenge.ordinal()));
		now = When.onConnect;
		HeaderSet hsConnectReply = clientSession.connect(hsConnect);
		int connectCode = hsConnectReply.getResponseCode();
		DebugLog.debug0x("==TEST== Client connect ResponseCode " + BlueCoveOBEX.obexResponseCodes(connectCode) + " = ",
				connectCode);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, connectCode);
		int writePacketsConnect = BlueCoveInternals.getPacketsCountWrite(clientSession);

		HeaderSet hsOperation = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hsOperation.setHeader(HeaderSet.NAME, name);
		hsOperation.setHeader(testHeaderID, new Long(whenChallenge.ordinal()));

		now = When.onPut;
		// Create PUT Operation
		Operation putOperation = clientSession.put(hsOperation);

		// Send some text to server
		byte data[] = simpleData;
		OutputStream os = putOperation.openOutputStream();
		os.write(data);
		os.close();
		DebugLog.debug("==TEST== Client getResponseCode");
		int responseCode = putOperation.getResponseCode();
		DebugLog.debug0x("==TEST== Client ResponseCode " + BlueCoveOBEX.obexResponseCodes(responseCode) + " = ",
				responseCode);

		putOperation.close();

		now = When.Never;

		DebugLog.debug("==TEST==  PUT packets", BlueCoveInternals.getPacketsCountWrite(clientSession)
				- writePacketsConnect);

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("invocations", 1, serverRequestHandlerInvocations);

		assertEquals("onAuthenticationChallenge", whenChallenge, authenticatorCalled);

		assertEquals("ResponseCodes." + BlueCoveOBEX.obexResponseCodes(serverResponseCode), serverResponseCode,
				responseCode);
		assertServerErrors();

	}

	public void XtestNoChallenge() throws IOException {
		runPUTOperation(When.Never);
	}

	public void testChallengeOnConnect() throws IOException {
		runPUTOperation(When.onConnect);
	}
}
