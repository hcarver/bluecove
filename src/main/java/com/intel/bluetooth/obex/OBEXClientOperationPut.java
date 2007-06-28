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

import javax.obex.HeaderSet;

import com.intel.bluetooth.DebugLog;

public class OBEXClientOperationPut extends OBEXClientOperation {

	protected OperationOutputStream os;
	
	protected class OperationOutputStream extends OutputStream {

		byte[] buffer; 
		
		int bufferLength;
		
		OperationOutputStream() {
			buffer = new byte[session.mtu - OBEXOperationCodes.OBEX_MTU_HEADER_RESERVE];
			bufferLength = 0;
		}
		     
        public void write(int i) throws IOException {
			write(new byte[] {(byte)i}, 0, 1);
		}
        
        public void write(byte b[], int off, int len) throws IOException {
			if ((os == null) || (isClosed)) {
				throw new IOException("stream closed");
			}
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (len < 0) || ((off + len) > b.length)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}

			synchronized (lock) {
				int written = 0;
				while (written < len) {
					int avalable = (buffer.length - bufferLength);
					if (len < avalable) {
						avalable = len;
					}
					System.arraycopy(b, off, buffer, bufferLength, avalable);
					bufferLength += avalable;
					written += avalable;
					if (bufferLength == buffer.length) {
						deliverPacket(false, buffer);
						bufferLength = 0;
					}
				}
			}
		}
        
        void deliverBuffer(boolean finalPacket) throws IOException {
			synchronized (lock) {
				byte[] b = new byte[bufferLength];
				System.arraycopy(buffer, 0, b, 0, bufferLength);
				deliverPacket(finalPacket, b);
				bufferLength = 0;
			}
		}

		public void close() throws IOException {
			synchronized (lock) {
				if (os != null) {
					os = null;
					deliverBuffer(true);
				}
			}
		}
		
	}
	
	OBEXClientOperationPut(OBEXClientSessionImpl session, HeaderSet headers) {
		super(session, headers);
	}

	public InputStream openInputStream() throws IOException {
		throw new IOException("Input not supported");
	}
	
	public OutputStream openOutputStream() throws IOException {
		if (isClosed) {
            throw new IOException("operation closed");
		}
		if (os != null) {
            throw new IOException("output still open");
		}
		os = new OperationOutputStream();
		return os;
	}

	public void sendHeaders(HeaderSet headers) throws IOException {
		super.sendHeaders(headers);
		if (os != null) {
			os.deliverBuffer(false);
		}
	}
	
	private void deliverPacket(boolean finalPacket, byte buffer[]) throws IOException {
		int commId = OBEXOperationCodes.PUT;
		int dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY; 
		if (finalPacket) {
			commId |= OBEXOperationCodes.FINAL_BIT;
			dataHeaderID = OBEXHeaderSetImpl.OBEX_HDR_BODY_END;
		}
		HeaderSet dataHeaders = session.createHeaderSet();
		dataHeaders.setHeader(dataHeaderID, buffer);
		
		if (sendHeadersLength + (buffer.length + OBEXOperationCodes.OBEX_MTU_HEADER_RESERVE) > session.mtu) {
			session.writeOperation(commId, OBEXHeaderSetImpl.toByteArray(dataHeaders));
		} else {
			session.writeOperation(commId, OBEXHeaderSetImpl.toByteArray(sendHeaders), OBEXHeaderSetImpl.toByteArray(dataHeaders));
			sendHeaders = null;
			sendHeadersLength = 0;
		}
		byte[] b = session.readOperation();
		replyHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
		DebugLog.debug0x("PUT reply", replyHeaders.getResponseCode());
	}
	
	public void close() throws IOException {
		if (os != null) {
			synchronized (lock) {
				if (os != null) {
					os.close();
				}
			}
		}
		super.close();
	}
}
