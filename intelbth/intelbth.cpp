/*
Copyright 2004 Intel Corporation

This file is part of Blue Cove.

Blue Cove is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation; either version 2.1 of the License, or
(at your option) any later version.

Blue Cove is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Blue Cove; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

@version $Id$

*/

#include "stdafx.h"

#ifdef _WIN32_WCE
//swprintf_s on XP  _snwprintf on CE
#define swprintf_s _snwprintf
#define sprintf_s _snprintf
#define _vsnprintf_s _vsnprintf
#endif // #else // _WIN32_WCE

#define INQUIRY_COMPLETED 0
#define INQUIRY_TERMINATED 5
#define INQUIRY_ERROR 7

#define SERVICE_SEARCH_COMPLETED 1
#define SERVICE_SEARCH_TERMINATED 2
#define SERVICE_SEARCH_ERROR 3
#define SERVICE_SEARCH_NO_RECORDS 4
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE 6

static BOOL started;
static DWORD dllWSAStartupError = 0;
static HANDLE hLookup;
static CRITICAL_SECTION csLookup;

static BOOL nativeDebugCallback= false;
static jclass nativeDebugListenerClass = NULL;
static jmethodID nativeDebugMethod = NULL;

void callDebugListener(JNIEnv *env, int lineN, const char *fmt, ...);
#define debug(fmt) callDebugListener(env, __LINE__, fmt);
#define debugs(fmt, message) callDebugListener(env, __LINE__, fmt, message);
#define debugss(fmt, message1, message2) callDebugListener(env, __LINE__, fmt, message1, message2);

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
			hLookup = NULL;
            //debug("InitializeCriticalSection");
			InitializeCriticalSection(&csLookup);
			return started;
		}
	case DLL_THREAD_ATTACH:
		break;
	case DLL_THREAD_DETACH:
		break;
	case DLL_PROCESS_DETACH:
		if (started) {
			WSACleanup();
		}
		DeleteCriticalSection(&csLookup);
		break;
//	default:
	    //debug("DllMain default %d", ul_reason_for_call);
	}
	return TRUE;
}

void throwException(JNIEnv *env, const char *name, const char *msg)
{
	 debugss("Throw Exception %s %s", name, msg);
	 jclass cls = env->FindClass(name);
     /* if cls is NULL, an exception has already been thrown */
     if (cls != NULL) {
         env->ThrowNew(cls, msg);
	 } else {
		 env->FatalError("Illegal Exception name");
	 }
     /* free the local ref */
    env->DeleteLocalRef(cls);
}

void throwIOException(JNIEnv *env, const char *msg)
{
	throwException(env, "java/io/IOException", msg);
}

WCHAR *GetWSAErrorMessage(DWORD last_error)
{
	static WCHAR errmsg[1024];
	if (!FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM,
		0,
		last_error,
		0,
		errmsg,
		511,
		NULL))
	{
		swprintf_s(errmsg, 1024, _T("No error message for code %d"), last_error);
		return errmsg;
	}
	size_t last = wcslen(errmsg) - 1;
	while ((errmsg[last] == '\n') || (errmsg[last] == '\r')) {
		errmsg[last] = 0;
		last --;
	}
	return errmsg;
}

