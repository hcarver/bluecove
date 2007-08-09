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

/**
 * @author vlads
 *
 */
class OBEXClientOperationGet extends OBEXClientOperation implements OBEXOperationReceive {

	private OBEXOperationInputStream inputStream;
	
	private boolean inputStreamOpened = false;
	
	OBEXClientOperationGet(OBEXClientSessionImpl session, HeaderSet replyHeaders) throws IOException {
		super(session, replyHeaders);
		this.inputStream = new OBEXOperationInputStream(this);
		processData(replyHeaders, inputStream);
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.InputConnection#openInputStream()
	 */
	public InputStream openInputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		if (inputStreamOpened) {
            throw new IOException("input stream already open");
		}
		DebugLog.debug("openInputStream");
		inputStreamOpened = true;
		return inputStream;
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
		writeAbort();
	}
	
	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		inputStream.close();
		super.close();
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.OutputConnection#openOutputStream()
	 */
	public OutputStream openOutputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		return new UnsupportedOutputStream();
	}
	
	public void receiveData(OBEXOperationInputStream is) throws IOException {
		session.writeOperation(OBEXOperationCodes.GET | OBEXOperationCodes.FINAL_BIT, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		byte[] b = session.readOperation();
		HeaderSet dataHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		switch (dataHeaders.getResponseCode()) {
		case OBEXOperationCodes.OBEX_RESPONSE_CONTINUE:
			processData(dataHeaders, is);
			break;
		case OBEXOperationCodes.OBEX_RESPONSE_SUCCESS:
			processData(dataHeaders, is);
			replyHeaders = dataHeaders;
			close();
			break;
		default:
			throw new IOException("Operation error");
		}
	}
	
	private boolean processData(HeaderSet requestHeaders, OBEXOperationInputStream is) throws IOException {
		byte[] data = (byte[])requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END);
		if (data == null) {
			data = (byte[])requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY);
		}
		if ((data != null) && (data.length != 0)) {
			DebugLog.debug("processData len", data.length);
			is.appendData(data);
			return true;
		} else {
			return false;
		}
	}

}
