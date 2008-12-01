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

import javax.obex.ResponseCodes;

/**
 * See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 *
 */
interface OBEXOperationCodes {

	public static final byte OBEX_VERSION = 0x10; /* OBEX Protocol Version 1.1 */

	public static final short OBEX_DEFAULT_MTU = 0x400;

	public static final short OBEX_MINIMUM_MTU = 0xFF;

	public static final short OBEX_MTU_HEADER_RESERVE = 3 + 5 + 3;

	public static final int OBEX_MAX_PACKET_LEN = 0xFFFF;

	public static final char FINAL_BIT = 0x80;

	public static final char CONNECT = 0x00 | FINAL_BIT;

	public static final char DISCONNECT = 0x01 | FINAL_BIT;

	public static final char PUT = 0x02;

	public static final char PUT_FINAL = PUT | FINAL_BIT;

	public static final char GET = 0x03;

	public static final char GET_FINAL = GET | FINAL_BIT;

	public static final char SETPATH = 0x05;

	public static final char SESSION = 0x07;

	public static final char ABORT = 0xFF;

	public static final int OBEX_RESPONSE_CONTINUE = 0x90;

	public static final int OBEX_RESPONSE_SUCCESS = ResponseCodes.OBEX_HTTP_OK;

}
