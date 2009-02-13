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
        _foundDevices = [NSMutableArray arrayWithCapacity:15];
        if (!_foundDevices) {
           return FALSE;
        }
        [_foundDevices retain];
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
    MPEventFlags flags;
    return (kMPTimeoutErr == MPWaitForEvent(*_notificationEvent, &flags, kDurationMillisecond * 1000));
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
    MPSetEvent(*_notificationEvent, 0);
}

//===========================================================================================================================
// deviceInquiryDeviceFound
//===========================================================================================================================

-(void) deviceInquiryDeviceFound:(IOBluetoothDeviceInquiry*)sender  device:(IOBluetoothDevice*)device {
    if (_finished) {
        return;
    }
    [self addDeviceToList:device];
    MPSetEvent(*_notificationEvent, 0);
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
    MPSetEvent(*_notificationEvent, 0);
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
            MPSetEvent(stack->deviceInquiryBusyEvent, 1);
        }
    }
    MPSetEvent(*_notificationEvent, 1);
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

    if (IOBluetoothLocalDeviceAvailable() == FALSE) {
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
            jint deviceClass = (jint)[d getClassOfDevice];
            jstring name = OSxNewJString(env, [d getName]);
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
        MPSetEvent(stack->deviceInquiryNotificationEvent, 1);
        return TRUE;
    } else {
        return FALSE;
    }
}

void remoteNameRequestResponse(void *userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status);

GetRemoteDeviceFriendlyName::GetRemoteDeviceFriendlyName() {
    name = "GetRemoteDeviceFriendlyName";
    MPCreateEvent(&inquiryFinishedEvent);
}

GetRemoteDeviceFriendlyName::~GetRemoteDeviceFriendlyName() {
    MPDeleteEvent(inquiryFinishedEvent);
}

void GetRemoteDeviceFriendlyName::run() {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(jlData, &btAddress);
    deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (deviceRef == NULL) {
        error = 1;
        return;
    }
    if (kIOReturnSuccess != IOBluetoothDeviceRemoteNameRequest(deviceRef, remoteNameRequestResponse, this, NULL)) {
        error = 1;
        IOBluetoothObjectRelease(deviceRef);
    }
}

