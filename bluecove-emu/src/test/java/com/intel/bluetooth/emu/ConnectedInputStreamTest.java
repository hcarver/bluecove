/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;

import com.intel.bluetooth.emu.ConnectedInputStream;
import com.intel.bluetooth.emu.ConnectedOutputStream;

/**
 * @author vlads
 * 
 */
public class ConnectedInputStreamTest extends TestCase {

	final static int TEST_BUFFER_SIZE = 4 * 2 * 10;

	ConnectedInputStream is;

	ConnectedOutputStream os;

	Random rnd = new Random();

	protected void setUp() throws Exception {
		super.setUp();
		is = new ConnectedInputStream(TEST_BUFFER_SIZE);
		os = new ConnectedOutputStream(is);
	}

	private byte[] createData(int size) {
		byte data[] = new byte[size];
		int off = rnd.nextInt(size);
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) ((off + i) % 0xFF);
			if (i > 0) {
				assertTrue("same data", data[i - 1] != data[i]);
			}
		}
		return data;
	}

	private byte[] verifyWrite(int writeSize, int expectedAvailable) throws IOException {
		byte data[] = createData(writeSize);
		os.write(data);
		assertEquals("available", expectedAvailable, is.available());
		return data;
	}

	private void verifyRead(int readSize, int expectedReadSize, byte expectedReadData[], int off) throws IOException {
		byte rcv[] = new byte[readSize];
		int recieved = is.read(rcv);
		assertEquals("recieved len from buffer", expectedReadSize, recieved);
		for (int i = 0; i < recieved; i++) {
			assertEquals("recieved data buffer[" + i + "]", rcv[i], expectedReadData[i + off]);
		}
	}

	private void verifyWriteRead(int writeSize, int expectedWritten, int readSize, int expectedReadSize)
			throws IOException {
		byte data[] = verifyWrite(writeSize, expectedWritten);
		verifyRead(readSize, expectedReadSize, data, 0);
		assertEquals("available", 0, is.available());
	}

	public void testWriteReadSimple() throws IOException {
		verifyWriteRead(1, 1, 1, 1);
		verifyWriteRead(7, 7, 7, 7);
	}

	public void testWriteClose() throws IOException {
		verifyWriteRead(1, 1, 1, 1);
		byte data[] = verifyWrite(3, 3);
		os.close();
		verifyRead(3, 3, data, 0);
		assertEquals("EOF available", 0, is.available());
		assertEquals("EOF expected", -1, is.read());
	}

	public void testWriteReadMoreThanSize() throws IOException {
		int size = TEST_BUFFER_SIZE - (TEST_BUFFER_SIZE / 4);
		for (int i = 0; i < 7; i++) {
			verifyWriteRead(size, size, size, size);
		}
	}

	public void testAllBytes() throws IOException {
		final int size = TEST_BUFFER_SIZE * 3;
		byte data[] = createData(size);
		for (int i = 0; i < size; i++) {
			os.write(data[i]);
			assertEquals("available[" + i + "]", 1, is.available());
			int b = is.read();
			assertEquals("recieved data buffer[" + i + "]", data[i], (byte) b);
			assertEquals("available[" + i + "]", 0, is.available());
		}
	}

	public void testWriteReadBorderConditions() throws IOException {
		int size = TEST_BUFFER_SIZE;
		verifyWriteRead(size, size, size, size);
		verifyWriteRead(size, size, size, size);
		verifyWriteRead(size, size, size, size);
	}

	public void verifyAllBytesBorderConditions(int shift) throws IOException {
		final int size = TEST_BUFFER_SIZE * 3;
		byte data[] = createData(size);
		int w;
		for (w = 0; w < shift; w++) {
			os.write(data[w]);
			assertEquals("available[" + w + "]", w + 1, is.available());
		}
		int r;
		for (r = 0; r < size - shift; r++) {
			int b = is.read();
			assertEquals("recieved data buffer[" + r + "]", data[r], (byte) b);
			assertEquals("available shift[" + r + "]", shift - 1, is.available());

			w = r + shift;
			os.write(data[w]);
			assertEquals("available[" + w + "]", shift, is.available());
		}
		for (; r < size; r++) {
			int b = is.read();
			assertEquals("recieved data buffer[" + r + "]", data[r], (byte) b);
			assertEquals("available shift[" + r + "]", size - r - 1, is.available());
		}
	}

	public void testAllBytesBorderConditions() throws IOException {
		verifyAllBytesBorderConditions(TEST_BUFFER_SIZE / 2);
		verifyAllBytesBorderConditions(TEST_BUFFER_SIZE / 4);
		verifyAllBytesBorderConditions(3 * TEST_BUFFER_SIZE / 4);
	}

	public void testWriteReadBorderConditions2() throws IOException {
		byte data[] = verifyWrite(TEST_BUFFER_SIZE, TEST_BUFFER_SIZE);
		int parts = 8;
		int size = TEST_BUFFER_SIZE / parts;
		for (int i = 0; i < parts; i++) {
			assertEquals("available [" + i + "]", size * (parts - i), is.available());
			verifyRead(size, size, data, size * i);
		}
		assertEquals("available", 0, is.available());
		for (int i = 0; i < 5; i++) {
			verifyWriteRead(size, size, size, size);
		}
	}
}
