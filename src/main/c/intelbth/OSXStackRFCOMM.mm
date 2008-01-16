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

#import "OSXStackRFCOMM.h"

#define CPP_FILE "OSXStackRFCOMM.mm"

BOOL isValidObject(RFCOMMChannelController* comm ) {
    if (comm == NULL) {
        return false;
    }
    if ((comm->magic1 != MAGIC_1) || (comm->magic2 != MAGIC_2)) {
		return false;
	}
	return comm->isValidObject();
}

@implementation RFCOMMChannelDelegate

- (id)initWithController:(RFCOMMChannelController*)controller {
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
            ndebug("ignore connectionComplete");
        }
    }
}

- (void)rfcommChannelOpenComplete:(IOBluetoothRFCOMMChannel*)rfcommChannel status:(IOReturn)error {
    if (isValidObject(_controller)) {
        if (_controller->rfcommChannel == rfcommChannel) {
            _controller->rfcommChannelOpenComplete(error);
        }
    }
}

- (void)rfcommChannelData:(IOBluetoothRFCOMMChannel *)rfcommChannel
                     data:(void *)dataPointer
                   length:(size_t)dataLength {
    if (isValidObject(_controller)) {
        if (_controller->rfcommChannel == rfcommChannel) {
            _controller->rfcommChannelData(dataPointer, dataLength);
        }
    }
}

- (void)rfcommChannelWriteComplete:(IOBluetoothRFCOMMChannel*)rfcommChannel
                            refcon:(void*)refcon
                            status:(IOReturn)error {
    if (isValidObject(_controller)) {
        if (_controller->rfcommChannel == rfcommChannel) {
            _controller->rfcommChannelWriteComplete(refcon, error);
        }
    }
}

- (void)rfcommChannelClosed:(IOBluetoothRFCOMMChannel *)rfcommChannel {
    ndebug("rfcommChannelClosed->");
    if (isValidObject(_controller)) {
        if (_controller->rfcommChannel == rfcommChannel) {
            ndebug("rfcommChannelClosed");
            _controller->rfcommChannelClosed();
        }
    }
}

// Not used
- (void)rfcommChannelControlSignalsChanged:(IOBluetoothRFCOMMChannel*)rfcommChannel {
}

- (void)rfcommChannelFlowControlChanged:(IOBluetoothRFCOMMChannel*)rfcommChannel {
}

- (void)rfcommChannelQueueSpaceAvailable:(IOBluetoothRFCOMMChannel*)rfcommChannel {
}


@end

RFCOMMChannelController::RFCOMMChannelController() {
    delegate = NULL;
}

RFCOMMChannelController::~RFCOMMChannelController() {
    ndebug("~RFCOMMChannelController");
}

void RFCOMMChannelController::initDelegate() {
    delegate = [[RFCOMMChannelDelegate alloc] initWithController:this];
    [delegate retain];
}

id RFCOMMChannelController::getDelegate() {
    return delegate;
}

void RFCOMMChannelController::connectionComplete(IOBluetoothDevice *device, IOReturn status) {
    ndebug("connectionComplete");
    if (status == kIOReturnSuccess) {
        isBasebandConnected = true;
    } else {
        openStatus = status;
    }
    MPSetEvent(notificationEvent, 1);
}

void RFCOMMChannelController::rfcommChannelOpenComplete(IOReturn error) {
    ndebug("rfcommChannelOpenComplete");
    if (error == kIOReturnSuccess) {
        isConnected = true;
        rfcommChannelMTU = [rfcommChannel getMTU];
    } else {
        openStatus = error;
    }
    MPSetEvent(notificationEvent, 1);
}

void RFCOMMChannelController::openIncomingChannel(IOBluetoothRFCOMMChannel* newRfcommChannel) {
    initDelegate();
    isConnected = false;
    isClosed = false;
    rfcommChannel = newRfcommChannel;
    [rfcommChannel retain];
    openStatus = [rfcommChannel setDelegate:delegate];
}

void RFCOMMChannelController::rfcommChannelClosed() {
    ndebug("rfcommChannelClosed");
    isClosed = true;
    MPSetEvent(notificationEvent, 0);
    MPSetEvent(writeCompleteNotificationEvent, 0);
}

void RFCOMMChannelController::rfcommChannelData(void* dataPointer, size_t dataLength) {
    ndebug("rfcommChannelData");
    if (isConnected && !isClosed) {
        receiveBuffer.write(dataPointer, dataLength);
		MPSetEvent(notificationEvent, 1);
    }
}

void RFCOMMChannelController::rfcommChannelWriteComplete(void* refcon, IOReturn error) {
    if (refcon != NULL) {
        ((RFCOMMConnectionWrite*)refcon)->rfcommChannelWriteComplete(error);
    }
    MPSetEvent(writeCompleteNotificationEvent, 1);
}

