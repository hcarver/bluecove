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

#ifdef _BLUESOLEIL

#pragma comment(lib, "btfunc.lib")
#include "bt_ui.h"
#include "com_intel_bluetooth_BluetoothStackBlueSoleil.h"

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

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_initialize
(JNIEnv * env, jobject) {
	if (BT_InitializeLibrary()) {
		BlueSoleilStarted = TRUE;
	} else {
		debug("Error in BlueSoleil InitializeLibrary");
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
		debug("fatalerror");
		return INQUIRY_ERROR;
	}

	jmethodID deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;)V");
	if (deviceDiscoveredCallbackMethod == NULL) {
		debug("fatalerror");
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
	}

	return INQUIRY_COMPLETED;
}

// --- Service search

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_runSearchServicesImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jobject uuid, jlong address)  {

	BLUETOOTH_DEVICE_INFO devInfo={0};
	devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO);
	LongToBsAddr(address, devInfo.address);

	SPPEX_SERVICE_INFO	sppex_svc_info[5];
	memset(&sppex_svc_info, 0, 5 * sizeof(SPPEX_SERVICE_INFO));
	DWORD dwLength = 5*sizeof(SPPEX_SERVICE_INFO);
	sppex_svc_info[0].dwSize = sizeof(SPPEX_SERVICE_INFO);

	GUID service_guid;

	jclass clsUUID = env->FindClass("javax/bluetooth/UUID");
    if (clsUUID == NULL) {
		env->FatalError("Can't create UUID Class");
	    return NULL;
    }
	jbyteArray uuidValue = (jbyteArray)env->GetObjectField(uuid, env->GetFieldID(clsUUID, "uuidValue", "[B"));

	// pin array
	jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);

	// build UUID
	convertBytesToUUID(bytes, &service_guid);

	// unpin array
	env->ReleaseByteArrayElements(uuidValue, bytes, 0);
	env->DeleteLocalRef(clsUUID);

	memcpy(&(sppex_svc_info[0].serviceClassUuid128), &service_guid, sizeof(UUID));

	DWORD dwReusult;
	dwReusult = BT_SearchSPPExServices(&devInfo, &dwLength, sppex_svc_info);
	if (dwReusult == BTSTATUS_SUCCESS)	{
		for(DWORD i = 0; i < dwLength / sizeof(SPPEX_SERVICE_INFO); i++) {
			//printf("SDAP Record Handle:	%d\n", sppex_svc_info[i].dwSDAPRecordHanlde);
			//printf("Service Name:	%s\n", sppex_svc_info[i].szServiceName);
			//printf("Service Channel:	%02X\n", sppex_svc_info[i].ucServiceChannel);
		}
	}

}

//	 --- Client RFCOMM connections

#endif

