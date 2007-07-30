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

import javax.microedition.io.Connection;
import javax.microedition.io.StreamConnection;
import javax.obex.Authenticator;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.UtilsJavaSE;

public class OBEXServerSessionImpl extends OBEXSessionBase implements Connection, Runnable {

	private ServerRequestHandler handler;
	
	private Authenticator auth;
	
	public OBEXServerSessionImpl(StreamConnection connection, ServerRequestHandler handler, Authenticator auth) throws IOException {
		super(connection);
		this.handler = handler;
		this.auth = auth;
		Thread t = new Thread(this);
		UtilsJavaSE.threadSetDaemon(t);
		t.start();
	}


	public void run() {
		try {
			while (!isClosed()) {
				if (processOperation()) {
					break;
				}
			}
		} catch (Throwable e) {
			DebugLog.error("OBEX connection error", e);
		} finally {
			try {
				super.close();
			} catch (IOException e) {
				DebugLog.error("close error", e);
			}
		}
	}

	public void close() throws IOException {
		super.close();
	}
	

	private boolean processOperation() throws IOException {
		boolean isEOF = false;
		byte[] b = readOperation();
		
		return isEOF;
	}
}
