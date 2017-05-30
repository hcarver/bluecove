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

#import "OSXStackDiscovery.h"

#include <dispatch/dispatch.h>

#import <IOBluetooth/IOBluetooth.h>

#define CPP_FILE "OSXStackDiscovery.mm"

@implementation OSXStackDiscovery

int deviceInquiryCount = 0;

//===========================================================================================================================
// startSearch
//===========================================================================================================================

-(BOOL) startSearch:(int)logID inquiryLength:(int)inquiryLength {
    IOReturn    status;
    _logID = logID;
    [self   stopSearch];

    ndebug(("deviceInquiry %i startSearch", _logID));
    _notificationEvent = &(stack->deviceInquiryNotificationEvent);

    _aborted = FALSE;
    _error = kIOReturnSuccess;
    _busy = FALSE;

    if (_foundDevices == NULL) {
        _foundDevices = [[NSMutableArray alloc] initWithCapacity:15];
        if (!_foundDevices) {
           return FALSE;
        }
    }

    _started = FALSE;
    _inquiry = [IOBluetoothDeviceInquiry inquiryWithDelegate:self];
    if (!_inquiry) {
        return FALSE;
    }
    [_inquiry setDelegate: self];
    [_inquiry setInquiryLength: inquiryLength];
    // Names are found by getRemoteDeviceFriendlyName
    [_inquiry setUpdateNewDeviceNames: FALSE];

    _finished = FALSE;
    status = [_inquiry start];

    if (status == kIOReturnSuccess) {
        _busy = TRUE;
        [_inquiry retain];
        return TRUE;
    } else {
        // Failed
        _finished = TRUE;
        _inquiry = NULL;
        return FALSE;
    }
}

//===========================================================================================================================
// stopSearch
//===========================================================================================================================

-(void) stopSearch {
    _finished = TRUE;
    ndebug(("deviceInquiry %i stopSearch", _logID));

    if (_inquiry) {
        [_inquiry stop];
        [_inquiry clearFoundDevices];
        [_inquiry release];
        _inquiry = NULL;
    }

    if (_foundDevices) {
        [_foundDevices removeAllObjects];
        [_foundDevices release];
        _foundDevices = NULL;
    }
}

//===========================================================================================================================
// Accessor methods
//===========================================================================================================================

-(BOOL) busy {
   return _busy;
}

-(BOOL) started {
   return _started;
}

-(BOOL) aborted {
    return _aborted;
}

-(IOReturn) error {
    return _error;
}

-(IOBluetoothDevice*) getDeviceToReport {
    if (_foundDevices == NULL) {
        return NULL;
    }
    IOBluetoothDevice* d = NULL;
    @synchronized(self) {
        if ([_foundDevices count] == 0) {
            return NULL;
        }
        d = [_foundDevices objectAtIndex:0];
        //[d retain];
        [_foundDevices removeObjectAtIndex:0];
    }
    return d;
}

-(BOOL) wait {
    if (![self busy]) {
        return FALSE;
    }

    return (kMPTimeoutErr == dispatch_semaphore_wait(*_notificationEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 1)));
}

//===========================================================================================================================
//  addDeviceToList
//===========================================================================================================================

-(void) addDeviceToList:(IOBluetoothDevice*)inDevice {
    @synchronized(self) {
        ndebug(("deviceInquiry %i deviceFound", _logID));
        [_foundDevices addObject:inDevice];
    }
}

//===========================================================================================================================
// updateDeviceInfoInList
//===========================================================================================================================

-(void) updateDeviceInfo:(IOBluetoothDevice *)inDevice {
    @synchronized(self) {
        ndebug(("deviceInquiry %i deviceUpdated", _logID));
        [_foundDevices addObject:inDevice];
    }
}

// IOBluetoothDeviceInquiryDelegate

//===========================================================================================================================
// deviceInquiryStarted
//===========================================================================================================================

-(void) deviceInquiryStarted:(IOBluetoothDeviceInquiry*)sender {
    _started = TRUE;
    dispatch_semaphore_signal(*_notificationEvent);
}

//===========================================================================================================================
// deviceInquiryDeviceFound
//===========================================================================================================================

