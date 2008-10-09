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

#ifndef __OSXSTACK_H__
#define __OSXSTACK_H__

#define EXT_DEBUG

#include "common.h"
#include "commonObjects.h"

#define BLUETOOTH_VERSION_USE_CURRENT

#include "com_intel_bluetooth_BluetoothStackOSX.h"

#include <IOBluetooth/IOBluetoothUserLib.h>

class OSXStack {
public:
	OSXStack();
	~OSXStack();

public:
	pthread_mutex_t deviceInquiryInProcessMutex;
	MPEventID deviceInquiryNotificationEvent;
	MPEventID deviceInquiryBusyEvent;
	MPEventID deviceInquiryFinishedEvent;
	volatile BOOL deviceInquiryInProcess;
	volatile BOOL deviceInquiryBusy;
	volatile BOOL deviceInquiryTerminated;
	BOOL deviceInquiryLock(JNIEnv* env);
	BOOL deviceInquiryUnlock();
	ObjectPool* commPool;
};

#define RUNNABLE_DATA_MAX 255

class Runnable {
public:
    long magic1b;
	long magic2b;
    char* name;
    int error;

    // Data passes and received from thread
    char sData[RUNNABLE_DATA_MAX];
    UniChar uData[RUNNABLE_DATA_MAX];
    int iData;
    long lData;
    bool bData;
    void* pData[RUNNABLE_DATA_MAX];

    long magic1e;
	long magic2e;

    Runnable();
    ~Runnable();
    virtual void run() = 0;
};

BOOL isRunnableCorrupted(Runnable* );

// parameterized macro to create class and its function run()

//TODO find a way to define a class with typed variables.

#define RUNNABLE(className, nameString) \
class className: public Runnable { \
public: \
    className(); \
    virtual void run(); \
};\
className::className() { \
    name = nameString;\
} \
void className::run()


void synchronousBTOperation(Runnable* runnable);

void OSxAddrToString(char* addressString, const BluetoothDeviceAddress* addr);
jlong OSxAddrToLong(const BluetoothDeviceAddress* addr);
void LongToOSxBTAddr(jlong longAddr, BluetoothDeviceAddress* addr);

jstring OSxNewJString(JNIEnv *env, NSString *nString);

class OSXJNIHelper{
private:
	NSAutoreleasePool *autoreleasepool;
public:
	OSXJNIHelper();
	~OSXJNIHelper();
};


extern OSXStack* stack;
extern jint localDeviceSupportedSoftwareVersion;

#endif // __OSXSTACK_H__