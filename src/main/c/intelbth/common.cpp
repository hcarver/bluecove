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
#define CPP_FILE "common.cpp"
#endif

static BOOL nativeDebugCallback= false;
static jclass nativeDebugListenerClass;
static jmethodID nativeDebugMethod = NULL;

static const char* cIOException = "java/io/IOException";
static const char* cInterruptedIOException = "java/io/InterruptedIOException";
static const char* cBluetoothStateException = "javax/bluetooth/BluetoothStateException";
static const char* cBluetoothConnectionException = "javax/bluetooth/BluetoothConnectionException";

jint blueCoveVersion() {
	return BLUECOVE_VERSION;
}

BOOL isDebugOn() {
    return nativeDebugCallback;
}

void enableNativeDebug(JNIEnv *env, jobject loggerClass, jboolean on) {
	if (on) {
		if (nativeDebugCallback) {
			return;
		}
		nativeDebugListenerClass = (jclass)env->NewGlobalRef(loggerClass);
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
		if ((env != NULL) && (nativeDebugCallback)) {
			char msg[1064];
			_vsnprintf_s(msg, 1064, fmt, ap);
			env->CallStaticVoidMethod(nativeDebugListenerClass, nativeDebugMethod, env->NewStringUTF(fileName), lineN, env->NewStringUTF(msg));
		}
	}
	va_end(ap);
}

char* bool2str(BOOL b) {
	if (b == false)  {
		return "FALSE";
	} else {
		return "TRUE";
	}
}

void throwException(JNIEnv *env, const char *name, const char *msg) {
	if (env == NULL) {
		return;
	}
	if (ExceptionCheckCompatible(env)) {
		debugss("ERROR: can't throw second exception %s(%s)", name, msg);
		return;
	}
	jclass cls = env->FindClass(name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        env->ThrowNew(cls, msg);
	} else {
	    debug1("Can't find Exception %s", name);
		env->FatalError(name);
	}
    /* free the local ref */
    env->DeleteLocalRef(cls);
}

void _vthrowExceptionExt(JNIEnv *env, const char *name, const char *fmt, va_list ap) {
	char msg[1064];
	_vsnprintf_s(msg, 1064, fmt, ap);
	throwException(env, name, msg);
}

void throwExceptionExt(JNIEnv *env, const char *name, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	_vthrowExceptionExt(env, name, fmt, ap);
	va_end(ap);
}

void throwIOException(JNIEnv *env, const char *msg) {
	throwException(env, cIOException, msg);
}

void throwIOExceptionExt(JNIEnv *env, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	_vthrowExceptionExt(env, cIOException, fmt, ap);
	va_end(ap);
}

void throwInterruptedIOException(JNIEnv *env, const char *msg) {
	throwException(env, cInterruptedIOException, msg);
}

void throwBluetoothStateException(JNIEnv *env, const char *msg) {
	throwException(env, cBluetoothStateException, msg);
}

void throwBluetoothStateExceptionExt(JNIEnv *env, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	_vthrowExceptionExt(env, cBluetoothStateException, fmt, ap);
	va_end(ap);
}

void throwBluetoothConnectionException(JNIEnv *env, int error, const char *msg) {
	if (env == NULL) {
		return;
	}
	if (ExceptionCheckCompatible(env)) {
		debugss("ERROR: can't throw second exception %s(%s)", cBluetoothConnectionException, msg);
		return;
	}
	jclass cls = env->FindClass(cBluetoothConnectionException);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
		jmethodID methodID = env->GetMethodID(cls, "<init>", "(ILjava/lang/String;)V");
		if (methodID == NULL) {
			env->FatalError("Fail to get constructor for Exception");
		} else {
			jstring excMessage = env->NewStringUTF(msg);
			jthrowable obj = (jthrowable)env->NewObject(cls, methodID, error, excMessage);
			if (obj != NULL) {
				env->Throw(obj);
			} else {
				env->FatalError("Fail to create new Exception");
			}
		}
	} else {
		env->FatalError(cBluetoothConnectionException);
	}
    /* free the local ref */
    env->DeleteLocalRef(cls);
}

void throwBluetoothConnectionExceptionExt(JNIEnv *env, int error, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	char msg[1064];
	_vsnprintf_s(msg, 1064, fmt, ap);
	throwBluetoothConnectionException(env, error, msg);
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
		swprintf_s(errmsg, 1024, L"No error message for code %d", last_error);
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
	sprintf_s(errmsg, 1064, "%s; [%d] %S", msg, last_error, getWinErrorMessage(last_error));
	throwException(env, name, errmsg);
}

void throwIOExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error) {
	throwExceptionWinErrorMessage(env, cIOException, msg, last_error);
}

void throwBluetoothStateExceptionWinErrorMessage(JNIEnv *env, const char *msg, DWORD last_error) {
	throwExceptionWinErrorMessage(env, cBluetoothStateException, msg, last_error);
}

void throwIOExceptionWinGetLastError(JNIEnv *env, const char *msg) {
	throwIOExceptionWinErrorMessage(env, msg, GetLastError());
}

char* waitResultsString(DWORD rc) {
	switch (rc) {
		case WAIT_FAILED: return "WAIT_FAILED";
		case WAIT_TIMEOUT: return "WAIT_TIMEOUT";
		case WAIT_OBJECT_0: return "WAIT_OBJECT_0";
		case WAIT_OBJECT_0 + 1: return "WAIT_OBJECT_1";
 	    case WAIT_OBJECT_0 + 2: return "WAIT_OBJECT_2";
		case WAIT_ABANDONED_0: return "WAIT_ABANDONED_0";
		case WAIT_ABANDONED_0 + 1: return "WAIT_ABANDONED_1";
		case WAIT_ABANDONED_0 + 2: return "WAIT_ABANDONED_2";
		default : return "Unknown";
	}
}

BOOL ExceptionCheckCompatible(JNIEnv *env) {
	if (env->GetVersion() > JNI_VERSION_1_1) {
		return env->ExceptionCheck();
	} else {
		return (env->ExceptionOccurred() != NULL);
	}
}

BOOL isCurrentThreadInterrupted(JNIEnv *env, jobject peer) {
	jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return TRUE;
	}
	jmethodID aMethod = env->GetMethodID(peerClass, "isCurrentThreadInterruptedCallback", "()Z");
	if (aMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID isCurrentThreadInterruptedCallback");
		return TRUE;
	}
	if (env->CallBooleanMethod(peer, aMethod)) {
		throwException(env, cInterruptedIOException, "thread interrupted");
		return TRUE;
	}
	return ExceptionCheckCompatible(env);
}

