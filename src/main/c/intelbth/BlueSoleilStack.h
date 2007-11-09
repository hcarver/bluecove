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