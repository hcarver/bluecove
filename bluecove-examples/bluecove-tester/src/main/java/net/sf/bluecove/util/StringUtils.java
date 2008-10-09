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
package net.sf.bluecove.util;

/**
 * @author vlads
 * 
 */
public class StringUtils {

	public static boolean isStringSet(String str) {
		return ((str != null) && (str.length() > 0));
	}

	/**
	 * Only Since CLDC-1.1
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
