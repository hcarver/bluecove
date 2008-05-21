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

import java.io.EOFException;
import java.io.IOException;

import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.HeaderSet;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BluetoothServerConnection;
import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.UtilsJavaSE;

class OBEXServerSessionImpl extends OBEXSessionBase implements Runnable, BluetoothServerConnection {

	private ServerRequestHandler handler;

	private boolean isConnected = false;

	private OBEXServerOperation operation;

	private boolean closeRequested = false;

	private volatile boolean delayClose = false;

	private Object canCloseEvent = new Object();

	private Object stackID;

	private static int threadNumber;

	private static synchronized int nextThreadNum() {
		return threadNumber++;
	}

	OBEXServerSessionImpl(StreamConnection connection, ServerRequestHandler handler, Authenticator authenticator,
			OBEXConnectionParams obexConnectionParams) throws IOException {
		super(connection, obexConnectionParams);
		this.handler = handler;
		this.authenticator = authenticator;
		stackID = BlueCoveImpl.getCurrentThreadBluetoothStackID();
		Thread t = new Thread(this, "OBEXServerSessionThread-" + nextThreadNum());
		UtilsJavaSE.threadSetDaemon(t);
		t.start();
	}

	public void run() {
		try {
			if (stackID != null) {
				BlueCoveImpl.setThreadBluetoothStackID(stackID);
			}
			while (!isClosed() && !closeRequested) {
				if (!handleRequest()) {
					return;
				}
			}
		} catch (Throwable e) {
			if (this.isConnected) {
				DebugLog.error("OBEXServerSession error", e);
			}
		} finally {
			DebugLog.debug("OBEXServerSession ends");
			try {
				super.close();
			} catch (IOException e) {
				DebugLog.debug("OBEXServerSession close error", e);
			}
		}
	}

	public void close() throws IOException {
		closeRequested = true;
		while (delayClose) {
			synchronized (canCloseEvent) {
				try {
					if (delayClose) {
						canCloseEvent.wait(700);
					}
				} catch (InterruptedException e) {
				}
				delayClose = false;
			}
		}
		if (!isClosed()) {
			DebugLog.debug("OBEXServerSession close");
			// (new Throwable()).printStackTrace();
			if (operation != null) {
				operation.close();
				operation = null;
			}
		}
		super.close();
	}

	private boolean handleRequest() throws IOException {
		DebugLog.debug("OBEXServerSession handleRequest");
		delayClose = false;
		byte[] b;
		try {
			b = readOperation();
		} catch (EOFException e) {
			if (isConnected) {
				throw e;
			}
			DebugLog.debug("OBEXServerSession got EOF");
			close();
			return false;
		}
		delayClose = true;
		try {
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
			case OBEXOperationCodes.PUT_FINAL:
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
			case OBEXOperationCodes.GET_FINAL:
			case OBEXOperationCodes.GET:
				processGet(b, finalPacket);
				break;
			default:
				writeOperation(ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED, null);
			}
		} finally {
			delayClose = false;
		}
		synchronized (canCloseEvent) {
			canCloseEvent.notifyAll();
		}
		return true;
	}

	private void processConnect(byte[] b) throws IOException {
		DebugLog.debug("Connect operation");
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

		OBEXHeaderSetImpl requestHeaders = OBEXHeaderSetImpl.readHeaders(b, 7);
		handleAuthenticationResponse(requestHeaders, handler);
		HeaderSet replyHeaders = createOBEXHeaderSet();
		handleAuthenticationChallenge(requestHeaders, (OBEXHeaderSetImpl) replyHeaders);
		int rc = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
		try {
			rc = handler.onConnect(requestHeaders, replyHeaders);
		} catch (Throwable e) {
			DebugLog.error("onConnect", e);
		}
		byte[] connectResponse = new byte[4];
		connectResponse[0] = OBEXOperationCodes.OBEX_VERSION;
		connectResponse[1] = 0; /* Flags */
		connectResponse[2] = OBEXUtils.hiByte(obexConnectionParams.mtu);
		connectResponse[3] = OBEXUtils.loByte(obexConnectionParams.mtu);
		writeOperation(rc, connectResponse, OBEXHeaderSetImpl.toByteArray(replyHeaders));
		if (rc == ResponseCodes.OBEX_HTTP_OK) {
			this.isConnected = true;
		}
	}

	private boolean validateConnection() throws IOException {
		if (this.isConnected) {
			return true;
		}
		writeOperation(ResponseCodes.OBEX_HTTP_BAD_REQUEST, null);
		return false;
	}

	private void processDisconnect(byte[] b) throws IOException {
		DebugLog.debug("Disconnect operation");
		if (!validateConnection()) {
			return;
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b, 3);
		HeaderSet replyHeaders = createOBEXHeaderSet();
		int rc = ResponseCodes.OBEX_HTTP_OK;
		try {
			handler.onDisconnect(requestHeaders, replyHeaders);
		} catch (Throwable e) {
			rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			DebugLog.error("onDisconnect", e);
		}
		this.isConnected = false;
		writeOperation(rc, OBEXHeaderSetImpl.toByteArray(replyHeaders));
	}

	private boolean processDelete(HeaderSet requestHeaders) throws IOException {
		if ((requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY) == null)
				&& (requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END) == null)) {
			DebugLog.debug("Delete operation");
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
		DebugLog.debug("Put operation");
		if (!validateConnection()) {
			return;
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b, 3);
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
			operation.writeResponse(rc);
		} finally {
			operation.close();
			operation = null;
		}
	}

	private void processGet(byte[] b, boolean finalPacket) throws IOException {
		DebugLog.debug("Get operation");
		if (!validateConnection()) {
			return;
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b, 3);

		try {
			operation = new OBEXServerOperationGet(this, requestHeaders, finalPacket);
			int rc = ResponseCodes.OBEX_HTTP_OK;
			try {
				rc = handler.onGet(operation);
			} catch (Throwable e) {
				rc = ResponseCodes.OBEX_HTTP_UNAVAILABLE;
				DebugLog.error("onGet", e);
			}
			operation.writeResponse(rc);
		} finally {
			operation.close();
			operation = null;
		}
	}

	private void processAbort() throws IOException {
		DebugLog.debug("Abort operation");
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
		DebugLog.debug("SetPath operation");
		if (!validateConnection()) {
			return;
		}
		if (b.length < 5) {
			throw new IOException("Corrupted OBEX data");
		}
		HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b, 5);
		// DebugLog.debug("setPath b[3]", b[3]);
		// b[4] = (byte) ((backup?1:0) | (create?0:2));
		boolean backup = ((b[3] & 1) != 0);
		boolean create = ((b[3] & 2) == 0);
		DebugLog.debug("setPath backup", backup);
		DebugLog.debug("setPath create", create);

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
