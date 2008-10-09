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
 *  @version $Id$
 */
package com.intel.bluetooth.obex;

import java.io.IOException;
import java.io.OutputStream;

class OBEXOperationOutputStream extends OutputStream {

	private final OBEXOperationDelivery operation;

	private byte[] buffer;

	private int bufferLength;

	private Object lock = new Object();

	private boolean isClosed = false;

	OBEXOperationOutputStream(int mtu, OBEXOperationDelivery op) {
		this.operation = op;
		buffer = new byte[mtu - OBEXOperationCodes.OBEX_MTU_HEADER_RESERVE];
		bufferLength = 0;
	}

	public void write(int i) throws IOException {
		write(new byte[] { (byte) i }, 0, 1);
	}

	public void write(byte b[], int off, int len) throws IOException {
		if (this.operation.isClosed() || isClosed) {
			throw new IOException("stream closed");
		}
		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (len < 0) || ((off + len) > b.length)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}

		synchronized (lock) {
			int written = 0;
			while (written < len) {
				int available = (buffer.length - bufferLength);
				if ((len - written) < available) {
					available = len - written;
				}
				System.arraycopy(b, off + written, buffer, bufferLength, available);
				bufferLength += available;
				written += available;
				if (bufferLength == buffer.length) {
					this.operation.deliverPacket(false, buffer);
					bufferLength = 0;
				}
			}
		}
	}

	public void flush() throws IOException {
		if (bufferLength > 0) {
			deliverBuffer(false);
		}
	}

	void deliverBuffer(boolean finalPacket) throws IOException {
		synchronized (lock) {
			byte[] b = new byte[bufferLength];
			System.arraycopy(buffer, 0, b, 0, bufferLength);
			this.operation.deliverPacket(finalPacket, b);
			bufferLength = 0;
		}
	}

	void abort() {
		synchronized (lock) {
			isClosed = true;
		}
	}

	public void close() throws IOException {
		if (!isClosed) {
			synchronized (lock) {
				isClosed = true;
				if (!operation.isClosed()) {
					deliverBuffer(true);
				}
			}
		}
	}

}