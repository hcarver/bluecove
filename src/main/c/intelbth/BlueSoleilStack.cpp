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

void BsAddrToString(wchar_t* addressString, BYTE* address) {
	swprintf_s(addressString, 14, _T("%02x%02x%02x%02x%02x%02x"),
			 address[5],
             address[4],
             address[3],
             address[2],
             address[1],
             address[0]);
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

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject) {
	BLUETOOTH_DEVICE_INFO_EX devInfo;
	memset(&devInfo, 0, sizeof(BLUETOOTH_DEVICE_INFO_EX));
	devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO_EX);
	DWORD dwResult = BT_GetLocalDeviceInfo(MASK_DEVICE_ADDRESS, &devInfo);
	if (dwResult != BTSTATUS_SUCCESS) {
		debugs("BT_GetLocalDeviceInfo return  [%i]", dwResult);
	}
	wchar_t addressString[14];
	BsAddrToString(addressString, devInfo.address);
	return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getLocalDeviceName
(JNIEnv *env, jobject) {
	BLUETOOTH_DEVICE_INFO_EX devInfo;
	memset(&devInfo, 0, sizeof(BLUETOOTH_DEVICE_INFO_EX));
	devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO_EX);
	DWORD dwResult = BT_GetLocalDeviceInfo(MASK_DEVICE_NAME, &devInfo);
	if (dwResult != BTSTATUS_SUCCESS) {
		debugs("BT_GetLocalDeviceInfo return  [%i]", dwResult);
		return NULL;
	}
	// For some reson devInfo.szName can't be used in call to JNI NewStringUTF
	char name[MAX_DEVICE_NAME_LENGTH];
	sprintf_s(name, MAX_DEVICE_NAME_LENGTH, "%s", devInfo.szName);
	return env->NewStringUTF(name);
}

#endif

