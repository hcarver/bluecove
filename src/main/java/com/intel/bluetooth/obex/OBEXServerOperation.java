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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.obex.HeaderSet;
import javax.obex.Operation;

/**
 * @author vlads
 *
 */
abstract class OBEXServerOperation implements Operation {

	protected OBEXServerSessionImpl session;
	
	protected HeaderSet receivedHeaders;
	
	protected HeaderSet sendHeaders;
	
	protected boolean isClosed = false;

	protected OBEXServerOperation(OBEXServerSessionImpl session, HeaderSet receivedHeaders) {
		this.session = session;
		this.receivedHeaders = receivedHeaders;
	}
	
	/* (non-Javadoc)
	 * @see javax.obex.Operation#abort()
	 */
	public void abort() throws IOException {
		throw new IOException("Can't abort server operation");
	}

	/* (non-Javadoc)
	 * @see javax.obex.Operation#getReceivedHeaders()
	 */
	public HeaderSet getReceivedHeaders() throws IOException {
		return receivedHeaders;
	}

	/* (non-Javadoc)
	 * @see javax.obex.Operation#getResponseCode()
	 */
	public int getResponseCode() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.obex.Operation#sendHeaders(javax.obex.HeaderSet)
	 */
	public void sendHeaders(HeaderSet headers) throws IOException {
		sendHeaders = headers;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.ContentConnection#getEncoding()
	 */
	public String getEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.ContentConnection#getLength()
	 */
	public long getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.ContentConnection#getType()
	 */
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.InputConnection#openDataInputStream()
	 */
	public DataInputStream openDataInputStream() throws IOException {
		 return new DataInputStream(openInputStream());
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.OutputConnection#openDataOutputStream()
	 */
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		this.isClosed = true;
	}

	boolean isClosed() {
		return this.isClosed;
	}
}
