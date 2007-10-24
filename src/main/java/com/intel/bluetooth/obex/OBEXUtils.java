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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;

import javax.obex.ResponseCodes;

/**
 * OBEX IO Utils
 * 
 * @author vlads
 * 
 */
abstract class OBEXUtils {

	static void readFully(InputStream is, OBEXConnectionParams obexConnectionParams, byte[] b) throws IOException,
			EOFException {
		readFully(is, obexConnectionParams, b, 0, b.length);
	}

	static void readFully(InputStream is, OBEXConnectionParams obexConnectionParams, byte[] b, int off, int len)
			throws IOException, EOFException {
		if (len < 0) {
			throw new IndexOutOfBoundsException();
		}
		int got = 0;
		while (got < len) {
			if (obexConnectionParams.timeouts) {
				long endOfDellay = System.currentTimeMillis() + obexConnectionParams.timeout;
				int available = 0;
				do {
					available = is.available();
					if (available == 0) {
						if (System.currentTimeMillis() > endOfDellay) {
							throw new InterruptedIOException("OBEX read timeout");
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							throw new InterruptedIOException();
						}
					}
				} while (available == 0);
			}
			int rc = is.read(b, off + got, len - got);
			if (rc < 0) {
				throw new EOFException();
			}
			got += rc;
		}
	}

