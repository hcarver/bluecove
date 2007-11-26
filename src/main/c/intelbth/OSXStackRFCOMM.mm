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

void rfcommEventListener(IOBluetoothRFCOMMChannelRef rfcommChannelRef, void *refCon, IOBluetoothRFCOMMChannelEvent *event);

RFCOMMChannel::RFCOMMChannel() {
    rfcommChannel = NULL;
    isClosed = false;
	isConnected = false;
	MPCreateEvent(&notificationEvent);
}

RFCOMMChannel::~RFCOMMChannel() {
    MPSetEvent(notificationEvent, 0);
    MPDeleteEvent(notificationEvent);
}

void RFCOMMChannel::rfcommEvent(IOBluetoothRFCOMMChannelRef rfcommChannelRef, IOBluetoothRFCOMMChannelEvent *event) {
    switch (event->eventType ) {
        case kIOBluetoothRFCOMMChannelEventTypeClosed:
            ndebug("RFCOMMChannelEvent Closed");
            isClosed = true;
            closedStatus = event->status;
            MPSetEvent(notificationEvent, 0);
            break;
        case kIOBluetoothRFCOMMChannelEventTypeOpenComplete:
            ndebug("RFCOMMChannelEvent OpenComplete");
            rfcommChannelMTU = IOBluetoothRFCOMMChannelGetMTU(rfcommChannelRef);
            isConnected = true;
            MPSetEvent(notificationEvent, 1);
            break;
        case kIOBluetoothRFCOMMChannelEventTypeData:
            if (isConnected && !isClosed) {
		        receiveBuffer.write(event->u.newData.dataPtr, event->u.newData.dataSize);
		        MPSetEvent(notificationEvent, 1);
	        }
            break;
        case kIOBluetoothRFCOMMChannelEventTypeWriteComplete:
        MPSetEvent(notificationEvent, 2);
            break;
    }
}

IOReturn RFCOMMChannel::close() {
    IOReturn rc;
    if (rfcommChannel != NULL) {
        isClosed = true;
        rc = IOBluetoothRFCOMMChannelCloseChannel(rfcommChannel);
        MPSetEvent(notificationEvent, 0);
        IOBluetoothDeviceRef dev = IOBluetoothRFCOMMChannelGetDevice(rfcommChannel);
        if (dev != NULL) {
            IOBluetoothDeviceCloseConnection(dev);
        }
        IOBluetoothObjectRelease(rfcommChannel);
        rfcommChannel = NULL;
    } else {
        rc = kIOReturnSuccess;
    }
    return rc;
}

BOOL RFCOMMChannel::waitForConnection(JNIEnv *env, jint timeout) {
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

    if (isConnected) {
        return true;
    }

    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection");
	return false;
}

OpenRFCOMMConnection::OpenRFCOMMConnection() {
    name = "OpenRFCOMMConnection";
}

