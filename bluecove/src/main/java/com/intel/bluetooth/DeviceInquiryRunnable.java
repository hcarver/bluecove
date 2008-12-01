/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryListener;

/**
 * DeviceInquiryThread and SearchServicesThread approach is nearly the same so I
 * will describe only DeviceInquiryThread.
 * <p>
 * a) DeviceInquiryThread is create from DeviceInquiryThread.startInquiry().
 * startInquiry function is returned when callDeviceInquiryStartedCallback is
 * called from native code or error returned from runDeviceInquiry.
 * <p>
 * b) stack.runDeviceInquiry is executed from DeviceInquiryThread.run() and
 * should not returned until Inquiry finished. The return code would be given to
 * listener.inquiryCompleted
 * <p>
 * c) all listener.deviceDiscovered() should not be called from native code! Use
 * java wrappers for this! stack.deviceDiscoveredCallback and
 * callback.callDeviceDiscovered in native code.
 *
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 */
interface DeviceInquiryRunnable {

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