	static String newStringUTF16Simple(byte bytes[]) throws UnsupportedEncodingException {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < bytes.length; i += 2) {
			buf.append((char) bytesToShort(bytes[i], bytes[i + 1]));
		}
		return buf.toString();
	}

	static String newStringUTF16(byte bytes[]) throws UnsupportedEncodingException {
		try {
			return new String(bytes, "UTF-16BE");
		} catch (IllegalArgumentException e) {
			// Java 1.1
			return newStringUTF16Simple(bytes);
		} catch (UnsupportedEncodingException e) {
			// IBM J9
			return newStringUTF16Simple(bytes);
		}
	}

	static byte[] getUTF16BytesSimple(String str) throws UnsupportedEncodingException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			buf.write(hiByte(c));
			buf.write(loByte(c));
		}
		return buf.toByteArray();
	}

	static byte[] getUTF16Bytes(String str) throws UnsupportedEncodingException {
		try {
			return str.getBytes("UTF-16BE");
		} catch (IllegalArgumentException e) {
			// Java 1.1
			return getUTF16BytesSimple(str);
		} catch (UnsupportedEncodingException e) {
			// IBM J9
			return getUTF16BytesSimple(str);
		}
	}

	static byte hiByte(int value) {
		return (byte) ((value >> 8) & 0xFF);
	}

	static byte loByte(int value) {
		return (byte) (0xFF & value);
	}

	static int bytesToShort(byte valueHi, byte valueLo) {
		return ((((int) valueHi << 8) & 0xFF00) + (valueLo & 0xFF));
	}

	public static String toStringObexResponseCodes(byte code) {
		return toStringObexResponseCodes(code & 0xFF);
	}

	public static String toStringObexResponseCodes(int code) {
		switch (code) {
		case OBEXOperationCodes.OBEX_RESPONSE_CONTINUE:
			return "OBEX_RESPONSE_CONTINUE";
		case ResponseCodes.OBEX_HTTP_OK:
			return "OBEX_HTTP_OK";
		case ResponseCodes.OBEX_HTTP_CREATED:
			return "OBEX_HTTP_CREATED";
		case ResponseCodes.OBEX_HTTP_ACCEPTED:
			return "OBEX_HTTP_ACCEPTED";
		case ResponseCodes.OBEX_HTTP_NOT_AUTHORITATIVE:
			return "OBEX_HTTP_NOT_AUTHORITATIVE";
		case ResponseCodes.OBEX_HTTP_NO_CONTENT:
			return "OBEX_HTTP_NO_CONTENT";
		case ResponseCodes.OBEX_HTTP_RESET:
			return "OBEX_HTTP_RESET";
		case ResponseCodes.OBEX_HTTP_PARTIAL:
			return "OBEX_HTTP_PARTIAL";
		case ResponseCodes.OBEX_HTTP_MULT_CHOICE:
			return "OBEX_HTTP_MULT_CHOICE";
		case ResponseCodes.OBEX_HTTP_MOVED_PERM:
			return "OBEX_HTTP_MOVED_PERM";
		case ResponseCodes.OBEX_HTTP_MOVED_TEMP:
			return "OBEX_HTTP_MOVED_TEMP";
		case ResponseCodes.OBEX_HTTP_SEE_OTHER:
			return "OBEX_HTTP_SEE_OTHER";
		case ResponseCodes.OBEX_HTTP_NOT_MODIFIED:
			return "OBEX_HTTP_NOT_MODIFIED";
		case ResponseCodes.OBEX_HTTP_USE_PROXY:
			return "OBEX_HTTP_USE_PROXY";
		case ResponseCodes.OBEX_HTTP_BAD_REQUEST:
			return "OBEX_HTTP_BAD_REQUEST";
		case ResponseCodes.OBEX_HTTP_UNAUTHORIZED:
			return "OBEX_HTTP_UNAUTHORIZED";
		case ResponseCodes.OBEX_HTTP_PAYMENT_REQUIRED:
			return "OBEX_HTTP_PAYMENT_REQUIRED";
		case ResponseCodes.OBEX_HTTP_FORBIDDEN:
			return "OBEX_HTTP_FORBIDDEN";
		case ResponseCodes.OBEX_HTTP_NOT_FOUND:
			return "OBEX_HTTP_NOT_FOUND";
		case ResponseCodes.OBEX_HTTP_BAD_METHOD:
			return "OBEX_HTTP_BAD_METHOD";
		case ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE:
			return "OBEX_HTTP_NOT_ACCEPTABLE";
		case ResponseCodes.OBEX_HTTP_PROXY_AUTH:
			return "OBEX_HTTP_PROXY_AUTH";
		case ResponseCodes.OBEX_HTTP_TIMEOUT:
			return "OBEX_HTTP_TIMEOUT";
		case ResponseCodes.OBEX_HTTP_CONFLICT:
			return "OBEX_HTTP_CONFLICT";
		case ResponseCodes.OBEX_HTTP_GONE:
			return "OBEX_HTTP_GONE";
		case ResponseCodes.OBEX_HTTP_LENGTH_REQUIRED:
			return "OBEX_HTTP_LENGTH_REQUIRED";
		case ResponseCodes.OBEX_HTTP_PRECON_FAILED:
			return "OBEX_HTTP_PRECON_FAILED";
		case ResponseCodes.OBEX_HTTP_ENTITY_TOO_LARGE:
			return "OBEX_HTTP_ENTITY_TOO_LARGE";
		case ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE:
			return "OBEX_HTTP_REQ_TOO_LARGE";
		case ResponseCodes.OBEX_HTTP_UNSUPPORTED_TYPE:
			return "OBEX_HTTP_UNSUPPORTED_TYPE";
		case ResponseCodes.OBEX_HTTP_INTERNAL_ERROR:
			return "OBEX_HTTP_INTERNAL_ERROR";
		case ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED:
			return "OBEX_HTTP_NOT_IMPLEMENTED";
		case ResponseCodes.OBEX_HTTP_BAD_GATEWAY:
			return "OBEX_HTTP_BAD_GATEWAY";
		case ResponseCodes.OBEX_HTTP_UNAVAILABLE:
			return "OBEX_HTTP_UNAVAILABLE";
		case ResponseCodes.OBEX_HTTP_GATEWAY_TIMEOUT:
			return "OBEX_HTTP_GATEWAY_TIMEOUT";
		case ResponseCodes.OBEX_HTTP_VERSION:
			return "OBEX_HTTP_VERSION";
		case ResponseCodes.OBEX_DATABASE_FULL:
			return "OBEX_DATABASE_FULL";
		case ResponseCodes.OBEX_DATABASE_LOCKED:
			return "OBEX_DATABASE_LOCKED";
		default:
			return "Unknown 0x" + Integer.toHexString(code);
		}
	}
}
