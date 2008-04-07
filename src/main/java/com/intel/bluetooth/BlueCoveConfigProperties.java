/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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

/**
 * BlueCove runtime configuration properties. Can be configured as system
 * properties. If System property is not an option (e.g. when running in Web
 * Start) create text file with property name e.g. "bluecove.stack.first" write
 * the value and add this file to BlueCove or Application jar
 * 
 * @see BlueCoveImpl#setConfigProperty(String, String)
 */
public interface BlueCoveConfigProperties {

	/**
	 * Will enable debug prints prints in BlueCove code. BlueCove log is
	 * redirected to log4j when log4j classes are available in classpath. Then
	 * BlueCove debug can be enabled using log4j configuration.
	 */
	public static final String PROPERTY_DEBUG = "bluecove.debug";

	/**
	 * If automatic Bluetooth Stack detection is not enough this can be used to
	 * force desired Stack Initialization. Values "widcomm", "bluesoleil" or
	 * "winsock". By default winsock is selected if available
	 */
	public static final String PROPERTY_STACK = "bluecove.stack";

	/**
	 * Used to optimize stack detection. If -Dbluecove.stack.first=widcomm then
	 * widcomm (bluecove.dll) stack is loaded first and if not available then
	 * BlueCove will switch to winsock. By default intelbth.dll is loaded first.
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
	 * If Stack support multiple bluetooth adapters select one by its system ID.
	 * (Linux BlueZ and Emulator)
	 */
	public static final String PROPERTY_LOCAL_DEVICE_ID = "bluecove.deviceID";

	/**
	 * If Stack support multiple bluetooth adapters select one by its bluetooth
	 * address. (Linux BlueZ and Emulator)
	 */
	public static final String PROPERTY_LOCAL_DEVICE_ADDRESS = "bluecove.deviceAddress";

	/**
	 * Some properties can't be changed at runtime once the Stack was
	 * initialized.
	 */
	public static final String[] INITIALIZATION_PROPERTIES = new String[] { PROPERTY_STACK, PROPERTY_STACK_FIRST,
			PROPERTY_NATIVE_RESOURCE, PROPERTY_NATIVE_RESOURCE, PROPERTY_LOCAL_DEVICE_ID, PROPERTY_LOCAL_DEVICE_ADDRESS };

	/**
	 * The amount of time in milliseconds for which the implementation will
	 * attempt to establish connection RFCOMM or L2CAP before it throws
	 * BluetoothConnectionException. Defaults to 2 minutes. WIDCOMM and OS X
	 * only.
	 */
	public static final String PROPERTY_CONNECT_TIMEOUT = "bluecove.connect.timeout";

	/**
	 * Device Inquiry time in seconds defaults to 11 seconds. MS Stack and OS X
	 * only.
	 */
	public static final String PROPERTY_INQUIRY_DURATION = "bluecove.inquiry.duration";

	public static final int PROPERTY_INQUIRY_DURATION_DEFAULT = 11;

	/**
	 * Set true to make Device Inquiry call DiscoveryListener?.deviceDiscovered
	 * without waiting for updated service class. WIDCOMM only.
	 */
	public static final String PROPERTY_INQUIRY_REPORT_ASAP = "bluecove.inquiry.report_asap";

	/**
	 * You can increase OBEX transfer speed by changing mtu to bigger value.
	 * Default is 1024
	 */
	public static final String PROPERTY_OBEX_MTU = "bluecove.obex.mtu";

	/**
	 * The amount of time in milliseconds for which the implementation will
	 * attempt to successfully transmit a packet before it throws
	 * InterruptedIOException. Defaults to 2 minutes.
	 */
	public static final String PROPERTY_OBEX_TIMEOUT = "bluecove.obex.timeout";

}
