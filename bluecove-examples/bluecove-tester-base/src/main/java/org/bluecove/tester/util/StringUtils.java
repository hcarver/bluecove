/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package org.bluecove.tester.util;

/**
 * 
 */
public class StringUtils {

	public static boolean isStringSet(String str) {
		return ((str != null) && (str.length() > 0));
	}

	/**
	 * String.equalsIgnoreCase() was added only Since CLDC-1.1.
	 * 
	 * @param anotherString
	 * @return
	 */
	public static boolean equalsIgnoreCase(String s1, String s2) {
		if ((s1 == null) || (s2 == null)) {
			return false;
		}
		return (s1.length() == s2.length()) && (s1.toUpperCase().equals(s2.toUpperCase()));
	}

	public static String d00(int i) {
		if ((i > 9) || (i < 0)) {
			return String.valueOf(i);
		} else {
			return "0" + String.valueOf(i);
		}
	}

	public static String d000(int i) {
		if ((i > 99) || (i < 0)) {
			return String.valueOf(i);
		} else if (i > 9) {
			return "0" + String.valueOf(i);
		} else {
			return "00" + String.valueOf(i);
		}
	}

	public static String d0000(int i) {
		if ((i > 999) || (i < 0)) {
			return String.valueOf(i);
		} else if (i > 99) {
			return "00" + String.valueOf(i);
		} else if (i > 9) {
			return "00" + String.valueOf(i);
		} else {
			return "000" + String.valueOf(i);
		}
	}

	public static String formatLong(long l) {
		if (l < 1000) {
			return Long.toString(l);
		}
		long l1K = (l / 1000);
		if (l1K < 1000) {
			return Long.toString(l1K) + "," + StringUtils.d000((int) (l - 1000L * l1K));
		} else {
			long l1M = (l1K / 1000);
			return Long.toString(l1M) + "," + StringUtils.d000((int) (l1K - 1000L * l1M)) + ","
					+ StringUtils.d000((int) (l - 1000L * l1K));
		}
	}

	public static String toHex00String(int c) {
		String s = Integer.toHexString(c);
		if (s.length() == 1) {
			return "0" + s;
		} else {
			return s;
		}
	}

	public static String padRight(String str, int length, char c) {
		int l = str.length();
		if (l >= length) {
			return str;
		}
		StringBuffer sb = new StringBuffer();
		sb.append(str);
		for (int i = l; i < length; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

	public static char printable(char c) {
		if (c < ' ') {
			return ' ';
		} else {
			return c;
		}
	}

	public static String toBinaryText(StringBuffer buf) {
		boolean bufHasBinary = false;
		int len = buf.length();
		for (int i = 0; i < len; i++) {
			if (buf.charAt(i) < ' ') {
				bufHasBinary = true;
				break;
			}
		}
		if (bufHasBinary) {
			StringBuffer formatedDataBuf = new StringBuffer();
			for (int k = 0; k < len; k++) {
				formatedDataBuf.append(printable(buf.charAt(k)));
			}
			formatedDataBuf.append(" 0x[");
			for (int k = 0; k < len; k++) {
				formatedDataBuf.append(toHex00String(buf.charAt(k))).append(' ');
			}
			formatedDataBuf.append("]");
			buf = formatedDataBuf;
		}

		return buf.toString();
	}
}