IOReturn RFCOMMChannelController::close() {
    IOReturn rc = kIOReturnSuccess;
    if (delegate != NULL) {
        [delegate close];
    }
    if (rfcommChannel != NULL) {
        isClosed = true;
        MPSetEvent(notificationEvent, 0);
        MPSetEvent(writeCompleteNotificationEvent, 0);

        IOBluetoothDevice *device = [rfcommChannel getDevice];
        [rfcommChannel setDelegate:NULL];
        rc = [rfcommChannel closeChannel];
        if (device != NULL) {
            [device closeConnection];
        }
        [rfcommChannel release];
        rfcommChannel = NULL;
    }
    if (delegate != NULL) {
        [delegate release];
        delegate = NULL;
    }
    return rc;
}

RFCOMMConnectionOpen::RFCOMMConnectionOpen() {
    name = "RFCOMMConnectionOpen";
}

void RFCOMMConnectionOpen::run() {

    comm->openStatus = kIOReturnSuccess;
    BluetoothRFCOMMChannelID channelID = this->channel;

    status = [comm->bluetoothDevice openRFCOMMChannelAsync:&(comm->rfcommChannel) withChannelID:channelID  delegate:comm->delegate];
    if ((status != kIOReturnSuccess) || (comm->rfcommChannel == NULL)) {
        error = 1;
        return;
    }
    status = [comm->rfcommChannel setDelegate:comm->delegate];
    if (status != kIOReturnSuccess) {
        error = 1;
    }

    [comm->rfcommChannel retain];

}

RUNNABLE(RFCOMMChannelClose, "RFCOMMChannelClose") {
    RFCOMMChannelController* comm = (RFCOMMChannelController*)pData[0];
    iData = comm->close();
}

long RFCOMMChannelCloseExec(RFCOMMChannelController* comm) {
    RFCOMMChannelClose runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	comm->readyToFree = TRUE;
	return runnable.lData;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfOpenClientConnectionImpl
  (JNIEnv *env, jobject peer, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint timeout) {
    RFCOMMChannelController* comm = new RFCOMMChannelController();
	if (!stack->commPool->addObject(comm, 'r')) {
		delete comm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}

	BasebandConnectionOpen basebandOpen;
	basebandOpen.comm = comm;
    basebandOpen.address = address;
    basebandOpen.authenticate = authenticate;
    basebandOpen.encrypt = encrypt;
    basebandOpen.timeout = timeout;
    synchronousBTOperation(&basebandOpen);

    if (basebandOpen.error != 0) {
        RFCOMMChannelCloseExec(comm);
        throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open baseband connection [0x%08x]", basebandOpen.status);
        return 0;
    }

    if (!comm->waitForConnection(env, peer, true, timeout)) {
        RFCOMMChannelCloseExec(comm);
        return 0;
    }

	RFCOMMConnectionOpen rfcommOpen;
	rfcommOpen.comm = comm;
    rfcommOpen.address = address;
    rfcommOpen.channel = channel;
    rfcommOpen.authenticate = authenticate;
    rfcommOpen.encrypt = encrypt;
    rfcommOpen.timeout = timeout;
    synchronousBTOperation(&rfcommOpen);

    if (rfcommOpen.error != 0) {
        RFCOMMChannelCloseExec(comm);
        throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection(1) [0x%08x]", rfcommOpen.status);
        return 0;
    }

    if (!comm->waitForConnection(env, peer, false, timeout)) {
        RFCOMMChannelCloseExec(comm);
        return 0;
    }
    debug(("rfcomm connected"));
	return comm->internalHandle;
}

RFCOMMChannelController* validRFCOMMChannelHandle(JNIEnv *env, jlong handle) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (RFCOMMChannelController*)stack->commPool->getObject(env, handle, 'r');
}

RFCOMMChannelController* validOpenRFCOMMChannelHandle(JNIEnv *env, jlong handle) {
    RFCOMMChannelController* comm = validRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return NULL;
	}
	if (!comm->isConnected || comm->isClosed) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return NULL;
	}
	return comm;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfCloseClientConnection
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMChannelController* comm = validRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return;
	}
	long rc = RFCOMMChannelCloseExec(comm);
	if (rc != kIOReturnSuccess) {
	    throwIOExceptionExt(env, "Failed to close RFCOMM channel [0x%08x]", rc);
	}
}

