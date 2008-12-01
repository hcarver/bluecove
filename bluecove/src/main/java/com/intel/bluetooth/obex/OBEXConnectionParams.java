/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.obex;

/**
 * @since bluecove 2.0.2
 */
public class OBEXConnectionParams {

	public static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;

	/**
	 * Enables timeouts.
	 *
	 * @see javax.microedition.io.Connector#open(String,int,boolean)
	 */
	public boolean timeouts;

	/**
	 * The amount of time in milliseconds for which the implementation will
	 * attempt to successfully transmit a packet before it throws
	 * InterruptedIOException.
	 *
	 * Java System property "bluecove.obex.timeout" can be used to define the
	 * value.
	 */
	public int timeout = DEFAULT_TIMEOUT;

	/**
	 * You can increase network speed by changing mtu to bigger value.
	 *
	 * Java System property "bluecove.obex.mtu" can be used to define the value.
	 */
	public int mtu = OBEXOperationCodes.OBEX_DEFAULT_MTU;
}
