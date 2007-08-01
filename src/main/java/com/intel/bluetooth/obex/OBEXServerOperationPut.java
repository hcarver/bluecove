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
class OBEXServerOperationPut extends OBEXServerOperation {

	private OperationInputStream is;
	
	private boolean inputStreamOpened = false;
	
	private class OperationInputStream extends InputStream {

		private byte[] buffer = new byte[0x100];
	    
		private int readPos = 0;
	    
		private int appendPos = 0;
	
		protected Object lock = new Object();
		
		/* (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		public int read() throws IOException {
			if (isClosed && (appendPos == readPos)) {
				return -1;
			}
			synchronized (lock) {
				while (!isClosed && (appendPos == readPos)) {	
					try {
						receiveData();
					} catch (IOException e) {
						if (!isClosed) {
							throw e;
						}
					}
				}
				if (appendPos == readPos) {
					return -1;
				}
				return buffer[readPos++] & 0xFF;
			}
		}
		
		/* (non-Javadoc)
		 * @see java.io.InputStream#available()
		 */
		public int available() throws IOException {
			synchronized (lock) {
				return (appendPos - readPos);
			}
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#close()
		 */
		public void close() throws IOException {
			synchronized (lock) {
				lock.notify();
			}
		}
		 
		public void appendData(byte[] b) {
			synchronized (lock) {
				if (appendPos + b.length > buffer.length) {
					int newSize = (b.length + (appendPos - readPos)) * 2;
					if (newSize < buffer.length) {
						newSize = buffer.length;
					}
					byte[] newBuffer = new byte[newSize];
			        System.arraycopy(buffer, readPos, newBuffer, 0, appendPos - readPos);
			        buffer = newBuffer;
			        appendPos -= readPos;
			        readPos = 0;
				}
				System.arraycopy(b, 0, buffer, appendPos, b.length);
				appendPos += b.length;
				
				lock.notify();
			}
		}
	}
	
	protected OBEXServerOperationPut(OBEXServerSessionImpl session, HeaderSet receivedHeaders, boolean finalPacket) throws IOException {
		super(session, receivedHeaders);
		this.is = new OperationInputStream();
		processData(receivedHeaders);
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
		return is;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.OutputConnection#openOutputStream()
	 */
	public OutputStream openOutputStream() throws IOException {
		throw new IOException("Output not supported");
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		DebugLog.debug("server put close");
		is.close();
		super.close();
	}
	
	private void receiveData() throws IOException {
		byte[] b = session.readOperation();
		int opcode = b[0] & 0xFF;
		boolean finalPacket = ((opcode & OBEXOperationCodes.FINAL_BIT) != 0);
		if (finalPacket) {
			DebugLog.debug("OBEXServerSession operation finalPacket");	
		}
		switch (opcode) {
		case OBEXOperationCodes.PUT | OBEXOperationCodes.FINAL_BIT:
		case OBEXOperationCodes.PUT:
			HeaderSet requestHeaders = OBEXHeaderSetImpl.read(b[0], b, 3);
			processRequest(requestHeaders, finalPacket);
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
	
	private boolean processData(HeaderSet requestHeaders) throws IOException {
		byte[] data = (byte[])requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END);
		if (data == null) {
			data = (byte[])requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY);
		}
		if ((data != null) && (data.length != 0)) {
			DebugLog.debug("processData len", data.length);
			is.appendData(data);
			return true;
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.obex.OBEXServerOperation#processRequest(javax.obex.HeaderSet, boolean)
	 */
	protected void processRequest(HeaderSet requestHeaders, boolean finalPacket) throws IOException {
		processData(requestHeaders);
		if (finalPacket || (requestHeaders.getHeader(OBEXHeaderSetImpl.OBEX_HDR_BODY_END) != null)) {
			close();
		} else {
			DebugLog.debug("reply continue");
			session.writeOperation(OBEXOperationCodes.OBEX_RESPONSE_CONTINUE, OBEXHeaderSetImpl.toByteArray(sendHeaders));
		}
	}

}
