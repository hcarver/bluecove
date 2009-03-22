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

#import "OSXStackL2CAP.h"

#define CPP_FILE "OSXStackL2CAP.mm"

BOOL isValidObject(L2CAPChannelController* comm ) {
    if (comm == NULL) {
        return false;
    }
    if ((comm->magic1 != MAGIC_1) || (comm->magic2 != MAGIC_2)) {
		return false;
	}
	return comm->isValidObject();
}

@implementation L2CAPChannelDelegate

- (id)initWithController:(L2CAPChannelController*)controller {
    _controller = controller;
    return self;
}

- (void)close {
    _controller = NULL;
}

- (void)connectionComplete:(IOBluetoothDevice *)device status:(IOReturn)status {
    if (isValidObject(_controller)) {
        if (_controller->bluetoothDevice && [_controller->bluetoothDevice isEqual:device]) {
            _controller->connectionComplete(device, status);
        } else {
            ndebug(("ignore connectionComplete"));
        }
    }
}

- (void)l2capChannelOpenComplete:(IOBluetoothL2CAPChannel*)l2capChannel status:(IOReturn)error {
    if (isValidObject(_controller)) {
        if (_controller->l2capChannel == l2capChannel) {
            _controller->l2capChannelOpenComplete(error);
        }
    }
}

- (void)l2capChannelClosed:(IOBluetoothL2CAPChannel*)l2capChannel {
    if (isValidObject(_controller)) {
        if (_controller->l2capChannel == l2capChannel) {
            ndebug(("l2capChannelClosed"));
            _controller->l2capChannelClosed();
        }
    }
}

- (void)l2capChannelData:(IOBluetoothL2CAPChannel*)l2capChannel data:(void *)dataPointer length:(size_t)dataLength {
    if (isValidObject(_controller)) {
        if (_controller->l2capChannel == l2capChannel) {
            _controller->l2capChannelData(dataPointer, dataLength);
        }
    }
}

- (void)l2capChannelWriteComplete:(IOBluetoothL2CAPChannel*)l2capChannel refcon:(void*)refcon status:(IOReturn)error {
    if (isValidObject(_controller)) {
        if (_controller->l2capChannel == l2capChannel) {
            _controller->l2capChannelWriteComplete(refcon, error);
        }
    }
}

// Not used
- (void)l2capChannelQueueSpaceAvailable:(IOBluetoothL2CAPChannel*)l2capChannel {
}

- (void)l2capChannelReconfigured:(IOBluetoothL2CAPChannel*)l2capChannel {
}

@end

L2CAPChannelController::L2CAPChannelController() {
    delegate = NULL;
    l2capChannel = NULL;
}

L2CAPChannelController::~L2CAPChannelController() {
}

void L2CAPChannelController::initDelegate() {
    delegate = [[L2CAPChannelDelegate alloc] initWithController:this];
    [delegate retain];
}

id L2CAPChannelController::getDelegate() {
    return delegate;
}

void L2CAPChannelController::connectionComplete(IOBluetoothDevice *device, IOReturn status) {
    ndebug(("connectionComplete"));
    if (status == kIOReturnSuccess) {
        isBasebandConnected = true;
    } else {
        openStatus = status;
    }
    MPSetEvent(notificationEvent, 1);
}

void L2CAPChannelController::l2capChannelOpenComplete(IOReturn error) {
    if (error == kIOReturnSuccess) {
        isConnected = true;
        int incomingMTU = [l2capChannel getIncomingMTU];
        if (receiveMTU > incomingMTU) {
            receiveMTU = incomingMTU;
        }

        int remoteMtu = [l2capChannel getOutgoingMTU];
	    if (transmitMTU == -1) {
		    transmitMTU = remoteMtu;
	    } else if (transmitMTU < remoteMtu) {
		    //transmitMTU = transmitMTU;
	    } else {
		    transmitMTU = remoteMtu;
	    }
	    //[l2capChannel requestRemoteMTU:transmitMTU];
    } else {
        openStatus = error;
    }
    MPSetEvent(notificationEvent, 1);
}

