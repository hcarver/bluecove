/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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