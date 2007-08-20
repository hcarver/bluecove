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

#include "BlueSoleilStack.h"

#ifdef VC6
#define CPP_FILE "BlueSoleilStack.cpp"
#endif

BOOL isBlueSoleilBluetoothStackPresent(JNIEnv *env) {
    HMODULE h = LoadLibrary(BLUESOLEIL_DLL);
    if (h == NULL) {
        return FALSE;
    }
    FreeLibrary(h);
    return TRUE;
}

#ifdef _BLUESOLEIL

void BsAddrToString(wchar_t* addressString, BYTE* address) {
    swprintf_s(addressString, 14, L"%02x%02x%02x%02x%02x%02x",
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
        address[i] = (BYTE)(addr & 0xFF);
        addr >>= 8;
    }
}

jint BsDeviceClassToInt(BYTE* devClass) {
    return (((devClass[0] << 8) + devClass[1]) << 8) + devClass[2];
}

//API calling status code to String
char * getBsAPIStatusString(DWORD dwResult) {
    switch (dwResult) {
        case BTSTATUS_FAIL: return "General fail";
        case BTSTATUS_SUCCESS: return "Success";
        case BTSTATUS_SYSTEM_ERROR: return "System error";
        case BTSTATUS_BT_NOT_READY: return "BT_NOT_READY";
        case BTSTATUS_ALREADY_PAIRED: return "BlueSoleil is already paired with the device";
        case BTSTATUS_AUTHENTICATE_FAILED: return "Authentication fails";
        case BTSTATUS_BT_BUSY: return "Bluetooth is busy with browsing services or connecting to a device";
        case BTSTATUS_CONNECTION_EXIST: return "The connection to the service is already established";
        case BTSTATUS_CONNECTION_NOT_EXIST: return "The connection does not exist or is released";
        case BTSTATUS_PARAMETER_ERROR: return "PARAMETER_ERROR";
        case BTSTATUS_SERVICE_NOT_EXIST: return "SERVICE_NOT_EXIST";
        case BTSTATUS_DEVICE_NOT_EXIST: return "DEVICE_NOT_EXIST";
        default:
            return "Unknown BlueSoleil error";
    }
}

