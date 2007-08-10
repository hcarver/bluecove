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

import javax.obex.HeaderSet;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.DebugLog;

/**
 * @author vlads
 *
 */
class OBEXServerOperationPut extends OBEXServerOperation implements OBEXOperationReceive {

	private OBEXOperationInputStream inputStream;
	
	private boolean inputStreamOpened = false;
	
	protected OBEXServerOperationPut(OBEXServerSessionImpl session, HeaderSet receivedHeaders, boolean finalPacket) throws IOException {
		super(session, receivedHeaders);
		this.inputStream = new OBEXOperationInputStream(this);
		processData(receivedHeaders, inputStream);
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.InputConnection#openInputStream()
	 */
	public InputStream openInputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		if (inputStreamOpened) {
            throw new IOException("input stream already open");
		}
		DebugLog.debug("openInputStream");
		inputStreamOpened = true;
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		return inputStream;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.OutputConnection#openOutputStream()
	 */
	public OutputStream openOutputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		return new UnsupportedOutputStream();
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		DebugLog.debug("server close put");
		//(new Throwable()).printStackTrace();
		inputStream.close();
		super.close();
	}
	
	public void receiveData(OBEXOperationInputStream is) throws IOException {
		byte[] b = session.readOperation();
		int opcode = b[0] & 0xFF;
		boolean finalPacket = ((opcode & OBEXOperationCodes.FINAL_BIT) != 0);
		if (finalPacket) {
			DebugLog.debug("OBEXServerSession operation final packet");	
			finalPacketReceived = true;
		}
		switch (opcode) {
		case OBEXOperationCodes.PUT | OBEXOperationCodes.FINAL_BIT:
		case OBEXOperationCodes.PUT:
			HeaderSet requestHeaders = OBEXHeaderSetImpl.readHeaders(b[0], b, 3);
			processRequest(requestHeaders, finalPacket, is);
			break;
		case OBEXOperationCodes.ABORT:
			processAbort();
			break;
		default:
			session.writeOperation(ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED, null);
		}
	}
	
	private void processAbort() throws IOException {
		session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_SUCCESS, null);
		close();
		throw new IOException("Operation aborted by client");
	}
	
	private boolean processData(HeaderSet requestHeaders, OBEXOperationInputStream is) throws IOException {
		byte[] data = (byte[])requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END);
		if (data == null) {
			data = (byte[])requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY);
		}
		if ((data != null) && (data.length != 0) && (is != null)) {
			DebugLog.debug("processData len", data.length);
			is.appendData(data);
			return true;
		} else {
			return false;
		}
	}
	
	protected void processRequest(HeaderSet requestHeaders, boolean finalPacket, OBEXOperationInputStream is) throws IOException {
		processData(requestHeaders, is);
		if (finalPacket) {
			DebugLog.debug("put got final packet");
			close();
		} else {
			DebugLog.debug("reply continue");
			session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		}
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.obex.OBEXServerOperation#writeResponse(int)
	 */
	void writeResponse(int responseCode) throws IOException {
		session.writeOperation(responseCode, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		if (responseCode == ResponseCodes.OBEX_HTTP_OK) {
			while ((!finalPacketReceived) && (!isClosed())) {
				DebugLog.debug("receive final packet");
				receiveData(null);
				if (finalPacketReceived) {
					session.writeOperation(responseCode, OBEXHeaderSetImpl.toByteArray(sendHeaders));			
				}
			}
		} else {
			DebugLog.debug("sent final reply");
		}
	}

}
