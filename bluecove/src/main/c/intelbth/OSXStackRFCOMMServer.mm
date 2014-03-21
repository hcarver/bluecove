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

#import "OSXStackRFCOMMServer.h"

#define CPP_FILE "OSXStackRFCOMMServer.mm"

RFCOMMServerController* validRFCOMMServerControllerHandle(JNIEnv *env, jlong handle) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (RFCOMMServerController*)stack->commPool->getObject(env, handle, 'R');
}

RFCOMMServerController::RFCOMMServerController() {
    rfcommChannelID = 0;
    acceptClientComm = NULL;
}

RFCOMMServerController::~RFCOMMServerController() {
}

IOReturn RFCOMMServerController::publish() {
    // publish the service
	IOBluetoothSDPServiceRecordRef serviceRecordRef;
	IOReturn status = IOBluetoothAddServiceDict((CFDictionaryRef)sdpEntries, &serviceRecordRef);
    if (status != kIOReturnSuccess) {
        ndebug(("failed to IOBluetoothAddServiceDict"));
        return status;
    }

	IOBluetoothSDPServiceRecord *serviceRecord = [IOBluetoothSDPServiceRecord withSDPServiceRecordRef:serviceRecordRef];
	if (serviceRecord == NULL) {
	    ndebug(("failed to create IOBluetoothSDPServiceRecord"));
	} else {
	    // get service channel ID & service record handle
	    status = [serviceRecord getRFCOMMChannelID:&rfcommChannelID];
	    if (status != kIOReturnSuccess) {
		    ndebug(("failed to getRFCOMMChannelID"));
		} else {
		    [rfcommChannelIDDataElement setObject:[NSNumber numberWithInt:rfcommChannelID] forKey:kDataElementValue];
		    status = [serviceRecord getServiceRecordHandle:&sdpServiceRecordHandle];
	    }
	}

	return status;
}

IOReturn RFCOMMServerController::updateSDPServiceRecord() {
    IOReturn status;
    if (sdpServiceRecordHandle != 0) {
        status = IOBluetoothRemoveServiceWithRecordHandle(sdpServiceRecordHandle);
        sdpServiceRecordHandle = 0;
        if (status != kIOReturnSuccess) {
            return status;
        }
    }

    IOBluetoothSDPServiceRecordRef serviceRecordRef;
	status = IOBluetoothAddServiceDict((CFDictionaryRef)sdpEntries, &serviceRecordRef);
    if (status != kIOReturnSuccess) {
        ndebug(("failed to IOBluetoothAddServiceDict updated"));
        return status;
    }

	IOBluetoothSDPServiceRecord *serviceRecord = [IOBluetoothSDPServiceRecord withSDPServiceRecordRef:serviceRecordRef];
	if (serviceRecord == NULL) {
	    ndebug(("failed to create IOBluetoothSDPServiceRecord updated"));
	} else {
	    // get service channel ID & service record handle
	    BluetoothRFCOMMChannelID newRfcommChannelID;

	    status = [serviceRecord getRFCOMMChannelID:&newRfcommChannelID];
	    if (status != kIOReturnSuccess) {
		    ndebug(("failed to getRFCOMMChannelID updated"));
		} else {
		    if (newRfcommChannelID != rfcommChannelID) {
		        ndebug(("Changed RFCOMMChannelID %d -> %d", rfcommChannelID, newRfcommChannelID));
		        rfcommChannelID = newRfcommChannelID;
		    }
		    [rfcommChannelIDDataElement setObject:[NSNumber numberWithInt:rfcommChannelID] forKey:kDataElementValue];
		    status = [serviceRecord getServiceRecordHandle:&sdpServiceRecordHandle];
	    }
	}

    return kIOReturnSuccess;
}

