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

// 7 for Server and 7 for Client, Bluetooth Can't have more
#define COMMPORTS_POOL_MAX 14

class BlueSoleilCOMPort;

class BlueSoleilStack {
public:
	BOOL inquiringDevice;
	
	CRITICAL_SECTION openingPortLock;
	int commPortsPoolAllocationHandleOffset;
	BlueSoleilCOMPort* commPortsPool[COMMPORTS_POOL_MAX];
	
	BlueSoleilStack();
	~BlueSoleilStack();

	BlueSoleilCOMPort* createCommPort(BOOL server);
	BlueSoleilCOMPort* getCommPort(JNIEnv *env, jlong handle);
	void deleteCommPort(BlueSoleilCOMPort* commPort);
};

class BlueSoleilCOMPort {
public:
	long magic1;
	long magic2;

	int internalHandle;
	HANDLE hComPort;
	DWORD dwConnectionHandle;

	jlong remoteAddress;

	BOOL isClosing;
	HANDLE hCloseEvent;
	BOOL receivedEOF;

	OVERLAPPED ovlComState;
	OVERLAPPED ovlRead;
	OVERLAPPED ovlWrite;

	COMSTAT comStat;
	DWORD   dwErrorFlags;

	BlueSoleilCOMPort();
	~BlueSoleilCOMPort();

	char* configureComPort(JNIEnv *env);

	void clearCommError();

	void close(JNIEnv *env);
};

static BlueSoleilStack* stack = NULL;

#endif