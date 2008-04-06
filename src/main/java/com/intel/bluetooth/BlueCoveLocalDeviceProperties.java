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
 * BlueCove specific LocalDevice properties.
 * 
 * @see javax.bluetooth.LocalDevice#getProperty(String)
 */
public interface BlueCoveLocalDeviceProperties {

	/**
	 * <code>bluecove</code> The version of BlueCove implementation/
	 */
	public static final String LOCAL_DEVICE_PROPERTY_BLUECOVE_VERSION = "bluecove";

	/**
	 * <code>bluecove.stack</code>The Bluetooth Stack: "winsock", "widcomm"
	 * or "bluesoleil" on windows.
	 */
	public static final String LOCAL_DEVICE_PROPERTY_STACK = BlueCoveConfigProperties.PROPERTY_STACK;

	/**
	 * <code>bluecove.feature.l2cap</code> Does the current Bluetooth Stack
	 * support L2CAP: "true" or "false"
	 */
	public static final String LOCAL_DEVICE_PROPERTY_FEATURE_L2CAP = "bluecove.feature.l2cap";

	public static final String LOCAL_DEVICE_PROPERTY_FEATURE_SERVICE_ATTRIBUTES = "bluecove.feature.service_attributes";

	public static final String LOCAL_DEVICE_PROPERTY_FEATURE_SET_DEVICE_SERVICE_CLASSES = "bluecove.feature.set_device_service_classes";

	/**
	 * <code>bluecove.connections</code> The number of open connections by
	 * current Bluetooth Stack.
	 */
	public static final String LOCAL_DEVICE_PROPERTY_OPEN_CONNECTIONS = "bluecove.connections";

}
