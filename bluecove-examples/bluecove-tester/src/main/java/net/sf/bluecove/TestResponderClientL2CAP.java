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

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.microedition.io.Connection;

/**
 * @author vlads
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
