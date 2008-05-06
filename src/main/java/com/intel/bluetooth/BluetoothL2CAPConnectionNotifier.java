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
import java.io.InterruptedIOException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;

/**
 * @author vlads
 * 
 */
class BluetoothL2CAPConnectionNotifier extends BluetoothConnectionNotifierBase implements L2CAPConnectionNotifier {

	private int psm = -1;

	public BluetoothL2CAPConnectionNotifier(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params,
			int receiveMTU, int transmitMTU) throws IOException {
		super(bluetoothStack, params);

		this.handle = bluetoothStack.l2ServerOpen(params, receiveMTU, transmitMTU, serviceRecord);

		this.psm = serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID);

		this.serviceRecord.attributeUpdated = false;

		this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);

		this.connectionCreated();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public L2CAPConnection acceptAndOpen() throws IOException {
		if (closed) {
			throw new IOException("Notifier is closed");
		}
		updateServiceRecord(true);
		try {
			long clientHandle = bluetoothStack.l2ServerAcceptAndOpenServerConnection(handle);
			int clientSecurityOpt = bluetoothStack.l2GetSecurityOpt(clientHandle, this.securityOpt);
			return new BluetoothL2CAPServerConnection(bluetoothStack, clientHandle, clientSecurityOpt);
		} catch (InterruptedIOException e) {
			throw e;
		} catch (IOException e) {
			if (closed) {
				throw new InterruptedIOException("Notifier has been closed");
			}
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#stackServerClose(long)
	 */
	protected void stackServerClose(long handle) throws IOException {
		bluetoothStack.l2ServerClose(handle, serviceRecord);
	}

	protected void validateServiceRecord(ServiceRecord srvRecord) {
		if (this.psm != serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
			throw new IllegalArgumentException("Must not change the PSM");
		}
		super.validateServiceRecord(srvRecord);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#updateStackServiceRecord(com.intel.bluetooth.ServiceRecordImpl,
	 *      boolean)
	 */
	protected void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException {
		bluetoothStack.l2ServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
	}

}
