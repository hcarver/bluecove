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
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth.obex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.DebugLog;

/**
 *
 *
 */
abstract class OBEXServerOperation implements Operation, OBEXOperation {

	protected OBEXServerSessionImpl session;

	protected HeaderSet receivedHeaders;

	protected HeaderSet sendHeaders;

	protected boolean isClosed = false;

	protected boolean finalPacketReceived = false;

	protected boolean requestEnded = false;

	protected boolean errorReceived = false;

	protected boolean incommingDataReceived = false;

	protected OBEXOperationOutputStream outputStream;

	protected boolean outputStreamOpened = false;

	protected OBEXOperationInputStream inputStream;

	protected boolean inputStreamOpened = false;

	protected OBEXServerOperation(OBEXServerSessionImpl session, HeaderSet receivedHeaders) {
		this.session = session;
		this.receivedHeaders = receivedHeaders;
	}

	public boolean exchangeRequestPhasePackets() throws IOException {
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, null);
		return readRequestPacket();
	}

	protected abstract boolean readRequestPacket() throws IOException;

	void writeResponse(int responseCode) throws IOException {
		DebugLog.debug("server operation reply final");
		session.writeOperation(responseCode, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		sendHeaders = null;
		if (responseCode == ResponseCodes.OBEX_HTTP_OK) {
			while ((!finalPacketReceived) && (!session.isClosed())) {
				DebugLog.debug("server waits to receive final packet");
				readRequestPacket();
				if (!errorReceived) {
					session.writeOperation(responseCode, null);
				}
			}
		} else {
			DebugLog.debug("sent final reply");
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
			incommingDataReceived = true;
			DebugLog.debug("server received Data eof: " + eof + " len:", data.length);
			inputStream.appendData(data, eof);
		} else if (eof) {
			inputStream.appendData(null, eof);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.obex.Operation#abort()
	 */
	public void abort() throws IOException {
		throw new IOException("Can't abort server operation");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.obex.Operation#getReceivedHeaders()
	 */
	public HeaderSet getReceivedHeaders() throws IOException {
		return OBEXHeaderSetImpl.cloneHeaders(receivedHeaders);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.obex.Operation#getResponseCode()
	 */
	public int getResponseCode() throws IOException {
		throw new IOException("Operation object was created by an OBEX server");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.obex.Operation#sendHeaders(javax.obex.HeaderSet)
	 */
	public void sendHeaders(HeaderSet headers) throws IOException {
		sendHeaders = headers;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.ContentConnection#getEncoding() <code>getEncoding()</code>
	 *      will always return <code>null</code>
	 */
	public String getEncoding() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.ContentConnection#getLength() <code>getLength()</code>
	 *      will return the length specified by the OBEX Length header or -1 if
	 *      the OBEX Length header was not included.
	 */
	public long getLength() {
		Long len;
		try {
			len = (Long) receivedHeaders.getHeader(HeaderSet.LENGTH);
		} catch (IOException e) {
			return -1;
		}
		if (len == null) {
			return -1;
		}
		return len.longValue();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.ContentConnection#getType() <code>getType()</code>
	 *      will return the value specified in the OBEX Type header or <code>null</code>
	 *      if the OBEX Type header was not included.
	 */
	public String getType() {
		try {
			return (String) receivedHeaders.getHeader(HeaderSet.TYPE);
		} catch (IOException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.InputConnection#openDataInputStream()
	 */
	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.OutputConnection#openDataOutputStream()
	 */
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		this.isClosed = true;
	}

	public boolean isClosed() {
		return this.isClosed;
	}

	public boolean isIncommingDataReceived() {
		return this.incommingDataReceived;
	}

	public boolean isErrorReceived() {
		return this.errorReceived;
	}
}
