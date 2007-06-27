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

import java.io.IOException;

import javax.obex.HeaderSet;

import junit.framework.TestCase;

/**
 * @author vlads
 *
 */
public class HeaderTest extends TestCase {

	private void validateReadWrite(HeaderSet headers) throws IOException {
		byte b[] = OBEXHeaderSetImpl.toByteArray(headers);
		HeaderSet r = OBEXHeaderSetImpl.read((byte)0, b, 0);
		
		int[] headerIDArray = headers.getHeaderList();
		assertEquals("HeaderList.length", headerIDArray.length, r.getHeaderList().length);
		
		for (int i = 0; i < headerIDArray.length; i++) {
			int hi = headerIDArray[i];
			Object valueO = headers.getHeader(hi);
			Object valueR = r.getHeader(hi);
			assertNotNull("value Write", valueO);
			assertNotNull("value Read", valueR);
			if (!(valueO instanceof byte[])) {
				assertEquals("Header value", valueO, valueR);		
			} else {
				byte[] bO = (byte[]) valueO;
				byte[] bR = (byte[]) valueR;
				assertEquals("Header value.length", bO.length, bR.length);
				for (int k = 0; i < bO.length; i++) {
					assertEquals("value["+k+"]", bO[k], bR[k]);			
				}
			}
		}
	}
	
	public void testHeaderByteReadWrite() throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		hs.setHeader(OBEXHeaderSetImpl.OBEX_HDR_USER | OBEXHeaderSetImpl.OBEX_BYTE, new Byte((byte)1));
		validateReadWrite(hs);
		hs.setHeader((OBEXHeaderSetImpl.OBEX_HDR_USER + 1) | OBEXHeaderSetImpl.OBEX_BYTE, new Byte((byte)-1));
		validateReadWrite(hs);
		hs.setHeader((OBEXHeaderSetImpl.OBEX_HDR_USER + 2) | OBEXHeaderSetImpl.OBEX_BYTE, new Byte(Byte.MIN_VALUE));
		validateReadWrite(hs);
		hs.setHeader((OBEXHeaderSetImpl.OBEX_HDR_USER + 3) | OBEXHeaderSetImpl.OBEX_BYTE, new Byte(Byte.MAX_VALUE));
		validateReadWrite(hs);
	}

	public void testHeaderByteArrayReadWrite() throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		hs.setHeader(HeaderSet.HTTP, new byte[]{1, -1, 120, -7});
		validateReadWrite(hs);
		hs.setHeader(OBEXHeaderSetImpl.OBEX_HDR_USER | OBEXHeaderSetImpl.OBEX_BYTE_STREAM, new byte[]{-1, 0, -120, Byte.MAX_VALUE});
		validateReadWrite(hs);
		byte[] b = new byte[0xFAB8];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte)(i & 0xFF);
		}
		hs.setHeader(OBEXHeaderSetImpl.OBEX_HDR_USER + 1 | OBEXHeaderSetImpl.OBEX_BYTE_STREAM, b);
		validateReadWrite(hs);
	}

	public void testHeaderIntReadWrite() throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		hs.setHeader(HeaderSet.LENGTH, new Long(1));
		validateReadWrite(hs);
		hs.setHeader(HeaderSet.COUNT, new Long(0x12345678));
		validateReadWrite(hs);
		hs.setHeader(OBEXHeaderSetImpl.OBEX_HDR_USER | OBEXHeaderSetImpl.OBEX_INT, new Long(0xFAFBFCFDl));
		validateReadWrite(hs);
	}
	
	public void testHeaderStringReadWrite() throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		hs.setHeader(HeaderSet.NAME, "Jumar");
		validateReadWrite(hs);
		hs.setHeader(HeaderSet.TYPE, "text");
		validateReadWrite(hs);
		hs.setHeader(HeaderSet.DESCRIPTION, "");
		validateReadWrite(hs);
		// Build long string
		StringBuffer s = new StringBuffer();  
		for(int i = 0; i < 256; i ++) {
			s.append(i);
		}
		hs.setHeader(OBEXHeaderSetImpl.OBEX_HDR_USER | OBEXHeaderSetImpl.OBEX_STRING, s.toString());
		validateReadWrite(hs);
	}
	
	public void testHeaderAllReadWrite() throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		hs.setHeader(HeaderSet.NAME, "test.txt");
		hs.setHeader(HeaderSet.TYPE, "text");
		hs.setHeader(OBEXHeaderSetImpl.OBEX_HDR_USER | OBEXHeaderSetImpl.OBEX_INT, new Long(0xFAFBFCFDl));
		hs.setHeader(HeaderSet.COUNT, new Long(0x12345678));
		hs.setHeader(HeaderSet.LENGTH, new Long(1));
		hs.setHeader((OBEXHeaderSetImpl.OBEX_HDR_USER + 1) | OBEXHeaderSetImpl.OBEX_BYTE, new Byte((byte)-1));
		validateReadWrite(hs);
	}

}
