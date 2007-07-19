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

#pragma once

#ifndef BLUECOVE_VERSION
#define BLUECOVE_VERSION 20001
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
// Windows Header Files:
#include <windows.h>
#include <tchar.h>

// vc6 = 1200, vc7 = 1300, vc7.1 = 1310, vc8 = 1400

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

#include <jni.h>

void enableNativeDebug(JNIEnv * env, jobject loggerClass, jboolean on);

#ifndef VC6
#define CPP_FILE __FILE__
#endif

//#define EXT_DEBUG
void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...);
BOOL isDebugOn();
#define debug(fmt) callDebugListener(env, CPP_FILE, __LINE__, fmt);
#define debugs(fmt, message) callDebugListener(env, CPP_FILE, __LINE__, fmt, message);
#define debug1(fmt, message) callDebugListener(env, CPP_FILE, __LINE__, fmt, message);
#define debugss(fmt, message1, message2) callDebugListener(env, CPP_FILE, __LINE__, fmt, message1, message2);
#define debug2(fmt, message1, message2) callDebugListener(env, CPP_FILE, __LINE__, fmt, message1, message2);
#define debug3(fmt, message1, message2, message3) callDebugListener(env, CPP_FILE, __LINE__, fmt, message1, message2, message3);
#define debug4(fmt, message1, message2, message3, message4) callDebugListener(env, CPP_FILE, __LINE__, fmt, message1, message2, message3, message4);

#ifdef EXT_DEBUG
#define Edebug(fmt)  debug(fmt)
#define Edebugs(fmt, message) debugs(fmt, message)
#define Edebugss(fmt, message1, message2) debugss(fmt, message1, message2)
#define Edebug1(fmt, message) debugs(fmt, message)
#define Edebug2(fmt, message1, message2) debugss(fmt, message1, message2)
#else
#define Edebug(fmt)
#define Edebugs(fmt, message)
#define Edebugss(fmt, message1, message2)
#define Edebug1(fmt, message)
#define Edebug2(fmt, message1, message2)
#endif

char* bool2str(BOOL b);

void throwException(JNIEnv *env, const char *name, const char *msg);

void throwExceptionExt(JNIEnv *env, const char *name, const char *fmt, ...);

void throwIOException(JNIEnv *env, const char *msg);
#define _throwIOException(env, msg) { callDebugListener(env, CPP_FILE, __LINE__, "throw"); throwIOException(env, msg); }

void throwIOExceptionExt(JNIEnv *env, const char *fmt, ...);

void throwBluetoothStateException(JNIEnv *env, const char *msg);
#define _throwBluetoothStateException(env, msg) { callDebugListener(env, CPP_FILE, __LINE__, "throw"); throwBluetoothStateException(env, msg); }

void throwBluetoothStateExceptionExt(JNIEnv *env, const char *fmt, ...);


void throwRuntimeException(JNIEnv *env, const char *msg);
#define _throwRuntimeException(env, msg) { callDebugListener(env, CPP_FILE, __LINE__, "throw"); throwRuntimeException(env, msg); }

void throwExceptionWinErrorMessage(JNIEnv *env, const char *name, const char *msg, DWORD last_error);

void throwIOExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error);

void throwBluetoothStateExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error);

void throwIOExceptionWinGetLastError(JNIEnv *env, const char *msg);

WCHAR *getWinErrorMessage(DWORD last_error);

char* waitResultsString(DWORD rc);

BOOL ExceptionCheckCompatible(JNIEnv *env);

void convertUUIDBytesToGUID(jbyte *bytes, GUID *uuid);
void convertGUIDToUUIDBytes(GUID *uuid, jbyte *bytes);

jint detectBluetoothStack(JNIEnv *env);
jint blueCoveVersion();
BOOL isMicrosoftBluetoothStackPresent(JNIEnv *env);
BOOL isWIDCOMMBluetoothStackPresent(JNIEnv *env);
BOOL isBlueSoleilBluetoothStackPresent(JNIEnv *env);

jint getDeviceClassByOS(JNIEnv *env);

#define MAGIC_1 0xBC1AA01
#define MAGIC_2 0xBC2BB02

#define RECEIVE_BUFFER_MAX 0x10000
// This is extra precaution, may be unnecessary
#define RECEIVE_BUFFER_SAFE TRUE
/*
* FIFO with no memory allocations in write, can be overflown but not with BT communication speed.
*/
class ReceiveBuffer {
private:
	BOOL safe;
	CRITICAL_SECTION lock;

	int size;

	long magic1b;
	long magic2b;
	jbyte buffer[RECEIVE_BUFFER_MAX];
	long magic1e;
	long magic2e;

	BOOL overflown;
	BOOL full;
	int rcv_idx;
	int read_idx;

	void incReadIdx(int count);
public:
	ReceiveBuffer();
	ReceiveBuffer(int size);
	~ReceiveBuffer();

    void reset();
	int write(void *p_data, int len);
	int readByte();
	int read(void *p_data, int len);
	int skip(int n);
	BOOL isOverflown();
	void setOverflown();
	int available();
	BOOL isCorrupted();
};

//#define SAFE_OBJECT_DESTRUCTION

class PoolableObject {
public:
	long magic1;
	long magic2;

	BOOL readyToFree;
	int internalHandle;
	char poolableObjectType;

	long usedCount;

	PoolableObject();
	virtual ~PoolableObject();

	// Used to enable safe object destruction, dellay destructor untill all threads entered are exited from Wait.
	void tInc();
	void tDec();

	virtual BOOL isValidObject();
	virtual BOOL isExternalHandle(jlong handle);
};

class ObjectPool {
private:
	CRITICAL_SECTION lock;

	int size;

	//each Handle type is different positive value range.
	int handleOffset;

	BOOL delayDelete;

	// generate different handlers for each new object
	int handleMove;

	PoolableObject** objs;

	jlong realIndex(jlong internalHandle);
	jlong realIndex(PoolableObject* obj);

public:

	ObjectPool(int size, int handleOffset, BOOL delayDelete);
	~ObjectPool();

	PoolableObject* getObject(JNIEnv *env, jlong handle);
	PoolableObject* getObject(JNIEnv *env, jlong handle, char poolableObjectType);

	PoolableObject* getObjectByExternalHandle(jlong handle);

	void removeObject(PoolableObject* obj);

	BOOL addObject(PoolableObject* obj);
	BOOL addObject(PoolableObject* obj, char poolableObjectType);
};