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

import org.bluecove.socket.LocalSocketImpl.LocalSocketOptions;

/**
 * Unix domain client socket on Linux.
 */
public class LocalSocket extends java.net.Socket {

    /**
     * The implementation of this Socket.
     */
    private LocalSocketImpl impl;

    public LocalSocket() throws IOException {
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

    public Credentials getPeerCredentials() throws IOException {
        return impl.readPeerCredentials();
    }

    public static Credentials getProcessCredentials() {
        return LocalSocketImpl.readProcessCredentials();
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

    /**
     * Enable/disable the SO_LINGER socket option.
     * 
     * @param on
     */
    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is already closed");
        }
        if (!on) {
            impl.setOption(LocalSocketOptions.SO_LINGER, Boolean.valueOf(on));
        } else {
            if (linger < 0) {
                throw new IllegalArgumentException("invalid value for SO_LINGER");
            }
            if (linger > 65535) {
                linger = 65535;
            }
            impl.setOption(LocalSocketOptions.SO_LINGER, Integer.valueOf(linger));
        }
    }

    /**
     * Returns setting for SO_LINGER. -1 returns disabled.
     */
    @Override
    public int getSoLinger() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is already closed");
        }
        Object value = impl.getOption(LocalSocketOptions.SO_LINGER);
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        } else {
            return -1;
        }
    }
    
    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        impl.setOption(LocalSocketOptions.SO_RCVBUF, Integer.valueOf(size));
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return ((Integer) impl.getOption(LocalSocketOptions.SO_RCVBUF)).intValue();
    }

    @Override
    public void setSendBufferSize(int n) throws SocketException {
        impl.setOption(LocalSocketOptions.SO_SNDBUF, Integer.valueOf(n));
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return ((Integer) impl.getOption(LocalSocketOptions.SO_SNDBUF)).intValue();
    }

    /**
     * Set SO_RCVTIMEO and SO_SNDTIMEO in milliseconds
     */
    @Override
    public void setSoTimeout(int n) throws SocketException {
        impl.setOption(LocalSocketOptions.SO_RCVTIMEO, Integer.valueOf(n));
        impl.setOption(LocalSocketOptions.SO_SNDTIMEO, Integer.valueOf(n));
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return ((Integer) impl.getOption(LocalSocketOptions.SO_SNDTIMEO)).intValue();
    }
    
    /**
     * Set SO_RCVTIMEO in milliseconds
     */
    public void setSoReceiveTimeout(int n) throws SocketException {
        impl.setOption(LocalSocketOptions.SO_RCVTIMEO, Integer.valueOf(n));
    }

    public int getSoReceiveTimeout() throws SocketException {
        return ((Integer) impl.getOption(LocalSocketOptions.SO_RCVTIMEO)).intValue();
    }

    /**
     * Set SO_SNDTIMEO in milliseconds
     */
    public void setSoSendTimeout(int n) throws SocketException {
        impl.setOption(LocalSocketOptions.SO_SNDTIMEO, Integer.valueOf(n));
    }

    public int getSoSendTimeout() throws SocketException {
        return ((Integer) impl.getOption(LocalSocketOptions.SO_SNDTIMEO)).intValue();
    }
    
    
    /**
     * Enable/disable the SO_PASSCRED socket option.
     * 
     * @param on
     * @throws SocketException 
     */
    public void setReceiveCredentials(boolean on) throws SocketException  {
        if (isClosed()) {
            throw new SocketException("Socket is already closed");
        }
        impl.setOption(LocalSocketOptions.SO_PASSCRED, Integer.valueOf((on?1:0)));
    }
    
    public boolean getReceiveCredentials() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is already closed");
        }
        Object value = impl.getOption(LocalSocketOptions.SO_PASSCRED);
        if (value instanceof Integer) {
            return (((Integer) value).intValue() > 0);
        } else {
            return false;
        }
    }
}
