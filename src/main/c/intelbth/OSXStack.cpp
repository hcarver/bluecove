/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

#include "OSXStack.h"

OSXStack* stack = NULL;


OSXStack::OSXStack() {
}

OSXStack::~OSXStack() {
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLibraryVersion
(JNIEnv *, jobject) {
	return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_detectBluetoothStack
(JNIEnv *env, jobject) {
	return BLUECOVE_STACK_DETECT_OSX;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_enableNativeDebug
(JNIEnv *env, jobject, jclass loggerClass, jboolean on) {
	enableNativeDebug(env, loggerClass, on);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_initializeImpl
(JNIEnv *env, jobject) {
	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_destroyImpl
(JNIEnv *, jobject) {
}

// --- LocalDevice

void OSxAddrToString(char* addressString, BluetoothDeviceAddress* addr) {
	snprintf(addressString, 14, "%02x%02x%02x%02x%02x%02x",
			 addr->data[0],
             addr->data[1],
             addr->data[2],
             addr->data[3],
             addr->data[4],
             addr->data[5]);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject) {
    BluetoothDeviceAddress localAddress;
    if (IOBluetoothLocalDeviceReadAddress(&localAddress, NULL, NULL, NULL)) {
        throwBluetoothStateException(env, "Bluetooth Device is not ready");
		return NULL;
    }
    char addressString[14];
    OSxAddrToString(addressString, &localAddress);
    return env->NewStringUTF(addressString);
}
