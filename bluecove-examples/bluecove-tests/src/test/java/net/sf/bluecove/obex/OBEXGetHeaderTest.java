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

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

/**
 * @author vlads
 * 
 */
public class OBEXGetHeaderTest extends OBEXBaseEmulatorTestCase {

	private class RequestHandler extends ServerRequestHandler {

		@Override
		public int onGet(Operation op) {
			try {
				serverRequestHandlerInvocations++;
				serverHeaders = op.getReceivedHeaders();
				String params = (String) serverHeaders.getHeader(OBEX_HDR_USER);
				if (params == null) {
					params = "";
				}
				int responsecode = ResponseCodes.OBEX_HTTP_OK;

				if (params.equals("OBEX_HTTP_NOT_MODIFIED")) {
					responsecode = ResponseCodes.OBEX_HTTP_NOT_MODIFIED;
				}

				op.close();
				return responsecode;
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.OBEXBaseEmulatorTestCase#createRequestHandler()
	 */
	@Override
	protected ServerRequestHandler createRequestHandler() {
		return new RequestHandler();
	}

	private void runGETHeader(String testParams, int expectServerDataResponseCode) throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hs = clientSession.createHeaderSet();
		hs.setHeader(OBEX_HDR_USER, testParams);

		// Create GET Operation
		Operation get = clientSession.get(hs);

		assertEquals("ResponseCode", expectServerDataResponseCode, get.getResponseCode());

		get.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("invocations", 1, serverRequestHandlerInvocations);
	}

	/**
	 * duplicate for TCK test
	 * com.motorola.tck.tests.api.javax.obex.ClientSession.getTests.ClientSession9003
	 * 
	 * Tests that server waits for FINAL packet in request before sending reply.
	 * e.g waits for request to end.
	 */

	public void testGETNoStreamJustHeader() throws IOException {
		runGETHeader("OBEX_HTTP_NOT_MODIFIED", ResponseCodes.OBEX_HTTP_NOT_MODIFIED);
	}
}
