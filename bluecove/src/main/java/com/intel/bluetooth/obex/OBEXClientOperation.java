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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.DebugLog;

abstract class OBEXClientOperation implements Operation, OBEXOperation, OBEXOperationReceive, OBEXOperationDelivery {

	/**
	 * This is not 100% by JSR-82 doc. But some know implementations of OBEX are working this way. This solves the
	 * problems for Samsung phones that are sending nothing in response to GET request without final bit.
	 * 
	 * Basically instead of sending at least two packets 'initial' and 'final' we are sending just 'final' one when
	 * applicable.
	 */
	final static boolean SHORT_REQUEST_PHASE = true;

	protected OBEXClientSessionImpl session;

	protected char operationId;

	protected HeaderSet replyHeaders;

	protected boolean isClosed;

	protected boolean operationInProgress;

	protected boolean operationInContinue;

	protected OBEXOperationOutputStream outputStream;

	protected boolean outputStreamOpened = false;

	protected OBEXOperationInputStream inputStream;

	protected boolean inputStreamOpened = false;

	protected boolean errorReceived = false;

	protected boolean requestEnded = false;

	protected boolean finalBodyReceived = false;

	protected OBEXHeaderSetImpl startOperationHeaders = null;

	private boolean authenticationChallengeCreated = false;

	protected Object lock;

	OBEXClientOperation(OBEXClientSessionImpl session, char operationId, OBEXHeaderSetImpl sendHeaders)
			throws IOException {
		this.session = session;
		this.operationId = operationId;
		this.isClosed = false;
		this.operationInProgress = false;
		this.lock = new Object();
		this.inputStream = new OBEXOperationInputStream(this);
		startOperation(sendHeaders);
	}

	static boolean isShortRequestPhase() {
		return SHORT_REQUEST_PHASE;
	}

