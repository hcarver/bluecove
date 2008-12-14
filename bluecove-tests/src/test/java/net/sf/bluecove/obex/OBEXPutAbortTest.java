/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

import com.intel.bluetooth.DebugLog;

public class OBEXPutAbortTest extends OBEXBaseEmulatorTestCase {
    
    private int serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
    
    private volatile boolean abortCalled = false;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
    }
    
    private class RequestHandler extends ServerRequestHandler {

        @Override
        public int onPut(Operation op) {
            try {
                serverRequestHandlerInvocations++;
                DebugLog.debug("==TEST== serverRequestHandlerInvocations", serverRequestHandlerInvocations);
                serverHeaders = op.getReceivedHeaders();
                InputStream is = op.openInputStream();
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int data;
                while ((data = is.read()) != -1) {
                    buf.write(data);
                }
                op.close();
                return serverResponseCode;
            } catch (IOException e) {
                if (!abortCalled) {
                    e.printStackTrace();
                }
                return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
            }
        }
    }
    
    @Override
    protected ServerRequestHandler createRequestHandler() {
        return new RequestHandler();
    }
    
    public void testPUTAbort() throws IOException {

        ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
        HeaderSet hsConnectReply = clientSession.connect(null);
        assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

        HeaderSet hsOperation = clientSession.createHeaderSet();
        String name = "Hello.txt";
        hsOperation.setHeader(HeaderSet.NAME, name);

        // Create PUT Operation
        Operation putOperation = clientSession.put(hsOperation);

        // Send some text to server
        OutputStream os = putOperation.openOutputStream();
        for (int i = 0; i < 2024; i++) {
            os.write(i);
        }

        abortCalled = true;
        putOperation.abort();
        
        putOperation.close();

        clientSession.disconnect(null);

        clientSession.close();

        assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
        assertEquals("invocations", 1, serverRequestHandlerInvocations);
    }
    
    public void testPUTAbortReceivedHeaders() throws IOException {

        ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));
        HeaderSet hsConnectReply = clientSession.connect(null);
        assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, hsConnectReply.getResponseCode());

        HeaderSet hsOperation = clientSession.createHeaderSet();
        String name = "Hello.txt";
        hsOperation.setHeader(HeaderSet.NAME, name);

        // Create PUT Operation
        Operation putOperation = clientSession.put(hsOperation);

        // Send some text to server
        OutputStream os = putOperation.openOutputStream();
        for (int i = 0; i < 0x1FFF; i++) {
            os.write(i);
        }
        os.flush();
        
        abortCalled = true;
        putOperation.abort();

        try {
            putOperation.getReceivedHeaders();
            fail("Operation was not closed");
        } catch (IOException e) {
        }
        
        putOperation.close();

        clientSession.disconnect(null);

        clientSession.close();

        assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));
        assertEquals("invocations", 1, serverRequestHandlerInvocations);
    }
    
}
