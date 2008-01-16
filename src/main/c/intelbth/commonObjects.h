/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

	BOOL hasObject(PoolableObject* obj);
};

class DeviceInquiryCallback {
private:
    jobject peer;
    jmethodID deviceDiscoveredCallbackMethod;

    jobject startedNotify;
    jmethodID startedNotifyNotifyMethod;

public:
    DeviceInquiryCallback();
    BOOL builDeviceInquiryCallbacks(JNIEnv * env, jobject peer, jobject startedNotify);
    BOOL callDeviceInquiryStartedCallback(JNIEnv * env);
    BOOL callDeviceDiscovered(JNIEnv * env, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired);
};


