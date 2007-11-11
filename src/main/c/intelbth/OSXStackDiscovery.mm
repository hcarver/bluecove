/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

#import <Foundation/NSDictionary.h>
#import <IOBluetooth/IOBluetoothUserLib.h>

#import "OSXStackDiscovery.h"


#define CPP_FILE "OSXStackDiscovery.mm"

@implementation OSXStackDiscovery

//===========================================================================================================================
// startSearch
//===========================================================================================================================

-(BOOL) startSearch {
	IOReturn	status;
	[self	stopSearch];
	int inquiryLength = 15;

    _notificationEvent = &(stack->deviceInquiryNotificationEvent);

    _aborted = false;
    _error = kIOReturnSuccess;
	_busy = false;

	if (_foundDevices == NULL) {
		_foundDevices = [[NSMutableArray alloc] initWithCapacity:1];
		if (!_foundDevices) {
		   return false;
        }
		[_foundDevices retain];
	}

    _started = false;
	_inquiry = [IOBluetoothDeviceInquiry inquiryWithDelegate:self];
    if (!_inquiry) {
	    return false;
    }
    [_inquiry setInquiryLength:inquiryLength];
    [_inquiry setUpdateNewDeviceNames: false];

    _finished = false;
	status = [_inquiry start];

	if (status == kIOReturnSuccess) {
	    _busy = true;
		[_inquiry retain];
		return true;
	} else {
		// Failed
		_finished = true;
		_inquiry = NULL;
		return false;
	}
}

//===========================================================================================================================
// stopSearch
//===========================================================================================================================

-(void) stopSearch {
	_finished = true;
	if (_inquiry) {
		[_inquiry stop];
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
    if ([_foundDevices count] == 0) {
        return NULL;
    }
    IOBluetoothDevice* d = [[_foundDevices objectAtIndex:0] retain];
    [_foundDevices removeObjectAtIndex:0];
    return d;
}

-(BOOL) wait {
    if (!_busy) {
        return false;
    }
    MPEventFlags flags;
    return (kMPTimeoutErr == MPWaitForEvent(*_notificationEvent, &flags, kDurationMillisecond * 500));
}

//===========================================================================================================================
//	addDeviceToList
//===========================================================================================================================

-(void) addDeviceToList:(IOBluetoothDevice*)inDevice {
    [_foundDevices addObject:inDevice];
}

//===========================================================================================================================
// updateDeviceInfoInList
//===========================================================================================================================

-(void) updateDeviceInfo:(IOBluetoothDevice *)inDevice {
    [_foundDevices addObject:inDevice];
}

// IOBluetoothDeviceInquiryDelegate

//===========================================================================================================================
// deviceInquiryStarted
//===========================================================================================================================

-(void) deviceInquiryStarted:(IOBluetoothDeviceInquiry*)sender {
    _started = true;
    MPSetEvent(*_notificationEvent, 0);
}

//===========================================================================================================================
// deviceInquiryDeviceFound
//===========================================================================================================================

-(void) deviceInquiryDeviceFound:(IOBluetoothDeviceInquiry*)sender	device:(IOBluetoothDevice*)device {
	if (_finished) {
	    return;
	}
	[self addDeviceToList:device];
	MPSetEvent(*_notificationEvent, 0);
}

//===========================================================================================================================
// deviceInquiryUpdatingDeviceNamesStarted
//===========================================================================================================================

-(void) deviceInquiryUpdatingDeviceNamesStarted:(IOBluetoothDeviceInquiry*)sender	devicesRemaining:(int)devicesRemaining {
}

//===========================================================================================================================
// deviceInquiryDeviceNameUpdated
//===========================================================================================================================

-(void) deviceInquiryDeviceNameUpdated:(IOBluetoothDeviceInquiry*)sender	device:(IOBluetoothDevice*)device devicesRemaining:(int)devicesRemaining {
	if (_finished) {
	    return;
	}
	[self updateDeviceInfo:device];
	MPSetEvent(*_notificationEvent, 0);
}

//===========================================================================================================================
// deviceInquiryComplete
//===========================================================================================================================

-(void) deviceInquiryComplete:(IOBluetoothDeviceInquiry*)sender	error:(IOReturn)error	aborted:(BOOL)aborted {
	ndebug("deviceInquiry complete");
	_aborted = aborted;
	_error = error;
	_busy = false;
	MPSetEvent(*_notificationEvent, 1);
}

@end

class DeviceInquiryStart: public Runnable {
public:
    BOOL startStatus;
    OSXStackDiscovery* discovery;

    DeviceInquiryStart();
    virtual void run();

    void stopAndRelease();

    BOOL wait();
    BOOL busy();
    BOOL started();
    BOOL aborted();
    IOReturn errorCode();
    IOBluetoothDevice* getDeviceToReport();
};

DeviceInquiryStart::DeviceInquiryStart() {
    name = "DeviceInquiryStart";
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
    startStatus = false;

    if (IOBluetoothLocalDeviceAvailable() == false) {
        this->error = 1;
        return;
    }

    discovery = [OSXStackDiscovery new];
    ndebug("deviceInquiry startSearch");
    startStatus = [discovery startSearch];
    if (startStatus) {
        [discovery retain];
    } else {
        discovery = NULL;
    }
}

void DeviceInquiryStart::stopAndRelease() {
    if (discovery != NULL) {
        [discovery stopSearch];
		[discovery release];
		discovery = NULL;
    }
}

RUNNABLE(DeviceInquiryRelease, "DeviceInquiryRelease") {
    DeviceInquiryStart* discovery = (DeviceInquiryStart*)pData[0];
    discovery->stopAndRelease();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runDeviceInquiryImpl
  (JNIEnv* env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {

    OSXJNIHelper allocHelper;

    DeviceInquiryStart discovery;
    DeviceInquiryRelease discoveryRelease;
    DeviceInquiryCallback callback;

    if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return INQUIRY_ERROR;
	}

	if (!stack->deviceInquiryLock(env)) {
	    return INQUIRY_ERROR;
	}
	stack->deviceInquiryTerminated = false;

    if (!callback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
        stack->deviceInquiryUnlock();
        return INQUIRY_ERROR;
    }
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
	    //debug("deviceInquiry get device");
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
	        //debug("deviceInquiry no devices");
	    }

		// When deviceInquiryComplete look at the remainder of Responded devices. Do Not Wait
		if (discovery.busy()) {
		    //debug("deviceInquiry sleep");
		    discovery.wait();
	    } else if (d == NULL) {
	        break;
	    }
    }
    debug("deviceInquiry ends");

    BOOL aborted = discovery.aborted();
    IOReturn error = discovery.errorCode();

    synchronousBTOperation(&discoveryRelease);

    if (stack != NULL) {
        stack->deviceInquiryUnlock();
    }

    if ((aborted) || (stack == NULL)) {
		return INQUIRY_TERMINATED;
	} else if (stack->deviceInquiryTerminated) {
	    return INQUIRY_TERMINATED;
	} else if (error != kIOReturnSuccess) {
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
		stack->deviceInquiryTerminated = true;
		MPSetEvent(stack->deviceInquiryNotificationEvent, 1);
		return true;
	} else {
	    return false;
	}
}

