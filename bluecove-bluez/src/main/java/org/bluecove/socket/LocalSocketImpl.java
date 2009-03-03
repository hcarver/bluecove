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
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.UnknownServiceException;

/**
 * Socket implementation used for Unix domain sockets
 */
class LocalSocketImpl extends java.net.SocketImpl {

    /**
     * socket file descriptor
     */
    private int socket;

    private InputStream in;

    private OutputStream out;

    LocalSocketImpl() {

    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        if (!(s instanceof LocalSocketImpl)) {
            throw new UnknownServiceException();
        }
        ((LocalSocketImpl)s).socket = nativeAccept(this.socket);
    }

    @Override
    protected int available() throws IOException {
        return nativeAvailable(socket);
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        throw new UnknownServiceException();
    }

    protected void bind(SocketAddress endpoint, int backlog) throws IOException {
        if (!(endpoint instanceof LocalSocketAddress)) {
            throw new UnknownServiceException();
        }
        nativeBind(socket, ((LocalSocketAddress) endpoint).getName(), ((LocalSocketAddress) endpoint).isAbstractNamespace(), backlog);
    }
    
    @Override
    protected void close() throws IOException {
        nativeClose(socket);
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        throw new UnknownServiceException();
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        throw new UnknownServiceException();
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        if (!(address instanceof LocalSocketAddress)) {
            throw new UnknownServiceException();
        }
        socket = nativeConnect(((LocalSocketAddress) address).getName(), ((LocalSocketAddress) address).isAbstractNamespace(), timeout);
    }

    @Override
    protected void create(boolean stream) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected InputStream getInputStream() throws IOException {
        if (in == null) {
            in = new LocalSocketInputStream();
        }
        return in;
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new LocalSocketOutputStream();
        }
        return out;
    }

    @Override
    protected void listen(int backlog) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        // TODO Auto-generated method stub

    }

    public Object getOption(int optID) throws SocketException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setOption(int optID, Object value) throws SocketException {
        // TODO Auto-generated method stub
    }

    /**
     * Clean up, do not rely on this.
     */
    protected void finalize() throws IOException {
        if (socket > 0) {
            close();
        }
    }

    private native int nativeConnect(String name, boolean abstractNamespace, int timeout);
    
    private native void nativeBind(int socket, String name, boolean abstractNamespace, int backlog);
    
    private native int nativeAccept(int socket) throws IOException;
    
    private native void nativeClose(int socket) throws IOException;

    private native int nativeAvailable(int socket) throws IOException;

    private native int nativeRead(int socket, byte[] buf, int off, int len) throws IOException;

    private native void nativeWrite(int socket, byte[] buf, int off, int len) throws IOException;

    private class LocalSocketInputStream extends InputStream {

        @Override
        public int available() throws IOException {
            return nativeAvailable(socket);
        }

        @Override
        public int read() throws IOException {
            byte[] data = new byte[1];
            int size = nativeRead(socket, data, 0, 1);
            if (size == -1) {
                return -1;
            }
            return 0xFF & data[0];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            return nativeRead(socket, b, off, len);
        }

    }

    private class LocalSocketOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            byte[] buf = new byte[1];
            buf[0] = (byte) (b & 0xFF);
            nativeWrite(socket, buf, 0, 1);
        }

        @Override
        public void write(byte[] buf) throws IOException {
            if (buf == null) {
                throw new NullPointerException();
            }
            nativeWrite(socket, buf, 0, buf.length);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            if (off < 0 || len < 0 || off + len > buf.length) {
                throw new IndexOutOfBoundsException();
            }
            nativeWrite(socket, buf, off, len);
        }

    }
}
