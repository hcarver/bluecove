/**
 *  MicroEmulator
 *  Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
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
package com.intel.bluetooth.gcf.socket;

import java.io.IOException;
import java.net.ServerSocket;

import javax.microedition.io.StreamConnection;

public class ServerSocketConnection implements
		javax.microedition.io.ServerSocketConnection {
	
	private ServerSocket serverSocket;
	
	public ServerSocketConnection(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}

	public String getLocalAddress() throws IOException {
		return serverSocket.getInetAddress().toString();
	}

	public int getLocalPort() throws IOException {
		return serverSocket.getLocalPort();
	}

	public StreamConnection acceptAndOpen() throws IOException {
		return new SocketConnection(serverSocket.accept());
	}

	public void close() throws IOException {
		serverSocket.close();
	}
}
