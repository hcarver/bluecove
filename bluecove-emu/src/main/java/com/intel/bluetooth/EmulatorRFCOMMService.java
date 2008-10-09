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
class EmulatorRFCOMMService extends EmulatorServiceConnection {

	private int channel;

	EmulatorRFCOMMService(EmulatorLocalDevice localDevice, long handle, int channel) {
		super(localDevice, handle);
		this.channel = channel;
	}

	void open(BluetoothConnectionNotifierParams params) throws IOException {
		this.params = params;
		localDevice.getDeviceManagerService().rfOpenService(localDevice.getAddress(), this.channel);
	}

	/**
	 * 
	 * @return connectionHandle on server
	 * @throws IOException
	 */
	long accept() throws IOException {
		return localDevice.getDeviceManagerService().rfAccept(localDevice.getAddress(), this.channel,
				this.params.authenticate, this.params.encrypt);
	}

	int getChannel() {
		return channel;
	}

	void close(ServiceRecordImpl serviceRecord) throws IOException {
		localDevice.getDeviceManagerService().removeServiceRecord(localDevice.getAddress(), serviceRecord.getHandle());
		localDevice.getDeviceManagerService().rfCloseService(localDevice.getAddress(), channel);
	}

}
