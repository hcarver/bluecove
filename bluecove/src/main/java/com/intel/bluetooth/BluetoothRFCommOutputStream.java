/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package com.intel.bluetooth;

import java.io.IOException;
import java.io.OutputStream;

class BluetoothRFCommOutputStream extends OutputStream {

	volatile private BluetoothRFCommConnection conn;

	public BluetoothRFCommOutputStream(BluetoothRFCommConnection conn) {
		this.conn = conn;
	}

	/*
	 * Writes the specified byte to this output stream. The general contract for
	 * write is that one byte is written to the output stream. The byte to be
	 * written is the eight low-order bits of the argument b. The 24 high-order
	 * bits of b are ignored. Subclasses of OutputStream must provide an
	 * implementation for this method.
	 *
	 * Parameters: b - the byte. Throws: IOException - if an I/O error occurs.
	 * In particular, an IOException may be thrown if the output stream has been
	 * closed.
	 */

	public void write(int b) throws IOException {
		if (conn == null) {
			throw new IOException("Stream closed");
		} else {
			conn.bluetoothStack.connectionRfWrite(conn.handle, b);
		}
	}

	/*
	 * Writes len bytes from the specified byte array starting at offset off to
	 * this output stream. The general contract for write(b, off, len) is that
	 * some of the bytes in the array b are written to the output stream in
	 * order; element b[off] is the first byte written and b[off+len-1] is the
	 * last byte written by this operation. The write method of OutputStream
	 * calls the write method of one argument on each of the bytes to be written
	 * out. Subclasses are encouraged to override this method and provide a more
	 * efficient implementation.
	 *
	 * If b is null, a NullPointerException is thrown.
	 *
	 * If off is negative, or len is negative, or off+len is greater than the
	 * length of the array b, then an IndexOutOfBoundsException is thrown.
	 *
	 * Parameters: b - the data. off - the start offset in the data. len - the
	 * number of bytes to write. Throws: IOException - if an I/O error occurs.
	 * In particular, an IOException is thrown if the output stream is closed.
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > b.length) {
			throw new IndexOutOfBoundsException();
		}

		if (conn == null) {
			throw new IOException("Stream closed");
		} else {
			conn.bluetoothStack.connectionRfWrite(conn.handle, b, off, len);
		}
	}

	public void flush() throws IOException {
		if (conn == null) {
			throw new IOException("Stream closed");
		} else {
			super.flush();
			conn.bluetoothStack.connectionRfFlush(conn.handle);
		}
    }

	/**
	 * Closes this output stream and releases any system resources associated
	 * with this stream.
	 * <p>
	 * The general contract of close is that it closes the output stream. A
	 * closed stream cannot perform output operations and cannot be reopened.
	 *
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public void close() throws IOException {
		// Function is not synchronized
		BluetoothRFCommConnection c = conn;
		if (c != null) {
			conn = null;
			c.streamClosed();
		}
	}

	boolean isClosed() {
		return this.conn == null;
	}
}