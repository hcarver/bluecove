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
}

L2CAPChannelController::~L2CAPChannelController() {
}

void L2CAPChannelController::l2capChannelOpenComplete(IOReturn error) {
}

void L2CAPChannelController::l2capChannelClosed() {
}

void L2CAPChannelController::l2capChannelData(void* dataPointer, size_t dataLength) {
}


void L2CAPChannelController::l2capChannelWriteComplete(void* refcon, IOReturn error) {
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

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2OpenClientConnectionImpl
  (JNIEnv *env, jobject, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint receiveMTU, jint transmitMTU, jint timeout) {
    return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2CloseClientConnection
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return;
	}
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Ready
  (JNIEnv *env, jobject, jlong handle) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return JNI_FALSE;
	}
	return JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Receive
  (JNIEnv *env, jobject, jlong handle, jbyteArray) {
    L2CAPChannelController* comm = validL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return 0;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_l2Send
  (JNIEnv *env, jobject, jlong handle, jbyteArray) {
    L2CAPChannelController* comm = validOpenL2CAPChannelHandle(env, handle);
	if (comm == NULL) {
		return;
	}
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