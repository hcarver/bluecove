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
 * This class represents an address local interprocess communication on Unix.
 * 
 * PF_UNIX, AF_UNIX, PF_LOCAL, AF_LOCAL
 */
public class LocalSocketAddress extends java.net.SocketAddress {

    private static final long serialVersionUID = -8958827981306326634L;

    private String name;

    private boolean abstractNamespace;

    /**
     * Create Unix Domain Socket Address using filesystem path
     * 
     * @param path
     *            pathname of the socket in the file system
     */
    public LocalSocketAddress(String path) {
        this(path, false);
    }

    /**
     * Create Unix Domain Socket Address
     * 
     * @param name
     *            pathname of the socket in the file system or unique string in
     *            the abstract namespace
     * @param abstractNamespace
     */
    public LocalSocketAddress(String name, boolean abstractNamespace) {
        super();
        if (name == null) {
            throw new NullPointerException("socket Name is null");
        }
        this.name = name;
        this.abstractNamespace = abstractNamespace;
    }

    /**
     * Filename in the filesystem of name in unix abstract namespace.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Is address filename in the filesystem or a unique string in the
     * abstract namespace.
     * 
     * @return
     */
    public boolean isAbstractNamespace() {
        return abstractNamespace;
    }

    public boolean equals(Object o) {
        if (o instanceof LocalSocketAddress) {
            return (this.abstractNamespace == ((LocalSocketAddress) o).abstractNamespace) && this.name.equals(((LocalSocketAddress) o).name);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.name.hashCode() * 2 ^ (this.abstractNamespace ? 0 : 1);
    }

}
