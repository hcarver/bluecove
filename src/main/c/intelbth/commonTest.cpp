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

#include "common.h"

#ifdef VC6
#define CPP_FILE "commonTest.cpp"
#endif

#include "com_intel_bluetooth_NativeTestInterfaces.h"

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
	debugs("UUID = %s", m);

	jbyteArray uuidValueConverted = env->NewByteArray(16);
	jbyte *bytesConverted = env->GetByteArrayElements(uuidValueConverted, 0);

	convertGUIDToUUIDBytes(&g, bytesConverted);

	env->ReleaseByteArrayElements(uuidValueConverted, bytesConverted, 0);

	return uuidValueConverted;
}

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
		case 1: throwExceptionExt(env, "java/lang/Exception", "1[%s]", "str"); break;
		case 2: throwIOException(env, "2"); break;
		case 3: throwIOExceptionExt(env, "3[%s]", "str"); break;
	    case 4: throwBluetoothStateException(env, "4"); break;
		case 5: throwBluetoothStateExceptionExt(env, "5[%s]", "str"); break;
		case 6: throwRuntimeException(env, "6"); break;
		case 7: throwBluetoothConnectionException(env, 1, "7"); break;
		case 8: throwBluetoothConnectionExceptionExt(env, 2, "8[%s]", "str"); break;

		case 22:
			// Throw Exception two times in a row. Second Exception ignored
			throwException(env, "java/lang/Exception", "22.1");
			throwIOException(env, "22.2");
			break;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testDebug
(JNIEnv *env, jclass, jstring message) {
	const char *c = env->GetStringUTFChars(message, 0);
	debugs("message[%s]", c);
	env->ReleaseStringUTFChars(message, c);
}