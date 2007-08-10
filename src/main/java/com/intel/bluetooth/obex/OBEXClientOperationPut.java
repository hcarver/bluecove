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
import java.io.InputStream;
import java.io.OutputStream;

import javax.obex.HeaderSet;

import com.intel.bluetooth.DebugLog;

class OBEXClientOperationPut extends OBEXClientOperation implements OBEXOperationDelivery {

	protected OBEXOperationOutputStream os;
	
	private boolean outputStreamOpened = false;
	
	OBEXClientOperationPut(OBEXClientSessionImpl session, HeaderSet sendHeaders) throws IOException {
		super(session, null);
		this.sendHeaders = sendHeaders;
	}

	public InputStream openInputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		return new UnsupportedInputStream();
	}
	
	void started() throws IOException {
		if ((!outputStreamOpened) && (!operationStarted)) {
			this.replyHeaders = session.deleteImp(sendHeaders);
			operationStarted = true;
		}
	}
	
	private void startPutOperation() throws IOException {
		session.writeOperation(OBEXOperationCodes.PUT, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		sendHeaders = null;
		byte[] b = session.readOperation();
		this.replyHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		DebugLog.debug0x("PUT got reply", replyHeaders.getResponseCode());
		this.operationInProgress = true;
	}
	
	public OutputStream openOutputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		if (outputStreamOpened) {
            throw new IOException("output already open");
		}
		outputStreamOpened = true;
		startPutOperation();
		os = new OBEXOperationOutputStream(session.mtu, this);
		return os;
	}

	public void sendHeaders(HeaderSet headers) throws IOException {
		super.sendHeaders(headers);
		if (os != null) {
			os.deliverBuffer(false);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.obex.OBEXOperationDelivery#deliverPacket(boolean, byte[])
	 */
	public void deliverPacket(boolean finalPacket, byte buffer[]) throws IOException {
		int commId = OBEXOperationCodes.PUT;
		int dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY; 
		if (finalPacket) {
			commId |= OBEXOperationCodes.FINAL_BIT;
			dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY_END;
		}
		HeaderSet dataHeaders = session.createHeaderSet();
		dataHeaders.setHeader(dataHeaderID, buffer);
		
		if ((sendHeadersLength + buffer.length + OBEXOperationCodes.OBEX_MTU_HEADER_RESERVE) > session.mtu) {
			session.writeOperation(commId, OBEXHeaderSetImpl.toByteArray(dataHeaders));
		} else {
			session.writeOperation(commId, OBEXHeaderSetImpl.toByteArray(sendHeaders), OBEXHeaderSetImpl.toByteArray(dataHeaders));
			sendHeaders = null;
			sendHeadersLength = 0;
		}
		byte[] b = session.readOperation();
		replyHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
		DebugLog.debug0x("PUT server reply", replyHeaders.getResponseCode());
		switch(replyHeaders.getResponseCode()) {
			case OBEXOperationCodes.OBEX_RESPONSE_SUCCESS:
				this.operationInProgress = false;
				break;
			case OBEXOperationCodes.OBEX_RESPONSE_CONTINUE:
				break;
			default: throw new IOException ("Can't continue connection, 0x" + Integer.toHexString(replyHeaders.getResponseCode()));
		}
	}

	/* (non-Javadoc)
	 * @see javax.obex.Operation#abort()
	 */
	public void abort() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		if (!this.operationInProgress) {
			return;
		}
		synchronized (lock) {
			if (os != null) {
				os.abort();
			}
		}
		writeAbort();
	}
	
	public void closeStream() throws IOException {
		if (os != null) {
			synchronized (lock) {
				if (os != null) {
					os.close();
				}
				os = null;
			}
		}
	}

}
