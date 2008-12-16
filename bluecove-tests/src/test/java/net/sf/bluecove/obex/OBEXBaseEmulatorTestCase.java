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

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import net.sf.bluecove.BaseEmulatorTestCase;
import net.sf.bluecove.TestCaseRunnable;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.obex.BlueCoveInternals;

/**
 * 
 */
public abstract class OBEXBaseEmulatorTestCase extends BaseEmulatorTestCase {

	protected static final int OBEX_HDR_USER = 0x30;

	protected static final String serverUUID = "11111111111111111111111111111123";

	protected static final byte[] simpleData = "Hello world!".getBytes();

	protected int serverRequestHandlerInvocations;

	protected HeaderSet serverHeaders;

	private volatile Connection serverAcceptedConnection;

	private final Object acceptedConnectionLock = new Object();

	private boolean ingoreServerErrors = false;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ingoreServerErrors = false;
		serverRequestHandlerInvocations = 0;
		serverHeaders = null;
		serverAcceptedConnection = null;
		BlueCoveInternals.readServerErrorCount();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		int errors = BlueCoveInternals.readServerErrorCount();
		if (errors != 0) {
			DebugLog.error("Test Server errors " + errors);
			if (!ingoreServerErrors) {
				throw new Error("Test Server errors " + errors);
			}
		}
	}

	protected abstract ServerRequestHandler createRequestHandler();

	@Override
	protected Runnable createTestServer() {
		return new TestCaseRunnable() {
			public void execute() throws Exception {
				SessionNotifier serverConnection = (SessionNotifier) Connector.open("btgoep://localhost:" + serverUUID
						+ ";name=ObexTest");
				synchronized (acceptedConnectionLock) {
					serverAcceptedConnection = serverConnection.acceptAndOpen(createRequestHandler());
				}
			}
		};
	}

	protected void ingoreServerErrors() {
		ingoreServerErrors = true;
		BlueCoveInternals.readServerErrorCount();
	}

	protected void assertServerErrors() {
		assertEquals("OBEX Server Errors", 0, BlueCoveInternals.readServerErrorCount());
	}

	protected Connection getServerAcceptedConnection() {
		synchronized (acceptedConnectionLock) {
			return serverAcceptedConnection;
		}
	}

	public static int longRequestPhasePackets() {
		return (BlueCoveInternals.isShortRequestPhase() ? 0 : 1);
	}

	protected byte[] makeTestData(int length) {
		byte data[] = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = (byte) (i & 0xFF);
		}
		return data;
	}
}
