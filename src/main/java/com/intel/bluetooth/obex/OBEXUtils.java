/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * OBEX IO Utils
 * 
 * @author vlads
 *
 */
abstract class OBEXUtils {
	
	static void readFully(InputStream is, byte[] b) throws IOException, EOFException {
		readFully(is, b, 0, b.length);
	}
	
	static void readFully(InputStream is, byte[] b, int off, int len) throws IOException, EOFException {
		if (len < 0) {
		    throw new IndexOutOfBoundsException();
		}
		int got = 0;
		while (got < len) {
			int rc = is.read(b, off + got, len - got);
			if (rc < 0) {
				throw new EOFException();
			}
			got += rc;
		}
	}
	
	static byte hiByte(int value) {
		return (byte)((value >> 8) & 0xFF);
	}
	
	static byte loByte(int value) {
		return (byte)(0xFF & value);
	}

	static int bytesToShort(byte valueHi, byte valueLo) {
		return ((((int)valueHi << 8) & 0xFF00) + (valueLo & 0xFF));
	}
}
