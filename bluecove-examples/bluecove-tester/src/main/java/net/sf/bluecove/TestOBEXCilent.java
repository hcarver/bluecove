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

import java.io.IOException;
import java.io.OutputStream;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import net.sf.bluecove.util.BluetoothTypesInfo;

/**
 * 
 */
public class TestOBEXCilent implements Runnable {

	public static final boolean obexEnabled = true;

	public static void obexPut() {
		Thread thread = Configuration.cldcStub.createNamedThread(new TestOBEXCilent(0), "ObexClinet");
		thread.start();
	}

	private TestOBEXCilent(int type) {

	}

	public void run() {
		try {
			runObecPut();
		} catch (Throwable e) {
			Logger.error("obex", e);
		}
	}

	private void runObecPut() throws IOException {

		String serverURL;
		if (Configuration.testServerOBEX_TCP.booleanValue()) {
			serverURL = "tcpobex://127.1.1.1:650";
		} else {
			DiscoveryAgent discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
			Logger.debug("Find OBEX_OBJECT_PUSH  service");
			serverURL = discoveryAgent.selectService(TestResponderServerOBEX.OBEX_OBJECT_PUSH, Configuration
					.getRequiredSecurity(), false);
			if (serverURL == null) {
				Logger.debug("no OBEX service found");
				return;
			}
		}
		Logger.debug("connect " + serverURL);
		ClientSession clientSession = (ClientSession) Connector.open(serverURL);
		HeaderSet hsConnectReply = clientSession.connect(null);
		if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
			Logger.debug("Failed to connect");
			return;
		}

		HeaderSet hsOperation = clientSession.createHeaderSet();
		hsOperation.setHeader(HeaderSet.NAME, "Hello.txt");
		hsOperation.setHeader(HeaderSet.TYPE, "text");

		// Create PUT Operation
		Operation po = clientSession.put(hsOperation);

		// Send some text to server
		byte data[] = "Hello world!".getBytes("iso-8859-1");
		OutputStream os = po.openOutputStream();
		os.write(data);
		os.close();

		Logger.debug("put responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(po.getResponseCode()));

		po.close();

		HeaderSet hsd = clientSession.disconnect(null);

		Logger.debug("disconnect responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(hsd.getResponseCode()));

		clientSession.close();
	}

}
