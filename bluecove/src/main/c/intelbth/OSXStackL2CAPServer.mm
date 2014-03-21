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

#import "OSXStackL2CAPServer.h"

#define CPP_FILE "OSXStackL2CAPServer.mm"

#define DEFAULT_PSM 0x1001

L2CAPServerController* validL2CAPServerControllerHandle(JNIEnv *env, jlong handle) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (L2CAPServerController*)stack->commPool->getObject(env, handle, 'L');
}

L2CAPServerController::L2CAPServerController() {
    l2capPSM = 0;
    acceptClientComm = NULL;
    l2capPSMDataElement = NULL;
}

L2CAPServerController::~L2CAPServerController() {
}

IOReturn L2CAPServerController::publish() {
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
	    status = [serviceRecord getL2CAPPSM:&l2capPSM];
	    if (status != kIOReturnSuccess) {
		    ndebug(("failed to getL2CAPPSM [0x%08x]", status));
		} else {
		    createPSMDataElement();
		    [l2capPSMDataElement setObject:[NSNumber numberWithInt:l2capPSM] forKey:kDataElementValue];
		    status = [serviceRecord getServiceRecordHandle:&sdpServiceRecordHandle];
	    }
	}

    // cleanup
	//IOBluetoothObjectRelease(serviceRecordRef);

	return status;
}

IOReturn L2CAPServerController::updateSDPServiceRecord() {
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
	    BluetoothL2CAPPSM newL2capPSM;

	    status = [serviceRecord getL2CAPPSM:&newL2capPSM];
	    if (status != kIOReturnSuccess) {
		    ndebug(("failed to getL2CAPPSM updated [0x%08x]", status));
		} else {
		    if (newL2capPSM != l2capPSM) {
		        ndebug(("Changed L2CAP PSM %d -> %d", l2capPSM, newL2capPSM));
		        l2capPSM = newL2capPSM;
		    }
		    createPSMDataElement();
		    [l2capPSMDataElement setObject:[NSNumber numberWithInt:l2capPSM] forKey:kDataElementValue];
		    status = [serviceRecord getServiceRecordHandle:&sdpServiceRecordHandle];
	    }
	}

    // cleanup
	//IOBluetoothObjectRelease(serviceRecordRef);

    return kIOReturnSuccess;
}

