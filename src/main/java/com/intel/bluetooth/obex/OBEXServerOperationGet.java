/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.obex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.obex.HeaderSet;
import javax.obex.ResponseCodes;

/**
 * @author vlads
 * 
 */
class OBEXServerOperationGet extends OBEXServerOperation implements OBEXOperationDelivery {

	protected OBEXOperationOutputStream os;

	protected OBEXServerOperationGet(OBEXServerSessionImpl session, HeaderSet receivedHeaders) {
		super(session, receivedHeaders);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.InputConnection#openInputStream()
	 */
	public InputStream openInputStream() throws IOException {
		if (isClosed) {
			throw new IOException("operation closed");
		}
		return new UnsupportedInputStream();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.OutputConnection#openOutputStream()
	 */
	public OutputStream openOutputStream() throws IOException {
		if (isClosed) {
			throw new IOException("operation closed");
		}
		if (os != null) {
			throw new IOException("output still open");
		}
		os = new OBEXOperationOutputStream(session.mtu, this);
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		return os;
	}

	public void close() throws IOException {
		if (os != null) {
			os.close();
			os = null;
		}
		super.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationDelivery#deliverPacket(boolean,
	 *      byte[])
	 */
	public void deliverPacket(boolean finalPacket, byte[] buffer) throws IOException {
		byte[] b = session.readOperation();
		HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		switch (requestHeaders.getResponseCode()) {
		case OBEXOperationCodes.GET | OBEXOperationCodes.FINAL_BIT:
			finalPacketReceived = true;
			replyWithPacket(finalPacket, buffer);
			break;
		case OBEXOperationCodes.ABORT:
			processAbort();
			break;
		default:
			session.writeOperation(ResponseCodes.OBEX_HTTP_UNAVAILABLE, null);
		}
	}

	private void processAbort() throws IOException {
		finalPacketReceived = true;
	}

	private void replyWithPacket(boolean finalPacket, byte[] buffer) throws IOException {
		HeaderSet dataHeaders = OBEXSessionBase.createOBEXHeaderSet();
		int opcode = OBEXOperationCodes.OBEX_RESPONSE_CONTINUE;
		int dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY;
		if (finalPacket) {
			opcode = OBEXOperationCodes.OBEX_RESPONSE_SUCCESS;
			dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY_END;
		}

		dataHeaders.setHeader(dataHeaderID, buffer);
		session.writeOperation(opcode, OBEXHeaderSetImpl.toByteArray(dataHeaders));

		// if (finalPacket) {
		// byte[] b = session.readOperation();
		// HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		// if (requestHeaders.getResponseCode() != (OBEXOperationCodes.GET |
		// OBEXOperationCodes.FINAL_BIT)) {
		// throw new IOException("wrong final request "
		// +
		// OBEXUtils.toStringObexResponseCodes(requestHeaders.getResponseCode()));
		// }
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXServerOperation#writeResponse(int)
	 */
	void writeResponse(int responseCode) throws IOException {
		session.writeOperation(responseCode, OBEXHeaderSetImpl.toByteArray(sendHeaders));
	}
}
