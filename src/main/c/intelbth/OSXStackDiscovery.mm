/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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

#import "OSXStackDiscovery.h"

#define CPP_FILE "OSXStackDiscovery.mm"

@implementation OSXStackDiscovery

int deviceInquiryCount = 0;

//===========================================================================================================================
// startSearch
//===========================================================================================================================

-(BOOL) startSearch:(int)count {
    IOReturn    status;
    _count = count;
    [self   stopSearch];
    int inquiryLength = 11;

    ndebug("deviceInquiry %i startSearch", _count);
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
    ndebug("deviceInquiry %i stopSearch", _count);

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
        ndebug("deviceInquiry %i deviceFound", _count);
        [_foundDevices addObject:inDevice];
    }
}

//===========================================================================================================================
// updateDeviceInfoInList
//===========================================================================================================================

-(void) updateDeviceInfo:(IOBluetoothDevice *)inDevice {
    @synchronized(self) {
        ndebug("deviceInquiry %i deviceUpdated", _count);
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
    ndebug("deviceInquiry %i complete, [0x%08x] %u", _count, error, aborted);
    if ((!BUG_Inquiry_stop) && (_inquiry != NULL)) {
        [_inquiry clearFoundDevices];
        [_inquiry release];
        _inquiry = NULL;
    }
    @synchronized(self) {
        if (!_busy) {
            ndebug("ERROR deviceInquiry complete and WAS NOT BUZY");
        }
        _aborted = aborted;
        _error = error;
        _busy = FALSE;
    }
    MPSetEvent(*_notificationEvent, 1);
}

@end

//BUG_Inquiry_stop
OSXStackDiscovery* discoveryOneInstance = NULL;

class DeviceInquiryStart: public Runnable {
public:
    int count;
    BOOL startStatus;
    OSXStackDiscovery* discovery;

    DeviceInquiryStart(int count);
    virtual void run();

    void stopAndRelease();

    BOOL wait();
    BOOL busy();
    BOOL started();
    BOOL aborted();
    IOReturn errorCode();
    IOBluetoothDevice* getDeviceToReport();
};

DeviceInquiryStart::DeviceInquiryStart(int count) {
    name = "DeviceInquiryStart";
    this->count = count;
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
    ndebug("deviceInquiry %i run", count);
    startStatus = [discovery startSearch: count];
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
        ndebug("ERROR deviceInquiry %i can't release", count);
    }
}

RUNNABLE(DeviceInquiryRelease, "DeviceInquiryRelease") {
    DeviceInquiryStart* discovery = (DeviceInquiryStart*)pData[0];
    discovery->stopAndRelease();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runDeviceInquiryImpl
  (JNIEnv* env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {

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

    if (!callback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
        stack->deviceInquiryUnlock();
        return INQUIRY_ERROR;
    }
    deviceInquiryCount++;
    DeviceInquiryStart discovery(deviceInquiryCount);
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
            debug("deviceInquiry Started");
            break;
        }
        discovery.wait();
    }

    while ((stack != NULL) && (!stack->deviceInquiryTerminated)) {
        Edebug("deviceInquiry get device");
        IOBluetoothDevice* d = discovery.getDeviceToReport();
        if ((stack != NULL) && (d != NULL)) {
            debug("deviceInquiry device discovered");
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
            Edebug("deviceInquiry no devices");
        }

        // When deviceInquiryComplete look at the remainder of Responded devices. Do Not Wait
        if (discovery.busy()) {
            Edebug("deviceInquiry sleep");
            discovery.wait();
        } else if (d == NULL) {
            break;
        }
    }
    debug("deviceInquiry ends");

    BOOL aborted = discovery.aborted();
    IOReturn error = discovery.errorCode();
    bool terminated = stack->deviceInquiryTerminated;

    synchronousBTOperation(&discoveryRelease);

    if (stack != NULL) {
        stack->deviceInquiryUnlock();
    }

    if (aborted) {
       debug("deviceInquiry aborted");
    }

    if (stack == NULL) {
        return INQUIRY_TERMINATED;
    } else if (terminated) {
        debug("deviceInquiry terminated");
        return INQUIRY_TERMINATED;
    } else if (error != kIOReturnSuccess) {
        debug1("deviceInquiry error code [0x%08x]", error);
        return INQUIRY_ERROR;
    } else {
        return INQUIRY_COMPLETED;
    }
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer) {
    debug("StopInquiry");
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
    LongToOSxBTAddr(lData, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (deviceRef == NULL) {
        error = 1;
        return;
    }
    if (kIOReturnSuccess != IOBluetoothDeviceRemoteNameRequest(deviceRef, remoteNameRequestResponse, this, NULL)) {
        error = 1;
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
    MPSetEvent(runnable->inquiryFinishedEvent, 0);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getRemoteDeviceFriendlyName
  (JNIEnv *env, jobject, jlong address) {
    debug("getRemoteDeviceFriendlyName");
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    // Do not lock Inquiry it works fine anyway.
    //if (!stack->deviceInquiryLock(env)) {
    //    return NULL;
    //}
    GetRemoteDeviceFriendlyName runnable;
    runnable.lData = address;
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