void L2CAPServerController::close() {
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

void L2CAPServerController::createPSMDataElement() {
    if (l2capPSMDataElement != NULL) {
        return;
    }

    NSMutableArray *protocolDescriptorList = [sdpEntries objectForKey:kServiceItemKeyProtocolDescriptorList];
	if (protocolDescriptorList == nil) {
	    ndebug(("create protocolDescriptorList"));
		protocolDescriptorList = [NSMutableArray array];
		[sdpEntries setObject:protocolDescriptorList forKey:kServiceItemKeyProtocolDescriptorList];
	}

	NSMutableArray *protocolDescriptorList1 = [protocolDescriptorList objectAtIndex:0];
	if (protocolDescriptorList1 == nil) {
	    ndebug(("create protocolDescriptorList1"));
	    protocolDescriptorList1 = [NSMutableArray array];

	    IOBluetoothSDPUUID* l2cap_uuid = [IOBluetoothSDPUUID uuid16:0x0100];
        [protocolDescriptorList1 addObject:l2cap_uuid];

        [protocolDescriptorList addObject:protocolDescriptorList1];
    }

    //0x1001-0xFFFF dynamically assigned
    int psm = DEFAULT_PSM;

    l2capPSMDataElement =  [protocolDescriptorList1 objectAtIndex:1];
    if (l2capPSMDataElement == nil) {
        ndebug(("create l2capPSMDataElement"));
        l2capPSMDataElement = createIntDataElement(2, 1, psm);
        [protocolDescriptorList1 addObject:l2capPSMDataElement];
    }
}

L2CAPServicePublish::L2CAPServicePublish() {
    name = "L2CAPServicePublish";
}

void L2CAPServicePublish::run() {
    comm->init();

    NSString* srvName = [NSString stringWithCharacters:(UniChar*)serviceName length:serviceNameLength];
    [comm->sdpEntries setObject:srvName forKey:kServiceItemKeyServiceName];

/*
0x0001 ServiceClassIDList:  DATSEQ {
  UUID b10c0be1111111111111111111110002 (BlueCoveT L2CAP long)
}
*/
    NSMutableArray *currentServiceList = [comm->sdpEntries objectForKey:kServiceItemKeyServiceClassIDList];
	if (currentServiceList == nil) {
		currentServiceList = [NSMutableArray array];
	}
	[currentServiceList addObject:[NSData dataWithBytes:uuidValue length:uuidValueLength]];

	// update dict
	[comm->sdpEntries setObject:currentServiceList forKey:kServiceItemKeyServiceClassIDList];

/*
0x0004 ProtocolDescriptorList:  DATSEQ {
  DATSEQ {
    UUID 0000010000001000800000805f9b34fb (L2CAP)
    U_INT_2 0x1001
  }
}
*/
    bool createProtocolDescriptorList = true;
    if (createProtocolDescriptorList) {
        NSMutableArray *protocolDescriptorList = [NSMutableArray array];
        NSMutableArray *protocolDescriptorList1 = [NSMutableArray array];
        [protocolDescriptorList addObject:protocolDescriptorList1];

        IOBluetoothSDPUUID* l2cap_uuid = [IOBluetoothSDPUUID uuid16:0x0100];
        [protocolDescriptorList1 addObject:l2cap_uuid];

        //0x1001-0xFFFF dynamically assigned
        int psm = DEFAULT_PSM;
        if (assignPsm != 0) {
            psm = assignPsm;
        }

        comm->l2capPSMDataElement = createIntDataElement(2, 1, psm);
        [protocolDescriptorList1 addObject:comm->l2capPSMDataElement];

        [comm->sdpEntries setObject:protocolDescriptorList forKey:kServiceItemKeyProtocolDescriptorList];
    } else {
        if (assignPsm != 0) {
            comm->createPSMDataElement();
            [comm->l2capPSMDataElement setObject:[NSNumber numberWithInt:assignPsm] forKey:kDataElementValue];
        }
    }

   	// publish the service
	status = comm->publish();

	if (status != kIOReturnSuccess) {
	    error = 1;
	}
}

RUNNABLE(L2CAPServiceClose, "L2CAPServiceClose") {
    L2CAPServerController* comm = (L2CAPServerController*)pData[0];
    comm->close();
}

void L2CAPServiceCloseExec(L2CAPServerController* comm) {
    L2CAPServiceClose runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	comm->readyToFree = TRUE;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2ServerOpenImpl
  (JNIEnv *env, jobject, jbyteArray uuidValue, jboolean authenticate, jboolean encrypt, jstring name, jint receiveMTU, jint transmitMTU, jint assignPsm) {
    L2CAPServerController* comm = new L2CAPServerController();
	if (!stack->commPool->addObject(comm, 'L')) {
		delete comm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}
	comm->authenticate = authenticate;
	L2CAPServicePublish runnable;
	runnable.comm = comm;
	runnable.uuidValue = env->GetByteArrayElements(uuidValue, 0);
	runnable.uuidValueLength = env->GetArrayLength(uuidValue);
    runnable.serviceName = env->GetStringChars(name, 0);
    runnable.serviceNameLength = env->GetStringLength(name);
    runnable.authenticate = authenticate;
    runnable.encrypt = encrypt;

    comm->receiveMTU = receiveMTU;
    comm->transmitMTU = transmitMTU;

    runnable.assignPsm = assignPsm;

    synchronousBTOperation(&runnable);

    env->ReleaseByteArrayElements(uuidValue, runnable.uuidValue, 0);
    env->ReleaseStringChars(name, runnable.serviceName);

    if (runnable.error != 0) {
        L2CAPServiceCloseExec(comm);
        throwIOException(env, "Failed to create L2CAP service [0x%08x]", runnable.status);
        return 0;
    }
    debug(("L2CAP server created, PSM %x", comm->l2capPSM));
    return comm->internalHandle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2ServerPSM
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPServerController* comm = validL2CAPServerControllerHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
    return comm->l2capPSM;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2ServerCloseImpl
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPServerController* comm = validL2CAPServerControllerHandle(env, handle);
    if (comm == NULL) {
		return;
	}
	L2CAPServiceCloseExec(comm);
}

void l2capServiceOpenNotificationCallback(void *userRefCon, IOBluetoothUserNotificationRef inRef, IOBluetoothObjectRef objectRef ) {
    ndebug(("l2capServiceOpenNotificationCallback"));
    L2CAPServerController* comm = (L2CAPServerController*)userRefCon;
    if (comm == NULL) {
        return;
    }
    if ((comm->magic1 != MAGIC_1) || (comm->magic2 != MAGIC_2)) {
		return;
	}
	IOBluetoothL2CAPChannel *l2capChannel = [IOBluetoothL2CAPChannel withL2CAPChannelRef:(IOBluetoothL2CAPChannelRef)objectRef];
	if (l2capChannel == NULL) {
	    ndebug(("fail to get IOBluetoothL2CAPChannel"));
	    return;
	}
	if (comm->authenticate) {
	    IOBluetoothDevice* device = l2capChannel.device;
	    if (device == NULL) {
	        ndebug(("drop incomming connection unable to get device"));
	        [l2capChannel closeChannel];
	        return;
	    }
	    IOReturn as = [device requestAuthentication];
	    if (as != kIOReturnSuccess) {
	        ndebug(("drop incomming connection unable to authenticate [0x%08x]", as));
	        [l2capChannel closeChannel];
	        return;
	    }
	    ndebug(("L2CAP incomming connection authenticated"));
	}
	L2CAPChannelController* client = comm->acceptClientComm;
	if (client == NULL) {
	    ndebug(("drop incomming connection since AcceptAndOpen not running"));
	    return;
	}
	client->openIncomingChannel(l2capChannel);
	comm->openningClient = true;
    dispatch_semaphore_signal(comm->incomingChannelNotificationEvent);
}

RUNNABLE(L2CAPServiceRegisterForOpen, "L2CAPServiceRegisterForOpen") {
    L2CAPServerController* comm = (L2CAPServerController*)pData[0];
    comm->incomingChannelNotification = IOBluetoothRegisterForFilteredL2CAPChannelOpenNotifications(l2capServiceOpenNotificationCallback, comm, comm->l2capPSM, kIOBluetoothUserNotificationChannelDirectionIncoming);
    if (comm->incomingChannelNotification == NULL) {
        error = 1;
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2ServerAcceptAndOpenServerConnection
  (JNIEnv *env, jobject peer, jlong handle) {
    L2CAPServerController* comm = validL2CAPServerControllerHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
	// Avoid more than once Accept at the same time
	while ((stack != NULL) && (!comm->isClosed) && (comm->acceptClientComm != NULL)) {
        dispatch_semaphore_wait(comm->acceptedEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
		if (isCurrentThreadInterrupted(env, peer, "close")) {
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
        L2CAPServiceRegisterForOpen runnable;
	    runnable.pData[0] = comm;
        synchronousBTOperation(&runnable);
        if (runnable.error) {
		    throwIOException(env, "Failed to register for L2CAPChannel Notifications");
		    return 0;
	    }
	}

    debug(("create ChannelController to accept incoming connection"));
	L2CAPChannelController* clientComm = new L2CAPChannelController();
	if (!stack->commPool->addObject(clientComm, 'l')) {
		delete clientComm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}
	clientComm->receiveMTU = comm->receiveMTU;
    clientComm->transmitMTU = comm->transmitMTU;
    comm->acceptClientComm = clientComm;
    comm->openningClient = false;
    BOOL error = false;
	while ((stack != NULL) && (!comm->isClosed) && (comm->openningClient == false)) {
        dispatch_semaphore_wait(comm->incomingChannelNotificationEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
		if (isCurrentThreadInterrupted(env, peer, "accept")) {
			error = true;
			break;
		}
	}
	if ((stack != NULL) && (!comm->isClosed)) {
	    comm->acceptClientComm = NULL;
        dispatch_semaphore_signal(comm->acceptedEvent);
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
            L2CAPChannelCloseExec(clientComm);
            return 0;
        }
        debug(("L2CAP client connected"));
        return clientComm->internalHandle;;
    }
}
