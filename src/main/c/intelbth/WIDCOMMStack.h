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

#ifdef _WIN32_WCE
	#define WIDCOMM_CE30
	#ifdef WIDCOMM_CE30
		#define WIDCOMM_DLL L"BtSdkCE30.dll"
	#else
		#define WIDCOMM_DLL L"BtCoreIf.dll"
	#endif
#else // _WIN32_WCE
	#define WIDCOMM_DLL L"wbtapi.dll"
	// DLL wbtapi.dll  -> WIDCOMM version 3.x and 4.x and SDK BTW-5_0_1_902-SDK
	// DLL btwapi.dll  -> WIDCOMM 5.1.x and SDK BTW-5_1_0_3101
#endif // #else // _WIN32_WCE


#ifdef _BTWLIB

#ifdef _WIN32_WCE
	#ifdef WIDCOMM_CE30
		#pragma comment(lib, "BtSdkCE30.lib")
		// Bug in BtSdkCE30.lib. See function CRfCommIf::SetSecurityLevel(unsigned short *,unsigned char,int)
		#define WCHAR unsigned short
	#else
		#define WIDCOMM_DLL L"BtSdkCE50.lib"
	#endif
#else // _WIN32_WCE
	// BTW-5_1_0_3101
	// #pragma comment(lib, "BtWdSdkLib.lib")
	// BTW-5_0_1_902-SDK and BTW-6_1_0_1501-SDK
	#pragma comment(lib, "WidcommSdklib.lib")
#endif // #else // _WIN32_WCE

//#include "btwlib.h"
#include "BtIfDefinitions.h"
#include "BtIfClasses.h"
#include "com_intel_bluetooth_BluetoothStackWIDCOMM.h"

// Have no idea how we can use BTWPRODUCTVERSION from btversinfo.h
#ifdef ATTR_ID_FAX_1_OR_AUD_VOL_OR_DEV_NAME
    #define BTW_SDK_6_0_1_300
#else
    #ifdef L2CAP_CONN_CFG_FAILED_NO_REASON
        #define BTW_SDK_5_1_0_3101
    #else
        #define BTW_SDK_5_0_1_902
    #endif
#endif

#ifdef BTW_SDK_6_0_1_300
    #define BWT_SINCE_SDK_5_0_1
    #define BWT_SINCE_SDK_5_1_0
    #define BWT_SINCE_SDK_6_0_1
#endif

#ifdef BTW_SDK_5_1_0_3101
    #define BWT_SINCE_SDK_5_0_1
    #define BWT_SINCE_SDK_5_1_0
#endif

#ifdef BTW_SDK_5_0_1_902
    #define BWT_SINCE_SDK_5_0_1
#endif

void BcAddrToString(wchar_t* addressString, BD_ADDR bd_addr);

jlong BcAddrToLong(BD_ADDR bd_addr);

void LongToBcAddr(jlong addr, BD_ADDR bd_addr);

jint DeviceClassToInt(DEV_CLASS devClass);

typedef struct {
	jlong deviceAddr;
	jint deviceClass;
	BD_NAME bdName;
	DEV_CLASS devClass;
} DeviceFound;

#define DEVICE_FOUND_MAX 50
#define SDP_DISCOVERY_RECORDS_USED_MAX 300
#define SDP_DISCOVERY_RECORDS_DEVICE_MAX 50
#define SDP_DISCOVERY_RECORDS_HOLDER_MARK 70000
#define SDP_DISCOVERY_RECORDS_HANDLE_OFFSET 1
// 7 for Server and 7 for Client, Bluetooth Can't have more
#define COMMPORTS_POOL_MAX 14

