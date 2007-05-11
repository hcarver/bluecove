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

#include "stdafx.h"

static BOOL nativeDebugCallback= false;
static jclass nativeDebugListenerClass;
static jmethodID nativeDebugMethod = NULL;

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_enableNativeDebug(JNIEnv *env, jobject peer, jboolean on) {
	if (on) {
		nativeDebugListenerClass = (jclass)env->NewGlobalRef(env->GetObjectClass(peer));
		if (nativeDebugListenerClass != NULL) {
			nativeDebugMethod = env->GetStaticMethodID(nativeDebugListenerClass, "nativeDebugCallback", "(ILjava/lang/String;)V");
			if (nativeDebugMethod != NULL) {
				nativeDebugCallback = true;
				debug("nativeDebugCallback ON");
			}
		}
	} else {
		nativeDebugCallback = false;
	}
}

void callDebugListener(JNIEnv *env, int lineN, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	{
		if (nativeDebugCallback) {
			char msg[1064];
			_vsnprintf_s(msg, 1064, fmt, ap);
			env->CallStaticVoidMethod(nativeDebugListenerClass, nativeDebugMethod, lineN, env->NewStringUTF(msg));
		}
	}
	va_end(ap);
}

void throwException(JNIEnv *env, const char *name, const char *msg)
{
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

void throwIOException(JNIEnv *env, const char *msg)
{
	throwException(env, "java/io/IOException", msg);
}

WCHAR *GetWSAErrorMessage(DWORD last_error)
{
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

void throwExceptionWSAErrorMessage(JNIEnv *env, const char *name, const char *msg, DWORD last_error)
{
	char errmsg[1064];
	sprintf_s(errmsg, 1064, "%s [%d] %S", msg, last_error, GetWSAErrorMessage(last_error));
	throwException(env, name, errmsg);
}

void throwIOExceptionWSAErrorMessage(JNIEnv *env, const char *msg, DWORD last_error)
{
	throwExceptionWSAErrorMessage(env, "java/io/IOException", msg, last_error);
}

void throwIOExceptionWSAGetLastError(JNIEnv *env, const char *msg)
{
	throwIOExceptionWSAErrorMessage(env, msg, WSAGetLastError());
}

BOOL ExceptionCheckCompatible(JNIEnv *env) {
	if (env->GetVersion() > JNI_VERSION_1_1) {
		return env->ExceptionCheck();
	} else {
		return (env->ExceptionOccurred() != NULL);
	}
}
