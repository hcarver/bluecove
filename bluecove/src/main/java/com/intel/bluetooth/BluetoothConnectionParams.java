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
package com.intel.bluetooth;

/**
 *
 *
 */
class BluetoothConnectionParams {

	public static final int DEFAULT_CONNECT_TIMEOUT = 2 * 60 * 1000;

	long address;

	int channel;

	boolean authenticate;

	boolean encrypt;

	/**
	 * Enables timeouts. Not used now, timeouts always enabled in connections
	 *
	 * @see javax.microedition.io.Connector#open(String,int,boolean)
	 */
	boolean timeouts;

	/**
	 * The amount of time in milliseconds for which the implementation will
	 * attempt to establish connection RFCOMM or L2CAP before it throws
	 * BluetoothConnectionException.
	 *
	 * Java System property "bluecove.connect.timeout" can be used to define the
	 * value.
	 */
	public int timeout = DEFAULT_CONNECT_TIMEOUT;

	public BluetoothConnectionParams(long address, int channel, boolean authenticate, boolean encrypt) {
		super();
		this.address = address;
		this.channel = channel;
		this.authenticate = authenticate;
		this.encrypt = encrypt;
	}
}
