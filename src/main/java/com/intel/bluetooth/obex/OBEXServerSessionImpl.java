/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.HeaderSet;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.UtilsJavaSE;

class OBEXServerSessionImpl extends OBEXSessionBase implements Connection, Runnable {

	private ServerRequestHandler handler;
	
	private Authenticator auth;
	
	private boolean isConnected = false;
	
	private OBEXServerOperation operation;
	
	public OBEXServerSessionImpl(StreamConnection connection, ServerRequestHandler handler, Authenticator auth) throws IOException {
		super(connection);
		this.handler = handler;
		this.auth = auth;
		Thread t = new Thread(this);
		UtilsJavaSE.threadSetDaemon(t);
		t.start();
	}


	public void run() {
		try {
			while (!isClosed()) {
				if (processOperation()) {
					break;
				}
			}
		} catch (Throwable e) {
			DebugLog.error("OBEXServerSession error", e);
		} finally {
			DebugLog.debug("OBEXServerSession ends");
			try {
				super.close();
			} catch (IOException e) {
				DebugLog.error("close error", e);
			}
		}
	}

	public void close() throws IOException {
		if (!isClosed()) {
			DebugLog.debug("OBEXServerSession close");
			if (operation != null) {
				operation.close();
				operation = null;
			}
		}
		super.close();
	}
	

	private boolean processOperation() throws IOException {
		boolean isEOF = false;
		DebugLog.debug("OBEXServerSession readOperation");
		byte[] b = readOperation();
		int opcode = b[0] & 0xFF;
		boolean finalPacket = ((opcode & OBEXOperationCodes.FINAL_BIT) != 0);
		if (finalPacket) {
			DebugLog.debug("OBEXServerSession operation finalPacket");	
		}
		switch (opcode) {
		case OBEXOperationCodes.CONNECT:
			processConnect(b);
			break;
		case OBEXOperationCodes.DISCONNECT:
			processDisconnect(b);
			break;
		case OBEXOperationCodes.PUT | OBEXOperationCodes.FINAL_BIT:
		case OBEXOperationCodes.PUT:
			processPut(b, finalPacket);
			break;
		case OBEXOperationCodes.SETPATH | OBEXOperationCodes.FINAL_BIT:
		case OBEXOperationCodes.SETPATH:
			processSetPath(b, finalPacket);
			break;
		case OBEXOperationCodes.ABORT:
			processAbort();
			break;
		case OBEXOperationCodes.GET | OBEXOperationCodes.FINAL_BIT:
		case OBEXOperationCodes.GET:
			processGet(b, finalPacket);
			break;
		default:
			writeOperation(ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED, null);
		}
		return isEOF;
	}

	private void processConnect(byte[] b) throws IOException {
		if (b[3] != OBEXOperationCodes.OBEX_VERSION) {
            throw new IOException("Unsupported client OBEX version " + b[3]);
		}
		if (b.length < 7) {
			throw new IOException("Corrupted OBEX data");
		}
		int requestedMTU = OBEXUtils.bytesToShort(b[5], b[6]);
		if (requestedMTU < OBEXOperationCodes.OBEX_MINIMUM_MTU) {
			throw new IOException("Invalid MTU " + requestedMTU);
		}
		this.mtu = requestedMTU;
		DebugLog.debug("mtu selected", this.mtu);
		
		HeaderSet requestHeaders = OBEXHeaderSetImpl.read(b[0], b, 7);
		HeaderSet replyHeaders = createOBEXHeaderSet();
		int rc = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
		try {
			rc = handler.onConnect(requestHeaders, replyHeaders);
		} catch (Throwable e) {
			DebugLog.error("onConnect", e);
		}
		byte[] connectResponse = new byte[4];
		connectResponse[0] = OBEXOperationCodes.OBEX_VERSION;
		connectResponse[1] = 0; /* Flags */
		connectResponse[2] = OBEXUtils.hiByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		connectResponse[3] = OBEXUtils.loByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		writeOperation(rc, connectResponse, OBEXHeaderSetImpl.toByteArray(replyHeaders));
		this.isConnected = true;
	}

	private boolean validateConnection() throws IOException {
		if (this.isConnected) {
			return true;
		}
		writeOperation(ResponseCodes.OBEX_HTTP_BAD_REQUEST, null);
		return false;
	}
	
	private void processDisconnect(byte[] b) throws IOException {
		if (!validateConnection()) {
			return;
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		HeaderSet replyHeaders = createOBEXHeaderSet();
		int rc = ResponseCodes.OBEX_HTTP_OK;
		try {
			handler.onDisconnect(requestHeaders, replyHeaders);
		} catch (Throwable e) {
			rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			DebugLog.error("onDisconnect", e);
		}
		writeOperation(rc, OBEXHeaderSetImpl.toByteArray(replyHeaders));
		this.isConnected = false;
	}


	private boolean processDelete(HeaderSet requestHeaders) throws IOException {
		if ((requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY) == null) && (requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END) == null)) {
			HeaderSet replyHeaders = createOBEXHeaderSet();
			int rc = ResponseCodes.OBEX_HTTP_OK;
			try {
				rc = handler.onDelete(requestHeaders, replyHeaders);
			} catch (Throwable e) {
				rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
				DebugLog.error("onDelete", e);
			}
			writeOperation(rc, OBEXHeaderSetImpl.toByteArray(replyHeaders));
			return true;
		}
		return false;
	}
	
	private void processPut(byte[] b, boolean finalPacket) throws IOException {
		if (!validateConnection()) {
			return;
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		if (finalPacket && processDelete(requestHeaders)) {
			return;
		}
		try {
			operation = new OBEXServerOperationPut(this, requestHeaders, finalPacket);
			int rc = ResponseCodes.OBEX_HTTP_OK;
			try {
				rc = handler.onPut(operation);
			} catch (Throwable e) {
				rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
				DebugLog.error("onPut", e);
			}
			writeOperation(rc, null);
		} finally {
			operation.close();
			operation = null;
		}
	}
	

	private void processGet(byte[] b, boolean finalPacket) throws IOException {
		if (!validateConnection()) {
			return;
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		
		try {
			operation = new OBEXServerOperationGet(this, requestHeaders);
			int rc = ResponseCodes.OBEX_HTTP_OK;
			try {
				rc = handler.onGet(operation);
			} catch (Throwable e) {
				rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
				DebugLog.error("onGet", e);
			}
			writeOperation(rc, null);
		} finally {
			operation.close();
			operation = null;
		}
	}

	
	private void processAbort() throws IOException {
		if (!validateConnection()) {
			return;
		}
		if (operation != null) {
			operation.close();
			operation = null;
		}
		writeOperation(OBEXOperationCodes.OBEX_RESPONSE_SUCCESS, null);
	}
	
	private void processSetPath(byte[] b, boolean finalPacket) throws IOException {
		if (!validateConnection()) {
			return;
		}
		if (b.length < 5) {
			throw new IOException("Corrupted OBEX data");
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.read(b[0], b, 5);
		boolean backup = ((b[4] & 1) != 0);
		boolean create = ((b[4] & 2) != 0);
		
		HeaderSet replyHeaders = createOBEXHeaderSet();
		int rc = ResponseCodes.OBEX_HTTP_OK;
		try {
			rc = handler.onSetPath(requestHeaders, replyHeaders, backup, create);
		} catch (Throwable e) {
			rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			DebugLog.error("onSetPath", e);
		}
		writeOperation(rc, OBEXHeaderSetImpl.toByteArray(replyHeaders));
	}

}
