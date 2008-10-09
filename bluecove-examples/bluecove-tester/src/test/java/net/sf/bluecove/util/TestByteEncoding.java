/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.util;

import junit.framework.TestCase;

/**
 * @author vlads
 * 
 */
public class TestByteEncoding extends TestCase {

	private void validateLong(long l) {
		byte[] b = new byte[8];
		IOUtils.long2Bytes(l, 8, b, 0);
		long l2 = IOUtils.bytes2Long(b, 0, 8);
		assertEquals("bytes of " + l, l, l2);
	}

	public void testLongToBytes() {
		validateLong(1);
		validateLong(-1);
		validateLong(0x0100);
		validateLong(-0x0100);
		validateLong(0xF000);
		validateLong(0xF10000);
		validateLong(0xF1000000);
		validateLong(0xBCfffffAffl);
	}
}
