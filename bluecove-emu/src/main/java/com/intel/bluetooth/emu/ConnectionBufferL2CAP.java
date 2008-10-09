/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package com.intel.bluetooth.emu;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author vlads
 * 
 */
class ConnectionBufferL2CAP extends ConnectionBuffer {

	private int remoteReceiveMTU;

	ConnectionBufferL2CAP(long remoteAddress, InputStream is, OutputStream os, int remoteReceiveMTU) {
		super(remoteAddress, is, os);
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
