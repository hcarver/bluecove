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
 *  @version $Id: OSXStack.cpp 1139 2007-11-07 02:41:25Z skarzhevskyy $
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
	int scanInterval = 15;

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
    [_inquiry setInquiryLength:scanInterval];

	status = [_inquiry start];

	if (status == kIOReturnSuccess) {
	    _busy = true;
		[_inquiry retain];
		return true;
	} else {
		// Failed
		_inquiry = NULL;
		return false;
	}
}

//===========================================================================================================================
// stopSearch
//===========================================================================================================================

-(void) stopSearch {
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
}

//===========================================================================================================================
// deviceInquiryDeviceFound
//===========================================================================================================================

-(void) deviceInquiryDeviceFound:(IOBluetoothDeviceInquiry*)sender	device:(IOBluetoothDevice*)device {
	[self addDeviceToList:device];
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
	[self updateDeviceInfo:device];
}

//===========================================================================================================================
// deviceInquiryComplete
//===========================================================================================================================

-(void) deviceInquiryComplete:(IOBluetoothDeviceInquiry*)sender	error:(IOReturn)error	aborted:(BOOL)aborted {
	_aborted = aborted;
	_error = error;
	_busy = false;
}

@end

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runDeviceInquiryImpl
  (JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {

    OSXJNIHelper helper;
    OSXStackDiscovery* discovery;
    BOOL startStatus;
    DeviceInquiryCallback callback;

    if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return INQUIRY_ERROR;
	}
	if (IOBluetoothLocalDeviceAvailable() == false) {
	    throwBluetoothStateException(env, "LocalDevice not ready");
		return INQUIRY_ERROR;
	}
	if (stack->deviceInquiryInProcess) {
	    throwBluetoothStateException(env, "Another inquiry already running");
	    return INQUIRY_ERROR;
	}
	stack->deviceInquiryInProcess = true;

    //if (!callback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
    //    stack->deviceInquiryInProcess = false;
    //    return INQUIRY_ERROR;
    //}

    discovery = [OSXStackDiscovery new];

    //debug("deviceInquiry startSearch");
    startStatus = [discovery startSearch];

    if (startStatus) {
	    if (!callback.callDeviceInquiryStartedCallback(env)) {
		    [discovery stopSearch];
		    [discovery release];
		    stack->deviceInquiryInProcess = false;
		    return INQUIRY_ERROR;
	    }
    } else {
        [discovery stopSearch];
		[discovery release];
		stack->deviceInquiryInProcess = false;
		return INQUIRY_ERROR;
    }

    while ((stack != NULL) && (!stack->deviceInquiryTerminated)) {
        if ([discovery started]) {
            //debug("deviceInquiry Started");
            break;
        }
    }

	while ((stack != NULL) && (!stack->deviceInquiryTerminated)) {
	    //debug("deviceInquiry get device");
	    IOBluetoothDevice* d = [discovery getDeviceToReport];
		if ((stack != NULL) && (d != NULL)) {
            debug("deviceInquiry device discovered");
            jlong deviceAddr = OSxAddrToLong([d getAddress]);
            jint deviceClass = (jint)[d getClassOfDevice];
            jstring name = OSxNewJString(env, [d getName]);
            jboolean paired = [d isPaired];
			//if (!callback.callDeviceDiscovered(env, listener, deviceAddr, deviceClass, name, paired)) {
			//	[discovery stopSearch];
            //    [discovery release];
			//	stack->deviceInquiryInProcess = false;
			//	return INQUIRY_ERROR;
			//}
		} else {
	        //debug("deviceInquiry no devices");
	    }

		// When deviceInquiryComplete look at the remainder of Responded devices. Do Not Wait
		if ([discovery busy]) {
		    //debug("deviceInquiry sleep");
		    sleep(1);
	    } else if (d == NULL) {
	        break;
	    }
    }
    debug("deviceInquiry ends");

    BOOL aborted = [discovery aborted];
    IOReturn error = [discovery error];

    [discovery stopSearch];
    [discovery release];

    if (stack != NULL) {
        stack->deviceInquiryInProcess = false;
    }

    if ((aborted) || (stack == NULL) || (stack->deviceInquiryTerminated)) {
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
	if (stack != NULL) {
		stack->deviceInquiryTerminated = true;
	}
	return true;
}

// TODO move to common Objective-C

jstring OSxNewJString(JNIEnv *env, NSString *nString) {
    jsize buflength = [nString length];
    unichar buffer[buflength];
    [nString getCharacters:buffer];
    return env->NewString((jchar *)buffer, buflength);
}

OSXJNIHelper::OSXJNIHelper() {
    autoreleasepool = [[NSAutoreleasePool alloc] init];
}

OSXJNIHelper::~OSXJNIHelper() {
    [autoreleasepool release];
}
