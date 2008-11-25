/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @version $Id$
 */

#include "ToshibaStack.h"
#include <process.h>

#ifdef VC6
#define CPP_FILE "ToshibaStack.cpp"
#endif

#ifndef _WIN32_WCE
BOOL isToshibaBluetoothStackPresent(JNIEnv *env) {
	HMODULE h = LoadLibrary(TOSHIBA_DLL);
	if (h == NULL) {
		return FALSE;
	}
	FreeLibrary(h);
	return TRUE;
}
#endif

#ifdef BLUECOVE_TOSHIBA

#pragma comment(lib, "TosBtAPI.lib")


const UINT MSG_DISCOVERY_UPDATE   = WM_USER+1;


static TCHAR *title = L"BLCV";
static HWND hHiddenWindow;
static BTLOCALDEVINFO2 btLocalInfo;
static wchar_t addressString[14];
static bool btInited = false;
static bool btSuccess = false;
static HINSTANCE hInstance;


// discovery stuff
static HANDLE hDiscoveryComplete;
static bool discoveryStarted = false;
static bool discoveryCancelled = false;
static bool discoveryError = false;
static PBTDEVINFOLIST devInfo = NULL;


void tsAddrToString(wchar_t* addressString, BDADDR bd_addr) {
	swprintf_s(addressString, 14, L"%02x%02x%02x%02x%02x%02x",
			 bd_addr[0],
             bd_addr[1],
             bd_addr[2],
             bd_addr[3],
             bd_addr[4],
             bd_addr[5]);
}


