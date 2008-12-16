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
 *  @author vlads
 *
 *  @version $Id$
 */
package com.intel.bluetooth.obex;

import javax.microedition.io.Connection;

/**
 * 
 */
public abstract class BlueCoveInternals {

	public static boolean isShortRequestPhase() {
		return OBEXClientOperation.isShortRequestPhase();
	}

	public static int readServerErrorCount() {
		synchronized (OBEXServerSessionImpl.class) {
			int count = OBEXServerSessionImpl.errorCount;
			OBEXServerSessionImpl.errorCount = 0;
			return count;
		}

	}

	/**
	 * @return the packetsCountWrite
	 */
	public static int getPacketsCountWrite(Connection c) {
		if (c instanceof OBEXSessionBase) {
			return ((OBEXSessionBase) c).getPacketsCountWrite();
		}
		throw new IllegalArgumentException("Not a BlueCove OBEX Session " + c.getClass().getName());
	}

	/**
	 * @return the packetsCountRead
	 */
	public static int getPacketsCountRead(Connection c) {
		if (c instanceof OBEXSessionBase) {
			return ((OBEXSessionBase) c).getPacketsCountRead();
		}
		throw new IllegalArgumentException("Not a BlueCove OBEX Session " + c.getClass().getName());
	}

	/**
	 * 
	 * @return the mtu
	 */
	public static int getPacketSize(Connection c) {
		if (c instanceof OBEXSessionBase) {
			return ((OBEXSessionBase) c).getPacketSize();
		}
		throw new IllegalArgumentException("Not a BlueCove OBEX Session " + c.getClass().getName());
	}
}
