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
package com.intel.bluetooth;

import java.io.IOException;

/**
 * @author vlads
 * 
 */
class EmulatorRFCOMMClient extends EmulatorLinkedConnection {

	EmulatorRFCOMMClient(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);

	}

	void connect(BluetoothConnectionParams params) throws IOException {
		connectVerify(params);
		this.connectionHandle = localDevice.getDeviceManagerService().rfConnect(localDevice.getAddress(),
				params.address, params.channel, params.authenticate, params.encrypt, params.timeout);
		this.remoteAddress = params.address;
	}

	int read() throws IOException {
		byte buf[] = new byte[1];
		int len = read(buf, 0, 1);
		if (len == -1) {
			return -1;
		}
		return buf[0] & 0xFF;
	}

	int read(byte[] b, int off, int len) throws IOException {
		byte buf[] = localDevice.getDeviceManagerService().rfRead(localDevice.getAddress(), this.connectionHandle, len);
		if (buf == null) {
			return -1;
		}
		System.arraycopy(buf, 0, b, off, buf.length);
		return buf.length;
	}

	int available() throws IOException {
		return localDevice.getDeviceManagerService().rfAvailable(localDevice.getAddress(), this.connectionHandle);
	}

	void write(int b) throws IOException {
		byte buf[] = new byte[1];
		buf[0] = (byte) (b & 0xFF);
		write(buf, 0, 1);
	}

	void write(byte[] b, int off, int len) throws IOException {
		byte buf[];
		if ((b.length == len) && (off == 0)) {
			buf = b;
		} else {
			buf = new byte[len];
			System.arraycopy(b, off, buf, 0, len);
		}
		localDevice.getDeviceManagerService().rfWrite(localDevice.getAddress(), this.connectionHandle, buf);
	}

	void flush() throws IOException {
	}

}
