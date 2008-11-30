/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Trent Gamblin
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


// Toshiba stack not supported on CE
#ifndef _WIN32_WCE


#include "ToshibaStack.h"
#include <process.h>

#ifdef VC6
#define CPP_FILE "ToshibaStack.cpp"
#endif

BOOL isToshibaBluetoothStackPresent(JNIEnv *env) {
	HMODULE h = LoadLibrary(TOSHIBA_DLL);
	if (h == NULL) {
		return FALSE;
	}
	FreeLibrary(h);
	return TRUE;
}

#ifdef BLUECOVE_TOSHIBA

#pragma comment(lib, "TosBtAPI.lib")


const UINT MSG_DISCOVERY_UPDATE         = WM_USER+1;
const UINT MSG_DISCOVER_NAME_UPDATE     = WM_USER+2;
const UINT MSG_SDP_CONNECT_UPDATE       = WM_USER+3;
const UINT MSG_SERVICE_SEARCH_UPDATE    = WM_USER+4;
const UINT MSG_SERVICE_ATTRIBUTE_UPDATE = WM_USER+5;


static TCHAR *title = L"BLCV";
static HWND hHiddenWindow;
static BTLOCALDEVINFO2 btLocalInfo;
static wchar_t addressString[14];
static bool btInited = false;
static bool btSuccess = false;
static HINSTANCE hInstance;


// discovery stuff
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


