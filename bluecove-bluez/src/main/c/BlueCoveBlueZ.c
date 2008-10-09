/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
 * @version $Id: BlueCoveBlueZ.c 1724 2008-01-31 16:59:24Z skarzhevskyy $
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
  	return com_intel_bluetooth_BluetoothStackBlueZ_BLUECOVE_DBUS_VERSION;
    //return com_intel_bluetooth_BluetoothStackBlueZ_NATIVE_LIBRARY_VERSION;
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

