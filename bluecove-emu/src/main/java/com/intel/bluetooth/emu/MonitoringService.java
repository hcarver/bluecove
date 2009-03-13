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

import java.util.List;

/**
 * 
 */
public interface MonitoringService {

	public List<MonitorDevice> getDevices();

	public List<MonitorService> getServices();

	public List<MonitorConnection> getConnections();

	public void setDevicePower(long address, boolean on);

	public void setDeviceDiscoverable(long address, int mode);

	public void createThreadDumpFile(long address);

	public void shutdownJVM(long address);

	public void connectionDellayDelivery(long address, long connectionId, int msecDelay);

	public void connectionBreak(long address, long connectionId);

}
