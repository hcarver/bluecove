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

#pragma once

#undef USE_WORKER_THREAD

#ifndef BLUECOVE_BUILD
#define BLUECOVE_BUILD 100
#endif

#ifndef BLUECOVE_VERSION
#define BLUECOVE_VERSION 20100
#endif

#ifdef _WIN32_WCE
#pragma comment(linker, "/nodefaultlib:libc.lib")
#pragma comment(linker, "/nodefaultlib:libcd.lib")

// NOTE - this value is not strongly correlated to the Windows CE OS version being targeted
#define WINVER _WIN32_WCE

#define STRSAFE_NO_DEPRECATE

#include <ceconfig.h>
#if defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)
#define SHELL_AYGSHELL
#endif // (WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)

#ifdef _CE_DCOM
#define _ATL_APARTMENT_THREADED
#endif // _CE_DCOM

//#ifdef SHELL_AYGSHELL
//#include <aygshell.h>
//#pragma comment(lib, "aygshell.lib")
//#endif // SHELL_AYGSHELL


// Windows Header Files:
#include <windows.h>

#if defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)
#ifndef _DEVICE_RESOLUTION_AWARE
#define _DEVICE_RESOLUTION_AWARE
#endif // _DEVICE_RESOLUTION_AWARE
#endif //(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)

#ifdef _DEVICE_RESOLUTION_AWARE
#include "DeviceResolutionAware.h"
#endif // _DEVICE_RESOLUTION_AWARE

#if _WIN32_WCE < 0x500 && ( defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP) )
	#pragma comment(lib, "ccrtrtti.lib")
	#ifdef _X86_
		#if defined(_DEBUG)
			#pragma comment(lib, "libcmtx86d.lib")
		#else
			#pragma comment(lib, "libcmtx86.lib")
		#endif
	#endif
#endif // _WIN32_WCE < 0x500 && ( defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP) )

#include <altcecrt.h>

#include <stdlib.h>
//swprintf_s on XP  _snwprintf on CE
#define swprintf_s _snwprintf
#define sprintf_s _snprintf
#define _vsnprintf_s _vsnprintf

#else // _WIN32_WCE

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers

#ifdef WIN32
// Windows Header Files:
#include <windows.h>
#include <tchar.h>

// #define CDC_DLL_VERSION

#ifdef CDC_DLL_VERSION
#define _JAVASOFT_JNI_MD_H_

#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __cdecl

typedef long jint;
typedef __int64 jlong;
typedef signed char jbyte;

#endif // CDC_DLL_VERSION

#else

// OS X
#include <unistd.h>
#include <CoreFoundation/CoreFoundation.h>
#include <Carbon/Carbon.h>

#define BOOL bool
//#define TRUE true
//#define FALSE false
#define DWORD unsigned int

#include <wchar.h>
#define WCHAR wchar_t

#define CRITICAL_SECTION MPCriticalRegionID

#define swprintf_s snwprintf
#define sprintf_s snprintf
#define _vsnprintf_s vsnprintf

#endif

// vc6 = 1200, vc7 = 1300, vc7.1 = 1310, vc8 = 1400

#ifdef WIN32
#if _MSC_VER > 1200
#include <strsafe.h>
#else
#define VC6
#define swprintf_s _snwprintf
#define sprintf_s _snprintf
#define _vsnprintf_s _vsnprintf
#ifndef INT_MAX
#define INT_MAX 2147483647
#endif
#endif

#endif

#endif // #else // _WIN32_WCE

// TODO: reference additional headers your program requires here
#include <stdlib.h>

#define INQUIRY_COMPLETED 0
#define INQUIRY_TERMINATED 5
#define INQUIRY_ERROR 7

#define SERVICE_SEARCH_COMPLETED 1
#define SERVICE_SEARCH_TERMINATED 2
#define SERVICE_SEARCH_ERROR 3
#define SERVICE_SEARCH_NO_RECORDS 4
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE 6

#define NOAUTHENTICATE_NOENCRYPT 0
#define AUTHENTICATE_NOENCRYPT 1
#define AUTHENTICATE_ENCRYPT 2

#define GIAC 0x9E8B33
#define LIAC 0x9E8B00

#define RETRIEVEDEVICES_OPTION_CACHED 0x00
#define RETRIEVEDEVICES_OPTION_PREKNOWN 0x01

#define DATA_ELEMENT_TYPE_NULL 0x0000
#define DATA_ELEMENT_TYPE_U_INT_1 0x0008
#define DATA_ELEMENT_TYPE_U_INT_2 0x0009
#define DATA_ELEMENT_TYPE_U_INT_4 0x000A
#define DATA_ELEMENT_TYPE_U_INT_8 0x000B
#define DATA_ELEMENT_TYPE_U_INT_16 0x000C
#define DATA_ELEMENT_TYPE_INT_1 0x0010
#define DATA_ELEMENT_TYPE_INT_2 0x0011
#define DATA_ELEMENT_TYPE_INT_4 0x0012
#define DATA_ELEMENT_TYPE_INT_8 0x0013
#define DATA_ELEMENT_TYPE_INT_16 0x0014
#define DATA_ELEMENT_TYPE_URL 0x0040
#define DATA_ELEMENT_TYPE_UUID 0x0018
#define DATA_ELEMENT_TYPE_BOOL 0x0028
#define DATA_ELEMENT_TYPE_STRING 0x0020
#define DATA_ELEMENT_TYPE_DATSEQ 0x0030
#define DATA_ELEMENT_TYPE_DATALT 0x0038

