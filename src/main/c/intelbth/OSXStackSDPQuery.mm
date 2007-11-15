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
 *  @version $Id: OSXStackDiscovery.mm 1174 2007-11-12 04:19:25Z skarzhevskyy $
 */

#import "OSXStackSDPQuery.h"

#define CPP_FILE "OSXStackSDPQuery.mm"

StackSDPQueryStart::StackSDPQueryStart() {
    name = "StackSDPQueryStart";
    complete = FALSE;
    error = 0;
}

void callbackSDPQueryIsComplete(void* userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status) {
    if (!((StackSDPQueryStart*)userRefCon)->isCorrupted()) {
        ((StackSDPQueryStart*)userRefCon)->sdpQueryComplete(deviceRef, status);
    }
}

void StackSDPQueryStart::run() {
    startTime = CFDateCreate(kCFAllocatorDefault, CFAbsoluteTimeGetCurrent());

    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(address, &btAddress);
    IOBluetoothDeviceRef dev = IOBluetoothDeviceCreateWithAddress(&btAddress);
    status = IOBluetoothDevicePerformSDPQuery(dev, callbackSDPQueryIsComplete, this);
    if (kIOReturnSuccess != status) {
        error = 1;
    }
}

void StackSDPQueryStart::sdpQueryComplete(IOBluetoothDeviceRef deviceRef, IOReturn status) {
    this->status = status;
    if (kIOReturnSuccess != status) {
        this->error = 1;
    } else {
        CFArrayRef services = IOBluetoothDeviceGetServices(deviceRef);
        if (services != NULL) {
            recordsSize = CFArrayGetCount(services);
            CFDateRef updatedTime = IOBluetoothDeviceGetLastServicesUpdate(deviceRef);
            if (CFDateGetTimeIntervalSinceDate(updatedTime, this->startTime) < 0) {
                this->status = kIOReturnNotFound;
                this->error = 1;
            }
        } else {
            recordsSize = 0;
        }
    }
    CFRelease(this->startTime);
    this->complete = TRUE;
    if (stack != NULL) {
        MPSetEvent(stack->deviceInquiryNotificationEvent, 1);
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runSearchServicesImpl
(JNIEnv *env, jobject, jlong address, jint transID) {
    StackSDPQueryStart runnable;
    runnable.address = address;
    synchronousBTOperation(&runnable);
    while ((stack != NULL) && (runnable.error == 0) && (!runnable.complete)) {
        MPEventFlags flags;
        MPWaitForEvent(stack->deviceInquiryNotificationEvent, &flags, kDurationMillisecond * 500);
    }
    if (stack == NULL) {
        return 0;
    }
    if (runnable.error) {
        if (runnable.status == kIOReturnNotFound) {
			throwException(env, "com/intel/bluetooth/SearchServicesDeviceNotReachableException", "");
		} else {
		    debug1("SearchServices error 0x%08x", runnable.status);
            throwException(env, "com/intel/bluetooth/SearchServicesException", "");
        }
        return 0;
    }
    return runnable.recordsSize;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_cancelServiceSearchImpl
(JNIEnv* env, jobject, jint transID) {
}