/**
 *  MicroEmulator
 *  Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @version $Id$
 */
package com.intel.bluetooth.gcf.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.microedition.io.StreamConnection;

public class ServerSocketConnection implements
		javax.microedition.io.ServerSocketConnection {

	private ServerSocket serverSocket;

	public ServerSocketConnection(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}

	public String getLocalAddress() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();
		return localHost.getHostAddress();
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
