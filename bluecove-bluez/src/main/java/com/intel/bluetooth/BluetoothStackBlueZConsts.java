/**
 *  BlueCove BlueZ module - Java library for Bluetooth on Linux
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
 *  @version $Id: BluetoothStackBlueZConsts.java 1745 2008-02-03 00:36:44Z skarzhevskyy $
 */
package com.intel.bluetooth;

import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.ServiceRecord;

/**
 * Export constants to native code
 *
 */
abstract class BluetoothStackBlueZConsts {

    public static final int NOT_DISCOVERABLE = DiscoveryAgent.NOT_DISCOVERABLE;

    public static final int GIAC = DiscoveryAgent.GIAC;

    public static final int LIAC = DiscoveryAgent.LIAC;

	static final int INQUIRY_COMPLETED = DiscoveryListener.INQUIRY_COMPLETED;

	static final int INQUIRY_TERMINATED = DiscoveryListener.INQUIRY_TERMINATED;

	static final int INQUIRY_ERROR = DiscoveryListener.INQUIRY_ERROR;

	static final int SERVICE_SEARCH_COMPLETED = DiscoveryListener.SERVICE_SEARCH_COMPLETED;

	static final int SERVICE_SEARCH_TERMINATED = DiscoveryListener.SERVICE_SEARCH_TERMINATED;

	static final int SERVICE_SEARCH_ERROR = DiscoveryListener.SERVICE_SEARCH_ERROR;

	static final int SERVICE_SEARCH_NO_RECORDS = DiscoveryListener.SERVICE_SEARCH_NO_RECORDS;

	static final int SERVICE_SEARCH_DEVICE_NOT_REACHABLE = DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE;

	static final int NOAUTHENTICATE_NOENCRYPT = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;

	static final int AUTHENTICATE_NOENCRYPT = ServiceRecord.AUTHENTICATE_NOENCRYPT;

	static final int AUTHENTICATE_ENCRYPT = ServiceRecord.AUTHENTICATE_ENCRYPT;

	static final int DataElement_NULL = DataElement.NULL;

	static final int DataElement_U_INT_1 = DataElement.U_INT_1;

	static final int DataElement_U_INT_2 = DataElement.U_INT_2;

	static final int DataElement_U_INT_4 = DataElement.U_INT_4;

	static final int DataElement_U_INT_8 = DataElement.U_INT_8;

	static final int DataElement_U_INT_16 = DataElement.U_INT_16;

	static final int DataElement_INT_1 = DataElement.INT_1;

	static final int DataElement_INT_2 = DataElement.INT_2;

	static final int DataElement_INT_4 = DataElement.INT_4;

	static final int DataElement_INT_8 = DataElement.INT_8;

	static final int DataElement_INT_16 = DataElement.INT_16;

	static final int DataElement_URL = DataElement.URL;

	static final int DataElement_UUID = DataElement.UUID;

	static final int DataElement_BOOL = DataElement.BOOL;

	static final int DataElement_STRING = DataElement.STRING;

	static final int DataElement_DATSEQ = DataElement.DATSEQ;

	static final int DataElement_DATALT = DataElement.DATALT;

	static final int CONNECTION_ERROR_UNKNOWN_PSM = BluetoothConnectionException.UNKNOWN_PSM;

	static final int CONNECTION_ERROR_SECURITY_BLOCK = BluetoothConnectionException.SECURITY_BLOCK;

	static final int CONNECTION_ERROR_NO_RESOURCES = BluetoothConnectionException.NO_RESOURCES;

	static final int CONNECTION_ERROR_FAILED_NOINFO = BluetoothConnectionException.FAILED_NOINFO;

	static final int CONNECTION_ERROR_TIMEOUT = BluetoothConnectionException.TIMEOUT;

	static final int CONNECTION_ERROR_UNACCEPTABLE_PARAMS = BluetoothConnectionException.UNACCEPTABLE_PARAMS;
}
