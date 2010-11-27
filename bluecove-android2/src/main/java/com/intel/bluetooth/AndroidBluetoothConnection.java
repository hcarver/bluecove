/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2010 Mina Shokry
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
 *  @version $Id$
 */

package com.intel.bluetooth;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Mina Shokry
 */
public class AndroidBluetoothConnection {
	private static volatile long nextHandle;
	private static Map<Long, AndroidBluetoothConnection> connectionsMap;

	static {
		nextHandle = 1;
		connectionsMap = new HashMap<Long, AndroidBluetoothConnection>();
	}

	private BluetoothSocket socket;
	private BluetoothServerSocket serverSocket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private long handle;

	private AndroidBluetoothConnection(long handle, BluetoothSocket socket, boolean isServer) throws IOException {
		this.handle = handle;
		this.socket = socket;
		if (!isServer) {
			socket.connect();
		}
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
	}

	private AndroidBluetoothConnection(long handle, BluetoothServerSocket serverSocket) {
		this.handle = handle;
		this.serverSocket = serverSocket;
	}

	public synchronized static AndroidBluetoothConnection createConnection(BluetoothSocket socket, boolean isServer) throws IOException {
		AndroidBluetoothConnection bluetoothConnection = new AndroidBluetoothConnection(nextHandle, socket, isServer);
		connectionsMap.put(nextHandle, bluetoothConnection);

		nextHandle ++;

		return bluetoothConnection;
	}

	public synchronized static AndroidBluetoothConnection createServerConnection(BluetoothServerSocket serverSocket) {
		AndroidBluetoothConnection bluetoothConnection = new AndroidBluetoothConnection(nextHandle, serverSocket);
		connectionsMap.put(nextHandle, bluetoothConnection);

		nextHandle ++;

		return bluetoothConnection;
	}


	public static AndroidBluetoothConnection getBluetoothConnection(long handle) {
		return connectionsMap.get(handle);
	}

	public long getHandle() {
		return handle;
	}

	public BluetoothSocket getSocket() {
		return socket;
	}

	public BluetoothServerSocket getServerSocket() {
		return serverSocket;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void close() throws IOException {
		if (outputStream != null) {
			outputStream.close();
		}
		if (inputStream != null) {
			inputStream.close();
		}
		if (socket != null) {
			socket.close();
		}
		if (serverSocket != null) {
			serverSocket.close();
		}
		connectionsMap.remove(handle);
	}
}
