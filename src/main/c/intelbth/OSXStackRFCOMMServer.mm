/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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

    // cleanup
	IOBluetoothObjectRelease(serviceRecordRef);

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

    // cleanup
	IOBluetoothObjectRelease(serviceRecordRef);

    return kIOReturnSuccess;
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
    MPSetEvent(comm->incomingChannelNotificationEvent, 0);
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
		MPEventFlags flags;
        OSStatus err = MPWaitForEvent(comm->acceptedEvent, &flags, kDurationMillisecond * 500);
		if ((err != kMPTimeoutErr) && (err != noErr)) {
			throwRuntimeException(env, "MPWaitForEvent");
			return 0;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
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
		MPEventFlags flags;
        OSStatus err = MPWaitForEvent(comm->incomingChannelNotificationEvent, &flags, kDurationMillisecond * 500);
		if ((err != kMPTimeoutErr) && (err != noErr)) {
			throwRuntimeException(env, "MPWaitForEvent");
			error = true;
			break;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
			debug(("Interrupted while waiting for connections"));
			error = true;
			break;
		}
	}
	if ((stack != NULL) && (!comm->isClosed)) {
	    comm->acceptClientComm = NULL;
	    MPSetEvent(comm->acceptedEvent, 0);
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
        debug(("RFCOMM client connected"));
        return clientComm->internalHandle;;
    }
}
