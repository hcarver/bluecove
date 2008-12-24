/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove;

import org.bluecove.tester.log.Logger;

public class SwitcherTck {

    public static void startTCKAgent() {
        if (Configuration.likedTCKAgent) {
            Switcher.tckRFCOMMThread = new BluetoothTCKAgent.RFCOMMThread("RFCOMMThread");
            if (Switcher.tckRFCOMMThread == null) {
                Logger.info("Due to the License we do not include the TCK agent in distribution");
            } else {
                Switcher.tckRFCOMMThread.start();

                try {
                    String agentMtu = System.getProperty("bluetooth.agent_mtu");
                    String timeout = System.getProperty("timeout");
                    Switcher.tckL2CALthread = new BluetoothTCKAgent.L2CAPThread("L2CAPThread", agentMtu, timeout);
                    if (Switcher.tckL2CALthread != null) {
                        Switcher.tckL2CALthread.start();
                    }
                } catch (Throwable e) {
                    Logger.debug("Fail to start L2CAP", e);
                }

                try {
                    Switcher.tckGOEPThread = new BluetoothTCKAgent.GOEPThread("GOEPThread");
                    if (Switcher.tckGOEPThread != null) {
                        Switcher.tckGOEPThread.start();
                    }
                } catch (Throwable e) {
                    Logger.debug("Fail to start GOEP srv", e);
                }

                try {
                    Switcher.tckOBEXThread = new OBEXTCKAgent.OBEXTCKAgentApp("10", Configuration.testServerOBEX_TCP
                            .booleanValue() ? "tcpobex" : "btgoep");
                    if (Switcher.tckOBEXThread != null) {
                        Switcher.tckOBEXThread.start();
                    }
                } catch (Throwable e) {
                    Logger.debug("Fail to start OBEX srv", e);
                }
            }
        }
    }
}
