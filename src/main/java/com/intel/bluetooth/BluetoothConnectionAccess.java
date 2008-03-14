/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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

import javax.bluetooth.RemoteDevice;

/**
 * Used when client application has only access to Proxy of the connection. e.g.
 * WebStart in MicroEmulator
 * 
 * @author vlads
 * 
 */
public interface BluetoothConnectionAccess {

	public BluetoothStack getBluetoothStack();

	public long getRemoteAddress() throws IOException;

	public int getSecurityOpt();

	/**
	 * @see javax.bluetooth.RemoteDevice#encrypt(javax.microedition.io.Connection ,
	 *      boolean)
	 */
	public boolean encrypt(long address, boolean on) throws IOException;

	public RemoteDevice getRemoteDevice();

	public void setRemoteDevice(RemoteDevice remoteDevice);
}
