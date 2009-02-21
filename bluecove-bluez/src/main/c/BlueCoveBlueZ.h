/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
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
 * @version $Id: BlueCoveBlueZ.h 1745 2008-02-03 00:36:44Z skarzhevskyy $
 */

#ifndef _BLUECOVEBLUEZ_H
#define _BLUECOVEBLUEZ_H

#include <jni.h>
#include <unistd.h>
#include <errno.h>
#include <malloc.h>

#include <bluetooth/bluetooth.h>
#include <bluetooth/sdp.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

#include "com_intel_bluetooth_BluetoothStackBlueZDBus.h"
#include "com_intel_bluetooth_BluetoothStackBlueZDBusConsts.h"
#include "common.h"

#define LOCALDEVICE_ACCESS_TIMEOUT 5000
#define READ_REMOTE_NAME_TIMEOUT 5000
#define DEVICE_NAME_MAX_SIZE 248

int deviceClassBytesToInt(uint8_t* deviceClass);

jlong deviceAddrToLong(bdaddr_t* address);
void longToDeviceAddr(jlong addr, bdaddr_t* address);

void reverseArray(jbyte* array,int length);

jlong ptr2jlong(void * ptr);
void* jlong2ptr(jlong l);

#define NOT_DISCOVERABLE com_intel_bluetooth_BluetoothStackBlueZDBusConsts_NOT_DISCOVERABLE
#define GIAC             com_intel_bluetooth_BluetoothStackBlueZDBusConsts_GIAC
#define LIAC             com_intel_bluetooth_BluetoothStackBlueZDBusConsts_LIAC

#define INQUIRY_COMPLETED  com_intel_bluetooth_BluetoothStackBlueZDBusConsts_INQUIRY_COMPLETED
#define INQUIRY_TERMINATED com_intel_bluetooth_BluetoothStackBlueZDBusConsts_INQUIRY_TERMINATED
#define INQUIRY_ERROR      com_intel_bluetooth_BluetoothStackBlueZDBusConsts_INQUIRY_ERROR

#define SERVICE_SEARCH_COMPLETED            com_intel_bluetooth_BluetoothStackBlueZDBusConsts_SERVICE_SEARCH_COMPLETED
#define SERVICE_SEARCH_TERMINATED           com_intel_bluetooth_BluetoothStackBlueZDBusConsts_SERVICE_SEARCH_TERMINATED
#define SERVICE_SEARCH_ERROR                com_intel_bluetooth_BluetoothStackBlueZDBusConsts_SERVICE_SEARCH_ERROR
#define SERVICE_SEARCH_NO_RECORDS           com_intel_bluetooth_BluetoothStackBlueZDBusConsts_SERVICE_SEARCH_NO_RECORDS
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE com_intel_bluetooth_BluetoothStackBlueZDBusConsts_SERVICE_SEARCH_DEVICE_NOT_REACHABLE

#define NOAUTHENTICATE_NOENCRYPT com_intel_bluetooth_BluetoothStackBlueZDBusConsts_NOAUTHENTICATE_NOENCRYPT
#define AUTHENTICATE_NOENCRYPT   com_intel_bluetooth_BluetoothStackBlueZDBusConsts_AUTHENTICATE_NOENCRYPT
#define AUTHENTICATE_ENCRYPT     com_intel_bluetooth_BluetoothStackBlueZDBusConsts_AUTHENTICATE_ENCRYPT


#define DATA_ELEMENT_TYPE_NULL     com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_NULL
#define DATA_ELEMENT_TYPE_U_INT_1  com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_U_INT_1
#define DATA_ELEMENT_TYPE_U_INT_2  com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_U_INT_2
#define DATA_ELEMENT_TYPE_U_INT_4  com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_U_INT_4
#define DATA_ELEMENT_TYPE_U_INT_8  com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_U_INT_8
#define DATA_ELEMENT_TYPE_U_INT_16 com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_U_INT_16
#define DATA_ELEMENT_TYPE_INT_1    com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_INT_1
#define DATA_ELEMENT_TYPE_INT_2    com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_INT_2
#define DATA_ELEMENT_TYPE_INT_4    com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_INT_4
#define DATA_ELEMENT_TYPE_INT_8    com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_INT_8
#define DATA_ELEMENT_TYPE_INT_16   com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_INT_16
#define DATA_ELEMENT_TYPE_URL      com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_URL
#define DATA_ELEMENT_TYPE_UUID     com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_UUID
#define DATA_ELEMENT_TYPE_BOOL     com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_BOOL
#define DATA_ELEMENT_TYPE_STRING   com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_STRING
#define DATA_ELEMENT_TYPE_DATSEQ   com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_DATSEQ
#define DATA_ELEMENT_TYPE_DATALT   com_intel_bluetooth_BluetoothStackBlueZDBusConsts_DataElement_DATALT

#define BT_CONNECTION_ERROR_UNKNOWN_PSM         com_intel_bluetooth_BluetoothStackBlueZDBusConsts_CONNECTION_ERROR_UNKNOWN_PSM
#define BT_CONNECTION_ERROR_SECURITY_BLOCK      com_intel_bluetooth_BluetoothStackBlueZDBusConsts_CONNECTION_ERROR_SECURITY_BLOCK
#define BT_CONNECTION_ERROR_NO_RESOURCES        com_intel_bluetooth_BluetoothStackBlueZDBusConsts_CONNECTION_ERROR_NO_RESOURCES
#define BT_CONNECTION_ERROR_FAILED_NOINFO       com_intel_bluetooth_BluetoothStackBlueZDBusConsts_CONNECTION_ERROR_FAILED_NOINFO
#define BT_CONNECTION_ERROR_TIMEOUT             com_intel_bluetooth_BluetoothStackBlueZDBusConsts_CONNECTION_ERROR_TIMEOUT
#define BT_CONNECTION_ERROR_UNACCEPTABLE_PARAMS com_intel_bluetooth_BluetoothStackBlueZDBusConsts_CONNECTION_ERROR_UNACCEPTABLE_PARAMS

#endif  /* _BLUECOVEBLUEZ_H */

