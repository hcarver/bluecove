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

#define BLUESOLEIL_DLL L"btfunc.dll"

#ifdef _BLUESOLEIL

// Should be installed to %ProgramFiles%\IVT Corporation\BlueSoleil\api
#pragma comment(lib, "btfunc.lib")
#include "bt_ui.h"
#include "com_intel_bluetooth_BluetoothStackBlueSoleil.h"

// We specify which DLLs to delay load with the /delayload:btfunc.dll linker option
#ifdef VC6
#pragma comment(lib, "DelayImp.lib")
#pragma comment(linker, "/delayload:btfunc.dll")
#endif

#define DEVICE_RESPONDED_MAX 50
#define SERVICE_COUNT_MAX	100

// 7 for Server and 7 for Client, Bluetooth Can't have more than 7?
#define COMMPORTS_POOL_MAX 14
#define SERVERS_POOL_MAX 32

class BlueSoleilCOMPort;

class BlueSoleilSPPExService;

class BlueSoleilStack {
public:
	BOOL inquiringDevice;

	CRITICAL_SECTION openingPortLock;

	ObjectPool* commPortsPool;
	ObjectPool* servicesPool;

	BlueSoleilStack();
	~BlueSoleilStack();

	void SPPEXConnectionCallback(DWORD dwServerHandle, BYTE* lpBdAddr, UCHAR ucStatus, DWORD dwConnetionHandle);

	BlueSoleilCOMPort* createCommPort();
	BlueSoleilCOMPort* getCommPort(JNIEnv *env, jlong handle);
	void deleteCommPort(BlueSoleilCOMPort* commPort);

	BlueSoleilSPPExService* createService();
	BlueSoleilSPPExService* getService(JNIEnv *env, jlong handle);
	void deleteService(BlueSoleilSPPExService* service);
};

class BlueSoleilCOMPort : public PoolableObject {
public:
	HANDLE hComPort;
	DWORD dwConnectionHandle;

	jlong remoteAddress;

	BOOL isClosing;
	HANDLE hCloseEvent;
	BOOL receivedEOF;

	OVERLAPPED ovlComState;
	OVERLAPPED ovlRead;
	OVERLAPPED ovlWrite;

	long portMagic1;

	COMSTAT comStat;
	DWORD   dwErrorFlags;

	BlueSoleilCOMPort();
	virtual ~BlueSoleilCOMPort();

	BOOL openComPort(JNIEnv *env, int portN);
	char* configureComPort(JNIEnv *env);

	void clearCommError();

	void close(JNIEnv *env);

	virtual BOOL isValidObject();
};


class BlueSoleilSPPExService : public PoolableObject {
private:
	long serviceMagic1;
public:
	DWORD wdServerHandle;
	SPPEX_SERVICE_INFO serviceInfo;

	BOOL isClosing;
	HANDLE hCloseEvent;

	BOOL isConnected;
	HANDLE hConnectionEvent;
	jlong connectedBdAddr;
	DWORD dwConnectedConnetionHandle;

	int portHandle;

	BlueSoleilSPPExService();
	virtual ~BlueSoleilSPPExService();

	void SPPEXConnectionCallback(BYTE* lpBdAddr, UCHAR ucStatus, DWORD dwConnetionHandle);

	void close(JNIEnv *env);

	virtual BOOL isValidObject();
	virtual BOOL isExternalHandle(jlong handle);
};

static BlueSoleilStack* stack = NULL;

#endif