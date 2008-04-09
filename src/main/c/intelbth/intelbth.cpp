/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright 2004 Intel Corporation
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

#include "common.h"
#include "commonObjects.h"

#include "com_intel_bluetooth_BluetoothStackMicrosoft.h"

#ifdef _WIN32_WCE
#include <winsock2.h>
#include <bthapi.h>
#include <bt_api.h>
#include <bthutil.h>
#include <bt_sdp.h>
#else // _WIN32_WCE
#include <winsock2.h>
#include <ws2bth.h>
#include <BluetoothAPIs.h>
#endif // #else // _WIN32_WCE


static BOOL started;
static int dllWSAStartupError = 0;
static HANDLE hDeviceLookup;
static CRITICAL_SECTION csLookup;

static BOOL restoreBtMode = false;
#ifdef _WIN32_WCE
static DWORD initialBtMode;
static BTH_LOCAL_VERSION localBluetoothDeviceInfo;
#else
static BOOL initialBtIsDiscoverable;
#endif

BOOL microsoftBluetoothStackPresent;

void dllCleanup();

void throwIOExceptionWSAGetLastError(JNIEnv *env, const char *msg);

BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved)
{
	switch(ul_reason_for_call) {
	case DLL_PROCESS_ATTACH:
		{
			WSADATA data;
			if (WSAStartup(MAKEWORD(2, 2), &data) != 0) {
				dllWSAStartupError = WSAGetLastError();
				started = FALSE;
			} else {
				started = TRUE;
            }
			hDeviceLookup = NULL;
            //debug(("InitializeCriticalSection"));
			InitializeCriticalSection(&csLookup);
			return started;
		}
	case DLL_THREAD_ATTACH:
		break;
	case DLL_THREAD_DETACH:
		break;
	case DLL_PROCESS_DETACH:
		dllCleanup();
		break;
	}
	return TRUE;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    dllCleanup();
}


void dllCleanup() {
	if (started) {
		if (restoreBtMode) {
#ifdef _BTWINSOCKLIB
#ifdef _WIN32_WCE
			BthSetMode(initialBtMode);
#else
			BluetoothEnableDiscovery(NULL, initialBtIsDiscoverable);
#endif // _WIN32_WCE
#endif // _BTWINSOCKLIB
            restoreBtMode = false;
		}
		WSACleanup();
	}
	DeleteCriticalSection(&csLookup);
}