-(void) deviceInquiryDeviceFound:(IOBluetoothDeviceInquiry*)sender  device:(IOBluetoothDevice*)device {
    if (_finished) {
        return;
    }
    [self addDeviceToList:device];
    dispatch_semaphore_signal(*_notificationEvent);
}

//===========================================================================================================================
// deviceInquiryUpdatingDeviceNamesStarted
//===========================================================================================================================

-(void) deviceInquiryUpdatingDeviceNamesStarted:(IOBluetoothDeviceInquiry*)sender   devicesRemaining:(int)devicesRemaining {
}

//===========================================================================================================================
// deviceInquiryDeviceNameUpdated
//===========================================================================================================================

-(void) deviceInquiryDeviceNameUpdated:(IOBluetoothDeviceInquiry*)sender    device:(IOBluetoothDevice*)device devicesRemaining:(int)devicesRemaining {
    if (_finished) {
        return;
    }
    [self updateDeviceInfo:device];
    dispatch_semaphore_signal(*_notificationEvent);
}

//===========================================================================================================================
// deviceInquiryComplete
//===========================================================================================================================

/**
 * OS x BUG. If discovery has been cancelled by stop. For next discovery deviceInquiryComplete function is called for previous Delegate Object, not for current
 */
-(void) deviceInquiryComplete:(IOBluetoothDeviceInquiry*)sender error:(IOReturn)error   aborted:(BOOL)aborted {
    // aborted may reflet the sate of previous transaction. Ignore it.
    ndebug(("deviceInquiry %i complete, [0x%08x] %u", _logID, error, aborted));
    if ((!BUG_Inquiry_stop) && (_inquiry != NULL)) {
        [_inquiry clearFoundDevices];
        [_inquiry release];
        _inquiry = NULL;
    }
    @synchronized(self) {
        if (!_busy) {
            if (!aborted) {
                ndebug(("WARN deviceInquiry complete and WAS NOT BUZY"));
            }
        } else {
            _aborted = aborted;
            // code 4 returned all the time, ignore it.
            if (error == 0x4) {
                ndebug(("deviceInquiry %i ignores error code [0x%08x]", _logID, error));
                error = kIOReturnSuccess;
            }
            _error = error;
            _busy = FALSE;
        }
        if (stack != NULL) {
            stack->deviceInquiryBusy = false;
            dispatch_semaphore_signal(stack->deviceInquiryBusyEvent); // , 1);
        }
    }
    dispatch_semaphore_signal(*_notificationEvent); // , 1);
}

@end

//BUG_Inquiry_stop
OSXStackDiscovery* discoveryOneInstance = NULL;

class DeviceInquiryStart: public Runnable {
public:
    int logID;
    int duration;
    BOOL startStatus;
    OSXStackDiscovery* discovery;

    DeviceInquiryStart(int logID, int duration);
    virtual void run();

    void stopAndRelease();

    BOOL wait();
    BOOL busy();
    BOOL started();
    BOOL aborted();
    IOReturn errorCode();
    IOBluetoothDevice* getDeviceToReport();
};

DeviceInquiryStart::DeviceInquiryStart(int logID, int duration) {
    name = "DeviceInquiryStart";
    this->logID = logID;
    this->duration = duration;
}

BOOL DeviceInquiryStart::busy() {
    return [discovery busy];
}

BOOL DeviceInquiryStart::wait() {
    return [discovery wait];
}

BOOL DeviceInquiryStart::started() {
    return [discovery started];
}

BOOL DeviceInquiryStart::aborted() {
    return [discovery aborted];
}

IOReturn DeviceInquiryStart::errorCode() {
    return [discovery error];
}

IOBluetoothDevice* DeviceInquiryStart::getDeviceToReport() {
    return [discovery getDeviceToReport];
}


void DeviceInquiryStart::run() {
    startStatus = FALSE;

    if ([IOBluetoothHostController defaultController] == nil) {
        this->error = 1;
        return;
    }
    if (BUG_Inquiry_stop) {
        if (discoveryOneInstance == NULL) {
            discoveryOneInstance = [[OSXStackDiscovery alloc] init];
            [discoveryOneInstance retain];
        }
        discovery = discoveryOneInstance;
    } else {
        discovery = [[OSXStackDiscovery alloc] init];
    }
    ndebug(("deviceInquiry %i run", logID));
    startStatus = [discovery startSearch: logID inquiryLength: duration];
    if (startStatus) {
        if (!BUG_Inquiry_stop) {
            [discovery retain];
        }
    } else {
        discovery = NULL;
    }
}

