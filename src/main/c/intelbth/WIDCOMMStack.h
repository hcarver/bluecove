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

#define WIDCOMM_DLL L"wbtapi.dll"
// DLL wbtapi.dll  -> WIDCOMM version 3.x and 4.x and SDK BTW-5_0_1_902-SDK
// DLL btwapi.dll  -> WIDCOMM 5.1.x and SDK BTW-5_1_0_3101

#ifdef _BTWLIB

// BTW-5_1_0_3101
// #pragma comment(lib, "BtWdSdkLib.lib")
// BTW-5_0_1_902-SDK
#pragma comment(lib, "WidcommSdklib.lib")

//#include "btwlib.h"
#include "BtIfDefinitions.h"
#include "BtIfClasses.h"
#include "com_intel_bluetooth_BluetoothStackWIDCOMM.h"

// We specify which DLLs to delay load with the /delayload:btwapi.dll linker option
// This is how it is now: wbtapi.dll;btfunc.dll;irprops.cpl

#ifdef VC6
#pragma comment(lib, "DelayImp.lib")
#pragma comment(linker, "/delayload:wbtapi.dll")
#endif

void BcAddrToString(wchar_t* addressString, BD_ADDR bd_addr);

jlong BcAddrToLong(BD_ADDR bd_addr);

void LongToBcAddr(jlong addr, BD_ADDR bd_addr);

jint DeviceClassToInt(DEV_CLASS devClass);

typedef struct {
	jlong deviceAddr;
	jint deviceClass;
	BD_NAME bdName;
} DeviceFound;

#define DEVICE_FOUND_MAX 50
#define SDP_DISCOVERY_RECORDS_USED_MAX 300
#define SDP_DISCOVERY_RECORDS_DEVICE_MAX 50
#define SDP_DISCOVERY_RECORDS_HOLDER_MARK 70000
#define SDP_DISCOVERY_RECORDS_HANDLE_OFFSET 1
// 7 for Server and 7 for Client, Bluetooth Can't have more
#define COMMPORTS_POOL_MAX 14

#define COMMPORTS_REUSE_OBJECTS FALSE
#define COMMPORTS_CONNECT_TIMEOUT 60000

class WIDCOMMStackRfCommPort;

class DiscoveryRecHolder {
public:
	BOOL oddHolder;
	int sdpDiscoveryRecordsUsed;
	CSdpDiscoveryRec sdpDiscoveryRecords[SDP_DISCOVERY_RECORDS_USED_MAX];

	DiscoveryRecHolder();
};

class WIDCOMMStack : public CBtIf {
public:
	HANDLE hEvent;

	DeviceFound deviceResponded[DEVICE_FOUND_MAX];
	int deviceRespondedIdx;
	BOOL deviceInquiryTerminated;
	BOOL deviceInquiryComplete;
	BOOL deviceInquirySuccess;

	BOOL searchServicesComplete;
	BOOL searchServicesTerminated;

	// Switch this buffers sequencialy when current if full
	DiscoveryRecHolder* discoveryRecHolderCurrent;
	DiscoveryRecHolder* discoveryRecHolderHold;

	int commPortsPoolDeletionCount;
	int commPortsPoolAllocationHandleOffset;
	WIDCOMMStackRfCommPort* commPortsPool[COMMPORTS_POOL_MAX];
	// One CRfCommIf shared by application, lock it when connection is made
	CRITICAL_SECTION csCRfCommIf;
	CRfCommIf rfCommIf;

	WIDCOMMStack();
	virtual ~WIDCOMMStack();
	void destroy(JNIEnv * env);

	void throwExtendedErrorException(JNIEnv * env, const char *name);
	char* getExtendedError();

    // methods to replace virtual methods in base class CBtIf
    virtual void OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected);
    virtual void OnInquiryComplete(BOOL success, short num_responses);

	virtual void OnDiscoveryComplete();

	int getCommPortFreeIndex();
	WIDCOMMStackRfCommPort* createCommPort(BOOL server);
	void deleteCommPort(WIDCOMMStackRfCommPort* commPort);
};

//	 --- Client RFCOMM connections

class WIDCOMMStackRfCommPort : public CRfCommPort {
public:
	long magic1;
	long magic2;

	int internalHandle;
	int commPortsPoolDeletionIndex;
	BOOL readyToFree;

	GUID service_guid;
	BT_CHAR service_name[BT_MAX_SERVICE_NAME_LEN + 1];

	BOOL isClosing;
	BOOL isConnected;
	BOOL isConnectionError;

	HANDLE hEvents[2];

	ReceiveBuffer receiveBuffer;

	WIDCOMMStackRfCommPort();
	virtual ~WIDCOMMStackRfCommPort();

	void readyForReuse();
	void resetReceiveBuffer();

	virtual void closeRfCommPort(JNIEnv *env);

	virtual void OnEventReceived (UINT32 event_code);
	virtual void OnDataReceived (void *p_data, UINT16 len);
};

class WIDCOMMStackRfCommPortServer : public WIDCOMMStackRfCommPort {
public:
	UINT8 scn;
	BOOL isClientOpen;

	//CRfCommIf rfCommIf;
	CSdpService* sdpService;

	WIDCOMMStackRfCommPortServer();
	virtual ~WIDCOMMStackRfCommPortServer();

	virtual void closeRfCommPort(JNIEnv *env);
};

#endif
