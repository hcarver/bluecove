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
#include <pthread.h>

#define CPP_FILE "OSXStack.cpp"

OSXStack* stack = NULL;

OSXStack::OSXStack() {
    deviceInquiryInProcess = FALSE;
    deviceInquiryTerminated = FALSE;
}

OSXStack::~OSXStack() {
}

Runnable::Runnable() {
    name = "n/a";
    sData[0] = '\0';
    error = 0;
    lData = 0;
}

// --- One Native Thread and RunLoop, An issue with the OS X BT implementation is all the calls need to come from the same thread.

CFRunLoopRef			mainRunLoop;
CFRunLoopSourceRef		btOperationSource;

typedef struct BTOperationParams {
	pthread_cond_t callComplete;
	Runnable* runnable;
};

void *oneNativeThreadMain(void *initializeCond);

void performBTOperationCallBack(void *info);
pthread_mutex_t	btOperationInProgress;

JavaVM *s_vm;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {

    pthread_t thread;
	pthread_mutex_t initializeMutex;
	pthread_cond_t  initializeCond;

	pthread_cond_init(&initializeCond, NULL);
	pthread_mutex_init(&initializeMutex, NULL);

	pthread_mutex_lock(&initializeMutex);
	s_vm = vm;
	// Starting the OS X init and run thread
	pthread_create(&thread, NULL, oneNativeThreadMain, (void*) &initializeCond);
	// wait until the OS X thread has initialized before returning
	pthread_cond_wait(&initializeCond, &initializeMutex);

	// clean up
	pthread_cond_destroy(&initializeCond);
	pthread_mutex_unlock(&initializeMutex);
	pthread_mutex_destroy(&initializeMutex);

    return JNI_VERSION_1_2;
}

void *oneNativeThreadMain(void *initializeCond) {

    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_2;
	args.name = "OS X Bluetooth CFRunLoop";
	args.group = NULL;
	JNIEnv	*env;
	s_vm->AttachCurrentThreadAsDaemon((void**)&env, &args);

    // setup OS X managed memory environment
    NSAutoreleasePool *autoreleasepool = [[NSAutoreleasePool alloc] init];

    mainRunLoop = CFRunLoopGetCurrent();

    pthread_mutex_init(&btOperationInProgress, NULL);

    // create event sources, i.e. requests from the java VM
    CFRunLoopSourceContext context = {0};
    BTOperationParams params = {0};
    // An arbitrary pointer to program-defined data, which can be associated with the CFRunLoopSource at creation time. This pointer is passed to callbacks.
    context.info = &params;
	context.perform = performBTOperationCallBack;
	btOperationSource = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &context);
	CFRunLoopAddSource(mainRunLoop, btOperationSource, kCFRunLoopDefaultMode);

    // Init complete, releasing the library load thread
    pthread_cond_signal((pthread_cond_t*)initializeCond);

    ndebug("Starting the CFRunLoop");
	// Starting the CFRunLoop
	CFRunLoopRun();
	// should only reach this point when getting unloaded
	pthread_mutex_destroy(&btOperationInProgress);
	[autoreleasepool release];
	return NULL;
}

void performBTOperationCallBack(void *info) {
    BTOperationParams* params = (BTOperationParams*)info;
    if (params->runnable != NULL) {
        ndebug("execute BTOperation %s", params->runnable->name);
        params->runnable->run();
    }
    pthread_cond_signal(&(params->callComplete));
}

void synchronousBTOperation(Runnable* runnable) {

	CFRunLoopSourceContext	context={0};

	CFRunLoopSourceGetContext(btOperationSource, &context);
	BTOperationParams* params = (BTOperationParams*)context.info;
	params->runnable = runnable;

    pthread_cond_init(&(params->callComplete), NULL);
	pthread_mutex_lock(&btOperationInProgress);

    ndebug("invoke BTOperation %s", params->runnable->name);
	CFRunLoopSourceSignal(btOperationSource);
	CFRunLoopWakeUp(mainRunLoop);

	pthread_cond_wait(&(params->callComplete), &btOperationInProgress);
	pthread_mutex_unlock(&btOperationInProgress);
	pthread_cond_destroy(&params->callComplete);
}

void ndebug(const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	fprintf(stderr, "NATIVE:");
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);
	fflush(stderr);
}

// --- Helper functions

OSXJNIHelper::OSXJNIHelper() {
    autoreleasepool = [[NSAutoreleasePool alloc] init];
}

OSXJNIHelper::~OSXJNIHelper() {
    [autoreleasepool release];
}

jstring OSxNewJString(JNIEnv *env, NSString *nString) {
    jsize buflength = [nString length];
    unichar buffer[buflength];
    [nString getCharacters:buffer];
    return env->NewString((jchar *)buffer, buflength);
}

void OSxAddrToString(char* addressString, const BluetoothDeviceAddress* addr) {
	snprintf(addressString, 14, "%02x%02x%02x%02x%02x%02x",
			 addr->data[0],
             addr->data[1],
             addr->data[2],
             addr->data[3],
             addr->data[4],
             addr->data[5]);
}

