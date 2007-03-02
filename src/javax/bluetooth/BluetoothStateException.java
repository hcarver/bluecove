/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package javax.bluetooth;

import java.io.IOException;

public class BluetoothStateException extends IOException {
	/*
	 * Creates a new BluetoothStateException without a detail message.
	 */

	private static final long serialVersionUID = 1L;

	public BluetoothStateException() {
	}

	/*
	 * Creates a BluetoothStateException with the specified detail message.
	 * Parameters: msg - the reason for the exception
	 */

	public BluetoothStateException(String msg) {
		super(msg);
	}
}