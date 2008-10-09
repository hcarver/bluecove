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
class EmulatorL2CAPService extends EmulatorServiceConnection {

	private int receiveMTU;

	private int transmitMTU;

	int pcm;

	EmulatorL2CAPService(EmulatorLocalDevice localDevice, long handle, int pcm) {
		super(localDevice, handle);
		this.pcm = pcm;
	}

	public int getPcm() {
		return this.pcm;
	}

	int getReceiveMTU() throws IOException {
		return receiveMTU;
	}

	int getTransmitMTU() throws IOException {
		return transmitMTU;
	}

	public void open(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU) throws IOException {
		this.params = params;
		this.receiveMTU = receiveMTU;
		this.transmitMTU = transmitMTU;
		localDevice.getDeviceManagerService().l2OpenService(localDevice.getAddress(), this.pcm);
	}

	/**
	 * 
	 * @return connectionHandle on server
	 * @throws IOException
	 */
	public long accept() throws IOException {
		return localDevice.getDeviceManagerService().l2Accept(localDevice.getAddress(), this.pcm,
				this.params.authenticate, this.params.encrypt, this.receiveMTU);
	}

	public void close(ServiceRecordImpl serviceRecord) throws IOException {
		localDevice.getDeviceManagerService().removeServiceRecord(localDevice.getAddress(), serviceRecord.getHandle());
		localDevice.getDeviceManagerService().l2CloseService(localDevice.getAddress(), this.pcm);
	}
}
