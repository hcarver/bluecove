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

#include "stdafx.h"

#ifdef _BTWLIB

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

struct deviceFound {
	jlong deviceAddr;
	jint deviceClass;
	BD_NAME bdName;
};

#define deviceRespondedMax 20

class WIDCOMMStack : public CBtIf {
public:
	deviceFound deviceResponded[deviceRespondedMax];
	int deviceRespondedIdx;
	BOOL deviceInquiryTerminated;
	BOOL deviceInquiryComplete;
	BOOL deviceInquirySuccess;

	BOOL searchServicesComplete;

	WIDCOMMStack();

    // methods to replace virtual methods in base class CBtIf
    virtual void OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected);
    virtual void OnInquiryComplete(BOOL success, short num_responses);

	virtual void OnDiscoveryComplete();
};

static WIDCOMMStack* stack;

WIDCOMMStack::WIDCOMMStack() {
}

void WIDCOMMStackInit() {
	if (stack == NULL) {
		stack = new WIDCOMMStack();
	}
}

// TODO
void BroadcomDebugError(CBtIf* stack) {
}


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject peer) {
	WIDCOMMStackInit();
	struct CBtIf::DEV_VER_INFO info;
	if (!stack->GetLocalDeviceVersionInfo(&info)) {
		BroadcomDebugError(stack);
	}
	wchar_t addressString[14];
	BcAddrToString(addressString, info.bd_addr);
	return env->NewString((jchar*)addressString, (jsize)wcslen(addressString));
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceName
(JNIEnv *env, jobject peer) {
	WIDCOMMStackInit();
	BD_NAME name;
	if (!stack->GetLocalDeviceName(&name)) {
		BroadcomDebugError(stack);
		return NULL;
	}
	return env->NewStringUTF((char*)name);
}

// --- Device Inquiry

void WIDCOMMStack::OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected) {
	int nextDevice = deviceRespondedIdx + 1;
	if (nextDevice >= deviceRespondedMax) {
		nextDevice = 0;
	}
	deviceResponded[nextDevice].deviceAddr = BcAddrToLong(bda);
    deviceResponded[nextDevice].deviceClass = DeviceClassToInt(devClass);
	memcpy(deviceResponded[nextDevice].bdName, bdName, sizeof(BD_NAME));

	deviceRespondedIdx = nextDevice;
}

void WIDCOMMStack::OnInquiryComplete(BOOL success, short num_responses) {
	deviceInquirySuccess = success;
	deviceInquiryComplete = TRUE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {
	WIDCOMMStackInit();
	debug("StartDeviceInquiry");
	stack->deviceInquiryComplete = false;
	stack->deviceInquiryTerminated = false;

    memset(stack->deviceResponded, 0, sizeof(stack->deviceResponded));
 	stack->deviceRespondedIdx = -1;

	jclass peerClass = env->GetObjectClass(peer);
	if (peerClass == NULL) {
		//fatalerror
	}

	jmethodID deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;)V");
	if (deviceDiscoveredCallbackMethod == NULL) {
		//fatalerror
		debug("fatalerror");
		return INQUIRY_ERROR;
	}

	if (!stack->StartInquiry()) {
		debug("deviceInquiryStart error");
		// TODO read error
		throwException(env, "javax/bluetooth/BluetoothStateException", "todo");
		return INQUIRY_ERROR;
	}
	debug("deviceInquiryStarted");

	jclass notifyClass = env->GetObjectClass(startedNotify);
	if (notifyClass == NULL) {
		//fatalerror
	}
	jmethodID notifyMethod = env->GetMethodID(notifyClass, "deviceInquiryStartedCallback", "()V");
	if (notifyMethod == NULL) {
		//fatalerror
	}
	env->CallVoidMethod(startedNotify, notifyMethod);

	int reportedIdx = -1;

	while ((!stack->deviceInquiryComplete) || (reportedIdx != stack->deviceRespondedIdx)) {
		// No Wait on Windows CE, TODO
		Sleep(100);
		if (reportedIdx != stack->deviceRespondedIdx) {
			reportedIdx ++;
			if (reportedIdx >= deviceRespondedMax) {
				reportedIdx = 0;
			}
			deviceFound dev = stack->deviceResponded[reportedIdx];
			env->CallVoidMethod(peer, deviceDiscoveredCallbackMethod, listener, dev.deviceAddr, dev.deviceClass, env->NewStringUTF((char*)(dev.bdName)));
		}
	}

	if (stack->deviceInquiryTerminated) {
		return INQUIRY_TERMINATED;
	} else if (stack->deviceInquirySuccess) {
		return INQUIRY_COMPLETED;
	} else {
		return INQUIRY_ERROR;
	}
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer, jobject nativeClass) {
	WIDCOMMStackInit();
	stack->deviceInquiryTerminated = TRUE;
	stack->StopInquiry();
	return TRUE;
}

// --- Service search

JNIEXPORT jlongArray JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runSearchServicesImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jobject uuid, jlong address) {
	WIDCOMMStackInit();
	debug("StartSearchServices");

	BD_ADDR bda; 
	LongToBcAddr(address, bda);
	wchar_t addressString[14];
	BcAddrToString(addressString, bda);
	debugs("StartSearchServices on [%S]", addressString);

	GUID service_guid;

	if (uuid != NULL) {
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
	}

	stack->searchServicesComplete = FALSE;

	if (!stack->StartDiscovery(bda, &service_guid)) {
		debug("StartSearchServices error");
		// TODO read error
		throwException(env, "javax/bluetooth/BluetoothStateException", "todo");
		return NULL;
	}

	jclass notifyClass = env->GetObjectClass(startedNotify);
	if (notifyClass == NULL) {
		//fatalerror
	}
	jmethodID notifyMethod = env->GetMethodID(notifyClass, "searchServicesStartedCallback", "()V");
	if (notifyMethod == NULL) {
		//fatalerror
	}
	env->CallVoidMethod(startedNotify, notifyMethod);

	while (!stack->searchServicesComplete) {
		// No Wait on Windows CE, TODO
		Sleep(100);
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


	CSdpDiscoveryRec* records = new CSdpDiscoveryRec[obtainedServicesRecords];

	int recSize = stack->ReadDiscoveryRecords(bda, obtainedServicesRecords, records, NULL);

	jlongArray result = env->NewLongArray(recSize);

	jlong *longs = env->GetLongArrayElements(result, 0);

	for (int r = 0; r < recSize; r ++) {
		longs[r] = (jlong)&(records[r]);
	}
	env->ReleaseLongArrayElements(result, longs, 0);

	return result;
}

void WIDCOMMStack::OnDiscoveryComplete() {
	searchServicesComplete = TRUE;
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getServiceAttributes
(JNIEnv * env, jobject peer, jint attrID, jlong handle) {

	CSdpDiscoveryRec* record = (CSdpDiscoveryRec*)handle;

	SDP_DISC_ATTTR_VAL* p_val = new SDP_DISC_ATTTR_VAL();

	if (!record->FindAttribute((UINT16)attrID, p_val)) {
		// attr not found
		return NULL;
	}
	//debugs("num_elem %i", p_val->num_elem);
	//debugs("first type %i", p_val->elem[0].type);
	//debugs("first len %i", p_val->elem[0].len);

	jbyteArray result = env->NewByteArray(sizeof(SDP_DISC_ATTTR_VAL));
	jbyte *bytes = env->GetByteArrayElements(result, 0);
	memcpy(bytes, p_val, sizeof(SDP_DISC_ATTTR_VAL));
	env->ReleaseByteArrayElements(result, bytes, 0);
	return result;
}

#endif
