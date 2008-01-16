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

#include "ToshibaStack.h"

#ifdef VC6
#define CPP_FILE "ToshibaStack.cpp"
#endif

#ifndef _WIN32_WCE
BOOL isToshibaBluetoothStackPresent(JNIEnv *env) {
	//HMODULE h = LoadLibrary(TOSHIBA_DLL);
	//if (h == NULL) {
	//	return FALSE;
	//}
	//FreeLibrary(h);
	//return TRUE;
	return FALSE;
}
#endif

#ifdef BLUECOVE_TOSHIBA

#pragma comment(lib, "TosBtAPI.lib")

ToshibaStack* stack = NULL;


void tsAddrToString(wchar_t* addressString, BDADDR bd_addr) {
	swprintf_s(addressString, 14, L"%02x%02x%02x%02x%02x%02x",
			 bd_addr[0],
             bd_addr[1],
             bd_addr[2],
             bd_addr[3],
             bd_addr[4],
             bd_addr[5]);
}

char *getTsAPIStatusString(LONG lSts) {
	switch (lSts) {
		case TOSBTAPI_NO_ERROR :
			return "No errors";
		default:
			return "Unknown Toshiba error";
	}
}

void tsThrowBluetoothStateException(JNIEnv * env, LONG lSts) {
	throwBluetoothStateException(env, "Toshiba error# %i", lSts);
}

ToshibaStack::ToshibaStack() {
}

ToshibaStack::~ToshibaStack() {
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_getLibraryVersion
(JNIEnv *, jobject) {
	return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_detectBluetoothStack
(JNIEnv *env, jobject) {
	return detectBluetoothStack(env);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_enableNativeDebug
(JNIEnv *env, jobject, jclass loggerClass, jboolean on) {
	enableNativeDebug(env, loggerClass, on);
}

DWORD pid;

BOOL CALLBACK findMainWindowEnumWindowsProc(HWND hwnd, LPARAM lParam) {
	DWORD winProcessId = 0;
	GetWindowThreadProcessId(hwnd, &winProcessId);
	if (pid == winProcessId){
		*((HWND*)lParam) = hwnd;
		return FALSE;
	} else {
		return TRUE;
	}
}

HWND getAPPMainWindow() {
	pid = GetCurrentProcessId();
	HWND hMainWindow = 0;
	EnumWindows(findMainWindowEnumWindowsProc, (LPARAM)&hMainWindow);
	return hMainWindow;
}

char	szAppName[] = {"BlueCove"};

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_initializeImpl
(JNIEnv *env, jobject) {
	LONG	lSts;
	HWND hMainWindow = getAPPMainWindow();
	debug(("hMainWindow %i", hMainWindow);
	if (BtOpenAPI(hMainWindow, szAppName, &lSts) == FALSE) {
		tsThrowBluetoothStateException(env, lSts);
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_destroyImpl
(JNIEnv *, jobject) {
	LONG	lSts;
	BtCloseAPI(&lSts);
}

// --- LocalDevice

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_getLocalDeviceBluetoothAddress
(JNIEnv * env, jobject) {
	LONG	lSts;
	BTLOCALDEVINFO	inf;
	if (FALSE == BtGetLocalInfo(&inf, &lSts)) {
		tsThrowBluetoothStateException(env, lSts);
		return NULL;
	}
	wchar_t addressString[14];
	tsAddrToString(addressString, inf.BdAddr);
	return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}


#endif //  BLUECOVE_TOSHIBA
