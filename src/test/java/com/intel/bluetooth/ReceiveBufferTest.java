/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import java.util.Random;

import junit.framework.TestCase;

/**
 * Test FIFO buffer implemented in C++.
 * 
 * @author vlads
 *
 */
public class ReceiveBufferTest extends TestCase {
	
	final static int TEST_BUFFER_SIZE = 4 * 2 * 10;
	
	Random rnd = new Random();
	
	long bufferHandler = 0; 
	
	protected void setUp() throws Exception {
		super.setUp();
		System.getProperties().put("bluecove.native.path", "./src/main/resources");
		bufferHandler = BluetoothPeer.testReceiveBufferCreate(TEST_BUFFER_SIZE);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		if (bufferHandler != 0) {
			if (BluetoothPeer.testReceiveBufferIsCorrupted(bufferHandler)) {
				System.err.println("Buffer IsCorrupted");
				System.exit(1);
			}
			BluetoothPeer.testReceiveBufferClose(bufferHandler);
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
		assertEquals("write", expectedWritten, BluetoothPeer.testReceiveBufferWrite(bufferHandler, data));
		assertEquals("available", expectedWritten, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
		return data;
	}
	
	private void verifyRead(int readSize, int expectedReadSize, byte expectedReadData[], int off) {
		byte rcv[] = new byte[readSize];
		int recieved = BluetoothPeer.testReceiveBufferRead(bufferHandler, rcv);
		assertEquals("recieved len from buffer", expectedReadSize, recieved);
		for (int i = 0; i < recieved; i++) {
			assertEquals("recieved data buffer["+i+"]", rcv[i], expectedReadData[i + off]);			
		}
	}
	private void verifyWriteRead(int writeSize, int expectedWritten, int readSize, int expectedReadSize) {
		byte data[] = verifyWrite(writeSize, expectedWritten);
		verifyRead(readSize, expectedReadSize, data, 0);
		assertEquals("available", 0, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
		assertEquals("read not available", -1, BluetoothPeer.testReceiveBufferRead(bufferHandler));
	}
	
	public void testWriteReadSimple() {
		verifyWriteRead(7, 7, 7, 7);
	}
	
	public void testWriteReadOverflow() {
		verifyWriteRead(TEST_BUFFER_SIZE * 2, TEST_BUFFER_SIZE, TEST_BUFFER_SIZE * 2, TEST_BUFFER_SIZE);
		assertEquals("IsOverflown", true, BluetoothPeer.testReceiveBufferIsOverflown(bufferHandler));
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
			int writen = BluetoothPeer.testReceiveBufferWrite(bufferHandler, new byte[] { data[i] });
			assertEquals("writen["+i+"]", 1,  writen);
			assertEquals("available["+i+"]", 1, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
			int b = BluetoothPeer.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+i+"]", data[i], (byte)b);
			assertEquals("available["+i+"]", 0, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
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
			int writen = BluetoothPeer.testReceiveBufferWrite(bufferHandler, new byte[] { data[w] });
			assertEquals("writen["+w+"]", 1,  writen);
			assertEquals("available["+w+"]", w + 1, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
		}
		int r;
		for (r = 0; r < size - shift; r++) {
			int b = BluetoothPeer.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+r+"]", data[r], (byte)b);
			assertEquals("available shift["+r+"]", shift - 1, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));

			w = r + shift;
			int writen = BluetoothPeer.testReceiveBufferWrite(bufferHandler, new byte[] { data[w] });
			assertEquals("writen["+w+"]", 1,  writen);
			assertEquals("available["+w+"]", shift, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
		}
		for (; r < size; r++) {
			int b = BluetoothPeer.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+r+"]", data[r], (byte)b);
			assertEquals("available shift["+r+"]", size - r - 1, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
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
			assertEquals("available ["+i+"]", size * (parts -i), BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
			verifyRead(size, size, data, size * i);
		}
		assertEquals("available", 0, BluetoothPeer.testReceiveBufferAvailable(bufferHandler));
		for (int i = 0; i < 5; i++) {
			verifyWriteRead(size, size, size, size);
		}
	}
	
	public void testSkip() {
		final int size = TEST_BUFFER_SIZE;
		byte data[] = verifyWrite(size, size);
		verifyRead(10, 10, data, 0);
		assertEquals("skip 5", 5, BluetoothPeer.testReceiveBufferSkip(bufferHandler, 5));
		verifyRead(20, 20, data, 10 + 5);
	}

	public void testWriteThread() {
		final int size = TEST_BUFFER_SIZE * 7;
		final byte send[] = createData(size);
		
		Thread t = new Thread() {
			public void run() {
				for (int i = 0; i < size; i++) {
					int writen = BluetoothPeer.testReceiveBufferWrite(bufferHandler, new byte[]{send[i]});
					if (writen != 1) {
						return;
					}
					while (BluetoothPeer.testReceiveBufferAvailable(bufferHandler) > TEST_BUFFER_SIZE / 2) {
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
			while (BluetoothPeer.testReceiveBufferAvailable(bufferHandler) == 0) {
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
			int data = BluetoothPeer.testReceiveBufferRead(bufferHandler);
			assertEquals("recieved data buffer["+i+"]", send[i], (byte)data);
		}
		assertEquals("recieved all", size, i);
	}
	
}