void throwIOExceptionWSAGetLastError(JNIEnv *env, const char *msg)
{
	char errmsg[1064];
	DWORD last_error = WSAGetLastError();
	sprintf_s(errmsg, 1064, "%s [%d] %S", msg, last_error, GetWSAErrorMessage(last_error));
    throwIOException(env, errmsg);
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_initializationStatus(JNIEnv *env, jobject peer) {
    if (started) {
        return 1;
    } else {
        char errmsg[1064];
	    sprintf_s(errmsg, 1064, "Initialization error [%d] %S", dllWSAStartupError, GetWSAErrorMessage(dllWSAStartupError));
        throwIOException(env, errmsg);
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_enableNativeDebug(JNIEnv *env, jobject peer, jboolean on) {
	if (on) {
		nativeDebugListenerClass = env->FindClass("com/intel/bluetooth/BluetoothPeer");
		if (nativeDebugListenerClass != NULL) {
			nativeDebugMethod = env->GetStaticMethodID(nativeDebugListenerClass, "nativeDebugCallback", "(ILjava/lang/String;)V");
			if (nativeDebugMethod != NULL) {
				nativeDebugCallback = true;
				debug("nativeDebugCallback ON");
			}
		}
	} else {
		nativeDebugCallback = false;
	}
}

void callDebugListener(JNIEnv *env, int lineN, const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	{
		if (nativeDebugCallback) {
			char msg[1064];
			_vsnprintf_s(msg, 1064, fmt, ap);
			env->CallVoidMethod(nativeDebugListenerClass, nativeDebugMethod, lineN, env->NewStringUTF(msg));
		}
	}
	va_end(ap);
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    doInquiry
* Signature: (ILjavax/bluetooth/DiscoveryListener;)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_doInquiry(JNIEnv *env, jobject peer, jint accessCode, jobject listener)
{
    jclass clsRemoteDevice = NULL;
    jclass clsDeviceClass = NULL;

    debug("->doInquiry");
	// build device query

#ifndef _WIN32_WCE
	BTH_QUERY_DEVICE query;
	query.LAP = 0;
#else
	BTHNS_INQUIRYBLOB query;
	query.LAP = accessCode;
	query.num_responses = 10;
#endif
	query.length = 10;

	// build BLOB pointing to device query

	BLOB blob;

	blob.cbSize = sizeof(query);
	blob.pBlobData = (BYTE *)&query;

	// build query

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));
	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
	queryset.lpBlob = &blob;

#ifndef _WIN32_WCE
	queryset.lpBlob = &blob;
#endif

	// begin query

	EnterCriticalSection(&csLookup);

	if (hLookup != NULL) {
		LeaveCriticalSection(&csLookup);
		return INQUIRY_ERROR;
	}

#ifdef _WIN32_WCE
	if (WSALookupServiceBegin(&queryset, LUP_CONTAINERS, &hLookup)) {
#else
	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE|LUP_CONTAINERS, &hLookup)) {
#endif
		hLookup = NULL;

		LeaveCriticalSection(&csLookup);
		return INQUIRY_ERROR;
	}

	LeaveCriticalSection(&csLookup);

	// fetch results
    jint result;

	while(true) {
		union {
			CHAR buf[4096];
			SOCKADDR_BTH __unused;
		};

		memset(buf, 0, sizeof(buf));

		LPWSAQUERYSET pwsaResults = (LPWSAQUERYSET) buf;
		pwsaResults->dwSize = sizeof(WSAQUERYSET);
		pwsaResults->dwNameSpace = NS_BTH;

		DWORD size = sizeof(buf);

		EnterCriticalSection(&csLookup);

		if (hLookup == NULL) {
			LeaveCriticalSection(&csLookup);
			result = INQUIRY_TERMINATED;
			break;
		}
        debug("doInquiry, WSALookupServiceNext");
		if (WSALookupServiceNext(hLookup, LUP_RETURN_NAME|LUP_RETURN_ADDR|LUP_RETURN_BLOB, &size, (WSAQUERYSET *)buf)) {
			WSALookupServiceEnd(hLookup);

			hLookup = NULL;

			LeaveCriticalSection(&csLookup);

            debug("doInquiry, exits");

			switch(WSAGetLastError()) {
			    case WSAENOMORE:
			    case WSA_E_NO_MORE:
				    result = INQUIRY_COMPLETED;
			    default:
				    result = INQUIRY_ERROR;
			}
			break;
		}

		LeaveCriticalSection(&csLookup);

        debug("doInquiry, has next Service");

#ifdef _WIN32_WCE
		BthInquiryResult *p_inqRes = (BthInquiryResult *)pwsaResults->lpBlob->pBlobData;

#else
		BTH_DEVICE_INFO *p_inqRes = (BTH_DEVICE_INFO *)pwsaResults->lpBlob->pBlobData;
#endif

		// get device name

		WCHAR name[256];
		BOOL bHaveName = pwsaResults->lpszServiceInstanceName && *(pwsaResults->lpszServiceInstanceName);
		StringCchPrintf(name, sizeof(name),L"%s",bHaveName ? pwsaResults->lpszServiceInstanceName : L"");

        debugs("ServiceInstanceName [%S]", name);

		// create remote device
		if (clsRemoteDevice == NULL) {
		    clsRemoteDevice = env->FindClass("javax/bluetooth/RemoteDevice");
		    if (clsRemoteDevice == NULL) {
		        env->FatalError("Can't create RemoteDevice Class");
		        result = INQUIRY_ERROR;
		        break;
		    }
	    }

		jobject dev = env->NewObject(clsRemoteDevice, env->GetMethodID(clsRemoteDevice, "<init>", "(Ljava/lang/String;J)V"), env->NewString((jchar*)name, (jsize)wcslen(name)), ((SOCKADDR_BTH *)pwsaResults->lpcsaBuffer->RemoteAddr.lpSockaddr)->btAddr);

		// create device class
        if (clsDeviceClass == NULL) {
		    clsDeviceClass = env->FindClass("javax/bluetooth/DeviceClass");
		    if (clsDeviceClass == NULL) {
		        env->FatalError("Can't create DeviceClass Class");
		        result = INQUIRY_ERROR;
		        break;
		    }
		}

#ifdef _WIN32_WCE
		int classOfDev = p_inqRes->cod;
#else
		int classOfDev = p_inqRes->classOfDevice;
#endif
		jobject cod = env->NewObject(clsDeviceClass, env->GetMethodID(clsDeviceClass, "<init>", "(I)V"), classOfDev);

		// notify listener
        debug("doInquiry, notify listener");
		env->CallVoidMethod(listener, env->GetMethodID(env->GetObjectClass(listener), "deviceDiscovered", "(Ljavax/bluetooth/RemoteDevice;Ljavax/bluetooth/DeviceClass;)V"), dev, cod);
		if (env->ExceptionCheck()) {
		    result = INQUIRY_ERROR;
		    break;
		}
		debug("doInquiry, listener returns");
	}

	if (clsDeviceClass != NULL) {
	  env->DeleteLocalRef(clsDeviceClass);
	}
	if (clsRemoteDevice != NULL) {
	  env->DeleteLocalRef(clsRemoteDevice);
	}
	return result;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    cancelInquiry
* Signature: ()Z
*/
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry(JNIEnv *env, jobject peer)
{
	debug("->cancelInquiry");
	EnterCriticalSection(&csLookup);

	if (hLookup == NULL) {
		LeaveCriticalSection(&csLookup);

		return JNI_FALSE;
	}

	WSALookupServiceEnd(hLookup);

	hLookup = NULL;

	LeaveCriticalSection(&csLookup);

	return JNI_TRUE;
}

static void convertBytesToUUID(jbyte *bytes, GUID *uuid)
{
	uuid->Data1 = bytes[0]<<24&0xff000000|bytes[1]<<16&0x00ff0000|bytes[2]<<8&0x0000ff00|bytes[3]&0x000000ff;
	uuid->Data2 = bytes[4]<<8&0xff00|bytes[5]&0x00ff;
	uuid->Data3 = bytes[6]<<8&0xff00|bytes[7]&0x00ff;

	for(int i = 0; i < 8; i++)
		uuid->Data4[i] = bytes[i+8];
}

JNIEXPORT jintArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_getServiceHandles(JNIEnv *env, jobject peer, jobjectArray uuidSet, jlong address)
{
	debug("->getServiceHandles");
	// 	check if we can handle the number of UUIDs supplied

	if (env->GetArrayLength(uuidSet) > MAX_UUIDS_IN_QUERY)
		return NULL;

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

	for(int i = 0; i < env->GetArrayLength(uuidSet); i++) {
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

		convertBytesToUUID(bytes, &guid);

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

	HANDLE hLookup;

	// begin query

#ifdef _WIN32_WCE
	if (WSALookupServiceBegin(&queryset, 0, &hLookup))
	return NULL;
#else
	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE, &hLookup))
		return NULL;
#endif
	// fetch results

	char buf[4096];

	memset(buf, 0, sizeof(buf));
	LPWSAQUERYSET pwsaResults = (LPWSAQUERYSET) buf;
	pwsaResults->dwSize = sizeof(WSAQUERYSET);
	pwsaResults->dwNameSpace = NS_BTH;
	pwsaResults->lpBlob = NULL;

	DWORD size = sizeof(buf);

#ifdef _WIN32_WCE
	if (WSALookupServiceNext(hLookup, 0, &size, pwsaResults)) {
#else
	if (WSALookupServiceNext(hLookup, LUP_RETURN_BLOB, &size, pwsaResults)) {
#endif
		switch(WSAGetLastError()) {
		case WSANO_DATA:
			return env->NewIntArray(0);

		default:
			WSALookupServiceEnd(hLookup);
			return NULL;
		}
	}

	WSALookupServiceEnd(hLookup);

	// construct int array to hold handles

	jintArray result = env->NewIntArray(pwsaResults->lpBlob->cbSize/sizeof(ULONG));

	jint *ints = env->GetIntArrayElements(result, 0);

	memcpy(ints, pwsaResults->lpBlob->pBlobData, pwsaResults->lpBlob->cbSize);

	env->ReleaseIntArrayElements(result, ints, 0);

	return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes(JNIEnv *env, jobject peer, jintArray attrIDs, jlong address, jint handle)
{
    debug("->getServiceAttributes");
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

	HANDLE hLookup;

	// begin query

#ifdef _WIN32_WCE
	if (WSALookupServiceBegin(&queryset, 0, &hLookup)) {
#else
	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE, &hLookup)) {
#endif
		free(queryservice);

		throwIOException(env, "Failed to begin attribute query");
		return NULL;
	}

	free(queryservice);

	// fetch results

	char buf[4096];

	memset(buf, 0, sizeof(buf));

	LPWSAQUERYSET pwsaResults = (LPWSAQUERYSET) buf;
	pwsaResults->dwSize = sizeof(WSAQUERYSET);
	pwsaResults->dwNameSpace = NS_BTH;
	pwsaResults->lpBlob = NULL;

	DWORD size = sizeof(buf);

#ifdef _WIN32_WCE
	if (WSALookupServiceNext(hLookup, 0, &size, pwsaResults)) {
#else
	if (WSALookupServiceNext(hLookup, LUP_RETURN_BLOB, &size, (WSAQUERYSET *)buf)) {
#endif
		WSALookupServiceEnd(hLookup);

		throwIOException(env, "Failed to perform attribute query");
		return NULL;
	}

	WSALookupServiceEnd(hLookup);

	// construct byte array to hold blob

	jbyteArray result = env->NewByteArray(pwsaResults->lpBlob->cbSize);

	jbyte *bytes = env->GetByteArrayElements(result, 0);

	memcpy(bytes, pwsaResults->lpBlob->pBlobData, pwsaResults->lpBlob->cbSize);

	env->ReleaseByteArrayElements(result, bytes, 0);

	return result;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    registerService
* Signature: ([B)I
*/

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_registerService(JNIEnv *env, jobject peer, jbyteArray record)
{
	debug("->registerService");
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
		free(setservice);
		throwIOException(env, "Failed to register service");
		return 0;
	}
	free(setservice);
	return (jlong)handle;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    unregisterService
* Signature: (I)V
*/

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_unregisterService(JNIEnv *env, jobject peer, jlong handle)
{
	debug("->unregisterService");
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
		throwIOException(env, "Failed to unregister service");
	}
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    socket
* Signature: (ZZ)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_socket(JNIEnv *env, jobject peer, jboolean authenticate, jboolean encrypt)
{
    debug("->socket");
	// create socket

	SOCKET s = socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM);

	if (s == INVALID_SOCKET) {
		throwIOException(env, "Failed to create socket");
		return 0;
	}

	// set socket options

	if (authenticate) {
		ULONG ul = TRUE;

		if (setsockopt(s, SOL_RFCOMM, SO_BTH_AUTHENTICATE, (char *)&ul, sizeof(ULONG))) {
			closesocket(s);
			throwIOException(env, "Failed to set authentication option");
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
			throwIOException(env, "Failed to set encryption option");
			return 0;
		}
	}
	return (jint)s;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    getsockaddress
* Signature: (I)J
*/

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_getsockaddress(JNIEnv *env, jobject peer, jint socket)
{
	debug("->getsockaddress");
	// get socket name

	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	if (getsockname(socket, (sockaddr *)&addr, &size)) {
		throwIOExceptionWSAGetLastError(env, "Failed to get socket name");
		return 0;
	}
	return addr.btAddr;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    getsockchannel
* Signature: (I)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_getsockchannel(JNIEnv *env, jobject peer, jint socket)
{
	debug("->getsockchannel");
	// get socket name

	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	if (getsockname(socket, (sockaddr *)&addr, &size)) {
		throwIOExceptionWSAGetLastError(env, "Failed to get socket name");
		return 0;
	}
	return addr.port;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    connect
* Signature: (IJI)V
*/

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_connect(JNIEnv *env, jobject peer, jint socket, jlong address, jint channel)
{
    debug("->connect");

	SOCKADDR_BTH addr;

	memset(&addr, 0, sizeof(SOCKADDR_BTH));

	addr.addressFamily = AF_BTH;
	addr.btAddr = address;
	addr.port = channel;

	if (connect((SOCKET)socket, (sockaddr *)&addr, sizeof(SOCKADDR_BTH))) {
		throwIOExceptionWSAGetLastError(env, "Failed to connect socket");
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_bind(JNIEnv *env, jobject peer, jint socket)
{
	// bind socket
	debug("->bind");

	SOCKADDR_BTH addr;
	memset(&addr, 0, sizeof(addr));
	addr.addressFamily = AF_BTH;
#ifdef _WIN32_WCE
	addr.port = 0;
#else
	addr.port = BT_PORT_ANY;
#endif
	if (bind(socket, (SOCKADDR *)&addr, sizeof(addr))) {
		closesocket(socket);
		throwIOExceptionWSAGetLastError(env, "Failed to bind socket");
		return;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_listen(JNIEnv *env, jobject peer, jint socket)
{
    debug("->listen");
	if (listen((SOCKET)socket, 10)) {
		//env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to listen socket");
		throwIOExceptionWSAGetLastError(env, "Failed to listen socket");
	}
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    accept
* Signature: (I)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_accept(JNIEnv *env, jobject peer, jint socket)
{
	debug("->accept");
	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	SOCKET s = accept((SOCKET)socket, (sockaddr *)&addr, &size);

	if (s == INVALID_SOCKET) {
		//env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to listen socket");
		throwIOException(env, "Failed to listen socket");
		return 0;
	}

	return (jint)s;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    recv_available
*
* Use to determine the amount of data pending in the network's input buffer that can be read from socket.
* returns the amount of data that can be read in a single call to the recv function, which may not be the same as 
* the total amount of data queued on the socket. 
*/
JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_recvAvailable(JNIEnv *env, jobject peer, jint socket)
{
	unsigned long arg = 0;
	if (ioctlsocket(socket, FIONREAD, &arg) != 0) {
		throwIOExceptionWSAGetLastError(env, "Failed to read available");
		return 0;
	}
	return arg;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    recv
* Signature: (I)I
*/
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_recv__I(JNIEnv *env, jobject peer, jint socket)
{
	debug("->recv(int)");
	unsigned char c;

	if (recv((SOCKET)socket, (char *)&c, 1, 0) != 1) {
		throwIOExceptionWSAGetLastError(env, "Failed to read");
		return 0;
	}
	return (int)c;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    recv
* Signature: (I[BII)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_recv__I_3BII(JNIEnv *env, jobject peer, jint socket, jbyteArray b, jint off, jint len)
{
	debug("->recv(int,byte[],int,int)");
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		int count = recv((SOCKET)socket, (char *)(bytes+off+done), len-done, 0);

		if (count <= 0) {
			env->ReleaseByteArrayElements(b, bytes, 0);

			if (done == 0) {
				throwIOExceptionWSAGetLastError(env, "Failed to read");
				return 0;
			} else
				return done;
		}

		done += count;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);

	return done;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    send
* Signature: (II)V
*/
JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_send__II(JNIEnv *env, jobject peer, jint socket, jint b)
{
	debug("->send(int,int)");
	char c = (char)b;

	if (send((SOCKET)socket, &c, 1, 0) != 1) {
		throwIOExceptionWSAGetLastError(env, "Failed to write");
	}
}
/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    send
* Signature: (I[BII)V
*/

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_send__I_3BII(JNIEnv *env, jobject peer, jint socket, jbyteArray b, jint off, jint len)
{
	debug("->send(int,byte[],int,int)");
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		int count = send((SOCKET)socket, (char *)(bytes+off+done), len-done, 0);

		if (count <= 0) {
			env->ReleaseByteArrayElements(b, bytes, 0);
			throwIOExceptionWSAGetLastError(env, "Failed to write");
			return;
		}

		done += count;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    close
* Signature: (I)V
*/
JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_close(JNIEnv *env, jobject peer, jint socket)
{
	debug("->close");
	if (closesocket((SOCKET)socket)) {
		throwIOExceptionWSAGetLastError(env, "Failed to close socket");
	}
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    getsockname
* Signature: (I)Ljava/lang/String;
*/
JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothPeer_getpeername(JNIEnv *env, jobject peer, jlong addr)
{
#ifdef _WIN32_WCE
	/*
	 * For the moment just return an empty string on Windows Mobile
	 * The next device scan will return a name anyway...
	 * To be modified later
	 */
	return env->NewStringUTF((char*)"");
#else

    debug("->getpeername");


	WSAQUERYSET querySet;
	memset(&querySet, 0, sizeof(querySet));
	querySet.dwSize = sizeof(querySet);
	querySet.dwNameSpace = NS_BTH;

	DWORD flags = LUP_RETURN_NAME |LUP_RETURN_ADDR | LUP_CONTAINERS;

	EnterCriticalSection(&csLookup);

	if (hLookup != NULL) {
		LeaveCriticalSection(&csLookup);
		throwIOException(env, "Another inquiry already running");
		return NULL;
	}

	if (WSALookupServiceBegin(&querySet, flags, &hLookup)) {
		hLookup = NULL;
		LeaveCriticalSection(&csLookup);
		throwIOException(env, (char*)GetWSAErrorMessage(GetLastError()));
		return NULL;
	}

	LeaveCriticalSection(&csLookup);

	while (true) {
		BYTE buffer[1000];
		DWORD bufferLength = sizeof(buffer);
		WSAQUERYSET *pResults = (WSAQUERYSET*)&buffer;

		EnterCriticalSection(&csLookup);
		if (WSALookupServiceNext(hLookup, flags, &bufferLength, pResults)) {
			WSALookupServiceEnd(hLookup);
			hLookup = NULL;
			LeaveCriticalSection(&csLookup);
			int err = GetLastError();
			switch(err) {
			case WSAENOMORE:
			case WSA_E_NO_MORE:
				break;
			default:
				throwIOException(env, (char*)GetWSAErrorMessage(GetLastError()));
				return NULL;
			}
		}

		LeaveCriticalSection(&csLookup);

		if (((SOCKADDR_BTH *)((CSADDR_INFO *)pResults->lpcsaBuffer)->RemoteAddr.lpSockaddr)->btAddr == addr) {
			EnterCriticalSection(&csLookup);
			WSALookupServiceEnd(hLookup);
			hLookup = NULL;
			LeaveCriticalSection(&csLookup);
			WCHAR *name = pResults->lpszServiceInstanceName;
			debugs("return %s", name);
			return env->NewString((jchar*)name, (jsize)wcslen(name));
		}
	} // while(true)
	//env->ThrowNew(env->FindClass("java/IO/IOException", "No name found"));
	debug("return empty");
	return env->NewStringUTF((char*)"");
#endif
}


JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_getpeeraddress(JNIEnv *env, jobject peer, jint socket)
{
	debug("->getpeeraddress");
	SOCKADDR_BTH addr;
	int size = sizeof(addr);
	if (getpeername((SOCKET) socket, (sockaddr*)&addr, &size)) {
		//env->ThrowNew(env->FindClass("java/io/IOException"), (char*)GetWSAErrorMessage(GetLastError()));
		throwIOException(env, (char*)GetWSAErrorMessage(GetLastError()));
		return 0;
	}
	return addr.btAddr;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothPeer_getradioname(JNIEnv *env, jobject peer, jlong address)
{
    debug("->getradioname");
// Unsupported for _WIN32_WCE for the moment...
#ifndef _WIN32_WCE
	HANDLE hRadio;
	BLUETOOTH_FIND_RADIO_PARAMS btfrp = { sizeof(btfrp) };
	HBLUETOOTH_RADIO_FIND hFind = BluetoothFindFirstRadio( &btfrp, &hRadio );

	if ( NULL != hFind )
	{
		do
		{
			BLUETOOTH_RADIO_INFO radioInfo;

			radioInfo.dwSize = sizeof(radioInfo);

			if (ERROR_SUCCESS == BluetoothGetRadioInfo(hRadio, &radioInfo))
			{
				if (radioInfo.address.ullLong == address) {
					BluetoothFindRadioClose(hFind);
					return env->NewString((jchar*)radioInfo.szName, (jsize) wcslen(radioInfo.szName));
				}
			}
		} while( BluetoothFindNextRadio( hFind, &hRadio ) );
		BluetoothFindRadioClose( hFind );
	}
#endif
	return NULL;
}