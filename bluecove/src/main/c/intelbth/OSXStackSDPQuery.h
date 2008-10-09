/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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

#import <Cocoa/Cocoa.h>

#include "OSXStack.h"

class StackSDPQueryStart: public Runnable {
public:
    volatile BOOL complete;
    jlong address;
    CFAbsoluteTime startTime;
    volatile IOReturn status;
    volatile int recordsSize;
    IOBluetoothDeviceRef deviceRef;

    StackSDPQueryStart();
    virtual void run();

    void sdpQueryComplete(IOBluetoothDeviceRef deviceRef, IOReturn status);
};

#define DATA_BLOB_MAX  0x4000

class GetAttributeDataElement: public Runnable {
public:
    jlong address;
    jlong serviceRecordIndex;
    jint attrID;

    // To avoid memory allocation problem we return standard BLOB to Java thread. See com.intel.bluetooth.SDPInputStream
    int   dataLen;
    UInt8 data[DATA_BLOB_MAX];

    GetAttributeDataElement();
    virtual void run();

    void getData(const IOBluetoothSDPDataElementRef dataElement);
};

class SDPOutputStream {
public:
    CFMutableDataRef data;

    SDPOutputStream();
    ~SDPOutputStream();

    void write(const UInt8 byte);
    void write(const UInt8 *bytes, CFIndex length);
    void writeLong(UInt64 l, int size);

    BOOL writeElement(const IOBluetoothSDPDataElementRef dataElement);
    int getLength(const IOBluetoothSDPDataElementRef dataElement);
    void getBytes(int max, int*  dataLen, UInt8* buf);
};