void L2CAPChannelController::openIncomingChannel(IOBluetoothL2CAPChannel* newL2CAPChannel) {
    initDelegate();
    isConnected = false;
    isClosed = false;
    l2capChannel = newL2CAPChannel;
    [l2capChannel retain];
    openStatus = [l2capChannel setDelegate:delegate];
    bluetoothDevice = [l2capChannel getDevice];
}

void L2CAPChannelController::l2capChannelClosed() {
    isClosed = true;
    isConnected = false;
    MPSetEvent(notificationEvent, 0);
    MPSetEvent(writeCompleteNotificationEvent, 0);
}

void L2CAPChannelController::l2capChannelData(void* dataPointer, size_t dataLength) {
    if (isConnected && !isClosed) {
	    receiveBuffer.write_with_len(dataPointer, dataLength);
		MPSetEvent(notificationEvent, 1);
    }
}

void L2CAPChannelController::l2capChannelWriteComplete(void* refcon, IOReturn error) {
    if (refcon != NULL) {
        ((L2CAPConnectionWrite*)refcon)->l2capChannelWriteComplete(error);
    }
    MPSetEvent(writeCompleteNotificationEvent, 1);
}

IOReturn L2CAPChannelController::close() {
    ndebug(("L2CAPChannelController::close"));
    IOReturn rc = kIOReturnSuccess;
    if (delegate != NULL) {
        [delegate close];
    }
    if (l2capChannel != NULL) {
        isClosed = true;
        MPSetEvent(notificationEvent, 0);
        MPSetEvent(writeCompleteNotificationEvent, 0);

        IOBluetoothDevice *device = [l2capChannel getDevice];
        [l2capChannel setDelegate:NULL];
        rc = [l2capChannel closeChannel];
        if (device != NULL) {
            [device closeConnection];
        }
        [l2capChannel release];
        l2capChannel = NULL;
    }
    if (delegate != NULL) {
        [delegate release];
        delegate = NULL;
    }
    return rc;
}

L2CAPChannelController* validL2CAPChannelHandle(JNIEnv *env, jlong handle) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (L2CAPChannelController*)stack->commPool->getObject(env, handle, 'l');
}

L2CAPChannelController*  validOpenL2CAPChannelHandle(JNIEnv *env, jlong handle) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
    if (comm == NULL) {
		return NULL;
	}
	if (!comm->isConnected) {
		throwIOException(env, cCONNECTION_CLOSED);
		return NULL;
	}
	if (comm->isClosed) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return NULL;
	}
	return comm;
}

L2CAPConnectionOpen::L2CAPConnectionOpen() {
    name = "L2CAPConnectionOpen";
}

void L2CAPConnectionOpen::run() {
    BluetoothL2CAPPSM psm = this->channel;

    status = [comm->bluetoothDevice openL2CAPChannelAsync:&(comm->l2capChannel) withPSM:psm  delegate:comm->delegate];
    if ((status != kIOReturnSuccess) || (comm->l2capChannel == NULL)) {
        error = 1;
        return;
    }

    status = [comm->l2capChannel setDelegate:comm->delegate];
    if (status != kIOReturnSuccess) {
        error = 1;
        return;
    }
    [comm->l2capChannel retain];
}

RUNNABLE(L2CAPRegisterDataListener, "L2CAPRegisterDataListener") {
    L2CAPChannelController* comm = (L2CAPChannelController*)pData[0];
    IOReturn status = [comm->l2capChannel setDelegate:comm->delegate];
    if (status != kIOReturnSuccess) {
        error = status;
        return;
    }
}

RUNNABLE(L2CAPChannelClose, "L2CAPChannelClose") {
    L2CAPChannelController* comm = (L2CAPChannelController*)pData[0];
    iData = comm->close();
}

