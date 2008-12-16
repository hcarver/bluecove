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

class OBEXClientOperationPut extends OBEXClientOperation {

	OBEXClientOperationPut(OBEXClientSessionImpl session, HeaderSet sendHeaders) throws IOException {
		super(session, OBEXOperationCodes.PUT, sendHeaders);
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
		this.operationInProgress = true;
		return this.inputStream;
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
		this.outputStreamOpened = true;
		this.outputStream = new OBEXOperationOutputStream(session.mtu, this);
		this.operationInProgress = true;
		return this.outputStream;
	}

}
