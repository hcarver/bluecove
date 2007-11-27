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

#ifndef OBJC_VERSION
void rfcommEventListener(IOBluetoothRFCOMMChannelRef rfcommChannelRef, void *refCon, IOBluetoothRFCOMMChannelEvent *event);
#else
@implementation RFCOMMChannelDelegate

- (id)initWithController:(RFCOMMChannelController*)controller {
    _controller = controller;
    return self;
}

- (void)close {
    _controller = NULL;
}

- (void)rfcommChannelOpenComplete:(IOBluetoothRFCOMMChannel*)rfcommChannel status:(IOReturn)error {
    if (isValidObject(_controller)) {
        _controller->rfcommChannelOpenComplete(error);
    }
}

- (void)rfcommChannelData:(IOBluetoothRFCOMMChannel *)rfcommChannel
                     data:(void *)dataPointer
                   length:(size_t)dataLength {
    if (isValidObject(_controller)) {
        _controller->rfcommChannelData(dataPointer, dataLength);
    }
}

- (void)rfcommChannelWriteComplete:(IOBluetoothRFCOMMChannel*)rfcommChannel
                            refcon:(void*)refcon
                            status:(IOReturn)error {
    if (isValidObject(_controller)) {
        _controller->rfcommChannelWriteComplete(refcon, error);
    }
}

- (void)rfcommChannelClosed:(IOBluetoothRFCOMMChannel *)rfcommChannel {
    ndebug("d.rfcommChannelClosed->");
    if (isValidObject(_controller)) {
        _controller->rfcommChannelClosed();
    }
    ndebug("d.rfcommChannelClosed-<");
}

// Not used
- (void)rfcommChannelControlSignalsChanged:(IOBluetoothRFCOMMChannel*)rfcommChannel {
}

- (void)rfcommChannelFlowControlChanged:(IOBluetoothRFCOMMChannel*)rfcommChannel {
}

- (void)rfcommChannelQueueSpaceAvailable:(IOBluetoothRFCOMMChannel*)rfcommChannel {
}


@end
#endif

RFCOMMChannelController::RFCOMMChannelController() {
#ifdef OBJC_VERSION
    delegate = NULL;
#endif
    rfcommChannel = NULL;

    openStatus = kIOReturnSuccess;
    closedStatus = kIOReturnSuccess;
    isClosed = false;
	isConnected = false;
	MPCreateEvent(&notificationEvent);
	MPCreateEvent(&writeCompleteNotificationEvent);
}

RFCOMMChannelController::~RFCOMMChannelController() {
    ndebug("~RFCOMMChannelController");
    MPSetEvent(notificationEvent, 0);
    MPSetEvent(writeCompleteNotificationEvent, 0);
    MPDeleteEvent(notificationEvent);
    MPDeleteEvent(writeCompleteNotificationEvent);
}

#ifdef OBJC_VERSION

void RFCOMMChannelController::initDelegate() {
    delegate = [[RFCOMMChannelDelegate alloc] initWithController:this];
    [delegate retain];
}

void RFCOMMChannelController::rfcommChannelOpenComplete(IOReturn error) {
    ndebug("rfcommChannelOpenComplete");
    rfcommChannelMTU = [rfcommChannel getMTU];
    openStatus = error;
    isConnected = true;
    MPSetEvent(notificationEvent, 1);
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
    MPSetEvent(writeCompleteNotificationEvent, 1);
}

#else // OBJC_VERSION

