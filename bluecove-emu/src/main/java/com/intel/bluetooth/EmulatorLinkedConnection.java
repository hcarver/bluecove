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

import javax.bluetooth.BluetoothConnectionException;

/**
 * @author vlads
 * 
 */
abstract class EmulatorLinkedConnection extends EmulatorConnection {

	protected long remoteAddress;

	EmulatorLinkedConnection(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);
	}

	void connectVerify(BluetoothConnectionParams params) throws IOException {
		if (params.address == localDevice.getAddress()) {
			throw new BluetoothConnectionException(BluetoothConnectionException.FAILED_NOINFO,
					"Can't connect to local device");
		}
		if ((params.encrypt) && (!localDevice.getConfiguration().isLinkEncryptionSupported())) {
			throw new BluetoothConnectionException(BluetoothConnectionException.SECURITY_BLOCK,
					"encrypt mode not supported");
		}
	}

	void connect(long remoteAddress, long connectionHandle) throws IOException {
		this.connectionHandle = connectionHandle;
		this.remoteAddress = remoteAddress;
	}

	int getSecurityOpt(int expected) throws IOException {
		return localDevice.getDeviceManagerService().getSecurityOpt(localDevice.getAddress(), this.connectionHandle,
				expected);
	}

	boolean encrypt(long address, boolean on) throws IOException {
		return localDevice.getDeviceManagerService().encrypt(localDevice.getAddress(), this.connectionHandle, address,
				on);
	}

	long getRemoteAddress() throws IOException {
		return remoteAddress;
	}

	void close() throws IOException {
		localDevice.getDeviceManagerService().closeConnection(localDevice.getAddress(), this.connectionHandle);
	}
}
