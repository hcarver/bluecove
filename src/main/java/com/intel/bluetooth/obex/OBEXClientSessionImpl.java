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

import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.NotImplementedError;
import com.intel.bluetooth.NotImplementedIOException;
import com.intel.bluetooth.Utils;

/**
 * See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 * 
 * 
 * @author vlads
 * 
 */
public class OBEXClientSessionImpl  extends OBEXSessionBase implements ClientSession {

	private Authenticator authenticator;
	
	private boolean isConnected;
	
	private OBEXClientOperation operation;
	
	public OBEXClientSessionImpl(StreamConnection conn) throws IOException {
		super(conn);
		this.isConnected = false;
		this.operation = null;
	}

	public HeaderSet createHeaderSet() {
		return OBEXSessionBase.createOBEXHeaderSet();
	}

	public HeaderSet connect(HeaderSet headers) throws IOException {
		byte[] connectRequest = new byte[4];
		connectRequest[0] = OBEXOperationCodes.OBEX_VERSION;
		connectRequest[1] = 0; /* Flags */
		connectRequest[2] = Utils.hiByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		connectRequest[3] = Utils.loByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		writeOperation(OBEXOperationCodes.CONNECT, connectRequest, OBEXHeaderSetImpl.toByteArray(headers));
		
		byte[] b = readOperation();
		int serverMTU = Utils.bytesToShort(b[5], b[6]);
		if (serverMTU < OBEXOperationCodes.OBEX_MINIMUM_MTU) {
			throw new IOException("Invalid MTU " + serverMTU);
		}
		if (serverMTU < this.mtu) {
			this.mtu = serverMTU;
		}
		DebugLog.debug("mtu selected", this.mtu);
		
		HeaderSet responseHeaders = OBEXHeaderSetImpl.read(b[0], b, 7);
		
		Object connID = responseHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_CONNECTION);
		if (connID != null) {
			this.connectionID = ((Long)connID).longValue(); 
		}
		
		Object authResp = responseHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_AUTH_RESPONSE);
		if ((authResp != null) && (authenticator != null)) {
			if (NotImplementedError.enabled) {
				throw new NotImplementedIOException();	
			}
			throw new NotImplementedIOException();
		}
		
		Object authChallenge = responseHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_AUTH_CHALLENGE);
		if ((authChallenge != null) && (authenticator != null)) {
			if (NotImplementedError.enabled) {
				throw new NotImplementedIOException();	
			}
			throw new NotImplementedIOException();
		}
		this.isConnected = true;
		return responseHeaders;
	}

	public HeaderSet disconnect(HeaderSet headers) throws IOException {
		if (!isConnected) {
            throw new IOException("Session not connected");
		}
		writeOperation(OBEXOperationCodes.DISCONNECT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		this.isConnected = false;
		return OBEXHeaderSetImpl.read(b[0], b, 3);
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

	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException {
		if (!isConnected) {
            throw new IOException("Session not connected");
		}
		byte[] request = new byte[2];
		request[0] = (byte) ((backup?1:0) | (create?0:2));
		request[1] = 0;
		writeOperation(OBEXOperationCodes.SETPATH | OBEXOperationCodes.FINAL_BIT, request, OBEXHeaderSetImpl.toByteArray(headers));
		
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.read(b[0], b, 3);
	}

	public Operation get(HeaderSet headers) throws IOException {
		if (!isConnected) {
            throw new IOException("Session not connected");
		}
		if (this.operation != null) {
			this.operation.close();
			this.operation = null;
		}
		writeOperation(OBEXOperationCodes.GET, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		HeaderSet replyHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		DebugLog.debug0x("GET reply", replyHeaders.getResponseCode());
		
		if (replyHeaders.getResponseCode() != OBEXOperationCodes.OBEX_RESPONSE_CONTINUE) {
			throw new IOException ("Connection not accepted");
		}
				
		this.operation = new OBEXClientOperationGet(this, replyHeaders);
		return this.operation;
	}

	public Operation put(HeaderSet headers) throws IOException {
		if (!isConnected) {
            throw new IOException("Session not connected");
		}
		if (this.operation != null) {
			this.operation.close();
			this.operation = null;
		}
		writeOperation(OBEXOperationCodes.PUT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		HeaderSet replyHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		DebugLog.debug0x("PUT reply", replyHeaders.getResponseCode());
		
		if (replyHeaders.getResponseCode() != OBEXOperationCodes.OBEX_RESPONSE_CONTINUE) {
			throw new IOException ("Connection not accepted");
		}
				
		this.operation = new OBEXClientOperationPut(this, replyHeaders);
		return this.operation;
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		if (!isConnected) {
            throw new IOException("Session not connected");
		}
		
		writeOperation(OBEXOperationCodes.PUT | OBEXOperationCodes.FINAL_BIT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.read(b[0], b, 3);
	}

	public void setAuthenticator(Authenticator auth) {
		this.authenticator = auth;
	}

	public void close() throws IOException {
		if (this.operation != null) {
			this.operation.close();
			this.operation = null;
		}
		super.close();
	}

}