#include <jni.h>

// --- Debug

void enableNativeDebug(JNIEnv * env, jobject loggerClass, jboolean on);

#ifdef WIN32
#ifndef VC6
#define CPP_FILE __FILE__
#endif
#endif

extern BOOL nativeDebugCallbackEnabled;

#define STD_DEBUG
//#define EXT_DEBUG
BOOL isDebugOn();

#define DEBUG_MESSAGE_MAX 1024

class DebugMessage {
private:
    char msg[DEBUG_MESSAGE_MAX+1];
public:
    void printf(const char *fmt, ...);
	void callDebugListener(JNIEnv *env, const char* fileName, int lineN);
	void callDebugStdOut(const char* fileName, int lineN);
};

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, ...);
void callDebugStdOut(const char* fileName, int lineN, ...);

// This can be used in JNI functions. The message would be sent to java code. Usage example debug(("", args));
// To support VC6 we can't use  ... and __VA_ARGS__
#ifdef STD_DEBUG
#ifdef VC6
#define debug(args) {DebugMessage dm; dm.printf args; dm.callDebugListener(env, CPP_FILE, __LINE__);}
#define ndebug(args) {DebugMessage dm; dm.printf args; dm.callDebugStdOut(CPP_FILE, __LINE__);}
#else
#define debugVA(...) callDebugListener(env, CPP_FILE, __LINE__, __VA_ARGS__)
#define debug(...) debugVA __VA_ARGS__
#define ndebugVA(...) callDebugStdOut(CPP_FILE, __LINE__, __VA_ARGS__)
#define ndebug(...) ndebugVA __VA_ARGS__
#endif
#else
#define debug(args)
#define ndebug(args)
#endif

#ifdef EXT_DEBUG
#define Edebug(args) {DebugMessage dm; dm.printf args; dm.callDebugListener(env, CPP_FILE, __LINE__);}
#else
#define Edebug(args)
#endif

void log_info(const char *fmt, ...);

const char* bool2str(BOOL b);

// --- Error handling

extern const char* cServiceRegistrationException;

void throwException(JNIEnv *env, const char *name, const char *fmt, ...);
void throwRuntimeException(JNIEnv *env, const char *fmt, ...);
void throwIOException(JNIEnv *env, const char *fmt, ...);
void throwInterruptedIOException(JNIEnv *env, const char *fmt, ...);
void throwServiceRegistrationException(JNIEnv *env, const char *fmt, ...);
void throwBluetoothStateException(JNIEnv *env, const char *fmt, ...);

#define BT_CONNECTION_ERROR_UNKNOWN_PSM  1
#define BT_CONNECTION_ERROR_SECURITY_BLOCK 2
#define BT_CONNECTION_ERROR_NO_RESOURCES 3
#define BT_CONNECTION_ERROR_FAILED_NOINFO 4
#define BT_CONNECTION_ERROR_TIMEOUT 5
#define BT_CONNECTION_ERROR_UNACCEPTABLE_PARAMS 6

void throwBluetoothConnectionException(JNIEnv *env, int error, const char *fmt, ...);

#ifdef WIN32
void throwExceptionWinErrorMessage(JNIEnv *env, const char *name, const char *msg, DWORD last_error);

void throwIOExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error);

void throwBluetoothStateExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error);

void throwIOExceptionWinGetLastError(JNIEnv *env, const char *msg);

WCHAR* getWinErrorMessage(DWORD last_error);

const char* waitResultsString(DWORD rc);

#endif


BOOL ExceptionCheckCompatible(JNIEnv *env);

BOOL isCurrentThreadInterrupted(JNIEnv *env, jobject peer, const char* message);

#ifdef WIN32
void convertUUIDBytesToGUID(jbyte *bytes, GUID *uuid);
void convertGUIDToUUIDBytes(GUID *uuid, jbyte *bytes);
#endif

#define BLUECOVE_STACK_DETECT_MICROSOFT  1
#define BLUECOVE_STACK_DETECT_WIDCOMM    (1<<1)
#define BLUECOVE_STACK_DETECT_BLUESOLEIL (1<<2)
#define BLUECOVE_STACK_DETECT_TOSHIBA    (1<<3)
#define BLUECOVE_STACK_DETECT_OSX        (1<<4)

jint detectBluetoothStack(JNIEnv *env);
jint blueCoveVersion();
BOOL isMicrosoftBluetoothStackPresent(JNIEnv *env);
BOOL isMicrosoftBluetoothStackPresentVC6(JNIEnv *env);
BOOL isWIDCOMMBluetoothStackPresent(JNIEnv *env);
BOOL isBlueSoleilBluetoothStackPresent(JNIEnv *env);
BOOL isToshibaBluetoothStackPresent(JNIEnv *env);

#define cSTACK_CLOSED "Stack closed"
#define cOUT_OF_MEMORY "Out of memory"
#define cCONNECTION_CLOSED "Connection closed"
#define cCONNECTION_IS_CLOSED "Connection is closed"
#define cINQUIRY_RUNNING "Another inquiry already running"

jint getDeviceClassByOS(JNIEnv *env);
