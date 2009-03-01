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

#define MAGIC_1 0xBC1AA01
#define MAGIC_2 0xBC2BB02

#ifndef _WIN32_WCE
#define RECEIVE_BUFFER_MAX 0x40000
#endif //_WIN32_WCE
#define RECEIVE_BUFFER_MAX 0x8000
#else  //_WIN32_WCE

// This is extra precaution, may be unnecessary
#define RECEIVE_BUFFER_SAFE TRUE
/*
 * FIFO with no memory allocations in write, 256K size can be overflown but not with BT communication speed we tested.
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
	int write_buffer(void *p_data, int len);
public:
	ReceiveBuffer();
	ReceiveBuffer(int size);
	~ReceiveBuffer();

    void reset();
	int write(void *p_data, int len);
	int write_with_len(void *p_data, int len);
	int sizeof_len();
	int readByte();
	int read_len(int* len);
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
	int handleBatch;
	int handleReturned;

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

	BOOL hasObject(PoolableObject* obj);
};

class DeviceInquiryCallback {
private:
    jobject inquiryRunnable;
    jmethodID deviceDiscoveredCallbackMethod;

    jobject startedNotify;
    jmethodID startedNotifyNotifyMethod;

public:
    DeviceInquiryCallback();
    BOOL builDeviceInquiryCallbacks(JNIEnv * env, jobject inquiryRunnable, jobject startedNotify);
    BOOL callDeviceInquiryStartedCallback(JNIEnv * env);
    BOOL callDeviceDiscovered(JNIEnv * env, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired);
};

class RetrieveDevicesCallback {
private:
    jobject listener;
    jmethodID deviceFoundCallbackMethod;
public:
    RetrieveDevicesCallback();
    BOOL builCallback(JNIEnv * env, jobject peer, jobject listener);
    BOOL callDeviceFoundCallback(JNIEnv * env, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired);
};

