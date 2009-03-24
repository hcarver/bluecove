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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Tests that not applicable java.net.ServerSocket functions throw exception
 */
public class NativeSocketOverridesTest extends NativeSocketTestCase {

    private final boolean socketAbstractNamespace = true;
    
    private final String socketName = "target/test-sock_name";

    
    @Override
    protected TestRunnable createTestServer() {
        
        return new TestRunnable() {
            public void run() throws Exception {
                LocalServerSocket serverSocket = new LocalServerSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
                try {
                    serverAcceptsNotifyAll();
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    int len = in.read();
                    out.write(len);
                    Thread.sleep(500);
                    in.close();
                    out.close();
                    socket.close();
                } finally {
                    serverSocket.close();
                }
            }
        };
    }
    
    public void testOverride() throws Exception  {
        serverAcceptsWait();
        Socket socket = new LocalSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
        OutputStream out = socket.getOutputStream();
        out.write(1);
        
        assertNull(socket.getChannel());
        try {
            socket.getInetAddress();
            fail("getInetAddress");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            socket.getLocalAddress();
            fail("getLocalAddress");
        } catch (IllegalArgumentException e) {
        }

        try {
            socket.getPort();
            fail("getPort");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            socket.getLocalPort();
            fail("getLocalPort");
        } catch (IllegalArgumentException e) {
        }
        
        InputStream in = socket.getInputStream();
        assertEquals(1, in.read());
        assertEquals(-1, in.read());
        in.close();
        out.close();
        socket.close();
        assertServerErrors();
    }
}
