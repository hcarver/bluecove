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

import java.util.StringTokenizer;

import junit.framework.TestCase;

/**
 * @author vlads
 *
 */
public class UtilsTest extends TestCase {

	private void validateLong(long l) {
		assertEquals("hex of " + l, Long.toHexString(l), Utils.toHexString(l));
	}
			
	public void testLongtoHexString() {
		validateLong(0x0100);
		validateLong(0xBCfffffAffl);
	}
	
	private void validateBytesLoHi(int i) {
		byte[] b = new byte[2];
		b[0] = Utils.hiByte(i);
		b[1] = Utils.loByte(i);
		int r = Utils.bytesToShort(b[0], b[1]);
		assertEquals("short from bytes", i, r);
	}
	
	public void testBytesLoHi() {
		validateBytesLoHi(0x0A);
		validateBytesLoHi(0x80);
		validateBytesLoHi(0xF7);
		validateBytesLoHi(0x0100);
		validateBytesLoHi(0x0102);
		validateBytesLoHi(0xFB00);
		validateBytesLoHi(0xBCEF);
	}
	
	private void validateStringTokenizer(String str, String delimiter) {
		StringTokenizer stRef = new StringTokenizer(str, delimiter);
		UtilsStringTokenizer stImp = new UtilsStringTokenizer(str, delimiter);
		while ((stRef.hasMoreTokens()) && (stImp.hasMoreTokens())) {
			assertEquals("nextToken", stRef.nextToken(), stImp.nextToken());
		}
		assertEquals("hasMoreTokens", stRef.hasMoreTokens(), stImp.hasMoreTokens());
	}
	
	public void testStringTokenizer() {
		validateStringTokenizer("AB", ";");
		validateStringTokenizer("AB;", ";");
		validateStringTokenizer(";AB", ";");
		validateStringTokenizer(";AB;", ";");
		validateStringTokenizer("AB;CD", ";");
		validateStringTokenizer("AB;CD;EF", ";");
		validateStringTokenizer(";", ";");
	}
	
}
