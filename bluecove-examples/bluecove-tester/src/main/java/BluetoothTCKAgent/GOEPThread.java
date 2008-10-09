/*
 *  $HeadURL$
 *
 *
 *  Copyright (c) 2001-2008 Motorola, Inc.  All rights reserved. 
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  Revision History:
 *
 *  Date             Author                   Comment
 *  ---------------------------------------------------------------------------------
 *  Oct 15,2006      Motorola, Inc.           Initial creation
 *
 */


package BluetoothTCKAgent;

import java.io.*;

import BluetoothTCKAgent.Connector;
import javax.microedition.io.*;
import javax.obex.*;



/**
 * This class tests the OBEX server side operations.
 */
public class GOEPThread extends Thread {

    protected boolean canRun = true;
    SessionNotifier server = null;
    Connection session = null;
    RequestHandlerImpl handler;

    public GOEPThread(String str) {
        super(str);
        System.out.println("GOEPThread: Starting GOEP Service");

        try {
            server = (SessionNotifier)Connector.open(
                "btgoep://localhost:3000000031b811d88698000874b33fc0");
        } catch (Exception e) {
            System.out.println("GOEPThread: Unable to start" +
                                " GOEP Service. Aborting.");
            System.out.println("GOEPThread: GOEP Tests Cannot be Run");
            canRun = false;
        }

        handler = new RequestHandlerImpl();
    }

    public void run() {
        while (canRun) {
            try {
                System.out.println("GOEPThread: Waiting for Client" +
                                    " to Connect");
                session = server.acceptAndOpen(handler);
                System.out.println("GOEPThread: Client made " +
                                                    "a Connection");
                TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
                System.out.println("GOEPThread: closing session");
                session.close();
            } catch (InterruptedIOException e) {
            	System.out.println("GOEPThread:TCK Interrupted");
            	return;
            } catch (Exception e) {
                System.out.println("GOEPThread: Error occured when " +
                                   "connecting with client: " + e);
                if ("Stack closed".equals(e.getMessage())) {
                	return;
                }
                try {
                    session.close();
                } catch(Exception ex) {
                }
            }
            canRun = true;
        } // while(canRun)
    } //run()

    class RequestHandlerImpl extends ServerRequestHandler {

        public RequestHandlerImpl() {
            super();

        }

        public int onConnect(HeaderSet headers, HeaderSet reply) {
            String testName = null;

            try {
                testName = (String)headers.getHeader(HeaderSet.NAME);
            } catch (IOException e) {
                System.out.println("GOEPThread: Error occured when " +
                                    "decoding client message");
            }

            if (testName == null) {
                System.out.println("GOEPThread: NULL HeaderSet " +
                                            "sent by client");
            }

            if (testName.startsWith("CLIENT ")) {
                System.out.println("GOEPThread: CLIENT command " +
                                            "sent by client");
                String url = testName.substring(testName.indexOf(" "));
                GOEPClientThread clientthread = new GOEPClientThread(url);
                clientthread.start();
            }

            return ResponseCodes.OBEX_HTTP_OK;
        }

        public void onDisconnect(HeaderSet headers, HeaderSet reply) {
            System.out.println("GOEPThread: Client requested to " +
                                "disconnect. Disconnecting.");

            return;
        }
    } // class RequestHandlerImpl
} // class OBEXServer
