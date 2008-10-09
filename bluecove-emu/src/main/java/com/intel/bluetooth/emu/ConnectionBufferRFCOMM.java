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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author vlads
 * 
 */
class ConnectionBufferRFCOMM extends ConnectionBuffer {

	ConnectionBufferRFCOMM(long remoteAddress, InputStream is, OutputStream os) {
		super(remoteAddress, is, os);
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
