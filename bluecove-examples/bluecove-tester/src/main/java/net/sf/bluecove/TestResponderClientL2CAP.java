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

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.microedition.io.Connection;

/**
 * 
 */
public class TestResponderClientL2CAP extends TestResponderClientConnection {

	ConnectionHolderL2CAP c;

	static void connectAndTest(TestResponderClient client, String serverURL) {
		client.connectAndTest(serverURL, ";TransmitMTU=" + TestResponderCommon.receiveMTU_max + ";ReceiveMTU="
				+ TestResponderCommon.receiveMTU_max, Configuration.TEST_CASE_L2CAP_FIRST,
				Configuration.TEST_CASE_L2CAP_LAST, new TestResponderClientL2CAP());
	}

	public String protocolID() {
		return "L2";
	}

	public ConnectionHolder connected(Connection conn) throws IOException {
		c = new ConnectionHolderL2CAP((L2CAPConnection) conn);
		return c;
	}

	public void executeTest(int testType, TestStatus testStatus) throws IOException {
		CommunicationTesterL2CAP.runTest(testType, false, c, null, testStatus);
	}

	public void replySuccess(String logPrefix, int testType, TestStatus testStatus) throws IOException {
	}

	public void sendStopServerCmd(String serverURL) {

	}
}
