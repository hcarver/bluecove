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
 *  @version $Id: OSXStackRFCOMM.mm 1244 2007-11-27 04:06:32Z skarzhevskyy $
 */

#import "OSXStackChannelController.h"

#define CPP_FILE "OSXStackChannelController.mm"

ChannelController::ChannelController() {
    openStatus = kIOReturnSuccess;
    isClosed = false;
    isBasebandConnected = false;
	isConnected = false;
	bluetoothDevice = NULL;
	MPCreateEvent(&notificationEvent);
	MPCreateEvent(&writeCompleteNotificationEvent);
}

ChannelController::~ChannelController() {
    MPSetEvent(notificationEvent, 0);
    MPSetEvent(writeCompleteNotificationEvent, 0);
    MPDeleteEvent(notificationEvent);
    MPDeleteEvent(writeCompleteNotificationEvent);
}

BOOL ChannelController::waitForConnection(JNIEnv *env, jobject peer, BOOL baseband, jint timeout) {
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent ();
    char* name = "";
    if (baseband) {
        name = "baseband ";
    }
    while ((stack != NULL) && (!isClosed) && (openStatus == kIOReturnSuccess)) {
        if (baseband && isBasebandConnected) {
            break;
        } else if (!baseband && isConnected) {
            break;
        }
        MPEventFlags flags;
        MPWaitForEvent(notificationEvent, &flags, kDurationMillisecond * 500);
        if (isCurrentThreadInterrupted(env, peer)) {
			debug(("Interrupted while reading"));
			return false;
		}
        CFAbsoluteTime nowTime = CFAbsoluteTimeGetCurrent ();
        if ((timeout > 0) && ((nowTime - startTime) * 1000  > timeout)) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_TIMEOUT, "%sconnection timeout", name);
        }
    }
    if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return false;
	}

    if (openStatus != kIOReturnSuccess) {
        isConnected = false;
        throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open %sconnection(2) [0x%08x]", name, openStatus);
        return false;
    }

    if (isClosed) {
	    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open %sconnection(3)", name);
	    return false;
    }

    if (baseband && isBasebandConnected) {
        return true;
    } else if (!baseband && isConnected) {
        return true;
    }

    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open %sconnection", name);
	return false;
}

BasebandConnectionOpen::BasebandConnectionOpen() {
    name = "BasebandConnectionOpen";
}

void BasebandConnectionOpen::run() {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(this->address, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (deviceRef == NULL) {
        error = 1;
        return;
    }
    comm->address = this->address;
    comm->isClosed = false;

    comm->initDelegate();
    comm->bluetoothDevice = [IOBluetoothDevice withDeviceRef:deviceRef];
    if (comm->bluetoothDevice == NULL) {
        error = 1;
        return;
    }
    if ([comm->bluetoothDevice isConnected]) {
        ndebug(("baseband connection to the device exists"));
        //comm->isBasebandConnected = true;
        //return;
    }

    id target = comm->getDelegate();
    BluetoothHCIPageTimeout pageTimeoutValue = this->timeout;
    BOOL authenticationRequired = this->authenticate;
    status = [comm->bluetoothDevice openConnection:target withPageTimeout:pageTimeoutValue authenticationRequired:authenticationRequired];
    if (status != kIOReturnSuccess) {
        error = 1;
        return;
    }
}

BasebandConnectionGetOptions::BasebandConnectionGetOptions() {
    name = "BasebandConnectionGetOptions";
}

void BasebandConnectionGetOptions::run() {
    if (comm->bluetoothDevice == NULL) {
        error = 1;
        return;
    }
    BluetoothHCIEncryptionMode em = [comm->bluetoothDevice getEncryptionMode];
    encrypted = (em != kEncryptionDisabled);
}
