/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
public class BluetoothL2CAPClientConnection extends BluetoothL2CAPConnection {

	public BluetoothL2CAPClientConnection(long address, int channel, boolean authenticate,	boolean encrypt, int receiveMTU, int transmitMTU) throws IOException {
		super(BlueCoveImpl.instance().getBluetoothStack().l2OpenClientConnection(address, channel, authenticate, encrypt, receiveMTU, transmitMTU));
		//this.securityOpt = BlueCoveImpl.instance().getBluetoothStack().getSecurityOpt(this.handle, Utils.securityOpt(authenticate, encrypt));
		//RemoteDeviceHelper.connected(this);
	}

}
