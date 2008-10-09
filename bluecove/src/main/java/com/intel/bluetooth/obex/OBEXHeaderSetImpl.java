/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import javax.obex.HeaderSet;

import com.intel.bluetooth.DebugLog;

class OBEXHeaderSetImpl implements HeaderSet {

	/** Number of objects (used by connect) (0xC0) */
	static final int OBEX_HDR_COUNT = HeaderSet.COUNT;

	/** Name of the object (0x01) */
	static final int OBEX_HDR_NAME = HeaderSet.NAME;

	/** Type of the object (0x42) */
	static final int OBEX_HDR_TYPE = HeaderSet.TYPE;

	/** Total lenght of object (0xC3) */
	static final int OBEX_HDR_LENGTH = HeaderSet.LENGTH;

	/** Last modification time of (ISO8601) (0x44) */
	static final int OBEX_HDR_TIME = HeaderSet.TIME_ISO_8601;

	/** Deprecated use HDR_TIME instead (0xC4) */
	static final int OBEX_HDR_TIME2 = HeaderSet.TIME_4_BYTE;

	/** Description of object (0x05) */
	static final int OBEX_HDR_DESCRIPTION = HeaderSet.DESCRIPTION;

	/** name of service that operation is targeted to (0x46) */
	static final int OBEX_HDR_TARGET = HeaderSet.TARGET;

	/** An HTTP 1.x header (0x47) */
	static final int OBEX_HDR_HTTP = HeaderSet.HTTP;

	/** Data part of the object (0x48) */
	static final int OBEX_HDR_BODY = 0x48;

	/** Last data part of the object (0x49) */
	static final int OBEX_HDR_BODY_END = 0x49;

	/** Identifies the sender of the object (0x4A) */
	static final int OBEX_HDR_WHO = HeaderSet.WHO;

	/** Connection identifier used for OBEX connection multiplexing (0xCB) */
	static final int OBEX_HDR_CONNECTION = 0xCB;

	/** Application parameters (0x4C) */
	static final int OBEX_HDR_APP_PARAM = HeaderSet.APPLICATION_PARAMETER;

	/** Authentication digest-challenge (0x4D) */
	static final int OBEX_HDR_AUTH_CHALLENGE = 0x4D;

	/** Authentication digest-response (0x4E) */
	static final int OBEX_HDR_AUTH_RESPONSE = 0x4E;

	/** OBEX Object class of object (0x51) */
	static final int OBEX_HDR_OBJECTCLASS = HeaderSet.OBJECT_CLASS;

	/** indicates the creator of an object (0xCF) */
	static final int OBEX_HDR_CREATOR = 0xCF;

	/** uniquely identifies the network client (OBEX server) (0x50) */
	static final int OBEX_HDR_WANUUID = 0x50;

	// /** OBEX Object class of object (0x51)*/
	// static final int OBEX_HDR_OBJECTCLASS = 0x51;

	/** Parameters used in sessioncommands/responses (0x52) */
	static final int OBEX_HDR_SESSIONPARAM = 0x52;

	/** Sequence number used in each OBEX packet for reliability (0x93) */
	static final int OBEX_HDR_SESSIONSEQ = 0x93;

	// 0x30 to 0x3F user defined - this range includes all combinations of the
	// upper 2 bits
	static final int OBEX_HDR_USER = 0x30;

	static final int OBEX_HDR_HI_MASK = 0xC0;

	static final int OBEX_HDR_ID_MASK = 0x3F;

	/**
	 * null terminated Unicode text, length prefixed with 2 byte unsigned
	 * integer
	 */
	static final int OBEX_STRING = 0x00;

	/** byte sequence, length prefixed with 2 byte unsigned integer */
	static final int OBEX_BYTE_STREAM = 0x40;

	/** 1 byte quantity */
	static final int OBEX_BYTE = 0x80;

	/** 4 byte quantity – transmitted in network byte order (high byte first) */
	static final int OBEX_INT = 0xC0;

	private static final int OBEX_MAX_FIELD_LEN = 0xFF;

	private int responseCode;

