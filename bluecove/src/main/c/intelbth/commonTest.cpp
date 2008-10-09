/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 *  @version $Id$
 */

#include "common.h"
#include "commonObjects.h"

#ifndef CPP_FILE
#define CPP_FILE "commonTest.cpp"
#endif

#include "com_intel_bluetooth_NativeTestInterfaces.h"

#ifdef WIN32
JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testUUIDConversion
(JNIEnv *env, jclass, jbyteArray uuidValue) {
	GUID g;
	// pin array
	jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
	// build UUID
	convertUUIDBytesToGUID(bytes, &g);
	// unpin array
	env->ReleaseByteArrayElements(uuidValue, bytes, 0);

	char m[1064];
	unsigned char* d = g.Data4;
	sprintf_s(m, 1064, "{%u , %u, %u, {%u , %u, %u , %u, %u, %u , %u, %u}}", g.Data1, g.Data2, g.Data3, d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7]);
	debug(("UUID = %s", m));

	jbyteArray uuidValueConverted = env->NewByteArray(16);
	jbyte *bytesConverted = env->GetByteArrayElements(uuidValueConverted, 0);

	convertGUIDToUUIDBytes(&g, bytesConverted);

	env->ReleaseByteArrayElements(uuidValueConverted, bytesConverted, 0);

	return uuidValueConverted;
}
#endif

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferCreate
(JNIEnv *, jclass, jint size) {
	return (jlong) new ReceiveBuffer(size);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferClose
(JNIEnv *, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	delete b;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferWrite
(JNIEnv *env, jclass, jlong bufferHandler, jbyteArray data) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	jint rc = b->write(bytes, env->GetArrayLength(data));
	env->ReleaseByteArrayElements(data, bytes, 0);
	return rc;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferRead__J_3B
(JNIEnv *env, jclass, jlong bufferHandler, jbyteArray data) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	jint rc = b->read(bytes, env->GetArrayLength(data));
	env->ReleaseByteArrayElements(data, bytes, 0);
	return rc;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferRead__J
(JNIEnv *env, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->readByte();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferSkip
(JNIEnv *env, jclass, jlong bufferHandler, jint size) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->skip(size);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferAvailable
(JNIEnv *env, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->available();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferIsOverflown
(JNIEnv *, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->isOverflown();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testReceiveBufferIsCorrupted
(JNIEnv *, jclass, jlong bufferHandler) {
	ReceiveBuffer* b = (ReceiveBuffer*)bufferHandler;
	return b->isCorrupted();
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testThrowException
(JNIEnv *env, jclass, jint extype) {
	switch (extype) {
		case 0: throwException(env, "java/lang/Exception", "0"); break;
		case 1: throwException(env, "java/lang/Exception", "1[%s]", "str"); break;
		case 2: throwIOException(env, "2"); break;
		case 3: throwIOException(env, "3[%s]", "str"); break;
	    case 4: throwBluetoothStateException(env, "4"); break;
		case 5: throwBluetoothStateException(env, "5[%s]", "str"); break;
		case 6: throwRuntimeException(env, "6"); break;
		case 7: throwBluetoothConnectionException(env, 1, "7"); break;
		case 8: throwBluetoothConnectionException(env, 2, "8[%s]", "str"); break;

		case 22:
			// Throw Exception two times in a row. Second Exception ignored
			throwException(env, "java/lang/Exception", "22.1");
			throwIOException(env, "22.2");
			break;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testDebug
(JNIEnv *env, jclass, jint argc, jstring message) {
	if ((argc == 0) || (message == NULL)) {
	    debug(("message"));
	    return;
	}
	const char *c = env->GetStringUTFChars(message, 0);
	switch (argc) {
		case 1: debug(("message[%s]", c)); break;
		case 2: debug(("message[%s],[%s]", c, c)); break;
		case 3: debug(("message[%s],[%s],[%i]", c, c, argc)); break;
	}
	env->ReleaseStringUTFChars(message, c);
}