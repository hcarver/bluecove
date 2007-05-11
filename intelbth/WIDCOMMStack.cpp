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
		l = (l << 4) + bd_addr[i];
	}
	return l;
}

jint DeviceClassToInt(DEV_CLASS devClass) {
	return (devClass[1] & MAJOR_DEV_CLASS_MASK) + ((devClass[2] & MINOR_DEV_CLASS_MASK) << 6) + (devClass[0] << 13);
}

class WIDCOMMStack : public CBtIf {
public:
	JNIEnv *env;
	jobject peer;
	BOOL deviceInquiryComplete;
	BOOL deviceInquirySuccess;
	jobject deviceDiscoveredListener;
	jmethodID deviceDiscoveredCallbackMethod;

	WIDCOMMStack(JNIEnv *env, jobject peer);

    // methods to replace virtual methods in base class CBtIf
    virtual void OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected);
    virtual void OnInquiryComplete(BOOL success, short num_responses);
};

static WIDCOMMStack* stack;

WIDCOMMStack::WIDCOMMStack(JNIEnv *env, jobject peer) {
	this->env = env;
	this->peer = env->NewGlobalRef(peer);
}

void WIDCOMMStackInit(JNIEnv *env, jobject peer) {
	if (stack == NULL) {
		debug("WIDCOMMStackInit");
		stack = new WIDCOMMStack(env, peer);
	} else {
		stack->env = env;
	}
}

// TODO
void BroadcomDebugError(CBtIf* stack) {
}


JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject peer) {
	WIDCOMMStackInit(env, peer);
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
	WIDCOMMStackInit(env, peer);
	BD_NAME name;
	if (!stack->GetLocalDeviceName(&name)) {
		BroadcomDebugError(stack);
		return NULL;
	}
	return env->NewStringUTF((char*)name);
}


void WIDCOMMStack::OnDeviceResponded(BD_ADDR bda, DEV_CLASS devClass, BD_NAME bdName, BOOL bConnected) {
	//debugs("->OnDeviceResponded [%s]", bdName);
	//debug("->OnDeviceResponded");
	if (peer == NULL) {
		return;
	}
	if (deviceDiscoveredCallbackMethod != NULL) {
		//wchar_t addressString[14];
		//BcAddrToString(addressString, bda);
		//debugs("OnDeviceResponded %S", addressString);
		//debug("call deviceDiscoveredCallback 1");
		jlong deviceAddr = BcAddrToLong(bda);
		//debug("call deviceDiscoveredCallback 2");
		jint deviceClass = DeviceClassToInt(devClass);
		//debug("call deviceDiscoveredCallback 3");
		//env->CallVoidMethod(peer, deviceDiscoveredCallbackMethod, deviceDiscoveredListener, deviceAddr, deviceClass, env->NewStringUTF((char*)bdName));
	}
}

void WIDCOMMStack::OnInquiryComplete(BOOL success, short num_responses) {
	//debug("->OnInquiryComplete");
	deviceInquirySuccess = success;
	deviceInquiryComplete = TRUE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_runDeviceInquiryImpl
(JNIEnv * env, jobject peer, jobject startedNotify, jint accessCode, jobject listener) {
	WIDCOMMStackInit(env, peer);
	debug("StartInquiry");
	stack->deviceInquiryComplete = false;

	jclass peerClass = env->GetObjectClass(stack->peer);
	if (peerClass == NULL) {
		//fatalerror
	}
	
	stack->deviceDiscoveredListener = listener;
	stack->deviceDiscoveredCallbackMethod = env->GetMethodID(peerClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;)V");
	if (stack->deviceDiscoveredCallbackMethod == NULL) {
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


	while (!stack->deviceInquiryComplete) {
		Sleep(100);
	}

	return stack->deviceInquirySuccess;

}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer, jobject nativeClass) {
	WIDCOMMStackInit(env, peer);
	stack->StopInquiry();
	return TRUE;
}

#endif