	private Hashtable headerValues;

	private Vector authResponses;

	private Vector authChallenges;

	private static final int NO_RESPONSE_CODE = Integer.MIN_VALUE;

	OBEXHeaderSetImpl() {
		this(NO_RESPONSE_CODE);
	}

	private OBEXHeaderSetImpl(int responseCode) {
		this.headerValues = new Hashtable();
		this.responseCode = responseCode;
		this.authResponses = new Vector();
		this.authChallenges = new Vector();
	}

	static void validateCreatedHeaderSet(HeaderSet headers) {
		if (headers == null) {
			return;
		}
		if (!(headers instanceof OBEXHeaderSetImpl)) {
			throw new IllegalArgumentException("Illegal HeaderSet type");
		}
		if (((OBEXHeaderSetImpl) headers).responseCode != NO_RESPONSE_CODE) {
			throw new IllegalArgumentException("Illegal HeaderSet");
		}
	}

	private void validateHeaderID(int headerID) throws IllegalArgumentException {
		if (headerID < 0 || headerID > 0xff) {
			throw new IllegalArgumentException("Expected header ID in range 0 to 255");
		}
		int identifier = headerID & OBEX_HDR_ID_MASK;
		if (identifier >= 0x10 && identifier < 0x2F) {
			throw new IllegalArgumentException("Reserved header ID");
		}
	}

	public void setHeader(int headerID, Object headerValue) {
		validateHeaderID(headerID);
		if (headerValue == null) {
			headerValues.remove(new Integer(headerID));
		} else {
			// Validate Java value Type
			if ((headerID == OBEX_HDR_TIME) || (headerID == OBEX_HDR_TIME2)) {
				if (!(headerValue instanceof Calendar)) {
					throw new IllegalArgumentException("Expected java.util.Calendar");
				}
			} else if (headerID == OBEX_HDR_TYPE) {
				if (!(headerValue instanceof String)) {
					throw new IllegalArgumentException("Expected java.lang.String");
				}
			} else {
				switch (headerID & OBEX_HDR_HI_MASK) {
				case OBEX_STRING:
					if (!(headerValue instanceof String)) {
						throw new IllegalArgumentException("Expected java.lang.String");
					}
					break;
				case OBEX_BYTE_STREAM:
					if (!(headerValue instanceof byte[])) {
						throw new IllegalArgumentException("Expected byte[]");
					}
					break;
				case OBEX_BYTE:
					if (!(headerValue instanceof Byte)) {
						throw new IllegalArgumentException("Expected java.lang.Byte");
					}
					break;
				case OBEX_INT:
					if (!(headerValue instanceof Long)) {
						throw new IllegalArgumentException("Expected java.lang.Long");
					}
					long v = ((Long) headerValue).longValue();
					if (v < 0 || v > 0xffffffffl) {
						throw new IllegalArgumentException("Expected long in range 0 to 2^32-1");
					}
					break;
				default:
					throw new IllegalArgumentException("Unsupported encoding " + (headerID & OBEX_HDR_HI_MASK));
				}
			}
			headerValues.put(new Integer(headerID), headerValue);
		}
	}

