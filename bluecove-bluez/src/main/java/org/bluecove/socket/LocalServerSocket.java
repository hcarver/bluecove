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
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Unix domain server socket.
 * 
 * Inheritance from java.net.ServerSocket is mainly for documentation consistency. 
 */
public class LocalServerSocket extends java.net.ServerSocket {
    
    /**
     * The implementation of this Socket.
     */
    private LocalSocketImpl impl;
    
    public LocalServerSocket() throws IOException {
        super();
        impl = new LocalSocketImpl();
        impl.create(true);
    }
    
    public LocalServerSocket(SocketAddress endpoint) throws IOException {
        this();
        bind(endpoint);
    }
    
    public LocalServerSocket(SocketAddress endpoint, int backlog) throws IOException {
        this();
        bind(endpoint, backlog);
    }
    
    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        impl.bind(endpoint, backlog);
    }
    
    @Override
    public Socket accept() throws IOException {
        LocalSocketImpl clientImpl = new LocalSocketImpl();
        impl.accept(clientImpl);
        return new LocalSocket(clientImpl);
    }
    
}