void RFCOMMChannelController::rfcommEvent(IOBluetoothRFCOMMChannelRef rfcommChannelRef, IOBluetoothRFCOMMChannelEvent *event) {
    switch (event->eventType ) {
        case kIOBluetoothRFCOMMChannelEventTypeClosed:
            ndebug("RFCOMMChannelEvent Closed");
            isClosed = true;
            closedStatus = event->status;
            MPSetEvent(notificationEvent, 0);
            MPSetEvent(writeCompleteNotificationEvent, 0);
            break;
        case kIOBluetoothRFCOMMChannelEventTypeOpenComplete:
            ndebug("RFCOMMChannelEvent OpenComplete");
            rfcommChannelMTU = IOBluetoothRFCOMMChannelGetMTU(rfcommChannelRef);
            openStatus = event->status;
            isConnected = true;
            MPSetEvent(notificationEvent, 1);
            break;
        case kIOBluetoothRFCOMMChannelEventTypeData:
            if (isConnected && !isClosed) {
		        receiveBuffer.write(event->u.newData.dataPtr, event->u.newData.dataSize);
		        MPSetEvent(notificationEvent, 1);
	        }
            break;
        case kIOBluetoothRFCOMMChannelEventTypeQueueSpaceAvailable:
        case kIOBluetoothRFCOMMChannelEventTypeWriteComplete:
            MPSetEvent(writeCompleteNotificationEvent, 1);
            break;
    }
}

#endif // ifdef OBJC_VERSION

IOReturn RFCOMMChannelController::close() {
    IOReturn rc = kIOReturnSuccess;
#ifdef OBJC_VERSION
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
#else // ifdef OBJC_VERSION
    if (rfcommChannel != NULL) {
        isClosed = true;
        rc = IOBluetoothRFCOMMChannelCloseChannel(rfcommChannel);
        MPSetEvent(notificationEvent, 0);
        MPSetEvent(writeCompleteNotificationEvent, 0);
        IOBluetoothDeviceRef dev = IOBluetoothRFCOMMChannelGetDevice(rfcommChannel);
        if (dev != NULL) {
            IOBluetoothDeviceCloseConnection(dev);
        }
        IOBluetoothObjectRelease(rfcommChannel);
        rfcommChannel = NULL;
    }
#endif // ifdef OBJC_VERSION
    return rc;
}

BOOL RFCOMMChannelController::waitForConnection(JNIEnv *env, jint timeout) {
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent ();
    while ((stack != NULL) && (!isClosed) && (!isConnected)) {
        MPEventFlags flags;
        MPWaitForEvent(notificationEvent, &flags, kDurationMillisecond * 500);
        CFAbsoluteTime nowTime = CFAbsoluteTimeGetCurrent ();
        if ((timeout > 0) && ((nowTime - startTime) * 1000  > timeout)) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_TIMEOUT, "Connection timeout");
        }
    }
    if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return false;
	}
    if (isClosed) {
	    throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection [0x%08x]", closedStatus);
	    return false;
    }

    if (openStatus != kIOReturnSuccess) {
        isConnected = false;
        throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection [0x%08x]", openStatus);
        return false;
    }

    if (isConnected) {
        return true;
    }

    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection");
	return false;
}

RFCOMMConnectionOpen::RFCOMMConnectionOpen() {
    name = "RFCOMMConnectionOpen";
}

