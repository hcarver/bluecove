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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.obex.HeaderSet;

import com.intel.bluetooth.Utils;

public class OBEXHeaderSetImpl implements HeaderSet {

	 /** Number of objects (used by connect) (0xC0)*/
	static final int OBEX_HDR_COUNT = HeaderSet.COUNT;

	/** Name of the object (0x01) */
	static final int OBEX_HDR_NAME = HeaderSet.NAME;

	/** Type of the object (0x42)*/
	static final int OBEX_HDR_TYPE = HeaderSet.TYPE;

	/** Total lenght of object (0xC3)*/
	static final int OBEX_HDR_LENGTH = HeaderSet.LENGTH;

	/** Last modification time of (ISO8601) (0x44)*/
	static final int OBEX_HDR_TIME = HeaderSet.TIME_ISO_8601;

	/** Deprecated use HDR_TIME instead (0xC4)*/
	static final int OBEX_HDR_TIME2 = HeaderSet.TIME_4_BYTE;

	/** Description of object (0x05)*/
	static final int OBEX_HDR_DESCRIPTION = HeaderSet.DESCRIPTION;

	/** name of service that operation is targeted to (0x46)*/
	static final int OBEX_HDR_TARGET = HeaderSet.TARGET;

	/** An HTTP 1.x header (0x47)*/
	static final int OBEX_HDR_HTTP = HeaderSet.HTTP;

	/** Data part of the object (0x48)*/
	static final int OBEX_HDR_BODY = 0x48;

	/** Last data part of the object (0x49)*/
	static final int OBEX_HDR_BODY_END = 0x49;

	/** Identifies the sender of the object (0x4A)*/
	static final int OBEX_HDR_WHO = HeaderSet.WHO;

	/** Connection identifier used for OBEX connection multiplexing (0xCB)*/
	static final int OBEX_HDR_CONNECTION = 0xCB;

	/** Application parameters (0x4C)*/
	static final int OBEX_HDR_APP_PARAM = HeaderSet.APPLICATION_PARAMETER;

	/** Authentication digest-challenge (0x4D)*/
	static final int OBEX_HDR_AUTH_CHALLENGE = 0x4D;

	/** Authentication digest-response (0x4E)*/
	static final int OBEX_HDR_AUTH_RESPONSE = 0x4E;

	/** OBEX Object class of object (0x51)*/
	static final int OBEX_HDR_OBJECTCLASS = HeaderSet.OBJECT_CLASS;
	
	/** indicates the creator of an object (0xCF)*/
	static final int OBEX_HDR_CREATOR = 0xCF;

	/** uniquely identifies the network client (OBEX server) (0x50)*/
	static final int OBEX_HDR_WANUUID = 0x50;

	///** OBEX Object class of object (0x51)*/
	//static final int OBEX_HDR_OBJECTCLASS = 0x51;

	/** Parameters used in sessioncommands/responses (0x52)*/
	static final int OBEX_HDR_SESSIONPARAM = 0x52;

	/** Sequence number used in each OBEX packet for reliability (0x93)*/
	static final int OBEX_HDR_SESSIONSEQ = 0x93;
    
	// 0x30 to 0x3F user defined - this range includes all combinations of the upper 2 bits
	static final int OBEX_HDR_USER = 0x30;
	
	static final int OBEX_HDR_HI_MASK = 0xC0;
	
	/** null terminated Unicode text, length prefixed with 2 byte unsigned integer */
	static final int OBEX_STRING = 0x00;
	
	/** byte sequence, length prefixed with 2 byte unsigned integer */
	static final int OBEX_BYTE_STREAM = 0x40;
	
	/** 1 byte quantity */
	static final int OBEX_BYTE = 0x80;
	
	/** 4 byte quantity – transmitted in network byte order (high byte first) */
	static final int OBEX_INT = 0xC0;
	
	
	private int responseCode;
	
	private Hashtable headerValues;

	OBEXHeaderSetImpl() {
		this(Integer.MIN_VALUE);
	}

	private OBEXHeaderSetImpl(int responseCode) {
		headerValues = new Hashtable();
		this.responseCode = responseCode;
	}
	
	public void setHeader(int headerID, Object headerValue) {
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
					long v = ((Long)headerValue).longValue();
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
		if (headerID < 0 || headerID > 0xff) {
			throw new IllegalArgumentException("Expected header ID in range 0 to 255");
		}
		return headerValues.get(new Integer(headerID));
	}

	public int[] getHeaderList() throws IOException {
		int[] headerIDArray = new int[headerValues.size()];
		int i = 0;
		for (Enumeration e = headerValues.keys(); e.hasMoreElements();) {
			headerIDArray[i++] = ((Integer) e.nextElement()).intValue();
		}
		return headerIDArray;
	}

	public int getResponseCode() throws IOException {
		if (this.responseCode == Integer.MIN_VALUE) {
			throw new IOException();
		}
		return this.responseCode;
	}

