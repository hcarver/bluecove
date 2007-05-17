/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;

public class BluetoothStackBlueSoleil implements BluetoothStack {

	BluetoothStackBlueSoleil() {
		initialize();
	}
	
	public String getStackID() {
		return BlueCoveImpl.STACK_BLUESOLEIL;
	}
	
	public native void initialize();
	
	public native String getLocalDeviceBluetoothAddress();

	public native String getLocalDeviceName();

	/**
	 * @todo
	 */
	public DeviceClass getLocalDeviceClass() {
		return new DeviceClass(BlueCoveImpl.instance().getBluetoothPeer().getDeviceClass(0));
	}

	/**
	 * @todo
	 * There are no functions to set BlueSoleil stack discoverable status.
	 */
	public boolean setLocalDeviceDiscoverable(int mode) throws BluetoothStateException {
		return true;
	}

	native boolean isBlueSoleilStarted(int seconds);
	
	private native boolean isBluetoothReady(int seconds);
	
	/**
	 * @todo
	 * There are no functions to find BlueSoleil discoverable status.
	 */
	public int getLocalDeviceDiscoverable() {
		if (isBluetoothReady(2)) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}

	public boolean isLocalDevicePowerOn() {
		return isBluetoothReady(15);
	}
	
	private native int getStackVersionInfo();
	
	private native int getDeviceVersion();
	
	private native int getDeviceManufacturer();
	
	public String getLocalDeviceProperty(String property) {
		final String TRUE = "true";
		if ("bluetooth.connected.devices.max".equals(property)) {
			return "7";
		}
		if ("bluetooth.sd.trans.max".equals(property)) {
			return "1";
		}
		if ("bluetooth.connected.inquiry.scan".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.page.scan".equals(property)) {
			return TRUE;
		}
		if ("bluetooth.connected.inquiry".equals(property)) {
			return TRUE;
		}
		
		if ("bluecove.radio.version".equals(property)) {
			return String.valueOf(getDeviceVersion());
		}
		if ("bluecove.radio.manufacturer".equals(property)) {
			return String.valueOf(getDeviceManufacturer());
		}
		if ("bluecove.stack.version".equals(property)) {
			return String.valueOf(getStackVersionInfo());
		}
		
		return null;
	}
	
// --- Device Inquiry

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		return DeviceInquiryThread.startInquiry(this, accessCode, listener);
	}
	
	public int runDeviceInquiry(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.deviceInquiryStartedCallback();
		return runDeviceInquiryImpl(startedNotify, accessCode, listener);
	}

	public native int runDeviceInquiryImpl(DeviceInquiryThread startedNotify, int accessCode, DiscoveryListener listener) throws BluetoothStateException;

	public void deviceDiscoveredCallback(DiscoveryListener listener, long deviceAddr, int deviceClass, String deviceName) {
		DebugLog.debug("deviceDiscoveredCallback", deviceName);
		listener.deviceDiscovered(new RemoteDeviceImpl(deviceAddr, deviceName), new DeviceClass(deviceClass));			
	}

	public boolean cancelInquiry(DiscoveryListener listener) {
		return false;
	}
	
// --- Service search 
	
	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		return SearchServicesThread.startSearchServices(this, attrSet, uuidSet, device, listener);
	}

	public boolean cancelServiceSearch(int transID) {
		return false;
	}
	
	private native long[] runSearchServicesImpl(SearchServicesThread startedNotify, UUID uuid, long address) throws BluetoothStateException;
	
	public int runSearchServices(SearchServicesThread startedNotify, int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		startedNotify.searchServicesStartedCallback();
		UUID uuid = null;
		if ((uuidSet != null) && (uuidSet.length > 0)) {
			uuid = uuidSet[uuidSet.length -1];
		}
		long[] handles = runSearchServicesImpl(startedNotify, uuid, ((RemoteDeviceImpl)device).getAddress());
		return DiscoveryListener.SERVICE_SEARCH_ERROR;
	}

	public boolean populateServicesRecordAttributeValues(ServiceRecordImpl serviceRecord, int[] attrIDs) throws IOException {
		return true;
	}
	
//	 --- Client RFCOMM connections
	
	public native long connectionRfOpen(long address, int channel, boolean authenticate, boolean encrypt) throws IOException;
	
	public native void connectionRfClose(long handle) throws IOException;

	public native long getConnectionRfRemoteAddress(long handle) throws IOException;
	
	public native int connectionRfRead(long handle) throws IOException;

	public native int connectionRfRead(long handle, byte[] b, int off, int len) throws IOException;

	public native int connectionRfReadAvailable(long handle) throws IOException;

	public native void connectionRfWrite(long handle, int b) throws IOException;

	public native void connectionRfWrite(long handle, byte[] b, int off, int len) throws IOException;

}
