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
 * BlueCove runtime configuration properties. Can be configured as system properties. If System property is not an
 * option (e.g. when running in Web Start) create text file with property name e.g. "bluecove.stack.first" write the
 * value and add this file to BlueCove or Application jar
 * 
 * @see BlueCoveImpl#setConfigProperty(String, String)
 */
public interface BlueCoveConfigProperties {

	/**
	 * Will enable debug prints prints in BlueCove code. BlueCove log is redirected to log4j when log4j classes are
	 * available in classpath. Then BlueCove debug can be enabled using log4j configuration.
	 */
	public static final String PROPERTY_DEBUG = "bluecove.debug";

	/**
	 * BlueCove log when enabled is printed to System.out. You can disable this feature.
	 */
	public static final String PROPERTY_DEBUG_STDOUT = "bluecove.debug.stdout";

	/**
	 * BlueCove log is redirected to log4j when log4j classes are available in classpath. You can disable this feature.
	 */
	public static final String PROPERTY_DEBUG_LOG4J = "bluecove.debug.log4j";

	/**
	 * If automatic Bluetooth Stack detection is not enough this can be used to force desired Stack Initialization.
	 * Values "widcomm", "bluesoleil" or "winsock". Use "emulator" value to start jsr-82 emulator. By default winsock is
	 * selected if available
	 */
	public static final String PROPERTY_STACK = "bluecove.stack";

	/**
	 * Used to optimize stack detection. If -Dbluecove.stack.first=widcomm then widcomm (bluecove.dll) stack is loaded
	 * first and if not available then BlueCove will switch to winsock. By default intelbth.dll is loaded first.
	 */
	public static final String PROPERTY_STACK_FIRST = "bluecove.stack.first";

	/**
	 * "false" to disable the load of native library from resources.
	 */
	public static final String PROPERTY_NATIVE_RESOURCE = "bluecove.native.resource";

	/**
	 * Load library (.dll) from specified location.
	 */
	public static final String PROPERTY_NATIVE_PATH = "bluecove.native.path";

	/**
	 * If Stack support multiple bluetooth adapters select one by its system ID. (Linux BlueZ and Emulator)
	 */
	public static final String PROPERTY_LOCAL_DEVICE_ID = "bluecove.deviceID";

	/**
	 * If Stack support multiple bluetooth adapters select one by its bluetooth address. (Linux BlueZ and Emulator)
	 */
	public static final String PROPERTY_LOCAL_DEVICE_ADDRESS = "bluecove.deviceAddress";

	/**
	 * JSR-82 air simulator server can be on remote computer, default 'localhost'.
	 */
	public static final String PROPERTY_EMULATOR_HOST = "bluecove.emu.rmiRegistryHost";

	/**
	 * JSR-82 air simulator server listen on different port, default 8090.
	 * <p>
	 * Use 0 on the client to enable in process server, RMI will not be used.
	 */
	public static final String PROPERTY_EMULATOR_PORT = "bluecove.emu.rmiRegistryPort";

	/**
	 * JSR-82 air simulator server and RMI registry can be started inside client JVM, default 'false'.
	 */
	public static final String PROPERTY_EMULATOR_RMI_REGISTRY = "bluecove.emu.rmiRegistry";

	/**
	 * Some properties can't be changed at runtime once the Stack was initialized.
	 */
	public static final String[] INITIALIZATION_PROPERTIES = new String[] { PROPERTY_STACK, PROPERTY_STACK_FIRST,
			PROPERTY_NATIVE_RESOURCE, PROPERTY_NATIVE_RESOURCE, PROPERTY_LOCAL_DEVICE_ID,
			PROPERTY_LOCAL_DEVICE_ADDRESS, PROPERTY_EMULATOR_HOST, PROPERTY_EMULATOR_PORT };

	/**
	 * The amount of time in milliseconds for which the implementation will attempt to establish connection RFCOMM or
	 * L2CAP before it throws BluetoothConnectionException. Defaults to 2 minutes. WIDCOMM and OS X only.
	 */
	public static final String PROPERTY_CONNECT_TIMEOUT = "bluecove.connect.timeout";

	/**
	 * On MS stack retry connection automatically when received WSAENETUNREACH during connect. Default to 2 retry
	 * attempts.
	 * 
	 * @since bluecove 2.1.0
	 */
	public static final String PROPERTY_CONNECT_UNREACHABLE_RETRY = "bluecove.connect.unreachable_retry";

	/**
	 * Device Inquiry time in seconds defaults to 11 seconds. MS Stack and OS X only.
	 */
	public static final String PROPERTY_INQUIRY_DURATION = "bluecove.inquiry.duration";

	static final int PROPERTY_INQUIRY_DURATION_DEFAULT = 11;

	/**
	 * Set true to make Device Inquiry call DiscoveryListener?.deviceDiscovered without waiting for updated service
	 * class. WIDCOMM only.
	 */
	public static final String PROPERTY_INQUIRY_REPORT_ASAP = "bluecove.inquiry.report_asap";

	/**
	 * You can increase OBEX transfer speed by changing mtu to bigger value. Default is 1024
	 */
	public static final String PROPERTY_OBEX_MTU = "bluecove.obex.mtu";

	/**
	 * The amount of time in milliseconds for which the implementation will attempt to successfully transmit a packet
	 * before it throws InterruptedIOException. Defaults to 2 minutes.
	 */
	public static final String PROPERTY_OBEX_TIMEOUT = "bluecove.obex.timeout";

	/**
	 * Remove JSR-82 1.1 restriction for legal PSM values are in the range (0x1001..0xFFFF).
	 * 
	 * For JSR-82 1.2 Reserved Ranges @see <A HREF="https://opensource.motorola.com/sf/discussion/do/listPosts/projects.jsr82/discussion.jsr_82_1_2_open_discussion.topc1808"
	 * >JSR-82 1.2</A>
	 */
	public static final String PROPERTY_JSR_82_PSM_MINIMUM_OFF = "bluecove.jsr82.psm_minimum_off";
}