class WIDCOMMStackRfCommPort;
class WIDCOMMStackL2CapConn;

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
	BOOL deviceInquiryInProcess;
	BOOL deviceInquiryTerminated;
	BOOL deviceInquiryComplete;
	BOOL deviceInquirySuccess;

	BOOL searchServicesComplete;
	BOOL searchServicesTerminated;

	// Switch this buffers sequencialy when current if full
	DiscoveryRecHolder* discoveryRecHolderCurrent;
	DiscoveryRecHolder* discoveryRecHolderHold;

	ObjectPool* commPool;
	// CRfCommIf shared by application, lock it when connection is made
	CRITICAL_SECTION csCRfCommIf;
	CRfCommIf rfCommIfClient;
	//CRfCommIf rfCommIf;

	WIDCOMMStack();
	virtual ~WIDCOMMStack();
	void destroy(JNIEnv * env);

	void throwExtendedBluetoothStateException(JNIEnv * env);
	char* getExtendedError();

    // methods to replace virtual methods in base class CBtIf
    virtual void OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected);
    virtual void OnInquiryComplete(BOOL success, short num_responses);

	virtual void OnDiscoveryComplete();

	WIDCOMMStackRfCommPort* createCommPort(BOOL server);
	void deleteCommPort(WIDCOMMStackRfCommPort* commPort);

	WIDCOMMStackL2CapConn* createL2CapConn();
	void deleteL2CapConn(WIDCOMMStackL2CapConn* conn);
};

extern WIDCOMMStack* stack;

//	 --- Client RFCOMM connections

class WIDCOMMStackRfCommPort : public CRfCommPort, public PoolableObject {
public:

	GUID service_guid;
	BT_CHAR service_name[BT_MAX_SERVICE_NAME_LEN + 1];

	BOOL isClosing;
	BOOL isConnected;
	int isConnectionErrorType;
	BOOL isConnectionError;

	HANDLE hConnectionEvent;
	HANDLE hDataReceivedEvent;

	UINT32 other_event_code;

	ReceiveBuffer receiveBuffer;

	WIDCOMMStackRfCommPort();
	virtual ~WIDCOMMStackRfCommPort();

	void readyForReuse();
	void resetReceiveBuffer();

	virtual void closeRfCommPort(JNIEnv *env);

	virtual void OnEventReceived(UINT32 event_code);
	virtual void OnDataReceived(void *p_data, UINT16 len);
};

class WIDCOMMStackRfCommPortServer : public WIDCOMMStackRfCommPort {
public:
	UINT8 scn;
	BOOL isClientOpen;

	CRfCommIf rfCommIf;
	CSdpService* sdpService;

	WIDCOMMStackRfCommPortServer();
	virtual ~WIDCOMMStackRfCommPortServer();

	virtual void closeRfCommPort(JNIEnv *env);
};

//	 --- Client and Server L2CAP connections

WIDCOMMStackL2CapConn* validL2CapConnHandle(JNIEnv *env, jlong handle) ;

class WIDCOMMStackL2CapConn : public CL2CapConn, public PoolableObject {
public:
	BOOL isConnected;
	BOOL isClientOpen;

	GUID service_guid;
	BT_CHAR service_name[BT_MAX_SERVICE_NAME_LEN + 1];
	CSdpService* sdpService;

	UINT16 receiveMTU;
	UINT16 transmitMTU;
	UINT16 connectionTransmitMTU;

	long incomingConnectionCount;

	CL2CapIf l2CapIf;

	HANDLE hConnectionEvent;
	HANDLE hDataReceivedEvent;
	ReceiveBuffer receiveBuffer;

	WIDCOMMStackL2CapConn();
	virtual ~WIDCOMMStackL2CapConn();

	void closeConnection(JNIEnv *env);
	void closeServerConnection(JNIEnv *env);

	void selectConnectionTransmitMTU(JNIEnv *env);

	virtual void OnConnected();
    virtual void OnDataReceived(void *p_data, UINT16 length);
	virtual void OnRemoteDisconnected(UINT16 reason);

	//Server
	virtual void OnIncomingConnection();
};

#endif //  _BTWLIB
