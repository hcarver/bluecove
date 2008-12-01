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
package net.sf.bluecove;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;

import junit.framework.Assert;

/**
 * 
 */
public class TestResponderClientRFCOMM extends TestResponderClientConnection {

	private ConnectionHolderStream c;

	static void connectAndTest(TestResponderClient client, String serverURL) {
		client.connectAndTest(serverURL, "", Configuration.TEST_CASE_FIRST, Configuration.TEST_CASE_LAST,
				new TestResponderClientRFCOMM());
	}

	public String protocolID() {
		return "RF";
	}

	public ConnectionHolder connected(Connection conn) throws IOException {
		c = new ConnectionHolderStream((StreamConnection) conn);
		c.os = c.conn.openOutputStream();

		return c;
	}

	public void executeTest(int testType, TestStatus testStatus) throws IOException {
		c.os.write(Consts.SEND_TEST_START);
		c.os.write(testType);
		c.os.flush();

		c.is = c.conn.openInputStream();
		c.active();

		CommunicationTester.runTest(testType, false, c, testStatus);
		c.active();

	}

	public void replySuccess(String logPrefix, int testType, TestStatus testStatus) throws IOException {
		c.os.flush();
		Logger.debug(logPrefix + "read server status");
		int ok = c.is.read();
		if (ok != Consts.SEND_TEST_REPLY_OK_MESSAGE) {
			Assert.assertEquals("Server reply OK", Consts.SEND_TEST_REPLY_OK, ok);
		}
		int conformTestType = c.is.read();
		Assert.assertEquals("Test reply conform#", testType, conformTestType);
		if (ok == Consts.SEND_TEST_REPLY_OK_MESSAGE) {
			DataInputStream dis = new DataInputStream(c.is);
			String message = dis.readUTF();
			if (message == null) {
				Assert.fail("Server message expected");
			}
			Logger.info(logPrefix + "Server message\n[" + message + "]");
		}
	}

	public void sendStopServerCmd(String serverURL) {
		// StreamConnection conn = null;
		// InputStream is = null;
		// OutputStream os = null;
		// try {
		// Logger.debug("Send stopServer command");
		// conn = (StreamConnection) Connector.open(serverURL);
		// os = conn.openOutputStream();
		//
		// os.write(Consts.TEST_SERVER_TERMINATE);
		// os.flush();
		// try {
		// Thread.sleep(1000);
		// } catch (Exception e) {
		// }
		// } catch (Throwable e) {
		// Logger.error("stopServer error", e);
		// }
		// IOUtils.closeQuietly(os);
		// IOUtils.closeQuietly(is);
		// IOUtils.closeQuietly(conn);
	}
}