jlong tsGetLongAddress(BDADDR addr)
{
	return 
		((jlong)addr[0] << 40) |
		((jlong)addr[1] << 32) |
		((jlong)addr[2] << 24) |
		((jlong)addr[3] << 16) |
		((jlong)addr[4] << 8) |
		((jlong)addr[5]);
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


LRESULT CALLBACK WndProcBt(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	LONG lStatus;

	switch (message) 
	{
		case WM_CREATE:
			btSuccess = btSuccess || BtOpenAPI(hWnd, "BLCV", &lStatus);
			if (btSuccess) {
				if (FALSE == BtGetLocalInfo2(&btLocalInfo, &lStatus)) {
					btSuccess = false;
				}
				else {
					tsAddrToString(addressString, btLocalInfo.BdAddr);
				}
			}
			btInited = true;
			return 0;
		case WM_DESTROY:
			btInited = false;
			btSuccess = false;
			BtCloseAPI(&lStatus);
			PostQuitMessage(0);
			return 0;
		case MSG_DISCOVERY_UPDATE: {
			switch (wParam) {
				case TOSBTAPI_NM_DISCOVERDEVICE_ERROR:
					BtCancelDiscoverRemoteDevice(&lStatus);
					discoveryError = true;
					SetEvent(hDiscoveryComplete);
					break;
				case TOSBTAPI_NM_DISCOVERDEVICE_END:
					SetEvent(hDiscoveryComplete);
					break;
			}
			return 0;
			}
	}

	return DefWindowProc(hWnd, message, wParam, lParam);
}


ATOM MyRegisterClassBt(HINSTANCE hInstance)
{
	WNDCLASSEX wcex;

	wcex.cbSize = sizeof(WNDCLASSEX); 

	wcex.style		= 0;
	wcex.lpfnWndProc	= (WNDPROC)WndProcBt;
	wcex.cbClsExtra		= 0;
	wcex.cbWndExtra		= 0;
	wcex.hInstance		= hInstance;
	wcex.hIcon		= NULL;
	wcex.hCursor		= 0;
	wcex.hbrBackground	= 0;
	wcex.lpszMenuName	= (TCHAR *)NULL;
	wcex.lpszClassName	= title;
	wcex.hIconSm		= NULL;

	return RegisterClassEx(&wcex);
}
	
static void dispatch_thread(void *ignored)
{
	MSG msg;

	hInstance = GetModuleHandle(NULL);

	wsprintf(title, L"BLCV");
	MyRegisterClassBt(hInstance);

	hHiddenWindow = CreateWindow(title, title, WS_OVERLAPPEDWINDOW,
		  CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, NULL, NULL,
		  hInstance, NULL);

	if (FAILED(hHiddenWindow)) {
		return;
	}

	while (1) {
		GetMessage(&msg, hHiddenWindow, 0, 0);
		DispatchMessage(&msg);
		if (!btInited)
			break;
	}
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_isNativeCodeLoaded
  (JNIEnv *env, jobject peer) {
    return JNI_TRUE;
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

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_initializeImpl
(JNIEnv *env, jobject) {

	_beginthread(dispatch_thread, 0, 0);

	while (!btInited) {
		Sleep(5);
	}

	if (FAILED(hHiddenWindow) || !btSuccess) {
		btInited = false;
		btSuccess = false;
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_destroyImpl
(JNIEnv *, jobject) {
	PostMessage(hHiddenWindow, WM_DESTROY, 0, 0);
}

// --- LocalDevice

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_getLocalDeviceBluetoothAddress
(JNIEnv * env, jobject) {
	return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}


// --- Device discovery

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {
	LONG lStatus;

 	if (discoveryStarted) {
		throwBluetoothStateException(env, cINQUIRY_RUNNING);
		return 0;
	}

	hDiscoveryComplete = CreateEvent(NULL, FALSE, FALSE, NULL);
	if (hDiscoveryComplete == NULL) {
		return INQUIRY_ERROR;
	}

	discoveryStarted = true;

	DeviceInquiryCallback discoveryCallback;

	if (!discoveryCallback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
		discoveryStarted = false;
		CloseHandle(hDiscoveryComplete);
		return INQUIRY_ERROR;
	}

	if (!discoveryCallback.callDeviceInquiryStartedCallback(env)) {
		discoveryStarted = false;
		CloseHandle(hDiscoveryComplete);
		return INQUIRY_ERROR;
	}

	if (devInfo) {
		BtMemFree(devInfo);
		devInfo = NULL;
	}

	BtDiscoverRemoteDevice2(&devInfo, TOSBTAPI_DD_NORMAL, &lStatus, hHiddenWindow, MSG_DISCOVERY_UPDATE, 0);
			
	if (lStatus < 0) {
		discoveryStarted = false;
		return INQUIRY_ERROR;
	}

	int idx = 0;

	while (1) {
		DWORD result = WaitForSingleObject(hDiscoveryComplete, 5);
		if ((result == WAIT_OBJECT_0 || result == WAIT_TIMEOUT) && devInfo) {
			for (int n = devInfo->dwDevListNum; idx < n; idx++) {
				PBTDEVINFO dev = &devInfo->DevInfo[idx];
				jboolean paired = false; // How do I get this on Toshiba stack?
				jlong addr = tsGetLongAddress(dev->BdAddr);
				if (!discoveryCallback.callDeviceDiscovered(env, listener, addr, dev->ClassOfDevice, env->NewStringUTF((char *)dev->FriendlyName), paired)) {
					BtCancelDiscoverRemoteDevice(&lStatus);
					discoveryError = true;
					SetEvent(hDiscoveryComplete);
				}
			}
			if (result == WAIT_OBJECT_0)
				break;
		}
		else if (result == WAIT_FAILED || result == WAIT_ABANDONED)
			break;
	}

	discoveryStarted = false;
	CloseHandle(hDiscoveryComplete);

	if (discoveryCancelled) {
		discoveryCancelled = false;
		return INQUIRY_TERMINATED;
	}
	else if (discoveryError) {
		discoveryError = false;
		return INQUIRY_ERROR;
	}
	else
		return INQUIRY_COMPLETED;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer) {
	if (discoveryStarted) {
		LONG lStatus;
		BtCancelDiscoverRemoteDevice(&lStatus);
		discoveryCancelled = true;
		SetEvent(hDiscoveryComplete);
	}

	return TRUE;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_peekRemoteDeviceFriendlyName
(JNIEnv *env, jobject, jlong address) {
	if (devInfo == NULL) {
		return NULL;
	}

	while (1) {
		int n = devInfo->dwDevListNum;
		for (int i = 0; i < n; i++) {
			jlong a = tsGetLongAddress(devInfo->DevInfo[i].BdAddr);
			if (a == address) {
                		return env->NewStringUTF((char*)devInfo->DevInfo[i].FriendlyName);
			}
		}
		if (discoveryStarted) {
			Sleep(200);
		}
		else {
			break;
		}
	}

	return NULL;
}


#endif //  BLUECOVE_TOSHIBA
