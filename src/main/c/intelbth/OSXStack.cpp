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

#include "OSXStack.h"

OSXStack* stack = NULL;


OSXStack::OSXStack() {
}

OSXStack::~OSXStack() {
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLibraryVersion
(JNIEnv *, jobject) {
	return blueCoveVersion();
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_detectBluetoothStack
(JNIEnv *env, jobject) {
	return BLUECOVE_STACK_DETECT_OSX;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_enableNativeDebug
(JNIEnv *env, jobject, jclass loggerClass, jboolean on) {
	enableNativeDebug(env, loggerClass, on);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_initializeImpl
(JNIEnv *env, jobject) {
	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_destroyImpl
(JNIEnv *, jobject) {
}

// --- LocalDevice

void OSxAddrToString(char* addressString, BluetoothDeviceAddress* addr) {
	snprintf(addressString, 14, "%02x%02x%02x%02x%02x%02x",
			 addr->data[0],
             addr->data[1],
             addr->data[2],
             addr->data[3],
             addr->data[4],
             addr->data[5]);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject) {
    if (!IOBluetoothLocalDeviceAvailable()) {
        throwBluetoothStateException(env, "Bluetooth Device is not available");
		return NULL;
    }
    BluetoothDeviceAddress localAddress;
    if (IOBluetoothLocalDeviceReadAddress(&localAddress, NULL, NULL, NULL)) {
        throwBluetoothStateException(env, "Bluetooth Device is not ready");
		return NULL;
    }
    char addressString[14];
    OSxAddrToString(addressString, &localAddress);
    return env->NewStringUTF(addressString);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceName
(JNIEnv *env, jobject) {
    BluetoothDeviceName localName;
    if (IOBluetoothLocalDeviceReadName(localName, NULL, NULL, NULL)) {
		return NULL;
    }
    return env->NewStringUTF((char*)localName);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getDeviceClassImpl
(JNIEnv *env, jobject) {
    BluetoothClassOfDevice cod;
    if (IOBluetoothLocalDeviceReadClassOfDevice(&cod, NULL, NULL, NULL)) {
        return 0;
    }
    return (jint)cod;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDevicePowerOn
(JNIEnv *env, jobject) {
    if (!IOBluetoothLocalDeviceAvailable()) {
        return JNI_FALSE;
    }
    BluetoothHCIPowerState powerState;
    if (IOBluetoothLocalDeviceGetPowerState(&powerState)) {
        return JNI_FALSE;
    }
    return (powerState == kBluetoothHCIPowerStateON)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceDiscoverableImpl
(JNIEnv *env, jobject) {
    if (!IOBluetoothLocalDeviceAvailable()) {
        return JNI_FALSE;
    }
    Boolean discoverableStatus;
    if (IOBluetoothLocalDeviceGetDiscoverable(&discoverableStatus)) {
        return JNI_FALSE;
    }
    return (discoverableStatus)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDeviceFeatureSwitchRoles
(JNIEnv *env, jobject) {
    BluetoothHCISupportedFeatures features;
    if (IOBluetoothLocalDeviceReadSupportedFeatures(&features, NULL, NULL, NULL)) {
        return JNI_FALSE;
    }
    return (kBluetoothFeatureSwitchRoles & features.data[7])?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDeviceFeatureParkMode
(JNIEnv *env, jobject) {
    BluetoothHCISupportedFeatures features;
    if (IOBluetoothLocalDeviceReadSupportedFeatures(&features, NULL, NULL, NULL)) {
        return JNI_FALSE;
    }
    return (kBluetoothFeatureParkMode & features.data[6])?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceL2CAPMTUMaximum
(JNIEnv *env, jobject) {
    return (jint)kBluetoothL2CAPMTUMaximum;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceSoftwareVersionInfo
(JNIEnv *env, jobject) {
    NumVersion btVersion;
	char swVers[133];

	if (IOBluetoothGetVersion( &btVersion, NULL )) {
	    return NULL;
	}
	snprintf(swVers, 133, "%1d%1d.%1d.%1d rev %d", btVersion.majorRev >> 4, btVersion.majorRev & 0x0F,
	                      btVersion.minorAndBugRev >> 4, btVersion.minorAndBugRev & 0x0F, btVersion.nonRelRev);
    return env->NewStringUTF(swVers);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceManufacturer
(JNIEnv *env, jobject) {
    BluetoothHCIVersionInfo	hciVersion;
	if (IOBluetoothGetVersion(NULL, &hciVersion )) {
	    return 0;
	}
	return hciVersion.manufacturerName;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceVersion
(JNIEnv *env, jobject) {
    BluetoothHCIVersionInfo	hciVersion;
	if (IOBluetoothGetVersion(NULL, &hciVersion )) {
	    return 0;
	}
    char swVers[133];
    snprintf(swVers, 133, "LMP Version: %d.%d, HCI Version: %d.%d", hciVersion.lmpVersion, hciVersion.lmpSubVersion,
                          hciVersion.hciVersion, hciVersion.hciRevision);
    return env->NewStringUTF(swVers);
}