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

#import "OSXStackRFCOMMServer.h"

#define CPP_FILE "OSXStackRFCOMMServer.mm"

static NSString *kServiceItemKeyServiceClassIDList = @"0001 - ServiceClassIDList";
static NSString *kServiceItemKeyServiceName = @"0100 - ServiceName*";
static NSString *kServiceItemKeyProtocolDescriptorList = @"0004 - ProtocolDescriptorList";

RFCOMMServerController* validRFCOMMServerControllerHandle(JNIEnv *env, jlong handle) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (RFCOMMServerController*)stack->commPool->getObject(env, handle, 'R');
}

RFCOMMServerController::RFCOMMServerController() {
    isClosed = false;
    sdpEntries = NULL;
    sdpServiceRecordHandle = 0;
    rfcommChannelID = 0;
    incomingChannelNotification = NULL;
    MPCreateEvent(&incomingChannelNotificationEvent);
}

RFCOMMServerController::~RFCOMMServerController() {
    isClosed = true;
    MPSetEvent(incomingChannelNotificationEvent, 0);
    MPDeleteEvent(incomingChannelNotificationEvent);
}

void RFCOMMServerController::init() {
    sdpEntries = [NSMutableDictionary dictionaryWithCapacity:256];
}

IOReturn RFCOMMServerController::publish() {
    // publish the service
	IOBluetoothSDPServiceRecordRef serviceRecordRef;
	IOReturn status = IOBluetoothAddServiceDict((CFDictionaryRef)sdpEntries, &serviceRecordRef);
    if (status != kIOReturnSuccess) {
        ndebug("failed to IOBluetoothAddServiceDict");
        return status;
    }

	IOBluetoothSDPServiceRecord *serviceRecord = [IOBluetoothSDPServiceRecord withSDPServiceRecordRef:serviceRecordRef];
	if (serviceRecord == NULL) {
	    ndebug("failed to create IOBluetoothSDPServiceRecord");
	} else {
	    // get service channel ID & service record handle
	    status = [serviceRecord getRFCOMMChannelID:&rfcommChannelID];
	    if (status != kIOReturnSuccess) {
		    ndebug("failed to getRFCOMMChannelID");
		} else {
		    status = [serviceRecord getServiceRecordHandle:&sdpServiceRecordHandle];
	    }
	}

    // cleanup
	IOBluetoothObjectRelease(serviceRecordRef);

	return status;
}

void RFCOMMServerController::close() {
    isClosed = true;
    MPSetEvent(incomingChannelNotificationEvent, 0);

    if (sdpServiceRecordHandle != 0) {
        IOBluetoothRemoveServiceWithRecordHandle(sdpServiceRecordHandle);
        sdpServiceRecordHandle = 0;
    }

    // Unregisters the notification:
    if (incomingChannelNotification != NULL) {
        IOBluetoothUserNotificationUnregister(incomingChannelNotification);
		incomingChannelNotification = NULL;
	}
}

RFCOMMServicePublish::RFCOMMServicePublish() {
    name = "RFCOMMServicePublish";
}

void RFCOMMServicePublish::run() {
    comm->init();

    NSString* srvName = [NSString stringWithCharacters:(UniChar*)serviceName length:serviceNameLength];
    [comm->sdpEntries setObject:srvName forKey:kServiceItemKeyServiceName];

    NSMutableArray *currentServiceList = [comm->sdpEntries objectForKey:kServiceItemKeyServiceClassIDList];
	if (currentServiceList == nil) {
		currentServiceList = [NSMutableArray array];
	}
	[currentServiceList addObject:[NSData dataWithBytes:uuidValue length:uuidValueLength]];
	// update dict
	[comm->sdpEntries setObject:currentServiceList forKey:kServiceItemKeyServiceClassIDList];


   	// publish the service
	status = comm->publish();

	if (status != kIOReturnSuccess) {
	    error = 1;
	}
}

RUNNABLE(RFCOMMServiceClose, "RFCOMMServiceClose") {
    RFCOMMServerController* comm = (RFCOMMServerController*)pData[0];
    comm->close();
}

void RFCOMMServiceCloseExec(RFCOMMServerController* comm) {
    RFCOMMServiceClose runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	comm->readyToFree = TRUE;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_rfServerCreateImpl
  (JNIEnv *env, jobject, jbyteArray uuidValue, jboolean obexSrv, jstring name, jboolean authenticate, jboolean encrypt) {
    RFCOMMServerController* comm = new RFCOMMServerController();
	if (!stack->commPool->addObject(comm, 'R')) {
		delete comm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}
	RFCOMMServicePublish runnable;
	runnable.comm = comm;
	runnable.uuidValue = env->GetByteArrayElements(uuidValue, 0);
	runnable.uuidValueLength = env->GetArrayLength(uuidValue);
    runnable.serviceName = env->GetStringChars(name, 0);
    runnable.serviceNameLength = env->GetStringLength(name);
    runnable.obexSrv = obexSrv;
    runnable.authenticate = authenticate;
    runnable.encrypt = encrypt;
    synchronousBTOperation(&runnable);

    env->ReleaseByteArrayElements(uuidValue, runnable.uuidValue, 0);
    env->ReleaseStringChars(name, runnable.serviceName);

    if (runnable.error != 0) {
        RFCOMMServiceCloseExec(comm);
        throwIOExceptionExt(env, "Failed to create RFCOMM service [0x%08x]", runnable.status);
        return 0;
    }

    return comm->internalHandle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_rfServerGetChannelID
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMServerController* comm = validRFCOMMServerControllerHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
    return comm->rfcommChannelID;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_rfServerCloseImpl
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMServerController* comm = validRFCOMMServerControllerHandle(env, handle);
    if (comm == NULL) {
		return;
	}
	RFCOMMServiceCloseExec(comm);
}

void rfcommServiceOpenNotificationCallback(void *userRefCon, IOBluetoothUserNotificationRef inRef, IOBluetoothObjectRef objectRef ) {
    ndebug("rfcommServiceOpenNotificationCallback");
}

RUNNABLE(RFCOMMServiceAcceptAndOpen, "RFCOMMServiceAcceptAndOpen") {
    RFCOMMServerController* comm = (RFCOMMServerController*)pData[0];
    comm->incomingChannelNotification = IOBluetoothRegisterForFilteredRFCOMMChannelOpenNotifications(rfcommServiceOpenNotificationCallback, comm, comm->rfcommChannelID, kIOBluetoothUserNotificationChannelDirectionIncoming);
    if (comm->incomingChannelNotification == NULL) {
        error = 1;
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_rfServerAcceptAndOpenRfServerConnection
  (JNIEnv *env, jobject peer, jlong handle) {
    RFCOMMServerController* comm = validRFCOMMServerControllerHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
    if (comm->incomingChannelNotification == NULL) {
        RFCOMMServiceAcceptAndOpen runnable;
	    runnable.pData[0] = comm;
        synchronousBTOperation(&runnable);
	    if (runnable.error) {
		    throwIOException(env, "Failed to register for RFCOMMChannel Notifications");
		    return 0;
	    }
	}
	while ((stack != NULL) && (!comm->isClosed)) {
		MPEventFlags flags;
        OSStatus err = MPWaitForEvent(comm->incomingChannelNotificationEvent, &flags, kDurationMillisecond * 500);
		if ((err != kMPTimeoutErr) && (err != noErr)) {
			throwRuntimeException(env, "MPWaitForEvent");
			return 0;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
			debug("Interrupted while waiting for connections");
			return 0;
		}
	}
    return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfCloseServerConnection
  (JNIEnv *env, jobject, jlong handle) {
}