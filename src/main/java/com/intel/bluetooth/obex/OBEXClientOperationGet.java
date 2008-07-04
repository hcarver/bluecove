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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.obex.HeaderSet;

import com.intel.bluetooth.DebugLog;

/**
 * @author vlads
 * 
 */
class OBEXClientOperationGet extends OBEXClientOperation implements OBEXOperationReceive, OBEXOperationDelivery {

	OBEXClientOperationGet(OBEXClientSessionImpl session, HeaderSet sendHeaders) throws IOException {
		super(session, OBEXOperationCodes.GET);
		this.inputStream = new OBEXOperationInputStream(this);
		startOperation(sendHeaders);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.InputConnection#openInputStream()
	 */
	public InputStream openInputStream() throws IOException {
		validateOperationIsOpen();
		if (this.inputStreamOpened) {
			throw new IOException("input stream already open");
		}
		DebugLog.debug("openInputStream");
		this.inputStreamOpened = true;
		endRequestPhase();
		return this.inputStream;
	}

	public void closeStream() throws IOException {
		this.operationInProgress = false;
		// try {
		// while (!isClosed() && (!finalBodyReceived) && (!errorReceived)) {
		// receiveData(this.inputStream);
		// }
		// } finally {
		inputStream.close();
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.io.OutputConnection#openOutputStream()
	 */
	public OutputStream openOutputStream() throws IOException {
		validateOperationIsOpen();
		if (outputStreamOpened) {
			throw new IOException("output already open");
		}
		if (this.requestEnded) {
			throw new IOException("the request phase has already ended");
		}
		this.outputStreamOpened = true;
		this.outputStream = new OBEXOperationOutputStream(session.mtu, this);
		return this.outputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationReceive#receiveData(com.intel.bluetooth.obex.OBEXOperationInputStream)
	 */
	public void receiveData(OBEXOperationInputStream is) throws IOException {
		if (SHORT_REQUEST_PHASE) {
			exchangePacket(OBEXHeaderSetImpl.toByteArray(this.startOperationHeaders));
			this.startOperationHeaders = null;
		} else {
			exchangePacket(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.obex.OBEXOperationDelivery#deliverPacket(boolean,
	 *      byte[])
	 */
	public void deliverPacket(boolean finalPacket, byte[] buffer) throws IOException {
		if (requestEnded) {
			return;
		}
		if (SHORT_REQUEST_PHASE && (this.startOperationHeaders != null)) {
			exchangePacket(OBEXHeaderSetImpl.toByteArray(this.startOperationHeaders));
			this.startOperationHeaders = null;
		}
		int dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY;
		if (finalPacket) {
			this.operationId |= OBEXOperationCodes.FINAL_BIT;
			dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY_END;
			requestEnded = true;
		}
		HeaderSet dataHeaders = session.createHeaderSet();
		dataHeaders.setHeader(dataHeaderID, buffer);
		exchangePacket(OBEXHeaderSetImpl.toByteArray(dataHeaders));
	}

}
