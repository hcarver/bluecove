/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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
 *  @version $Id$
 */
package com.intel.bluetooth.emu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 */
class ConnectionBufferRFCOMM extends ConnectionBuffer {

	ConnectionBufferRFCOMM(long remoteAddress, String portID, InputStream is, OutputStream os) {
		super(remoteAddress, portID, is, os);
	}

	void rfWrite(byte[] b) throws IOException {
		monitor.writeTimeStamp = System.currentTimeMillis();
		monitor.writeOperations++;
		monitor.writeBytes += b.length;
		os.write(b);
		os.flush();
	}

	int rfAvailable() throws IOException {
		return is.available();
	}

	byte[] rfRead(int len) throws IOException {
		byte[] b = new byte[len];
		int rc = is.read(b);
		if (rc == -1) {
			return null;
		}
		monitor.readTimeStamp = System.currentTimeMillis();
		monitor.readOperations++;
		monitor.readBytes += rc;

		if (rc == len) {
			return b;
		} else {
			byte[] b2 = new byte[rc];
			System.arraycopy(b, 0, b2, 0, rc);
			return b2;
		}
	}

}
