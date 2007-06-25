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

import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

public class OBEXClientSessionImpl implements ClientSession {

	public OBEXClientSessionImpl(long address, int channel, boolean authenticate,	boolean encrypt) throws IOException {
		

	}
	public HeaderSet connect(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet createHeaderSet() {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet disconnect(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Operation get(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public long getConnectionID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Operation put(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAuthenticator(Authenticator auth) {
		// TODO Auto-generated method stub

	}

	public void setConnectionID(long id) {
		// TODO Auto-generated method stub

	}

	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

}