jint detectBluetoothStack(JNIEnv *env) {
	jint rc = 0;
#ifndef VC6
	if (isMicrosoftBluetoothStackPresent(env)) {
		rc += 1;
	}
#endif
	if (isWIDCOMMBluetoothStackPresent(env)) {
		rc += 2;
	}
	if (isBlueSoleilBluetoothStackPresent(env)) {
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

#define MAJOR_COMPUTER 0x0100
#define MAJOR_PHONE 0x0200
#define COMPUTER_MINOR_HANDHELD 0x10
#define PHONE_MINOR_SMARTPHONE 0x0c

jint getDeviceClassByOS(JNIEnv *env) {
#ifndef _WIN32_WCE
	return MAJOR_COMPUTER;
#else
	OSVERSIONINFO osvi;
	TCHAR szPlatform[MAX_PATH];

	BOOL rb;

	osvi.dwOSVersionInfoSize = sizeof(osvi);
	rb = GetVersionEx(&osvi);
	if (rb == FALSE) {
		return MAJOR_COMPUTER;
	}
	switch (osvi.dwPlatformId) {
    // A Windows CE platform.
    case VER_PLATFORM_WIN32_CE:
        // Get platform string.
        rb = SystemParametersInfo(SPI_GETPLATFORMTYPE, MAX_PATH, szPlatform, 0);
        if (rb == FALSE)  // SystemParametersInfo failed.
        {
			return MAJOR_COMPUTER & COMPUTER_MINOR_HANDHELD;
		}
		debugs("PLATFORMTYPE %S", szPlatform);
		if (0 == lstrcmpi(szPlatform, TEXT("Smartphone")))  {
			return MAJOR_PHONE | PHONE_MINOR_SMARTPHONE;
		}
		if (0 == lstrcmpi(szPlatform, TEXT("PocketPC"))) {
			return MAJOR_COMPUTER | COMPUTER_MINOR_HANDHELD;
		}
	}
	return MAJOR_COMPUTER;
#endif
}

ReceiveBuffer::ReceiveBuffer() {
	safe = RECEIVE_BUFFER_SAFE;
	if (safe) InitializeCriticalSection(&lock);
	this->size = RECEIVE_BUFFER_MAX;
	reset();
}

ReceiveBuffer::ReceiveBuffer(int size) {
	safe = RECEIVE_BUFFER_SAFE;
	if (safe) InitializeCriticalSection(&lock);
	this->size = size;
	if (this->size > RECEIVE_BUFFER_MAX) {
		this->size = RECEIVE_BUFFER_MAX;
	}
	reset();
}

ReceiveBuffer::~ReceiveBuffer() {
	if (safe) DeleteCriticalSection(&lock);
}

void ReceiveBuffer::reset() {
	rcv_idx = 0;
	read_idx = 0;
	overflown = FALSE;
	full = FALSE;
	magic1b = MAGIC_1;
	magic2b = MAGIC_2;
	magic1e = MAGIC_1;
	magic2e = MAGIC_2;
}

BOOL ReceiveBuffer::isCorrupted() {
	return ((magic1b != MAGIC_1) || (magic2b != MAGIC_2) || (magic1e != MAGIC_1) || (magic2e != MAGIC_2));
}

BOOL ReceiveBuffer::isOverflown() {
	return overflown && (available() == 0);
}

void ReceiveBuffer::setOverflown() {
	overflown = TRUE;
}

int ReceiveBuffer::write(void *p_data, int len) {
	if (overflown) {
		return 0;
	}
	if (safe) EnterCriticalSection(&lock);
	int accept;
	int _read_idx = read_idx;

	if ((_read_idx == rcv_idx) && full) {
		accept = 0;
	} else if (_read_idx <= rcv_idx) {
		accept = size - rcv_idx + _read_idx;
	} else {
		accept = _read_idx - rcv_idx;
	}

	if (accept > len) {
		accept = len;
	} else if (accept < len) {
		overflown = TRUE;
	}

	if (accept != 0) {
		if (rcv_idx + accept <= size) {
			memcpy((buffer + rcv_idx), p_data, accept);
			int new_rcv_idx = rcv_idx + accept;
			if (new_rcv_idx >= size) {
				new_rcv_idx = 0;
			}
			rcv_idx = new_rcv_idx;
		} else {
			// Read first part till the end of the buffer.
			int accept_fill_end_size = size - rcv_idx;
			memcpy((buffer + rcv_idx), p_data, accept_fill_end_size);
			// Read second part at the beginning of the buffer.
			int accept_fill_begin_size = accept - accept_fill_end_size;
			memcpy(buffer, ((jbyte*)p_data + accept_fill_end_size), accept_fill_begin_size);
			rcv_idx = accept_fill_begin_size;
		}

		if (read_idx == rcv_idx) {
			full = TRUE;
		}
	}
	if (safe) LeaveCriticalSection(&lock);
	return accept;
}

void ReceiveBuffer::incReadIdx(int count) {
	int next_read_idx = read_idx + count;
	if (next_read_idx >= size) {
		next_read_idx -= size;
	}
	read_idx = next_read_idx;
	full = FALSE;
}

int ReceiveBuffer::readByte() {
	if (available() == 0) {
		return -1;
	}
	if (safe) EnterCriticalSection(&lock);
	jint result = (unsigned char)buffer[read_idx];
	incReadIdx(1);
	if (safe) LeaveCriticalSection(&lock);
	return result;
}

int ReceiveBuffer::read(void *p_data, int len) {
	int count = available();
	if (count == 0) {
		return 0;
	}
	if (safe) EnterCriticalSection(&lock);
	if (count > len) {
		count = len;
	}
	if (read_idx + count < size) {
		if (p_data != NULL) {
			memcpy(p_data, (buffer + read_idx), count);
		}
	} else {
		// Read first part from the end of the buffer.
		int accept_fill_end_size = size - read_idx;
		if (p_data != NULL) {
			memcpy(p_data, (buffer + read_idx), accept_fill_end_size);
		}
		// Read second part from the beginning of the buffer.
		int accept_fill_begin_size = count - accept_fill_end_size;
		if (p_data != NULL) {
			memcpy((jbyte*)p_data + accept_fill_end_size, buffer, accept_fill_begin_size);
		}
	}
	incReadIdx(count);
	if (safe) LeaveCriticalSection(&lock);
	return count;
}

int ReceiveBuffer::skip(int n) {
	return read(NULL, n);
}

int ReceiveBuffer::available() {
	if (safe) EnterCriticalSection(&lock);
	int rc;
	int _rcv_idx = rcv_idx;
	int _read_idx = read_idx;
	if ((_read_idx == _rcv_idx) && full) {
		rc = size;
	} else if (_read_idx <= _rcv_idx) {
		rc = (_rcv_idx - _read_idx);
	} else {
		rc = (_rcv_idx + (size - _read_idx));
	}
	if (safe) LeaveCriticalSection(&lock);
	return rc;
}

// --------- ObjectPool -------------

PoolableObject::PoolableObject() {
	magic1 = MAGIC_1;
	magic2 = MAGIC_2;
	internalHandle = -1;
	usedCount = 0;
	readyToFree = FALSE;
}

PoolableObject::~PoolableObject() {
	magic1 = 0;
	magic2 = 0;
#ifdef SAFE_OBJECT_DESTRUCTION
	// Do not allow to free the object until thre are no functions in wait using it. e.g. read and write
	while (usedCount > 0) {
		Sleep(50);
	}
#endif
}

void PoolableObject::tInc() {
#ifdef SAFE_OBJECT_DESTRUCTION
	InterlockedIncrement(&usedCount);
#endif
}

void PoolableObject::tDec() {
#ifdef SAFE_OBJECT_DESTRUCTION
	InterlockedDecrement(&usedCount);
#endif
}

BOOL PoolableObject::isValidObject() {
	return TRUE;
}

BOOL PoolableObject::isExternalHandle(jlong handle) {
	return FALSE;
}

ObjectPool::ObjectPool(int size, int handleOffset, BOOL delayDelete) {
	InitializeCriticalSection(&lock);
	this->size = size;
	this->handleOffset = handleOffset;
	this->delayDelete = delayDelete;
	handleMove = 0;
	objs = new (PoolableObject* [size]);
	for(int i = 0; i < size; i ++) {
		objs[i] = NULL;
	}
}

ObjectPool::~ObjectPool() {
	for(int i = 0; i < size; i ++) {
		if (objs[i] != NULL) {
			PoolableObject* o = objs[i];
			objs[i] = NULL;
			delete o;
		}
	}
	delete objs;
	DeleteCriticalSection(&lock);
}

jlong ObjectPool::realIndex(jlong internalHandle) {
	return internalHandle - handleOffset;
}

jlong ObjectPool::realIndex(PoolableObject* obj) {
	return realIndex(obj->internalHandle);
}

BOOL ObjectPool::addObject(PoolableObject* obj) {
	EnterCriticalSection(&lock);
	int freeIndex = -1;
	for(int k = 0; k < size; k++) {
		int i = k + handleMove;
		if (i >= size) {
			i -= size;
		}

		if (delayDelete && (objs[i] != NULL)) {
			if (objs[i]->readyToFree) {
				delete objs[i];
				objs[i] = NULL;
			}
		}

		if (objs[i] == NULL) {
			freeIndex = i;
			objs[freeIndex] = obj;
			obj->internalHandle = handleOffset + freeIndex;

			handleMove ++;
			if (handleMove >= size) {
				handleMove = 0;
			}

			LeaveCriticalSection(&lock);
			return TRUE;
		}
	}
	LeaveCriticalSection(&lock);
	return FALSE;
}

BOOL ObjectPool::addObject(PoolableObject* obj, char poolableObjectType) {
	obj->poolableObjectType = poolableObjectType;
	return addObject(obj);
}

PoolableObject* ObjectPool::getObject(JNIEnv *env, jlong handle) {
	if (handle <= 0) {
		_throwIOException(env, "Invalid handle");
		return NULL;
	}
	jlong idx = realIndex(handle);
	if ((idx < 0) || (idx >= size)) {
		_throwIOException(env, "Obsolete handle");
		return NULL;
	}
	PoolableObject* o = objs[idx];
	if (o == NULL) {
		_throwIOException(env, "Destroyed handle");
		return NULL;
	}
	if ((o->magic1 != MAGIC_1) || (o->magic2 != MAGIC_2)) {
		_throwIOException(env, "Corrupted object");
		return NULL;
	}
	if ((o->internalHandle != handle) || (!o->isValidObject())) {
		_throwIOException(env, "Corrupted handle");
		return NULL;
	}
	return o;
}

PoolableObject* ObjectPool::getObject(JNIEnv *env, jlong handle, char poolableObjectType) {
	PoolableObject* o = getObject(env, handle);
	if ((o != NULL) && (o->poolableObjectType != poolableObjectType)) {
		_throwIOException(env, "Invalid handle type");
		return NULL;
	}
	return o;
}

PoolableObject* ObjectPool::getObjectByExternalHandle(jlong handle) {
	for(int i = 0; i < size; i ++) {
		if (objs[i] != NULL) {
			PoolableObject* o = objs[i];
			if (o->isExternalHandle(handle)) {
				return o;
			}
		}
	}
	return NULL;
}

void ObjectPool::removeObject(PoolableObject* obj) {
	jlong idx = realIndex(obj);
	if ((idx >= 0) && (idx < size)) {
		objs[idx] = NULL;
	}
}
