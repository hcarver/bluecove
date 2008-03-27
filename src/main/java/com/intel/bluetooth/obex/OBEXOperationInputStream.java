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

class OBEXOperationInputStream extends InputStream {

	private final OBEXOperationReceive operation;

	OBEXOperationInputStream(OBEXOperationReceive op) {
		this.operation = op;
	}

	private byte[] buffer = new byte[0x100];

	private int readPos = 0;

	private int appendPos = 0;

	private Object lock = new Object();

	private boolean isClosed = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (this.operation.isClosed() && (appendPos == readPos)) {
			return -1;
		}
		synchronized (lock) {
			while (!isClosed && (!this.operation.isClosed()) && (appendPos == readPos)) {
				try {
					this.operation.receiveData(this);
				} catch (IOException e) {
					if (!this.operation.isClosed()) {
						throw e;
					}
				}
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

	void appendData(byte[] b) {
		if (isClosed) {
			return;
		}
		synchronized (lock) {
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

			lock.notifyAll();
		}
	}
}