void OpenRFCOMMConnection::run() {
    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(this->address, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    if (deviceRef == NULL) {
        error = 1;
        return;
    }
    comm->address = this->address;

    BluetoothRFCOMMChannelID channelID = this->channel;
    IOReturn rc = IOBluetoothDeviceOpenRFCOMMChannelAsync(deviceRef, &(comm->rfcommChannel), channelID, rfcommEventListener, comm);
    if ((rc != kIOReturnSuccess) || (comm->rfcommChannel == NULL))  {
        error = 1;
    } else {
        rc = IOBluetoothRFCOMMChannelRegisterIncomingEventListener(comm->rfcommChannel, rfcommEventListener, comm);
        if (rc != kIOReturnSuccess) {
            error = 1;
        }
    }
    lData = rc;
}

BOOL isValidObject(RFCOMMChannel* comm ) {
    if (comm == NULL) {
        return false;
    }
    if ((comm->magic1 != MAGIC_1) || (comm->magic2 != MAGIC_2)) {
		return false;
	}
	return comm->isValidObject();
}

// IOBluetoothRFCOMMChannelIncomingEventListener, Callback for RFCOMM Events
void rfcommEventListener(IOBluetoothRFCOMMChannelRef rfcommChannelRef, void *refCon, IOBluetoothRFCOMMChannelEvent *event) {
    ndebug("RFCOMMChannelEvent");
    RFCOMMChannel* comm = (RFCOMMChannel*)refCon;
    if (isValidObject(comm)) {
        comm->rfcommEvent(rfcommChannelRef, event);
    }
}

RUNNABLE(RFCOMMChannelClose, "RFCOMMChannelClose") {
    RFCOMMChannel* comm = (RFCOMMChannel*)pData[0];
    iData = comm->close();
}

long RFCOMMChannelCloseExec(RFCOMMChannel* comm) {
    RFCOMMChannelClose runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);
	comm->readyToFree = TRUE;
	return runnable.lData;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfOpenClientConnectionImpl
  (JNIEnv *env, jobject, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint timeout) {
    RFCOMMChannel* comm = new RFCOMMChannel();
	if (!stack->commPool->addObject(comm, 'r')) {
		delete comm;
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
		return 0;
	}

	OpenRFCOMMConnection runnable;
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

RFCOMMChannel* validRFCOMMChannelHandle(JNIEnv *env, jlong handle) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (RFCOMMChannel*)stack->commPool->getObject(env, handle, 'r');
}

RFCOMMChannel* validOpenRFCOMMChannelHandle(JNIEnv *env, jlong handle) {
    RFCOMMChannel* comm = validRFCOMMChannelHandle(env, handle);
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
    RFCOMMChannel* comm = validRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return;
	}
	long rc = RFCOMMChannelCloseExec(comm);
	if (rc != kIOReturnSuccess) {
	    throwIOExceptionExt(env, "Failed to close RFCOMM channel [0x%08x]", rc);
	}
}

RUNNABLE(RFCOMMChannelIsOpen, "RFCOMMChannelIsOpen") {
    RFCOMMChannel* comm = (RFCOMMChannel*)pData[0];
    if (comm->rfcommChannel == NULL) {
        bData = false;
    } else {
        bData = IOBluetoothRFCOMMChannelIsOpen(comm->rfcommChannel);
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getConnectionRfRemoteAddress
  (JNIEnv *env, jobject, jlong handle) {
    RFCOMMChannel* comm = validOpenRFCOMMChannelHandle(env, handle);
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
    RFCOMMChannel* comm = validRFCOMMChannelHandle(env, handle);
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
    RFCOMMChannel* comm = validRFCOMMChannelHandle(env, handle);
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
    RFCOMMChannel* comm = validOpenRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return 0;
	}
    if (comm->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
	}
	return comm->receiveBuffer.available();
}

WriteRFCOMMConnection::WriteRFCOMMConnection() {
    name = "WriteRFCOMMConnection";
}

void WriteRFCOMMConnection::run() {
    IOReturn rc = IOBluetoothRFCOMMChannelWriteAsync(comm->rfcommChannel, data, length, comm);
    lData = rc;
    if (rc != kIOReturnSuccess) {
        error = 1;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_connectionRfWrite
  (JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
    RFCOMMChannel* comm = validOpenRFCOMMChannelHandle(env, handle);
    if (comm == NULL) {
		return;
	}

	jbyte *bytes = env->GetByteArrayElements(b, 0);

	UInt16 done = 0;
    BOOL error = false;
	while ((stack != NULL) && (!error) && (done < len) && (comm->isConnected) && (!comm->isClosed)) {
		UInt16 numBytesRemaining = len - done;
		UInt16 writeLen = ( ( numBytesRemaining > comm->rfcommChannelMTU ) ? comm->rfcommChannelMTU :  numBytesRemaining );
		WriteRFCOMMConnection runnable;
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
            OSStatus err = MPWaitForEvent(comm->notificationEvent, &flags, kDurationMillisecond * 500);
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
		    if (flags & 2) {
		        break;
		    }
		}
        done += writeLen;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}