	protected void startOperation(OBEXHeaderSetImpl sendHeaders) throws IOException {
		if (SHORT_REQUEST_PHASE) {
			this.startOperationHeaders = sendHeaders;
		} else {
			this.operationInProgress = true;
			exchangePacket(sendHeaders);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationReceive#receiveData(com.intel.bluetooth.obex.OBEXOperationInputStream)
	 */
	public void receiveData(OBEXOperationInputStream is) throws IOException {
		if (SHORT_REQUEST_PHASE) {
			exchangePacket(this.startOperationHeaders);
			this.startOperationHeaders = null;
		} else {
			exchangePacket(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationDelivery#deliverPacket(boolean, byte[])
	 */
	public void deliverPacket(boolean finalPacket, byte[] buffer) throws IOException {
		if (requestEnded) {
			return;
		}
		if (SHORT_REQUEST_PHASE && (this.startOperationHeaders != null)) {
			exchangePacket(this.startOperationHeaders);
			this.startOperationHeaders = null;
		}
		int dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY;
		if (finalPacket) {
			this.operationId |= OBEXOperationCodes.FINAL_BIT;
			dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY_END;
			DebugLog.debug("client Request Phase ended");
			requestEnded = true;
		}
		OBEXHeaderSetImpl dataHeaders = OBEXSessionBase.createOBEXHeaderSetImpl();
		dataHeaders.setHeader(dataHeaderID, buffer);
		exchangePacket(dataHeaders);
	}

	protected void endRequestPhase() throws IOException {
		if (requestEnded) {
			return;
		}
		DebugLog.debug("client ends Request Phase");
		this.operationInProgress = false;
		this.requestEnded = true;
		this.operationId |= OBEXOperationCodes.FINAL_BIT;
		if (SHORT_REQUEST_PHASE) {
			exchangePacket(this.startOperationHeaders);
			this.startOperationHeaders = null;
		} else {
			exchangePacket(null);
		}
	}

	private void exchangePacket(OBEXHeaderSetImpl headers) throws IOException {
		boolean success = false;
		try {
			session.writePacket(this.operationId, headers);
			byte[] b = session.readPacket();
			OBEXHeaderSetImpl dataHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
			int responseCode = dataHeaders.getResponseCode();
			DebugLog.debug0x("client operation got reply", OBEXUtils.toStringObexResponseCodes(responseCode),
					responseCode);
			switch (responseCode) {
			case ResponseCodes.OBEX_HTTP_UNAUTHORIZED:
				if (!authenticationChallengeCreated) {
					// Send the original data again, since it is not accepted
					session.handleAuthenticationChallenge(dataHeaders, (OBEXHeaderSetImpl) headers);
					authenticationChallengeCreated = true;
					exchangePacket(headers);
				} else {
					this.errorReceived = true;
					this.operationInContinue = false;
					processIncommingHeaders(dataHeaders);
					throw new IOException("Authentication Failure");
				}
				break;
			case OBEXOperationCodes.OBEX_RESPONSE_SUCCESS:
				processIncommingHeaders(dataHeaders);
				processIncommingData(dataHeaders, true);
				this.operationInProgress = false;
				this.operationInContinue = false;
				break;
			case OBEXOperationCodes.OBEX_RESPONSE_CONTINUE:
				processIncommingHeaders(dataHeaders);
				processIncommingData(dataHeaders, false);
				this.operationInContinue = true;
				break;
			default:
				this.errorReceived = true;
				this.operationInContinue = false;
				// responseCode may be reported by getResponseCode()
				processIncommingHeaders(dataHeaders);
				processIncommingData(dataHeaders, true);

				// OFF; Rely on getResponseCode() to report the error to the application.
				// if ((this.operationId & OBEXOperationCodes.FINAL_BIT) == 0) {
				// throw new IOException("Operation error, 0x" + Integer.toHexString(responseCode) + " "
				// + OBEXUtils.toStringObexResponseCodes(responseCode));
				// }

			}
			success = true;
		} finally {
			if (!success) {
				errorReceived = true;
			}
		}
	}

	protected void processIncommingHeaders(HeaderSet dataHeaders) throws IOException {
		if (replyHeaders != null) {
			// accumulate all received headers.
			OBEXHeaderSetImpl.appendHeaders(dataHeaders, replyHeaders);
		}
		// replyHeaders will contain responseCode from last reply
		// The Body values are removed by cloneHeaders in getReceivedHeaders()
		this.replyHeaders = dataHeaders;
	}

	protected void processIncommingData(HeaderSet dataHeaders, boolean eof) throws IOException {
		byte[] data = (byte[]) dataHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY);
		if (data == null) {
			data = (byte[]) dataHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END);
			if (data != null) {
				finalBodyReceived = true;
				eof = true;
			}
		}
		if (data != null) {
			DebugLog.debug("client received Data eof: " + eof + " len: ", data.length);
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
		validateOperationIsOpen();
		if ((!this.operationInProgress) && (!this.operationInContinue)) {
			throw new IOException("the transaction has already ended");
		}
		synchronized (lock) {
			if (outputStream != null) {
				outputStream.abort();
			}
			this.inputStream.close();
		}
		writeAbort();
	}

	private void writeAbort() throws IOException {
		try {
			session.writePacket(OBEXOperationCodes.ABORT, null);
			requestEnded = true;
			byte[] b = session.readPacket();
			HeaderSet dataHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
			if (dataHeaders.getResponseCode() != OBEXOperationCodes.OBEX_RESPONSE_SUCCESS) {
				throw new IOException("Fails to abort operation, received "
						+ OBEXUtils.toStringObexResponseCodes(dataHeaders.getResponseCode()));
			}
		} finally {
			this.isClosed = true;
			closeStream();
		}
	}

	private void closeStream() throws IOException {
		try {
			receiveOperationEnd();
		} finally {
			this.operationInProgress = false;
			inputStream.close();
			closeOutputStream();
		}
	}

	private void receiveOperationEnd() throws IOException {
		while (!isClosed() && (operationInContinue)) {
			DebugLog.debug("operation expects operation end");
			receiveData(this.inputStream);
		}
	}

	private void closeOutputStream() throws IOException {
		if (outputStream != null) {
			synchronized (lock) {
				if (outputStream != null) {
					outputStream.close();
				}
				outputStream = null;
			}
		}
	}

	protected void validateOperationIsOpen() throws IOException {
		if (isClosed) {
			throw new IOException("operation closed");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.obex.Operation#getReceivedHeaders()
	 */
	public HeaderSet getReceivedHeaders() throws IOException {
		validateOperationIsOpen();
		endRequestPhase();
		return OBEXHeaderSetImpl.cloneHeaders(this.replyHeaders);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.obex.Operation#getResponseCode()
	 * 
	 * A call will do an implicit close on the Stream and therefore signal that the request is done.
	 */
	public int getResponseCode() throws IOException {
		validateOperationIsOpen();
		endRequestPhase();
		closeOutputStream();
		receiveOperationEnd();
		return this.replyHeaders.getResponseCode();
	}

	public void sendHeaders(HeaderSet headers) throws IOException {
		if (headers == null) {
			throw new NullPointerException("headers are null");
		}
		OBEXHeaderSetImpl.validateCreatedHeaderSet(headers);
		validateOperationIsOpen();
		if (this.requestEnded) {
			throw new IOException("the request phase has already ended");
		}
		if (SHORT_REQUEST_PHASE && (this.startOperationHeaders != null)) {
			exchangePacket(this.startOperationHeaders);
			this.startOperationHeaders = null;
		}
		exchangePacket((OBEXHeaderSetImpl) headers);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.ContentConnection#getEncoding() <code>getEncoding()</code> will always return
	 * <code>null</code>
	 */
	public String getEncoding() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.ContentConnection#getLength() <code>getLength()</code> will return the length
	 * specified by the OBEX Length header or -1 if the OBEX Length header was not included.
	 */
	public long getLength() {
		Long len;
		try {
			len = (Long) replyHeaders.getHeader(HeaderSet.LENGTH);
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
	 * @see javax.microedition.io.ContentConnection#getType() <code>getType()</code> will return the value specified in
	 * the OBEX Type header or <code>null</code> if the OBEX Type header was not included.
	 */
	public String getType() {
		try {
			return (String) replyHeaders.getHeader(HeaderSet.TYPE);
		} catch (IOException e) {
			return null;
		}
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		try {
			endRequestPhase();
		} finally {
			closeStream();
			if (!this.isClosed) {
				this.isClosed = true;
				DebugLog.debug("client operation closed");
			}
		}
	}

	public boolean isClosed() {
		return this.isClosed || this.errorReceived;
	}
}
