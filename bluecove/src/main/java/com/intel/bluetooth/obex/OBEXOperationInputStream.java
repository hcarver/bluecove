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
import java.io.InputStream;

class OBEXOperationInputStream extends InputStream {

	private final OBEXOperation operation;

	OBEXOperationInputStream(OBEXOperation op) {
		this.operation = op;
	}

	private byte[] buffer = new byte[0x100];

	private int readPos = 0;

	private int appendPos = 0;

	private Object lock = new Object();

	private boolean isClosed = false;

	private boolean eofReceived = false;

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (isClosed) {
			throw new IOException("Stream closed");
		}
		if (this.operation.isClosed() && (appendPos == readPos)) {
			return -1;
		}
		synchronized (lock) {
			while (!eofReceived && (this.operation instanceof OBEXOperationReceive) && !isClosed
					&& (!this.operation.isClosed()) && (appendPos == readPos)) {
				((OBEXOperationReceive) this.operation).receiveData(this);
			}
			if (appendPos == readPos) {
				return -1;
			}
			return buffer[readPos++] & 0xFF;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		synchronized (lock) {
			return (appendPos - readPos);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		isClosed = true;
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	void appendData(byte[] b, boolean eof) {
		if (isClosed || eofReceived) {
			return;
		}
		synchronized (lock) {
			if (eof) {
				eofReceived = true;
			}
			if ((b != null) && (b.length != 0)) {
				if (appendPos + b.length > buffer.length) {
					int newSize = (b.length + (appendPos - readPos)) * 2;
					if (newSize < buffer.length) {
						newSize = buffer.length;
					}
					byte[] newBuffer = new byte[newSize];
					System.arraycopy(buffer, readPos, newBuffer, 0, appendPos - readPos);
					buffer = newBuffer;
					appendPos -= readPos;
					readPos = 0;
				}
				System.arraycopy(b, 0, buffer, appendPos, b.length);
				appendPos += b.length;
			}
			lock.notifyAll();
		}
	}
}