void RFCOMMServerController::close() {
    isClosed = true;
    dispatch_semaphore_signal(incomingChannelNotificationEvent); // , 0);

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

/*
0x0001 ServiceClassIDList:  DATSEQ {
  UUID b10c0be1111111111111111111110001 (SERVICE UUID)
  UUID 0000110100001000800000805f9b34fb (SERIAL_PORT)
}
*/
    NSMutableArray *currentServiceList = [comm->sdpEntries objectForKey:kServiceItemKeyServiceClassIDList];
	if (currentServiceList == nil) {
		currentServiceList = [NSMutableArray array];
	}
	[currentServiceList addObject:[NSData dataWithBytes:uuidValue length:uuidValueLength]];

    if (!obexSrv) {
	    IOBluetoothSDPUUID* serial_port_uuid = [IOBluetoothSDPUUID uuid16:0x1101];
	    [currentServiceList addObject:serial_port_uuid];
    }

	// update dict
	[comm->sdpEntries setObject:currentServiceList forKey:kServiceItemKeyServiceClassIDList];

/*
0x0004 ProtocolDescriptorList:  DATSEQ {
  DATSEQ {
    UUID 0000010000001000800000805f9b34fb (L2CAP)
  }
  DATSEQ {
    UUID 0000000300001000800000805f9b34fb (RFCOMM)
    U_INT_1 0x1
  }
}
*/
    NSMutableArray *protocolDescriptorList = [NSMutableArray array];
    NSMutableArray *protocolDescriptorList1 = [NSMutableArray array];
    [protocolDescriptorList addObject:protocolDescriptorList1];

    IOBluetoothSDPUUID* l2cap_uuid = [IOBluetoothSDPUUID uuid16:0x0100];
    [protocolDescriptorList1 addObject:l2cap_uuid];

    NSMutableArray *protocolDescriptorList2 = [NSMutableArray array];
    [protocolDescriptorList addObject:protocolDescriptorList2];

    IOBluetoothSDPUUID* rfcomm_uuid = [IOBluetoothSDPUUID uuid16:0x0003];
    [protocolDescriptorList2 addObject:rfcomm_uuid];
    comm->rfcommChannelIDDataElement = createIntDataElement(1, 1, 1);
    [protocolDescriptorList2 addObject:(comm->rfcommChannelIDDataElement)];

    if (obexSrv) {
        NSMutableArray *protocolDescriptorList3 = [NSMutableArray array];
        [protocolDescriptorList addObject:protocolDescriptorList3];

        IOBluetoothSDPUUID* obex_uuid = [IOBluetoothSDPUUID uuid16:0x0008];
        [protocolDescriptorList3 addObject:obex_uuid];
    }

    [comm->sdpEntries setObject:protocolDescriptorList forKey:kServiceItemKeyProtocolDescriptorList];

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
	comm->authenticate = authenticate;
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
        throwIOException(env, "Failed to create RFCOMM service [0x%08x]", runnable.status);
        return 0;
    }
    debug(("RFCOMM server created, ChannelID %i", comm->rfcommChannelID));
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
    ndebug(("rfcommServiceOpenNotificationCallback"));
    RFCOMMServerController* comm = (RFCOMMServerController*)userRefCon;
    if (comm == NULL) {
        return;
    }
    if ((comm->magic1 != MAGIC_1) || (comm->magic2 != MAGIC_2)) {
		return;
	}
	IOBluetoothRFCOMMChannel *rfcommChannel = [IOBluetoothRFCOMMChannel withRFCOMMChannelRef:(IOBluetoothRFCOMMChannelRef)objectRef];
	if (rfcommChannel == NULL) {
	    ndebug(("fail to get IOBluetoothRFCOMMChannel"));
	    return;
	}
	if (comm->authenticate) {
	    IOBluetoothDevice* device = [rfcommChannel getDevice];
	    if (device == NULL) {
	        ndebug(("drop incomming connection unable to get device"));
	        [rfcommChannel closeChannel];
	        return;
	    }
	    IOReturn as = [device requestAuthentication];
	    if (as != kIOReturnSuccess) {
	        ndebug(("drop incomming connection unable to authenticate [0x%08x]", as));
	        [rfcommChannel closeChannel];
	        return;
	    }
	    ndebug(("RFCOMM incomming connection authenticated"));
	}
	RFCOMMChannelController* client = comm->acceptClientComm;
	if (client == NULL) {
	    ndebug(("drop incomming connection since AcceptAndOpen not running"));
	    [rfcommChannel closeChannel];
	    return;
	}
	client->openIncomingChannel(rfcommChannel);
	comm->openningClient = true;
    dispatch_semaphore_signal(comm->incomingChannelNotificationEvent); // , 0);
}

RUNNABLE(RFCOMMServiceRegisterForOpen, "RFCOMMServiceRegisterForOpen") {
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
	while ((stack != NULL) && (!comm->isClosed) && (comm->acceptClientComm != NULL)) {
        dispatch_semaphore_wait(comm->acceptedEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
		if (isCurrentThreadInterrupted(env, peer, "close")) {
			debug(("Interrupted while waiting for connections"));
			return 0;
		}
	}
	if (stack == NULL) {
	    throwIOException(env, cSTACK_CLOSED);
	    return 0;
	}
    if (comm->isClosed) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return 0;
    }
    if (comm->incomingChannelNotification == NULL) {
        RFCOMMServiceRegisterForOpen runnable;
	    runnable.pData[0] = comm;
        synchronousBTOperation(&runnable);
	    if (runnable.error) {
		    throwIOException(env, "Failed to register for RFCOMMChannel Notifications");
		    return 0;
	    }
	}

    debug(("create ChannelController to accept incoming connection"));
	RFCOMMChannelController* clientComm = new RFCOMMChannelController();
	if (!stack->commPool->addObject(clientComm, 'r')) {
		delete clientComm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}
    comm->acceptClientComm = clientComm;
    comm->openningClient = false;
    BOOL error = false;
	while ((stack != NULL) && (!comm->isClosed) && (comm->openningClient == false)) {
        dispatch_semaphore_wait(comm->incomingChannelNotificationEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
		if (isCurrentThreadInterrupted(env, peer, "accept")) {
			debug(("Interrupted while waiting for connections"));
			error = true;
			break;
		}
	}
	if ((stack != NULL) && (!comm->isClosed)) {
	    comm->acceptClientComm = NULL;
	    dispatch_semaphore_signal(comm->acceptedEvent); // , 0);
    }

	if ((error) || (stack == NULL) || (comm->isClosed) || (!comm->openningClient)) {
	    clientComm->readyToFree = TRUE;
	    if (!error) {
	        throwIOException(env, cCONNECTION_IS_CLOSED);
	    }
        return 0;
	} else {
	    int timeout = 120 * 1000;
	    if (!clientComm->waitForConnection(env, peer, false, timeout)) {
            RFCOMMChannelCloseExec(clientComm);
            return 0;
        }
        debug(("rfcomm (%i) client connected", clientComm->internalHandle));
        debug(("rfcomm MTU %i", clientComm->rfcommChannelMTU));
        return clientComm->internalHandle;
    }
}
