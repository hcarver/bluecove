/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2009 Vlad Skarzhevskyy
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

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.Utils;

/**
 * ClientSession implementation. See <a href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 */
public class OBEXClientSessionImpl extends OBEXSessionBase implements ClientSession {

    protected OBEXClientOperation operation;

	private static final String FQCN = OBEXClientSessionImpl.class.getName();

	private static final Vector fqcnSet = new Vector();

	static {
		fqcnSet.addElement(FQCN);
	}

	/**
	 * Applications should not used this function.
	 * 
	 * @exception Error
	 *                if called from outside of BlueCove internal code.
	 */
	public OBEXClientSessionImpl(StreamConnection conn, OBEXConnectionParams obexConnectionParams) throws IOException,
			Error {
		super(conn, obexConnectionParams);
		Utils.isLegalAPICall(fqcnSet);
		this.requestSent = false;
		this.isConnected = false;
		this.operation = null;
	}

	public HeaderSet createHeaderSet() {
		return OBEXSessionBase.createOBEXHeaderSet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.obex.ClientSession#connect(javax.obex.HeaderSet)
	 */
	public HeaderSet connect(HeaderSet headers) throws IOException {
		return connectImpl(headers, false);
	}

	private HeaderSet connectImpl(HeaderSet headers, boolean retry) throws IOException {
		validateCreatedHeaderSet(headers);
		if (isConnected) {
			throw new IOException("Session already connected");
		}
		byte[] connectRequest = new byte[4];
		connectRequest[0] = OBEXOperationCodes.OBEX_VERSION;
		connectRequest[1] = 0; /* Flags */
		connectRequest[2] = OBEXUtils.hiByte(obexConnectionParams.mtu);
		connectRequest[3] = OBEXUtils.loByte(obexConnectionParams.mtu);
		writePacketWithFlags(OBEXOperationCodes.CONNECT, connectRequest, (OBEXHeaderSetImpl) headers);

		byte[] b = readPacket();
		if (b.length < 6) {
			if (b.length == 3) {
				throw new IOException("Invalid response from OBEX server " + OBEXUtils.toStringObexResponseCodes(b[0]));
			}
			throw new IOException("Invalid response from OBEX server");
		}
		int serverMTU = OBEXUtils.bytesToShort(b[5], b[6]);
		if (serverMTU < OBEXOperationCodes.OBEX_MINIMUM_MTU) {
			throw new IOException("Invalid MTU " + serverMTU);
		}
		if (serverMTU < this.mtu) {
			this.mtu = serverMTU;
		}
		DebugLog.debug("mtu selected", this.mtu);

		OBEXHeaderSetImpl responseHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 7);

		Object connID = responseHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_CONNECTION);
		if (connID != null) {
			this.connectionID = ((Long) connID).longValue();
		}

		validateAuthenticationResponse((OBEXHeaderSetImpl) headers, responseHeaders);
		if ((!retry) && (responseHeaders.getResponseCode() == ResponseCodes.OBEX_HTTP_UNAUTHORIZED)
				&& (responseHeaders.hasAuthenticationChallenge())) {
			HeaderSet replyHeaders = OBEXHeaderSetImpl.cloneHeaders(headers);
			handleAuthenticationChallenge(responseHeaders, (OBEXHeaderSetImpl) replyHeaders);
			return connectImpl(replyHeaders, true);

		}
		if (responseHeaders.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
			this.isConnected = true;
		}
		return responseHeaders;
	}

	public HeaderSet disconnect(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		if (!isConnected) {
			throw new IOException("Session not connected");
		}
		writePacket(OBEXOperationCodes.DISCONNECT, (OBEXHeaderSetImpl) headers);
		byte[] b = readPacket();
		this.isConnected = false;
		if (this.operation != null) {
			this.operation.close();
			this.operation = null;
		}
		return OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
	}

	public void setConnectionID(long id) {
		if (id < 0 || id > 0xffffffffl) {
			throw new IllegalArgumentException("Invalid connectionID " + id);
		}
		this.connectionID = id;
	}

	public long getConnectionID() {
		return this.connectionID;
	}

	protected void canStartOperation() throws IOException {
		if (!isConnected) {
			throw new IOException("Session not connected");
		}
		if (this.operation != null) {
			if (!this.operation.isClosed()) {
				throw new IOException("Client is already in an operation");
			}
			this.operation = null;
		}
	}

	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		return setPathImpl(headers, backup, create, false);
	}

	private HeaderSet setPathImpl(HeaderSet headers, boolean backup, boolean create, boolean authentRetry)
			throws IOException {
		byte[] request = new byte[2];
		request[0] = (byte) ((backup ? 1 : 0) | (create ? 0 : 2));
		request[1] = 0;
		// DebugLog.debug("setPath b[3]", request[0]);
		writePacketWithFlags(OBEXOperationCodes.SETPATH_FINAL, request, (OBEXHeaderSetImpl) headers);

		byte[] b = readPacket();
		OBEXHeaderSetImpl responseHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		validateAuthenticationResponse((OBEXHeaderSetImpl) headers, responseHeaders);
		if (!authentRetry && (responseHeaders.getResponseCode() == ResponseCodes.OBEX_HTTP_UNAUTHORIZED)
				&& (responseHeaders.hasAuthenticationChallenge())) {
			OBEXHeaderSetImpl retryHeaders = OBEXHeaderSetImpl.cloneHeaders(headers);
			handleAuthenticationChallenge(responseHeaders, retryHeaders);
			return setPathImpl(retryHeaders, backup, create, true);
		} else {
			return responseHeaders;
		}
	}

	public Operation get(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		this.operation = new OBEXClientOperationGet(this, (OBEXHeaderSetImpl) headers);
		return this.operation;
	}

	public Operation put(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		this.operation = new OBEXClientOperationPut(this, (OBEXHeaderSetImpl) headers);
		return this.operation;
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		return deleteImp(headers, false);
	}

	HeaderSet deleteImp(HeaderSet headers, boolean authentRetry) throws IOException {
		writePacket(OBEXOperationCodes.PUT_FINAL, (OBEXHeaderSetImpl) headers);
		byte[] b = readPacket();
		OBEXHeaderSetImpl responseHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		validateAuthenticationResponse((OBEXHeaderSetImpl) headers, responseHeaders);
		if (!authentRetry && (responseHeaders.getResponseCode() == ResponseCodes.OBEX_HTTP_UNAUTHORIZED)
				&& (responseHeaders.hasAuthenticationChallenge())) {
			OBEXHeaderSetImpl retryHeaders = OBEXHeaderSetImpl.cloneHeaders(headers);
			handleAuthenticationChallenge(responseHeaders, retryHeaders);
			return deleteImp(retryHeaders, true);
		} else {
			return responseHeaders;
		}
	}

	public void setAuthenticator(Authenticator auth) {
		if (auth == null) {
			throw new NullPointerException("auth is null");
		}
		this.authenticator = auth;
	}

	public void close() throws IOException {
        try {
            if (this.operation != null) {
                this.operation.close();
                this.operation = null;
            }
        } finally {
            // Close connection even if operation can't be closed.
            super.close();
        }
    }

}
