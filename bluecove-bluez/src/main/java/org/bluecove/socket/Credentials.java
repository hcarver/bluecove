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

/**
 * Authentication credentials.
 * 
 * The credentials are passed as ancillary message.
 */
public class Credentials {

    /* process ID of the sending process */
    private final int pid;
    
    /* user ID of the sending process */
    private final int uid;
    
    /* group ID of the sending process */
    private final int gid; 

    public Credentials(int pid, int uid, int gid) {
        this.pid = pid;
        this.uid = uid;
        this.gid = gid;
    }
    
    public int getPid() {
        return pid;
    }

    public int getUid() {
        return uid;
    }

    public int getGid() {
        return gid;
    }

    public boolean equals(Object o) {
        if (o instanceof Credentials) {
            return (this.pid == ((Credentials) o).pid) && (this.uid == ((Credentials) o).uid) && (this.gid == ((Credentials) o).gid);
        } else {
            return false;
        }
    }

}