long L2CAPChannelCloseExec(L2CAPChannelController* comm) {
    L2CAPChannelClose runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	comm->readyToFree = TRUE;
	return runnable.lData;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2OpenClientConnectionImpl
  (JNIEnv *env, jobject peer, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint receiveMTU, jint transmitMTU, jint timeout) {
    L2CAPChannelController* comm = new L2CAPChannelController();
	if (!stack->commPool->addObject(comm, 'l')) {
		delete comm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}
	debug(("l2cap OpenClientConnection"));

    BasebandConnectionOpen basebandOpen;
	basebandOpen.comm = comm;
    basebandOpen.address = address;
    basebandOpen.authenticate = authenticate;
    basebandOpen.encrypt = encrypt;
    basebandOpen.timeout = timeout;
    synchronousBTOperation(&basebandOpen);

    if (basebandOpen.error != 0) {
        L2CAPChannelCloseExec(comm);
        throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open baseband connection [0x%08x]", basebandOpen.status);
        return 0;
    }

    if (!comm->waitForConnection(env, peer, true, timeout)) {
        L2CAPChannelCloseExec(comm);
        return 0;
    }

	L2CAPConnectionOpen runnable;
	runnable.comm = comm;
    runnable.address = address;
    runnable.channel = channel;
    runnable.authenticate = authenticate;
    runnable.encrypt = encrypt;
    runnable.timeout = timeout;
    comm->receiveMTU = receiveMTU;
    comm->transmitMTU = transmitMTU;
    synchronousBTOperation(&runnable);

    if (runnable.error != 0) {
        L2CAPChannelCloseExec(comm);
        throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection(1) [0x%08x]", runnable.status);
        return 0;
    }

    if (!comm->waitForConnection(env, peer, false, timeout)) {
        L2CAPChannelCloseExec(comm);
        return 0;
    }

    L2CAPRegisterDataListener reg;
    reg.pData[0] = comm;
    synchronousBTOperation(&reg);
    if (reg.error != 0) {
        L2CAPChannelCloseExec(comm);
        throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection(4) [0x%08x]", reg.error);
        return 0;
    }

    debug(("l2cap connected"));
	return comm->internalHandle;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2CloseClientConnection
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return;
	}
	debug(("l2CloseClientConnection"));
	long rc = L2CAPChannelCloseExec(comm);
	if (rc != kIOReturnSuccess) {
	    throwIOException(env, "Failed to close L@CAP channel [0x%08x]", rc);
	}
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Ready
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return JNI_FALSE;
	}
	if (comm->receiveBuffer.available() > comm->receiveBuffer.sizeof_len()) {
		return JNI_TRUE;
	}
	if (!comm->isConnected) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return JNI_FALSE;
	}
	return JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Receive
  (JNIEnv *env, jobject peer, jlong handle, jbyteArray inBuf) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return 0;
	}
	if ((!comm->isConnected ) && (comm->receiveBuffer.available() < comm->receiveBuffer.sizeof_len())) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return 0;
	}
	if (comm->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
		return 0;
	}

	int paketLengthSize = comm->receiveBuffer.sizeof_len();

	while ((stack != NULL) && comm->isConnected  && (!comm->isClosed) && (comm->receiveBuffer.available() <= paketLengthSize)) {
		Edebug(("receive[] waits for data"));
		MPEventFlags flags;
        OSStatus err = MPWaitForEvent(comm->notificationEvent, &flags, kDurationMillisecond * 500);
		if ((err != kMPTimeoutErr) && (err != noErr)) {
			throwRuntimeException(env, "MPWaitForEvent");
			return 0;
		}
		if (isCurrentThreadInterrupted(env, peer, "receive")) {
			return 0;
		}
	}
	if ((stack == NULL) || ((!comm->isConnected) && (comm->receiveBuffer.available() <= paketLengthSize)) ) {
		throwIOException(env, cCONNECTION_CLOSED);
		return 0;
	}

	int count = comm->receiveBuffer.available();
	if (count < paketLengthSize) {
	    if (comm->isClosed) {
	        throwIOException(env, cCONNECTION_CLOSED);
	    } else {
		    throwIOException(env, "Receive buffer corrupted (1)");
		}
		return 0;
	}
	int paketLength = 0;
	int done = comm->receiveBuffer.read_len(&paketLength);
	if ((done != paketLengthSize) || (paketLength > (count - paketLengthSize))) {
		throwIOException(env, "Receive buffer corrupted (2)");
		return 0;
	}
	if (paketLength == 0) {
		return 0;
	}

	jbyte *bytes = env->GetByteArrayElements(inBuf, 0);
	size_t inBufLen = (size_t)env->GetArrayLength(inBuf);

	int readLen = paketLength;
	if (readLen > inBufLen) {
		readLen = inBufLen;
	}
	if (readLen > comm->receiveMTU) {
		readLen = comm->receiveMTU;
	}

	done = comm->receiveBuffer.read(bytes, readLen);
	if (done != readLen) {
		throwIOException(env, "Receive buffer corrupted (3)");
	}
	if (done < paketLength) {
		// the rest will be discarded.
		int skip = paketLength - done;
		if (skip != comm->receiveBuffer.skip(skip)) {
			throwIOException(env, "Receive buffer corrupted (4)");
		}
	}

	env->ReleaseByteArrayElements(inBuf, bytes, 0);
	debug(("receive[] returns %i", done));
	return done;
}