jlong OSxAddrToLong(const BluetoothDeviceAddress* addr) {
	jlong l = 0;
	for (int i = 0; i < 6; i++) {
		l = (l << 8) + addr->data[i];
	}
	return l;
}

void LongToOSxBTAddr(jlong longAddr, BluetoothDeviceAddress* addr) {
	for (int i = 6 - 1; i >= 0; i--) {
		addr->data[i] = (UInt8)(longAddr & 0xFF);
		longAddr >>= 8;
	}
}

// --- JNI function

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
    stack = new OSXStack();
	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_destroyImpl
(JNIEnv *env, jobject) {
    if (stack != NULL) {
		OSXStack* stackTmp = stack;
		stack = NULL;
		delete stackTmp;
	}
}

// --- LocalDevice

RUNNABLE(GetLocalDeviceBluetoothAddress, "GetLocalDeviceBluetoothAddress") {
    if (!IOBluetoothLocalDeviceAvailable()) {
		error = 1;
		return;
    }
    BluetoothDeviceAddress localAddress;
    if (IOBluetoothLocalDeviceReadAddress(&localAddress, NULL, NULL, NULL)) {
        error = 2;
		return;
    }
    OSxAddrToString(sData, &localAddress);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject) {
    Edebug("getLocalDeviceBluetoothAddress");
    GetLocalDeviceBluetoothAddress runnable;
    synchronousBTOperation(&runnable);
    switch (runnable.error) {
        case 1:
            throwBluetoothStateException(env, "Bluetooth Device is not available");
		    return NULL;
        case 2:
            throwBluetoothStateException(env, "Bluetooth Device is not ready");
	        return NULL;
    }
    return env->NewStringUTF(runnable.sData);
}

RUNNABLE(GetLocalDeviceName, "GetLocalDeviceName") {
    BluetoothDeviceName localName;
    if (IOBluetoothLocalDeviceReadName(localName, NULL, NULL, NULL)) {
		error = 0;
    } else {
        strncpy(sData, (char*)localName, RUNNABLE_DATA_MAX);
    }
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceName
(JNIEnv *env, jobject) {
    Edebug("getLocalDeviceName");
    GetLocalDeviceName runnable;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return NULL;
    }
    return env->NewStringUTF(runnable.sData);
}

RUNNABLE(GetDeviceClass, "GetDeviceClass") {
    BluetoothClassOfDevice cod;
    if (!IOBluetoothLocalDeviceReadClassOfDevice(&cod, NULL, NULL, NULL)) {
        lData = cod;
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getDeviceClassImpl
(JNIEnv *env, jobject) {
    Edebug("getDeviceClassImpl");
    GetDeviceClass runnable;
    synchronousBTOperation(&runnable);
    return (jint)runnable.lData;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDevicePowerOn
(JNIEnv *env, jobject) {
    Edebug("isLocalDevicePowerOn");
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
    Edebug("getLocalDeviceDiscoverableImpl");
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
    Edebug("isLocalDeviceFeatureSwitchRoles");
    BluetoothHCISupportedFeatures features;
    //if (IOBluetoothLocalDeviceReadSupportedFeatures(&features, NULL, NULL, NULL)) {
    //    return JNI_FALSE;
    //}
    return (kBluetoothFeatureSwitchRoles & features.data[7])?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDeviceFeatureParkMode
(JNIEnv *env, jobject) {
    Edebug("isLocalDeviceFeatureParkMode");
    BluetoothHCISupportedFeatures features;
    //if (IOBluetoothLocalDeviceReadSupportedFeatures(&features, NULL, NULL, NULL)) {
    //    return JNI_FALSE;
    //}
    return (kBluetoothFeatureParkMode & features.data[6])?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceL2CAPMTUMaximum
(JNIEnv *env, jobject) {
    return (jint)kBluetoothL2CAPMTUMaximum;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceSoftwareVersionInfo
(JNIEnv *env, jobject) {
    Edebug("getLocalDeviceSoftwareVersionInfo");
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
    Edebug("getLocalDeviceManufacturer");
    BluetoothHCIVersionInfo	hciVersion;
	if (IOBluetoothGetVersion(NULL, &hciVersion )) {
	    return 0;
	}
	return hciVersion.manufacturerName;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceVersion
(JNIEnv *env, jobject) {
    Edebug("getLocalDeviceVersion");
    BluetoothHCIVersionInfo	hciVersion;
	if (IOBluetoothGetVersion(NULL, &hciVersion )) {
	    return 0;
	}
    char swVers[133];
    snprintf(swVers, 133, "LMP Version: %d.%d, HCI Version: %d.%d", hciVersion.lmpVersion, hciVersion.lmpSubVersion,
                          hciVersion.hciVersion, hciVersion.hciRevision);
    return env->NewStringUTF(swVers);
}