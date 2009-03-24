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
 * Socket implementation used for Unix domain sockets on Linux 
 */
class LocalSocketImpl extends java.net.SocketImpl {

    public interface LocalSocketOptions {
        
        public final static int SO_LINGER = 1; 
        
        public final static int SO_PASSCRED = 2;
        
        public final static int SO_SNDBUF = 3;
        
        public final static int SO_RCVBUF = 4;
        
        public final static int SO_RCVTIMEO = 5;

        public final static int SO_SNDTIMEO  = 6;
        
    }
    
    /**
     * socket file descriptor
     */
    private int socket = -1;

    private boolean bound;
    
    private boolean connected;
    
    private boolean closed;
    
    private SocketAddress endpoint;
    
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
        ((LocalSocketImpl)s).connected = true;
        ((LocalSocketImpl)s).endpoint = endpoint;
    }

    @Override
    protected int available() throws IOException {
        return nativeAvailable(socket);
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        throw new UnknownServiceException();
    }

    protected void bind(SocketAddress endpoint) throws IOException {
        if (!(endpoint instanceof LocalSocketAddress)) {
        	throw new IllegalArgumentException("Unsupported address type");
        }
        nativeBind(socket, ((LocalSocketAddress) endpoint).getName(), ((LocalSocketAddress) endpoint).isAbstractNamespace());
        this.bound = true;
        this.endpoint = endpoint;
    }
    
    @Override
    protected void listen(int backlog) throws IOException {
        nativeListen(socket, backlog);
    }
    
    @Override
    protected void close() throws IOException {
    	if (!this.closed) {
    		this.closed = true;
        	nativeClose(socket);
    	}
    	this.bound = false;
    	this.endpoint = null;
    }
    
    void unlink(String path) {
        nativeUnlink(path);
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
        	throw new IllegalArgumentException("Unsupported address type");
        }
        nativeConnect(socket, ((LocalSocketAddress) address).getName(), ((LocalSocketAddress) address).isAbstractNamespace(), timeout);
        this.connected = true;
        this.bound = true;
    }

    public SocketAddress getSocketAddress() {
        return endpoint;
    }
    
    @Override
    protected void create(boolean stream) throws IOException {
    	socket = nativeCreate(stream);
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
    protected void shutdownInput() throws IOException {
        nativeShutdown(socket, true);
    }

    @Override
    protected void shutdownOutput() throws IOException {
        nativeShutdown(socket, false);
    }  

    @Override
    protected void sendUrgentData(int data) throws IOException {
        // TODO Auto-generated method stub
    }

    public Object getOption(int optID) throws SocketException {
        int rc = nativeGetOption(socket, optID);
        return Integer.valueOf(rc);
    }

    public void setOption(int optID, Object value) throws SocketException {
        int nativeValue;
        if (value instanceof Boolean) {
            nativeValue = ((Boolean) value) ? 1 : -1;
        } else if (value instanceof Integer) {
            nativeValue = ((Integer) value).intValue();
        } else {
            throw new IllegalArgumentException();
        }
        nativeSetOption(socket, optID, nativeValue);
    }

    /**
     * Clean up, do not rely on this.
     */
    protected void finalize() throws IOException {
        if (socket > 0) {
            close();
        }
    }
    
    public boolean isCurrentThreadInterruptedCallback() {
		return Thread.interrupted();
	}

    boolean isClosed() {
    	return closed;
    }

    boolean isConnected() {
    	return connected;
    }
    
    boolean isBound() {
    	return bound;
    }
    
    Credentials readPeerCredentials() throws IOException {
        int[] ucred = new int[3];
        nativeReadCredentials(socket, ucred);
        return new Credentials(ucred[0],ucred[2],ucred[2]);
    }
    
    static Credentials readProcessCredentials() {
        int[] ucred = new int[3];
        nativeReadProcessCredentials(ucred);
        return new Credentials(ucred[0],ucred[2],ucred[2]);
    }
    
    private native int nativeCreate(boolean stream) throws IOException;
    
    private native void nativeConnect(int socket, String name, boolean abstractNamespace, int timeout);
    
    private native void nativeBind(int socket, String name, boolean abstractNamespace);
    
    private native void nativeListen(int socket, int backlog);
    
    private native int nativeAccept(int socket) throws IOException;
    
    private native void nativeClose(int socket) throws IOException;

    private native void nativeShutdown(int socket, boolean read) throws IOException;
    
    private native void nativeUnlink(String path);
    
    private native int nativeAvailable(int socket) throws IOException;

    private native int nativeRead(int socket, byte[] buf, int off, int len) throws IOException;

    private native void nativeWrite(int socket, byte[] buf, int off, int len) throws IOException;

    private native void nativeReadCredentials(int socket, int[] buf) throws IOException;
    
    private static native void nativeReadProcessCredentials(int[] buf);
    
    private native void nativeSetOption(int socket, int optID, int value) throws SocketException;
    
    private native int nativeGetOption(int socket, int optID) throws SocketException;
    
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
