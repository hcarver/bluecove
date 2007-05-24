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

#ifndef _BLUESOLEIL
BOOL isBlueSoleilBluetoothStackPresent() {
	return FALSE;
}
#endif

#ifdef _BLUESOLEIL

// Should be installed to %ProgramFiles%\IVT Corporation\BlueSoleil\api
#pragma comment(lib, "btfunc.lib")
#include "bt_ui.h"
#include "com_intel_bluetooth_BluetoothStackBlueSoleil.h"

#define BLUESOLEIL_DLL "btfunc.dll"
// We specify which DLLs to delay load with the /delayload:btfunc.dll linker option

#define deviceRespondedMax 50
#define MAX_SERVICE_COUNT	100

void BsAddrToString(wchar_t* addressString, BYTE* address) {
	swprintf_s(addressString, 14, _T("%02x%02x%02x%02x%02x%02x"),
			 address[5],
             address[4],
             address[3],
             address[2],
             address[1],
             address[0]);
}

jlong BsAddrToLong(BYTE* address) {
	jlong l = 0;
	for (int i = 5; i >= 0; i--) {
		l = (l << 8) + address[i];
	}
	return l;
}

void LongToBsAddr(jlong addr, BYTE* address) {
	for (int i = 0; i < 6 ; i++) {
		address[i] = (UINT8)(addr & 0xFF);
		addr >>= 8;
	}
}

jint BsDeviceClassToInt(BYTE* devClass) {
	return (((devClass[0] << 8) + devClass[1]) << 8) + devClass[2];
}

static BOOL BlueSoleilStarted = FALSE;

