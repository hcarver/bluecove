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
import java.util.Vector;

import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.NotImplementedError;
import com.intel.bluetooth.NotImplementedIOException;
import com.intel.bluetooth.Utils;

/**
 * ClientSession implementation. See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 * @author vlads
 * 
 */
public class OBEXClientSessionImpl extends OBEXSessionBase implements ClientSession {

	private boolean isConnected;
	
	private OBEXClientOperation operation;
	
	private static final String FQCN = OBEXClientSessionImpl.class.getName();
	
	private static final Vector fqcnSet = new Vector(); 
	
	static {
		fqcnSet.addElement(FQCN);
	}
	
    /**
     * Applications should not used this function.
     * 
     * @exception Error if called from outside of BlueCove internal code.
     */
	public OBEXClientSessionImpl(StreamConnection conn) throws IOException, Error {
		super(conn);
		Utils.isLegalAPICall(fqcnSet);
		this.isConnected = false;
		this.operation = null;
	}

	public HeaderSet createHeaderSet() {
		return OBEXSessionBase.createOBEXHeaderSet();
	}

	/* (non-Javadoc)
	 * @see javax.obex.ClientSession#connect(javax.obex.HeaderSet)
	 */
	public HeaderSet connect(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		if (isConnected) {
            throw new IOException("Session already connected");
		}
		byte[] connectRequest = new byte[4];
		connectRequest[0] = OBEXOperationCodes.OBEX_VERSION;
		connectRequest[1] = 0; /* Flags */
		connectRequest[2] = OBEXUtils.hiByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		connectRequest[3] = OBEXUtils.loByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		writeOperation(OBEXOperationCodes.CONNECT, connectRequest, OBEXHeaderSetImpl.toByteArray(headers));
		
		byte[] b = readOperation();
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
		
		HeaderSet responseHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 7);
		
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
		writeOperation(OBEXOperationCodes.DISCONNECT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
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

	private void canStartOperation()  throws IOException {
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
		byte[] request = new byte[2];
		request[0] = (byte) ((backup?1:0) | (create?0:2));
		request[1] = 0;
		//DebugLog.debug("setPath b[3]", request[0]);
		writeOperation(OBEXOperationCodes.SETPATH | OBEXOperationCodes.FINAL_BIT, request, OBEXHeaderSetImpl.toByteArray(headers));
		
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
	}

	public Operation get(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		writeOperation(OBEXOperationCodes.GET | OBEXOperationCodes.FINAL_BIT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		HeaderSet replyHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		DebugLog.debug0x("GET got reply", replyHeaders.getResponseCode());
		
		this.operation = new OBEXClientOperationGet(this, replyHeaders);
		return this.operation;
	}

	public Operation put(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		this.operation = new OBEXClientOperationPut(this, headers);
		return this.operation;
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		validateCreatedHeaderSet(headers);
		canStartOperation();
		return deleteImp(headers);
	}

	HeaderSet deleteImp(HeaderSet headers) throws IOException {
		writeOperation(OBEXOperationCodes.PUT | OBEXOperationCodes.FINAL_BIT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
	}
	
	public void setAuthenticator(Authenticator auth) {
		if (auth == null) {
			throw new NullPointerException("auth is null");
		}
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
