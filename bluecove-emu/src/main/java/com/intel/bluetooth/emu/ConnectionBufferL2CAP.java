/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Michael Lifshits
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 *
 */
class ConnectionBufferL2CAP extends ConnectionBuffer {

	private final int remoteReceiveMTU;

	ConnectionBufferL2CAP(long remoteAddress, String portID, InputStream is, OutputStream os, int remoteReceiveMTU) {
		super(remoteAddress, portID, is, os);
		this.remoteReceiveMTU = remoteReceiveMTU;
	}

	public int getRemoteReceiveMTU() {
		return this.remoteReceiveMTU;
	}

	void send(byte[] data) throws IOException {
		monitor.writeTimeStamp = System.currentTimeMillis();
		monitor.writeOperations++;
		monitor.writeBytes += data.length;

		byte[] packet = new byte[data.length + 2];
		packet[0] = (byte) ((data.length >> 8) & 0xFF);
		packet[1] = (byte) (0xFF & data.length);
		System.arraycopy(data, 0, packet, 2, data.length);
		os.write(packet);
	}

	boolean ready() throws IOException {
		return (is.available() > 1);
	}

	static void readFully(InputStream is, byte[] b, int off, int len) throws IOException, EOFException {
		if (len < 0) {
			throw new IndexOutOfBoundsException();
		}
		int got = 0;
		while (got < len) {
			int rc = is.read(b, off + got, len - got);
			if (rc < 0) {
				throw new EOFException();
			}
			got += rc;
		}
	}

	byte[] receive(int len) throws IOException {
		byte[] packetLenData = new byte[2];
		int packetLen;
		byte[] packetData;
		synchronized (is) {
			readFully(is, packetLenData, 0, 2);
			packetLen = ((((int) packetLenData[0] << 8) & 0xFF00) + (packetLenData[1] & 0xFF));
			packetData = new byte[packetLen];
			readFully(is, packetData, 0, packetLen);
		}
		monitor.readTimeStamp = System.currentTimeMillis();
		monitor.readOperations++;
		monitor.readBytes += packetLen;

		if (packetLen == len) {
			return packetData;
		} else {
			if (packetLen > len) {
				packetLen = len;
			}
			byte[] b2 = new byte[packetLen];
			System.arraycopy(packetData, 0, b2, 0, packetLen);
			return b2;
		}
	}

}
