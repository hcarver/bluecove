/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
package org.bluez;

import java.io.IOException;
import java.util.List;

import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * Abstraction interface to access BlueZ over D-Bus
 */
public interface BlueZAPI {

    public interface DeviceInquiryListener {

        public void deviceInquiryStarted();
        
        public void deviceDiscovered(String deviceAddr, String deviceName, int deviceClass, boolean paired);
        
    }
    
	public List<String> listAdapters();

	public Path getAdapter(int number);

	public Path findAdapter(String pattern) throws Error.InvalidArguments;

	public Path defaultAdapter() throws Error.InvalidArguments;

	public Adapter selectAdapter(Path adapterPath) throws DBusException;

	public String getAdapterID();

	public String getAdapterAddress();
	
	public int getAdapterDeviceClass();
	
	public String getAdapterName();
	
	public boolean isAdapterDiscoverable();
	
	public int getAdapterDiscoverableTimeout();
	
	public String getAdapterVersion();
	 
	public String getAdapterRevision();
	 
	public String getAdapterManufacturer();
	
	public boolean isAdapterPowerOn();
	
	public boolean setAdapterDiscoverable(int mode) throws DBusException;
	
	public void deviceInquiry(final DeviceInquiryListener listener) throws DBusException, InterruptedException;
	
	public void deviceInquiryCancel() throws DBusException;
	
	public String getRemoteDeviceFriendlyName(String deviceAddress) throws DBusException, IOException;
	
	public Boolean isRemoteDeviceTrusted(String deviceAddress) throws DBusException;
}