L2CAPConnectionWrite::L2CAPConnectionWrite() {
    name = "L2CAPConnectionWrite";
    writeComplete = false;
    ioerror = kIOReturnSuccess;
}

void L2CAPConnectionWrite::l2capChannelWriteComplete(IOReturn status) {
    ioerror = status;
    if (ioerror != kIOReturnSuccess) {
        error = 1;
    }
    writeComplete = true;
}

void L2CAPConnectionWrite::run() {
    void* notify = this;
    ioerror = [comm->l2capChannel writeAsync:data length:length refcon:notify];
    if (ioerror != kIOReturnSuccess) {
        error = 1;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Send
  (JNIEnv *env, jobject peer, jlong handle, jbyteArray data, jint transmitMTU) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return;
	}
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	int len = (int)env->GetArrayLength(data);
    if (len > comm->transmitMTU) {
		len = comm->transmitMTU;
	}
	if (len > transmitMTU) {
		len = transmitMTU;
	}

	L2CAPConnectionWrite runnable;
    runnable.comm = comm;
    runnable.data = (void*)(bytes);
    runnable.length = len;

    synchronousBTOperation(&runnable);
    if (runnable.error != 0) {
        throwIOException(env, "Failed to write [0x%08x]", runnable.ioerror);
    } else {
        while ((stack != NULL) &&( comm->isConnected) && (!comm->isClosed)) {
            // Already finished
            if (runnable.writeComplete) {
                break;
            }
            MPEventFlags flags;
            OSStatus err = MPWaitForEvent(comm->writeCompleteNotificationEvent, &flags, kDurationMillisecond * 500);
            if (err == kMPTimeoutErr) {
                continue;
            }
		    if ((err != kMPTimeoutErr) && (err != noErr)) {
			    throwRuntimeException(env, "MPWaitForEvent");
			    break;
		    }
            if (isCurrentThreadInterrupted(env, peer, "send")) {
			    break;
		    }
		    break;
		}
        if (runnable.error != 0) {
            throwIOException(env, "Failed to write [0x%08x]", runnable.ioerror);
        }
    }
	env->ReleaseByteArrayElements(data, bytes, 0);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2GetReceiveMTU
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return 0;
}
	return comm->receiveMTU;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2GetTransmitMTU
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return 0;
	}
    return comm->transmitMTU;
}

RUNNABLE(L2CAPChannelRemoteAddress, "L2CAPChannelRemoteAddress") {
    L2CAPChannelController* comm = (L2CAPChannelController*)pData[0];
    if (comm->l2capChannel == NULL) {
        error = 1;
        return;
    }
    IOBluetoothDevice* device = [comm->l2capChannel getDevice];
    if (device == NULL) {
        error = 1;
        return;
    }
    comm->address = OSxAddrToLong([device getAddress]);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2RemoteAddress
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return 0;
	}
	L2CAPChannelRemoteAddress runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	if (runnable.error) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return 0;
	}
    return comm->address;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2GetSecurityOpt
  (JNIEnv *env, jobject peer, jlong handle, jint expected) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}

	BasebandConnectionGetOptions runnable;
	runnable.comm = comm;
    synchronousBTOperation(&runnable);
	if (runnable.error) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return 0;
	}

	if (runnable.encrypted) {
	    return AUTHENTICATE_ENCRYPT;
	} else if (NOAUTHENTICATE_NOENCRYPT == expected) {
	    return NOAUTHENTICATE_NOENCRYPT;
	} else {
	    return AUTHENTICATE_NOENCRYPT;
	}
}