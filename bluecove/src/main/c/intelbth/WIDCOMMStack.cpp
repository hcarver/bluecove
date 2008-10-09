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

#include "WIDCOMMStack.h"
#include "com_intel_bluetooth_NativeTestInterfaces.h"

#ifdef VC6
#define CPP_FILE "WIDCOMMStack.cpp"

#ifdef BWT_SINCE_SDK_6_0_1

    // see http://support.microsoft.com/kb/130869
    #define MDEFINE_GUID(name, l, w1, w2, b1, b2, b3, b4, b5, b6, b7, b8)  \
        EXTERN_C const GUID name = { l, w1, w2, { b1, b2,  b3,  b4,  b5,  b6,  b7,  b8 } }

    MDEFINE_GUID(GUID_BLUETOOTH_HCI_EVENT,               0xfc240062, 0x1541, 0x49be, 0xb4, 0x63, 0x84, 0xc4, 0xdc, 0xd7, 0xbf, 0x7f);
    MDEFINE_GUID(GUID_BLUETOOTH_RADIO_IN_RANGE,          0xea3b5b82, 0x26ee, 0x450e, 0xb0, 0xd8, 0xd2, 0x6f, 0xe3, 0x0a, 0x38, 0x69);
    MDEFINE_GUID(GUID_BLUETOOTH_RADIO_OUT_OF_RANGE,      0xe28867c9, 0xc2aa, 0x4ced, 0xb9, 0x69, 0x45, 0x70, 0x86, 0x60, 0x37, 0xc4);
    MDEFINE_GUID(GUID_BLUETOOTH_PIN_REQUEST,             0xbd198b7c, 0x24ab, 0x4b9a, 0x8c, 0x0d, 0xa8, 0xea, 0x83, 0x49, 0xaa, 0x16);
    MDEFINE_GUID(GUID_BLUETOOTH_L2CAP_EVENT,             0x7eae4030, 0xb709, 0x4aa8, 0xac, 0x55, 0xe9, 0x53, 0x82, 0x9c, 0x9d, 0xaa);
#endif

void WIDCOMMCleanup();

BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
    switch(ul_reason_for_call) {
    case DLL_PROCESS_DETACH:
        WIDCOMMCleanup();
        break;
    }
    return TRUE;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    WIDCOMMCleanup();
}

#endif

BOOL isWIDCOMMReady();

BOOL isWIDCOMMBluetoothStackPresent(JNIEnv *env) {
    HMODULE h = LoadLibrary(WIDCOMM_DLL);
    if (h == NULL) {
        return FALSE;
    }
    BOOL present = isWIDCOMMReady();
    FreeLibrary(h);
    return present;
}

#ifndef _BTWLIB
BOOL isWIDCOMMReady() {
    return TRUE;
}
#endif

#ifdef _BTWLIB

// We specify which DLLs to delay load with the /delayload:btwapi.dll linker option
// This is how it is now: wbtapi.dll;btfunc.dll;irprops.cpl
#ifdef VC6
#pragma comment(lib, "DelayImp.lib")
#pragma comment(linker, "/delayload:wbtapi.dll")
#endif

WIDCOMMStack* stack;

void BcAddrToString(wchar_t* addressString, BD_ADDR bd_addr) {
    swprintf_s(addressString, 14, L"%02x%02x%02x%02x%02x%02x",
             bd_addr[0],
             bd_addr[1],
             bd_addr[2],
             bd_addr[3],
             bd_addr[4],
             bd_addr[5]);
}

jlong BcAddrToLong(BD_ADDR bd_addr) {
    jlong l = 0;
    for (int i = 0; i < BD_ADDR_LEN; i++) {
        l = (l << 8) + bd_addr[i];
    }
    return l;
}

void LongToBcAddr(jlong addr, BD_ADDR bd_addr) {
    for (int i = BD_ADDR_LEN - 1; i >= 0; i--) {
        bd_addr[i] = (UINT8)(addr & 0xFF);
        addr >>= 8;
    }
}

