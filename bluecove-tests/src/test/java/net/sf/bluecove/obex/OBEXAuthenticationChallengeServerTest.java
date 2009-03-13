/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.PasswordAuthentication;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;

import net.sf.bluecove.obex.OBEXAuthenticationBaseTestCase.ChallengeData;
import net.sf.bluecove.obex.OBEXAuthenticationBaseTestCase.When;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.obex.BlueCoveOBEX;

public class OBEXAuthenticationChallengeServerTest extends OBEXAuthenticationBaseTestCase {

    private byte[] serverData;

    private volatile ChallengeData clientChallenge;

    private volatile ChallengeData serverChallenge;

    private volatile int onServerAuthenticationFailureCalled;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverData = null;
        serverChallenge = null;
        clientChallenge = new ChallengeData("CliTest", true, true);
        onServerAuthenticationFailureCalled = 0;
    }

    private class RequestHandler extends ServerRequestHandler {

        @Override
        public int onConnect(HeaderSet request, HeaderSet reply) {
            DebugLog.debug("==TEST== Server onConnect");
            return ResponseCodes.OBEX_HTTP_OK;
        }

        @Override
        public int onPut(Operation op) {
            try {
                serverRequestHandlerInvocations++;
                DebugLog.debug("==TEST== Server onPut serverRequestHandlerInvocations", serverRequestHandlerInvocations);
                serverHeaders = op.getReceivedHeaders();
                InputStream is = op.openInputStream();
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int data;
                while ((data = is.read()) != -1) {
                    buf.write(data);
                }
                serverData = buf.toByteArray();
                DebugLog.debug("==TEST== Server close Operation");
                op.close();
                op = null;
                serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
                DebugLog.debug("==TEST== Server returns " + BlueCoveOBEX.obexResponseCodes(serverResponseCode));
                return serverResponseCode;
            } catch (IOException e) {
                DebugLog.error("==TEST== Server", e);
                return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
            } finally {
                try {
                    if (op != null) {
                        op.close();
                    }
                } catch (IOException e) {
                    DebugLog.error("==TEST== Server close", e);
                    return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
                }
            }
        }

        @Override
        public int onGet(Operation op) {
            try {
                serverRequestHandlerInvocations++;
                DebugLog.debug("==TEST== Server onGet serverRequestHandlerInvocations", serverRequestHandlerInvocations);
                serverHeaders = op.getReceivedHeaders();
                serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
                DebugLog.debug("==TEST== Server returns " + BlueCoveOBEX.obexResponseCodes(serverResponseCode));
                return serverResponseCode;
            } catch (IOException e) {
                DebugLog.error("==TEST== Server", e);
                return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
            } finally {
                try {
                    op.close();
                } catch (IOException e) {
                    DebugLog.error("==TEST== Server close", e);
                    return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
                }
            }
        }

        public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {
            serverRequestHandlerInvocations++;
            serverHeaders = request;
            serverResponseCode = ResponseCodes.OBEX_HTTP_OK;
            DebugLog.debug("==TEST== Server returns " + BlueCoveOBEX.obexResponseCodes(serverResponseCode));
            return serverResponseCode;
        }

        public void onAuthenticationFailure(byte[] userName) {
            DebugLog.debug("==TEST== Server onAuthenticationFailure");
            onServerAuthenticationFailureCalled++;
        }
    }

    private class ServerRequestAuthenticator implements Authenticator {

        ServerRequestAuthenticator(ServerRequestHandler handler) {

        }

        public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired, boolean isFullAccess) {
            serverOnAuthenticationChallengeCalled++;
            authenticatorCalled = now;
            DebugLog.debug("==TEST== Server challenge " + (isUserIdRequired ? "U" : "") + (isFullAccess ? "A" : "") + " " + description);
            serverChallenge = new ChallengeData(description, isUserIdRequired, isFullAccess);
            byte[] password;
            if (isUserIdRequired) {
                password = getUserPassword(userName);
            } else {
                password = getUserPassword((String) null);
            }
            return new PasswordAuthentication(userName.getBytes(), password);
        }

        public byte[] onAuthenticationResponse(byte[] userName) {
            serverOnAuthenticationResponseCalled++;
            DebugLog.debug("==TEST== Server authenticate user", (userName != null) ? new String(userName) : "{null}");
            return getUserPassword(userName);
        }

    }

    @Override
    protected Authenticator getServerAuthenticator(ServerRequestHandler handler) {
        return new ServerRequestAuthenticator(handler);
    }

    @Override
    protected ServerRequestHandler createRequestHandler() {
        return new RequestHandler();
    }

    private void createClientAuthenticationChallenge(HeaderSet hs) {
        hs.createAuthenticationChallenge(clientChallenge.realm, clientChallenge.userID, clientChallenge.access);
    }

    private void runPUTOperation(final When whenChallenge, boolean empty) throws IOException {

        now = When.Never;

        authenticatorCalled = When.Never;

        Authenticator auth = new Authenticator() {

            public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired, boolean isFullAccess) {
                clientOnAuthenticationChallengeCalled++;
                return null;
            }

            public byte[] onAuthenticationResponse(byte[] userName) {
                clientOnAuthenticationResponseCalled++;
                DebugLog.debug("==TEST== Client authenticate user", (userName != null) ? new String(userName) : "{null}");
                return getUserPassword(userName);
            }
        };

        ClientSession clientSession = (ClientSession) Connector.open(selectService(serverUUID));

        clientSession.setAuthenticator(auth);
        HeaderSet hsConnect = clientSession.createHeaderSet();
        hsConnect.setHeader(testHeaderID, new Long(whenChallenge.ordinal()));
        now = When.onConnect;
        if (whenChallenge == now) {
            createClientAuthenticationChallenge(hsConnect);
        }
        HeaderSet hsConnectReply = clientSession.connect(hsConnect);
        int connectCode = hsConnectReply.getResponseCode();
        DebugLog.debug0x("==TEST== Client connect ResponseCode " + BlueCoveOBEX.obexResponseCodes(connectCode) + " = ", connectCode);
        assertEquals("connect", ResponseCodes.OBEX_HTTP_OK, connectCode);

        HeaderSet hsOperation = clientSession.createHeaderSet();
        String name = "Hello.txt";
        hsOperation.setHeader(HeaderSet.NAME, name);
        hsOperation.setHeader(testHeaderID, new Long(whenChallenge.ordinal()));

        int responseCode;
        if (whenChallenge == When.onGet) {
            now = When.onGet;
            createClientAuthenticationChallenge(hsOperation);
            Operation getOperation = clientSession.get(hsOperation);

            DebugLog.debug("==TEST== Client getResponseCode");
            responseCode = getOperation.getResponseCode();
            DebugLog.debug0x("==TEST== Client GET ResponseCode " + BlueCoveOBEX.obexResponseCodes(responseCode) + " = ", responseCode);

            getOperation.close();

        } else if (whenChallenge == When.onSetPath) {
            createClientAuthenticationChallenge(hsOperation);
            now = When.onSetPath;
            HeaderSet reply = clientSession.setPath(hsOperation, true, true);
            responseCode = reply.getResponseCode();
        } else {
            now = When.onPut;
            if (whenChallenge == When.onPut) {
                createClientAuthenticationChallenge(hsOperation);
            }
            // Create PUT Operation
            Operation putOperation = clientSession.put(hsOperation);

            // Send some text to server
            byte data[] = new byte[0];
            OutputStream os = putOperation.openOutputStream();
            if (!empty) {
                data = simpleData;
                os.write(data);
            }
            os.close();
            DebugLog.debug("==TEST== Client getResponseCode");
            responseCode = putOperation.getResponseCode();
            DebugLog.debug0x("==TEST== Client PUT ResponseCode " + BlueCoveOBEX.obexResponseCodes(responseCode) + " = ", responseCode);

            putOperation.close();

            assertEquals("data", data, serverData);
        }

        now = When.Never;

        clientSession.disconnect(null);

        clientSession.close();

        assertEquals("NAME", name, serverHeaders.getHeader(HeaderSet.NAME));

        int invocationsExpected = 1;
        assertEquals("invocations", invocationsExpected, serverRequestHandlerInvocations);

        assertEquals("onAuthenticationChallenge", whenChallenge.name(), authenticatorCalled.name());

        if (When.Never != whenChallenge) {
            assertEquals("Challenge.realm", serverChallenge.realm, clientChallenge.realm);
            assertEquals("Challenge.userID", serverChallenge.userID, clientChallenge.userID);
            assertEquals("Challenge.access", serverChallenge.access, clientChallenge.access);
        }

        int challengeCalledExpected = 1;
        if (When.Never == whenChallenge) {
            challengeCalledExpected = 0;
        }

        assertEquals("serverOnAuthenticationResponseCalled", 0, serverOnAuthenticationResponseCalled);

        assertEquals("clientOnAuthenticationChallengeCalled", 0, clientOnAuthenticationChallengeCalled);

        assertEquals("serverOnAuthenticationChallengeCalled", challengeCalledExpected, serverOnAuthenticationChallengeCalled);
        assertEquals("clientOnAuthenticationResponseCalled", challengeCalledExpected, clientOnAuthenticationResponseCalled);
        
        assertEquals("ResponseCodes." + BlueCoveOBEX.obexResponseCodes(serverResponseCode), serverResponseCode, responseCode);
        assertServerErrors();

    }

    public void testNoChallenge() throws IOException {
        runPUTOperation(When.Never, false);
    }

    public void testChallengeOnConnect() throws IOException {
        runPUTOperation(When.onConnect, false);
    }

    public void testChallengeOnConnectFalseFalse() throws IOException {
        clientChallenge = new ChallengeData("RealmFalseFalse", false, false);
        runPUTOperation(When.onConnect, false);
    }

    public void testChallengeOnPut() throws IOException {
        runPUTOperation(When.onPut, false);
    }

    public void testChallengeOnPutEmpty() throws IOException {
        runPUTOperation(When.onPut, true);
    }

    public void testChallengeOnGet() throws IOException {
        runPUTOperation(When.onGet, false);
    }

    public void testChallengeOnSetPath() throws IOException {
        runPUTOperation(When.onSetPath, false);
    }

}