void remoteNameRequestResponse(void *userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status);

RUNNABLE(GetRemoteDeviceFriendlyName, "GetRemoteDeviceFriendlyName") {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(lData, &btAddress);
    IOBluetoothDeviceRef dev = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (kIOReturnSuccess != IOBluetoothDeviceRemoteNameRequest(dev, remoteNameRequestResponse, this, NULL)) {
        error = 1;
    }
}

void remoteNameRequestResponse(void *userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status ) {
	GetRemoteDeviceFriendlyName* runnable = (GetRemoteDeviceFriendlyName*) userRefCon;
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
	        runnable->bData = true;
	    }
    }
	MPSetEvent(stack->deviceInquiryNotificationEvent, 0);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getRemoteDeviceFriendlyName
  (JNIEnv *env, jobject, jlong address) {
    debug("getRemoteDeviceFriendlyName");
    if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}

	if (!stack->deviceInquiryLock(env)) {
	    return NULL;
	}
	GetRemoteDeviceFriendlyName runnable;
	runnable.lData = address;
    synchronousBTOperation(&runnable);

    while ((stack != NULL) && (runnable.error == 0) && (!runnable.bData)) {
        MPEventFlags flags;
        MPWaitForEvent(stack->deviceInquiryNotificationEvent, &flags, kDurationMillisecond * 500);
    }
	if (stack != NULL) {
        stack->deviceInquiryUnlock();
    }
    if (runnable.error) {
        throwIOException(env, "The remote device can not be contacted");
        return NULL;
    }
    return env->NewString(runnable.uData, runnable.iData);
}