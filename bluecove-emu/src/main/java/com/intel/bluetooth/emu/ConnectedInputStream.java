/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 *
 *
 */
class ConnectedInputStream extends InputStream {

	/**
	 * The circular buffer which receives data.
	 */
	private byte buffer[];

	private boolean closed = false;

	private boolean receiverClosed = false;

	/**
	 * The index of the position in the circular buffer at which the byte of
	 * data will be stored.
	 */
	private int write = 0;

	/**
	 * The index of the position in the circular buffer from which the next byte
	 * of data will be read.
	 */
	private int read = 0;

	private int available = 0;

	public ConnectedInputStream(int size) {
		buffer = new byte[size];
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.InputStream#read()
	 */
	@Override
	public synchronized int read() throws IOException {
		while (available == 0) {
			if (closed) {
				throw new IOException("Stream closed");
			}
			if (receiverClosed) {
				// EOF
				return -1;
			}
			// Let the receive run
			notifyAll();
			try {
				wait(1000);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
		int r = buffer[read++] & 0xFF;
		if (read >= buffer.length) {
			read = 0;
		}
		available--;
		notifyAll();
		return r;
	}

	/**
	 * Reads up to <code>len</code> bytes of data from this input stream into
	 * an array of bytes. Less than <code>len</code> bytes will be read if the
	 * end of the data stream is reached. This method blocks until at least one
	 * byte of input is available.
	 */
	@Override
	public synchronized int read(byte b[], int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > b.length) {
			throw new IndexOutOfBoundsException();
		}
		if (len == 0) {
			if ((closed) && (available == 0)) {
				throw new IOException("Stream closed");
			}
			return 0;
		}
		// wait only on first byte
		int c = read();
		if (c < 0) {
			return -1;
		}
		b[off] = (byte) (c & 0xFF);
		int rlen = 1;
		while ((available > 0) && (--len > 0)) {
			b[off + rlen] = buffer[read++];
			rlen++;
			if (read >= buffer.length) {
				read = 0;
			}
			available--;
			notifyAll();
		}
		return rlen;
	}

	public synchronized int available() throws IOException {
		return available;
	}

	/**
     * Block sender till client reads all.
     */
	void receiveFlush() throws IOException {
	    receiveFlushBlock();
	}
	
	void receiveFlushBlock() throws IOException {
	    while (available != 0) {
	        if (closed) {
                throw new IOException("Stream closed");
            }
            if (receiverClosed) {
                throw new IOException("Connection closed");
            }
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
	    }
	}
	
	synchronized void receive(int b) throws IOException {
		if (closed) {
			throw new IOException("Connection closed");
		}
		if (available == buffer.length) {
			waitFreeBuffer();
		}
		buffer[write++] = (byte) (b & 0xFF);
		if (write >= buffer.length) {
			write = 0;
		}
		available++;
		notifyAll();
	}

	synchronized public void receive(byte b[], int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			if (closed) {
				throw new IOException("Connection closed");
			}
			if (available == buffer.length) {
				waitFreeBuffer();
			}
			buffer[write++] = b[off + i];
			if (write >= buffer.length) {
				write = 0;
			}
			available++;
		}
		notifyAll();
	}

	private void waitFreeBuffer() throws IOException {
		while (available == buffer.length) {
			if (receiverClosed || closed) {
				throw new IOException("Receiver closed");
			}
			// Let the read run
			notifyAll();
			try {
				wait(1000);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
	}

	synchronized void receiverClose() throws IOException {
		receiverClosed = true;
		notifyAll();
	}

	@Override
	public synchronized void close() throws IOException {
		closed = true;
		notifyAll();
	}
}
