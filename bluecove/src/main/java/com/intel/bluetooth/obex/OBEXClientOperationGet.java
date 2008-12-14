/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
import java.io.InputStream;
import java.io.OutputStream;

import javax.obex.HeaderSet;

import com.intel.bluetooth.DebugLog;

/**
 *
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
        try {
            while (!isClosed() && (operationInContinue) ) {
                DebugLog.debug("operation expects operation end");
                receiveData(this.inputStream);
            }
        } finally {
            this.operationInProgress = false;
            inputStream.close();
        }
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
	 * @see com.intel.bluetooth.obex.OBEXOperationDelivery#deliverPacket(boolean, byte[])
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
			DebugLog.debug("client Request Phase ended");
			requestEnded = true;
		}
		HeaderSet dataHeaders = session.createHeaderSet();
		dataHeaders.setHeader(dataHeaderID, buffer);
		exchangePacket(OBEXHeaderSetImpl.toByteArray(dataHeaders));
	}

}
