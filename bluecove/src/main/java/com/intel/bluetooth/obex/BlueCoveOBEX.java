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
 *  Created on Dec 3, 2008
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth.obex;

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * Allow access to BlueCove OBEX internals.  Non JSR-82.
 * <p>
 * <b>PUBLIC JSR-82 extension</b>
 */
public abstract class BlueCoveOBEX {

    private BlueCoveOBEX() {
        
    }
    
    /**
     * Once OBEX connection is made this will return negotiated MTU value.
     * 
     * @return the connection mtu
     */
    public static int getPacketSize(Connection c) {
        if (c instanceof OBEXSessionBase) {
            return ((OBEXSessionBase) c).getPacketSize();
        }
        throw new IllegalArgumentException("Not a BlueCove OBEX Session " + c.getClass().getName());
    }
    
    /**
     * Allows to change the MTU before calling clientSession.connect(headers).
     * Alternative is to use java system property "bluecove.obex.mtu" to define the global value.
     *  
     * @param c the OBEX connection
     * @param mtu
     * @throws IOException
     */
    public static void setPacketSize(Connection c, int mtu) throws IOException {
        if (c instanceof OBEXSessionBase) {
            ((OBEXSessionBase) c).setPacketSize(mtu);
        }
        throw new IllegalArgumentException("Not a BlueCove OBEX Session " + c.getClass().getName());
    }
    
}
