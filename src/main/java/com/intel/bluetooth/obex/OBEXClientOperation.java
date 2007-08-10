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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.obex.HeaderSet;
import javax.obex.Operation;

import com.intel.bluetooth.NotImplementedError;

abstract class OBEXClientOperation implements Operation {

	protected OBEXClientSessionImpl session;
	
	protected HeaderSet replyHeaders;
	
	protected HeaderSet sendHeaders;
	
	protected int sendHeadersLength = 0;
	
	protected boolean isClosed;
	
	protected boolean operationInProgress;
	
	protected boolean operationStarted;
	
	protected Object lock;
	
	OBEXClientOperation(OBEXClientSessionImpl session, HeaderSet replyHeaders) throws IOException {
		this.session = session;
		this.replyHeaders = replyHeaders;
		this.isClosed = false;
		this.lock = new Object();
		if (replyHeaders != null) {
			switch (replyHeaders.getResponseCode()) {
			case OBEXOperationCodes.OBEX_RESPONSE_SUCCESS:
			case OBEXOperationCodes.OBEX_RESPONSE_CONTINUE:
				this.operationInProgress = true;
				break;
			default:
				this.operationInProgress = false;
			}
		} else {
			this.operationInProgress = false;
		}
	}
	
	protected void writeAbort() throws IOException {
		try {
			session.writeOperation(OBEXOperationCodes.ABORT, null);
			byte[] b = session.readOperation();
			HeaderSet dataHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
			if (dataHeaders.getResponseCode() != OBEXOperationCodes.OBEX_RESPONSE_SUCCESS) {
				throw new IOException("Fails to abort operation");
			}
		} finally {
			closeStream();
		}
	}

	abstract void started() throws IOException;
	
	abstract void closeStream() throws IOException;
	
	public HeaderSet getReceivedHeaders() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		started();
		return OBEXHeaderSetImpl.cloneHeaders(this.replyHeaders);
	}

	/* (non-Javadoc)
	 * @see javax.obex.Operation#getResponseCode()
	 * 
	 *  A call will do an implicit close on the Stream and therefore signal that the request is done.
	 */
	public int getResponseCode() throws IOException {
		started();
		closeStream();
		return this.replyHeaders.getResponseCode();
	}

	public void sendHeaders(HeaderSet headers) throws IOException {
		synchronized (lock) {
			sendHeaders = headers;
			sendHeadersLength = OBEXHeaderSetImpl.toByteArray(sendHeaders).length;
		}
	}

	public String getEncoding() {
		// TODO implement
		throw new NotImplementedError();
	}

	public long getLength() {
		// TODO implement
		throw new NotImplementedError();
	}

	public String getType() {
		// TODO implement
		throw new NotImplementedError();
	}

	public DataInputStream openDataInputStream() throws IOException {
		 return new DataInputStream(openInputStream());
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		started();
		closeStream();
		this.isClosed = true;
	}

	public boolean isClosed() {
		return this.isClosed;
	}
}
