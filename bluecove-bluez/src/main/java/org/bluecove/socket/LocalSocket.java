/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
 *  @author vlads
 *  @version $Id$
 */
package org.bluecove.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/**
 * Unix domain client socket.
 */
public class LocalSocket extends java.net.Socket {

	/**
	 * The implementation of this Socket.
	 */
	private LocalSocketImpl impl;

	LocalSocket() throws IOException {
		super((SocketImpl) null);
		impl = new LocalSocketImpl();
		impl.create(true);
	}

	public LocalSocket(LocalSocketAddress address) throws IOException {
		this();
		connect(address);
	}

	LocalSocket(LocalSocketImpl impl) throws IOException {
		super((SocketImpl) null);
		this.impl = impl;
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is already closed");
		}
		if (isConnected()) {
			throw new SocketException("Socket is already connected");
		}
		impl.connect(endpoint, timeout);
	}

	public void close() throws IOException {
		impl.close();
	}

	@Override
	public boolean isConnected() {
		return impl.isConnected();
	}

	@Override
	public boolean isBound() {
		return impl.isBound();
	}

	@Override
	public boolean isClosed() {
		return impl.isClosed();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is already closed");
		}
		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}
		if (isOutputShutdown()) {
			throw new SocketException("Socket output is shutdown");
		}
		return impl.getOutputStream();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is already closed");
		}
		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}
		if (isInputShutdown()) {
			throw new SocketException("Socket input is shutdown");
		}
		return impl.getInputStream();
	}
}