static BOOL BlueSoleilStarted = FALSE;

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getLibraryVersion
(JNIEnv *, jobject) {
    return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_detectBluetoothStack
(JNIEnv *env, jobject) {
    return detectBluetoothStack(env);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_enableNativeDebug
  (JNIEnv *env, jobject, jclass loggerClass, jboolean on) {
    enableNativeDebug(env, loggerClass, on);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_initializeImpl
(JNIEnv *env, jobject) {
    if (BT_InitializeLibrary()) {
        if (BT_IsBluetoothReady(10)) {
            BlueSoleilStarted = TRUE;
            stack = new BlueSoleilStack();
            return JNI_TRUE;
        } else {
            debug("Error in BlueSoleil BT_IsBluetoothReady");
        }
    } else {
        debug("Error in BlueSoleil InitializeLibrary");
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_uninitialize
(JNIEnv *env, jobject) {
    if (stack != NULL) {
        BlueSoleilStack* stackTmp = stack;
        stack = NULL;
        delete stackTmp;
    }
    if (BlueSoleilStarted) {
        BlueSoleilStarted = FALSE;
        BT_UninitializeLibrary();
    }
}

BOOL BsGetLocalDeviceInfo(JNIEnv *env, DWORD dwMask, PBLUETOOTH_DEVICE_INFO_EX pDevInfo) {
    memset(pDevInfo, 0, sizeof(BLUETOOTH_DEVICE_INFO_EX));
    pDevInfo->dwSize = sizeof(BLUETOOTH_DEVICE_INFO_EX);
    DWORD dwResult = BT_GetLocalDeviceInfo(dwMask, pDevInfo);
    if (dwResult != BTSTATUS_SUCCESS) {
        debugs("BT_GetLocalDeviceInfo return  [%s]", getBsAPIStatusString(dwResult));
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

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getDeviceClassImpl
(JNIEnv *env, jobject) {
    return getDeviceClassByOS(env);
}

// --- Device Inquiry

//void BsOnDeviceResponded(PBLUETOOTH_DEVICE_INFO pDevInfo) {
//}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    // We do Asynchronous call and there are no way to see when inquiry is started.
    // TODO Create Thread here.

//  DWORD dwResult = BT_RegisterCallback(EVENT_INQUIRY_DEVICE_REPORT, &BsOnDeviceResponded);
//  if (dwResult != BTSTATUS_SUCCESS) {
//      //throwException(env, "javax/bluetooth/BluetoothStateException", "Can't RegisterCallback");
//      return INQUIRY_ERROR;
//  }

    jclass peerClass = env->GetObjectClass(peer);
    if (peerClass == NULL) {
        throwRuntimeException(env, "Fail to get Object Class");
        return INQUIRY_ERROR;
    }

    jmethodID deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;Z)V");
    if (deviceDiscoveredCallbackMethod == NULL) {
        throwRuntimeException(env, "Fail to get MethodID deviceInquiryStartedCallback");
        return INQUIRY_ERROR;
    }

    UCHAR  ucInqMode = INQUIRY_GENERAL_MODE;
    if (accessCode == LIAC) {
        ucInqMode = INQUIRY_LIMITED_MODE;
    }
    UCHAR ucInqLen = 0x0A; //~~ 15 sec
    BLUETOOTH_DEVICE_INFO   lpDevsList[DEVICE_RESPONDED_MAX] = {0};
    DWORD devsListLen = sizeof(BLUETOOTH_DEVICE_INFO) * DEVICE_RESPONDED_MAX;

    stack->inquiringDevice = TRUE;
    DWORD dwResult = BT_InquireDevices(ucInqMode, ucInqLen, &devsListLen, lpDevsList);
    stack->inquiringDevice = FALSE;
    if (dwResult != BTSTATUS_SUCCESS) {
        debugs("BT_InquireDevices return  [%s]", getBsAPIStatusString(dwResult));
        return INQUIRY_ERROR;
    }

    for (DWORD i=0; i < ((devsListLen)/sizeof(BLUETOOTH_DEVICE_INFO)); i++) {
        BLUETOOTH_DEVICE_INFO *pDevice = (BLUETOOTH_DEVICE_INFO*)((UCHAR*)lpDevsList + i * sizeof(BLUETOOTH_DEVICE_INFO));
        jlong deviceAddr = BsAddrToLong(pDevice->address);


        BLUETOOTH_DEVICE_INFO_EX devInfo = {0};
        memcpy(&devInfo.address, pDevice->address, DEVICE_ADDRESS_LENGTH);
        devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO_EX);
        devInfo.szName[0] = '\0';
        BT_GetRemoteDeviceInfo(MASK_DEVICE_NAME | MASK_DEVICE_CLASS, &devInfo);
        jboolean paired = pDevice->bPaired;

        jint deviceClass = BsDeviceClassToInt(devInfo.classOfDevice);

        env->CallVoidMethod(peer, deviceDiscoveredCallbackMethod, listener, deviceAddr, deviceClass, env->NewStringUTF((char*)(devInfo.szName)), paired);
        if (ExceptionCheckCompatible(env)) {
           return INQUIRY_ERROR;
        }
    }

    return INQUIRY_COMPLETED;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_cancelInquirympl
(JNIEnv *env, jobject){
    if (stack == NULL) {
        return FALSE;
    }
    if (!stack->inquiringDevice) {
        return FALSE;
    }
    DWORD dwResult = BT_CancelInquiry();
    if (dwResult != BTSTATUS_SUCCESS) {
        debugs("BT_CancelInquiry return  [%s]", getBsAPIStatusString(dwResult));
        return FALSE;
    } else {
        return TRUE;
    }
}

// --- Service search

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_runSearchServicesImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jobject listener, jbyteArray uuidValue, jlong address, jobject device)  {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
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
    if (dwResult != BTSTATUS_SUCCESS)   {
        debugs("BT_SearchSPPExServices return  [%s]", getBsAPIStatusString(dwResult));
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

    jmethodID servicesFoundCallbackMethod = env->GetMethodID(peerClass, "servicesFoundCallback", "(Lcom/intel/bluetooth/SearchServicesThread;Ljavax/bluetooth/DiscoveryListener;Ljavax/bluetooth/RemoteDevice;Ljava/lang/String;[BIJ)V");
    if (servicesFoundCallbackMethod == NULL) {
        throwRuntimeException(env, "Fail to get MethodID servicesFoundCallback");
        return SERVICE_SEARCH_ERROR;
    }
    debugs("services found: %i", dwLength / sizeof(SPPEX_SERVICE_INFO));
    for(DWORD i = 0; i < dwLength / sizeof(SPPEX_SERVICE_INFO); i++) {
        SPPEX_SERVICE_INFO* sr = &(sppex_svc_info[i]);
        if (sr->dwSDAPRecordHanlde == 0) {
            continue;
        }

        debugs("SDAP Record Handle: %d", sr->dwSDAPRecordHanlde);
        debugs("      Service Name: %s", sr->szServiceName);
        debugs("   Service Channel: %02X", sr->ucServiceChannel);
        debugs("         Com Index: %i", sr->ucComIndex);

        jbyteArray uuidValueFound = env->NewByteArray(16);
        jbyte *bytes = env->GetByteArrayElements(uuidValueFound, 0);

        GUID found_service_guid;
        memcpy(&found_service_guid, &(sr->serviceClassUuid128), sizeof(UUID));
        convertGUIDToUUIDBytes(&found_service_guid, bytes);

        env->ReleaseByteArrayElements(uuidValueFound, bytes, 0);


        //DiscoveryListener listener, RemoteDevice device, String serviceName, byte[] uuidValue, int channel
        env->CallVoidMethod(peer, servicesFoundCallbackMethod,
            startedNotify, listener, device, env->NewStringUTF((char*)(sr->szServiceName)), uuidValueFound, (jint)sr->ucServiceChannel, (jlong)sr->dwSDAPRecordHanlde);

        if (ExceptionCheckCompatible(env)) {
           return SERVICE_SEARCH_ERROR;
        }
    }

    return SERVICE_SEARCH_COMPLETED;
}

//   --- Client RFCOMM connections

void BS_SPPEXConnectionCallback(DWORD dwServerHandle, BYTE* lpBdAddr, UCHAR ucStatus, DWORD dwConnetionHandle) {
    if (stack != NULL) {
        stack->SPPEXConnectionCallback(dwServerHandle, lpBdAddr, ucStatus, dwConnetionHandle);
    }
}

BlueSoleilStack::BlueSoleilStack() {
    inquiringDevice = FALSE;
    InitializeCriticalSection(&openingPortLock);

    commPortsPool = new ObjectPool(COMMPORTS_POOL_MAX, 1, FALSE);
    servicesPool = new ObjectPool(SERVERS_POOL_MAX, 1000, FALSE);

    BT_RegisterCallback(EVENT_SPPEX_CONNECTION_STATUS, BS_SPPEXConnectionCallback);
}

BlueSoleilStack::~BlueSoleilStack() {
    BT_UnregisterCallback(EVENT_SPPEX_CONNECTION_STATUS);
    delete servicesPool;
    delete commPortsPool;

    DeleteCriticalSection(&openingPortLock);
    if (inquiringDevice) {
        BT_CancelInquiry();
        inquiringDevice = FALSE;
    }
}

BlueSoleilCOMPort* BlueSoleilStack::createCommPort() {
    BlueSoleilCOMPort* rf = new BlueSoleilCOMPort();
    if (!commPortsPool->addObject(rf)) {
        delete rf;
        return NULL;
    }
    return rf;
}

BlueSoleilCOMPort* validRfCommHandle(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    return stack->getCommPort(env, handle);
}

BlueSoleilCOMPort* BlueSoleilStack::getCommPort(JNIEnv *env, jlong handle) {
    return (BlueSoleilCOMPort*)commPortsPool->getObject(env, handle);
}

void BlueSoleilStack::deleteCommPort(BlueSoleilCOMPort* commPort) {
    if (commPort != NULL) {
        commPortsPool->removeObject(commPort);
        delete commPort;
    }
}

BlueSoleilSPPExService* BlueSoleilStack::createService() {
    BlueSoleilSPPExService* o = new BlueSoleilSPPExService();
    if (!servicesPool->addObject(o)) {
        delete o;
        return NULL;
    }
    return o;
}

BlueSoleilSPPExService* validServiceHandle(JNIEnv *env, jlong handle) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return NULL;
    }
    return stack->getService(env, handle);
}

BlueSoleilSPPExService* BlueSoleilStack::getService(JNIEnv *env, jlong handle) {
    return (BlueSoleilSPPExService*)servicesPool->getObject(env, handle);
}

void BlueSoleilStack::deleteService(BlueSoleilSPPExService* service) {
    if (service != NULL) {
        servicesPool->removeObject(service);
        delete service;
    }
}


void BlueSoleilStack::SPPEXConnectionCallback(DWORD dwServerHandle, BYTE* lpBdAddr, UCHAR ucStatus, DWORD dwConnetionHandle) {
    BlueSoleilSPPExService* service = (BlueSoleilSPPExService*)servicesPool->getObjectByExternalHandle(dwServerHandle);
    if (service != NULL) {
        service->SPPEXConnectionCallback(lpBdAddr, ucStatus, dwConnetionHandle);
    }
}

BlueSoleilCOMPort::BlueSoleilCOMPort() {
    portMagic1 = MAGIC_1;
    hComPort = INVALID_HANDLE_VALUE;
    dwConnectionHandle = 0;
    isClosing = FALSE;
    receivedEOF = FALSE;
    memset(&ovlRead, 0, sizeof(OVERLAPPED));
    memset(&ovlWrite, 0, sizeof(OVERLAPPED));
    memset(&ovlComState, 0, sizeof(OVERLAPPED));
}

BlueSoleilCOMPort::~BlueSoleilCOMPort() {
    portMagic1 = 0;
    close(NULL);
}

BOOL BlueSoleilCOMPort::isValidObject() {
    return (portMagic1 == MAGIC_1);
}

BOOL BlueSoleilCOMPort::openComPort(JNIEnv *env, int portN) {
    char portString[20];
    sprintf_s(portString, 20, "\\\\.\\COM%i", portN);
    debugs("open COM port [%s]", portString);
    hComPort = CreateFileA(portString, GENERIC_READ | GENERIC_WRITE,
        0, /* exclusive access */
        NULL, /* no security attrs */
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED, /* overlapped I/O */
        NULL);
    if (hComPort == INVALID_HANDLE_VALUE) {
        DWORD last_error = GetLastError();
        char message[80];
        sprintf_s(message, 80, "Can't open COM port [%s]", portString);
        debug(message);
        throwIOExceptionWinErrorMessage(env, message, last_error);
        return FALSE;
    }
    return TRUE;
}

char* BlueSoleilCOMPort::configureComPort(JNIEnv *env) {

    /* get any early notifications */
    //DWORD dwEvtMask = EV_RXCHAR | EV_ERR | EV_BREAK | EV_RLSD | EV_TXEMPTY | EV_DSR | EV_CTS;
    DWORD dwEvtMask = EV_RXCHAR | EV_ERR | EV_BREAK | EV_RLSD;
    if (!SetCommMask(hComPort, dwEvtMask)) {
        return "SetCommMask error";
    }

    /* setup device buffers */
    if (!SetupComm(hComPort, 0x1000, 0)) {
        return "SetupComm error";
    }

    /* purge any information in the buffer */
    if (!PurgeComm(hComPort, PURGE_TXABORT | PURGE_RXABORT |  PURGE_TXCLEAR | PURGE_RXCLEAR)) {
        return "PurgeComm error";
    }

    COMMTIMEOUTS commTimeouts;

    if (!GetCommTimeouts(hComPort, &commTimeouts)) {
        return "GetCommTimeouts error";
    }
    Edebugs("commTimeouts.ReadIntervalTimeout         [%i]", commTimeouts.ReadIntervalTimeout);
    Edebugs("commTimeouts.ReadTotalTimeoutConstant    [%i]", commTimeouts.ReadTotalTimeoutConstant);
    Edebugs("commTimeouts.ReadTotalTimeoutMultiplier  [%i]", commTimeouts.ReadTotalTimeoutMultiplier);
    Edebugs("commTimeouts.WriteTotalTimeoutConstant   [%i]", commTimeouts.WriteTotalTimeoutConstant);
    Edebugs("commTimeouts.WriteTotalTimeoutMultiplier [%i]", commTimeouts.WriteTotalTimeoutMultiplier);

    /* set up for overlapped I/O */
    commTimeouts.ReadIntervalTimeout = 0xFFFFFFFF;
    commTimeouts.ReadTotalTimeoutConstant = 1000;
    commTimeouts.ReadTotalTimeoutMultiplier = 0;
    commTimeouts.WriteTotalTimeoutConstant = 0;
    commTimeouts.WriteTotalTimeoutMultiplier = 10;
    if (!SetCommTimeouts (hComPort, &commTimeouts)) {
        return "SetCommTimeouts error";
    }

    DCB dcb;
    memset(&dcb, 0, sizeof(DCB));
    dcb.DCBlength = sizeof(DCB);
    if (!GetCommState(hComPort, &dcb)) {
        return "GetCommState error";
    }
    // Fill in DCB: 115,200 bps, 8 data bits, no parity, and 1 stop bit.
    dcb.BaudRate = CBR_115200;    // set the baud rate
    dcb.ByteSize = 8;             // data size, xmit, and rcv
    dcb.Parity = NOPARITY;        // no parity bit
    dcb.StopBits = ONESTOPBIT;    // one stop bit
    dcb.fAbortOnError = TRUE;

    dcb.fBinary = TRUE;

    /*
    dcb.fOutxDsrFlow = 0;
    BOOL hardwareHandshake = TRUE;
    if (hardwareHandshake) {
        dcb.fOutxCtsFlow = TRUE;
        dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
    } else {
        dcb.fOutxCtsFlow = FALSE;
        dcb.fRtsControl = RTS_CONTROL_ENABLE;
    }
    */

    if (!SetCommState(hComPort, &dcb)) {
        return "SetCommState error";
    }

    /*
    if (!hardwareHandshake) {
        if (!EscapeCommFunction(hComPort, SETRTS)) { //Sends the DTR (data-terminal-ready) signal.
            return "EscapeCommFunction error";
        }
    if (!EscapeCommFunction(hComPort, SETDTR)) { //Sends the RTS (request-to-send) signal.
            return "EscapeCommFunction error";
        }
    }
    */

    ovlRead.hEvent = CreateEvent(
            NULL,    // no security attributes
            TRUE,    // (bManualReset) auto-reset event
            FALSE,   // initial state is NOT signaled
            NULL);   // object not named
    if (ovlRead.hEvent == NULL) {
        return "Error creating overlapped event";
    }

    ovlWrite.hEvent = CreateEvent(
            NULL,    // no security attributes
            TRUE,    // (bManualReset) auto-reset event
            FALSE,   // initial state is NOT signaled
            NULL);   // object not named
    if (ovlWrite.hEvent == NULL) {
        return "Error creating overlapped event";
    }

    hCloseEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,    // (bManualReset) auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
    if (hCloseEvent == NULL) {
        return "Error creating event";
    }
    ovlComState.hEvent = CreateEvent(
            NULL,     // no security attributes
            TRUE,     // (bManualReset) auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
    if (ovlComState.hEvent == NULL) {
        return "Error creating event";
    }

    return NULL;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfOpenImpl
(JNIEnv *env, jobject, jlong address, jbyteArray uuidValue) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }

    BlueSoleilCOMPort* rf = stack->createCommPort();
    if (rf == NULL) {
        throwIOException(env, "No free connections Objects in Pool");
        return 0;
    }
    rf->remoteAddress = address;

    BLUETOOTH_DEVICE_INFO devInfo={0};
    devInfo.dwSize = sizeof(BLUETOOTH_DEVICE_INFO);
    LongToBsAddr(address, devInfo.address);

    SPPEX_SERVICE_INFO svcInfoSPPEx;
    memset(&svcInfoSPPEx, 0, sizeof(SPPEX_SERVICE_INFO));
    svcInfoSPPEx.dwSize = sizeof(SPPEX_SERVICE_INFO);

    GUID service_guid;
    jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
    convertUUIDBytesToGUID(bytes, &service_guid);
    env->ReleaseByteArrayElements(uuidValue, bytes, 0);
    memcpy(&(svcInfoSPPEx.serviceClassUuid128), &service_guid, sizeof(UUID));

    DWORD dwConnectionHandle;
    EnterCriticalSection(&stack->openingPortLock);

    DWORD dwResult;
    dwResult = BT_ConnectSPPExService(&devInfo, &svcInfoSPPEx, &dwConnectionHandle);
    if (dwResult != BTSTATUS_SUCCESS)   {
        debugs("BT_ConnectSPPExService return  [%s]", getBsAPIStatusString(dwResult));
        stack->deleteCommPort(rf);
        LeaveCriticalSection(&stack->openingPortLock);
        throwIOExceptionExt(env, "Can't connect [%s]", getBsAPIStatusString(dwResult));
        return 0;
    }
    if (dwConnectionHandle == 0) {
        stack->deleteCommPort(rf);
        BT_DisconnectSPPExService(dwConnectionHandle);
        LeaveCriticalSection(&stack->openingPortLock);
        throwIOException(env, "Can't use 0 Handle");
        return 0;
    }

    BOOL bIsOutGoing;
    SPP_CONNECT_INFO sppConnInfo ={0};
    sppConnInfo.dwSize = sizeof(SPP_CONNECT_INFO);
    DWORD dwLen = sizeof(SPP_CONNECT_INFO);
    BYTE bdAddr[6]={0};
    WORD wClass;

    dwResult = BT_GetConnectInfo(dwConnectionHandle, &bIsOutGoing, &wClass, bdAddr, &dwLen, (BYTE*)&sppConnInfo);
    if (dwResult != BTSTATUS_SUCCESS)   {
        debugs("BT_GetConnectInfo return  [%s]", getBsAPIStatusString(dwResult));
        stack->deleteCommPort(rf);
        LeaveCriticalSection(&stack->openingPortLock);
        throwIOExceptionExt(env, "Can't get SPP info [%s]", getBsAPIStatusString(dwResult));
        return 0;
    }

    if (sppConnInfo.ucComPort != svcInfoSPPEx.ucComIndex) {
        debug2("Port# mismatch [%i] and [%i]", sppConnInfo.ucComPort, svcInfoSPPEx.ucComIndex);
        stack->deleteCommPort(rf);
        LeaveCriticalSection(&stack->openingPortLock);
        throwIOExceptionExt(env, "Port# mismatch [%i] and [%i]", sppConnInfo.ucComPort, svcInfoSPPEx.ucComIndex);
    }

    // To solve concurrent connections problem
    //Sleep(5000);

    int portN;
    //portN = svcInfoSPPEx.ucComIndex;
    portN = sppConnInfo.ucComPort;

    debug2("open COM port [%i] for [%i]", portN, address);
    if (!rf->openComPort(env, portN)) {
        stack->deleteCommPort(rf);
        BT_DisconnectSPPExService(dwConnectionHandle);
        LeaveCriticalSection(&stack->openingPortLock);
        return 0;
    }
    rf->dwConnectionHandle = dwConnectionHandle;

    char* errorMessage = rf->configureComPort(env);
    if (errorMessage != NULL) {
        DWORD last_error = GetLastError();
        stack->deleteCommPort(rf);
        LeaveCriticalSection(&stack->openingPortLock);
        throwIOExceptionWinErrorMessage(env, errorMessage, last_error);
        return 0;
    }
    LeaveCriticalSection(&stack->openingPortLock);
    debug3("Connected [%i] [%p]-[%i]", rf->internalHandle, rf->hComPort, rf->dwConnectionHandle);
    return rf->internalHandle;
}

void BlueSoleilCOMPort::clearCommError() {
    if (isClosing) {
        return;
    }
    dwErrorFlags = 0;
    ClearCommError(hComPort, &dwErrorFlags, &comStat);
    if (dwErrorFlags != 0) {
        comStat.fEof = TRUE;
    }
}

void BlueSoleilCOMPort::close(JNIEnv *env) {
    BOOL error = FALSE;
    DWORD last_error = 0;

    debug3("close [%i] [%p]-[%i]", internalHandle, hComPort, dwConnectionHandle);

    if (hCloseEvent != NULL) {
        isClosing = TRUE;
        SetEvent(hCloseEvent);
    }

    if (ovlWrite.hEvent != NULL) {
        CloseHandle(ovlWrite.hEvent);
        ovlWrite.hEvent = NULL;
    }
    if (ovlRead.hEvent != NULL) {
        CloseHandle(ovlRead.hEvent);
        ovlRead.hEvent = NULL;
    }
    if (ovlComState.hEvent != NULL) {
        CloseHandle(ovlComState.hEvent);
        ovlComState.hEvent = NULL;
    }

    if (hComPort != INVALID_HANDLE_VALUE) {
        /* disable event notification and wait for thread to halt */
        //SetCommMask(hComPort, 0);

        /* drop DTR */
        //EscapeCommFunction(hComPort, CLRDTR);

        /* purge any outstanding reads/writes and close device handle */
        //PurgeComm(hComPort, PURGE_TXABORT | PURGE_RXABORT | PURGE_TXCLEAR | PURGE_RXCLEAR);

        if (!CloseHandle(hComPort)) {
            last_error = GetLastError();
            debugss("close ComPort error [%d] %S", last_error, getWinErrorMessage(last_error));
            error = TRUE;
        }
        hComPort = INVALID_HANDLE_VALUE;
    }

    if (hCloseEvent != NULL) {
        CloseHandle(hCloseEvent);
        hCloseEvent = NULL;
    }

    DWORD dwResult = BTSTATUS_SUCCESS;
    if (dwConnectionHandle != 0) {
        //Sleep(5500);
        if (stack != NULL) {
            EnterCriticalSection(&stack->openingPortLock);
        }
        dwResult = BT_DisconnectSPPExService(dwConnectionHandle);
        if (stack != NULL) {
            LeaveCriticalSection(&stack->openingPortLock);
        }
        dwConnectionHandle = 0;
        if ((dwResult != BTSTATUS_SUCCESS) /*&& (dwResult != BTSTATUS_CONNECTION_NOT_EXIST)*/ && (env != NULL)) {
            debugs("BT_DisconnectSPPExService return  [%s]", getBsAPIStatusString(dwResult));
        }
    }

    if (env != NULL) {
        if (error) {
            throwIOExceptionWinErrorMessage(env, "close ComPort error", last_error);
            return;
        }
        if ((dwResult != BTSTATUS_SUCCESS) && (dwResult != BTSTATUS_CONNECTION_NOT_EXIST))  {
            throwIOExceptionExt(env, "Can't disconnect SPP [%s]", getBsAPIStatusString(dwResult));
            return;
        }
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfCloseClientConnection
(JNIEnv *env, jobject, jlong handle) {
    debugs("close connection [%i]", handle);
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return;
    }
    rf->close(env);
    if (stack != NULL) {
        stack->deleteCommPort(rf);
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_getConnectionRfRemoteAddress
(JNIEnv *env, jobject, jlong handle) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return 0;
    }
    if (rf->isClosing) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return 0;
    }
    return rf->remoteAddress;
}

void printCOMSTAT(JNIEnv *env, COMSTAT* comStat) {
    Edebug1("COMSTAT.fEof      %i", comStat->fEof);
    Edebug1("COMSTAT.cbInQue   %i", comStat->cbInQue);
    Edebug1("COMSTAT.cbOutQue  %i", comStat->cbOutQue);
    Edebug1("COMSTAT.fCtsHold  %i", comStat->fCtsHold);
    Edebug1("COMSTAT.fDsrHold  %i", comStat->fDsrHold);
    Edebug1("COMSTAT.fRlsdHold %i", comStat->fRlsdHold);
    Edebug1("COMSTAT.fXoffHold %i", comStat->fXoffHold);
    Edebug1("COMSTAT.fXoffSent %i", comStat->fXoffSent);
}

void printCOMEvtMask(JNIEnv *env, DWORD dwEvtMask) {
    if (dwEvtMask & EV_BREAK) {
        debug("EV_BREAK");
    }
    if (dwEvtMask & EV_ERR) {
        debug("EV_ERR");
    }
    if (dwEvtMask & EV_RXCHAR) {
        debug("EV_RXCHAR");
    }
    if (dwEvtMask & EV_RLSD) {
        debug("EV_RLSD");
    }
    if (dwEvtMask & EV_DSR) {
        debug("EV_DSR");
    }
    if (dwEvtMask & EV_CTS) {
        debug("EV_CTS");
    }
}

int waitBytesAvailable(JNIEnv *env, jobject peer, BlueSoleilCOMPort* rf) {

    HANDLE hEvents[2];
    hEvents[0] = rf->hCloseEvent;
    hEvents[1] = rf->ovlComState.hEvent;

    // In fact we make asynchronous IO synchronous Just to be able to Close it any time!
    while ((rf->comStat.cbInQue == 0) && (!rf->isClosing) && (!rf->comStat.fEof) && (!rf->receivedEOF)) {
        DWORD dwEvtMask = 0;
        debug("read WaitCommEvent");
        if (!WaitCommEvent(rf->hComPort, &dwEvtMask, &(rf->ovlComState))) {
            DWORD last_error = GetLastError();
            if ((last_error == ERROR_SUCCESS) && (last_error != ERROR_IO_PENDING)) {
                debug2("connection handle [%i] [%p]", rf->internalHandle, rf->hComPort);
                throwIOExceptionWinErrorMessage(env, "Failed to read", last_error);
                return -1;
            }
            if (last_error != ERROR_SUCCESS)  {
                debug("read WaitForMultipleObjects");
                DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, INFINITE);
                if (rc == WAIT_FAILED) {
                    throwRuntimeException(env, "WaitForMultipleObjects");
                    return -1;
                }
            }
        }
        printCOMEvtMask(env, dwEvtMask);
        if (dwEvtMask & EV_RLSD) {
            DWORD dwModemStat = 0;
            if (GetCommModemStatus(rf->hComPort, &dwModemStat)) {
                BOOL RDLS_ON = dwModemStat & MS_RLSD_ON;
                if (!RDLS_ON) {
                    rf->receivedEOF = TRUE;
                    debug("read receivedEOF");
                }
            }
        }
        rf->clearCommError();
        printCOMSTAT(env, &(rf->comStat));
        if (isCurrentThreadInterrupted(env, peer)) {
            debug("Interrupted while reading");
            return -1;
        }
    }
    if (rf->isClosing) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return -1;
    }
    if ((rf->comStat.fEof) || (rf->receivedEOF)) {
        return 0;
    }
    return rf->comStat.cbInQue;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfRead__J
(JNIEnv *env, jobject peer, jlong handle) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return -1;
    }
    debug("->read()");
    if (rf->isClosing) {
        return -1;
    }
    rf->clearCommError();
    if ((rf->comStat.fEof) || (rf->receivedEOF)) {
        return -1;
    }
    rf->tInc();
    //printCOMSTAT(env, &(rf->comStat));
    int avl = waitBytesAvailable(env, peer, rf);
    if ((avl == -1) || (avl == 0)) {
        rf->tDec();
        return -1;
    }

    HANDLE hEvents[2];
    hEvents[0] = rf->hCloseEvent;
    hEvents[1] = rf->ovlRead.hEvent;

    unsigned char c;
    DWORD numberOfBytesRead = 0;
    while ((!rf->isClosing) && (!rf->receivedEOF) && (numberOfBytesRead == 0)) {
        if (!ReadFile(rf->hComPort, &c, 1, &numberOfBytesRead, &(rf->ovlRead))) {
            DWORD last_error = GetLastError();
            if (last_error != ERROR_IO_PENDING) {
                throwIOExceptionWinErrorMessage(env, "Failed to read", last_error);
                rf->tDec();
                return -1;
            }
            while ((!rf->isClosing) && (!rf->receivedEOF) && (!GetOverlappedResult(rf->hComPort, &(rf->ovlRead), &numberOfBytesRead, FALSE))) {
                last_error = GetLastError();
                if (last_error == ERROR_SUCCESS) {
                    break;
                }
                if (last_error != ERROR_IO_INCOMPLETE) {
                    throwIOExceptionWinErrorMessage(env, "Failed to read overlapped", last_error);
                    return -1;
                }
                debug("read WaitForMultipleObjects");
                DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, 500);
                if (rc == WAIT_FAILED) {
                    throwRuntimeException(env, "WaitForMultipleObjects");
                    rf->tDec();
                    return -1;
                }
                if (isCurrentThreadInterrupted(env, peer)) {
                    debug("Interrupted while reading");
                    return -1;
                }
                rf->clearCommError();
            }
        }
        rf->clearCommError();
    }
    if (rf->isClosing) {
        rf->tDec();
        return -1;
    }
    if (numberOfBytesRead == 0) {
        rf->receivedEOF = TRUE;
        rf->tDec();
        return -1;
    }
    rf->tDec();
    return (int)c;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfRead__J_3BII
(JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return -1;
    }
    debug1("->read(byte[%i])", len);
    if (rf->isClosing) {
        return -1;
    }
    rf->clearCommError();
    if ((rf->comStat.fEof) || (rf->receivedEOF)) {
        return -1;
    }
    rf->tInc();
    jbyte *bytes = env->GetByteArrayElements(b, 0);
    int done = 0;

    HANDLE hEvents[2];
    hEvents[0] = rf->hCloseEvent;
    hEvents[1] = rf->ovlRead.hEvent;

    while (!rf->isClosing && (!rf->receivedEOF) && (done < len)) {
        int avl = waitBytesAvailable(env, peer, rf);
        if (avl == -1) {
            rf->tDec();
            return -1;
        }
        if (avl == 0) {
            break;
        }
        DWORD numberOfBytesRead = 0;
        if (!ReadFile(rf->hComPort, (void*)(bytes + off + done), len - done, &numberOfBytesRead, &(rf->ovlRead))) {
            if (GetLastError() != ERROR_IO_PENDING) {
                env->ReleaseByteArrayElements(b, bytes, 0);
                throwIOExceptionWinGetLastError(env, "Failed to read array");
                rf->tDec();
                return -1;
            }
            while ((!rf->isClosing) && (!rf->receivedEOF) && (!GetOverlappedResult(rf->hComPort, &(rf->ovlRead), &numberOfBytesRead, FALSE))) {
                DWORD last_error = GetLastError();
                if (last_error == ERROR_SUCCESS) {
                    break;
                }
                if (last_error != ERROR_IO_INCOMPLETE) {
                    env->ReleaseByteArrayElements(b, bytes, 0);
                    throwIOExceptionWinErrorMessage(env, "Failed to read array overlapped", last_error);
                    rf->tDec();
                    return -1;
                }
                DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, 500);
                if (rc == WAIT_FAILED) {
                    throwRuntimeException(env, "WaitForMultipleObjects");
                    rf->tDec();
                    return -1;
                }
                rf->clearCommError();
                if (isCurrentThreadInterrupted(env, peer)) {
                    debug("Interrupted while reading");
                    return -1;
                }
            }
        }
        rf->clearCommError();
        debug1("numberOfBytesRead [%i]", numberOfBytesRead);
        done += numberOfBytesRead;
        if (done != 0) {
            // Don't do readFully!
            break;
        }
    }

    env->ReleaseByteArrayElements(b, bytes, 0);

    if (done == 0) {
        rf->receivedEOF = TRUE;
        done = -1;
    }
    rf->tDec();
    return done;

}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfReadAvailable
(JNIEnv *env, jobject peer, jlong handle) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL || rf->isClosing) {
        return 0;
    }
    rf->clearCommError();
    return rf->comStat.cbInQue;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfFlush
