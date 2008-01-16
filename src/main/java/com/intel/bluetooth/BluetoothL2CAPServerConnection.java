/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
class BluetoothL2CAPServerConnection extends BluetoothL2CAPConnection {

	/**
	 * @param handle
	 * @throws IOException 
	 */
	protected BluetoothL2CAPServerConnection(BluetoothStack bluetoothStack, long handle, int securityOpt) throws IOException {
		super(bluetoothStack, handle);
		boolean initOK = false;
		try {
			this.securityOpt = securityOpt;
			RemoteDeviceHelper.connected(this);
			initOK = true;
		} finally {
			if (!initOK) {
				try {
					bluetoothStack.l2CloseServerConnection(this.handle);
				} catch (IOException e) {
					DebugLog.error("close error", e);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothL2CAPConnection#closeConnectionHandle(long)
	 */
	void closeConnectionHandle(long handle) throws IOException {
		RemoteDeviceHelper.disconnected(this);
		bluetoothStack.l2CloseServerConnection(handle);
	}

}