void RFCOMMConnectionOpen::run() {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(this->address, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (deviceRef == NULL) {
        error = 1;
        return;
    }
    comm->address = this->address;

    BluetoothRFCOMMChannelID channelID = this->channel;
    IOReturn rc;
#ifdef OBJC_VERSION
    comm->initDelegate();
    IOBluetoothDevice* dev = [IOBluetoothDevice withDeviceRef:deviceRef];
    rc = [dev openRFCOMMChannelAsync:&(comm->rfcommChannel) withChannelID:channelID  delegate:comm->delegate];
    if ((rc != kIOReturnSuccess) || (comm->rfcommChannel == NULL)) {
        error = 1;
    } else {
        rc = [comm->rfcommChannel setDelegate:comm->delegate];
        if (rc != kIOReturnSuccess) {
            error = 1;
        } else {
            [comm->rfcommChannel retain];
        }
    }
#else // OBJC_VERSION
    rc = IOBluetoothDeviceOpenRFCOMMChannelAsync(deviceRef, &(comm->rfcommChannel), channelID, rfcommEventListener, comm);
    if ((rc != kIOReturnSuccess) || (comm->rfcommChannel == NULL))  {
        error = 1;
    } else {
        rc = IOBluetoothRFCOMMChannelRegisterIncomingEventListener(comm->rfcommChannel, rfcommEventListener, comm);
        if (rc != kIOReturnSuccess) {
            error = 1;
        }
    }
#endif // OBJC_VERSION
    lData = rc;
}

#ifndef OBJC_VERSION
// IOBluetoothRFCOMMChannelIncomingEventListener, Callback for RFCOMM Events
void rfcommEventListener(IOBluetoothRFCOMMChannelRef rfcommChannelRef, void *refCon, IOBluetoothRFCOMMChannelEvent *event) {
    ndebug("RFCOMMChannelEvent");
    RFCOMMChannelController* comm = (RFCOMMChannelController*)refCon;
    if (isValidObject(comm)) {
        comm->rfcommEvent(rfcommChannelRef, event);
    }
}
#endif // ifndef OBJC_VERSION

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
  (JNIEnv *env, jobject, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint timeout) {
    RFCOMMChannelController* comm = new RFCOMMChannelController();
	if (!stack->commPool->addObject(comm, 'r')) {
		delete comm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}

	RFCOMMConnectionOpen runnable;
	runnable.comm = comm;
    runnable.address = address;
    runnable.channel = channel;
    runnable.authenticate = authenticate;
    runnable.encrypt = encrypt;
    runnable.timeout = timeout;
    synchronousBTOperation(&runnable);

    if (runnable.error != 0) {
        RFCOMMChannelCloseExec(comm);
        throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection [0x%08x]", runnable.lData);
        return 0;
    }

    if (!comm->waitForConnection(env, timeout)) {
        RFCOMMChannelCloseExec(comm);
        return 0;
    }
    debug("connected");
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

RUNNABLE(RFCOMMChannelIsOpen, "RFCOMMChannelIsOpen") {
    RFCOMMChannelController* comm = (RFCOMMChannelController*)pData[0];
    if (comm->rfcommChannel == NULL) {
        bData = false;
    } else {
#ifdef OBJC_VERSION
        bData = [comm->rfcommChannel isOpen];
#else
        bData = IOBluetoothRFCOMMChannelIsOpen(comm->rfcommChannel);
#endif // ifdef OBJC_VERSION
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getConnectionRfRemoteAddress
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMChannelController* comm = validOpenRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
	RFCOMMChannelIsOpen runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	if (!runnable.bData) {
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
	Edebug("->read()");
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
			debug("Interrupted while reading");
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
	Edebugs("->read(byte[%i])", len);
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
				debug("Interrupted while reading");
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
		debug1("read([]) received %i", count);
	}

	if (!comm->isConnected) {
		debug("read([]) not connected");
	}
	// Read from not Connected
	int count = comm->receiveBuffer.available();
	if (count > 0) {
		if (count > len - done) {
			count = len - done;
		}
		done += comm->receiveBuffer.read(bytes + off + done, count);
		debug1("read[] available %i", done);
	}

	if ((stack == NULL) || (comm->isClosed) || (!comm->isConnected && done == 0)) {
		if (done == 0) {
			debug("read([]) no data");
		}
		// See InputStream.read();
		debug("read([]) return EOF");
		done = -1;
	} else {
		debugs("read([]) return %i", done);
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
}

void RFCOMMConnectionWrite::run() {
    IOReturn rc;
#ifdef OBJC_VERSION
    rc = [comm->rfcommChannel writeAsync:data length:length refcon:this];
#else
    rc = IOBluetoothRFCOMMChannelWriteAsync(comm->rfcommChannel, data, length, comm);
#endif // ifdef OBJC_VERSION
    lData = rc;
    if (rc != kIOReturnSuccess) {
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
            throwIOExceptionExt(env, "Failed to write [0x%08x]", runnable.lData);
			break;
        }
        while ((stack != NULL) &&( comm->isConnected) && (!comm->isClosed)) {
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
			    debug("Interrupted while writing");
			    error = true;
			    break;
		    }
	        break;
		}
        done += writeLen;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}