/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.util.Random;

/**
 * Test FIFO buffer implemented in C++.
 *
 */
public class NativeReceiveBufferTest extends NativeTestCase {
	
	final static int TEST_BUFFER_SIZE = 4 * 2 * 10;
	
	Random rnd = new Random();
	
	long bufferHandler = 0; 
	
	protected void setUp() throws Exception {
		super.setUp();
		bufferHandler = NativeTestInterfaces.testReceiveBufferCreate(TEST_BUFFER_SIZE);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		if (bufferHandler != 0) {
			if (NativeTestInterfaces.testReceiveBufferIsCorrupted(bufferHandler)) {
				System.err.println("Buffer IsCorrupted");
				System.exit(1);
			}
			NativeTestInterfaces.testReceiveBufferClose(bufferHandler);
		}
	}

	private byte[] createData(int size) {
		byte data[] = new byte[size];
		int off = rnd.nextInt(size);
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) ( (off + i) % 0xFF);
			if (i > 0) {
				assertTrue("same data", data[i-1] != data[i]);		
			}
		}
		return data;
	}
	private byte[] verifyWrite(int writeSize, int expectedWritten) {
		byte data[] = createData(writeSize);
		assertEquals("write", expectedWritten, NativeTestInterfaces.testReceiveBufferWrite(bufferHandler, data));
		assertEquals("available", expectedWritten, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		return data;
	}
	
	private void verifyRead(int readSize, int expectedReadSize, byte expectedReadData[], int off) {
		byte rcv[] = new byte[readSize];
		int recieved = NativeTestInterfaces.testReceiveBufferRead(bufferHandler, rcv);
		assertEquals("recieved len from buffer", expectedReadSize, recieved);
		for (int i = 0; i < recieved; i++) {
			assertEquals("recieved data buffer["+i+"]", rcv[i], expectedReadData[i + off]);			
		}
	}
	private void verifyWriteRead(int writeSize, int expectedWritten, int readSize, int expectedReadSize) {
		byte data[] = verifyWrite(writeSize, expectedWritten);
		verifyRead(readSize, expectedReadSize, data, 0);
		assertEquals("available", 0, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		assertEquals("read not available", -1, NativeTestInterfaces.testReceiveBufferRead(bufferHandler));
	}
	
	public void testWriteReadSimple() {
		verifyWriteRead(7, 7, 7, 7);
	}
	
	public void testWriteReadOverflow() {
		verifyWriteRead(TEST_BUFFER_SIZE * 2, TEST_BUFFER_SIZE, TEST_BUFFER_SIZE * 2, TEST_BUFFER_SIZE);
		assertEquals("IsOverflown", true, NativeTestInterfaces.testReceiveBufferIsOverflown(bufferHandler));
	}
	
	public void testWriteReadMoreThanSize() {
		int size = TEST_BUFFER_SIZE - (TEST_BUFFER_SIZE / 4);
		for (int i = 0; i < 7; i++) {
			verifyWriteRead(size, size, size, size);
		}
	}

	public void testAllBytes() {
		final int size = TEST_BUFFER_SIZE * 3;
		byte data[] = createData(size);
		for (int i = 0; i < size; i++) {
			int writen = NativeTestInterfaces.testReceiveBufferWrite(bufferHandler, new byte[] { data[i] });
			assertEquals("writen["+i+"]", 1,  writen);
			assertEquals("available["+i+"]", 1, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
			int b = NativeTestInterfaces.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+i+"]", data[i], (byte)b);
			assertEquals("available["+i+"]", 0, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		}
	}
	
	public void testWriteReadBorderConditions() {
		int size = TEST_BUFFER_SIZE;
		verifyWriteRead(size, size, size, size);
		verifyWriteRead(size, size, size, size);
		verifyWriteRead(size, size, size, size);
	}

	public void verifyAllBytesBorderConditions(int shift) {
		final int size = TEST_BUFFER_SIZE * 3;
		byte data[] = createData(size);
		int w;
		for (w = 0; w < shift; w++) {
			int writen = NativeTestInterfaces.testReceiveBufferWrite(bufferHandler, new byte[] { data[w] });
			assertEquals("writen["+w+"]", 1,  writen);
			assertEquals("available["+w+"]", w + 1, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		}
		int r;
		for (r = 0; r < size - shift; r++) {
			int b = NativeTestInterfaces.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+r+"]", data[r], (byte)b);
			assertEquals("available shift["+r+"]", shift - 1, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));

			w = r + shift;
			int writen = NativeTestInterfaces.testReceiveBufferWrite(bufferHandler, new byte[] { data[w] });
			assertEquals("writen["+w+"]", 1,  writen);
			assertEquals("available["+w+"]", shift, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		}
		for (; r < size; r++) {
			int b = NativeTestInterfaces.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+r+"]", data[r], (byte)b);
			assertEquals("available shift["+r+"]", size - r - 1, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		}
	}
	
	public void testAllBytesBorderConditions() {
		verifyAllBytesBorderConditions(TEST_BUFFER_SIZE / 2);
		verifyAllBytesBorderConditions(TEST_BUFFER_SIZE / 4);
		verifyAllBytesBorderConditions(3 * TEST_BUFFER_SIZE / 4);
	}

	public void testWriteReadBorderConditions2() {
		byte data[] = verifyWrite(TEST_BUFFER_SIZE, TEST_BUFFER_SIZE);
		int parts = 8;
		int size = TEST_BUFFER_SIZE / parts;
		for (int i = 0; i < parts; i++) {
			assertEquals("available ["+i+"]", size * (parts -i), NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
			verifyRead(size, size, data, size * i);
		}
		assertEquals("available", 0, NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler));
		for (int i = 0; i < 5; i++) {
			verifyWriteRead(size, size, size, size);
		}
	}
	
	public void testSkip() {
		final int size = TEST_BUFFER_SIZE;
		byte data[] = verifyWrite(size, size);
		verifyRead(10, 10, data, 0);
		assertEquals("skip 5", 5, NativeTestInterfaces.testReceiveBufferSkip(bufferHandler, 5));
		verifyRead(20, 20, data, 10 + 5);
	}

	public void testWriteThread() {
		final int size = TEST_BUFFER_SIZE * 7;
		final byte send[] = createData(size);
		
		Thread t = new Thread() {
			public void run() {
				for (int i = 0; i < size; i++) {
					int writen = NativeTestInterfaces.testReceiveBufferWrite(bufferHandler, new byte[]{send[i]});
					if (writen != 1) {
						return;
					}
					while (NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler) > TEST_BUFFER_SIZE / 2) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							return;
						}
					}
				}
			}
		};
		
		t.start();

		int i;
		for (i = 0; i < size; i++) {
			int sleepCount = 0;
			while (NativeTestInterfaces.testReceiveBufferAvailable(bufferHandler) == 0) {
				if (sleepCount > 50) {
					fail("Writer not runnning");
				}
				try {
					Thread.sleep(50);
					sleepCount ++;
				} catch (InterruptedException e) {
					return;
				}
			}
			int data = NativeTestInterfaces.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+i+"]", send[i], (byte)data);
		}
		assertEquals("recieved all", size, i);
	}
	
}