(JNIEnv *env, jobject peer, jlong handle) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return;
    }
    debug("->flush");
    if (rf->isClosing || rf->receivedEOF) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return;
    }
    /* TODO This does not work. "output buffer is empty" all the time. We use rf->receivedEOF hack for now.
    rf->clearCommError();
    if (rf->comStat.fEof) {
        // EOF character received
        throwIOException(env, "Failed to flush to closed connection");
        return;
    }
    if (rf->comStat.cbOutQue == 0) {
        debug("output buffer is empty");
        return;
    }

    HANDLE hEvents[2];
    hEvents[0] = rf->hCloseEvent;
    hEvents[1] = rf->ovlComState.hEvent;

    while ((!rf->isClosing) && (!rf->comStat.fEof)) {
        DWORD dwEvtMask = 0;
        if (!WaitCommEvent(rf->hComPort, &dwEvtMask, &(rf->ovlComState))) {
            DWORD last_error = GetLastError();
            if ((last_error == ERROR_SUCCESS) && (last_error != ERROR_IO_PENDING)) {
                debug2("connection handle [%i] [%p]", rf->internalHandle, rf->hComPort);
                throwIOExceptionWinErrorMessage(env, "Failed to flush", last_error);
                return;
            }
            DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, INFINITE);
            if (rc == WAIT_FAILED) {
                throwRuntimeException(env, "WaitForMultipleObjects");
                return;
            }
        }

        if (dwEvtMask & EV_TXEMPTY) {
            debug("output buffer was sent");
            break;
        }

        if ((dwEvtMask & EV_BREAK) || (dwEvtMask & EV_ERR)) {
            throwIOException(env, "Connection flush error");
            return;
        }

    }

    if (rf->isClosing) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return;
    }
    */
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfWrite__JI
(JNIEnv *env, jobject peer, jlong handle, jint b) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return;
    }
    debug("->write(byte)");
    if (rf->isClosing) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return;
    }
    rf->clearCommError();
    if (rf->comStat.fEof || rf->receivedEOF) {
        // EOF character received
        throwIOException(env, "Failed to write to closed connection");
        return;
    }
    rf->tInc();
    HANDLE hEvents[2];
    hEvents[0] = rf->hCloseEvent;
    hEvents[1] = rf->ovlWrite.hEvent;

    char c = (char)b;
    DWORD numberOfBytesWritten = 0;
    if (!WriteFile(rf->hComPort, &c, 1, &numberOfBytesWritten, &(rf->ovlWrite))) {
        DWORD last_error = GetLastError();
        if (last_error == ERROR_SUCCESS) {
            rf->tDec();
            return;
        }
        if (last_error != ERROR_IO_PENDING) {
            debug2("connection handle [%i] [%p]", rf->internalHandle, rf->hComPort);
            throwIOExceptionWinErrorMessage(env, "Failed to write byte", last_error);
            rf->clearCommError();
            rf->tDec();
            return;
        }
        BOOL wait = TRUE;
        while (wait) {
            Edebug("write(byte) ovl wait");
            DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, 500);
            if (rf->isClosing) {
                throwIOException(env, cCONNECTION_CLOSED);
                return;
            }
            if (rc == WAIT_FAILED) {
                throwRuntimeException(env, "WaitForMultipleObjects write(byte)");
                rf->tDec();
                return;
            }
            if (!GetOverlappedResult(rf->hComPort, &(rf->ovlWrite), &numberOfBytesWritten, FALSE)) {
                last_error = GetLastError();
                if (last_error == ERROR_SUCCESS) {
                    Edebug("write(byte) GetOverlappedResult return ERROR_SUCCESS");
                    break;
                }
                if (last_error != ERROR_IO_PENDING) {
                    debug2("connection handle [%i] [%p]", handle, rf->hComPort);
                    throwIOExceptionWinErrorMessage(env, "Failed to write byte overlapped", last_error);
                    rf->tDec();
                    return;
                }
                Edebug("write(byte) wait GetOverlappedResult");
            } else {
                Edebug("write(byte) GetOverlappedResult returns SUCCESS");
                break;
            }
            if (isCurrentThreadInterrupted(env, peer)) {
                debug("Interrupted while writing");
                return;
            }
            wait = (rc != WAIT_TIMEOUT);
        }
    }
    if (numberOfBytesWritten != 1) {
        throwIOException(env, "Failed to write byte");
    }
    rf->tDec();
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_connectionRfWrite__J_3BII
(JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
    BlueSoleilCOMPort* rf = validRfCommHandle(env, handle);
    if (rf == NULL) {
        return;
    }
    debug("->write(byte[])");
    if (rf->isClosing) {
        throwIOException(env, cCONNECTION_IS_CLOSED);
        return;
    }
    rf->clearCommError();
    if (rf->comStat.fEof || rf->receivedEOF) {
        // EOF character received
        throwIOException(env, "Failed to write to closed connection");
        return;
    }

    jbyte *bytes = env->GetByteArrayElements(b, 0);

   rf->tInc();
    HANDLE hEvents[2];
    hEvents[0] = rf->hCloseEvent;
    hEvents[1] = rf->ovlWrite.hEvent;

    int done = 0;

    while ((done < len) && (!rf->isClosing)) {
        DWORD numberOfBytesWritten = 0;
        if (!WriteFile(rf->hComPort, (char *)(bytes + off + done), len - done, &numberOfBytesWritten, &(rf->ovlWrite))) {
            if (GetLastError() != ERROR_IO_PENDING) {
                env->ReleaseByteArrayElements(b, bytes, 0);
                debug2("connection handle [%i] [%p]", handle, rf->hComPort);
                throwIOExceptionWinGetLastError(env, "Failed to write array");
                rf->tDec();
                return;
            }
            BOOL wait = TRUE;
            while (wait) {
                Edebug("write(byte[]) ovl wait");
                DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, 500);
                if (rf->isClosing) {
                    throwIOException(env, cCONNECTION_CLOSED);
                    return;
                }
                if (rc == WAIT_FAILED) {
                    throwRuntimeException(env, "WaitForMultipleObjects write(byte[])");
                    rf->tDec();
                    return;
                }
                if (!GetOverlappedResult(rf->hComPort, &(rf->ovlWrite), &numberOfBytesWritten, FALSE)) {
                    DWORD last_error = GetLastError();
                    if (last_error == ERROR_SUCCESS) {
                        Edebug("write(byte[]) GetOverlappedResult return ERROR_SUCCESS");
                        break;
                    }
                    if (last_error != ERROR_IO_PENDING) {
                        env->ReleaseByteArrayElements(b, bytes, 0);
                        debug2("connection handle [%i] [%p]", handle, rf->hComPort);
                        throwIOExceptionWinErrorMessage(env, "Failed to write array overlapped", last_error);
                        rf->tDec();
                        return;
                    }
                    Edebug("write(byte[]) wait GetOverlappedResult");
                } else {
                    Edebug("write(byte[]) GetOverlappedResult returns SUCCESS");
                    break;
                }
                if (isCurrentThreadInterrupted(env, peer)) {
                    debug("Interrupted while writing");
                    return;
                }
                wait = (rc != WAIT_TIMEOUT);
            }
        }
        if (numberOfBytesWritten <= 0) {
            env->ReleaseByteArrayElements(b, bytes, 0);
            throwIOException(env, "Failed to write full array");
            rf->tDec();
            return;
        }
        done += numberOfBytesWritten;
    }

    env->ReleaseByteArrayElements(b, bytes, 0);
    rf->tDec();
}

