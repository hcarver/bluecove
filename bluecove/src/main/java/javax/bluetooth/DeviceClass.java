/**
 *  BlueCove - Java library for Bluetooth
 *
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *  
 *  @version $Id$
 */ 
package javax.bluetooth;

import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.DebugLog;

/**
 * The <code>DeviceClass</code> class represents the class of device (CoD)
 * record as defined by the Bluetooth specification.  This record is defined in
 * the Bluetooth Assigned Numbers document
 * and contains information on the type of the device and the type of services
 * available on the device.
 * <P>
 * The Bluetooth Assigned Numbers document
 * (<A HREF="http://www.bluetooth.org/assigned-numbers/baseband.htm">
 * http://www.bluetooth.org/assigned-numbers/baseband.htm</A>)
 * defines the service class, major device class, and minor device class.  The
 * table below provides some examples of possible return values and their
 * meaning:
 * <TABLE>
 * <TR><TH>Method</TH><TH>Return Value</TH><TH>Class of Device</TH></TR>
 * <TR><TD><code>getServiceClasses()</code></TD>
 * <TD>0x22000</TD>
 * <TD>Networking and Limited Discoverable Major Service Classes</TD></TR>
 * <TR><TD><code>getServiceClasses()</code></TD>
 * <TD>0x100000</TD>
 * <TD>Object Transfer Major Service Class</TD></TR>
 * <TR><TD><code>getMajorDeviceClass()</code></TD>
 * <TD>0x00</TD>
 * <TD>Miscellaneous Major Device Class</TD></TR>
 * <TR><TD><code>getMajorDeviceClass()</code></TD>
 * <TD>0x200</TD>
 * <TD>Phone Major Device Class</TD></TR>
 * <TR><TD><code>getMinorDeviceClass()</code></TD>
 * <TD>0x0C</TD><TD>With a Computer Major Device Class,
 *   Laptop Minor Device Class</TD></TR>
 * <TR><TD><code>getMinorDeviceClass()</code></TD>
 * <TD>0x04</TD><TD>With a Phone Major Device Class,
 *   Cellular Minor Device Class</TD></TR>
 * </TABLE>
 *
 * @version 1.0 February 11, 2002
 */

public class DeviceClass {

	private static final int SERVICE_MASK = 0xffe000;

	private static final int MAJOR_MASK = 0x001f00;

	private static final int MINOR_MASK = 0x0000fc;

	private int record;

    /**
     * Creates a <code>DeviceClass</code> from the class of device record
     * provided.  <code>record</code> must follow the format of the
     * class of device record in the Bluetooth specification.
     *
     * @param record describes the classes of a device
     *
     * @exception IllegalArgumentException if <code>record</code> has any bits
     * between 24 and 31 set
	 */

	public DeviceClass(int record) {
		
		DebugLog.debug("new DeviceClass", record);
		
		this.record = record;

		if ((record & 0xff000000) != 0)
			throw new IllegalArgumentException();
	}

	/**
	 * Retrieves the major service classes. A device may have multiple major
     * service classes.  When this occurs, the major service classes are
     * bitwise OR'ed together.
     *
     * @return the major service classes
	 */

	public int getServiceClasses() {
		return record & SERVICE_MASK;
	}

    /**
	 * Retrieves the major device class. A device may have only a single major
     * device class.
     *
     * @return the major device class
	 */

	public int getMajorDeviceClass() {
		return record & MAJOR_MASK;
	}

    /**
     * Retrieves the minor device class.
     *
     * @return the minor device class
	 */

	public int getMinorDeviceClass() {
		return record & MINOR_MASK;
	}

	/**
	 * @deprecated Use ((Object)deviceClass).toString() if you want your application to run in MDIP profile
	 */
	public String toString() {
		return BluetoothConsts.toString(this);
	}
}