void tsGetBdAddr(jlong address, PBDADDR addr)
{
	addr[0] = (BYTE)(((jlong)address >> 40) & 0xFF);
	addr[1] = (BYTE)(((jlong)address >> 32) & 0xFF);
	addr[2] = (BYTE)(((jlong)address >> 24) & 0xFF);
	addr[3] = (BYTE)(((jlong)address >> 16) & 0xFF);
	addr[4] = (BYTE)(((jlong)address >>  8) & 0xFF);
	addr[5] = (BYTE)(((jlong)address >>  0) & 0xFF);
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
			btSuccess = btSuccess || BtExecBtMng(&lStatus);
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
			HANDLE h = (HANDLE)lParam;
			switch (wParam) {
				case TOSBTAPI_NM_DISCOVERDEVICE_ERROR:
					BtCancelDiscoverRemoteDevice(&lStatus);
					discoveryError = true;
					SetEvent(h);
					break;
				case TOSBTAPI_NM_DISCOVERDEVICE_END:
					SetEvent(h);
					break;
			}
			return 0;
			}
		case MSG_DISCOVER_NAME_UPDATE: {
			HANDLE h = (HANDLE)lParam;
			switch (wParam) {
				case TOSBTAPI_NM_DISCOVERNAME_ERROR:
				case TOSBTAPI_NM_DISCOVERNAME_END:
					SetEvent(h);
					break;
			}
			return 0;
			}
		case MSG_SDP_CONNECT_UPDATE: {
			HANDLE h = (HANDLE)lParam;
			switch (wParam) {
				case TOSBTAPI_NM_CONNECTSDP_ERROR:
				case TOSBTAPI_NM_CONNECTSDP_END:
					SetEvent(h);
					break;
			}
			return 0;
			}
		case MSG_SERVICE_SEARCH_UPDATE: {
			HANDLE h = (HANDLE)lParam;
			switch (wParam) {
				case TOSBTAPI_NM_SS_ERROR:
				case TOSBTAPI_NM_SS_END:
					SetEvent(h);
					break;
			}
			return 0;
		}
		case MSG_SERVICE_ATTRIBUTE_UPDATE: {
			HANDLE h = (HANDLE)lParam;
			switch (wParam) {
				case TOSBTAPI_NM_SA_START:
				case TOSBTAPI_NM_SA_RESULT:
					break;
				default:
					SetEvent(h);
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

	wcex.style			= 0;
	wcex.lpfnWndProc	= (WNDPROC)WndProcBt;
	wcex.cbClsExtra		= 0;
	wcex.cbWndExtra		= 0;
	wcex.hInstance		= hInstance;
	wcex.hIcon			= NULL;
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

	HANDLE hDiscoveryComplete;

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

	BtDiscoverRemoteDevice2(&devInfo, TOSBTAPI_DD_NORMAL, &lStatus, hHiddenWindow, MSG_DISCOVERY_UPDATE, (LPARAM)hDiscoveryComplete);
			
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
	}

	return TRUE;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_getRemoteDeviceFriendlyNameImpl
  (JNIEnv *env, jobject, jlong address)
{
	HANDLE hDiscoverName;

	hDiscoverName = CreateEvent(NULL, FALSE, FALSE, NULL);
	if (hDiscoverName == NULL) {
		return NULL;
	}

	BDADDR addr;
	FRIENDLYNAME name;
	LONG lStatus;

	tsGetBdAddr(address, addr);

	BtDiscoverRemoteName(addr, name, &lStatus, hHiddenWindow, MSG_DISCOVER_NAME_UPDATE, (LPARAM)hDiscoverName);

	WaitForSingleObject(hDiscoverName, INFINITE);

	CloseHandle(hDiscoverName);

	if (lStatus >= 0) {
		return env->NewStringUTF((char *)name);
	}
	else {
		return NULL;
	}
}


// --- Service search


static LONG connectSDP(PBDADDR addr, PWORD pwCID)
{
	HANDLE hSDP;
	LONG status;

	hSDP = CreateEvent(NULL, FALSE, FALSE, NULL);

	BtConnectSDP(addr, pwCID, &status, hHiddenWindow, MSG_SDP_CONNECT_UPDATE, (LPARAM)hSDP);

	WaitForSingleObject(hSDP, INFINITE);

	CloseHandle(hSDP);

	return status;
}


JNIEXPORT jshort JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_connectSDPImpl
  (JNIEnv *env, jobject, jlong address)
{
	WORD cid;
	LONG status;
	BDADDR addr;

	tsGetBdAddr(address, addr);

	if ((status = connectSDP(addr, &cid)) < 0) {
		tsThrowBluetoothStateException(env, status);
	}

	return cid;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_disconnectSDPImpl
  (JNIEnv *, jobject, jshort cid)
{
	LONG status;
	BtDisconnectSDP(cid, &status);
}

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_searchServicesImpl
  (JNIEnv *env, jobject obj, jobject startedNotify, jshort cid, jobjectArray uuidSet)
{
	// Convert uuidSet array to Toshiba format
	int n = env->GetArrayLength(uuidSet);
	PBTUUIDLIST uuidList = (PBTUUIDLIST)(new BYTE[n*18+4]);
	uuidList->dwUUIDInfoNum = n;

	for (unsigned int i = 0; i < uuidList->dwUUIDInfoNum; i++) {
		jbyteArray barray = (jbyteArray)env->GetObjectArrayElement(uuidSet, i);
		jbyte *bytes = env->GetByteArrayElements(barray, false);
		for (int j = 0; j < 16; j++) {
			uuidList->BtUUIDInfo[i].BtUUID[j] = (BYTE)bytes[j];
		}
		uuidList->BtUUIDInfo[i].wUUIDType = 128; // 128 bit uuid
	}

	DWORD patternSize;
	PBYTE pattern;
	LONG status;

	if (BtMakeServiceSearchPattern2(uuidList, &patternSize, &pattern, &status) == false) {
		delete[] uuidList;
		tsThrowBluetoothStateException(env, status);
	}

	HANDLE hServiceSearch = CreateEvent(NULL, FALSE, FALSE, NULL);
	if (hServiceSearch == NULL) {
		delete[] uuidList;
		BtMemFree(pattern);
		tsThrowBluetoothStateException(env, TOSBTAPI_ERROR);
	}

	PBTSDPSSRESULT results;

	if (BtServiceSearch2(cid, patternSize, pattern, &results, &status, hHiddenWindow, MSG_SERVICE_SEARCH_UPDATE, (LPARAM)hServiceSearch) == FALSE) {
		CloseHandle(hServiceSearch);
		delete[] uuidList;
		BtMemFree(pattern);
		tsThrowBluetoothStateException(env, status);
	}

    jclass notifyClass = env->GetObjectClass(startedNotify);
    if (notifyClass == NULL) {
        throwRuntimeException(env, "Fail to get Object Class");
        return NULL;
    }
    jmethodID notifyMethod = env->GetMethodID(notifyClass, "searchServicesStartedCallback", "()V");
    if (notifyMethod == NULL) {
        throwRuntimeException(env, "Fail to get MethodID searchServicesStartedCallback");
        return NULL;
    }
    env->CallVoidMethod(startedNotify, notifyMethod);
    if (ExceptionCheckCompatible(env)) {
        return NULL;
    }

	WaitForSingleObject(hServiceSearch, INFINITE);

	CloseHandle(hServiceSearch);

	if (results == NULL || status < 0) {
		if (isDebugOn()) {
			debug(("Search services results == NULL or status < 0\n"));
		}
		delete[] uuidList;
		BtMemFree(pattern);
		if (results)
			BtMemFree(results);
		tsThrowBluetoothStateException(env, status);
	}

	int size = results->wServiceRecordCount;
	jlongArray la = env->NewLongArray(size);
	env->SetLongArrayRegion(la, 0, size, (jlong *)results->ulServiceRecordHandleList);

	delete[] uuidList;
	BtMemFree(pattern);
	BtMemFree(results);

	return la;
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothStackToshiba_populateWorkerImpl
  (JNIEnv *env, jobject, jshort cid, jlong handle, jintArray attrIDs)
{
	if (attrIDs == NULL) {
		return NULL;
	}

	int nAttr = env->GetArrayLength(attrIDs);
	BYTE *attrIdList = new BYTE[nAttr*4];
	jint *ids = env->GetIntArrayElements(attrIDs, false);
	int offs = 0;

	for (int i = 0; i < nAttr; i++) {
		jint id = ids[i];
		attrIdList[offs++] = (BYTE)((id >>  0) & 0xFF);
		attrIdList[offs++] = (BYTE)((id >>  8) & 0xFF);
		attrIdList[offs++] = (BYTE)((id >> 16) & 0xFF);
		attrIdList[offs++] = (BYTE)((id >> 24) & 0xFF);
	}

	HANDLE plock = CreateEvent(NULL, FALSE, FALSE, NULL);
	if (plock == NULL) {
		return NULL;
	}

	PBTSDPSARESULT result;
	LONG status;

	BtServiceAttribute2(cid, (unsigned long)handle, offs, attrIdList, &result, &status, hHiddenWindow, MSG_SERVICE_ATTRIBUTE_UPDATE, (LPARAM)plock);

	WaitForSingleObject(plock, INFINITE);

	CloseHandle(plock);
	delete[] attrIdList;

	if (status < 0)
		return NULL;

	jbyteArray b = env->NewByteArray(result->dwAttributeListSize);
	env->SetByteArrayRegion(b, 0, result->dwAttributeListSize, (const signed char *)result->bAttributeList);

	return b;
}

#endif //  BLUECOVE_TOSHIBA
#endif //  !_WIN32_WCE
