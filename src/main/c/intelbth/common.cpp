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

#include "com_intel_bluetooth_BlueCoveNativeCommon.h"

static BOOL nativeDebugCallback= false;
static jclass nativeDebugListenerClass;
static jmethodID nativeDebugMethod = NULL;

__declspec(dllexport) jint blueCoveVersion() {
	return 02 * 10000 + 0 * 100 + 0;
}

__declspec(dllexport) jint blueCoveVersion_2_0_0() {
	return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BlueCoveNativeCommon_getLibraryVersion
(JNIEnv *, jobject) {
	return blueCoveVersion();
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BlueCoveNativeCommon_enableNativeDebug
(JNIEnv * env, jobject common, jboolean on) {
	if (on) {
		nativeDebugListenerClass = (jclass)env->NewGlobalRef(env->GetObjectClass(common));
		if (nativeDebugListenerClass != NULL) {
			nativeDebugMethod = env->GetStaticMethodID(nativeDebugListenerClass, "nativeDebugCallback", "(Ljava/lang/String;ILjava/lang/String;)V");
			if (nativeDebugMethod != NULL) {
				nativeDebugCallback = true;
				debug("nativeDebugCallback ON");
			}
		}
	} else {
		nativeDebugCallback = false;
	}
}

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	{
		if (nativeDebugCallback) {
			char msg[1064];
			_vsnprintf_s(msg, 1064, fmt, ap);
			env->CallStaticVoidMethod(nativeDebugListenerClass, nativeDebugMethod, env->NewStringUTF(fileName), lineN, env->NewStringUTF(msg));
		}
	}
	va_end(ap);
}

void throwException(JNIEnv *env, const char *name, const char *msg) {
	 //debugss("Throw Exception %s %s", name, msg);
	 jclass cls = env->FindClass(name);
     /* if cls is NULL, an exception has already been thrown */
     if (cls != NULL) {
         env->ThrowNew(cls, msg);
	 } else {
		 env->FatalError("Illegal Exception name");
	 }
     /* free the local ref */
    env->DeleteLocalRef(cls);
}

void throwExceptionExt(JNIEnv *env, const char *name, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	{
		char msg[1064];
		_vsnprintf_s(msg, 1064, fmt, ap);
		throwException(env, name, msg);
	}
	va_end(ap);
}

void throwIOException(JNIEnv *env, const char *msg) {
	throwException(env, "java/io/IOException", msg);
}

void throwIOExceptionExt(JNIEnv *env, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	{
		char msg[1064];
		_vsnprintf_s(msg, 1064, fmt, ap);
		throwIOException(env, msg);
	}
	va_end(ap);
}

void throwRuntimeException(JNIEnv *env, const char *msg) {
	throwException(env, "java/lang/RuntimeException", msg);
}

WCHAR *getWinErrorMessage(DWORD last_error) {
	static WCHAR errmsg[1024];
	if (!FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM,
		0,
		last_error,
		0,
		errmsg,
		511,
		NULL))
	{
		swprintf_s(errmsg, 1024, _T("No error message for code %d"), last_error);
		return errmsg;
	}
	size_t last = wcslen(errmsg) - 1;
	while ((errmsg[last] == '\n') || (errmsg[last] == '\r')) {
		errmsg[last] = 0;
		last --;
	}
	return errmsg;
}

void throwExceptionWinErrorMessage(JNIEnv *env, const char *name, const char *msg, DWORD last_error) {
	char errmsg[1064];
	sprintf_s(errmsg, 1064, "%s [%d] %S", msg, last_error, getWinErrorMessage(last_error));
	throwException(env, name, errmsg);
}

void throwIOExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error) {
	throwExceptionWinErrorMessage(env, "java/io/IOException", msg, last_error);
}

void throwIOExceptionWinGetLastError(JNIEnv *env, const char *msg) {
	throwIOExceptionWinErrorMessage(env, msg, GetLastError());
}


BOOL ExceptionCheckCompatible(JNIEnv *env) {
	if (env->GetVersion() > JNI_VERSION_1_1) {
		return env->ExceptionCheck();
	} else {
		return (env->ExceptionOccurred() != NULL);
	}
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BlueCoveNativeCommon_detectBluetoothStack
(JNIEnv *env, jobject) {
	jint rc = 0;
	if (isMicrosoftBluetoothStackPresent()) {
		rc += 1;
	}
	if (isWIDCOMMBluetoothStackPresent()) {
		rc += 2;
	}
	if (isBlueSoleilBluetoothStackPresent()) {
		rc += 4;
	}
	return rc;
}

void convertUUIDBytesToGUID(jbyte *bytes, GUID *uuid) {
	uuid->Data1 = bytes[0]<<24&0xff000000|bytes[1]<<16&0x00ff0000|bytes[2]<<8&0x0000ff00|bytes[3]&0x000000ff;
	uuid->Data2 = bytes[4]<<8&0xff00|bytes[5]&0x00ff;
	uuid->Data3 = bytes[6]<<8&0xff00|bytes[7]&0x00ff;

	for(int i = 0; i < 8; i++) {
		uuid->Data4[i] = bytes[i + 8];
	}
}

void convertGUIDToUUIDBytes(GUID *uuid, jbyte *bytes) {
	bytes[0] = (jbyte)((uuid->Data1>>24) & 0x00ff);
	bytes[1] = (jbyte)((uuid->Data1>>16) & 0x00ff);
	bytes[2] = (jbyte)((uuid->Data1>>8) & 0x00ff);
	bytes[3] = (jbyte)((uuid->Data1) & 0x00ff);
	bytes[4] = (jbyte)((uuid->Data2>>8) & 0x00ff);
	bytes[5] = (jbyte)((uuid->Data2 & 0x00ff));
	bytes[6] = (jbyte)((uuid->Data3>>8) & 0x00ff);
	bytes[7] = (jbyte)((uuid->Data3 & 0x00ff));
	for(int i = 0; i < 8; i++) {
		bytes[i + 8] = uuid->Data4[i];
	}
}