//   --- Server RFCOMM connections

BlueSoleilSPPExService::BlueSoleilSPPExService() {
    serviceMagic1 = MAGIC_1;
    wdServerHandle = 0;
    isClosing = FALSE;
    hCloseEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,    // (bManualReset) auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
    portHandle = 0;
    isConnected = FALSE;
    dwConnectedConnetionHandle = 0;
    hConnectionEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,    // (bManualReset) auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named

}

BlueSoleilSPPExService::~BlueSoleilSPPExService() {
    serviceMagic1 = 0;
    close(NULL);
}

BOOL BlueSoleilSPPExService::isValidObject() {
    return (serviceMagic1 == MAGIC_1);
}

BOOL BlueSoleilSPPExService::isExternalHandle(jlong handle) {
    return (handle == wdServerHandle);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_rfServerOpenImpl
(JNIEnv *env, jobject, jbyteArray uuidValue, jstring name, jboolean authenticate, jboolean encrypt) {
    if (stack == NULL) {
        throwIOException(env, cSTACK_CLOSED);
        return 0;
    }
    BlueSoleilSPPExService* srv = stack->createService();
    if (srv == NULL) {
        throwIOException(env, "No free connections Objects in Pool");
        return 0;
    }

    memset(&(srv->serviceInfo), 0, sizeof(SPPEX_SERVICE_INFO));
    srv->serviceInfo.dwSize = sizeof(SPPEX_SERVICE_INFO);

    GUID service_guid;
    jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
    convertUUIDBytesToGUID(bytes, &service_guid);
    env->ReleaseByteArrayElements(uuidValue, bytes, 0);
    memcpy(&(srv->serviceInfo.serviceClassUuid128), &service_guid, sizeof(UUID));

    const char *cname = env->GetStringUTFChars(name, 0);
    sprintf_s(srv->serviceInfo.szServiceName, MAX_PATH, "%s", cname);
    env->ReleaseStringUTFChars(name, cname);

    DWORD dwResult = BT_StartSPPExService(&(srv->serviceInfo), &(srv->wdServerHandle));
    if (dwResult != BTSTATUS_SUCCESS) {
        debugs("BT_StartSPPExService return  [%s]", getBsAPIStatusString(dwResult));
        throwIOExceptionExt(env, "Can't create Service [%s]", getBsAPIStatusString(dwResult));
        return 0;
    }


    return srv->internalHandle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_rfServerSCN
(JNIEnv *env, jobject, jlong handle) {
    BlueSoleilSPPExService* srv = validServiceHandle(env, handle);
    if (srv == NULL) {
        return -1;
    }
    return srv->serviceInfo.ucServiceChannel;
}

void BlueSoleilSPPExService::close(JNIEnv *env) {
    debug3("service close [%i] [%i] port [%i]", internalHandle, wdServerHandle, portHandle);
    if (portHandle != 0) {
        BlueSoleilCOMPort* rf = validRfCommHandle(NULL, portHandle);
        if (rf != NULL) {
            rf->close(env);
            if (stack != NULL) {
                stack->deleteCommPort(rf);
            }
        }
        portHandle = 0;
    }
    isClosing = TRUE;
    if (hCloseEvent != NULL) {
        SetEvent(hCloseEvent);
    }
    DWORD dwResult = BTSTATUS_SUCCESS;
    if (wdServerHandle != 0) {
        dwResult = BT_StopSPPExService(wdServerHandle);
        if ((dwResult != BTSTATUS_SUCCESS) && (env != NULL))    {
            debugs("BT_StopSPPExService return  [%s]", getBsAPIStatusString(dwResult));
        }
        wdServerHandle = 0;
    }
    if (hConnectionEvent != NULL) {
        CloseHandle(hConnectionEvent);
        hConnectionEvent = NULL;
    }
    if (hCloseEvent != NULL) {
        CloseHandle(hCloseEvent);
        hCloseEvent = NULL;
    }
    if (dwResult != BTSTATUS_SUCCESS){
        throwIOExceptionExt(env, "Can't Stop SPP service [%s]", getBsAPIStatusString(dwResult));
        return;
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_rfServerClose
(JNIEnv *env, jobject, jlong handle, jobject) {
    BlueSoleilSPPExService* srv = validServiceHandle(env, handle);
    if (srv == NULL) {
        return;
    }
    srv->close(env);
    stack->deleteService(srv);
}

void BlueSoleilSPPExService::SPPEXConnectionCallback(BYTE* lpBdAddr, UCHAR ucStatus, DWORD dwConnetionHandle) {
    switch (ucStatus) {
    case STATUS_INCOMING_CONNECT:
        isConnected = TRUE;
        dwConnectedConnetionHandle = dwConnetionHandle;
        connectedBdAddr = BsAddrToLong(lpBdAddr);
        SetEvent(hConnectionEvent);
        break;
    case STATUS_INCOMING_DISCONNECT:
        isConnected = FALSE;

        // TODO hack for now.
        BlueSoleilCOMPort* rf = validRfCommHandle(NULL, portHandle);
        if (rf != NULL) {
            rf->receivedEOF = TRUE;
        }

        SetEvent(hConnectionEvent);
        break;
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueSoleil_rfServerAcceptAndOpenRfServerConnection
(JNIEnv *env, jobject peer, jlong handle) {
    BlueSoleilSPPExService* srv = validServiceHandle(env, handle);
    if (srv == NULL) {
        return 0;
    }
    debug3("service accept [%i] [%i] port [%i]", srv->internalHandle, srv->wdServerHandle, srv->portHandle);

    HANDLE hEvents[2];
    hEvents[0] = srv->hCloseEvent;
    hEvents[1] = srv->hConnectionEvent;

    BOOL debugWaitsOnce = TRUE;
    while ((stack != NULL) &&
        (srv->isConnected || (validRfCommHandle(NULL, srv->portHandle) != NULL))) {
        if (debugWaitsOnce) {
            debug("server waits for client prev connection to close");
            debugWaitsOnce = FALSE;
        }
        DWORD rc = WaitForMultipleObjects(1, hEvents, FALSE, 500);
        if (rc == WAIT_FAILED) {
            throwRuntimeException(env, "WaitForMultipleObjects");
            return 0;
        } else if (rc == WAIT_OBJECT_0) {
            debug1("hCloseEvent became signaled, isConnected=%s", bool2str(srv->isConnected));
        } else if (rc != WAIT_TIMEOUT) {
            debug1("server prev connection close, waits returns %s", waitResultsString(rc));
        }
        if (stack == NULL) {
            throwIOException(env, cSTACK_CLOSED);
            return 0;
        }
        if (srv->isClosing) {
            _throwInterruptedIOException(env, cCONNECTION_CLOSED);
            return 0;
        }
        if (isCurrentThreadInterrupted(env, peer)) {
            debug("Interrupted while waiting for connections");
            return 0;
        }
    }

    BOOL debugOnce = TRUE;
    while ((stack != NULL) && (!srv->isConnected)) {
        if (debugOnce) {
            debug("server waits for connection");
            debugOnce = FALSE;
        }
        DWORD rc = WaitForMultipleObjects(2, hEvents, FALSE, 500);
        if (rc == WAIT_FAILED) {
            throwRuntimeException(env, "WaitForMultipleObjects");
            return 0;
        } else if (rc == WAIT_OBJECT_0) {
            debug("hCloseEvent became signaled");
        } else if (rc == WAIT_OBJECT_0 + 1) {
            debug1("hConnectionEvent became signaled, isConnected=%s", bool2str(srv->isConnected));
        } else if (rc != WAIT_TIMEOUT) {
            debug1("server waits returns %s", waitResultsString(rc));
        }
        if (stack == NULL) {
            throwIOException(env, cSTACK_CLOSED);
            return 0;
        }
        if (srv->isClosing) {
            _throwInterruptedIOException(env, cCONNECTION_CLOSED);
            return 0;
        }
        if (isCurrentThreadInterrupted(env, peer)) {
            debug("Interrupted while waiting for connections");
            return 0;
        }
    }

    debug1("server received connection, %i", srv->dwConnectedConnetionHandle);

    /*
    BOOL bIsOutGoing;
    SPP_CONNECT_INFO sppConnInfo ={0};
    sppConnInfo.dwSize = sizeof(SPP_CONNECT_INFO);
    DWORD dwLen = sizeof(SPP_CONNECT_INFO);
    BYTE bdAddr[6]={0};
    WORD wClass;

    DWORD dwResult = BT_GetConnectInfo(srv->dwConnectedConnetionHandle, &bIsOutGoing, &wClass, bdAddr, &dwLen, (BYTE*)&sppConnInfo);
    if (dwResult != BTSTATUS_SUCCESS)   {
        debugs("BT_GetConnectInfo return  [%s]", getBsAPIStatusString(dwResult));
        throwIOExceptionExt(env, "Can't get SPP info [%s]", getBsAPIStatusString(dwResult));
        return 0;
    }
    */

    BlueSoleilCOMPort* rf = stack->createCommPort();
    if (rf == NULL) {
        throwIOException(env, "No free connections Objects in Pool");
        return 0;
    }

    //rf->remoteAddress = BsAddrToLong(bdAddr);
    rf->remoteAddress = srv->connectedBdAddr;
    //rf->dwConnectionHandle = srv->dwConnectedConnetionHandle;

    if (!rf->openComPort(env, srv->serviceInfo.ucComIndex)) {
        stack->deleteCommPort(rf);
        return 0;
    }
    char* errorMessage = rf->configureComPort(env);
    if (errorMessage != NULL) {
        DWORD last_error = GetLastError();
        stack->deleteCommPort(rf);
        throwIOExceptionWinErrorMessage(env, errorMessage, last_error);
        return 0;
    }

    srv->portHandle = rf->internalHandle;

    debug3("service connected [%i] [%i] port [%i]", srv->internalHandle, srv->wdServerHandle, srv->portHandle);

    return rf->internalHandle;
}

#endif
