/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth.emu;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.ServiceRegistrationException;

// import com.pyx4j.rpcoverhttp.common.RoHService;

public interface DeviceManagerService /* extends RoHService */{

	public DeviceDescriptor createNewDevice(String deviceID, String deviceAddress) throws BluetoothStateException;

	public void shutdown();

	public EmulatorConfiguration getEmulatorConfiguration(long localAddress);

	public void releaseDevice(long localAddress);

	public DeviceDescriptor getDeviceDescriptor(long address);

	public DeviceCommand pollCommand(long localAddress);

	public boolean isLocalDevicePowerOn(long localAddress);

	public void setLocalDevicePower(long localAddress, boolean on);

	public int getLocalDeviceDiscoverable(long localAddress);

	public boolean setLocalDeviceDiscoverable(long localAddress, int mode) throws BluetoothStateException;

	public void setLocalDeviceServiceClasses(long localAddress, int classOfDevice);

	public DeviceDescriptor[] getDiscoveredDevices(long localAddress);

	public String getRemoteDeviceFriendlyName(long remoteAddress) throws IOException;

	public void updateServiceRecord(long localAddress, long handle, ServicesDescriptor sdpData)
			throws ServiceRegistrationException;

	public void removeServiceRecord(long localAddress, long handle) throws IOException;

	public long[] searchServices(long remoteAddress, String[] uuidSet);

	public byte[] getServicesRecordBinary(long remoteAddress, long handle) throws IOException;

	public void rfOpenService(long localAddress, int channel) throws IOException;

	public long rfAccept(long localAddress, int channel, boolean authenticate, boolean encrypt) throws IOException;

	public void connectionAccepted(long localAddress, long connectionId) throws IOException;

	public long rfConnect(long localAddress, long remoteAddress, int channel, boolean authenticate, boolean encrypt,
			int timeout) throws IOException;

	public void rfCloseService(long localAddress, int channel);

	public void closeConnection(long localAddress, long connectionId) throws IOException;

	public int getSecurityOpt(long localAddress, long connectionId, int expected) throws IOException;

	public boolean encrypt(long localAddress, long connectionId, long remoteAddress, boolean on) throws IOException;

	public void l2OpenService(long localAddress, int pcm) throws IOException;

	public long l2Accept(long localAddress, int pcm, boolean authenticate, boolean encrypt, int receiveMTU)
			throws IOException;

	public long l2Connect(long localAddress, long remoteAddress, int pcm, boolean authenticate, boolean encrypt,
			int receiveMTU, int timeout) throws IOException;

	public int l2RemoteDeviceReceiveMTU(long localAddress, long connectionId) throws IOException;

	public void l2CloseService(long localAddress, int pcm);

	public long getRemoteAddress(long localAddress, long connectionId) throws IOException;

	public void rfWrite(long localAddress, long connectionId, byte[] b) throws IOException;

	public int rfAvailable(long localAddress, long connectionId) throws IOException;
	
	public void rfFlush(long localAddress, long connectionId) throws IOException;

	public byte[] rfRead(long localAddress, long connectionId, int len) throws IOException;

	public boolean l2Ready(long localAddress, long connectionId) throws IOException;

	public byte[] l2Receive(long localAddress, long connectionId, int len) throws IOException;

	public void l2Send(long localAddress, long connectionId, byte[] data) throws IOException;
}
