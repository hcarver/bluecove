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
class EmulatorL2CAPClient extends EmulatorLinkedConnection {

	private int receiveMTU;

	private int transmitMTU;

	EmulatorL2CAPClient(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);
	}

	void connect(BluetoothConnectionParams params, int receiveMTU, int transmitMTU) throws IOException {
		connectVerify(params);
		this.connectionHandle = localDevice.getDeviceManagerService().l2Connect(localDevice.getAddress(),
				params.address, params.channel, params.authenticate, params.encrypt, receiveMTU, params.timeout);
		this.remoteAddress = params.address;
		this.receiveMTU = receiveMTU;
		this.transmitMTU = transmitMTU;
		int remoteDeviceReceiveMTU = localDevice.getDeviceManagerService().l2RemoteDeviceReceiveMTU(
				localDevice.getAddress(), this.connectionHandle);
		if (this.transmitMTU == -1) {
			this.transmitMTU = remoteDeviceReceiveMTU;
		} else if (this.transmitMTU < remoteDeviceReceiveMTU) {
			// Ok use selected
		} else {
			this.transmitMTU = remoteDeviceReceiveMTU;
		}
	}

	void connect(long remoteAddress, long connectionHandle, int receiveMTU, int transmitMTU) throws IOException {
		super.connect(remoteAddress, connectionHandle);
		this.receiveMTU = receiveMTU;
		this.transmitMTU = transmitMTU;
	}

	int getReceiveMTU() throws IOException {
		return receiveMTU;
	}

	int getTransmitMTU() throws IOException {
		return transmitMTU;
	}

	boolean ready() throws IOException {
		return localDevice.getDeviceManagerService().l2Ready(localDevice.getAddress(), this.connectionHandle);
	}

	int receive(byte[] inBuf) throws IOException {
		byte[] packetData = localDevice.getDeviceManagerService().l2Receive(localDevice.getAddress(),
				this.connectionHandle, this.receiveMTU);
		int length = packetData.length;
		if (length > inBuf.length) {
			length = inBuf.length;
		}
		System.arraycopy(packetData, 0, inBuf, 0, length);
		return length;
	}

	void send(byte[] data) throws IOException {
		if (data.length > transmitMTU) {
			byte[] b = new byte[transmitMTU];
			System.arraycopy(data, 0, b, 0, transmitMTU);
			data = b;
		}
		localDevice.getDeviceManagerService().l2Send(localDevice.getAddress(), this.connectionHandle, data);
	}

}
