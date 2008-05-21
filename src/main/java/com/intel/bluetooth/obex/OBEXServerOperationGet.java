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

import com.intel.bluetooth.DebugLog;

/**
 * @author vlads
 * 
 */
class OBEXServerOperationGet extends OBEXServerOperation implements OBEXOperationDelivery, OBEXOperationReceive {

	protected OBEXServerOperationGet(OBEXServerSessionImpl session, HeaderSet receivedHeaders, boolean finalPacket)
			throws IOException {
		super(session, receivedHeaders);
		if (finalPacket) {
			requestEnded = true;
		}
		this.inputStream = new OBEXOperationInputStream(this);
		processIncommingData(receivedHeaders, finalPacket);
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
		if (inputStreamOpened) {
			throw new IOException("input stream already open");
		}
		this.inputStreamOpened = true;
		return this.inputStream;
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
		if (outputStream != null) {
			throw new IOException("output stream already open");
		}
		requestEnded = true;
		outputStream = new OBEXOperationOutputStream(session.mtu, this);
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		sendHeaders = null;
		return outputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (outputStream != null) {
			outputStream.close();
			outputStream = null;
		}
		inputStream.close();
		super.close();
	}

	protected void processIncommingHeaders(HeaderSet dataHeaders) throws IOException {
		if (this.receivedHeaders != null) {
			OBEXHeaderSetImpl.appendHeaders(this.receivedHeaders, dataHeaders);
		} else {
			this.receivedHeaders = dataHeaders;
		}
	}

	protected void processIncommingData(HeaderSet dataHeaders, boolean eof) throws IOException {
		byte[] data = (byte[]) dataHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY);
		if (data == null) {
			data = (byte[]) dataHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END);
			if (data != null) {
				eof = true;
			}
		}
		if (data != null) {
			DebugLog.debug("server received Data eof " + eof + " len", data.length);
			inputStream.appendData(data, eof);
		} else if (eof) {
			inputStream.appendData(null, eof);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationReceive#receiveData(com.intel.bluetooth.obex.OBEXOperationInputStream)
	 */
	public void receiveData(OBEXOperationInputStream is) throws IOException {
		if (requestEnded) {
			this.inputStream.appendData(null, true);
			return;
		}
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		sendHeaders = null;
		byte[] b = session.readOperation();
		HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		int requestCode = requestHeaders.getResponseCode();
		switch (requestCode) {
		case OBEXOperationCodes.GET | OBEXOperationCodes.FINAL_BIT:
			finalPacketReceived = true;
			requestEnded = true;
		case OBEXOperationCodes.GET:
			processIncommingHeaders(requestHeaders);
			processIncommingData(requestHeaders, requestEnded);
			break;
		case OBEXOperationCodes.ABORT:
			processAbort();
			break;
		default:
			DebugLog.debug0x("server failed to handle request", OBEXUtils.toStringObexResponseCodes(requestCode),
					requestCode);
			session.writeOperation(ResponseCodes.OBEX_HTTP_UNAVAILABLE, null);
			throw new IOException("Operation request error, 0x" + Integer.toHexString(requestCode) + " "
					+ OBEXUtils.toStringObexResponseCodes(requestCode));
		}
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
		int requestCode = requestHeaders.getResponseCode();
		switch (requestCode) {
		case OBEXOperationCodes.GET | OBEXOperationCodes.FINAL_BIT:
			finalPacketReceived = true;
			requestEnded = true;
			processIncommingHeaders(requestHeaders);
			processIncommingData(requestHeaders, requestEnded);
			replyWithDataPacket(finalPacket, buffer);
			break;
		case OBEXOperationCodes.ABORT:
			processAbort();
			break;
		default:
			DebugLog.debug0x("server failed to handle request", OBEXUtils.toStringObexResponseCodes(requestCode),
					requestCode);
			session.writeOperation(ResponseCodes.OBEX_HTTP_UNAVAILABLE, null);
			throw new IOException("Operation request error, 0x" + Integer.toHexString(requestCode) + " "
					+ OBEXUtils.toStringObexResponseCodes(requestCode));
		}
	}

	private void processAbort() throws IOException {
		// TODO proper abort + UnitTests
		finalPacketReceived = true;
		requestEnded = true;
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_SUCCESS, null);
		throw new IOException("Operation aborted");
	}

	private void replyWithDataPacket(boolean finalPacket, byte[] buffer) throws IOException {
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
		sendHeaders = null;
	}

}