void throwIOExceptionWSAGetLastError(JNIEnv *env, const char *msg) {
	throwIOExceptionWinErrorMessage(env, msg, WSAGetLastError());
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getLibraryVersion
(JNIEnv *, jobject) {
	return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_detectBluetoothStack
(JNIEnv *env, jobject) {
	return detectBluetoothStack(env);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_enableNativeDebug
  (JNIEnv *env, jobject, jclass loggerClass, jboolean on) {
	enableNativeDebug(env, loggerClass, on);
}

//See same function for VC6 isMicrosoftBluetoothStackPresentVC6  in WIDCOMMStack.cpp
BOOL isMicrosoftBluetoothStackPresent(JNIEnv *env) {
	SOCKET s = socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM);
	if (s == INVALID_SOCKET) {
		int last_error = WSAGetLastError();
		debug(("socket error [%d] %S", last_error, getWinErrorMessage(last_error)));
		return FALSE;
	}
	SOCKADDR_BTH btAddr;
	memset(&btAddr, 0, sizeof(SOCKADDR_BTH));
	btAddr.addressFamily = AF_BTH;
#ifdef _WIN32_WCE
	btAddr.port = 0;
#else
	btAddr.port = BT_PORT_ANY;
#endif
	if (bind(s, (SOCKADDR *)&btAddr, sizeof(SOCKADDR_BTH))) {
		int last_error = WSAGetLastError();
		debug(("bind error [%d] %S", last_error, getWinErrorMessage(last_error)));
		closesocket(s);
		return FALSE;
	}

	int size = sizeof(SOCKADDR_BTH);
	if (getsockname(s, (sockaddr*)&btAddr, &size)) {
		int last_error = WSAGetLastError();
		debug(("getsockname error [%d] %S", last_error, getWinErrorMessage(last_error)));
		closesocket(s);
		return FALSE;
	}
	closesocket(s);
	//return TRUE;
	microsoftBluetoothStackPresent = (btAddr.btAddr != 0);
	return microsoftBluetoothStackPresent;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_initializationStatus(JNIEnv *env, jclass peerClass) {
    if (!microsoftBluetoothStackPresent) {
        if (!isMicrosoftBluetoothStackPresent(env)) {
            throwBluetoothStateException(env, "BluetoothStack not detected");
        }
    }
#ifdef _BTWINSOCKLIB
	if (started) {
#ifdef _WIN32_WCE
		// Use the BthGetMode function to retrieve the current mode of operation of the Bluetooth radio.
		int rc = BthGetMode(&initialBtMode);
		if (rc == ERROR_SUCCESS) {
			if (initialBtMode == BTH_POWER_OFF) {
				rc = BthSetMode(BTH_CONNECTABLE);
				if (rc == ERROR_SUCCESS) {
					restoreBtMode = true;
					return 1;
				} else {
					throwIOExceptionWinErrorMessage(env, "set Bluetooth mode error ", rc);
				}
			} else {
				return 1;
			}
		} else {
			throwIOExceptionWinErrorMessage(env, "Bluetooth radio error ", rc);
		}
		started = false;
		dllWSAStartupError = rc;
		return 0;
#else
		if (BluetoothIsDiscoverable(NULL)) {
			initialBtIsDiscoverable = true;
		}
		return 1;
#endif
    }
	throwIOExceptionWinErrorMessage(env, "Initialization error ", dllWSAStartupError);
    return 0;
#else
	return 0;
#endif // _BTWINSOCKLIB
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_uninitialize
(JNIEnv *, jobject) {

}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_isWindowsCE
(JNIEnv *, jobject) {
#ifdef _WIN32_WCE
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif // _WIN32_WCE
}

#ifdef _BTWINSOCKLIB

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_runDeviceInquiryImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jint accessCode, jint duration, jobject listener) {

    debug(("->runDeviceInquiry, duration=%i", duration));

    DeviceInquiryCallback callback;
    if (!callback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
        return INQUIRY_ERROR;
    }

	// build device query

#ifndef _WIN32_WCE
	BTH_QUERY_DEVICE query;
	query.LAP = accessCode;
#else
	BTHNS_INQUIRYBLOB query;
	query.LAP = accessCode;
	query.num_responses = 10;
#endif
	query.length = (unsigned char)duration;

	// build BLOB pointing to device query

	BLOB blob;

	blob.cbSize = sizeof(query);
	blob.pBlobData = (BYTE *)&query;

	// build query

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));
	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;

	// TODO Test this.
	//queryset.lpBlob = &blob;

#ifndef _WIN32_WCE
	queryset.lpBlob = &blob;
#endif

	// begin query

	EnterCriticalSection(&csLookup);

	if (hDeviceLookup != NULL) {
		LeaveCriticalSection(&csLookup);
		throwBluetoothStateException(env, cINQUIRY_RUNNING);
		return INQUIRY_ERROR;
	}

	// WSALookupServiceBegin Do not return for 10 seconds.
	if (!callback.callDeviceInquiryStartedCallback(env)) {
		LeaveCriticalSection(&csLookup);
		return INQUIRY_ERROR;
	}

#ifdef _WIN32_WCE
	if (WSALookupServiceBegin(&queryset, LUP_CONTAINERS, &hDeviceLookup)) {
#else
	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE|LUP_CONTAINERS, &hDeviceLookup)) {
#endif
		int last_error = WSAGetLastError();

		LeaveCriticalSection(&csLookup);
		//throwBluetoothStateExceptionWinErrorMessage(env, "Can't start Lookup", last_error);
		debug(("WSALookupServiceBegin error [%d] %S", last_error, getWinErrorMessage(last_error)));
		return INQUIRY_ERROR;
	}

	LeaveCriticalSection(&csLookup);


	// fetch results
    jint result = -1;

	int bufSize = 0x2000;
	void* buf = malloc(bufSize);
	if (buf == NULL) {
		result = INQUIRY_ERROR;
	}

	while (result == -1) {
		memset(buf, 0, bufSize);

		LPWSAQUERYSET pwsaResults = (LPWSAQUERYSET) buf;
		pwsaResults->dwSize = sizeof(WSAQUERYSET);
		pwsaResults->dwNameSpace = NS_BTH;

		DWORD size = bufSize;

		EnterCriticalSection(&csLookup);

		if (hDeviceLookup == NULL) {
			LeaveCriticalSection(&csLookup);
			result = INQUIRY_TERMINATED;
			debug(("doInquiry, INQUIRY_TERMINATED"));
			break;
		}
        debug(("doInquiry, WSALookupServiceNext"));
		if (WSALookupServiceNext(hDeviceLookup, LUP_RETURN_NAME|LUP_RETURN_ADDR|LUP_RETURN_BLOB, &size, pwsaResults)) {
			int last_error = WSAGetLastError();
			switch(last_error) {
				case WSAENOMORE:
			    case WSA_E_NO_MORE:
				    result = INQUIRY_COMPLETED;
					break;
			    default:
					debug(("Device lookup error [%d] %S", last_error, getWinErrorMessage(last_error)));
				    result = INQUIRY_ERROR;
			}
			WSALookupServiceEnd(hDeviceLookup);
			hDeviceLookup = NULL;
			LeaveCriticalSection(&csLookup);
			debug(("doInquiry, exits"));
		break;
		}

		LeaveCriticalSection(&csLookup);

        debug(("doInquiry, has next Service"));

#ifdef _WIN32_WCE
		BthInquiryResult *p_inqRes = (BthInquiryResult *)pwsaResults->lpBlob->pBlobData;

#else
		BTH_DEVICE_INFO *p_inqRes = (BTH_DEVICE_INFO *)pwsaResults->lpBlob->pBlobData;
#endif

		// get device name
		WCHAR name[256];
		BOOL bHaveName = pwsaResults->lpszServiceInstanceName && *(pwsaResults->lpszServiceInstanceName);
		StringCchPrintf(name, sizeof(name),L"%s",bHaveName ? pwsaResults->lpszServiceInstanceName : L"");
        debug(("ServiceInstanceName [%S]", name));
		jstring deviceName = env->NewString((jchar*)name, (jsize)wcslen(name));

        jboolean paired = JNI_FALSE;

#ifdef _WIN32_WCE
		int deviceClass = p_inqRes->cod;
		bt_addr deviceAddr;
#else
		int deviceClass = p_inqRes->classOfDevice;
		if (p_inqRes->flags & BDIF_PAIRED) {
		    paired = JNI_TRUE;
		}
		BTH_ADDR deviceAddr;
#endif
		deviceAddr = ((SOCKADDR_BTH *)pwsaResults->lpcsaBuffer->RemoteAddr.lpSockaddr)->btAddr;

		// notify listener
        debug(("doInquiry, notify listener"));
		if (!callback.callDeviceDiscovered(env, listener, deviceAddr, deviceClass, deviceName, paired)) {
			debug(("doInquiry, ExceptionOccurred"));
			result = INQUIRY_ERROR;
		    break;
		}
		debug(("doInquiry, listener returns"));
	}

	if (buf != NULL) {
		free(buf);
	}

	if (hDeviceLookup != NULL) {
		WSALookupServiceEnd(hDeviceLookup);
		hDeviceLookup = NULL;
	}

	return result;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_cancelInquiry(JNIEnv *env, jobject peer) {
	debug(("->cancelInquiry"));
	EnterCriticalSection(&csLookup);

	if (hDeviceLookup == NULL) {
		LeaveCriticalSection(&csLookup);
		return JNI_FALSE;
	}

	debug(("->cancelInquiry WSALookupServiceEnd"));

	WSALookupServiceEnd(hDeviceLookup);

	hDeviceLookup = NULL;

	LeaveCriticalSection(&csLookup);

	return JNI_TRUE;
}


JNIEXPORT jintArray JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_runSearchServices
(JNIEnv *env, jobject peer, jobjectArray uuidSet, jlong address)
{
	debug(("->runSearchServices"));

	// 	check if we can handle the number of UUIDs supplied
	if ((uuidSet != NULL) && (env->GetArrayLength(uuidSet) > MAX_UUIDS_IN_QUERY)) {
		return NULL;
	}

#ifndef _WIN32_WCE
	// 	generate a Bluetooth address string (WSAAddressToString doesn't work on WinCE)

	WCHAR addressString[20];

	swprintf_s(addressString, _T("(%02x:%02x:%02x:%02x:%02x:%02x)"), (int)(address>>40&0xff), (int)(address>>32&0xff), (int)(address>>24&0xff), (int)(address>>16&0xff), (int)(address>>8&0xff), (int)(address&0xff));

	//	build service query

	BTH_QUERY_SERVICE queryservice;

#else
	BTHNS_RESTRICTIONBLOB queryservice;
#endif

	memset(&queryservice, 0, sizeof(queryservice));

	queryservice.type = SDP_SERVICE_SEARCH_REQUEST;

	GUID guid;

    jclass clsUUID = NULL;

	for(int i = 0; (uuidSet != NULL) && (i < env->GetArrayLength(uuidSet)); i++) {
	    if (clsUUID == NULL) {
	        clsUUID = env->FindClass("javax/bluetooth/UUID");
            if (clsUUID == NULL) {
                env->FatalError("Can't create UUID Class");
		        return NULL;
            }
        }

		jbyteArray uuidValue = (jbyteArray)env->GetObjectField(env->GetObjectArrayElement(uuidSet, i), env->GetFieldID(clsUUID, "uuidValue", "[B"));

		// pin array

		jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);

		// build UUID

		convertUUIDBytesToGUID(bytes, &guid);

		//UUID is full 128 bits

		queryservice.uuids[i].uuidType = SDP_ST_UUID128;

		memcpy(&queryservice.uuids[i].u.uuid128, &guid, sizeof(guid));

		// unpin array

		env->ReleaseByteArrayElements(uuidValue, bytes, 0);
	}
	if (clsUUID != NULL) {
	    env->DeleteLocalRef(clsUUID);
	}

	// build BLOB pointing to service query

	BLOB blob;

	blob.cbSize = sizeof(queryservice);
	blob.pBlobData = (BYTE *)&queryservice;

	// build query

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
	queryset.lpBlob = &blob;

#ifdef _WIN32_WCE

	// Build address

	SOCKADDR_BTH sa;
	memset (&sa, 0, sizeof(sa));
	sa.addressFamily = AF_BT;
	sa.btAddr = address;
	CSADDR_INFO csai;
	memset (&csai, 0, sizeof(csai));
	csai.RemoteAddr.lpSockaddr = (sockaddr *)&sa;
	csai.RemoteAddr.iSockaddrLength = sizeof(sa);
	queryset.lpcsaBuffer = &csai;
#else
	queryset.lpszContext = addressString;
#endif

	HANDLE hLookupSearchServices;

	// begin query

#ifdef _WIN32_WCE
	if (WSALookupServiceBegin(&queryset, 0, &hLookupSearchServices)) {
		int last_error = WSAGetLastError();
		debug(("WSALookupServiceBegin error [%i] %S", last_error, getWinErrorMessage(last_error)));
		return NULL;
	}
#else
	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE, &hLookupSearchServices)) {
		int last_error = WSAGetLastError();
		debug(("WSALookupServiceBegin error [%i] %S", last_error, getWinErrorMessage(last_error)));
		// [10108] No such service is known. The service cannot be found in the specified name space. -> SERVICE_SEARCH_DEVICE_NOT_REACHABLE
		if (10108 == last_error) {
			throwException(env, "com/intel/bluetooth/SearchServicesDeviceNotReachableException", "");
		}
		return NULL;
	}
