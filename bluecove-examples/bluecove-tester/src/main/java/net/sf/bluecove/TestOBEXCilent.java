/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
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
 * @author vlads
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
