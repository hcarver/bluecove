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
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;

import javax.bluetooth.RemoteDevice;

/**
 * Helper class to create connection Proxy classes.
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 */
public abstract class BluetoothConnectionAccessAdapter implements BluetoothConnectionAccess {

	protected abstract BluetoothConnectionAccess getImpl();

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteAddress()
	 */
	public long getRemoteAddress() throws IOException {
		return getImpl().getRemoteAddress();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getRemoteDevice()
	 */
	public RemoteDevice getRemoteDevice() {
		return getImpl().getRemoteDevice();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#isClosed()
	 */
	public boolean isClosed() {
		return getImpl().isClosed();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#shutdown()
	 */
	public void shutdown() throws IOException {
		getImpl().shutdown();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#markAuthenticated()
	 */
	public void markAuthenticated() {
		getImpl().markAuthenticated();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#getSecurityOpt()
	 */
	public int getSecurityOpt() {
		return getImpl().getSecurityOpt();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#encrypt(boolean)
	 */
	public boolean encrypt(long address, boolean on) throws IOException {
		return getImpl().encrypt(address, on);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionAccess#setRemoteDevice(javax.bluetooth.RemoteDevice)
	 */
	public void setRemoteDevice(RemoteDevice remoteDevice) {
		getImpl().setRemoteDevice(remoteDevice);
	}

	public BluetoothStack getBluetoothStack() {
		return getImpl().getBluetoothStack();
	}
}
