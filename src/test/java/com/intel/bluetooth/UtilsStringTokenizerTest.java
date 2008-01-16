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
package com.intel.bluetooth;

import java.util.StringTokenizer;

import junit.framework.TestCase;

public class UtilsStringTokenizerTest extends TestCase {
	
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
