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
*/

#include "stdafx.h"

#define INQUIRY_COMPLETED 0
#define INQUIRY_TERMINATED 5
#define INQUIRY_ERROR 7

#define SERVICE_SEARCH_COMPLETED 1
#define SERVICE_SEARCH_TERMINATED 2
#define SERVICE_SEARCH_ERROR 3
#define SERVICE_SEARCH_NO_RECORDS 4
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE 6

static BOOL started;

static HANDLE hLookup;
static CRITICAL_SECTION csLookup;

BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved)
{
	switch(ul_reason_for_call) {
	case DLL_PROCESS_ATTACH:
		{
			WSADATA data;
			if (WSAStartup(MAKEWORD(2, 2), &data)) {
				started = FALSE;
				return FALSE;
			} else
				started = TRUE;

			hLookup = NULL;

			InitializeCriticalSection(&csLookup);
			break;
		}
	case DLL_THREAD_ATTACH:
		break;
	case DLL_THREAD_DETACH:
		break;
	case DLL_PROCESS_DETACH:
		if (started)
			WSACleanup();
		DeleteCriticalSection(&csLookup);
		break;
	}
	return TRUE;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    doInquiry
* Signature: (ILjavax/bluetooth/DiscoveryListener;)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_doInquiry(JNIEnv *env, jobject peer, jint accessCode, jobject listener)
{
	jclass cls;
#ifndef _WIN32_WCE
	/*
	build device query
	*/

	BTH_QUERY_DEVICE query;

	query.LAP = 0;
	query.length = 10;

	/*
	build BLOB pointing to device query
	*/
	BLOB blob;

	blob.cbSize = sizeof(BTH_QUERY_DEVICE);
	blob.pBlobData = (BYTE *)&query;
#endif
	/*
	build query
	*/

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;

#ifndef _WIN32_WCE
	queryset.lpBlob = &blob;
#endif
	/*
	begin query
	*/

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

	/*
	fetch results
	*/

	while(true) {
		union {
			CHAR buf[4096];
			SOCKADDR_BTH __unused;
		};

		memset(buf, 0, sizeof(buf));

		((WSAQUERYSET *)buf)->dwSize = sizeof(WSAQUERYSET);
		((WSAQUERYSET *)buf)->dwNameSpace = NS_BTH;

		DWORD size = sizeof(buf);

		EnterCriticalSection(&csLookup);

		if (hLookup == NULL) {
			LeaveCriticalSection(&csLookup);

			return INQUIRY_TERMINATED;
		}

		if (WSALookupServiceNext(hLookup, LUP_RETURN_NAME|LUP_RETURN_ADDR|LUP_RETURN_BLOB, &size, (WSAQUERYSET *)buf)) {
			WSALookupServiceEnd(hLookup);

			hLookup = NULL;

			LeaveCriticalSection(&csLookup);

			switch(WSAGetLastError()) {
			case WSAENOMORE:
			case WSA_E_NO_MORE:
				return INQUIRY_COMPLETED;
			default:
				return INQUIRY_ERROR;
			}
		}

		LeaveCriticalSection(&csLookup);

		/*
		get device name
		*/

		WCHAR *name = ((WSAQUERYSET *)buf)->lpszServiceInstanceName;
		/*
		create remote device
		*/

		cls = env->FindClass("javax/bluetooth/RemoteDevice");

		jobject dev = env->NewObject(cls, env->GetMethodID(cls, "<init>", "(Ljava/lang/String;J)V"), env->NewString((jchar*)name, (jsize)wcslen(name)), ((SOCKADDR_BTH *)((WSAQUERYSET *)buf)->lpcsaBuffer[0].RemoteAddr.lpSockaddr)->btAddr);

		/*
		create device class
		*/

		cls = env->FindClass("javax/bluetooth/DeviceClass");

		jobject cod = env->NewObject(cls, env->GetMethodID(cls, "<init>", "(I)V"), ((BTH_DEVICE_INFO *)((WSAQUERYSET *)buf)->lpBlob->pBlobData)->classOfDevice);

		/*
		notify listener
		*/

		env->CallVoidMethod(listener, env->GetMethodID(env->GetObjectClass(listener), "deviceDiscovered", "(Ljavax/bluetooth/RemoteDevice;Ljavax/bluetooth/DeviceClass;)V"), dev, cod);
	}
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    cancelInquiry
* Signature: ()Z
*/
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothPeer_cancelInquiry(JNIEnv *env, jobject peer)
{
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
	/*
	check if we can handle the number of UUIDs supplied
	*/

	if (env->GetArrayLength(uuidSet) > MAX_UUIDS_IN_QUERY)
		return NULL;

	/*
	generate a Bluetooth address string (WSAAddressToString doesn't work on WinCE)
	*/

	WCHAR addressString[20];

	wsprintf(addressString, _T("(%02x:%02x:%02x:%02x:%02x:%02x)"), (int)(address>>40&0xff), (int)(address>>32&0xff), (int)(address>>24&0xff), (int)(address>>16&0xff), (int)(address>>8&0xff), (int)(address&0xff));

	/*
	build service query
	*/

	BTH_QUERY_SERVICE queryservice;

	memset(&queryservice, 0, sizeof(BTH_QUERY_SERVICE));

	queryservice.type = SDP_SERVICE_SEARCH_REQUEST;

	for(int i = 0; i < env->GetArrayLength(uuidSet); i++) {
		jbyteArray uuidValue = (jbyteArray)env->GetObjectField(env->GetObjectArrayElement(uuidSet, i), env->GetFieldID(env->FindClass("javax/bluetooth/UUID"), "uuidValue", "[B"));

		/*
		pin array
		*/

		jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);

		/*
		build UUID
		*/

		convertBytesToUUID(bytes, &queryservice.uuids[i].u.uuid128);

		/*
		unpin array
		*/

		env->ReleaseByteArrayElements(uuidValue, bytes, 0);

		/*
		UUID is full 128 bits
		*/

		queryservice.uuids[i].uuidType = SDP_ST_UUID128;
	}

	/*
	build BLOB pointing to service query
	*/

	BLOB blob;

	blob.cbSize = sizeof(BTH_QUERY_SERVICE);
	blob.pBlobData = (BYTE *)&queryservice;

	/*
	build query
	*/

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
	queryset.lpszContext = addressString;
	queryset.lpBlob = &blob;

	HANDLE hLookup;

	/*
	begin query
	*/

	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE, &hLookup))
		return NULL;

	/*
	fetch results
	*/

	char buf[4096];

	memset(buf, 0, sizeof(buf));

	((WSAQUERYSET *)buf)->dwSize = sizeof(WSAQUERYSET);
	((WSAQUERYSET *)buf)->dwNameSpace = NS_BTH;

	DWORD size = sizeof(buf);

	if (WSALookupServiceNext(hLookup, LUP_RETURN_BLOB, &size, (WSAQUERYSET *)buf))
		switch(WSAGetLastError()) {
		case WSANO_DATA:
			return env->NewIntArray(0);

		default:
			WSALookupServiceEnd(hLookup);
			return NULL;
	}

	WSALookupServiceEnd(hLookup);

	/*
	construct int array to hold handles
	*/

	jintArray result = env->NewIntArray(((WSAQUERYSET *)buf)->lpBlob->cbSize/sizeof(ULONG));

	jint *ints = env->GetIntArrayElements(result, 0);

	memcpy(ints, ((WSAQUERYSET *)buf)->lpBlob->pBlobData, ((WSAQUERYSET *)buf)->lpBlob->cbSize);

	env->ReleaseIntArrayElements(result, ints, 0);

	return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothPeer_getServiceAttributes(JNIEnv *env, jobject peer, jintArray attrIDs, jlong address, jint handle)
{
	/*
	generate a Bluetooth address string (WSAAddressToString doesn't work on WinCE)
	*/

	WCHAR addressString[20];

	wsprintf(addressString, _T("(%02x:%02x:%02x:%02x:%02x:%02x)"), (int)(address>>40&0xff), (int)(address>>32&0xff), (int)(address>>24&0xff), (int)(address>>16&0xff), (int)(address>>8&0xff), (int)(address&0xff));

	/*
	build attribute query
	*/

	BTH_QUERY_SERVICE *queryservice = (BTH_QUERY_SERVICE *)malloc(sizeof(BTH_QUERY_SERVICE)+sizeof(SdpAttributeRange)*(env->GetArrayLength(attrIDs)-1));

	memset(queryservice, 0, sizeof(BTH_QUERY_SERVICE)-sizeof(SdpAttributeRange));

	queryservice->type = SDP_SERVICE_ATTRIBUTE_REQUEST;
	queryservice->serviceHandle = handle;
	queryservice->numRange = env->GetArrayLength(attrIDs);

	/*
	set attribute ranges
	*/

	jint *ints = env->GetIntArrayElements(attrIDs, 0);

	for(int i = 0; i < env->GetArrayLength(attrIDs); i++) {
		queryservice->pRange[i].minAttribute = (USHORT)ints[i];
		queryservice->pRange[i].maxAttribute = (USHORT)ints[i];
	}

	env->ReleaseIntArrayElements(attrIDs, ints, 0);

	/*
	build BLOB pointing to attribute query
	*/

	BLOB blob;

	blob.cbSize = sizeof(BTH_QUERY_SERVICE);
	blob.pBlobData = (BYTE *)queryservice;

	/*
	build query
	*/

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH;
	queryset.lpszContext = addressString;
	queryset.lpBlob = &blob;

	HANDLE hLookup;

	/*
	begin query
	*/

	if (WSALookupServiceBegin(&queryset, LUP_FLUSHCACHE, &hLookup)) {
		free(queryservice);

		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to begin attribute query");
		return NULL;
	}

	free(queryservice);

	/*
	fetch results
	*/

	char buf[4096];

	memset(buf, 0, sizeof(buf));

	((WSAQUERYSET *)buf)->dwSize = sizeof(WSAQUERYSET);
	((WSAQUERYSET *)buf)->dwNameSpace = NS_BTH;

	DWORD size = sizeof(buf);

	if (WSALookupServiceNext(hLookup, LUP_RETURN_BLOB, &size, (WSAQUERYSET *)buf)) {
		WSALookupServiceEnd(hLookup);

		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to perform attribute query");
		return NULL;
	}

	WSALookupServiceEnd(hLookup);

	/*
	construct byte array to hold blob
	*/

	jbyteArray result = env->NewByteArray(((WSAQUERYSET *)buf)->lpBlob->cbSize);

	jbyte *bytes = env->GetByteArrayElements(result, 0);

	memcpy(bytes, ((WSAQUERYSET *)buf)->lpBlob->pBlobData, ((WSAQUERYSET *)buf)->lpBlob->cbSize);

	env->ReleaseByteArrayElements(result, bytes, 0);

	return result;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    registerService
* Signature: ([B)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_registerService(JNIEnv *env, jobject peer, jbyteArray record)
{
	int length = env->GetArrayLength(record);

	HANDLE handle = NULL;

	/*
	build service set
	*/

	ULONG version = BTH_SDP_VERSION;

	BTH_SET_SERVICE *setservice = (BTH_SET_SERVICE *)malloc(sizeof(BTH_SET_SERVICE)+length-1);

	memset(setservice, 0, sizeof(BTH_SET_SERVICE)-1);

	setservice->pSdpVersion = &version;
	setservice->pRecordHandle = &handle;
	setservice->ulRecordLength = length;

	jbyte *bytes = env->GetByteArrayElements(record, 0);

	memcpy(setservice->pRecord, bytes, length);

	env->ReleaseByteArrayElements(record, bytes, 0);

	/*
	build BLOB pointing to service set
	*/

	BLOB blob;

	blob.cbSize = sizeof(BTH_SET_SERVICE);
	blob.pBlobData = (BYTE *)setservice;

	/*
	build set
	*/

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH; 
	queryset.lpBlob = &blob;

	/*
	perform set
	*/

	if (WSASetService(&queryset, RNRSERVICE_REGISTER, 0)) {
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to register service");
		return 0;
	}

	return (jint)handle;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    unregisterService
* Signature: (I)V
*/

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_unregisterService(JNIEnv *env, jobject peer, jint handle)
{
	/*
	build service set
	*/

	ULONG version = BTH_SDP_VERSION;

	BTH_SET_SERVICE setservice;

	memset(&setservice, 0, sizeof(BTH_SET_SERVICE));

	setservice.pSdpVersion = &version;
	setservice.pRecordHandle = (HANDLE *)&handle;

	/*
	build BLOB pointing to service set
	*/

	BLOB blob;

	blob.cbSize = sizeof(BTH_SET_SERVICE);
	blob.pBlobData = (BYTE *)&setservice;

	/*
	build set
	*/

	WSAQUERYSET queryset;

	memset(&queryset, 0, sizeof(WSAQUERYSET));

	queryset.dwSize = sizeof(WSAQUERYSET);
	queryset.dwNameSpace = NS_BTH; 
	queryset.lpBlob = &blob;

	/*
	perform set
	*/

	if (WSASetService(&queryset, RNRSERVICE_DELETE, 0))
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to unregister service");
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    socket
* Signature: (ZZ)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_socket(JNIEnv *env, jobject peer, jboolean authenticate, jboolean encrypt)
{
	/*
	create socket
	*/

	SOCKET s = socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM);

	if (s == INVALID_SOCKET) {
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to create socket");
		return 0;
	}

	/*
	set socket options
	*/

	if (authenticate) {
		ULONG ul = TRUE;

		if (setsockopt(s, SOL_RFCOMM, SO_BTH_AUTHENTICATE, (char *)&ul, sizeof(ULONG))) {
			closesocket(s);

			env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to set authentication option");
			return 0;
		}
	}

	if (encrypt) {
		ULONG ul = TRUE;

		if (setsockopt(s, SOL_RFCOMM, SO_BTH_ENCRYPT, (char *)&ul, sizeof(ULONG))) {
			closesocket(s);

			env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to set encryption option");
			return 0;
		}
	}

	/*
	bind socket
	*/

	SOCKADDR_BTH addr;

	memset(&addr, 0, sizeof(SOCKADDR_BTH));

	addr.addressFamily = AF_BTH;
	addr.port = BT_PORT_ANY;

	if (bind(s, (sockaddr*)&addr, sizeof(SOCKADDR_BTH))) {
		closesocket(s);

		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to bind socket");
		return 0;
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
	/*
	get socket name
	*/

	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	if (getsockname(socket, (sockaddr *)&addr, &size)) {
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to get socket name");
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
	/*
	get socket name
	*/

	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	if (getsockname(socket, (sockaddr *)&addr, &size)) {
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to get socket name");
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
	/* 
	connect
	*/

	SOCKADDR_BTH addr;

	memset(&addr, 0, sizeof(SOCKADDR_BTH));

	addr.addressFamily = AF_BTH;
	addr.btAddr = address;
	addr.port = channel;

	if (connect((SOCKET)socket, (sockaddr *)&addr, sizeof(SOCKADDR_BTH)))
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to connect socket");
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    listen
* Signature: (I)V
*/

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_listen(JNIEnv *env, jobject peer, jint socket)
{
	/*
	listen
	*/

	if (listen((SOCKET)socket, 10))
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to listen socket");
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    accept
* Signature: (I)I
*/

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_accept(JNIEnv *env, jobject peer, jint socket)
{
	SOCKADDR_BTH addr;

	int size = sizeof(SOCKADDR_BTH);

	SOCKET s = accept((SOCKET)socket, (sockaddr *)&addr, &size);

	if (s == INVALID_SOCKET) {
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to listen socket");
		return 0;
	}

	return (jint)s;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    recv
* Signature: (I)I
*/
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothPeer_recv__I(JNIEnv *env, jobject peer, jint socket)
{
	unsigned char c;

	if (recv((SOCKET)socket, (char *)&c, 1, 0) != 1) {
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to read");
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
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		int count = recv((SOCKET)socket, (char *)(bytes+off+done), len-done, 0);

		if (count <= 0) {
			env->ReleaseByteArrayElements(b, bytes, 0);

			if (done == 0) {
				env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to write");
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
	char c = (char)b;

	if (send((SOCKET)socket, &c, 1, 0) != 1)
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to write");
}
/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    send
* Signature: (I[BII)V
*/

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothPeer_send__I_3BII(JNIEnv *env, jobject peer, jint socket, jbyteArray b, jint off, jint len)
{
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		int count = send((SOCKET)socket, (char *)(bytes+off+done), len-done, 0);

		if (count <= 0) {
			env->ReleaseByteArrayElements(b, bytes, 0);

			env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to write");
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
	if (closesocket((SOCKET)socket))
		env->ThrowNew(env->FindClass("java/io/IOException"), "Failed to close socket");
}

WCHAR *GetWSAErrorMessage(DWORD last_error)
{
	static WCHAR errmsg[512];

	if (!FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM, 
		0,
		last_error,
		0,
		errmsg, 
		511,
		NULL))
	{
		/* if we fail, call ourself to find out why and return that error */
		return (GetWSAErrorMessage(GetLastError()));  
	}

	return errmsg;
}

/*
* Class:     com_intel_bluetooth_BluetoothPeer
* Method:    getsockname
* Signature: (I)Ljava/lang/String;
*/
JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothPeer_getpeername(JNIEnv *env, jobject peer, jlong addr)
{
	WSAQUERYSET querySet;
	memset(&querySet, 0, sizeof(querySet));
	querySet.dwSize = sizeof(querySet);
	querySet.dwNameSpace = NS_BTH;

	DWORD flags = LUP_RETURN_NAME |LUP_RETURN_ADDR | LUP_CONTAINERS;

	EnterCriticalSection(&csLookup);

	if (hLookup != NULL) {
		LeaveCriticalSection(&csLookup);
		env->ThrowNew(env->FindClass("java/IO/IOException"), "Another inquiry already running");
	}

	if (WSALookupServiceBegin(&querySet, flags, &hLookup)) {
		hLookup = NULL;
		LeaveCriticalSection(&csLookup);
		env->ThrowNew(env->FindClass("java/IO/IOException"), 
			(char*)GetWSAErrorMessage(GetLastError()));
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
				env->ThrowNew(env->FindClass("java/io/IOException"), 
					(char*)GetWSAErrorMessage(GetLastError()));
			}
		}

		LeaveCriticalSection(&csLookup);

		if (((SOCKADDR_BTH *)((CSADDR_INFO *)pResults->lpcsaBuffer)->RemoteAddr.lpSockaddr)->btAddr == addr) {
			EnterCriticalSection(&csLookup);
			WSALookupServiceEnd(hLookup);
			hLookup = NULL;
			LeaveCriticalSection(&csLookup);
			WCHAR *name = pResults->lpszServiceInstanceName;
			return env->NewString((jchar*)name, wcslen(name));
		}
	} // while(true)
	//env->ThrowNew(env->FindClass("java/IO/IOException", "No name found"));
	return env->NewStringUTF((char*)"");
}


JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothPeer_getpeeraddress(JNIEnv *env, jobject peer, jint socket)
{
	SOCKADDR_BTH addr;
	int size = sizeof(addr);
	if (getpeername((SOCKET) socket, (sockaddr*)&addr, &size)) {
		env->ThrowNew(env->FindClass("java/io/IOException"), (char*)GetWSAErrorMessage(GetLastError()));
		return 0;
	}
	return addr.btAddr;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothPeer_getradioname(JNIEnv *env, jobject peer, jlong address)
{
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

	return NULL;
}