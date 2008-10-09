/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;

import junit.framework.Assert;

/**
 * @author vlads
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
