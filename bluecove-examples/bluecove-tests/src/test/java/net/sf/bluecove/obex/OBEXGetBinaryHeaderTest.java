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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

/**
 * @author vlads
 * 
 * This tests OBEX Operation get and OutputStream.
 * 
 * Some talk on the subject here:
 * https://opensource.motorola.com/sf/discussion/do/listPosts/projects.jsr82/discussion.google_jsr_82_support.topc1544
 * 
 */
public class OBEXGetBinaryHeaderTest extends OBEXBaseEmulatorTestCase {

	protected static final byte[] simpleHeaderData = "Ask for data!".getBytes();

	private byte[] serverHeaderData;

	private byte[] serverReplyBigData;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		serverHeaderData = null;
	}

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

				InputStream isHeader = op.openInputStream();
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				int data;
				while ((data = isHeader.read()) != -1) {
					buf.write(data);
				}
				serverHeaderData = buf.toByteArray();

				byte[] replyData = simpleData;
				if (params.contains("bigData")) {
					replyData = serverReplyBigData;
				}
				HeaderSet hs = createHeaderSet();
				hs.setHeader(HeaderSet.LENGTH, new Long(replyData.length));
				op.sendHeaders(hs);

				OutputStream os = op.openOutputStream();
				os.write(replyData);
				os.close();

				op.close();
				return ResponseCodes.OBEX_HTTP_ACCEPTED;
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

	private void runGETBinaryHeader(byte[] sendHeaderData, byte[] expectServerData, String testParams)
			throws IOException {

		ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
		HeaderSet hsConnectReply = clientSession.connect(null);
		assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

		HeaderSet hs = clientSession.createHeaderSet();
		String name = "Hello.txt";
		hs.setHeader(HeaderSet.NAME, name);
		hs.setHeader(OBEX_HDR_USER, testParams);

		// Create GET Operation
		Operation get = clientSession.get(hs);

		OutputStream osHeader = get.openOutputStream();
		osHeader.write(sendHeaderData);
		osHeader.close();

		// request portion is done
		HeaderSet headers = get.getReceivedHeaders();

		InputStream is = get.openInputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int data;
		while ((data = is.read()) != -1) {
			buf.write(data);
		}
		byte serverData[] = buf.toByteArray();

		is.close();

		get.close();

		clientSession.disconnect(null);

		clientSession.close();

		assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
		assertEquals("data in header", sendHeaderData, serverHeaderData);
		assertEquals("data in responce", expectServerData, serverData);
		assertEquals("LENGTH", new Long(serverData.length), headers.getHeader(HeaderSet.LENGTH));
		assertEquals("invocations", 1, serverRequestHandlerInvocations);
	}

	public void testGETBinaryHeader() throws IOException {
		runGETBinaryHeader(simpleHeaderData, simpleData, null);
	}

	public void testGETBinaryHeaderBigData() throws IOException {
		// Send big Data to server
		int length = 0x4001;
		byte sendHeaderData[] = new byte[length];
		for (int i = 0; i < length; i++) {
			sendHeaderData[i] = (byte) (i & 0xFF);
		}
		int lengthReply = 0x4001;
		serverReplyBigData = new byte[lengthReply];
		for (int i = 0; i < lengthReply; i++) {
			serverReplyBigData[i] = (byte) (i & 0xFF);
		}
		runGETBinaryHeader(sendHeaderData, serverReplyBigData, "bigData");
	}
}