jint DeviceClassToInt(DEV_CLASS devClass) {
    return (((devClass[0] << 8) + devClass[1]) << 8) + devClass[2];
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isNativeCodeLoaded
  (JNIEnv *env, jobject peer) {
    return JNI_TRUE;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLibraryVersion
(JNIEnv *, jobject) {
    return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_detectBluetoothStack
(JNIEnv *env, jobject) {
    return detectBluetoothStack(env);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_enableNativeDebug
  (JNIEnv *env, jobject, jclass loggerClass, jboolean on) {
    enableNativeDebug(env, loggerClass, on);
}

BOOL isWIDCOMMReady() {
    BOOL present = true;
    #ifndef WIDCOMM_CE30
        CBtIf* s = new CBtIf();
        present = s->IsDeviceReady();
        delete s;
    #else

    #endif
    return present;
}

/**
 * This is minimal MS detection under VC6. Compleate detection is done MS library anyway.
 */
BOOL isMicrosoftBluetoothStackPresentVC6(JNIEnv *env) {
    debug(("isMicrosoftBluetoothStackPresentVC6"));
    WSADATA data;
    if (WSAStartup(MAKEWORD(2, 2), &data) != 0) {
        int last_error = WSAGetLastError();
        debug(("WSAStartup error [%d] %S", last_error, getWinErrorMessage(last_error)));
        return FALSE;
    }

    SOCKET s = socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM);
    if (s == INVALID_SOCKET) {
        int last_error = WSAGetLastError();
        debug(("socket error [%d] %S", last_error, getWinErrorMessage(last_error)));
        WSACleanup();
        return FALSE;
    }
    closesocket(s);
    WSACleanup();
    return TRUE;
}

WIDCOMMStack::WIDCOMMStack() {
    hEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,    // auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
    InitializeCriticalSection(&csCommIf);

    delayDeleteComm = TRUE;
    commPool = new ObjectPool(COMMPORTS_POOL_MAX, 1, delayDeleteComm);

    deviceInquiryInProcess = FALSE;
    deviceRespondedIdx = -1;
    discoveryRecHolderCurrent = NULL;
    discoveryRecHolderHold = NULL;
}

DiscoveryRecHolder::DiscoveryRecHolder() {
    sdpDiscoveryRecordsUsed = 0;
}

WIDCOMMStack* createWIDCOMMStack() {
    return new WIDCOMMStack();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_initializeImpl
(JNIEnv *env, jobject) {
    jboolean rc = JNI_TRUE;
    if (stack == NULL) {
        __try  {
            stack = createWIDCOMMStack();
            if (stack->hEvent == NULL) {
                throwRuntimeException(env, "fails to CreateEvent");
            }
        } __except(GetExceptionCode() == 0xC06D007E) {
            rc = JNI_FALSE;
        }
    }
    return rc;
}

WIDCOMMStack::~WIDCOMMStack() {
    destroy(NULL);
    deviceInquiryInProcess = FALSE;
    CloseHandle(hEvent);
    DeleteCriticalSection(&csCommIf);
}

DWORD WINAPI GCThreadFunction(LPVOID lpParam) {
    Sleep(30 * 1000);
    ndebug(("GC ObjectPool"));
    ObjectPool* commPool = (ObjectPool*)lpParam;
    delete commPool;
    return 0;
}

void WIDCOMMStack::destroy(JNIEnv * env) {
    SetEvent(hEvent);
    if (commPool != NULL) {
        //delete commPool;
        // create Delay GC Thread
        HANDLE  hThread = CreateThread(
            NULL,                   // default security attributes
            0,                      // use default stack size
            GCThreadFunction,       // thread function name
            commPool,               // argument to thread function
            0,                      // use default creation flags
            NULL);   // returns the thread identifier
        CloseHandle(hThread);

        commPool = NULL;
    }
    if (discoveryRecHolderHold != NULL) {
        delete discoveryRecHolderHold;
        discoveryRecHolderHold = NULL;
    }
    if (discoveryRecHolderCurrent != NULL) {
        delete discoveryRecHolderCurrent;
        discoveryRecHolderCurrent = NULL;
    }
}

void WIDCOMMStack::throwExtendedBluetoothStateException(JNIEnv * env) {
    LPCTSTR msg = NULL;
    #ifndef WIDCOMM_CE30
    if (!IsDeviceReady()) {
        throwBluetoothStateException(env, "Bluetooth Device is not ready");
        return;
    }
    #endif
#ifndef _WIN32_WCE
    WBtRc er = GetExtendedError();
    if (er == WBT_SUCCESS) {
        throwBluetoothStateException(env, "No errors in WIDCOMM stack");
        return;
    }
    msg = WBtRcToString(er);
#endif //! _WIN32_WCE
    if (msg != NULL) {
        throwBluetoothStateException(env, "WIDCOMM error[%s]", msg);
    } else {
        throwBluetoothStateException(env, "No WIDCOMM error code");
    }
}

char* WIDCOMMStack::getExtendedError() {
    static char* noError = "No error code";
    if (stack == NULL) {
        return noError;
    }
    LPCTSTR msg = NULL;
#ifndef _WIN32_WCE
    WBtRc er = stack->GetExtendedError();
    msg = WBtRcToString(er);
#endif //! _WIN32_WCE
    if (msg != NULL) {
        return (char*)msg;
    } else {
        return noError;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_uninitialize
(JNIEnv *env, jobject) {
    if (stack != NULL) {
        debug(("destroy WIDCOMMStack"));
        WIDCOMMStack* stackTmp = stack;
        stack = NULL;
        stackTmp->destroy(env);
        delete stackTmp;
    }
}

void WIDCOMMCleanup() {
    if (stack != NULL) {
        ///log_info("WIDCOMMCleanup");
        WIDCOMMStack* stackTmp = stack;
        stack = NULL;
        stackTmp->destroy(NULL);
        delete stackTmp;
    }
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject peer) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    wchar_t addressString[14];
    #ifndef WIDCOMM_CE30
    struct CBtIf::DEV_VER_INFO info;
    if (!stack->GetLocalDeviceVersionInfo(&info)) {
        stack->throwExtendedBluetoothStateException(env);
        return NULL;
    }
    BcAddrToString(addressString, info.bd_addr);
    #else
    if (!stack->GetLocalDeviceInfo()) {
        stack->throwExtendedBluetoothStateException(env);
        return NULL;
    }
    BcAddrToString(addressString, stack->m_BdAddr);
    #endif
    return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceName
(JNIEnv *env, jobject peer) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    #ifndef WIDCOMM_CE30
    BD_NAME name;
    if (!stack->GetLocalDeviceName(&name)) {
        debug(("WIDCOMM error[%s]", stack->getExtendedError()));
        return NULL;
    }
    return env->NewStringUTF((char*)name);
    #else
    return NULL;
    #endif
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isLocalDevicePowerOn
(JNIEnv *env, jobject peer) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    #ifndef WIDCOMM_CE30
    return stack->IsDeviceReady();
    #else
    return true;
    #endif
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isStackServerUp
(JNIEnv *env, jobject peer) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    #ifndef WIDCOMM_CE30
    return stack->IsStackServerUp();
    #else
    return true;
    #endif
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isLocalDeviceDiscoverable
 (JNIEnv *env, jobject) {

    HKEY hkey;
    REGSAM samDesired = KEY_READ;
    DWORD dwAllowOthersToDiscover = 0;

    HKEY hMainKey = HKEY_LOCAL_MACHINE;
    #ifndef WIDCOMM_CE30
    hMainKey = HKEY_CURRENT_USER;
    #endif

    if (RegOpenKeyEx(hMainKey, TEXT("Software\\Widcomm\\BtConfig\\Filters"), 0, samDesired, &hkey) == ERROR_SUCCESS) {
        DWORD dwType, dwSize;
        dwType = REG_DWORD;
        dwSize = sizeof(DWORD);
        if (RegQueryValueEx(hkey, TEXT("AllowOthersToDiscover"), NULL, &dwType, (PBYTE)&dwAllowOthersToDiscover, &dwSize) != ERROR_SUCCESS) {
            debug(("e.WIDCOMM AllowOthersToDiscover not found"));
        }
        RegCloseKey(hkey);
    } else {
        debug(("e.WIDCOMM BtConfig not found"));
    }
    return (dwAllowOthersToDiscover)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getBTWVersionInfo
(JNIEnv *env, jobject peer) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    BT_CHAR p_version[256];
#ifdef _WIN32_WCE
    #ifdef WIDCOMM_CE30
    memcpy(p_version, L"<1.4.0", wcslen(L"<1.4.0"));
    #else
    if (!stack->GetBTWCEVersionInfo(p_version, 256)) {
        return NULL;
    }
    #endif
#else // _WIN32_WCE
    BT_CHAR bwt_version[256];
    BT_CHAR dk_version[256];
    if (!stack->GetBTWVersionInfo(bwt_version, 256)) {
        return NULL;
    }
    if (!stack->GetDKVersionInfo(dk_version, 256)) {
        return NULL;
    }
    sprintf_s(p_version, 256, "BWT %s, DK %s", bwt_version, dk_version);
#endif // #else // _WIN32_WCE
    return env->NewStringUTF((char*)p_version);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getDeviceVersion
(JNIEnv *env, jobject) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    #ifndef WIDCOMM_CE30
    CBtIf::DEV_VER_INFO dev_Ver_Info;
    if (!stack->GetLocalDeviceVersionInfo(&dev_Ver_Info)) {
        return -1;
    }
    return dev_Ver_Info.lmp_sub_version;
    #else
    return 0;
    #endif
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getDeviceManufacturer
(JNIEnv *env, jobject) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    #ifndef WIDCOMM_CE30
    CBtIf::DEV_VER_INFO dev_Ver_Info;
    if (!stack->GetLocalDeviceVersionInfo(&dev_Ver_Info)) {
        return -1;
    }
    return dev_Ver_Info.manufacturer;
    #else
    return 0;
    #endif
}

void debugGetRemoteDeviceInfo_rc(JNIEnv *env, CBtIf::REM_DEV_INFO_RETURN_CODE rc) {
    if ((isDebugOn()) && (rc != CBtIf::REM_DEV_INFO_SUCCESS)) {
        switch (rc) {
            case CBtIf::REM_DEV_INFO_EOF: debug(("RemoteDeviceFriendlyName REM_DEV_INFO_EOF")); break;
            case CBtIf::REM_DEV_INFO_ERROR: debug(("RemoteDeviceFriendlyName REM_DEV_INFO_ERROR")); break;
            case CBtIf::REM_DEV_INFO_MEM_ERROR: debug(("RemoteDeviceFriendlyName REM_DEV_INFO_MEM_ERROR")); break;
            default: debug(("RemoteDeviceFriendlyName ???")); break;
        }
    }
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getRemoteDeviceFriendlyName
(JNIEnv *env, jobject, jlong address, jint majorDeviceClass, jint minorDeviceClass) {
    if (stack == NULL) {
        return NULL;
    }
    #ifndef WIDCOMM_CE30
    // if device InquiryInProcess wait untill FriendlyName is returned by running running Inquiry
    do {
        // filter needs to be exact....
        DEV_CLASS filter_dev_class;
        filter_dev_class[0] = 0;
        filter_dev_class[1] = (unsigned char)majorDeviceClass;
        filter_dev_class[2] = (unsigned char)minorDeviceClass;
        CBtIf::REM_DEV_INFO dev_info;
        CBtIf::REM_DEV_INFO_RETURN_CODE rc = stack->GetRemoteDeviceInfo(filter_dev_class, &dev_info);
        debugGetRemoteDeviceInfo_rc(env, rc);
        while ((rc == CBtIf::REM_DEV_INFO_SUCCESS) && (stack != NULL)) {
            jlong a = BcAddrToLong(dev_info.bda);
            if (isDebugOn()) {
                wchar_t addressString[14];
                BcAddrToString(addressString, dev_info.bda);
                debug(("RemoteDeviceFriendlyName found %S", addressString));
            }
            if (a == address) {
                if (dev_info.bd_name[0] != '\0') {
                    return env->NewStringUTF((char*)dev_info.bd_name);
                } else {
                    break;
                }
            }
            rc = stack->GetNextRemoteDeviceInfo(&dev_info);
            debugGetRemoteDeviceInfo_rc(env, rc);
        }
        if ((stack != NULL) && (stack->deviceInquiryInProcess)) {
            Sleep(400);
        }
    } while ((stack != NULL) && (stack->deviceInquiryInProcess));
    #endif
    return NULL;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_authenticateRemoteDeviceImpl
  (JNIEnv *env, jobject, jlong address, jstring passkey) {
    if (stack == NULL) {
        return FALSE;
    }
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    BT_CHAR* pin_code = NULL;
    BT_CHAR pinBuf[PIN_CODE_LEN +1];
    if (passkey != NULL) {
        #ifdef WIDCOMM_CE30
            const jchar *cpasskey = env->GetStringChars(passkey, JNI_FALSE);
            jsize size = env->GetStringLength(passkey);
            int i = 0;
            for(; (i < PIN_CODE_LEN) && (i < size); i ++) {
                pinBuf[i] = cpasskey[i];
            }
            pinBuf[i] = '\0';
            pin_code = pinBuf;
            env->ReleaseStringChars(passkey, cpasskey);
        #else
            const char *cpasskey = env->GetStringUTFChars(passkey, 0);
            jsize size = env->GetStringLength(passkey);
            int i = 0;
            for(; (i < PIN_CODE_LEN) && (i < size); i ++) {
                pinBuf[i] = cpasskey[i];
            }
            pinBuf[i] = '\0';
            pin_code = pinBuf;
            env->ReleaseStringUTFChars(passkey, cpasskey);
            debug(("Bond pin [%s]", pin_code));
        #endif
    }
    CBtIf::BOND_RETURN_CODE rc = stack->Bond(bda, pin_code);
    if ((rc == CBtIf::SUCCESS) || (rc == CBtIf::ALREADY_BONDED)) {
        return JNI_TRUE;
    } else {
        throwIOException(env, "Bonding error [%i]", rc);
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isRemoteDeviceConnected
  (JNIEnv *env, jobject, jlong address) {
    if (stack == NULL) {
        throwRuntimeException(env, cSTACK_CLOSED);
        return FALSE;
    }
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    return stack->IsRemoteDeviceConnected(bda);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getRemoteDeviceLinkMode
  (JNIEnv *env, jobject, jlong address) {
    if (stack == NULL) {
        throwRuntimeException(env, cSTACK_CLOSED);
        return NULL;
    }
    UINT8 mode = 0;
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    BOOL rc;
    #ifdef WIDCOMM_CE30
        rc = stack->ReadLinkMode(bda, &mode);
        // TODO verify this values
        const UINT8 LINK_MODE_NORMAL = 0;
        const UINT8 LINK_MODE_HOLD = 1;
        const UINT8 LINK_MODE_SNIFF = 2;
        const UINT8 LINK_MODE_PARK = 3;
    #else
        rc = CBtIf::ReadLinkMode(bda, &mode);
        const UINT8 LINK_MODE_NORMAL = CBtIf::LINK_MODE::LINK_MODE_NORMAL;
        const UINT8 LINK_MODE_HOLD = CBtIf::LINK_MODE::LINK_MODE_HOLD;
        const UINT8 LINK_MODE_SNIFF = CBtIf::LINK_MODE::LINK_MODE_SNIFF;
        const UINT8 LINK_MODE_PARK = CBtIf::LINK_MODE::LINK_MODE_PARK;
    #endif

    if (rc) {
        char* cMode;
        switch(mode) {
            case LINK_MODE_NORMAL:
                cMode = "normal";
                break;
            case LINK_MODE_HOLD:
                cMode = "hold";
                break;
            case LINK_MODE_SNIFF:
                cMode = "sniff";
                break;
            case LINK_MODE_PARK:
                cMode = "park";
                break;
            default:
                throwIOException(env, "Invalid link mode [%i]", mode);
                return NULL;
        }
        return env->NewStringUTF(cMode);
    } else {
        throwRuntimeException(env, "Can't read link mode");
        return NULL;
    }
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getRemoteDeviceVersionInfo
  (JNIEnv *env, jobject, jlong address) {
    // TODO
    if (stack == NULL) {
        throwRuntimeException(env, cSTACK_CLOSED);
        return NULL;
    }
    UINT8 mode = 0;
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    CBtIf::DEV_VER_INFO devVerInfo;
    if (!stack->GetRemoteDeviceVersionInfo(bda, &devVerInfo)) {
        return NULL;
    } else {
        char info[256];
        sprintf_s(info, 256, "manufacturer=%i,lmp_version=%i,lmp_sub_version=%i", devVerInfo.manufacturer, devVerInfo.lmp_version, devVerInfo.lmp_sub_version);
        return env->NewStringUTF(info);
    }
}

#ifndef WIDCOMM_CE30
JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_setSniffMode
  (JNIEnv *env, jobject, jlong address) {
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    return CBtIf::SetSniffMode(bda);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_cancelSniffMode
  (JNIEnv *env, jobject, jlong address) {
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    return CBtIf::CancelSniffMode(bda);
}
#endif

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getRemoteDeviceRSSI
  (JNIEnv *env, jobject, jlong address) {
    if (stack == NULL) {
        throwRuntimeException(env, cSTACK_CLOSED);
        return NULL;
    }
    BD_ADDR bda;
    LongToBcAddr(address, bda);
    tBT_CONN_STATS conn_stats;
    if (!stack->GetConnectionStats(bda, &conn_stats)) {
        return -1;
    }
    return conn_stats.Rssi;
}

jboolean isDevicePaired(JNIEnv *env, jlong address, DEV_CLASS devClass) {
    if (stack == NULL) {
        return FALSE;
    }
    #ifndef WIDCOMM_CE30
        CBtIf::REM_DEV_INFO dev_info;
        CBtIf::REM_DEV_INFO_RETURN_CODE rc = stack->GetRemoteDeviceInfo(devClass, &dev_info);
        debugGetRemoteDeviceInfo_rc(env, rc);
        while ((rc == CBtIf::REM_DEV_INFO_SUCCESS) && (stack != NULL)) {
            jlong a = BcAddrToLong(dev_info.bda);
            if (a == address) {
                return dev_info.b_paired;
            }
            rc = stack->GetNextRemoteDeviceInfo(&dev_info);
            debugGetRemoteDeviceInfo_rc(env, rc);
        }
    #endif
    return FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getDeviceClassImpl
(JNIEnv *env, jobject) {
    return getDeviceClassByOS(env);
}

// --- Device Inquiry

void WIDCOMMStack::OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected) {
    if ((stack == NULL) || (!deviceInquiryInProcess)) {
        return;
    }
    int nextDevice = deviceRespondedIdx + 1;
    if (nextDevice >= DEVICE_FOUND_MAX) {
        nextDevice = 0;
    }
    deviceResponded[nextDevice].deviceAddr = BcAddrToLong(bda);
    deviceResponded[nextDevice].deviceClass = DeviceClassToInt(devClass);
    memcpy(deviceResponded[nextDevice].bdName, bdName, sizeof(BD_NAME));
    memcpy(deviceResponded[nextDevice].devClass, devClass, sizeof(DEV_CLASS));
    deviceRespondedIdx = nextDevice;
    SetEvent(hEvent);
}

void WIDCOMMStack::OnInquiryComplete(BOOL success, short num_responses) {
    if (stack == NULL) {
        return;
    }
    deviceInquiryInProcess = FALSE;
    deviceInquirySuccess = success;
    deviceInquiryComplete = TRUE;
    SetEvent(hEvent);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    if (stack->deviceInquiryInProcess) {
        throwBluetoothStateException(env, cINQUIRY_RUNNING);
    }
    stack->deviceInquiryInProcess = TRUE;
    debug(("StartDeviceInquiry"));
    stack->deviceInquiryComplete = FALSE;
    stack->deviceInquiryTerminated = FALSE;

    memset(stack->deviceResponded, 0, sizeof(stack->deviceResponded));
    stack->deviceRespondedIdx = -1;

    DeviceInquiryCallback callback;
    if (!callback.builDeviceInquiryCallbacks(env, peer, startedNotify)) {
        if (stack != NULL) {
            stack->deviceInquiryInProcess = FALSE;
        }
        return INQUIRY_ERROR;
    }

    // synchronized (BluetoothStackWIDCOMM instance) {
    if (env->MonitorEnter(peer) != JNI_OK) {
        if (stack != NULL) {
            stack->deviceInquiryInProcess = FALSE;
        }
        throwRuntimeException(env, "Monitor error");
        return INQUIRY_ERROR;
    }

    BOOL startOk = stack->StartInquiry();

    if (env->MonitorExit(peer) != JNI_OK) {
        if (stack != NULL) {
            stack->deviceInquiryInProcess = FALSE;
        }
        throwRuntimeException(env, "Monitor error");
        return INQUIRY_ERROR;
    }

    if (!startOk) {
        debug(("deviceInquiryStart error"));
        if (stack != NULL) {
            stack->throwExtendedBluetoothStateException(env);
            stack->deviceInquiryInProcess = FALSE;
        }
        return INQUIRY_ERROR;
    }
    debug(("deviceInquiryStarted"));

    if (!callback.callDeviceInquiryStartedCallback(env)) {
        if (stack != NULL) {
            stack->StopInquiry();
            stack->deviceInquiryInProcess = FALSE;
        }
        return INQUIRY_ERROR;
    }

    int reportedIdx = -1;

    while ((stack != NULL) && (!stack->deviceInquiryTerminated) && ((!stack->deviceInquiryComplete) || (reportedIdx != stack->deviceRespondedIdx))) {
        // When deviceInquiryComplete look at the remainder of Responded devices. Do Not Wait
        if (!stack->deviceInquiryComplete) {
            DWORD  rc = WaitForSingleObject(stack->hEvent, 200);
            if (rc == WAIT_FAILED) {
                throwRuntimeException(env, "WaitForSingleObject");
                if (stack != NULL) {
                    stack->deviceInquiryInProcess = FALSE;
                }
                return INQUIRY_ERROR;
            }
        }
        if ((stack != NULL) && (reportedIdx != stack->deviceRespondedIdx)) {
            reportedIdx ++;
            if (reportedIdx >= DEVICE_FOUND_MAX) {
                reportedIdx = 0;
            }
            DeviceFound dev = stack->deviceResponded[reportedIdx];
            jboolean paired = isDevicePaired(env, dev.deviceAddr, dev.devClass);
            if (!callback.callDeviceDiscovered(env, listener, dev.deviceAddr, dev.deviceClass, env->NewStringUTF((char*)(dev.bdName)), paired)) {
                if (stack != NULL) {
                    stack->StopInquiry();
                    stack->deviceInquiryInProcess = FALSE;
                }
                return INQUIRY_ERROR;
            }
        }
    }

    if (stack != NULL) {
        stack->StopInquiry();
        if (stack != NULL) {
            stack->deviceInquiryInProcess = FALSE;
        }
    }

    if (stack == NULL) {
        return INQUIRY_TERMINATED;
    } else if (stack->deviceInquiryTerminated) {
        return INQUIRY_TERMINATED;
    } else if (stack->deviceInquirySuccess) {
        return INQUIRY_COMPLETED;
    } else {
        return INQUIRY_ERROR;
    }
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer) {
    debug(("StopInquiry"));
    if (stack != NULL) {
        stack->deviceInquiryTerminated = TRUE;
        stack->StopInquiry();
        if (stack != NULL) {
            SetEvent(stack->hEvent);
        }
    }
    return TRUE;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_peekRemoteDeviceFriendlyName
(JNIEnv *env, jobject, jlong address) {
    if (stack == NULL) {
        return NULL;
    }
    while (stack != NULL) {
        for(int idx = 0; ((stack != NULL) && idx <= stack->deviceRespondedIdx); idx ++) {
            DeviceFound dev = stack->deviceResponded[idx];
            if ((address == dev.deviceAddr) && (dev.bdName != '\0')) {
                return env->NewStringUTF((char*)dev.bdName);
            }
        }
        if ((stack != NULL) && (stack->deviceInquiryInProcess)) {
            debug(("peekRemoteDeviceFriendlyName sleeps"));
            Sleep(200);
        } else {
            break;
        }
    };
    return NULL;
}

// --- Service search

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runSearchServicesImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jbyteArray uuidValue, jlong address) {
    if (stack == NULL) {
        throwBluetoothStateException(env, cSTACK_CLOSED);
        return 0;
    }
    debug(("StartSearchServices"));

    BD_ADDR bda;
    LongToBcAddr(address, bda);

#ifdef EXT_DEBUG
    if (isDebugOn()) {
        wchar_t addressString[14];
        BcAddrToString(addressString, bda);
        debug(("StartSearchServices on %S", addressString));
    }
#endif

    GUID *p_service_guid = NULL;
    GUID service_guid;
    //If uuidValue parameter is NULL, all public browseable services for the device will be reported
    if (uuidValue != NULL) {
        jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
        // build UUID
        convertUUIDBytesToGUID(bytes, &service_guid);
        env->ReleaseByteArrayElements(uuidValue, bytes, 0);
        p_service_guid = &service_guid;
    }
    if (p_service_guid == NULL) {
        debug(("p_service_guid is NULL"));
    }

    stack->searchServicesComplete = FALSE;
    stack->searchServicesTerminated = FALSE;

    // synchronized (BluetoothStackWIDCOMM instance) {
    if (env->MonitorEnter(peer) != JNI_OK) {
        throwRuntimeException(env, "Monitor error");
        return NULL;
    }

    BOOL discoveryStarted = stack->StartDiscovery(bda, p_service_guid);

    if (env->MonitorExit(peer) != JNI_OK) {
        throwRuntimeException(env, "Monitor error");
        return NULL;
    }

    if (!discoveryStarted) {
        if (stack == NULL) {
            throwBluetoothStateException(env, cSTACK_CLOSED);
            return NULL;
        }
        #ifndef _WIN32_WCE
            WBtRc er = stack->GetExtendedError();
            if (er == WBT_SUCCESS) {
                discoveryStarted = FALSE;
            }
        #endif
        if (discoveryStarted) {
            debug(("StartSearchServices error"));
            stack->throwExtendedBluetoothStateException(env);
            return NULL;
        }
    }

    if (stack == NULL) {
        throwBluetoothStateException(env, cSTACK_CLOSED);
        return NULL;
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

    if (discoveryStarted) {
        while ((stack != NULL) && (!stack->searchServicesComplete) && (!stack->searchServicesTerminated)) {
            DWORD  rc = WaitForSingleObject(stack->hEvent, 500);
            if (rc == WAIT_FAILED) {
                throwRuntimeException(env, "WaitForSingleObject");
                return NULL;
            }
        }
    }
    if (stack == NULL) {
        return NULL;
    }

    if (stack->searchServicesTerminated) {
        throwException(env, "com/intel/bluetooth/SearchServicesTerminatedException", "");
        return NULL;
    }

    UINT16 obtainedServicesRecords;
    CBtIf::DISCOVERY_RESULT searchServicesResultCode = stack->GetLastDiscoveryResult(bda, &obtainedServicesRecords);

    //todo SERVICE_SEARCH_TERMINATED

    if (searchServicesResultCode != CBtIf::DISCOVERY_RESULT_SUCCESS) {
        debug(("searchServicesResultCode %i", searchServicesResultCode));
        return NULL;
    }
    if (obtainedServicesRecords <= 0) {
        return env->NewLongArray(0);
    }

    if (obtainedServicesRecords > SDP_DISCOVERY_RECORDS_DEVICE_MAX) {
        debug(("WARN too many ServicesRecords %u", obtainedServicesRecords));
        //obtainedServicesRecords = SDP_DISCOVERY_RECORDS_USED_MAX;
    }
    debug(("obtainedServicesRecords %u", obtainedServicesRecords));
    // Retrive all Records and filter in Java
    int retriveRecords = SDP_DISCOVERY_RECORDS_DEVICE_MAX;

    // Select RecHolder
    DiscoveryRecHolder* discoveryRecHolder = stack->discoveryRecHolderCurrent;
    if (discoveryRecHolder == NULL) {
        discoveryRecHolder = new DiscoveryRecHolder();
        discoveryRecHolder->oddHolder = TRUE;
        stack->discoveryRecHolderCurrent = discoveryRecHolder;
        Edebug(("DiscoveryRecHolder created"));
    }

    int useIdx = discoveryRecHolder->sdpDiscoveryRecordsUsed;
    if (useIdx + retriveRecords > SDP_DISCOVERY_RECORDS_USED_MAX) {
        useIdx = 0;
        Edebug(("DiscoveryRecHolder switch"));
        // Select next RecHolder
        if (stack->discoveryRecHolderHold != NULL) {
            Edebug(("DiscoveryRecHolder delete %p", discoveryRecHolder));
            delete stack->discoveryRecHolderHold;
        }
        BOOL newOdd = !(discoveryRecHolder->oddHolder);
        stack->discoveryRecHolderHold = discoveryRecHolder;
        discoveryRecHolder = new DiscoveryRecHolder();
        discoveryRecHolder->oddHolder = newOdd;
        stack->discoveryRecHolderCurrent = discoveryRecHolder;
        if (newOdd) {
            Edebug(("DiscoveryRecHolder created ODD"));
        } else {
            Edebug(("DiscoveryRecHolder created EVEN"));
        }
    } else {
        Edebug(("DiscoveryRecHolder useIdx %i", useIdx));
    }

    Edebug(("discoveryRecHolderHold %p", stack->discoveryRecHolderHold));
    Edebug(("discoveryRecHolderCurrent %p", stack->discoveryRecHolderCurrent));

#ifdef EXT_DEBUG
    if (isDebugOn()) {
        wchar_t addressString2[14];
        BcAddrToString(addressString2, bda);
        debug(("ReadDiscoveryRecords on %S", addressString2));
    }
#endif

    CSdpDiscoveryRec *sdpDiscoveryRecordsList = discoveryRecHolder->sdpDiscoveryRecords + useIdx;

    //guid_filter does not work as Expected with SE Phones!
    int recSize = stack->ReadDiscoveryRecords(bda, retriveRecords, sdpDiscoveryRecordsList, NULL);
    if (recSize == 0) {
        debug(("ReadDiscoveryRecords returns empty, While expected min %i", obtainedServicesRecords));
        return NULL;
    }
    Edebug(("DiscoveryRecHolder curr %p", discoveryRecHolder));
    debug(("DiscoveryRecHolder +=recSize %i", recSize));
    discoveryRecHolder->sdpDiscoveryRecordsUsed += recSize;

    int offset = SDP_DISCOVERY_RECORDS_HANDLE_OFFSET + useIdx;
    if (discoveryRecHolder->oddHolder) {
        offset += SDP_DISCOVERY_RECORDS_HOLDER_MARK;
    }

    Edebug(("DiscoveryRecHolder first.h %i", offset));

    jlongArray result = env->NewLongArray(recSize);
    jlong *longs = env->GetLongArrayElements(result, 0);
    for (int r = 0; r < recSize; r ++) {
        longs[r] = offset + r;
    }
    env->ReleaseLongArrayElements(result, longs, 0);

    return result;
}

void WIDCOMMStack::OnDiscoveryComplete() {
    if (stack == NULL) {
        return;
    }
    searchServicesComplete = TRUE;
    SetEvent(hEvent);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_cancelServiceSearchImpl
(JNIEnv *env, jobject) {
    if (stack != NULL)  {
        stack->searchServicesTerminated = TRUE;
        SetEvent(stack->hEvent);
    }
}

CSdpDiscoveryRec* validDiscoveryRec(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, "Invalid DiscoveryRec handle");
        return NULL;
    }
    int offset = (int)handle;
    Edebug(("DiscoveryRecHolder handle %i", offset));
    Edebug(("stack->discoveryRecHolderHold %p", stack->discoveryRecHolderHold));
    Edebug(("stack->discoveryRecHolderCurrent %p", stack->discoveryRecHolderCurrent));

    DiscoveryRecHolder* discoveryRecHolder;
    BOOL heedOdd = (handle > SDP_DISCOVERY_RECORDS_HOLDER_MARK);
    if (heedOdd) {
        offset -= SDP_DISCOVERY_RECORDS_HOLDER_MARK;
    }
    if (stack->discoveryRecHolderCurrent->oddHolder == heedOdd) {
        discoveryRecHolder = stack->discoveryRecHolderCurrent;
        Edebug(("DiscoveryRecHolder use ODD"));
    } else {
        discoveryRecHolder = stack->discoveryRecHolderHold;
        Edebug(("DiscoveryRecHolder use EVEN"));
    }
    if (discoveryRecHolder == NULL) {
        throwIOException(env, "Invalid DiscoveryRec holder");
        return NULL;
    }
    if ((offset < SDP_DISCOVERY_RECORDS_HANDLE_OFFSET)
        || (offset > SDP_DISCOVERY_RECORDS_USED_MAX + SDP_DISCOVERY_RECORDS_HANDLE_OFFSET)) {
        throwIOException(env, "Invalid DiscoveryRec handle [%i]", offset);
        return NULL;
    }
    Edebug(("DiscoveryRecHolder used %p", discoveryRecHolder));
    return discoveryRecHolder->sdpDiscoveryRecords + offset - SDP_DISCOVERY_RECORDS_HANDLE_OFFSET;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isServiceRecordDiscoverable
(JNIEnv *env, jobject, jlong address, jlong handle) {
    CSdpDiscoveryRec* record = validDiscoveryRec(env, handle);
    if (record == NULL) {
        return JNI_FALSE;
    }
    stack->searchServicesComplete = FALSE;
    stack->searchServicesTerminated = FALSE;

    BD_ADDR bda;
    LongToBcAddr(address, bda);
    if (!stack->StartDiscovery(bda, &(record->m_service_guid))) {
        debug(("StartDiscovery WIDCOMM error[%s]", stack->getExtendedError()));
        return JNI_FALSE;
    }

    while ((stack != NULL) && (!stack->searchServicesComplete) && (!stack->searchServicesTerminated)) {
        DWORD  rc = WaitForSingleObject(stack->hEvent, 500);
        if (rc == WAIT_FAILED) {
            return JNI_FALSE;
        }
    }
    if ((stack == NULL) || (stack->searchServicesTerminated)) {
        return JNI_FALSE;
    }

    UINT16 obtainedServicesRecords = 0;
    CBtIf::DISCOVERY_RESULT searchServicesResultCode = stack->GetLastDiscoveryResult(bda, &obtainedServicesRecords);
    if (searchServicesResultCode != CBtIf::DISCOVERY_RESULT_SUCCESS) {
        debug(("isServiceRecordDiscoverable resultCode %i", searchServicesResultCode));
        return JNI_FALSE;
    }
    debug(("isServiceRecordDiscoverable found sr %i", obtainedServicesRecords));
    if (obtainedServicesRecords < 1) {
        return JNI_FALSE;
    }

    /* This does not help.
    CSdpDiscoveryRec sdpDiscoveryRecord;
    //guid_filter does not work as Expected with SE Phones!
    int recSize = stack->ReadDiscoveryRecords(bda, 1, &sdpDiscoveryRecord, &(record->m_service_guid));
    if (recSize == 0) {
        debug(("ReadDiscoveryRecords returns empty, while expected 1");
        return JNI_FALSE;
    }
    */

    return JNI_TRUE;
}

typedef struct {

    int valueSize;

} SDP_DISC_ATTTR_VAL_VERSION_INFO;

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getServiceAttribute
(JNIEnv *env, jobject peer, jint attrID, jlong handle) {
    CSdpDiscoveryRec* record = validDiscoveryRec(env, handle);
    if (record == NULL) {
        return NULL;
    }

    SDP_DISC_ATTTR_VAL* pval = new SDP_DISC_ATTTR_VAL;

    if (!record->FindAttribute((UINT16)attrID, pval)) {
        // attr not found
        delete pval;
        return NULL;
    }

    /*
    if (attrID == 4) {
        UINT16 psm;
        if (record->FindL2CapPsm(&psm) ) {
            debug(("FindL2CapPsm %i", psm);
        }
    }
    */
    SDP_DISC_ATTTR_VAL_VERSION_INFO verInfo;
    verInfo.valueSize = MAX_ATTR_LEN;

    jbyteArray result = env->NewByteArray(sizeof(SDP_DISC_ATTTR_VAL) + sizeof(SDP_DISC_ATTTR_VAL_VERSION_INFO));
    jbyte *bytes = env->GetByteArrayElements(result, 0);
    memcpy(bytes, &verInfo, sizeof(SDP_DISC_ATTTR_VAL_VERSION_INFO));
    memcpy(bytes + sizeof(SDP_DISC_ATTTR_VAL_VERSION_INFO), pval, sizeof(SDP_DISC_ATTTR_VAL));
    env->ReleaseByteArrayElements(result, bytes, 0);
    delete pval;
    return result;
}

//   --- Client RFCOMM connections

BOOL isValidStackObject(PoolableObject* object) {
    if (stack == NULL) {
        return FALSE;
    }
    return stack->commPool->hasObject(object);
}

// Guarded by CriticalSection csCommIf
WIDCOMMStackRfCommPort* WIDCOMMStack::createCommPort() {
    WIDCOMMStackRfCommPort* port = new WIDCOMMStackRfCommPort();

    if (!commPool->addObject(port, 'r')) {
        delete port;
        return NULL;
    }
    return port;
}

WIDCOMMStackRfCommPortServer* WIDCOMMStack::createCommServer() {
    WIDCOMMStackRfCommPortServer* port = new WIDCOMMStackRfCommPortServer();

    if (!commPool->addObject(port, 'R')) {
        delete port;
        return NULL;
    }
    return port;
}

WIDCOMMStackRfCommPort* validRfCommHandle(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    return (WIDCOMMStackRfCommPort*)stack->commPool->getObject(env, handle, 'r');
}

WIDCOMMStackRfCommPortServer* validRfCommServerHandle(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    return (WIDCOMMStackRfCommPortServer*)stack->commPool->getObject(env, handle, 'R');
}

void WIDCOMMStack::deleteConnection(PoolableObject* object) {
    if (object == NULL) {
        return;
    }
    object->readyToFree = TRUE;
    object->magic1 = 0;
    if (!delayDeleteComm) {
        if (commPool != NULL) {
            commPool->removeObject(object);
        }
        delete object;
    }
}

WIDCOMMStackL2CapConn* WIDCOMMStack::createL2CapConn() {
    WIDCOMMStackL2CapConn* conn = new WIDCOMMStackL2CapConn();
    if (!commPool->addObject(conn, 'l')) {
        delete conn;
        return NULL;
    }
    return conn;
}

WIDCOMMStackL2CapServer* WIDCOMMStack::createL2CapServer() {
    WIDCOMMStackL2CapServer* conn = new WIDCOMMStackL2CapServer();
    if (!commPool->addObject(conn, 'L')) {
        delete conn;
        return NULL;
    }
    return conn;
}

WIDCOMMStackL2CapConn* validL2CapConnHandle(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    return (WIDCOMMStackL2CapConn*)stack->commPool->getObject(env, handle, 'l');
}

WIDCOMMStackL2CapServer* validL2CapServerHandle(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    return (WIDCOMMStackL2CapServer*)stack->commPool->getObject(env, handle, 'L');
}

WIDCOMMStackConnectionBase::WIDCOMMStackConnectionBase() {
    isConnected = FALSE;
    isClosing = FALSE;
    service_name[0] = '\0';

    memset(&service_guid, 0, sizeof(GUID));

    hConnectionEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,     // auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
}

WIDCOMMStackConnectionBase::~WIDCOMMStackConnectionBase() {
    isConnected = FALSE;
    CloseHandle(hConnectionEvent);
}


WIDCOMMStackServerConnectionBase::WIDCOMMStackServerConnectionBase() {
    sdpService = new CSdpService();
    for(int i = 0 ; i < OPEN_COMMPORTS_MAX; i ++) {
        conn[i] = NULL;
    }
    sdpRecordCommited = FALSE;
    service_guids = NULL;
    service_guids_len = 0;
    proto_elem_list = NULL;
    proto_num_elem = 0;
}

WIDCOMMStackServerConnectionBase::~WIDCOMMStackServerConnectionBase() {
    if (sdpService != NULL) {
        delete sdpService;
        sdpService = NULL;
    }
    if (proto_elem_list != NULL) {
        delete proto_elem_list;
        proto_elem_list = NULL;
    }
    if (service_guids != NULL) {
        delete service_guids;
        service_guids = NULL;
    }
}

void WIDCOMMStackServerConnectionBase::addClient(WIDCOMMStackConnectionBase* c) {
    for(int i = 0 ; i < OPEN_COMMPORTS_MAX; i ++) {
        if (NULL == conn[i]) {
            conn[i] = c;
            c->server = this;
            break;
        }
    }
}

void WIDCOMMStackServerConnectionBase::closeClient(JNIEnv *env, WIDCOMMStackConnectionBase* c) {
    c->server = NULL;
    for(int i = 0 ; i < OPEN_COMMPORTS_MAX; i ++) {
        const void* p = conn[i];
        if (p == c) {
            conn[i] = NULL;
            break;
        }
    }
    c->close(env, false);
}

void WIDCOMMStackServerConnectionBase::close(JNIEnv *env, BOOL allowExceptions) {
    for(int i = 0 ; i < OPEN_COMMPORTS_MAX; i ++) {
        WIDCOMMStackConnectionBase* c = conn[i];
        if (c != NULL) {
            if (isValidStackObject(c)) {
                debug(("s(%i) close client #%i c(%i)", internalHandle, i, c->internalHandle));
                c->close(env, false);
            }
            conn[i] = NULL;
        }
    }

    if (sdpService != NULL) {
        delete sdpService;
        sdpService = NULL;
    }
    isClosing = TRUE;
    isConnected = FALSE;
    SetEvent(hConnectionEvent);
}

// --- SDP ---

BOOL WIDCOMMStackServerConnectionBase::finalizeSDPRecord(JNIEnv *env) {
    if (sdpRecordCommited) {
        return TRUE;
    }
    sdpRecordCommited = TRUE;
    CSdpService* sdpService = this->sdpService;
    if (sdpService == NULL) {
        throwServiceRegistrationException(env, cCONNECTION_IS_CLOSED);
        return FALSE;
    }

    SDP_RETURN_CODE rc;
    rc = sdpService->AddServiceClassIdList(this->service_guids_len, this->service_guids);
	if (rc != SDP_OK) {
		throwServiceRegistrationException(env, "Failed to addServiceClassIdList (%i)", (int)rc);
		return FALSE;
	}
    rc = sdpService->AddServiceName(this->service_name);
	if (rc != SDP_OK) {
		throwServiceRegistrationException(env, "Failed to addServiceName (%i)", (int)rc);
		return FALSE;
	}

    rc = sdpService->AddProtocolList(this->proto_num_elem, this->proto_elem_list);
	if (rc != SDP_OK) {
		throwServiceRegistrationException(env, "Failed to addProtocolList (%i)", (int)rc);
		return FALSE;
	}

	UINT32 service_name_len;
	#ifdef _WIN32_WCE
		service_name_len = wcslen((wchar_t*)this->service_name);
	#else // _WIN32_WCE
		service_name_len = (UINT32)strlen(this->service_name);
	#endif // #else // _WIN32_WCE

    rc = sdpService->AddAttribute(0x0100, TEXT_STR_DESC_TYPE, service_name_len, (UINT8*)this->service_name);
	if (rc != SDP_OK) {
		throwServiceRegistrationException(env, "Failed to addAttribute ServiceName (%i)", (int)rc);
		return FALSE;
	}
	debug(("service_name assigned [%s]", this->service_name));

    /*
	rc = sdpService->MakePublicBrowseable();
	if (rc != SDP_OK) {
		throwIOException(env, "Failed to MakePublicBrowseable (%i)", (int)rc);
		return FALSE;
	}
	sdpService->SetAvailability(255);
	*/

	return TRUE;
}

WIDCOMMStackServerConnectionBase* getServerConnection(JNIEnv *env, jlong handle, jchar handleType) {
    if (handleType == 'r') {
        WIDCOMMStackRfCommPortServer* rf = validRfCommServerHandle(env, handle);
        if (rf == NULL) {
            return  NULL;
        }
        if (rf->sdpService == NULL) {
            throwServiceRegistrationException(env, cCONNECTION_IS_CLOSED);
            return NULL;
        }
        return rf;
    } else if (handleType == 'l') {
        WIDCOMMStackL2CapServer* l2c = validL2CapServerHandle(env, handle);
        if (l2c == NULL) {
            return NULL;
        }
        if (l2c->sdpService == NULL) {
            throwServiceRegistrationException(env, cCONNECTION_IS_CLOSED);
            return NULL;
        }
        return l2c;
    } else {
        throwServiceRegistrationException(env, cCONNECTION_IS_CLOSED);
        return NULL;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_sdpServiceAddAttribute
(JNIEnv *env, jobject, jlong handle, jchar handleType, jint attrID, jshort attrType, jbyteArray value) {
    WIDCOMMStackServerConnectionBase* srv = getServerConnection(env, handle, handleType);
    if (srv == NULL) {
        return;
    }
    if (!srv->finalizeSDPRecord(env)) {
        return;
    }
    CSdpService* sdpService = srv->sdpService;

    #define ATTR_LEN_MAX 497
    UINT32 arrLen = 0;
    UINT8 *p_val;
    UINT8 val[ATTR_LEN_MAX];

    p_val = val;
    // = new UINT8[2 * arrLen + 1];

    if (value != NULL) {
        jbyte *inBytes = env->GetByteArrayElements(value, 0);
        arrLen = (UINT8)env->GetArrayLength(value);

        for (UINT8 i = 0; ((i < arrLen) && (i < ATTR_LEN_MAX)); i++ ) {
            p_val[i] = (UINT8)inBytes[i];
        }
        env->ReleaseByteArrayElements(value, inBytes, 0);
    }
    p_val[arrLen] = '\0';

    //if (attrType == TEXT_STR_DESC_TYPE) {
    //    arrLen ++;
    //}

    debug(("AddAttribute %i type=%i len=%i [%s]", attrID, attrType, arrLen, p_val));
    SDP_RETURN_CODE rc = sdpService->AddAttribute((UINT16)attrID, (UINT8)attrType, arrLen, p_val);
    if (rc != SDP_OK) {
        throwServiceRegistrationException(env, "Failed to addAttribute %i (%i)", attrID, (int)rc);
    }

    //delete p_val;

}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_sdpServiceAddServiceClassIdList
(JNIEnv *env, jobject, jlong handle, jchar handleType, jobjectArray uuidArray) {
    WIDCOMMStackServerConnectionBase* srv = getServerConnection(env, handle, handleType);
    if (srv == NULL) {
        return;
    }
    int service_guids_len = env->GetArrayLength(uuidArray);
    GUID *service_guids = new GUID[service_guids_len];
    if (service_guids == NULL) {
        throwIOException(env, cOUT_OF_MEMORY);
        return;
    }
    for(int i = 0; i < service_guids_len; i++) {
        jbyteArray uuidValue = (jbyteArray) env->GetObjectArrayElement(uuidArray, i);
	    jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
	    convertUUIDBytesToGUID(bytes, &(service_guids[i]));
	    env->ReleaseByteArrayElements(uuidValue, bytes, 0);
	}

	if (srv->service_guids != NULL) {
	    delete srv->service_guids;
	}
	srv->service_guids = service_guids;
	srv->service_guids_len = service_guids_len;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testWIDCOMMConstants
(JNIEnv *env, jclass) {

    if (NULL_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_NULL_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (UINT_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_UINT_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (TWO_COMP_INT_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_TWO_COMP_INT_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (UUID_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_UUID_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (TEXT_STR_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_TEXT_STR_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (BOOLEAN_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_BOOLEAN_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (DATA_ELE_SEQ_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_DATA_ELE_SEQ_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (DATA_ELE_ALT_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_DATA_ELE_ALT_DESC_TYPE) {
        return JNI_FALSE;
    }

    if (URL_DESC_TYPE != com_intel_bluetooth_BluetoothStackWIDCOMM_URL_DESC_TYPE) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

#endif //  _BTWLIB
