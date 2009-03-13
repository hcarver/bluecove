/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Michael Lifshits
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.RemoteDeviceHelper;

/**
 * Defines Emulator configuration properties.
 * 
 * See properties defined in javax.bluetooth.LocalDevice.getProperty();
 */
public class EmulatorConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Name of configuration file or resource.
	 * 
	 * Defaults to "bluecove.emulator.properties". Can be changed using system property "bluecove.emulator.properties"
	 * 
	 */
	public static final String CONFIG_FILE_NAME = "bluecove.emulator.properties";

	/**
	 * Allow to define device name. <br/>
	 * Example
	 * 
	 * <pre>
	 * 0B1000000000.deviceName=My Device Server
	 * # Device is Computer
	 * 0B1000000000.deviceClass=0x0100
	 * 
	 * 0B1000000001.deviceName=My Device Client
	 * # Device is Phone
	 * 0B1000000001.deviceClass=0x0200
	 * 
	 * <pre>
	 */
	public static final String deviceName = "deviceName";

	/**
	 * Allow to define device class.
	 * 
	 * @see deviceName
	 */
	public static final String deviceClass = "deviceClass";

	/**
	 * Defaults to '0B1000000000'.
	 */
	protected long firstDeviceAddress = 0x0B1000000000L;

	/**
	 * Defaults to "EmuDevice".
	 * 
	 */
	protected String deviceNamePrefix = "EmuDevice";

	/**
	 * Created Devices are initially discoverable.
	 */
	protected boolean deviceDiscoverable = true;

	/**
	 * Discoverable duration in seconds for LIAC. Defaults to <code>3</code>.
	 */
	protected int durationLIAC = 3;

	/**
	 * deviceInquiryDuration in seconds, Defaults to <code>11</code>.
	 */
	protected int deviceInquiryDuration = 11;

	/**
	 * Device Inquiry Duration is random. Defaults to <code>false</code>.
	 */
	protected boolean deviceInquiryRandomDelay = true;

	/**
	 * Defaults to 8K.
	 */
	protected int connectionBufferSize = 8 * 1024;

	/**
	 * Defaults to <code>true</code>.
	 */
	protected boolean linkEncryptionSupported = true;

	/**
	 * stream.flush() will Block sender till client reads all data.
	 */
	protected boolean senderFlushBlock = false;

	/**
	 * Monitor if client device is active.
	 * 
	 * RMI timeout is up to 10 minutes. This property enables killing application and recovery faster.
	 */
	protected int keepAliveSeconds = 5;

	private Map<String, String> propertiesMap;

	public EmulatorConfiguration() {
		propertiesMap = new Hashtable<String, String>();
		final String TRUE = "true";
		final String FALSE = "false";
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX, "7");
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX, "7");
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY_SCAN, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE_SCAN, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE, TRUE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_SD_ATTR_RETRIEVABLE_MAX, "255");
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_MASTER_SWITCH, FALSE);
		propertiesMap.put(BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX, "2048");
	}

	public void loadConfigFile() {
		String configName = System.getProperty(CONFIG_FILE_NAME, CONFIG_FILE_NAME);
		// Try file to find the file first
		File file = new File(configName);
		if (file.exists()) {
			try {
				load(new FileInputStream(file));
			} catch (IOException e) {
				DebugLog.error("Error loading properties from file " + file.getAbsolutePath(), e);
			}
		} else {
			// find the resource in the classPath
			if (!configName.startsWith("/")) {
				configName = "/" + configName;
			}
			InputStream input = this.getClass().getResourceAsStream(configName);
			if (input != null) {
				try {
					load(input);
				} catch (IOException e) {
					DebugLog.error("Error loading properties from resource " + configName, e);
				}
			}
		}
	}

	private void load(InputStream input) throws IOException {
		Properties values = new Properties();
		try {
			values.load(input);
		} finally {
			input.close();
		}
		for (Map.Entry<Object, Object> me : values.entrySet()) {
			Object value = me.getValue();
			if (value == null) {
				continue;
			}
			String txt = value.toString().trim();
			if (txt.length() == 0) {
				continue;
			}
			propertiesMap.put(me.getKey().toString(), txt);
		}
		copyPertiesToFields();
	}

	private void copyPertiesToFields() {
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			String value = propertiesMap.get(field.getName());
			if (value == null) {
				continue;
			}
			Class<?> type = field.getType();
			try {
				if (String.class.isAssignableFrom(type)) {
					field.set(this, value);
				} else if (boolean.class.isAssignableFrom(type)) {
					field.setBoolean(this, valueToBoolean(value));
				} else if (int.class.isAssignableFrom(type)) {
					field.setInt(this, valueToInt(value));
				} else if (long.class.isAssignableFrom(type)) {
					field.setLong(this, valueToLong(value));
				}
			} catch (Throwable e) {
				DebugLog.error("Error setting property " + field.getName(), e);
			}
		}
	}

	private boolean valueToBoolean(String value) {
		if (value.equalsIgnoreCase("true") || "1".equals(value)) {
			return true;
		} else {
			return false;
		}
	}

	public static int valueToInt(String value) {
		if (value.startsWith("0x")) {
			return Integer.parseInt(value.substring(2), 16);
		} else {
			return Integer.parseInt(value);
		}
	}

	public static long valueToLong(String value) {
		if (value.startsWith("0x")) {
			return Long.parseLong(value.substring(2), 16);
		} else {
			return Long.parseLong(value);
		}
	}

	public EmulatorConfiguration clone(long localAddress) {
		String namePrefix = RemoteDeviceHelper.getBluetoothAddress(localAddress) + ".";
		EmulatorConfiguration deviceConfig = new EmulatorConfiguration();
		for (Map.Entry<String, String> me : this.propertiesMap.entrySet()) {
			String key = me.getKey();
			String value = me.getValue();
			deviceConfig.propertiesMap.put(key, value);
			if (key.startsWith(namePrefix)) {
				deviceConfig.propertiesMap.put(key.substring(namePrefix.length()), value);
			}
		}
		deviceConfig.copyPertiesToFields();
		return deviceConfig;
	}

	public int getDurationLIAC() {
		return durationLIAC;
	}

	public int getDeviceInquiryDuration() {
		return deviceInquiryDuration;
	}

	public boolean isDeviceInquiryRandomDelay() {
		return deviceInquiryRandomDelay;
	}

	public long getFirstDeviceAddress() {
		return firstDeviceAddress;
	}

	/**
	 * Get specific device property.
	 * 
	 * If specific not found, return global value.
	 * 
	 * @param address
	 *            device address
	 * @param property
	 * @return value or null
	 */
	public String getProperty(long address, String property) {
		String addressString = RemoteDeviceHelper.getBluetoothAddress(address);
		String v = getProperty(addressString + "." + property);
		if (v != null) {
			return v;
		} else {
			return getProperty(property);
		}
	}

	public String getProperty(String property) {
		return (String) propertiesMap.get(property);
	}

	public int getIntProperty(String property) {
		return Integer.valueOf(getProperty(property)).intValue();
	}

	public String getDeviceNamePrefix() {
		return deviceNamePrefix;
	}

	public boolean isDeviceDiscoverable() {
		return this.deviceDiscoverable;
	}

	public int getConnectionBufferSize() {
		return connectionBufferSize;
	}

	public boolean isLinkEncryptionSupported() {
		return this.linkEncryptionSupported;
	}

	public int getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	public boolean isSenderFlushBlock() {
		return this.senderFlushBlock;
	}
}
