/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @version $Id$
 */
#define CPP__FILE "BlueCoveBlueZ.c"

#include "BlueCoveBlueZ.h"

#include <bluetooth/sdp_lib.h>

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_isNativeCodeLoaded
  (JNIEnv *env, jobject peer) {
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getLibraryVersionNative
  (JNIEnv *env, jobject peer) {
    return com_intel_bluetooth_BluetoothStackBlueZ_NATIVE_LIBRARY_VERSION;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_enableNativeDebug
  (JNIEnv *env, jobject peer, jclass loggerClass, jboolean on) {
    enableNativeDebug(env, loggerClass, on);
}

int deviceClassBytesToInt(uint8_t* deviceClass) {
    return ((deviceClass[2] & 0xff)<<16)|((deviceClass[1] & 0xff)<<8)|(deviceClass[0] & 0xff);
}

jlong deviceAddrToLong(bdaddr_t* address) {
    jlong addressLong = 0;
    int i;
    for (i = sizeof(address->b) - 1; i >= 0; i--) {
        addressLong = (addressLong << 8) | address->b[i];
    }
    return addressLong;
}

void longToDeviceAddr(jlong addr, bdaddr_t* address) {
    int i;
    for(i = 0; i < sizeof(address->b); i++) {
        address->b[i] = (uint8_t)(addr & 0xFF);
        addr >>= 8;
    }
}

jlong ptr2jlong(void *ptr) {
    jlong l = 0;
    memcpy(&l, &ptr, sizeof(void*));
    return l;
}

void* jlong2ptr(jlong l) {
    void* ptr = NULL;
    memcpy(&ptr, &l, sizeof(void*));
    return ptr;
}

void reverseArray(jbyte* array, int length) {
    int i;
    jbyte temp;
    for(i = 0; i < length / 2; i++) {
        temp = array[i];
        array[i] = array[length - 1 - i];
        array[length - 1 - i] = temp;
    }
}

void convertUUIDByteArrayToUUID(JNIEnv *env, jbyteArray byteArray, uuid_t* uuid) {
    jbyte *bytes = (*env)->GetByteArrayElements(env, byteArray, 0);
    convertUUIDBytesToUUID(bytes, uuid);
    // unpin array
    (*env)->ReleaseByteArrayElements(env, byteArray, bytes, 0);
}

void convertUUIDBytesToUUID(jbyte *bytes, uuid_t* uuid) {
    uuid->type = SDP_UUID128;
    memcpy(&uuid->value, bytes, 128/8);
}

int getBlueZVersionMajor() {
    // until a way is found to detect installed version, assume it is 4
    return BLUEZ_VERSION_MAJOR_4;
}