BOOL isBlueSoleilBluetoothStackPresent() {
	HMODULE h = LoadLibrary(_T(BLUESOLEIL_DLL));
	if (h == NULL) {
		return FALSE;
	}
	return TRUE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_initialize
(JNIEnv *env, jobject) {
	if (BT_InitializeLibrary()) {
		BlueSoleilStarted = TRUE;
	} else {
		debug("Error in BlueSoleil InitializeLibrary");
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_uninitialize
(JNIEnv *env, jobject) {
	if (BlueSoleilStarted) {
		BlueSoleilStarted = FALSE;
		BT_UninitializeLibrary();
	}
}

BOOL BsGetLocalDeviceInfo (JNIEnv *env, DWORD dwMask, PBLUETOOTH_DEVICE_INFO_EX pDevInfo) {
	memset(pDevInfo, 0, sizeof(BLUETOOTH_DEVICE_INFO_EX));
	pDevInfo->dwSize = sizeof(BLUETOOTH_DEVICE_INFO_EX);
	DWORD dwResult = BT_GetLocalDeviceInfo(dwMask, pDevInfo);
	if (dwResult != BTSTATUS_SUCCESS) {
		debugs("BT_GetLocalDeviceInfo return  [%i]", dwResult);
		return FALSE;
	} else {
		return TRUE;
	}
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject) {
	BLUETOOTH_DEVICE_INFO_EX devInfo;
	BsGetLocalDeviceInfo(env, MASK_DEVICE_ADDRESS, &devInfo);
	wchar_t addressString[14];
	BsAddrToString(addressString, devInfo.address);
	return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getLocalDeviceName
(JNIEnv *env, jobject) {
	BLUETOOTH_DEVICE_INFO_EX devInfo;
	if (!BsGetLocalDeviceInfo(env, MASK_DEVICE_NAME, &devInfo)) {
		return NULL;
	}
	// For some reson devInfo.szName can't be used in call to JNI NewStringUTF
	char name[MAX_DEVICE_NAME_LENGTH];
	sprintf_s(name, MAX_DEVICE_NAME_LENGTH, "%s", devInfo.szName);
	return env->NewStringUTF(name);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getDeviceVersion
(JNIEnv *env, jobject) {
	BLUETOOTH_DEVICE_INFO_EX devInfo;
	BsGetLocalDeviceInfo(env, MASK_LMP_VERSION, &devInfo);
	return devInfo.wLmpSubversion;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getDeviceManufacturer
(JNIEnv *env, jobject) {
	BLUETOOTH_DEVICE_INFO_EX devInfo;
	BsGetLocalDeviceInfo(env, MASK_LMP_VERSION, &devInfo);
	return devInfo.wManuName;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getStackVersionInfo
(JNIEnv *, jobject) {
	return BT_GetVersion();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_isBlueSoleilStarted
(JNIEnv *, jobject, jint seconds) {
	return BT_IsBlueSoleilStarted(seconds);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_isBluetoothReady
(JNIEnv *, jobject, jint seconds) {
	return BT_IsBluetoothReady(seconds);
}

// --- Device Inquiry

//void BsOnDeviceResponded(PBLUETOOTH_DEVICE_INFO pDevInfo) {
//}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {

	// We do Asynchronous call and there are no way to see when inquiry is started.
	// TODO Create Thread here.

//	DWORD dwResult = BT_RegisterCallback(EVENT_INQUIRY_DEVICE_REPORT, &BsOnDeviceResponded);
//	if (dwResult != BTSTATUS_SUCCESS) {
//		//throwException(env, "javax/bluetooth/BluetoothStateException", "Can't RegisterCallback");
//		return INQUIRY_ERROR;
//	}

	jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return INQUIRY_ERROR;
	}

	jmethodID deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;)V");
	if (deviceDiscoveredCallbackMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID deviceInquiryStartedCallback");
		return INQUIRY_ERROR;
	}

	UCHAR  ucInqMode = INQUIRY_GENERAL_MODE;
	if (accessCode == LIAC) {
		ucInqMode = INQUIRY_LIMITED_MODE;
	}
	UCHAR ucInqLen = 0x08; //~~ 15 sec
	BLUETOOTH_DEVICE_INFO	lpDevsList[deviceRespondedMax] = {0};
	DWORD devsListLen = sizeof(BLUETOOTH_DEVICE_INFO) * deviceRespondedMax;

	DWORD dwResult = BT_InquireDevices(ucInqMode, ucInqLen, &devsListLen, lpDevsList);
	if (dwResult != BTSTATUS_SUCCESS) {
		return INQUIRY_ERROR;
	}

	for (DWORD i=0; i < ((devsListLen)/sizeof(BLUETOOTH_DEVICE_INFO)); i++) {
		BLUETOOTH_DEVICE_INFO *pDevice = (BLUETOOTH_DEVICE_INFO*)((UCHAR*)lpDevsList + i * sizeof(BLUETOOTH_DEVICE_INFO));
		jlong deviceAddr = BsAddrToLong(pDevice->address);
		jint deviceClass = BsDeviceClassToInt(pDevice->classOfDevice);


		BLUETOOTH_DEVICE_INFO_EX devInfo = {0};
		memcpy(&devInfo.address, pDevice->address, DEVICE_ADDRESS_LENGTH);
		devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO_EX);
		devInfo.szName[0] = '\0';
		BT_GetRemoteDeviceInfo(MASK_DEVICE_NAME, &devInfo);

		env->CallVoidMethod(peer, deviceDiscoveredCallbackMethod, listener, deviceAddr, deviceClass, env->NewStringUTF((char*)(devInfo.szName)));
		if (ExceptionCheckCompatible(env)) {
		   return INQUIRY_ERROR;
		}
	}

	return INQUIRY_COMPLETED;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_cancelInquirympl
(JNIEnv *env, jobject){
	DWORD dwResult = BT_CancelInquiry();
	if (dwResult != BTSTATUS_SUCCESS) {
		debugs("BT_CancelInquiry return  [%i]", dwResult);
		return FALSE;
	} else {
		return TRUE;
	}
}

// --- Service search

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_runSearchServicesImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jobject listener, jbyteArray uuidValue, jlong address, jobject device)  {

	BLUETOOTH_DEVICE_INFO devInfo={0};
	devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO);
	LongToBsAddr(address, devInfo.address);

	SPPEX_SERVICE_INFO sppex_svc_info[5];
	memset(&sppex_svc_info, 0, 5 * sizeof(SPPEX_SERVICE_INFO));
	DWORD dwLength = 5 * sizeof(SPPEX_SERVICE_INFO);
	sppex_svc_info[0].dwSize = sizeof(SPPEX_SERVICE_INFO);

	GUID service_guid;

	// pin array
	jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);

	// build UUID
	convertUUIDBytesToGUID(bytes, &service_guid);

	// unpin array
	env->ReleaseByteArrayElements(uuidValue, bytes, 0);

	memcpy(&(sppex_svc_info[0].serviceClassUuid128), &service_guid, sizeof(UUID));

	DWORD dwResult;
	dwResult = BT_SearchSPPExServices(&devInfo, &dwLength, sppex_svc_info);
	if (dwResult != BTSTATUS_SUCCESS)	{
		debugs("BT_SearchSPPExServices return  [%i]", dwResult);
		if (dwResult == BTSTATUS_SERVICE_NOT_EXIST) {
			return SERVICE_SEARCH_NO_RECORDS;
		} else {
			return SERVICE_SEARCH_ERROR;
		}
	}

	jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return SERVICE_SEARCH_ERROR;
	}

	jmethodID servicesFoundCallbackMethod = env->GetMethodID(peerClass, "servicesFoundCallback", "(Lcom/intel/bluetooth/SearchServicesThread;Ljavax/bluetooth/DiscoveryListener;Ljavax/bluetooth/RemoteDevice;Ljava/lang/String;[BII)V");
	if (servicesFoundCallbackMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID servicesFoundCallback");
		return SERVICE_SEARCH_ERROR;
	}
	for(DWORD i = 0; i < dwLength / sizeof(SPPEX_SERVICE_INFO); i++) {
		SPPEX_SERVICE_INFO* sr = &(sppex_svc_info[i]);
		if (sr->dwSDAPRecordHanlde == 0) {
			continue;
		}

		//printf("SDAP Record Handle:	%d\n", sppex_svc_info[i].dwSDAPRecordHanlde);
		//printf("Service Name:	%s\n", sppex_svc_info[i].szServiceName);
		//printf("Service Channel:	%02X\n", sppex_svc_info[i].ucServiceChannel);

		jbyteArray uuidValueFound = env->NewByteArray(16);
		jbyte *bytes = env->GetByteArrayElements(uuidValueFound, 0);

		GUID found_service_guid;
		memcpy(&found_service_guid, &(sr->serviceClassUuid128), sizeof(UUID));
		convertGUIDToUUIDBytes(&found_service_guid, bytes);

		env->ReleaseByteArrayElements(uuidValueFound, bytes, 0);


		//DiscoveryListener listener, RemoteDevice device, String serviceName, byte[] uuidValue, int channel
		env->CallVoidMethod(peer, servicesFoundCallbackMethod,
			startedNotify, listener, device, env->NewStringUTF((char*)(sr->szServiceName)), uuidValueFound, sr->ucServiceChannel, sr->dwSDAPRecordHanlde);

		if (ExceptionCheckCompatible(env)) {
		   return SERVICE_SEARCH_ERROR;
		}
	}

	return SERVICE_SEARCH_COMPLETED;
}

//	 --- Client RFCOMM connections

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfOpenImpl
(JNIEnv *env, jobject, jlong address, jbyteArray uuidValue) {

	BLUETOOTH_DEVICE_INFO devInfo={0};
	devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO);
	LongToBsAddr(address, devInfo.address);

	SPPEX_SERVICE_INFO svcInfo;
	svcInfo.dwSize = sizeof(SPPEX_SERVICE_INFO);

	GUID service_guid;

	// pin array
	jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);

	// build UUID
	convertUUIDBytesToGUID(bytes, &service_guid);

	// unpin array
	env->ReleaseByteArrayElements(uuidValue, bytes, 0);

	memcpy(&(svcInfo.serviceClassUuid128), &service_guid, sizeof(UUID));

	DWORD dwHandle;
	DWORD dwResult = BT_ConnectSPPExService(&devInfo, &svcInfo, &dwHandle);
	if (dwResult != BTSTATUS_SUCCESS)	{
		debugs("BT_SearchSPPExServices return  [%i]", dwResult);
		throwIOExceptionExt(env, "Can't connect SPP [%i]", dwResult);
		return NULL;
	}
	debugs("open COM port [%i]", (int)svcInfo.ucComIndex);
	char portString[20];
	_snprintf_s(portString, 20, "\\\\.\\COM%i", (int)svcInfo.ucComIndex);
	HANDLE hComPort;
	hComPort = CreateFileA(portString, GENERIC_READ | GENERIC_WRITE, 0, 0, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, 0);
	if (hComPort == INVALID_HANDLE_VALUE) {
		BT_DisconnectSPPExService(dwHandle);
		throwIOExceptionExt(env, "Can't open COM port [%s]", portString);
		return NULL;
	}

	jlongArray result = env->NewLongArray(2);
	jlong *longs = env->GetLongArrayElements(result, 0);
	longs[0] = (jlong)hComPort;
	longs[1] = dwHandle;
	env->ReleaseLongArrayElements(result, longs, 0);

	return result;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfCloseImpl
(JNIEnv *env, jobject, jlong comHandle, jlong connectionHandle) {
	CloseHandle((HANDLE)comHandle);
	BT_DisconnectSPPExService((DWORD)connectionHandle);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfRead__J
(JNIEnv *env, jobject peer, jlong handle) {
	HANDLE hComPort = (HANDLE)handle;

	unsigned char c;
	DWORD numberOfBytesRead;
	if (!ReadFile(hComPort, (char *)&c, 1, &numberOfBytesRead, NULL)) {
		throwIOException(env, "Failed to read");
	}
	if (numberOfBytesRead == 0) {
		return -1;
	}
	return (int)c;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfRead__J_3BII
(JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
	HANDLE hComPort = (HANDLE)handle;
	jbyte *bytes = env->GetByteArrayElements(b, 0);
	DWORD numberOfBytesRead;

	if (!ReadFile(hComPort, (void*)(bytes + off), len, &numberOfBytesRead, NULL)) {
		env->ReleaseByteArrayElements(b, bytes, 0);
		throwIOException(env, "Failed to read");
		return -1;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);

	if (numberOfBytesRead == 0) {
		return -1;
	}

	return numberOfBytesRead;

}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfReadAvailable
(JNIEnv *env, jobject peer, jlong handle) {
	return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfWrite__JI
(JNIEnv *env, jobject peer, jlong handle, jint b) {
	HANDLE hComPort = (HANDLE)handle;
	char c = (char)b;
	DWORD numberOfBytesWritten;
	if (!WriteFile(hComPort, &c, 1, &numberOfBytesWritten, NULL)) {
		throwIOException(env, "Failed to write");
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfWrite__J_3BII
(JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
	HANDLE hComPort = (HANDLE)handle;

	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while(done < len) {
		DWORD numberOfBytesWritten = 0;
		if (!WriteFile(hComPort, (char *)(bytes + off + done), len - done, &numberOfBytesWritten, NULL)) {
			throwIOException(env, "Failed to write");
		}
		if (numberOfBytesWritten <= 0) {
			env->ReleaseByteArrayElements(b, bytes, 0);
			throwIOException(env, "Failed to write");
			return;
		}

		done += numberOfBytesWritten;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}

#endif
