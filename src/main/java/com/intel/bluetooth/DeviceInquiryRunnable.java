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

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryListener;

/**
 * DeviceInquiryThread and SearchServicesThread approach is nearly the same so I
 * will describe only DeviceInquiryThread.
 * 
 * a) DeviceInquiryThread is create from DeviceInquiryThread.startInquiry().
 * startInquiry function is returned when callDeviceInquiryStartedCallback is
 * called from native code or error returned from runDeviceInquiry.
 * 
 * b) stack.runDeviceInquiry is executed from DeviceInquiryThread.run() and
 * should not returned until Inquiry finished. The return code would be given to
 * listener.inquiryCompleted
 * 
 * c) all listener.deviceDiscovered() should not be called from native code! Use
 * java wrappers for this! stack.deviceDiscoveredCallback and
 * callback.callDeviceDiscovered in native code.
 * 
 * @author vlads
 * 
 */
public interface DeviceInquiryRunnable {

	/**
	 * Common synchronous method called by DeviceInquiryThread. Should throw
	 * BluetoothStateException only if it can't start Inquiry
	 */
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener)
			throws BluetoothStateException;

	/**
	 * Convenience method called from native code
	 */
	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass,
			String deviceName, boolean paired);
}