	public void createAuthenticationChallenge(String realm, boolean userID, boolean access) {
		// TODO Auto-generated method stub

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
		b[0] = (byte)headerID;
		b[1] = (byte)((data >>> 24) & 0xFF);
        b[2] = (byte)((data >>> 16) & 0xFF);
        b[3] = (byte)((data >>>  8) & 0xFF);
        b[4] = (byte)((data >>>  0) & 0xFF);
        out.write(b);
    }
	
	static void writeObexLen(OutputStream out, int headerID, int len) throws IOException {
		byte[] b = new byte[3];
		b[0] = (byte)headerID;
		if ((len < 0) || len > 0xFFFF) {
			throw new IOException("very large data" + len);
		}
		b[1] = Utils.hiByte(len);
		b[2] = Utils.loByte(len);
        out.write(b);
    }
	
	static void writeObexASCII(OutputStream out, int headerID, String value) throws IOException {
		writeObexLen(out, headerID, 3 + value.length() + 1);
		out.write(value.getBytes("iso-8859-1"));
		out.write(0);
	}
	
	static void writeObexUnicode(OutputStream out, int headerID, String value) throws IOException {
		// null terminated Unicode text, length prefixed with 2 byte unsigned integer
		// the length field includes the 2 bytes of the null
		// terminator (0x00, 0x00). Therefore the length of the string ”Jumar” would be 12 bytes; 5 visible
		// characters plus the null terminator, each two bytes in length.
		if (value.length() == 0) {
			writeObexLen(out, headerID, 3);
			return;
		}
		byte[] b = value.getBytes("UTF-16BE");
		writeObexLen(out, headerID, 3 + b.length + 2);
		out.write(b);
		out.write(new byte[]{0, 0});
	}
	
	static byte[] toByteArray(HeaderSet headers) throws IOException {
		if (headers == null) {
			return new byte[0];
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int[] headerIDArray = headers.getHeaderList();
		for (int i = 0; i < headerIDArray.length; i++) {
			int hi = headerIDArray[i];
			if (hi == OBEX_HDR_TIME) {
				Calendar c = (Calendar) headers.getHeader(hi);
				// TODO UTC YYYYMMDDTHHMMSSZ
			} else if (hi == OBEX_HDR_TIME2) {
				Calendar c = (Calendar) headers.getHeader(hi);
				writeObexInt(buf, hi, c.getTimeInMillis() / 1000);
			} else if (hi == OBEX_HDR_TYPE) {
				//ASCII string
				writeObexASCII(buf, hi, (String)headers.getHeader(hi));
			} else {
				switch (hi & OBEX_HDR_HI_MASK) {
				case OBEX_STRING:
					writeObexUnicode(buf, hi, (String)headers.getHeader(hi));
					break;
				case OBEX_BYTE_STREAM:
					byte data[] = (byte[])headers.getHeader(hi);
					writeObexLen(buf, hi, 3 + data.length);
					buf.write(data);
					break;
				case OBEX_BYTE:
					buf.write(hi);
					buf.write(((Byte)headers.getHeader(hi)).byteValue());
					break;
				case OBEX_INT:
					writeObexInt(buf, hi, ((Long) headers.getHeader(hi)).longValue());
					break;
				default:
					throw new IOException("Unsupported encoding " + (hi & OBEX_HDR_HI_MASK));
				}
			}
		}
		return buf.toByteArray();
	}
	
	static HeaderSet read(byte responseCode, byte[] buf, int off) throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl(0xFF & responseCode);
		
		while (off < buf.length) {
			int hi = 0xFF & buf[off];
			int len = 0;
			switch (hi & OBEX_HDR_HI_MASK) {
			case OBEX_STRING:
				len = Utils.bytesToShort(buf[off + 1], buf[off + 2]);
				if (len == 3) {
					hs.setHeader(hi, "");
				} else {
					byte data[] = new byte[len - 5];
					System.arraycopy(buf, off + 3, data, 0, data.length);
					hs.setHeader(hi, new String(data, "UTF-16BE"));
				}
				break;
			case OBEX_BYTE_STREAM:
				len = Utils.bytesToShort(buf[off + 1], buf[off + 2]);
				byte data[] = new byte[len - 3];
				System.arraycopy(buf, off + 3, data, 0, data.length);
				if (hi == OBEX_HDR_TYPE) {
					if (data[data.length - 1] != 0) {
						hs.setHeader(hi, new String(data, "iso-8859-1"));
					} else {
						hs.setHeader(hi, new String(data, 0, data.length - 1, "iso-8859-1"));
					}
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
				hs.setHeader(hi, new Long(readObexInt(buf, off + 1)));
				break;
			default:
				throw new IOException("Unsupported encoding " + (hi & OBEX_HDR_HI_MASK));
			}
			off += len;
		}
		return hs;
	}

}
