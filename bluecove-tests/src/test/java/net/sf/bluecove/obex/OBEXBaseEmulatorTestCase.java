/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import net.sf.bluecove.BaseEmulatorTestCase;
import net.sf.bluecove.TestCaseRunnable;

import com.intel.bluetooth.obex.BlueCoveInternals;

/**
 * @author vlads
 * 
 */
public abstract class OBEXBaseEmulatorTestCase extends BaseEmulatorTestCase {

	protected static final int OBEX_HDR_USER = 0x30;

	protected static final String serverUUID = "11111111111111111111111111111123";

	protected static final byte[] simpleData = "Hello world!".getBytes();

	protected int serverRequestHandlerInvocations;

	protected HeaderSet serverHeaders;

	protected Connection serverAcceptedConnection;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverRequestHandlerInvocations = 0;
		serverHeaders = null;
		serverAcceptedConnection = null;
	}

	protected abstract ServerRequestHandler createRequestHandler();

	@Override
	protected Runnable createTestServer() {
		return new TestCaseRunnable() {
			public void execute() throws Exception {
				SessionNotifier serverConnection = (SessionNotifier) Connector.open("btgoep://localhost:" + serverUUID
						+ ";name=ObexTest");
				serverAcceptedConnection = serverConnection.acceptAndOpen(createRequestHandler());
			}
		};
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
