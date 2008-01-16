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
		deliverBuffer(false);
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