void remoteNameRequestResponse(void *userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status ) {
    GetRemoteDeviceFriendlyName* runnable = (GetRemoteDeviceFriendlyName*) userRefCon;
    if (isRunnableCorrupted(runnable)) {
        return;
    }
    if (kIOReturnSuccess != status) {
        runnable->error = 1;
    } else {
        CFStringRef name = IOBluetoothDeviceGetName(deviceRef);
        if (name == NULL) {
            runnable->error = 1;
        } else {
            CFIndex buflength = CFStringGetLength(name);
            CFRange range = {0};
            range.length = MIN(RUNNABLE_DATA_MAX, buflength);
            runnable->iData = range.length;
            CFStringGetCharacters(name, range, runnable->uData);
            runnable->bData = TRUE;
        }
    }
    IOBluetoothObjectRelease(runnable->deviceRef);
    MPSetEvent(runnable->inquiryFinishedEvent, 0);
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
        MPEventFlags flags;
        MPWaitForEvent(stack->deviceInquiryBusyEvent, &flags, kDurationMillisecond * 500);
    }
    // Call from DeviceDiscovered callback deviceInquiryInProcessMutex already locked
    //if (!stack->deviceInquiryLock(env)) {
    //    return NULL;
    //}
    GetRemoteDeviceFriendlyName runnable;
    runnable.jlData = address;
    synchronousBTOperation(&runnable);

    while ((stack != NULL) && (runnable.error == 0) && (!runnable.bData)) {
        MPEventFlags flags;
        MPWaitForEvent(runnable.inquiryFinishedEvent, &flags, kDurationMillisecond * 500);
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

RetrieveDevices::RetrieveDevices() {
    name = "RetrieveDevices";
    pairedDevices = NULL;
    favoriteDevices = NULL;
    recentDevices = NULL;
}

void RetrieveDevices::run() {
    pairedDevices = [IOBluetoothDevice pairedDevices];
    favoriteDevices = [IOBluetoothDevice favoriteDevices];
    recentDevices = [IOBluetoothDevice recentDevices:0];
}

jboolean callDeviceFound(JNIEnv *env, RetrieveDevicesCallback *callback, NSArray *devices) {
    jboolean result = JNI_TRUE;
    if (devices == NULL) {
        return JNI_TRUE;
    }
    NSEnumerator* devicesEnum = [devices objectEnumerator];
    id device = nil;
    while ( device = (IOBluetoothDevice*) [devicesEnum nextObject]) {
        jlong deviceAddr = OSxAddrToLong([device getAddress]);
        jint deviceClass = (jint)[device getClassOfDevice];
        jstring deviceName = OSxNewJString(env, [device getName]);
        jboolean paired = [device isPaired];
        if (!callback->callDeviceFoundCallback(env, deviceAddr, deviceClass, deviceName, paired)) {
            result = JNI_FALSE;
            break;
        }
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_retrieveDevicesImpl
  (JNIEnv *env, jobject peer, jint option, jobject retrieveDevicesCallback) {
    if (stack == NULL) {
        throwRuntimeException(env, cSTACK_CLOSED);
        return JNI_FALSE;
    }
    RetrieveDevicesCallback callback;
    if (!callback.builCallback(env, peer, retrieveDevicesCallback)) {
        return JNI_FALSE;
    }

    RetrieveDevices runnable;
    synchronousBTOperation(&runnable);
    if (RETRIEVEDEVICES_OPTION_CACHED == option) {
        return callDeviceFound(env, &callback, runnable.recentDevices);
    } else {
        jboolean result = callDeviceFound(env, &callback, runnable.pairedDevices);
        if (result == JNI_TRUE) {
            return callDeviceFound(env, &callback, runnable.favoriteDevices);
        } else {
            return result;
        }
    }
}

RUNNABLE(IsRemoteDeviceTrusted, "IsRemoteDeviceTrusted") {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(jlData, &btAddress);
    IOBluetoothDevice *device = [IOBluetoothDevice withAddress:&btAddress];
    bData = [device isPaired];
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isRemoteDeviceTrustedImpl
  (JNIEnv *env, jobject, jlong address) {
    IsRemoteDeviceTrusted runnable;
    runnable.jlData = address;
    synchronousBTOperation(&runnable);
    return (runnable.bData)?JNI_TRUE:JNI_FALSE;
}

RUNNABLE(IsRemoteDeviceAuthenticated, "IsRemoteDeviceAuthenticated") {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(jlData, &btAddress);
    IOBluetoothDevice *device = [IOBluetoothDevice withAddress:&btAddress];
    bData = [device isPaired] && [device isConnected];
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isRemoteDeviceAuthenticatedImpl
  (JNIEnv *env, jobject, jlong address) {
    IsRemoteDeviceAuthenticated runnable;
    runnable.jlData = address;
    synchronousBTOperation(&runnable);
    return (runnable.bData)?JNI_TRUE:JNI_FALSE;
}

//  -------- read RSSI --------

#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER
@implementation RemoteDeviceRSSIHostControllerDelegate

- (id)initWithRunnable:(GetRemoteDeviceRSSI*)runnable {
    _runnable = runnable;
    return self;
}

- (void)readRSSIForDeviceComplete:(id)controller device:(IOBluetoothDevice*)device  info:(BluetoothHCIRSSIInfo*)info    error:(IOReturn)error {
    if (!isRunnableCorrupted(_runnable)) {
        if (_runnable->bluetoothDevice && [_runnable->bluetoothDevice isEqual:device]) {
            if ((error != kIOReturnSuccess) || (info == NULL)) {
                ndebug(("ERROR: readRSSIForDeviceComplete return error"));
                _runnable->error = 1;
                _runnable->lData = error;
            } else if (info->handle == kBluetoothConnectionHandleNone) {
                ndebug(("ERROR: readRSSIForDeviceComplete no connection"));
                _runnable->error = 2;
            } else {
                _runnable->bData = TRUE;
                _runnable->iData = info->RSSIValue;
            }
            MPSetEvent(_runnable->inquiryFinishedEvent, 0);
        }
    }
}

@end

GetRemoteDeviceRSSI::GetRemoteDeviceRSSI() {
    name = "GetRemoteDeviceRSSI";
    delegate = NULL;
    MPCreateEvent(&inquiryFinishedEvent);
}

GetRemoteDeviceRSSI::~GetRemoteDeviceRSSI() {
    MPDeleteEvent(inquiryFinishedEvent);
}

void GetRemoteDeviceRSSI::run() {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(jlData, &btAddress);
    bluetoothDevice = [IOBluetoothDevice withAddress:&btAddress];
    if (bluetoothDevice == NULL) {
        this->error = 1;
        this->lData = 0;
        return;
    }
    IOBluetoothHostController* controller = [IOBluetoothHostController defaultController];
    if (controller == NULL) {
        this->error = 1;
        this->lData = 0;
        return;
    }
    orig_delegate = [controller delegate];
    delegate = [[RemoteDeviceRSSIHostControllerDelegate alloc] initWithRunnable:this];
    [delegate retain];
    [controller setDelegate:delegate];
    IOReturn rc = [controller readRSSIForDevice:bluetoothDevice];
    if (rc != noErr) {
        ndebug(("ERROR: call readRSSIForDevice failed"));
        this->error = 1;
        this->lData = rc;
    }
}

 void GetRemoteDeviceRSSI::release() {
    if (delegate != NULL) {
        IOBluetoothHostController* controller = [IOBluetoothHostController defaultController];
        [controller setDelegate:orig_delegate];
        [delegate release];
        delegate = NULL;
    }
 }

RUNNABLE(GetRemoteDeviceRSSIRelease, "GetRemoteDeviceRSSIRelease") {
    GetRemoteDeviceRSSI* r = (GetRemoteDeviceRSSI*)pData[0];
    r->release();
}

#endif

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_readRemoteDeviceRSSIImpl
  (JNIEnv *env, jobject, jlong address) {
#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER
    if (localDeviceSupportedSoftwareVersion < BLUETOOTH_VERSION_2_0) {
        // Run on Tiger
        throwIOException(env, "Not Supported on OS X Bluetooth API before 2.0");
    }
    GetRemoteDeviceRSSI runnable;
    runnable.jlData = address;
    synchronousBTOperation(&runnable);

    if ((stack != NULL) && (runnable.error == 0) && (!runnable.bData)) {
        MPEventFlags flags;
        MPWaitForEvent(runnable.inquiryFinishedEvent, &flags, kDurationMillisecond * 700);
    }

    GetRemoteDeviceRSSIRelease release;
    release.pData[0] = &runnable;
    synchronousBTOperation(&release);

    if (runnable.error) {
        switch (runnable.error) {
        case 2:
            throwIOException(env, "Error reading remote device RSSI, no connection");
            break;
        default:
            throwIOException(env, "Error reading remote device RSSI [0x%08x]", runnable.lData);
        }
        return -1;
    } else if (!runnable.bData) {
        throwIOException(env, "Error reading remote device RSSI timeout");
        return -1;
    } else {
        return runnable.iData;
    }
#else
    throwIOException(env, "Not Supported on OS X Bluetooth API before 2.0");
    return -1;
#endif
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_authenticateRemoteDeviceImpl
  (JNIEnv *env, jobject, jlong address) {
    //TODO
    return JNI_FALSE;
}