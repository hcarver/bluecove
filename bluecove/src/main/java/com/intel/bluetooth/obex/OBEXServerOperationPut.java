/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
class OBEXServerOperationPut extends OBEXServerOperation implements OBEXOperationReceive {

	protected OBEXServerOperationPut(OBEXServerSessionImpl session, HeaderSet receivedHeaders, boolean finalPacket)
			throws IOException {
		super(session, receivedHeaders);
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
		DebugLog.debug("openInputStream");
		inputStreamOpened = true;
		return inputStream;
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
		return new UnsupportedOutputStream();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		DebugLog.debug("server close put operation");
		inputStream.close();
		super.close();
	}

	protected boolean readRequestPacket() throws IOException {
		byte[] b = session.readOperation();
		int opcode = b[0] & 0xFF;
		boolean finalPacket = ((opcode & OBEXOperationCodes.FINAL_BIT) != 0);
		if (finalPacket) {
			DebugLog.debug("server operation got final packet");
			finalPacketReceived = true;
		}
		switch (opcode) {
		case OBEXOperationCodes.PUT_FINAL:
		case OBEXOperationCodes.PUT:
			HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
			OBEXHeaderSetImpl.appendHeaders(this.receivedHeaders, requestHeaders);
			processIncommingData(requestHeaders, finalPacket);
			break;
		case OBEXOperationCodes.ABORT:
			processAbort();
			break;
		default:
			errorReceived = true;
			DebugLog.debug0x("server operation invalid request", OBEXUtils.toStringObexResponseCodes(opcode), opcode);
			session.writeOperation(ResponseCodes.OBEX_HTTP_BAD_REQUEST, null);
		}
		return finalPacket;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.obex.OBEXOperationReceive#receiveData(com.intel.bluetooth.obex.OBEXOperationInputStream)
	 */
	public void receiveData(OBEXOperationInputStream is) throws IOException {
		if (finalPacketReceived || errorReceived) {
			is.appendData(null, true);
			return;
		}
		DebugLog.debug("server operation reply continue");
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		sendHeaders = null;
		readRequestPacket();
	}

	private void processAbort() throws IOException {
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_SUCCESS, null);
		close();
		throw new IOException("Operation aborted by client");
	}

}
