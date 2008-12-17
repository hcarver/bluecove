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
package net.sf.bluecove.awt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;
import org.bluecove.tester.util.RuntimeDetect;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.OBEXTestAuthenticator;
import net.sf.bluecove.util.BluetoothTypesInfo;

public class ObexClientConnectionThread extends Thread {

	private Object threadLocalBluetoothStack;

	private String serverURL;

	private String name;

	private String text;

	boolean isPut;

	boolean isRunning = false;

	boolean timeouts;

	String status;

	private boolean stoped = false;

	private ClientSession clientSession;

	private static int count = 0;

	public ObexClientConnectionThread(String serverURL, String name, String text, boolean isPut) {
		this.serverURL = serverURL;
		this.name = name;
		this.text = text;
		this.isPut = isPut;
		threadLocalBluetoothStack = Configuration.threadLocalBluetoothStack;
		count++;
	}

	public void run() {
		final boolean isUserIdRequired = true;
		final boolean isFullAccess = true;

		isRunning = true;
		try {
			RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);

			status = "Connecting...";
			clientSession = (ClientSession) Connector.open(serverURL, Connector.READ_WRITE, timeouts);
			if (stoped) {
				return;
			}
			if (Configuration.authenticateOBEX.getValue() != 0) {
				clientSession.setAuthenticator(new OBEXTestAuthenticator("client" + count));
			}
			status = "Connected";
			HeaderSet hsConnect = clientSession.createHeaderSet();
			if (Configuration.authenticateOBEX.getValue() == 1) {
				hsConnect.createAuthenticationChallenge("OBEX-Con-Auth-Test", isUserIdRequired, isFullAccess);
			}
			HeaderSet hsConnectReply = clientSession.connect(hsConnect);
			Logger.debug("connect responseCode "
					+ BluetoothTypesInfo.toStringObexResponseCodes(hsConnectReply.getResponseCode()));

			HeaderSet hsOperation = clientSession.createHeaderSet();
			hsOperation.setHeader(HeaderSet.NAME, name);
			hsOperation.setHeader(HeaderSet.TYPE, "text");

			if (Configuration.authenticateOBEX.getValue() == 2) {
				hsOperation.createAuthenticationChallenge("OBEX-OP-Auth-Test", isUserIdRequired, isFullAccess);
			}
			if (stoped) {
				return;
			}
			if (isPut) {
				byte data[] = text.getBytes("iso-8859-1");
				hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length));
				status = "Putting";
				Operation po = clientSession.put(hsOperation);

				OutputStream os = po.openOutputStream();
				os.write(data);
				os.close();

				Logger.debug("put responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(po.getResponseCode()));

				HeaderSet receivedHeaders = po.getReceivedHeaders();
				String description = (String) receivedHeaders.getHeader(HeaderSet.DESCRIPTION);
				if (description != null) {
					Logger.debug("Description " + description);
				}

				po.close();
			} else {
				status = "Getting";
				Operation po = clientSession.get(hsOperation);

				InputStream is = po.openInputStream();
				StringBuffer buf = new StringBuffer();
				while (!stoped) {
					int i = is.read();
					if (i == -1) {
						break;
					}
					buf.append((char) i);
				}
				if (buf.length() > 0) {
					Logger.debug("got:" + buf);
				}
				is.close();

				Logger.debug("get responseCode " + BluetoothTypesInfo.toStringObexResponseCodes(po.getResponseCode()));

				HeaderSet receivedHeaders = po.getReceivedHeaders();
				String description = (String) receivedHeaders.getHeader(HeaderSet.DESCRIPTION);
				if (description != null) {
					Logger.debug("Description " + description);
				}

				po.close();
			}

			HeaderSet hsd = clientSession.disconnect(null);
			Logger.debug("disconnect responseCode "
					+ BluetoothTypesInfo.toStringObexResponseCodes(hsd.getResponseCode()));

			status = "Finished";

		} catch (IOException e) {
			status = "Communication error " + e.toString();
			Logger.error("Communication error", e);
		} catch (Throwable e) {
			status = "Error " + e.toString();
			Logger.error("Error", e);
		} finally {
			isRunning = false;
			IOUtils.closeQuietly(clientSession);
			clientSession = null;
			if (stoped) {
				status = "Terminated";
			}
		}
	}

	public void shutdown() {
		stoped = true;
		if (clientSession != null) {
			IOUtils.closeQuietly(clientSession);
			clientSession = null;
		}
	}
}
