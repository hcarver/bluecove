/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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
package com.intel.bluetooth.emu;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author vlads
 * 
 */
public class EmulatorConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	private long firstDeviceAddress = 0x0B1000000000L;

	private String deviceNamePrefix = "EmuDevice";

	private boolean deviceDiscoverable = true;

	private int durationLIAC = 3;

	private int deviceInquiryDuration = 0;// 5;

	private boolean deviceInquiryRandomDelay = true;

	private int connectionBufferSize = 8 * 1024;

	private boolean linkEncryptionSupported = true;

	// RMI timeout is up to 10 minutes. This enables killing application and
	// recovery faster.
	private int keepAliveSeconds = 5;

	private Map<String, String> propertiesMap;

	public EmulatorConfiguration() {
		propertiesMap = new Hashtable<String, String>();
		final String TRUE = "true";
		final String FALSE = "false";
		propertiesMap.put("bluetooth.connected.devices.max", "7");
		propertiesMap.put("bluetooth.sd.trans.max", "7");
		propertiesMap.put("bluetooth.connected.inquiry.scan", TRUE);
		propertiesMap.put("bluetooth.connected.page.scan", TRUE);
		propertiesMap.put("bluetooth.connected.inquiry", TRUE);
		propertiesMap.put("bluetooth.connected.page", TRUE);
		propertiesMap.put("bluetooth.sd.attr.retrievable.max", "255");
		propertiesMap.put("bluetooth.master.switch", FALSE);
		propertiesMap.put("bluetooth.l2cap.receiveMTU.max", "2048");
	}

	public int getDurationLIAC() {
		return durationLIAC;
	}

	public void setDurationLIAC(int durationLIAC) {
		this.durationLIAC = durationLIAC;
	}

	public int getDeviceInquiryDuration() {
		return deviceInquiryDuration;
	}

	public void setDeviceInquiryDuration(int deviceInquiryDuration) {
		this.deviceInquiryDuration = deviceInquiryDuration;
	}

	public boolean isDeviceInquiryRandomDelay() {
		return deviceInquiryRandomDelay;
	}

	public void setDeviceInquiryRandomDelay(boolean deviceInquiryRandomDelay) {
		this.deviceInquiryRandomDelay = deviceInquiryRandomDelay;
	}

	public long getFirstDeviceAddress() {
		return firstDeviceAddress;
	}

	public void setFirstDeviceAddress(long firstDeviceAddress) {
		this.firstDeviceAddress = firstDeviceAddress;
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

	public void setDeviceNamePrefix(String deviceNamePrefix) {
		this.deviceNamePrefix = deviceNamePrefix;
	}

	public boolean isDeviceDiscoverable() {
		return this.deviceDiscoverable;
	}

	public void setDeviceDiscoverable(boolean deviceDiscoverable) {
		this.deviceDiscoverable = deviceDiscoverable;
	}

	public int getConnectionBufferSize() {
		return connectionBufferSize;
	}

	public void setConnectionBufferSize(int connectionBufferSize) {
		this.connectionBufferSize = connectionBufferSize;
	}

	public boolean isLinkEncryptionSupported() {
		return this.linkEncryptionSupported;
	}

	public void setLinkEncryptionSupported(boolean linkEncryptionSupported) {
		this.linkEncryptionSupported = linkEncryptionSupported;
	}

	/**
	 * @return the keepAliveSeconds
	 */
	public int getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	/**
	 * @param keepAliveSeconds
	 *            the keepAliveSeconds to set
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}
}
