/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

/**
 * BlueCove specific LocalDevice properties.
 * 
 * @see javax.bluetooth.LocalDevice#getProperty(String)
 */
public interface BlueCoveLocalDeviceProperties {

	/**
	 * <code>"bluecove"</code> The version of BlueCove implementation.
	 */
	public static final String LOCAL_DEVICE_PROPERTY_BLUECOVE_VERSION = "bluecove";

	/**
	 * <code>"bluecove.stack"</code> The Bluetooth Stack: "winsock", "widcomm" or "bluesoleil" on windows. "mac", "bluez" or "emulator".
	 */
	public static final String LOCAL_DEVICE_PROPERTY_STACK = BlueCoveConfigProperties.PROPERTY_STACK;

	/**
     * <code>"bluecove"</code> The version of native stack.
     */
	public static final String LOCAL_DEVICE_PROPERTY_STACK_VERSION = "bluecove.stack.version";
	
	
	/**
	 * <code>"bluecove.feature.l2cap"</code> Does the current Bluetooth Stack support L2CAP: "true" or "false"
	 */
	public static final String LOCAL_DEVICE_PROPERTY_FEATURE_L2CAP = "bluecove.feature.l2cap";

	/**
	 * <code>"bluecove.feature.service_attributes"</code>
	 */
	public static final String LOCAL_DEVICE_PROPERTY_FEATURE_SERVICE_ATTRIBUTES = "bluecove.feature.service_attributes";

	/**
	 * <code>"bluecove.feature.set_device_service_classes"</code>
	 */
	public static final String LOCAL_DEVICE_PROPERTY_FEATURE_SET_DEVICE_SERVICE_CLASSES = "bluecove.feature.set_device_service_classes";

	/**
	 * <code>"bluecove.connections"</code> The number of open connections by current Bluetooth Stack.
	 */
	public static final String LOCAL_DEVICE_PROPERTY_OPEN_CONNECTIONS = "bluecove.connections";

	/**
	 * If Stack support multiple bluetooth adapters return selected one ID. (Linux BlueZ and Emulator)
	 * 
	 * @see com.intel.bluetooth.BlueCoveConfigProperties#PROPERTY_LOCAL_DEVICE_ID
	 */
	public static final String LOCAL_DEVICE_PROPERTY_DEVICE_ID = BlueCoveConfigProperties.PROPERTY_LOCAL_DEVICE_ID;

	/**
	 * List the local adapters supported by the system. Returns comma separated String list.
	 * <code>"bluecove.local_devices_ids"</code>.
	 * 
	 * @see com.intel.bluetooth.BlueCoveConfigProperties#PROPERTY_LOCAL_DEVICE_ID
	 */
	public static final String LOCAL_DEVICE_DEVICES_LIST = "bluecove.local_devices_ids";

	/**
     * <code>"bluecove.radio.version"</code>.
     */
	public static final String LOCAL_DEVICE_RADIO_VERSION = "bluecove.radio.version";
	
	/**
     * <code>"bluecove.radio.manufacturer"</code>.
     */
	public static final String LOCAL_DEVICE_RADIO_MANUFACTURER = "bluecove.radio.manufacturer";
}
