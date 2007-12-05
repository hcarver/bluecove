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

- (void)l2capChannelOpenComplete:(IOBluetoothL2CAPChannel*)l2capChannel status:(IOReturn)error {
    if (isValidObject(_controller)) {
        _controller->l2capChannelOpenComplete(error);
    }
}

- (void)l2capChannelClosed:(IOBluetoothL2CAPChannel*)l2capChannel {
    if (isValidObject(_controller)) {
        _controller->l2capChannelClosed();
    }
}

- (void)l2capChannelData:(IOBluetoothL2CAPChannel*)l2capChannel data:(void *)dataPointer length:(size_t)dataLength {
    if (isValidObject(_controller)) {
        _controller->l2capChannelData(dataPointer, dataLength);
    }
}

- (void)l2capChannelWriteComplete:(IOBluetoothL2CAPChannel*)l2capChannel refcon:(void*)refcon status:(IOReturn)error {
    if (isValidObject(_controller)) {
        _controller->l2capChannelWriteComplete(refcon, error);
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

void L2CAPChannelController::l2capChannelOpenComplete(IOReturn error) {
    if (error == kIOReturnSuccess) {
        isConnected = true;
        receiveMTU = [l2capChannel getIncomingMTU];
        transmitMTU = [l2capChannel getOutgoingMTU];
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
}

void L2CAPChannelController::l2capChannelClosed() {
    isClosed = true;
    isConnected = false;
    MPSetEvent(notificationEvent, 0);
    MPSetEvent(writeCompleteNotificationEvent, 0);
}

void L2CAPChannelController::l2capChannelData(void* dataPointer, size_t dataLength) {
    if (isConnected && !isClosed) {
        receiveBuffer.write(&dataLength, sizeof(size_t));
	    receiveBuffer.write(dataPointer, dataLength);
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
	if (!comm->isConnected || comm->isClosed) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return NULL;
	}
	return comm;
}

L2CAPConnectionOpen::L2CAPConnectionOpen() {
    name = "L2CAPConnectionOpen";
}

void L2CAPConnectionOpen::run() {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(this->address, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (deviceRef == NULL) {
        error = 1;
        return;
    }
    comm->address = this->address;
    comm->isClosed = false;

    BluetoothL2CAPPSM psm = this->channel;
    comm->initDelegate();
    IOBluetoothDevice* dev = [IOBluetoothDevice withDeviceRef:deviceRef];
    if (dev == NULL) {
        error = 1;
        return;
    }

    status = [dev openConnection];
    if (status != kIOReturnSuccess) {
        error = 1;
        return;
    }

    status = [dev openL2CAPChannelAsync:&(comm->l2capChannel) withPSM:psm  delegate:comm->delegate];
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

	L2CAPConnectionOpen runnable;
	runnable.comm = comm;
    runnable.address = address;
    runnable.channel = channel;
    runnable.authenticate = authenticate;
    runnable.encrypt = encrypt;
    runnable.timeout = timeout;
    runnable.receiveMTU = receiveMTU;
    runnable.transmitMTU = transmitMTU;
    synchronousBTOperation(&runnable);

    if (runnable.error != 0) {
        L2CAPChannelCloseExec(comm);
        throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection(1) [0x%08x]", runnable.status);
        return 0;
    }

    if (!comm->waitForConnection(env, peer, timeout)) {
        L2CAPChannelCloseExec(comm);
        return 0;
    }
    debug("l2cap connected");
	return comm->internalHandle;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2CloseClientConnection
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return;
	}
	long rc = L2CAPChannelCloseExec(comm);
	if (rc != kIOReturnSuccess) {
	    throwIOExceptionExt(env, "Failed to close L@CAP channel [0x%08x]", rc);
	}
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Ready
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return JNI_FALSE;
	}
	if (comm->receiveBuffer.available() > sizeof(size_t)) {
		return JNI_TRUE;
	}
	if (!comm->isConnected) {
		_throwIOException(env, cCONNECTION_IS_CLOSED);
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
	if ((!comm->isConnected ) && (comm->receiveBuffer.available() < sizeof(size_t))) {
		_throwIOException(env, cCONNECTION_IS_CLOSED);
		return 0;
	}
	if (comm->receiveBuffer.isOverflown()) {
		_throwIOException(env, "Receive buffer overflown");
		return 0;
	}

	int paketLengthSize = sizeof(size_t);

	while ((stack != NULL) && comm->isConnected  && (comm->receiveBuffer.available() <= paketLengthSize)) {
		Edebug("receive[] waits for data");
		MPEventFlags flags;
        OSStatus err = MPWaitForEvent(comm->notificationEvent, &flags, kDurationMillisecond * 500);
		if ((err != kMPTimeoutErr) && (err != noErr)) {
			throwRuntimeException(env, "MPWaitForEvent");
			return 0;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
			debug("Interrupted while reading");
			return 0;
		}
	}
	if ((stack == NULL) || ((!comm->isConnected) && (comm->receiveBuffer.available() <= paketLengthSize)) ) {
		_throwIOException(env, cCONNECTION_CLOSED);
		return 0;
	}

	int count = comm->receiveBuffer.available();
	if (count < paketLengthSize) {
		_throwIOException(env, "Receive buffer corrupted (1)");
		return 0;
	}
	size_t paketLength = 0;
	int done = comm->receiveBuffer.read(&paketLength, paketLengthSize);
	if ((done != paketLengthSize) || (paketLength > (count - paketLengthSize))) {
		_throwIOException(env, "Receive buffer corrupted (2)");
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
		_throwIOException(env, "Receive buffer corrupted (3)");
	}
	if (done < paketLength) {
		// the rest will be discarded.
		int skip = paketLength - done;
		if (skip != comm->receiveBuffer.skip(skip)) {
			_throwIOException(env, "Receive buffer corrupted (4)");
		}
	}

	env->ReleaseByteArrayElements(inBuf, bytes, 0);
	debug1("receive[] returns %i", done);
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
  (JNIEnv *env, jobject peer, jlong handle, jbyteArray data) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return;
	}
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	int len = (int)env->GetArrayLength(data);
    if (len > comm->transmitMTU) {
		len = comm->transmitMTU;
	}

	L2CAPConnectionWrite runnable;
    runnable.comm = comm;
    runnable.data = (void*)(bytes);
    runnable.length = len;

    synchronousBTOperation(&runnable);
    if (runnable.error != 0) {
        throwIOExceptionExt(env, "Failed to write [0x%08x]", runnable.ioerror);
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
            if (isCurrentThreadInterrupted(env, peer)) {
			    debug("Interrupted while writing");
			    break;
		    }
		    break;
		}
        if (runnable.error != 0) {
            throwIOExceptionExt(env, "Failed to write [0x%08x]", runnable.ioerror);
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