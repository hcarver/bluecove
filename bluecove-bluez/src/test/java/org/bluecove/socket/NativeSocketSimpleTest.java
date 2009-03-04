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


public class NativeSocketSimpleTest extends NativeSocketTestCase {

	private final boolean socketAbstractNamespace = true;
	
    private final String socketName = "target/test-sock_name";
    
    @Override
    protected void setUp() throws Exception {
    	//debug = true;
    	super.setUp();
    }
    
    protected TestRunnable createTestServer() {
        
        return new TestRunnable() {
            public void run() throws Exception {
                LocalServerSocket serverSocket = new LocalServerSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
                try {
                	System.out.println("server starts");
                	synchronized (serverAcceptEvent) {
                		serverAccepts = true;
                		serverAcceptEvent.notifyAll();
                    }
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    int len = in.read();
                    out.write(len);
                    for (int i = 0; i < len; i++) {
                        out.write(in.read());
                    }
                    Thread.sleep(500);
                    in.close();
                    out.close();
                    socket.close();
                } finally {
                    serverSocket.close();
                    System.out.println("server ends");
                }
            }
        };
    }
    
    public void testOneByte() throws Exception  {
    	while (!serverAccepts) {
    		synchronized (serverAcceptEvent) {
    			serverAcceptEvent.wait(500);
    		}
    		assertServerErrors();
        }
    	Thread.sleep(200);
        Socket socket = new LocalSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
        System.out.println("client connected");
        OutputStream out = socket.getOutputStream();
        out.write(1);
        out.write(2);
        InputStream in = socket.getInputStream();
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(-1, in.read());
        in.close();
        out.close();
        socket.close();
        assertServerErrors();
    }
    
}