RUNNABLE(RFCOMMChannelRemoteAddress, "RFCOMMChannelRemoteAddress") {
    RFCOMMChannelController* comm = (RFCOMMChannelController*)pData[0];
    if (comm->rfcommChannel == NULL) {
        ndebug("rfcommChannel is NULL");
        error = 1;
    } else {
        bool isOpen = [comm->rfcommChannel isOpen];
        if (!isOpen) {
            ndebug("rfcommChannel is NOT Open");
            error = 1;
            return;
        }
        IOBluetoothDevice* device = [comm->rfcommChannel getDevice];
        if (device == NULL) {
            ndebug("rfcommChannel getDevice is NULL");
            error = 1;
            return;
        }
        comm->address = OSxAddrToLong([device getAddress]);
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getConnectionRfRemoteAddress
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMChannelController* comm = validOpenRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
	RFCOMMChannelRemoteAddress runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	if (runnable.error) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return 0;
	}

    return comm->address;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfRead__J
  (JNIEnv *env, jobject peer, jlong handle) {
    RFCOMMChannelController* comm = validRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return -1;
	}
	Edebug(("->read()"));
	if (comm->isClosed) {
		return -1;
	}
	if (comm->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
		return 0;
	}
	while ((stack != NULL) && comm->isConnected && (!comm->isClosed) && (comm->receiveBuffer.available() == 0)) {
		MPEventFlags flags;
        OSStatus err = MPWaitForEvent(comm->notificationEvent, &flags, kDurationMillisecond * 500);
		if ((err != kMPTimeoutErr) && (err != noErr)) {
			throwRuntimeException(env, "MPWaitForEvent");
			return 0;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
			debug(("Interrupted while reading"));
			return 0;
		}
	}
	if ((stack == NULL) || (comm->isClosed) || (comm->receiveBuffer.available() == 0)) {
		// See InputStream.read();
		return -1;
	}
	return comm->receiveBuffer.readByte();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfRead__J_3BII
  (JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
    RFCOMMChannelController* comm = validRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return -1;
	}
	Edebug(("->read(byte[%i])", len));
	if (comm->isClosed) {
		return -1;
	}
	if (comm->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
		return -1;
	}
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;
	while ((stack != NULL) && comm->isConnected && (!comm->isClosed) && (done < len)) {
		while ((stack != NULL) && comm->isConnected  && (!comm->isClosed) && (comm->receiveBuffer.available() == 0)) {
			MPEventFlags flags;
            OSStatus err = MPWaitForEvent(comm->notificationEvent, &flags, kDurationMillisecond * 500);
		    if ((err != kMPTimeoutErr) && (err != noErr)) {
			    throwRuntimeException(env, "MPWaitForEvent");
			    return 0;
		    }
			if (isCurrentThreadInterrupted(env, peer)) {
				debug(("Interrupted while reading"));
				return 0;
			}
		}
		if (stack == NULL) {
			env->ReleaseByteArrayElements(b, bytes, 0);
			return -1;
		}
		int count = comm->receiveBuffer.available();
		if (count > 0) {
		if (count > len - done) {
				count = len - done;
			}
			done += comm->receiveBuffer.read(bytes + off + done, count);
		}
		if (done != 0) {
		    // Don't do readFully!
		    break;
		}
		debug(("read([]) received %i", count));
	}

	if (!comm->isConnected) {
		debug(("read([]) not connected"));
	}
	// Read from not Connected
	int count = comm->receiveBuffer.available();
	if (count > 0) {
		if (count > len - done) {
			count = len - done;
		}
		done += comm->receiveBuffer.read(bytes + off + done, count);
		debug(("read[] available %i", done));
	}

	if ((stack == NULL) || (comm->isClosed) || (!comm->isConnected && done == 0)) {
		if (done == 0) {
			debug(("read([]) no data"));
		}
		// See InputStream.read();
		debug(("read([]) return EOF"));
		done = -1;
	} else {
		debug(("read([]) return %i", done));
	}
	env->ReleaseByteArrayElements(b, bytes, 0);
	return done;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfReadAvailable
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMChannelController* comm = validOpenRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
    if (comm->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
	}
	return comm->receiveBuffer.available();
}

RFCOMMConnectionWrite::RFCOMMConnectionWrite() {
    name = "RFCOMMConnectionWrite";
    writeComplete = false;
    ioerror = kIOReturnSuccess;
}

void RFCOMMConnectionWrite::rfcommChannelWriteComplete(IOReturn status) {
    ioerror = status;
    if (ioerror != kIOReturnSuccess) {
        error = 1;
    }
    writeComplete = true;
}

void RFCOMMConnectionWrite::run() {
    void* notify = NULL;
    notify = this;
    ioerror = [comm->rfcommChannel writeAsync:data length:length refcon:notify];
    if (ioerror != kIOReturnSuccess) {
        error = 1;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfWrite
  (JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
    RFCOMMChannelController* comm = validOpenRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return;
	}

    jbyte *bytes = env->GetByteArrayElements(b, 0);

	UInt16 done = 0;
    BOOL error = false;
	while ((stack != NULL) && (!error) && (done < len) && (comm->isConnected) && (!comm->isClosed)) {
		UInt16 numBytesRemaining = len - done;
		UInt16 writeLen = ( ( numBytesRemaining > comm->rfcommChannelMTU ) ? comm->rfcommChannelMTU :  numBytesRemaining );
		RFCOMMConnectionWrite runnable;
	    runnable.comm = comm;
	    runnable.data = (void*)(bytes + off + done);
	    runnable.length = writeLen;
        synchronousBTOperation(&runnable);
        if (runnable.error != 0) {
            throwIOExceptionExt(env, "Failed to write [0x%08x]", runnable.ioerror);
			break;
        }
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
			    error = true;
			    break;
		    }
            if (isCurrentThreadInterrupted(env, peer)) {
			    debug(("Interrupted while writing"));
			    error = true;
			    break;
		    }
	        break;
		}
        done += writeLen;
        if (runnable.error != 0) {
            throwIOExceptionExt(env, "Failed to write [0x%08x]", runnable.ioerror);
			break;
        }
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}