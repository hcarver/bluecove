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

import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.DebugLog;

public class DeviceClass {

	private static final int SERVICE_MASK = 0xffe000;

	private static final int MAJOR_MASK = 0x001f00;

	private static final int MINOR_MASK = 0x0000fc;

	private int record;

	/*
	 * Creates a DeviceClass from the class of device record provided. record
	 * must follow the format of the class of device record in the Bluetooth
	 * specification. Parameters: record - describes the classes of a device
	 * Throws: IllegalArgumentException - if record has any bits between 24 and
	 * 31 set
	 */

	public DeviceClass(int record) {
		
		DebugLog.debug("new DeviceClass", record);
		
		this.record = record;

		if ((record & 0xff000000) != 0)
			throw new IllegalArgumentException();
	}

	/*
	 * Retrieves the major service classes. A device may have multiple major
	 * service classes. When this occurs, the major service classes are bitwise
	 * OR'ed together. Returns: the major service classes
	 */

	public int getServiceClasses() {
		return record & SERVICE_MASK;
	}

	/*
	 * Retrieves the major device class. A device may have only a single major
	 * device class. Returns: the major device class
	 */

	public int getMajorDeviceClass() {
		return record & MAJOR_MASK;
	}

	/*
	 * Retrieves the minor device class. Returns: the minor device class
	 */

	public int getMinorDeviceClass() {
		return record & MINOR_MASK;
	}

	public String toString() {
		return BluetoothConsts.DeviceClassConsts.toString(this);
	}
}