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

#include "WIDCOMMStack.h"

#ifndef _BTWLIB
BOOL isWIDCOMMBluetoothStackPresent() {
	return FALSE;
}
#endif

#ifdef _BTWLIB

static WIDCOMMStack* stack;
static int openConnections = 0;
static GUID test_client_service_guid = { 0x5fc2a42e, 0x144e, 0x4bb5, { 0xb4, 0x3f, 0x4e, 0x61, 0x71, 0x1d, 0x1c, 0x32 } };

void BcAddrToString(wchar_t* addressString, BD_ADDR bd_addr) {
	swprintf_s(addressString, 14, _T("%02x%02x%02x%02x%02x%02x"),
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

BOOL isWIDCOMMBluetoothStackPresent() {
	HMODULE h = LoadLibrary(_T(WIDCOMM_DLL));
	if (h == NULL) {
		return FALSE;
	}
	FreeLibrary(h);
	return TRUE;
}

WIDCOMMStack::WIDCOMMStack() {
	hEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,    // auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
	InitializeCriticalSection(&csCRfCommIf);

	commPortsPoolDeletionCount = 0;
	commPortsPoolAllocationHandleOffset = 1;
	for(int i = 0; i < COMMPORTS_POOL_MAX; i ++) {
		commPortsPool[i] = NULL;
	}
	discoveryRecHolderCurrent = NULL;
	discoveryRecHolderHold = NULL;
}

DiscoveryRecHolder::DiscoveryRecHolder() {
	sdpDiscoveryRecordsUsed = 0;
}

WIDCOMMStack* createWIDCOMMStack() {
	return new WIDCOMMStack();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_initialize
(JNIEnv *env, jobject) {
	jboolean rc = TRUE;
	if (stack == NULL) {
		__try  {
			stack = createWIDCOMMStack();
			if (stack->hEvent == NULL) {
				throwRuntimeException(env, "fails to CreateEvent");
			}
		} __except(GetExceptionCode() == 0xC06D007E) {
			rc = FALSE;
		}
	}
	return rc;
}

WIDCOMMStack::~WIDCOMMStack() {
	SetEvent(hEvent);
	for(int i = 0; i < COMMPORTS_POOL_MAX; i ++) {
		if (commPortsPool[i] != NULL) {
			delete commPortsPool[i];
		}
	}
	if (discoveryRecHolderHold != NULL) {
		delete discoveryRecHolderHold;
	}
	if (discoveryRecHolderCurrent != NULL) {
		delete discoveryRecHolderCurrent;
	}
	CloseHandle(hEvent);
	DeleteCriticalSection(&csCRfCommIf);
}

void WIDCOMMStack::throwExtendedErrorException(JNIEnv * env, const char *name) {
	WBtRc er = GetExtendedError();
	LPCTSTR msg = WBtRcToString(er);
	if (msg != NULL) {
		throwExceptionExt(env, name, "WIDCOMM error[%s]", msg);
	} else {
		throwException(env, name, "No error code");
	}
}

void BroadcomDebugError(JNIEnv *env, CBtIf* stack) {
	WBtRc er = stack->GetExtendedError();
	LPCTSTR msg = WBtRcToString(er);
	if (msg != NULL) {
		debugs("WIDCOMM error[%s]", msg);
	} else {
		debug("No error code");
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_uninitialize
(JNIEnv *, jobject) {
	if (stack != NULL) {
		WIDCOMMStack* stackTmp = stack;
		stack = NULL;
		delete stackTmp;
	}
}


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject peer) {
	struct CBtIf::DEV_VER_INFO info;
	if (!stack->GetLocalDeviceVersionInfo(&info)) {
		stack->throwExtendedErrorException(env, "javax/bluetooth/BluetoothStateException");
		return NULL;
	}

	wchar_t addressString[14];
	BcAddrToString(addressString, info.bd_addr);
	return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceName
(JNIEnv *env, jobject peer) {
	BD_NAME name;
	if (!stack->GetLocalDeviceName(&name)) {
		BroadcomDebugError(env, stack);
		return NULL;
	}
	return env->NewStringUTF((char*)name);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isLocalDevicePowerOn
(JNIEnv *env, jobject peer) {
	return stack->IsDeviceReady();
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_isStackServerUp
(JNIEnv *env, jobject peer) {
	return stack->IsStackServerUp();
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getBTWVersionInfo
(JNIEnv *env, jobject peer) {
	BT_CHAR p_version[256];
	if (!stack->GetBTWVersionInfo(p_version, 256)) {
		return NULL;
	}
	return env->NewStringUTF((char*)p_version);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getDeviceVersion
(JNIEnv *, jobject) {
	CBtIf::DEV_VER_INFO dev_Ver_Info;
	if (!stack->GetLocalDeviceVersionInfo(&dev_Ver_Info)) {
		return -1;
	}
	return dev_Ver_Info.lmp_sub_version;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getDeviceManufacturer
(JNIEnv *, jobject) {
	CBtIf::DEV_VER_INFO dev_Ver_Info;
	if (!stack->GetLocalDeviceVersionInfo(&dev_Ver_Info)) {
		return -1;
	}
	return dev_Ver_Info.manufacturer;
}

// --- Device Inquiry

void WIDCOMMStack::OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected) {
	if (stack == NULL) {
		return;
	}
	int nextDevice = deviceRespondedIdx + 1;
	if (nextDevice >= DEVICE_FOUND_MAX) {
		nextDevice = 0;
	}
	deviceResponded[nextDevice].deviceAddr = BcAddrToLong(bda);
    deviceResponded[nextDevice].deviceClass = DeviceClassToInt(devClass);
	memcpy(deviceResponded[nextDevice].bdName, bdName, sizeof(BD_NAME));
	deviceRespondedIdx = nextDevice;
	SetEvent(hEvent);
}

void WIDCOMMStack::OnInquiryComplete(BOOL success, short num_responses) {
	if (stack == NULL) {
		return;
	}
	deviceInquirySuccess = success;
	deviceInquiryComplete = TRUE;
	SetEvent(hEvent);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {
	debug("StartDeviceInquiry");
	stack->deviceInquiryComplete = FALSE;
	stack->deviceInquiryTerminated = FALSE;

    memset(stack->deviceResponded, 0, sizeof(stack->deviceResponded));
 	stack->deviceRespondedIdx = -1;

	jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return INQUIRY_ERROR;
	}

	jmethodID deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;)V");
	if (deviceDiscoveredCallbackMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID deviceDiscoveredCallback");
		return INQUIRY_ERROR;
	}

	jclass notifyClass = env->GetObjectClass(startedNotify);
	if (notifyClass == NULL) {
		throwRuntimeException(env, "Fail to get Object Class");
		return INQUIRY_ERROR;
	}
	jmethodID notifyMethod = env->GetMethodID(notifyClass, "deviceInquiryStartedCallback", "()V");
	if (notifyMethod == NULL) {
		throwRuntimeException(env, "Fail to get MethodID deviceInquiryStartedCallback");
		return INQUIRY_ERROR;
	}

	if (!stack->StartInquiry()) {
		debug("deviceInquiryStart error");
		stack->throwExtendedErrorException(env, "javax/bluetooth/BluetoothStateException");
		return INQUIRY_ERROR;
	}
	debug("deviceInquiryStarted");

	env->CallVoidMethod(startedNotify, notifyMethod);
	if (ExceptionCheckCompatible(env)) {
		stack->StopInquiry();
		return INQUIRY_ERROR;
	}

	int reportedIdx = -1;

	while ((stack != NULL) && ((!stack->deviceInquiryComplete) || (reportedIdx != stack->deviceRespondedIdx))) {
		DWORD  rc = WaitForSingleObject(stack->hEvent, 200);
		if (rc == WAIT_FAILED) {
			throwRuntimeException(env, "WaitForSingleObject");
			return INQUIRY_ERROR;
		}
		if ((stack != NULL) && (reportedIdx != stack->deviceRespondedIdx)) {
			reportedIdx ++;
			if (reportedIdx >= DEVICE_FOUND_MAX) {
				reportedIdx = 0;
			}
			DeviceFound dev = stack->deviceResponded[reportedIdx];
			env->CallVoidMethod(peer, deviceDiscoveredCallbackMethod, listener, dev.deviceAddr, dev.deviceClass, env->NewStringUTF((char*)(dev.bdName)));
			if (ExceptionCheckCompatible(env)) {
				stack->StopInquiry();
				return INQUIRY_ERROR;
			}
		}
	}

	if (stack != NULL) {
		stack->StopInquiry();
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
(JNIEnv *env, jobject peer, jobject nativeClass) {
	stack->deviceInquiryTerminated = TRUE;
	stack->StopInquiry();
	SetEvent(stack->hEvent);
	return TRUE;
}

// --- Service search

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runSearchServicesImpl
(JNIEnv *env, jobject peer, jobject startedNotify, jbyteArray uuidValue, jlong address) {
	debug("StartSearchServices");

	BD_ADDR bda;
	LongToBcAddr(address, bda);

	wchar_t addressString[14];
	BcAddrToString(addressString, bda);
	debugs("StartSearchServices on %S", addressString);

	GUID *p_service_guid = NULL;
	GUID service_guid;
	//If uuidValue parameter is NULL, all public browseable services for the device will be reported
	if (uuidValue != NULL) {
		jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
		// build UUID
		convertUUIDBytesToGUID(bytes, &service_guid);
		env->ReleaseByteArrayElements(uuidValue, bytes, 0);
		p_service_guid = &service_guid;
		memcpy(&test_client_service_guid, &service_guid, sizeof(GUID));
	}
	if (p_service_guid == NULL) {
		debug("p_service_guid is NULL");
	}

	stack->searchServicesComplete = FALSE;
	stack->searchServicesTerminated = FALSE;

	if (!stack->StartDiscovery(bda, p_service_guid)) {
		debug("StartSearchServices error");
		stack->throwExtendedErrorException(env, "javax/bluetooth/BluetoothStateException");
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

	while ((stack != NULL) && (!stack->searchServicesComplete) && (!stack->searchServicesTerminated)) {
		DWORD  rc = WaitForSingleObject(stack->hEvent, 500);
		if (rc == WAIT_FAILED) {
			throwRuntimeException(env, "WaitForSingleObject");
			return NULL;
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
		debugs("searchServicesResultCode %i", searchServicesResultCode);
		return NULL;
	}
	if (obtainedServicesRecords <= 0) {
		return env->NewLongArray(0);
	}

	if (obtainedServicesRecords > SDP_DISCOVERY_RECORDS_DEVICE_MAX) {
		debugs("WARN too many ServicesRecords %i", obtainedServicesRecords);
		//obtainedServicesRecords = SDP_DISCOVERY_RECORDS_USED_MAX;
	} 
	debugs("obtainedServicesRecords %i", obtainedServicesRecords);
	// Retrive all Records and filter in Java
	int retriveRecords = SDP_DISCOVERY_RECORDS_DEVICE_MAX;

	// Select RecHolder
	if (stack->discoveryRecHolderCurrent == NULL) {
		stack->discoveryRecHolderCurrent = new DiscoveryRecHolder();
		stack->discoveryRecHolderCurrent->oddHolder = TRUE;
		debug("DiscoveryRecHolder created");
	}

	int useIdx = stack->discoveryRecHolderCurrent->sdpDiscoveryRecordsUsed;
	if (useIdx + retriveRecords > SDP_DISCOVERY_RECORDS_USED_MAX) {
		useIdx = 0;
		debug("DiscoveryRecHolder switch");
		// Select next RecHolder
		if (stack->discoveryRecHolderHold != NULL) {
			delete stack->discoveryRecHolderHold;
		}
		stack->discoveryRecHolderHold = stack->discoveryRecHolderCurrent;
		stack->discoveryRecHolderCurrent = new DiscoveryRecHolder();
		stack->discoveryRecHolderCurrent->oddHolder = !(stack->discoveryRecHolderHold->oddHolder);
	} else {
		debugs("DiscoveryRecHolder useIdx %i", useIdx);
	}

	wchar_t addressString2[14];
	BcAddrToString(addressString2, bda);
	debugs("ReadDiscoveryRecords on %S", addressString2);

	CSdpDiscoveryRec *sdpDiscoveryRecordsList = stack->discoveryRecHolderCurrent->sdpDiscoveryRecords + useIdx;

	//guid_filter does not work as Expected with SE Phones!
	int recSize = stack->ReadDiscoveryRecords(bda, retriveRecords, sdpDiscoveryRecordsList, NULL);
	if (recSize == 0) {
		debugs("ReadDiscoveryRecords returns empty, While expected min %i", obtainedServicesRecords);
		return NULL;
	}
	debugs("DiscoveryRecHolder +=recSize %i", recSize);
	stack->discoveryRecHolderCurrent->sdpDiscoveryRecordsUsed += recSize;

	int oddOffset = 0;
	if (stack->discoveryRecHolderCurrent->oddHolder) {
		oddOffset = SDP_DISCOVERY_RECORDS_HOLDER_MASK;
	}

	jlongArray result = env->NewLongArray(recSize);
	jlong *longs = env->GetLongArrayElements(result, 0);
	for (int r = 0; r < recSize; r ++) {
		longs[r] = oddOffset + SDP_DISCOVERY_RECORDS_HANDLE_OFFSET + useIdx + r;
	}
	env->ReleaseLongArrayElements(result, longs, 0);

	return result;
}

void WIDCOMMStack::OnDiscoveryComplete() {
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
	DiscoveryRecHolder* discoveryRecHolder;
	BOOL heedOdd = ((handle | SDP_DISCOVERY_RECORDS_HOLDER_MASK) != 0);
	if (stack->discoveryRecHolderCurrent->oddHolder == heedOdd) {
		discoveryRecHolder = stack->discoveryRecHolderCurrent;
		offset -= SDP_DISCOVERY_RECORDS_HOLDER_MASK;
	} else {
		discoveryRecHolder = stack->discoveryRecHolderHold;
	}
	if (discoveryRecHolder == NULL) {
		throwIOException(env, "Invalid DiscoveryRec holder");
		return NULL;
	}
	if ((offset < SDP_DISCOVERY_RECORDS_HANDLE_OFFSET)
		|| (offset > SDP_DISCOVERY_RECORDS_USED_MAX + SDP_DISCOVERY_RECORDS_HANDLE_OFFSET)) {
		throwIOExceptionExt(env, "Invalid DiscoveryRec handle [%i]", offset);
		return NULL;
	}
	return discoveryRecHolder->sdpDiscoveryRecords + offset - SDP_DISCOVERY_RECORDS_HANDLE_OFFSET;
}

/*
JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getServiceAttributeRFCommScn
(JNIEnv *env, jobject, jlong handle) {
	CSdpDiscoveryRec* record = validDiscoveryRec(env, handle);
	UINT8 scn = -1;
	if (record->FindRFCommScn(&scn)) {
		return scn;
	} else {
		return -1;
	}
}
*/

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

	jbyteArray result = env->NewByteArray(sizeof(SDP_DISC_ATTTR_VAL));
	jbyte *bytes = env->GetByteArrayElements(result, 0);
	memcpy(bytes, pval, sizeof(SDP_DISC_ATTTR_VAL));
	env->ReleaseByteArrayElements(result, bytes, 0);
	delete pval;
	return result;
}

//	 --- Client RFCOMM connections

// Guarded by CriticalSection csCRfCommIf
int WIDCOMMStack::getCommPortFreeIndex() {
	int freeIndex = -1;
	int minDeletionIndex = INT_MAX;
	for(int i = 0; i < COMMPORTS_POOL_MAX; i ++) {
		if (commPortsPool[i] == NULL) {
			return i;
		}
		if (!commPortsPool[i]->readyToFree) {
			continue;
		}
		if (minDeletionIndex > commPortsPool[i]->commPortsPoolDeletionIndex) {
			minDeletionIndex = commPortsPool[i]->commPortsPoolDeletionIndex;
			freeIndex = i;
		}
	}
	if ((!COMMPORTS_REUSE_OBJECTS) && (freeIndex != -1))  {
		delete commPortsPool[freeIndex];
		commPortsPool[freeIndex] = NULL;
	}

	// TODO Inc commPortsPoolAllocationHandleOffset to avoid Handle reuse

	return freeIndex;
}

WIDCOMMStackRfCommPort* WIDCOMMStack::createCommPort() {

	int freeIndex = getCommPortFreeIndex();
	if (freeIndex == -1) {
		return NULL;
	}
	if (commPortsPool[freeIndex] == NULL) {
		commPortsPool[freeIndex] = new WIDCOMMStackRfCommPort();
	}

	int internalHandle = commPortsPoolAllocationHandleOffset + freeIndex;
	WIDCOMMStackRfCommPort* rf = commPortsPool[freeIndex];
	rf->readyForReuse();
	rf->internalHandle = internalHandle;
	rf->commPortsPoolDeletionIndex = 0;

	return rf;
}

void WIDCOMMStack::deleteCommPort(WIDCOMMStackRfCommPort* commPort) {
	commPort->commPortsPoolDeletionIndex = (++commPortsPoolDeletionCount);
	commPort->readyToFree = TRUE;
}

WIDCOMMStackRfCommPort::WIDCOMMStackRfCommPort() {

	readyForReuse();

    hEvents[0] = CreateEvent(
            NULL,     // no security attributes
            FALSE,     // auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
    hEvents[1] = CreateEvent(
            NULL,     // no security attributes
            FALSE,     // auto-reset event
            FALSE,     // initial state is NOT signaled
            NULL);    // object not named

	magic1 = MAGIC_1;
	magic2 = MAGIC_2;
	openConnections ++;
}

void WIDCOMMStackRfCommPort::readyForReuse() {
	todo_buf_rcv_idx = 0;
	todo_buf_read_idx = 0;
	isConnected = FALSE;
	isConnectionError = FALSE;
	isClosing = FALSE;
	readyToFree = FALSE;
	service_name[0] = '\0';
}

WIDCOMMStackRfCommPort::~WIDCOMMStackRfCommPort() {
	magic1 = 0;
	magic2 = 0;
	if (isConnected) {
		isClosing = TRUE;
		SetEvent(hEvents[0]);
		Close();
	}
	isConnected = FALSE;
	CloseHandle(hEvents[0]);
	CloseHandle(hEvents[1]);
	openConnections --;
}

WIDCOMMStackRfCommPort* validRfCommHandle(JNIEnv *env, jlong handle) {
	if ((handle <= 0) || (stack == NULL)) {
		throwIOException(env, "Invalid handle");
		return NULL;
	}
	if ((handle < stack->commPortsPoolAllocationHandleOffset) || (handle >= stack->commPortsPoolAllocationHandleOffset + COMMPORTS_POOL_MAX))  {
		throwIOException(env, "Obsolete handle");
		return NULL;
	}
	int idx = (int)(handle - stack->commPortsPoolAllocationHandleOffset);

	WIDCOMMStackRfCommPort* rf = stack->commPortsPool[idx];
	if ((rf->magic1 != MAGIC_1) || (rf->magic2 != MAGIC_2)) {
		throwIOException(env, "Invalid or destroyed handle");
		return NULL;
	}
	return rf;
}

void WIDCOMMStackRfCommPort::OnEventReceived (UINT32 event_code) {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || isClosing) {
		return;
	}
	if (PORT_EV_CONNECTED & event_code) {
        isConnected = TRUE;
		SetEvent(hEvents[0]);
	}
	if (PORT_EV_CONNECT_ERR & event_code) {
		isConnectionError = TRUE;
		isConnected = FALSE;
		SetEvent(hEvents[0]);
	}
}

void WIDCOMMStackRfCommPort::OnDataReceived(void *p_data, UINT16 len) {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || isClosing) {
		return;
	}
	if (isConnected) {
		int accept = TODO_BUF_MAX - todo_buf_rcv_idx;
		if (len > accept) {
			len = accept;
		}
		memcpy((todo_buf + todo_buf_rcv_idx), p_data, len);
		todo_buf_rcv_idx += len;
		SetEvent(hEvents[1]);
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfOpenClientConnection
(JNIEnv *env, jobject peer, jlong address, jint channel, jboolean authenticate, jboolean encrypt) {
	BD_ADDR bda;
	LongToBcAddr(address, bda);

	EnterCriticalSection(&stack->csCRfCommIf);
	__try {
		WIDCOMMStackRfCommPort* rf = stack->createCommPort();
		if (rf == NULL) {
			throwIOException(env, "No free connections Objects in Pool");
			return 0;
		}
        debugs("RfCommPort handle %i", rf->internalHandle);
		if ((rf->hEvents[0] == NULL) || (rf->hEvents[1] == NULL)) {
			throwRuntimeException(env, "fails to CreateEvent");
			stack->deleteCommPort(rf);
			return 0;
		}
		//debug("AssignScnValue");
		// What GUID do we need in call to CRfCommIf.AssignScnValue() if we don't have any?
		memcpy(&(rf->service_guid), &test_client_service_guid, sizeof(GUID));
		if (!stack->rfCommIf.AssignScnValue(&(rf->service_guid), (UINT8)channel)) {
			stack->deleteCommPort(rf);
			throwIOException(env, "failed to assign SCN");
			return 0;
		}
		//debug("SetSecurityLevel");
		UINT8 sec_level = BTM_SEC_NONE;
		if (!stack->rfCommIf.SetSecurityLevel("bluecovesrv"/*rf->service_name*/, sec_level, FALSE)) {
			throwIOException(env, "Error setting security level");
            stack->deleteCommPort(rf);
			return 0;
        }
		//debug("OpenClient");
		CRfCommPort::PORT_RETURN_CODE rc = rf->OpenClient((UINT8)channel, bda);
		if (rc != CRfCommPort::SUCCESS) {
			// Just in case Close
			rf->Close();
			throwIOException(env, "Failed to OpenClient");
			if (stack != NULL) {
				stack->deleteCommPort(rf);
			}
			return 0;
		}

		//debug("waitFor Connection signal");
		DWORD waitStart = GetTickCount();
		while ((stack != NULL) && !rf->isConnected && !rf->isConnectionError) {
			DWORD  rc = WaitForSingleObject(rf->hEvents[0], 500);
			if (rc == WAIT_FAILED) {
				throwRuntimeException(env, "WaitForSingleObject");
				// Just in case Close
				rf->Close();
				stack->deleteCommPort(rf);
				return 0;
			}
			if ((GetTickCount() - waitStart)  > COMMPORTS_CONNECT_TIMEOUT) {
				throwIOException(env, "Connection timeout");
				// Just in case Close
				rf->Close();
				stack->deleteCommPort(rf);
				return 0;
			}
		}
		if ((stack == NULL) || rf->isConnectionError) {
			throwIOException(env, "Failed to connect");
			if (stack != NULL) {
				// Just in case Close
				rf->Close();
				stack->deleteCommPort(rf);
			}
			return 0;
		}
		debug("connected");
		return rf->internalHandle;
	} __finally {
		LeaveCriticalSection(&stack->csCRfCommIf);
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfCloseClientConnection
(JNIEnv *env, jobject peer, jlong handle) {
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL) {
		return;
	}
	//debug("CloseClientConnection");
	rf->isClosing = TRUE;
	SetEvent(rf->hEvents[0]);
	CRfCommPort::PORT_RETURN_CODE rc = rf->Close();
	if (rc != CRfCommPort::SUCCESS && rc != CRfCommPort::NOT_OPENED) {
		throwIOException(env, "Failed to Close");
	}
	// Some worker thread is still trying to access this object, delete later
	if (stack != NULL) {
		stack->deleteCommPort(rf);
	}
	//debugs("connection handles %i", openConnections);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getConnectionRfRemoteAddress
(JNIEnv *env, jobject peer, jlong handle) {
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL) {
		return 0;
	}
	if (!rf->isConnected || rf->isClosing) {
		throwIOException(env, "Connection is closed");
		return 0;
	}
	BD_ADDR connected_bd_addr;
    if (rf->IsConnected(&connected_bd_addr)) {
		return BcAddrToLong(connected_bd_addr);
	} else {
		throwIOException(env, "Connection down");
		return 0;
	}
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfRead__J
(JNIEnv *env, jobject peer, jlong handle) {
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL) {
		return -1;
	}
	debug("->read()");
	if (rf->isClosing) {
		return -1;
	}
	while (rf->isConnected && (!rf->isClosing) && (rf->todo_buf_read_idx == rf->todo_buf_rcv_idx)) {
		if (TODO_BUF_MAX == rf->todo_buf_rcv_idx) {
			throwIOException(env, "rcv buffer overflown, Fix me");
			return 0;
		}
		DWORD  rc = WaitForMultipleObjects(2, rf->hEvents, FALSE, INFINITE);
		if (rc == WAIT_FAILED) {
			throwRuntimeException(env, "WaitForMultipleObjects");
			return 0;
		}
	}
	if ((rf->isClosing) || (!rf->isConnected && (rf->todo_buf_read_idx == rf->todo_buf_rcv_idx))) {
		// See InputStream.read();
		return -1;
	}
	jint result = (unsigned char)rf->todo_buf[rf->todo_buf_read_idx];
	rf->todo_buf_read_idx ++;
	return result;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfRead__J_3BII
(JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL) {
		return -1;
	}
	debug("->read(byte[])");
	if (rf->isClosing) {
		return -1;
	}
	jbyte *bytes = env->GetByteArrayElements(b, 0);

	int done = 0;

	while (rf->isConnected && (!rf->isClosing) && (done < len)) {
		while (rf->isConnected && rf->todo_buf_read_idx == rf->todo_buf_rcv_idx) {
			if (TODO_BUF_MAX == rf->todo_buf_rcv_idx) {
				throwIOException(env, "rcv buffer overflown, Fix me");
				return 0;
			}
			DWORD  rc = WaitForMultipleObjects(2, rf->hEvents, FALSE, INFINITE);
			if (rc == WAIT_FAILED) {
				throwRuntimeException(env, "WaitForMultipleObjects");
				return 0;
			}
		}
		int count = rf->todo_buf_rcv_idx - rf->todo_buf_read_idx;
		if (count > len - done) {
			count = len - done;
		}

		memcpy(bytes + off + done , (rf->todo_buf + rf->todo_buf_read_idx), count);
		rf->todo_buf_read_idx += count;

		done += count;
	}
	if ((rf->isClosing) || (!rf->isConnected && done == 0)) {
		// See InputStream.read();
		done = -1;
	}
	env->ReleaseByteArrayElements(b, bytes, 0);
	return done;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfReadAvailable
(JNIEnv *env, jobject peer, jlong handle) {
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL || rf->isClosing) {
		return 0;
	}
	return (rf->todo_buf_rcv_idx - rf->todo_buf_read_idx);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfWrite__JI
(JNIEnv *env, jobject peer, jlong handle, jint b) {
	debug("->write(int)");
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL) {
		return;
	}
	if (!rf->isConnected || rf->isClosing) {
		throwIOException(env, "Failed to write to closed connection");
		return;
	}
	char c = (char)b;
	UINT16 written = 0;
	while ((written == 0) && rf->isConnected) {
		CRfCommPort::PORT_RETURN_CODE rc = rf->Write((void*)(&c), 1, &written);
		if (rc != CRfCommPort::SUCCESS) {
			throwIOException(env, "Failed to write");
			return;
		}
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_connectionRfWrite__J_3BII
(JNIEnv *env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {
	debug("->write(byte[])");
	WIDCOMMStackRfCommPort* rf = validRfCommHandle(env, handle);
	if (rf == NULL) {
		return;
	}
	if (!rf->isConnected || rf->isClosing) {
		throwIOException(env, "Failed to write to closed connection");
		return;
	}

	jbyte *bytes = env->GetByteArrayElements(b, 0);

	UINT16 done = 0;

	while ((done < len) && rf->isConnected && (!rf->isClosing)) {
		UINT16 written = 0;
		CRfCommPort::PORT_RETURN_CODE rc = rf->Write((void*)(bytes + off + done), (UINT16)(len - done), &written);
		if (rc != CRfCommPort::SUCCESS) {
			env->ReleaseByteArrayElements(b, bytes, 0);
			throwIOException(env, "Failed to write");
			return;
		}
		done += written;
	}

	env->ReleaseByteArrayElements(b, bytes, 0);
}

#endif
