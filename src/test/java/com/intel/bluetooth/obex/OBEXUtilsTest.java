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
package com.intel.bluetooth.obex;

import junit.framework.TestCase;

public class OBEXUtilsTest extends TestCase {
	
	public void testBytesLoHi() {
		assertEquals("loByte", (byte)0x77, OBEXUtils.loByte(0x1077));
		assertEquals("loByte", (byte)0x81, OBEXUtils.loByte(0x1081));
		
		assertEquals("hiByte", (byte)0x10, OBEXUtils.hiByte(0x1077));
		assertEquals("hiByte", (byte)0x95, OBEXUtils.hiByte(0x9581));
	}
	
	private void validateBytesToShort(int i) {
		byte[] b = new byte[2];
		b[0] = OBEXUtils.hiByte(i);
		b[1] = OBEXUtils.loByte(i);
		int r = OBEXUtils.bytesToShort(b[0], b[1]);
		assertEquals("short from bytes", i, r);
	}
	
	public void testBytesToShort() {
		validateBytesToShort(0x0A);
		validateBytesToShort(0x80);
		validateBytesToShort(0xF7);
		validateBytesToShort(0x0100);
		validateBytesToShort(0x0102);
		validateBytesToShort(0xFB00);
		validateBytesToShort(0xBCEF);
	}

}
