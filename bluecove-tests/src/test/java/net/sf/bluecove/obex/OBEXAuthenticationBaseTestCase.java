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

import javax.obex.ResponseCodes;

public abstract class OBEXAuthenticationBaseTestCase extends OBEXBaseEmulatorTestCase {

    protected String userName = "bob";

    protected static String userPasswordSufix = "_Pwd";

    protected int serverResponseCode;

    protected volatile int serverOnAuthenticationChallengeCalled;

    protected volatile int serverOnAuthenticationResponseCalled;

    protected volatile int clientOnAuthenticationChallengeCalled;

    protected volatile int clientOnAuthenticationResponseCalled;

    protected final int testHeaderID = 0xF0;

    protected enum When {
        Never, onConnect, onPut, onGet, onSetPath
    }

    protected volatile When now;

    protected volatile When authenticatorCalled;

    protected class ChallengeData {

        String realm;

        boolean userID;

        boolean access;

        ChallengeData() {
            this(null, false, false);
        }

        ChallengeData(String realm, boolean userID, boolean access) {
            this.realm = realm;
            this.userID = userID;
            this.access = access;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverResponseCode = ResponseCodes.OBEX_HTTP_OK;

        serverOnAuthenticationResponseCalled = 0;
        serverOnAuthenticationChallengeCalled = 0;
        clientOnAuthenticationResponseCalled = 0;
        clientOnAuthenticationChallengeCalled = 0;
        now = When.Never;
        authenticatorCalled = When.Never;
    }

    protected static byte[] getUserPassword(byte[] userName) {
        if (userName == null) {
            return getUserPassword((String) null);
        } else {
            return getUserPassword(new String(userName));
        }
    }

    protected static byte[] getUserPassword(String userName) {
        if (userName == null) {
            return "secret".getBytes();
        } else {
            return (userName + userPasswordSufix).getBytes();
        }
    }

}