#endif

	// fetch results
    jintArray result = NULL;

	int bufSize = 0x2000;
	void* buf = malloc(bufSize);
	if (buf == NULL) {
		WSALookupServiceEnd(hLookupSearchServices);
		return NULL;
	}
	memset(buf, 0, bufSize);

	LPWSAQUERYSET pwsaResults = (LPWSAQUERYSET) buf;
	pwsaResults->dwSize = sizeof(WSAQUERYSET);
	pwsaResults->dwNameSpace = NS_BTH;
	pwsaResults->lpBlob = NULL;

	DWORD size = bufSize;

#ifdef _WIN32_WCE
	if (WSALookupServiceNext(hLookupSearchServices, 0, &size, pwsaResults)) {
#else
	if (WSALookupServiceNext(hLookupSearchServices, LUP_RETURN_BLOB, &size, pwsaResults)) {
#endif
		int last_error = WSAGetLastError();
		switch(last_error) {
			case WSANO_DATA:
				result = env->NewIntArray(0);
				break;
			default:
				debug(("WSALookupServiceNext error [%i] %S", last_error, getWinErrorMessage(last_error)));
				result =  NULL;
		}
	} else {
		// construct int array to hold handles
		result = env->NewIntArray(pwsaResults->lpBlob->cbSize/sizeof(ULONG));
		jint *ints = env->GetIntArrayElements(result, 0);
		memcpy(ints, pwsaResults->lpBlob->pBlobData, pwsaResults->lpBlob->cbSize);
		env->ReleaseIntArrayElements(result, ints, 0);
	}
	WSALookupServiceEnd(hLookupSearchServices);
	free(buf);
	return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getServiceAttributes(JNIEnv *env, jobject peer, jintArray attrIDs, jlong address, jint handle)
{
    debug(("->getServiceAttributes"));
#ifdef _WIN32_WCE
	BTHNS_RESTRICTIONBLOB *queryservice = (BTHNS_RESTRICTIONBLOB *)malloc(sizeof(BTHNS_RESTRICTIONBLOB)+sizeof(SdpAttributeRange)*(1));
	queryservice->type = SDP_SERVICE_ATTRIBUTE_REQUEST;

	queryservice->serviceHandle = handle;
	queryservice->numRange = 1;

	// set attribute ranges
	jint *ints = env->GetIntArrayElements(attrIDs, 0);

	queryservice->pRange[0].minAttribute = (USHORT)ints[0];
	queryservice->pRange[0].maxAttribute = (USHORT)ints[env->GetArrayLength(attrIDs)-1];

	env->ReleaseIntArrayElements(attrIDs, ints, 0);
#else
	// generate a Bluetooth address string (WSAAddressToString doesn't work on WinCE)

	WCHAR addressString[20];

	swprintf_s(addressString, _T("(%02x:%02x:%02x:%02x:%02x:%02x)"), (int)(address>>40&0xff), (int)(address>>32&0xff), (int)(address>>24&0xff), (int)(address>>16&0xff), (int)(address>>8&0xff), (int)(address&0xff));

	// build attribute query

	BTH_QUERY_SERVICE *queryservice = (BTH_QUERY_SERVICE *)malloc(sizeof(BTH_QUERY_SERVICE)+sizeof(SdpAttributeRange)*(env->GetArrayLength(attrIDs)-1));
	memset(queryservice, 0, sizeof(BTH_QUERY_SERVICE)-sizeof(SdpAttributeRange));

	queryservice->type = SDP_SERVICE_ATTRIBUTE_REQUEST;
	queryservice->serviceHandle = handle;
	queryservice->numRange = env->GetArrayLength(attrIDs);

	// set attribute ranges

	jint *ints = env->GetIntArrayElements(attrIDs, 0);

	for(int i = 0; i < env->GetArrayLength(attrIDs); i++) {
		queryservice->pRange[i].minAttribute = (USHORT)ints[i];
		queryservice->pRange[i].maxAttribute = (USHORT)ints[i];
	}

	env->ReleaseIntArrayElements(attrIDs, ints, 0);
#endif

	// build BLOB pointing to attribute query

	BLOB blob;

#ifdef _WIN32_WCE
	blob.cbSize = sizeof(BTHNS_RESTRICTIONBLOB);
#else
	blob.cbSize = sizeof(BTH_QUERY_SERVICE);
#endif
	blob.pBlobData = (BYTE *)queryservice;

	// build query

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
#ifdef _WIN32_WCE

	// Build address

	SOCKADDR_BTH sa;
	memset (&sa, 0, sizeof(sa));
	sa.addressFamily = AF_BT;
	sa.btAddr = address;
	CSADDR_INFO csai;
	memset (&csai, 0, sizeof(csai));
	csai.RemoteAddr.lpSockaddr = (sockaddr *)&sa;
	csai.RemoteAddr.iSockaddrLength = sizeof(sa);
	queryset.lpcsaBuffer = &csai;
#else
	queryset.lpszContext = addressString;
#endif
	queryset.lpBlob = &blob;

	HANDLE hLookupServiceAttributes;

	// begin query

#ifdef _WIN32_WCE
	if (WSALookupServiceBegin(&queryset, 0, &hLookupServiceAttributes)) {
#else
	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE, &hLookupServiceAttributes)) {
#endif
		free(queryservice);
		throwIOExceptionWSAGetLastError(env, "Failed to begin attribute query");
		return NULL;
	}

	free(queryservice);

	// fetch results
	int bufSize = 0x2000;
	void* buf = malloc(bufSize);
	if (buf == NULL) {
		WSALookupServiceEnd(hLookupServiceAttributes);
		return NULL;
	}
	memset(buf, 0, bufSize);

	LPWSAQUERYSET pwsaResults = (LPWSAQUERYSET) buf;
	pwsaResults->dwSize = sizeof(WSAQUERYSET);
	pwsaResults->dwNameSpace = NS_BTH;
	pwsaResults->lpBlob = NULL;

	DWORD size = bufSize;

	jbyteArray result = NULL;
#ifdef _WIN32_WCE
	if (WSALookupServiceNext(hLookupServiceAttributes, 0, &size, pwsaResults)) {
#else
	if (WSALookupServiceNext(hLookupServiceAttributes, LUP_RETURN_BLOB, &size, pwsaResults)) {
#endif
		throwIOExceptionWSAGetLastError(env, "Failed to perform attribute query");
		result = NULL;
	} else {
		// construct byte array to hold blob
		result = env->NewByteArray(pwsaResults->lpBlob->cbSize);

		jbyte *bytes = env->GetByteArrayElements(result, 0);

		memcpy(bytes, pwsaResults->lpBlob->pBlobData, pwsaResults->lpBlob->cbSize);

		env->ReleaseByteArrayElements(result, bytes, 0);
	}
	WSALookupServiceEnd(hLookupServiceAttributes);
	free(buf);
	return result;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_registerService(JNIEnv *env, jobject peer, jbyteArray record, jint classOfDevice) {
	debug(("->registerService"));
	int length = env->GetArrayLength(record);

	HANDLE handle = NULL;

	// build service set

	ULONG version = BTH_SDP_VERSION;

#ifdef _WIN32_WCE
	BTHNS_SETBLOB *setservice = (BTHNS_SETBLOB*)malloc(sizeof(BTHNS_SETBLOB)+length-1);
	memset(setservice, 0, sizeof(BTHNS_SETBLOB)-1);
	setservice->pRecordHandle = (ULONG*)&handle;
#else
	BTH_SET_SERVICE *setservice = (BTH_SET_SERVICE *)malloc(sizeof(BTH_SET_SERVICE)+length-1);
	memset(setservice, 0, sizeof(BTH_SET_SERVICE)-1);
	setservice->pRecordHandle = &handle;
    setservice->fCodService = GET_COD_SERVICE(classOfDevice);
#endif

	setservice->pSdpVersion = &version;
	setservice->ulRecordLength = length;

	jbyte *bytes = env->GetByteArrayElements(record, 0);

	memcpy(setservice->pRecord, bytes, length);

	env->ReleaseByteArrayElements(record, bytes, 0);

	// build BLOB pointing to service set

	BLOB blob;

#ifdef _WIN32_WCE
	blob.cbSize = sizeof(BTHNS_SETBLOB);
#else
	blob.cbSize = sizeof(BTH_SET_SERVICE);
#endif
	blob.pBlobData = (BYTE *)setservice;

	// build set

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
	queryset.lpBlob = &blob;

	// perform set

	if (WSASetService(&queryset, RNRSERVICE_REGISTER, 0)) {
		throwExceptionWinErrorMessage(env, cServiceRegistrationException, "Failed to register service", WSAGetLastError());
		free(setservice);
		return 0;
	}
	free(setservice);
	return (jlong)handle;
}


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_unregisterService(JNIEnv *env, jobject peer, jlong handle) {
	debug(("->unregisterService"));
	// build service set

	ULONG version = BTH_SDP_VERSION;

#ifdef _WIN32_WCE
	BTHNS_SETBLOB setservice;
	memset(&setservice, 0, sizeof(BTHNS_SETBLOB));
	setservice.pRecordHandle = (ULONG *)&handle;
#else
	BTH_SET_SERVICE setservice;
	memset(&setservice, 0, sizeof(BTH_SET_SERVICE));
	setservice.pRecordHandle = (HANDLE *)&handle;
#endif

	setservice.pSdpVersion = &version;

	// build BLOB pointing to service set

	BLOB blob;
#ifdef _WIN32_WCE
	blob.cbSize = sizeof(BTHNS_SETBLOB);
#else
	blob.cbSize = sizeof(BTH_SET_SERVICE);
#endif
	blob.pBlobData = (BYTE *)&setservice;

	// build set

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
	queryset.lpBlob = &blob;

	// perform set

	if (WSASetService(&queryset, RNRSERVICE_DELETE, 0)) {
		throwServiceRegistrationException(env, "Failed to unregister service");
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_socket(JNIEnv *env, jobject peer, jboolean authenticate, jboolean encrypt) {
    debug(("->socket"));
	// create socket

	SOCKET s = socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM);

	if (s == INVALID_SOCKET) {
		throwIOExceptionWinGetLastError(env, "Failed to create socket");
		return 0;
	}

	// set socket options

	if (authenticate) {
		ULONG ul = TRUE;

		if (setsockopt(s, SOL_RFCOMM, SO_BTH_AUTHENTICATE, (char *)&ul, sizeof(ULONG))) {
			closesocket(s);
			throwIOExceptionWinGetLastError(env, "Failed to set authentication option");
			return 0;
		}
	}

	if (encrypt) {
#ifdef _WIN32_WCE
		int ul = TRUE;
#else
		ULONG ul = TRUE;
#endif
		if (setsockopt(s, SOL_RFCOMM, SO_BTH_ENCRYPT, (char *)&ul, sizeof(ul))) {
			closesocket(s);
			throwIOExceptionWinGetLastError(env, "Failed to set encryption option");
			return 0;
		}
	}
	return s;
}

/* This does not work: [10042] An unknown, invalid, or unsupported option or level was specified in a getsockopt or setsockopt call. */
/*
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getSecurityOptImpl(JNIEnv * env, jobject peer, jlong socket) {
	ULONG authenticatedVal = FALSE;
	int optLen = sizeof(ULONG);
	if (getsockopt((SOCKET)socket, SOL_RFCOMM,  SO_BTH_AUTHENTICATE,  (char*)&authenticatedVal,  &optLen) == SOCKET_ERROR) {
		throwIOExceptionWinGetLastError(env, "Failed to get authenticate option");
		return NOAUTHENTICATE_NOENCRYPT;
	}
	ULONG encryptedVal = FALSE;
	optLen = sizeof(ULONG);
	if (getsockopt((SOCKET)socket, SOL_RFCOMM,  SO_BTH_ENCRYPT,  (char*)&encryptedVal,  &optLen) == SOCKET_ERROR) {
		throwIOExceptionWinGetLastError(env, "Failed to get encryption option");
		return NOAUTHENTICATE_NOENCRYPT;
	}
	if (!authenticatedVal) {
		return NOAUTHENTICATE_NOENCRYPT;
	}

	if (encryptedVal) {
		return AUTHENTICATE_ENCRYPT;
	} else {
		return AUTHENTICATE_NOENCRYPT;
	}
}
*/

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getsockaddress(JNIEnv *env, jobject peer, jlong socket) {
	debug(("->getsockaddress"));
	// get socket name

	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	if (getsockname((SOCKET)socket, (sockaddr *)&addr, &size)) {
		throwIOExceptionWSAGetLastError(env, "Failed to get socket name");
		return 0;
	}
	return addr.btAddr;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getsockchannel(JNIEnv *env, jobject peer, jlong socket) {
	debug(("->getsockchannel"));
	// get socket name

	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	if (getsockname((SOCKET)socket, (sockaddr *)&addr, &size)) {
		throwIOExceptionWSAGetLastError(env, "Failed to get socket name");
		return 0;
	}
	return addr.port;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_connect(JNIEnv *env, jobject peer, jlong socket, jlong address, jint channel) {
    debug(("->connect"));

	SOCKADDR_BTH addr;

	memset(&addr, 0, sizeof(SOCKADDR_BTH));

	addr.addressFamily = AF_BTH;
	addr.btAddr = address;
	addr.port = channel;

	int retyCount = 0;
	int retyMAX = 2;
connectRety:
	if (connect((SOCKET)socket, (sockaddr *)&addr, sizeof(SOCKADDR_BTH))) {
		retyCount ++;
		int last_error = WSAGetLastError();
		//10051 - A socket operation was attempted to an unreachable network. / Error other than time-out at L2CAP or Bluetooth radio level.
		if (last_error == WSAENETUNREACH) {
			if (retyCount < retyMAX) {
				debug(("connectRety %i", retyCount));
				goto connectRety;
			}
		}
		if (last_error == WSAEACCES) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_SECURITY_BLOCK, "Connecting application requested authentication, but authentication failed [10013] .");
		} else if (last_error == WSAETIMEDOUT) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_TIMEOUT, "Connection timeout; [%lu] %S", last_error, getWinErrorMessage(last_error));
		} else {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to connect; [%lu] %S", last_error, getWinErrorMessage(last_error));
		}
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_bind(JNIEnv *env, jobject peer, jlong socket) {
	// bind socket
	debug(("->bind"));

	SOCKADDR_BTH addr;
	memset(&addr, 0, sizeof(addr));
	addr.addressFamily = AF_BTH;
#ifdef _WIN32_WCE
	addr.port = 0;
#else
	addr.port = BT_PORT_ANY;
#endif
	if (bind((SOCKET)socket, (SOCKADDR *)&addr, sizeof(addr))) {
		closesocket((SOCKET)socket);
		throwIOExceptionWSAGetLastError(env, "Failed to bind socket");
		return;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_listen(JNIEnv *env, jobject peer, jlong socket)
{
    debug(("->listen"));
	if (listen((SOCKET)socket, 10)) {
		throwIOExceptionWSAGetLastError(env, "Failed to listen socket");
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_accept(JNIEnv *env, jobject peer, jlong socket) {
	debug(("->accept"));
	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	SOCKET s = accept((SOCKET)socket, (sockaddr *)&addr, &size);

	if (s == INVALID_SOCKET) {
		throwIOException(env, "Failed to listen socket");
		return 0;
	}

	debug(("connection accepted"));

	return s;
}

/*
 *
 * Use to determine the amount of data pending in the network's input buffer that can be read from socket.
 * returns the amount of data that can be read in a single call to the recv function, which may not be the same as
 * the total amount of data queued on the socket.
 */
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_recvAvailable(JNIEnv *env, jobject peer, jlong socket)
{
	unsigned long arg = 0;
	if (ioctlsocket((SOCKET)socket, FIONREAD, &arg) != 0) {
		throwIOExceptionWSAGetLastError(env, "Failed to read available");
		return 0;
	}
	return (jint)arg;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_recv__J(JNIEnv *env, jobject peer, jlong socket) {
	debug(("->recv()"));
	unsigned char c;

	int rc = recv((SOCKET)socket, (char *)&c, 1, 0);
	if (rc == SOCKET_ERROR) {
		throwIOExceptionWSAGetLastError(env, "Failed to read(int)");
		return 0;
	} else if (rc == 0) {
		debug(("Connection closed"));
		// See InputStream.read();
		return -1;
	}
	return (int)c;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_recv__J_3BII(JNIEnv *env, jobject peer, jlong socket, jbyteArray b, jint off, jint len) {
	debug(("->recv(byte[],int,int=%i)", len));
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		int count = recv((SOCKET)socket, (char *)(bytes+off+done), len-done, 0);
		if (count == SOCKET_ERROR) {
			env->ReleaseByteArrayElements(b, bytes, 0);
			throwIOExceptionWSAGetLastError(env, "Failed to read(byte[])");
			return 0;
		} else if (count == 0) {
			debug(("Connection closed"));
			if (done == 0) {
				// See InputStream.read();
				env->ReleaseByteArrayElements(b, bytes, 0);
				return -1;
			} else {
				break;
			}
		}
		done += count;
		if (done != 0) {
		    unsigned long available = 0;
	        if (ioctlsocket((SOCKET)socket, FIONREAD, &available) != 0) {
	            // error;
	            break;
	        } else if (available == 0) {
	            break;
	        }
		}
	}

	env->ReleaseByteArrayElements(b, bytes, 0);

	return done;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_send__JI(JNIEnv *env, jobject peer, jlong socket, jint b) {
	debug(("->send(int,int)"));
	char c = (char)b;

	if (send((SOCKET)socket, &c, 1, 0) != 1) {
		throwIOExceptionWSAGetLastError(env, "Failed to write");
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_send__J_3BII(JNIEnv *env, jobject peer, jlong socket, jbyteArray b, jint off, jint len) {
	debug(("->send(int,byte[],int,int=%i)", len));
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		int count = send((SOCKET)socket, (char *)(bytes + off + done), len - done, 0);
		if (count <= 0) {
			env->ReleaseByteArrayElements(b, bytes, 0);
			throwIOExceptionWSAGetLastError(env, "Failed to write");
			return;
		}

		done += count;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_close(JNIEnv *env, jobject peer, jlong socket)
{
	debug(("->close"));
	if (closesocket((SOCKET)socket)) {
		throwIOExceptionWSAGetLastError(env, "Failed to close socket");
	}
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getpeername(JNIEnv *env, jobject peer, jlong addr)
{
#ifdef _WIN32_WCE
	/*
	 * For the moment just return an empty string on Windows Mobile
	 * The next device scan will return a name anyway...
	 * To be modified later
	 */
	return env->NewStringUTF((char*)"");
#else

    debug(("->getpeername"));


	WSAQUERYSET querySet;
	memset(&querySet, 0, sizeof(WSAQUERYSET));
	querySet.dwSize = sizeof(WSAQUERYSET);
	querySet.dwNameSpace = NS_BTH;

	DWORD flagsBegin = LUP_FLUSHCACHE | LUP_CONTAINERS;
	DWORD flagsNext = LUP_RETURN_NAME | LUP_RETURN_ADDR | LUP_CONTAINERS;

	EnterCriticalSection(&csLookup);

	if (hDeviceLookup != NULL) {
		LeaveCriticalSection(&csLookup);
		throwIOException(env, cINQUIRY_RUNNING);
		return NULL;
	}

	HANDLE hLookupPeerName = NULL;

	if (WSALookupServiceBegin(&querySet, flagsBegin, &hLookupPeerName)) {
		LeaveCriticalSection(&csLookup);
		throwIOExceptionWSAGetLastError(env, "Name Lookup error");
		return NULL;
	}

	LeaveCriticalSection(&csLookup);
	DWORD bufSize = 0x2000;
	void* buf = malloc(bufSize);
	if (buf == NULL) {
		return NULL;
	}

	jstring result = NULL;
	BOOL error = FALSE;
	while (!error) {
		memset(buf, 0, bufSize);
		WSAQUERYSET *pwsaResults = (WSAQUERYSET*)buf;
		pwsaResults->dwSize = sizeof(WSAQUERYSET);
		pwsaResults->dwNameSpace = NS_BTH;

		DWORD bufferLength = bufSize;

		EnterCriticalSection(&csLookup);
		if (WSALookupServiceNext(hLookupPeerName, flagsNext, &bufferLength, pwsaResults)) {
			int err = WSAGetLastError();
			LeaveCriticalSection(&csLookup);
			switch(err) {
			case WSAENOMORE:
			case WSA_E_NO_MORE:
				break;
			default:
				throwIOExceptionWinErrorMessage(env, "Service Lookup error", err);
				error = TRUE;
				break;
			}
			break;
		}

		LeaveCriticalSection(&csLookup);

		if (((SOCKADDR_BTH *)((CSADDR_INFO *)pwsaResults->lpcsaBuffer)->RemoteAddr.lpSockaddr)->btAddr == addr) {
			WCHAR *name = pwsaResults->lpszServiceInstanceName;
			debug(("return %s", name));
			result = env->NewString((jchar*)name, (jsize)wcslen(name));
			break;
		}
	}
	free(buf);
	if (hLookupPeerName != NULL) {
		WSALookupServiceEnd(hLookupPeerName);
	}

	if ((result == NULL) && (!error)) {
		debug(("return empty"));
		result = env->NewStringUTF((char*)"");
	}
	return result;
#endif
}


JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getpeeraddress(JNIEnv *env, jobject peer, jlong socket)
{
	debug(("->getpeeraddress"));
	SOCKADDR_BTH addr;
	int size = sizeof(addr);
	if (getpeername((SOCKET) socket, (sockaddr*)&addr, &size)) {
		throwIOExceptionWSAGetLastError(env, "peername error");
		return 0;
	}
	return addr.btAddr;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_storesockopt(JNIEnv * env, jobject peer, jlong socket) {
#ifndef _WIN32_WCE
	int optVal;
	int optLen = sizeof(int);
	if (getsockopt((SOCKET)socket, SOL_SOCKET,  SO_RCVBUF,  (char*)&optVal,  &optLen) != SOCKET_ERROR) {
		debug(("receive buffer %i", optVal));
	}
	if (getsockopt((SOCKET)socket, SOL_SOCKET,  SO_SNDBUF,  (char*)&optVal,  &optLen) != SOCKET_ERROR) {
		debug(("send buffer %i", optVal));
	}
#else
	int optVal;
	int optLen = sizeof(int);
	if (getsockopt(socket, SOL_RFCOMM,  SO_BTH_GET_RECV_BUFFER,  (char*)&optVal,  &optLen) != SOCKET_ERROR) {
		debug(("receive buffer %i", optVal));
	}
	if (getsockopt(socket, SOL_RFCOMM,  SO_BTH_GET_SEND_BUFFER,  (char*)&optVal,  &optLen) != SOCKET_ERROR) {
		debug(("send buffer %i", optVal));
	}
	optLen = sizeof(BTH_LOCAL_VERSION);
	if (getsockopt(socket, SOL_RFCOMM,  SO_BTH_GET_SEND_BUFFER,  (char*)&localBluetoothDeviceInfo,  &optLen) != SOCKET_ERROR) {
		//
	}
#endif
}

// Unsupported for _WIN32_WCE for the moment...
#ifndef _WIN32_WCE
BOOL getBluetoothGetRadioInfo(jlong address, BLUETOOTH_RADIO_INFO* info) {
	HANDLE hRadio;
	BLUETOOTH_FIND_RADIO_PARAMS btfrp = { sizeof(btfrp) };
	HBLUETOOTH_RADIO_FIND hFind = BluetoothFindFirstRadio( &btfrp, &hRadio );
	if ( NULL != hFind ) {
		do {
			BLUETOOTH_RADIO_INFO radioInfo;
			radioInfo.dwSize = sizeof(radioInfo);
			if (ERROR_SUCCESS == BluetoothGetRadioInfo(hRadio, &radioInfo)) {
				if (radioInfo.address.ullLong == address) {
					BluetoothFindRadioClose(hFind);
					memcpy(info, &radioInfo, sizeof(BLUETOOTH_RADIO_INFO));
					return TRUE;
				}
			}
		} while( BluetoothFindNextRadio( hFind, &hRadio ) );
		BluetoothFindRadioClose( hFind );
	}
	return FALSE;
}
#endif

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getradioname(JNIEnv *env, jobject peer, jlong address)
{
    debug(("->getradioname"));
// Unsupported for _WIN32_WCE for the moment...
#ifndef _WIN32_WCE
	BLUETOOTH_RADIO_INFO radioInfo;
	if (getBluetoothGetRadioInfo(address, &radioInfo)) {
		return env->NewString((jchar*)radioInfo.szName, (jsize) wcslen(radioInfo.szName));
	}
#endif
	return NULL;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getDeviceVersion(JNIEnv *env, jobject peer, jlong address)
{
#ifndef _WIN32_WCE
	BLUETOOTH_RADIO_INFO radioInfo;
	if (getBluetoothGetRadioInfo(address, &radioInfo)) {
		return radioInfo.lmpSubversion;
	}
	return -1;
#else
	return localBluetoothDeviceInfo.lmp_subversion;
#endif
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getDeviceManufacturer(JNIEnv *env, jobject peer, jlong address)
{
#ifndef _WIN32_WCE
	BLUETOOTH_RADIO_INFO radioInfo;
	if (getBluetoothGetRadioInfo(address, &radioInfo)) {
		return radioInfo.manufacturer;
	}
	return -1;
#else
	return localBluetoothDeviceInfo.manufacturer;
#endif
}

#define MAJOR_COMPUTER 0x0100
#define MAJOR_PHONE 0x0200
#define COMPUTER_MINOR_HANDHELD 0x10
#define PHONE_MINOR_SMARTPHONE 0x0c

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getDeviceClass(JNIEnv *env, jobject peer, jlong address)
{
#ifndef _WIN32_WCE
	if (address == 0) {
		return MAJOR_COMPUTER;
	}
	BLUETOOTH_RADIO_INFO radioInfo;
	if (getBluetoothGetRadioInfo(address, &radioInfo)) {
		return radioInfo.ulClassofDevice;
	} else {
	    debug(("e.can't find RadioInfo"));
	}
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
		debug(("PLATFORMTYPE %S", szPlatform));
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

#define BTH_MODE_POWER_OFF 1
#define BTH_MODE_CONNECTABLE  2
#define BTH_MODE_DISCOVERABLE 3

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_getBluetoothRadioMode(JNIEnv *env, jobject peer)
{
#ifndef _WIN32_WCE
	if (BluetoothIsDiscoverable(NULL)) {
		return BTH_MODE_DISCOVERABLE;
	}
	if (BluetoothIsConnectable(NULL)) {
		return BTH_MODE_CONNECTABLE;
	}
#else
	DWORD dwMode;
	int rc = BthGetMode(&dwMode);
	if (rc == ERROR_SUCCESS) {
		switch(dwMode) {
		case BTH_DISCOVERABLE:
			return BTH_MODE_DISCOVERABLE;
		case BTH_CONNECTABLE:
			return BTH_MODE_CONNECTABLE;
		case BTH_POWER_OFF:
			return BTH_MODE_POWER_OFF;
		}
	} else {
		throwBluetoothStateExceptionWinErrorMessage(env, "Bluetooth mode error ", rc);
	}
#endif
	return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackMicrosoft_setDiscoverable(JNIEnv *env, jobject peer, jboolean on)
{
#ifndef _WIN32_WCE
	BOOL enabled = FALSE;
	if (on) {
		if (!BluetoothEnableIncomingConnections(NULL, TRUE)) {
		    throwBluetoothStateException(env, "Enable Incoming Connections error");
		}
		enabled = TRUE;
	}
	if (BluetoothEnableDiscovery(NULL, enabled)) {
		restoreBtMode = (initialBtIsDiscoverable != enabled);
	} else {
	    throwBluetoothStateException(env, "Change Bluetooth Discovery mode error");
	}
#else
	DWORD dwMode = BTH_CONNECTABLE;
	if (on) {
		dwMode = BTH_DISCOVERABLE;
	}
	int rc = BthSetMode(dwMode);
	if (rc == ERROR_SUCCESS) {
		restoreBtMode = (initialBtMode != dwMode);
	} else {
		throwBluetoothStateExceptionWinErrorMessage(env, "Set Bluetooth mode error ", rc);
	}
#endif
}

#endif // _BTWINSOCKLIB