	public Object getHeader(int headerID) throws IOException {
		validateHeaderID(headerID);
		return headerValues.get(new Integer(headerID));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.obex.HeaderSet#getHeaderList()
	 */
	public int[] getHeaderList() throws IOException {
		if (headerValues.size() == 0) {
			// Spec: null if no headers are available
			return null;
		}
		int[] headerIDArray = new int[headerValues.size()];
		int i = 0;
		for (Enumeration e = headerValues.keys(); e.hasMoreElements();) {
			headerIDArray[i++] = ((Integer) e.nextElement()).intValue();
		}
		return headerIDArray;
	}

	public int getResponseCode() throws IOException {
		if (this.responseCode == NO_RESPONSE_CODE) {
			throw new IOException();
		}
		return this.responseCode;
	}

	static HeaderSet cloneHeaders(HeaderSet headers) throws IOException {
		if (headers == null) {
			return null;
		}
		if (!(headers instanceof OBEXHeaderSetImpl)) {
			throw new IllegalArgumentException("Illegal HeaderSet type");
		}
		HeaderSet hs = new OBEXHeaderSetImpl(((OBEXHeaderSetImpl) headers).responseCode);

		int[] headerIDArray = headers.getHeaderList();
		for (int i = 0; (headerIDArray != null) && (i < headerIDArray.length); i++) {
			int headerID = headerIDArray[i];
			// Body is not accessible by the client
			if ((headerID == OBEX_HDR_BODY) || (headerID == OBEX_HDR_BODY_END)) {
				continue;
			}
			hs.setHeader(headerID, headers.getHeader(headerID));
		}
		return hs;
	}

	static HeaderSet appendHeaders(HeaderSet dst, HeaderSet src) throws IOException {
		int[] headerIDArray = src.getHeaderList();
		for (int i = 0; (headerIDArray != null) && (i < headerIDArray.length); i++) {
			int headerID = headerIDArray[i];
			if ((headerID == OBEX_HDR_BODY) || (headerID == OBEX_HDR_BODY_END)) {
				continue;
			}
			dst.setHeader(headerID, src.getHeader(headerID));
		}
		return dst;
	}

	public void createAuthenticationChallenge(String realm, boolean isUserIdRequired, boolean isFullAccess) {
		authChallenges.addElement(OBEXAuthentication.createChallenge(realm, isUserIdRequired, isFullAccess));
	}

	void addAuthenticationResponse(byte[] authResponse) {
		authResponses.addElement(authResponse);
	}

	boolean hasAuthenticationChallenge() {
		return !authChallenges.isEmpty();
	}

	Enumeration getAuthenticationChallenges() {
		return authChallenges.elements();
	}

	boolean hasAuthenticationResponse() {
		return !authResponses.isEmpty();
	}

	Enumeration getAuthenticationResponses() {
		return authResponses.elements();
	}

	static long readObexInt(byte[] data, int off) throws IOException {
		long l = 0;
		for (int i = 0; i < 4; i++) {
			l = l << 8;
			l += (int) (data[off + i] & 0xFF);
		}
		return l;
	}

	static void writeObexInt(OutputStream out, int headerID, long data) throws IOException {
		byte[] b = new byte[5];
		b[0] = (byte) headerID;
		b[1] = (byte) ((data >>> 24) & 0xFF);
		b[2] = (byte) ((data >>> 16) & 0xFF);
		b[3] = (byte) ((data >>> 8) & 0xFF);
		b[4] = (byte) ((data >>> 0) & 0xFF);
		out.write(b);
	}

	static void writeObexLen(OutputStream out, int headerID, int len) throws IOException {
		byte[] b = new byte[3];
		b[0] = (byte) headerID;
		if ((len < 0) || len > 0xFFFF) {
			throw new IOException("very large data" + len);
		}
		b[1] = OBEXUtils.hiByte(len);
		b[2] = OBEXUtils.loByte(len);
		out.write(b);
	}

	static void writeObexASCII(OutputStream out, int headerID, String value) throws IOException {
		writeObexLen(out, headerID, 3 + value.length() + 1);
		out.write(value.getBytes("iso-8859-1"));
		out.write(0);
	}

	static void writeObexUnicode(OutputStream out, int headerID, String value) throws IOException {
		// null terminated Unicode text, length prefixed with 2 byte unsigned
		// integer
		// the length field includes the 2 bytes of the null
		// terminator (0x00, 0x00). Therefore the length of the string ”Jumar”
		// would be 12 bytes; 5 visible
		// characters plus the null terminator, each two bytes in length.
		if (value.length() == 0) {
			writeObexLen(out, headerID, 3);
			return;
		}
		byte[] b = OBEXUtils.getUTF16Bytes(value);
		writeObexLen(out, headerID, 3 + b.length + 2);
		out.write(b);
		out.write(new byte[] { 0, 0 });
	}

	static byte[] toByteArray(HeaderSet headers) throws IOException {
		if (headers == null) {
			return new byte[0];
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int[] headerIDArray = headers.getHeaderList();
		for (int i = 0; (headerIDArray != null) && (i < headerIDArray.length); i++) {
			int hi = headerIDArray[i];
			if (hi == OBEX_HDR_TIME) {
				Calendar c = (Calendar) headers.getHeader(hi);
				writeObexLen(buf, hi, 19);
				writeTimeISO8601(buf, c);
			} else if (hi == OBEX_HDR_TIME2) {
				Calendar c = (Calendar) headers.getHeader(hi);
				writeObexInt(buf, hi, c.getTime().getTime() / 1000);
			} else if (hi == OBEX_HDR_TYPE) {
				// ASCII string
				writeObexASCII(buf, hi, (String) headers.getHeader(hi));
			} else {
				switch (hi & OBEX_HDR_HI_MASK) {
				case OBEX_STRING:
					writeObexUnicode(buf, hi, (String) headers.getHeader(hi));
					break;
				case OBEX_BYTE_STREAM:
					byte data[] = (byte[]) headers.getHeader(hi);
					writeObexLen(buf, hi, 3 + data.length);
					buf.write(data);
					break;
				case OBEX_BYTE:
					buf.write(hi);
					buf.write(((Byte) headers.getHeader(hi)).byteValue());
					break;
				case OBEX_INT:
					writeObexInt(buf, hi, ((Long) headers.getHeader(hi)).longValue());
					break;
				default:
					throw new IOException("Unsupported encoding " + (hi & OBEX_HDR_HI_MASK));
				}
			}
		}
		if ((headerIDArray != null) && (headerIDArray.length != 0)) {
			DebugLog.debug("written headers", headerIDArray.length);
		}
		for (Enumeration iter = ((OBEXHeaderSetImpl) headers).authChallenges.elements(); iter.hasMoreElements();) {
			byte[] authChallenge = (byte[]) iter.nextElement();
			writeObexLen(buf, OBEX_HDR_AUTH_CHALLENGE, 3 + authChallenge.length);
			buf.write(authChallenge);
			DebugLog.debug("written AUTH_CHALLENGE");
		}
		for (Enumeration iter = ((OBEXHeaderSetImpl) headers).authResponses.elements(); iter.hasMoreElements();) {
			byte[] authResponse = (byte[]) iter.nextElement();
			writeObexLen(buf, OBEX_HDR_AUTH_RESPONSE, 3 + authResponse.length);
			buf.write(authResponse);
			DebugLog.debug("written AUTH_RESPONSE");
		}
		return buf.toByteArray();
	}

	/*
	 * Read by server
	 */
	static OBEXHeaderSetImpl readHeaders(byte[] buf, int off) throws IOException {
		return readHeaders(new OBEXHeaderSetImpl(NO_RESPONSE_CODE), buf, off);
	}

	static OBEXHeaderSetImpl readHeaders(byte responseCode, byte[] buf, int off) throws IOException {
		return readHeaders(new OBEXHeaderSetImpl(0xFF & responseCode), buf, off);
	}

	private static OBEXHeaderSetImpl readHeaders(OBEXHeaderSetImpl hs, byte[] buf, int off) throws IOException {
		int count = 0;
		while (off < buf.length) {
			int hi = 0xFF & buf[off];
			int len = 0;
			switch (hi & OBEX_HDR_HI_MASK) {
			case OBEX_STRING:
				len = OBEXUtils.bytesToShort(buf[off + 1], buf[off + 2]);
				if (len == 3) {
					hs.setHeader(hi, "");
				} else {
					byte data[] = new byte[len - 5];
					System.arraycopy(buf, off + 3, data, 0, data.length);
					hs.setHeader(hi, OBEXUtils.newStringUTF16(data));
				}
				break;
			case OBEX_BYTE_STREAM:
				len = OBEXUtils.bytesToShort(buf[off + 1], buf[off + 2]);
				byte data[] = new byte[len - 3];
				System.arraycopy(buf, off + 3, data, 0, data.length);
				if (hi == OBEX_HDR_TYPE) {
					if (data[data.length - 1] != 0) {
						hs.setHeader(hi, new String(data, "iso-8859-1"));
					} else {
						hs.setHeader(hi, new String(data, 0, data.length - 1, "iso-8859-1"));
					}
				} else if (hi == OBEX_HDR_TIME) {
					hs.setHeader(hi, readTimeISO8601(data));
				} else if (hi == OBEX_HDR_AUTH_CHALLENGE) {
					((OBEXHeaderSetImpl) hs).authChallenges.addElement(data);
					DebugLog.debug("received AUTH_CHALLENGE");
				} else if (hi == OBEX_HDR_AUTH_RESPONSE) {
					((OBEXHeaderSetImpl) hs).authResponses.addElement(data);
					DebugLog.debug("received AUTH_RESPONSE");
				} else {
					hs.setHeader(hi, data);
				}
				break;
			case OBEX_BYTE:
				len = 2;
				hs.setHeader(hi, new Byte(buf[off + 1]));
				break;
			case OBEX_INT:
				len = 5;
				long intValue = readObexInt(buf, off + 1);
				if (hi == OBEX_HDR_TIME2) {
					Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
					cal.setTime(new Date(intValue * 1000));
					hs.setHeader(hi, cal);
				} else {
					hs.setHeader(hi, new Long(intValue));
				}
				break;
			default:
				throw new IOException("Unsupported encoding " + (hi & OBEX_HDR_HI_MASK));
			}
			off += len;
			count++;
		}
		if (count != 0) {
			DebugLog.debug("read headers", count);
		}
		return hs;
	}

	private static byte[] d4(int i) {
		byte[] b = new byte[4];
		int d = 1000;
		for (int k = 0; k < 4; k++) {
			b[k] = (byte) (i / d + '0');
			i %= d;
			d /= 10;
		}
		return b;
	}

	private static byte[] d2(int i) {
		byte[] b = new byte[2];
		b[0] = (byte) (i / 10 + '0');
		b[1] = (byte) (i % 10 + '0');
		return b;
	}

	/**
	 * ISO-8601 UTC YYYYMMDDTHHMMSSZ
	 */
	static void writeTimeISO8601(OutputStream out, Calendar c) throws IOException {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(c.getTime());
		out.write(d4(cal.get(Calendar.YEAR)));
		out.write(d2(cal.get(Calendar.MONTH) + 1));
		out.write(d2(cal.get(Calendar.DAY_OF_MONTH)));
		out.write('T');
		out.write(d2(cal.get(Calendar.HOUR_OF_DAY)));
		out.write(d2(cal.get(Calendar.MINUTE)));
		out.write(d2(cal.get(Calendar.SECOND)));
		out.write('Z');
	}

	/**
	 * ISO-8601 UTC YYYYMMDDTHHMMSS(Z) Z for UTC time
	 */
	static Calendar readTimeISO8601(byte data[]) throws IOException {
		boolean utc = false;
		if ((data.length != 16) && (data.length != 15)) {
			throw new IOException("Invalid ISO-8601 date length " + new String(data) + " length " + data.length);
		} else if (data[8] != 'T') {
			throw new IOException("Invalid ISO-8601 date " + new String(data));
		} else if (data.length == 16) {
			if (data[15] != 'Z') {
				throw new IOException("Invalid ISO-8601 date " + new String(data));
			} else {
				utc = true;
			}
		}
		Calendar cal = utc ? Calendar.getInstance(TimeZone.getTimeZone("UTC")) : Calendar.getInstance();
		cal.set(Calendar.YEAR, Integer.valueOf(new String(data, 0, 4)).intValue());
		cal.set(Calendar.MONTH, Integer.valueOf(new String(data, 4, 2)).intValue() - 1);
		cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(new String(data, 6, 2)).intValue());
		cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(new String(data, 9, 2)).intValue());
		cal.set(Calendar.MINUTE, Integer.valueOf(new String(data, 11, 2)).intValue());
		cal.set(Calendar.SECOND, Integer.valueOf(new String(data, 13, 2)).intValue());
		return cal;
	}

}
