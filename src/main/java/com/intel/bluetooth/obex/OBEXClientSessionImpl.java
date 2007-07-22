/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
public class OBEXClientSessionImpl implements ClientSession {

	private StreamConnection conn;
	
	private InputStream is;
	
	private OutputStream os;
	
	private long connectionID;
	
	private Authenticator authenticator;
	
	int mtu = OBEXOperationCodes.OBEX_DEFAULT_MTU;
	
	private boolean isConnected;
	
	private Operation operation;
	
	public OBEXClientSessionImpl(StreamConnection conn) throws IOException {
		this.conn = conn;
		this.os = conn.openOutputStream();
		this.is = conn.openInputStream();
		this.connectionID = -1;
		this.isConnected = false;
		this.operation = null;
	}

	public HeaderSet createHeaderSet() {
		return new OBEXHeaderSetImpl();
	}

	public HeaderSet connect(HeaderSet headers) throws IOException {
		byte[] connectRequest = new byte[4];
		connectRequest[0] = OBEXOperationCodes.OBEX_VERSION;
		connectRequest[1] = 0; /* Flags */
		connectRequest[2] = Utils.hiByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		connectRequest[3] = Utils.loByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		writeOperation(OBEXOperationCodes.CONNECT | OBEXOperationCodes.FINAL_BIT, connectRequest, OBEXHeaderSetImpl.toByteArray(headers));
		
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
		writeOperation(OBEXOperationCodes.DISCONNECT | OBEXOperationCodes.FINAL_BIT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		this.isConnected = false;
		return OBEXHeaderSetImpl.read(b[0], b, 3);
	}

	public void setConnectionID(long id) {
		if (id < 0 || id > 0xffffffffl) {
			throw new IllegalArgumentException();
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
		throw new NotImplementedIOException();
	}

	public Operation put(HeaderSet headers) throws IOException {
		if (!isConnected) {
            throw new IOException("Session not connected");
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
		
		writeOperation(OBEXOperationCodes.PUT, OBEXHeaderSetImpl.toByteArray(headers));
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
		if (this.is != null) {
			this.is.close();
			this.is = null;
		}
		if (this.os != null) {
			this.os.close();
			this.os = null;
		}
		if (this.conn != null) {
			this.conn.close();
			this.conn = null;
		}
	}

	void writeOperation(int commId, byte[] data) throws IOException {
		writeOperation(commId, data, null);
	}
	
	void writeOperation(int commId, byte[] data1, byte[] data2) throws IOException {
		int len = 3;
		if (this.connectionID != -1) {
			len += 5;
		}
		if (data1 != null) {
			len += data1.length;
		}
		if (data2 != null) {
			len += data2.length;
		}
		if (len > mtu) {
			 throw new IOException("Can't sent more data than in MTU, len=" + len + ", mtu="+ mtu);
		}
		OBEXHeaderSetImpl.writeObexLen(os, commId, len);
		if (this.connectionID != -1) {
			OBEXHeaderSetImpl.writeObexInt(os, OBEXHeaderSetImpl.OBEX_HDR_CONNECTION, this.connectionID);
		}
		if (data1 != null) {
			os.write(data1);
		}
		if (data2 != null) {
			os.write(data2, 0, data2.length);
		}
		os.flush();
		DebugLog.debug0x("obex sent", commId);
		DebugLog.debug("obex sent len", len);
	}
	
	protected byte[] readOperation() throws IOException {
		byte[] header = new byte[3];
		Utils.readFully(is, header);
		DebugLog.debug0x("obex received", header[0] & 0xFF);
		int lenght = Utils.bytesToShort(header[1], header[2]);
		byte[] data = new byte[lenght];
		System.arraycopy(header, 0, data, 0, header.length);
		Utils.readFully(is, data, header.length, lenght - header.length);
		if (is.available() > 0) {
			DebugLog.debug("has more data after read", is.available());
		}
		return data;
	}



}