void DeviceInquiryStart::stopAndRelease() {
    if (discovery != NULL) {
        [discovery stopSearch];
        if (!BUG_Inquiry_stop) {
            [discovery release];
        }
        discovery = NULL;
    } else {
        ndebug(("ERROR deviceInquiry %i can't release", logID));
    }
}

RUNNABLE(DeviceInquiryRelease, "DeviceInquiryRelease") {
    DeviceInquiryStart* discovery = (DeviceInquiryStart*)pData[0];
    discovery->stopAndRelease();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runDeviceInquiryImpl
  (JNIEnv* env, jobject peer, jobject inquiryRunnable, jobject startedNotify, jint accessCode, jint duration, jobject listener) {

    OSXJNIHelper allocHelper;

    DeviceInquiryCallback callback;

    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return INQUIRY_ERROR;
    }

    if (!stack->deviceInquiryLock(env)) {
        return INQUIRY_ERROR;
    }
    stack->deviceInquiryTerminated = FALSE;

    if (!callback.builDeviceInquiryCallbacks(env, inquiryRunnable, startedNotify)) {
        stack->deviceInquiryUnlock();
        return INQUIRY_ERROR;
    }
    deviceInquiryCount++;
    DeviceInquiryStart discovery(deviceInquiryCount, duration);
    DeviceInquiryRelease discoveryRelease;
    discoveryRelease.pData[0] = &discovery;

    synchronousBTOperation(&discovery);

    if (discovery.startStatus) {
        if (!callback.callDeviceInquiryStartedCallback(env)) {
            synchronousBTOperation(&discoveryRelease);
            stack->deviceInquiryUnlock();
            return INQUIRY_ERROR;
        }
    } else {
        if (discovery.error == 1) {
            throwBluetoothStateException(env, "LocalDevice not ready");
        }
        synchronousBTOperation(&discoveryRelease);
        stack->deviceInquiryUnlock();
        return INQUIRY_ERROR;
    }

    while ((stack != NULL) && (!stack->deviceInquiryTerminated)) {
        if (discovery.started()) {
            debug(("deviceInquiry Started"));
            break;
        }
        discovery.wait();
    }

    while ((stack != NULL) && (!stack->deviceInquiryTerminated)) {
        Edebug(("deviceInquiry get device"));
        IOBluetoothDevice* d = discovery.getDeviceToReport();
        if ((stack != NULL) && (d != NULL)) {
            debug(("deviceInquiry device discovered"));
            jlong deviceAddr = OSxAddrToLong([d getAddress]);
            jint deviceClass = (jint)d.classOfDevice;
            jstring name = OSxNewJString(env, d.name);
            jboolean paired = [d isPaired];
            if (!callback.callDeviceDiscovered(env, listener, deviceAddr, deviceClass, name, paired)) {
                synchronousBTOperation(&discoveryRelease);
                stack->deviceInquiryUnlock();
                return INQUIRY_ERROR;
            }
        } else {
            Edebug(("deviceInquiry no devices"));
        }

        // When deviceInquiryComplete look at the remainder of Responded devices. Do Not Wait
        if (discovery.busy()) {
            Edebug(("deviceInquiry sleep"));
            discovery.wait();
        } else if (d == NULL) {
            break;
        }
    }
    debug(("deviceInquiry ends"));

    BOOL aborted = discovery.aborted();
    IOReturn error = discovery.errorCode();
    bool terminated = stack->deviceInquiryTerminated;

    synchronousBTOperation(&discoveryRelease);

    if (stack != NULL) {
        stack->deviceInquiryUnlock();
    }

    if (aborted) {
       debug(("deviceInquiry aborted"));
    }

    if (stack == NULL) {
        return INQUIRY_TERMINATED;
    } else if (terminated) {
        debug(("deviceInquiry terminated"));
        return INQUIRY_TERMINATED;
    } else if (error != kIOReturnSuccess) {
        debug(("deviceInquiry error code [0x%08x]", error));
        return INQUIRY_ERROR;
    } else {
        return INQUIRY_COMPLETED;
    }
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer) {
    debug(("StopInquiry"));
    if ((stack != NULL) && (stack->deviceInquiryInProcess)) {
        // This will dellay termiantion untill loop in runDeviceInquiryImpl will detect this flag
        stack->deviceInquiryTerminated = TRUE;
        dispatch_semaphore_signal(stack->deviceInquiryNotificationEvent); // , 1);
        return TRUE;
    } else {
        return FALSE;
    }
}

