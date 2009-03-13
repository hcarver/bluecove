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
package com.intel.bluetooth.obex;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.obex.HeaderSet;

import junit.framework.TestCase;

/**
 *
 */
public class HeaderTest extends TestCase {

	private void validateWriteLength(int lenExpected, int headerID, Object headerValue) throws IOException {
		HeaderSet headers = new OBEXHeaderSetImpl();
		headers.setHeader(headerID, headerValue);
		byte b[] = OBEXHeaderSetImpl.toByteArray(headers);
		assertEquals("length", lenExpected, b.length);
	}
	
	public void testHeaderWriteLength() throws IOException {
		validateWriteLength(3 + 12, HeaderSet.NAME, "Jumar");
		validateWriteLength(2, OBEXHeaderSetImpl.OBEX_HDR_USER | OBEXHeaderSetImpl.OBEX_BYTE, new Byte((byte)1));
		validateWriteLength(1 + 4, HeaderSet.LENGTH, new Long(1));
		validateWriteLength(1 + 2 + 3, HeaderSet.HTTP, new byte[]{1, 2, 3});
		validateWriteLength(1 + 4, HeaderSet.TIME_4_BYTE,  new GregorianCalendar());
		validateWriteLength(1 + 2 + 16, HeaderSet.TIME_ISO_8601,  new GregorianCalendar());
	}
	
	private void validateReadWrite(HeaderSet headers) throws IOException {
		byte b[] = OBEXHeaderSetImpl.toByteArray(headers);
		HeaderSet r = OBEXHeaderSetImpl.readHeaders((byte)0, b, 0);
		
		int[] headerIDArray = headers.getHeaderList();
		assertEquals("HeaderList.length", headerIDArray.length, r.getHeaderList().length);
		
		for (int i = 0; i < headerIDArray.length; i++) {
			int hi = headerIDArray[i];
			Object valueO = headers.getHeader(hi);
			Object valueR = r.getHeader(hi);
			assertNotNull("value Write", valueO);
			assertNotNull("value Read", valueR);
			if ((valueO instanceof Calendar)) {
				Calendar cO = (Calendar) valueO;
				Calendar cR = (Calendar) valueR;
				assertEquals("Header time value", cO.getTime().getTime() / 1000, cR.getTime().getTime() / 1000);
			} else if (!(valueO instanceof byte[])) {
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
	
	public void testHeaderCalendarReadWrite() throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		Calendar c = new GregorianCalendar();
		c.setTime(new Date());
		hs.setHeader(HeaderSet.TIME_4_BYTE, c);
		validateReadWrite(hs);
		c = new GregorianCalendar();
		c.setTime(new Date());
		hs.setHeader(HeaderSet.TIME_ISO_8601, c);
		validateReadWrite(hs);
	}

    public static Date detectDateformat(String str) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		return format.parse(str);
	}

	public void testHeaderCalendarESTReadWrite() throws IOException, ParseException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("EST"));
		c.setTime(detectDateformat("2006-01-02 18:30:21"));
		hs.setHeader(HeaderSet.TIME_4_BYTE, c);
		validateReadWrite(hs);
		c = Calendar.getInstance(TimeZone.getTimeZone("EST"));
		c.setTime(detectDateformat("2005-03-27 11:15:01"));
		hs.setHeader(HeaderSet.TIME_ISO_8601, c);
		validateReadWrite(hs);
	}
	
	public void testHeaderCalendarESTDaylightReadWrite() throws IOException, ParseException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("EST"));
		c.setTime(detectDateformat("2006-07-02 18:30:21"));
		hs.setHeader(HeaderSet.TIME_4_BYTE, c);
		validateReadWrite(hs);
		c = Calendar.getInstance(TimeZone.getTimeZone("EST"));
		c.setTime(detectDateformat("2005-07-27 11:15:01"));
		hs.setHeader(HeaderSet.TIME_ISO_8601, c);
		validateReadWrite(hs);
	}

	public void testHeaderCalendarPSTReadWrite() throws IOException, ParseException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		c.setTime(detectDateformat("2006-01-02 18:30:21"));
		hs.setHeader(HeaderSet.TIME_4_BYTE, c);
		validateReadWrite(hs);
		c = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		c.setTime(detectDateformat("2005-03-27 11:15:01"));
		hs.setHeader(HeaderSet.TIME_ISO_8601, c);
		validateReadWrite(hs);
	}
	
	public void testHeaderCalendarPSTDaylightReadWrite() throws IOException, ParseException {
		HeaderSet hs = new OBEXHeaderSetImpl();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		c.setTime(detectDateformat("2006-07-02 18:30:21"));
		hs.setHeader(HeaderSet.TIME_4_BYTE, c);
		validateReadWrite(hs);
		c = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		c.setTime(detectDateformat("2005-07-27 11:15:01"));
		hs.setHeader(HeaderSet.TIME_ISO_8601, c);
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
