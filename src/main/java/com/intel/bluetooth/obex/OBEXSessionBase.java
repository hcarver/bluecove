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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;
import javax.obex.HeaderSet;

import com.intel.bluetooth.DebugLog;

/**
 * Base for Client and Server implementations. See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 * 
 * @author vlads
 * 
 */
public abstract class OBEXSessionBase implements Connection {
	
	protected StreamConnection conn;
	
	protected InputStream is;
	
	protected OutputStream os;

	protected long connectionID;
	
	protected int mtu = OBEXOperationCodes.OBEX_DEFAULT_MTU;
	
	public OBEXSessionBase(StreamConnection conn) throws IOException {
		this.conn = conn;
		connectionID = -1;
		boolean initOK = false;
		try {
			this.os = conn.openOutputStream();
			this.is = conn.openInputStream();
			initOK = true;
		} finally {
			if (!initOK) {
				try {
					this.close();
				} catch (IOException e) {
					DebugLog.error("close error", e);
				}	
			}
		}
	}
	
	public void close() throws IOException {
		StreamConnection c = this.conn;
		this.conn = null;
		if (this.is != null) {
			this.is.close();
			this.is = null;
		}
		if (this.os != null) {
			this.os.close();
			this.os = null;
		}
		if (c != null) {
			c.close();
		}
	}
	
	protected boolean isClosed() {
		return (this.conn == null);
	}
	
	public static HeaderSet createOBEXHeaderSet() {
		return new OBEXHeaderSetImpl();
	}
	
	static void validateCreatedHeaderSet(HeaderSet headers) {
		OBEXHeaderSetImpl.validateCreatedHeaderSet(headers);
	}

	protected void writeOperation(int commId, byte[] data) throws IOException {
		writeOperation(commId, data, null);
	}
	
	protected void writeOperation(int commId, byte[] data1, byte[] data2) throws IOException {
		int len = 3;
		if (this.connectionID != -1) {
			len += 5;
		}
		if (data1 != null) {
			len += data1.length;
		}
		if (data2 != null) {
			len += data2.length;
		}
		if (len > mtu) {
			 throw new IOException("Can't sent more data than in MTU, len=" + len + ", mtu="+ mtu);
		}
		OBEXHeaderSetImpl.writeObexLen(os, commId, len);
		if (this.connectionID != -1) {
			OBEXHeaderSetImpl.writeObexInt(os, OBEXHeaderSetImpl.OBEX_HDR_CONNECTION, this.connectionID);
		}
		if (data1 != null) {
			os.write(data1);
		}
		if (data2 != null) {
			os.write(data2, 0, data2.length);
		}
		os.flush();
		DebugLog.debug0x("obex sent", commId);
		DebugLog.debug("obex sent len", len);
	}
	
	protected byte[] readOperation() throws IOException {
		byte[] header = new byte[3];
		OBEXUtils.readFully(is, header);
		DebugLog.debug0x("obex received", header[0] & 0xFF);
		int lenght = OBEXUtils.bytesToShort(header[1], header[2]);
		if (lenght == 3) {
			return header;
		}
		if ((lenght < 3) || (lenght > OBEXOperationCodes.OBEX_MAX_PACKET_LEN)) {
			throw new IOException("Invalid packet lenght");
		}
		byte[] data = new byte[lenght];
		System.arraycopy(header, 0, data, 0, header.length);
		OBEXUtils.readFully(is, data, header.length, lenght - header.length);
		if (is.available() > 0) {
			DebugLog.debug("has more data after read", is.available());
		}
		return data;
	}
	
}