void remoteNameRequestResponse(void *userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status);

GetRemoteDeviceFriendlyName::GetRemoteDeviceFriendlyName() {
    name = "GetRemoteDeviceFriendlyName";
    inquiryFinishedEvent = dispatch_semaphore_create(0);
    delegate = [[GetRemoteDeviceFriendlyNameDelegate alloc] init];
}

GetRemoteDeviceFriendlyName::~GetRemoteDeviceFriendlyName() {
    dispatch_release(inquiryFinishedEvent);
    [delegate release];
    delegate = nil;
}

void GetRemoteDeviceFriendlyName::run() {
    [delegate remoteNameRequest:lData];
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getRemoteDeviceFriendlyName
  (JNIEnv *env, jobject, jlong address) {
    debug(("getRemoteDeviceFriendlyName"));
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    // Need to have only one Inquiry otherwise it will ends prematurely.
    if (stack->deviceInquiryBusy) {
        debug(("blocked until deviceInquiry ends"));
    }
    while ((stack != NULL) && (stack->deviceInquiryBusy)) {
                dispatch_semaphore_wait(stack->deviceInquiryBusyEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
    }
    // Call from DeviceDiscovered callback deviceInquiryInProcessMutex already locked
    //if (!stack->deviceInquiryLock(env)) {
    //    return NULL;
    //}
    GetRemoteDeviceFriendlyName runnable;
    runnable.lData = address;
    synchronousBTOperation(&runnable);

    while ((stack != NULL) && (runnable.error == 0) && (!runnable.bData)) {
                dispatch_semaphore_wait(runnable.inquiryFinishedEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
    }
    //if (stack != NULL) {
    //  stack->deviceInquiryUnlock();
    //}
    if (runnable.error) {
        throwIOException(env, "The remote device can not be contacted");
        return NULL;
    }
    return env->NewString(runnable.uData, runnable.iData);
}

@implementation GetRemoteDeviceFriendlyNameDelegate

- (id)initWithRunnable:(GetRemoteDeviceFriendlyName*)runnable
{
    _runnable = runnable;
    
    return self;
}

- (void)remoteNameRequest:(long)address
{
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(address, &btAddress);
    
    _device = [IOBluetoothDevice deviceWithAddress:(const BluetoothDeviceAddress*)&btAddress];
    if (_device == NULL) {
        _runnable->error = 1;
        return;
    }
    
    if (kIOReturnSuccess != [_device remoteNameRequest:self]) {
        _runnable->error = 1;
        _device = nil;
    }    
}

- (void)remoteNameRequestComplete:(IOBluetoothDevice *)device status:(IOReturn)status
{
    if (isRunnableCorrupted(_runnable)) {
        return;
    }
    
    if (kIOReturnSuccess != status) {
        _runnable->error = 1;
    }
    else {
        NSString* name = [device name]; // IOBluetoothDeviceGetName(deviceRef);
        if (name == NULL) {
            _runnable->error = 1;
        }
        else {
            NSData* data = [name dataUsingEncoding:NSUnicodeStringEncoding];
            UniChar* uniData = (UniChar*)[data bytes];
            _runnable->iData = MIN(RUNNABLE_DATA_MAX, [data length]);
            
            memccpy(_runnable->uData, uniData, 0, _runnable->iData);
            _runnable->bData = TRUE;
        }
    }
    
    device = nil;
    dispatch_semaphore_signal(_runnable->inquiryFinishedEvent); // , 0);
}

- (void)connectionComplete:(IOBluetoothDevice *)device status:(IOReturn)status
{
    
}

- (void)sdpQueryComplete:(IOBluetoothDevice *)device status:(IOReturn)status
{
    
}


@end
