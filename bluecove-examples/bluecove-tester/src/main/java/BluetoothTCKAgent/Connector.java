/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package BluetoothTCKAgent;

import java.io.IOException;

import javax.bluetooth.BluetoothConnectionException;
import javax.microedition.io.Connection;
import net.sf.bluecove.Logger;

/**
 * Small hack to enable connection retry while working on other implementations.
 * This will improve test stability and make them reproducible without tweaks in
 * timeouts.
 *
 */
public class Connector {

	public static final int READ = javax.microedition.io.Connector.READ;

	public static final int WRITE = javax.microedition.io.Connector.WRITE;

	public static final int READ_WRITE = javax.microedition.io.Connector.READ_WRITE;

	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		return javax.microedition.io.Connector.open(name, mode, timeouts);
	}

	public static Connection open(String name, int mode) throws IOException {
		int retryMax = 3;
		int retry = 0;
		while (true) {
			try {
				return javax.microedition.io.Connector.open(name, mode);
			} catch (BluetoothConnectionException e) {
				if (retry >= retryMax) {
					Logger.error(name);
					throw e;
				}
				retry++;
				Logger.debug("retry " + retry, e);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ie) {
					throw new IOException(ie.getMessage());
				}
			}
		}
	}

	public static Connection open(String name) throws IOException {
		return open(name, Connector.READ_WRITE);
	}
}
