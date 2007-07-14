/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package com.intel.bluetooth;

import java.io.IOException;
import java.io.OutputStream;

class BluetoothOutputStream extends OutputStream {
	
	volatile private BluetoothRFCommConnection conn;

	public BluetoothOutputStream(BluetoothRFCommConnection conn) {
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
			BlueCoveImpl.instance().getBluetoothStack().connectionRfWrite(conn.handle, b);
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
			BlueCoveImpl.instance().getBluetoothStack().connectionRfWrite(conn.handle, b, off, len);
		}
	}

	public void flush() throws IOException {
		if (conn == null) {
			throw new IOException("Stream closed");
		} else {
			super.flush();
			BlueCoveImpl.instance().getBluetoothStack().connectionRfFlush(conn.handle);
		}
    }
	
	public void close() throws IOException {
		try {
			if (conn != null) {
				conn.out = null;
				BluetoothRFCommConnection c = conn;
				conn = null;
				// Function is not synchronized
				if (c != null) {
					c.closeConnection();
				}
			}
		} catch (NullPointerException ignore) {
			// Function is not synchronized
		}
	}
}