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
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

import com.intel.bluetooth.Utils;

/**
 * See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 * 
 * 
 * @author vlads
 * 
 */
public class OBEXClientSessionImpl implements ClientSession {

	private StreamConnection conn;
	
	private InputStream is;
	
	private OutputStream os;
	
	private long connectionID;
	
	private short mtu = OBEXOperationCodes.OBEX_DEFAULT_MTU;
		
	public OBEXClientSessionImpl(StreamConnection conn) throws IOException {
		this.conn = conn;
		this.os = conn.openOutputStream();
		this.is = conn.openInputStream();
		this.connectionID = -1;
	}

	public HeaderSet createHeaderSet() {
		return new OBEXHeaderSetImpl();
	}

	public HeaderSet connect(HeaderSet headers) throws IOException {
		byte[] connectRequest = new byte[4];
		connectRequest[0] = OBEXOperationCodes.OBEX_VERSION;
		connectRequest[1] = 0; /* Flags */
		connectRequest[2] = Utils.hiByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		connectRequest[3] = Utils.loByte(OBEXOperationCodes.OBEX_DEFAULT_MTU);
		writeOperation(OBEXOperationCodes.CONNECT | OBEXOperationCodes.FINAL_BIT, connectRequest, OBEXHeaderSetImpl.toByteArray(headers));
		
		byte[] b = readOperation();
		short serverMTU = Utils.bytesToShort(b[5], b[6]);
		if (serverMTU < this.mtu) {
			this.mtu = serverMTU;
		}
		return OBEXHeaderSetImpl.read(b[0], b, 7);
	}

	public HeaderSet disconnect(HeaderSet headers) throws IOException {
		writeOperation(OBEXOperationCodes.DISCONNECT | OBEXOperationCodes.FINAL_BIT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.read(b[0], b, 3);
	}

	public void setConnectionID(long id) {
		this.connectionID = id;
	}
	
	public long getConnectionID() {
		return this.connectionID;
	}

	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException {
		byte[] request = new byte[2];
		request[0] = (byte) ((backup?1:0) | (create?0:2));
		request[1] = 0;
		writeOperation(OBEXOperationCodes.SETPATH, request, OBEXHeaderSetImpl.toByteArray(headers));
		
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.read(b[0], b, 3);
	}

	public Operation get(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Operation put(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		writeOperation(OBEXOperationCodes.PUT, OBEXHeaderSetImpl.toByteArray(headers));
		byte[] b = readOperation();
		return OBEXHeaderSetImpl.read(b[0], b, 3);
	}

	public void setAuthenticator(Authenticator auth) {
		// TODO Auto-generated method stub

	}

	public void close() throws IOException {
		if (this.is != null) {
			this.is.close();
			this.is = null;
		}
		if (this.os != null) {
			this.os.close();
			this.os = null;
		}
		if (this.conn != null) {
			this.conn.close();
			this.conn = null;
		}
	}

	private void writeOperation(int commId, byte[] data) throws IOException {
		writeOperation(commId, data, null);
	}
	
	private void writeOperation(int commId, byte[] data1, byte[] data2) throws IOException {
		short len = 3; 
		len += data1.length;
		if (data2 != null) {
			len += data2.length;
		}
		byte[] header = new byte[3];
		header[0] = (byte)commId;
		header[1] = Utils.hiByte(len);
		header[2] = Utils.loByte(len);
		os.write(header);
		os.write(data1);
		if (data2 != null) {
			os.write(data2);
		}
		os.flush();
	}
	
	private byte[] readOperation() throws IOException {
		byte[] header = new byte[3];
		Utils.readFully(is, header);
		//byte code = header[0];
		short lenght = Utils.bytesToShort(header[1], header[2]);
		byte[] data = new byte[lenght];
		System.arraycopy(header, 0, data, 0, header.length);
		Utils.readFully(is, data, header.length, lenght - header.length);
		return data;
	}



}
