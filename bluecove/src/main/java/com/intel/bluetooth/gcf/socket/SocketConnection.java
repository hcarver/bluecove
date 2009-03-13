/**
 *  MicroEmulator
 *  Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketConnection implements javax.microedition.io.SocketConnection {

	protected Socket socket;

	public SocketConnection() {
	}

	public SocketConnection(String host, int port) throws IOException {
		this.socket = new Socket(host, port);
	}

	public SocketConnection(Socket socket) {
		this.socket = socket;
	}

	public String getAddress() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getInetAddress().toString();
	}

	public String getLocalAddress() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getLocalAddress().toString();
	}

	public int getLocalPort() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getLocalPort();
	}

	public int getPort() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getPort();
	}

	public int getSocketOption(byte option) throws IllegalArgumentException,
			IOException {
		if (socket != null && socket.isClosed()) {
			throw new IOException();
		}
		switch (option) {
		case DELAY:
			if (socket.getTcpNoDelay()) {
				return 1;
			} else {
				return 0;
			}
		case LINGER:
			int value = socket.getSoLinger();
			if (value == -1) {
				return 0;
			} else {
				return value;
			}
		case KEEPALIVE:
			if (socket.getKeepAlive()) {
				return 1;
			} else {
				return 0;
			}
		case RCVBUF:
			return socket.getReceiveBufferSize();
		case SNDBUF:
			return socket.getSendBufferSize();
		default:
			throw new IllegalArgumentException();
		}
	}

	public void setSocketOption(byte option, int value)
			throws IllegalArgumentException, IOException {
		if (socket.isClosed()) {
			throw new IOException();
		}
		switch (option) {
		case DELAY:
			int delay;
			if (value == 0) {
				delay = 0;
			} else {
				delay = 1;
			}
			socket.setTcpNoDelay(delay == 0 ? false : true);
			break;
		case LINGER:
			if (value < 0) {
				throw new IllegalArgumentException();
			}
			socket.setSoLinger(value == 0 ? false : true, value);
			break;
		case KEEPALIVE:
			int keepalive;
			if (value == 0) {
				keepalive = 0;
			} else {
				keepalive = 1;
			}
			socket.setKeepAlive(keepalive == 0 ? false : true);
			break;
		case RCVBUF:
			if (value <= 0) {
				throw new IllegalArgumentException();
			}
			socket.setReceiveBufferSize(value);
			break;
		case SNDBUF:
			if (value <= 0) {
				throw new IllegalArgumentException();
			}
			socket.setSendBufferSize(value);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	public void close() throws IOException {
		// TODO fix differences between Java ME and Java SE

		socket.close();
	}

	public InputStream openInputStream() throws IOException {
		return socket.getInputStream();
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	public OutputStream openOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}
}
