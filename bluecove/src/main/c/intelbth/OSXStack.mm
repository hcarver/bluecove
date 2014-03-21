/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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

#include "OSXStack.h"
#include <pthread.h>
#include <dispatch/dispatch.h>

#import <IOBluetooth/IOBluetooth.h>


#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER
#import <IOBluetooth/objc/IOBluetoothHostController.h>
#endif

#define CPP_FILE "OSXStack.mm"

OSXStack* stack = NULL;
jint localDeviceSupportedSoftwareVersion = 0;

OSXStack::OSXStack() {
    deviceInquiryInProcess = FALSE;
    deviceInquiryBusy = FALSE;
    deviceInquiryTerminated = FALSE;
    
    pthread_mutexattr_t   mta;
    // Create a default mutex attribute
    pthread_mutexattr_init(&mta);
    // If a thread attempts to relock a mutex that it has already locked, an error is returned.
    pthread_mutexattr_settype(&mta, PTHREAD_MUTEX_ERRORCHECK);
    pthread_mutex_init(&deviceInquiryInProcessMutex, &mta);
    
    deviceInquiryNotificationEvent = dispatch_semaphore_create(0);
    deviceInquiryFinishedEvent = dispatch_semaphore_create(0);
    deviceInquiryBusyEvent = dispatch_semaphore_create(0);
    
    //    dispatch_semaphore_wait(retrieveConnectedPeripheralsSema, DISPATCH_TIME_FOREVER);
    //    dispatch_release(retrieveConnectedPeripheralsSema);
    
    commPool = new ObjectPool(100, 1, TRUE);
}

OSXStack::~OSXStack() {
    
    if (commPool != NULL) {
        delete commPool;
        commPool = NULL;
    }
    
    dispatch_semaphore_signal(deviceInquiryNotificationEvent);
    
    dispatch_release(deviceInquiryNotificationEvent);
    dispatch_release(deviceInquiryFinishedEvent);
    dispatch_release(deviceInquiryBusyEvent);
    
    pthread_mutex_destroy(&deviceInquiryInProcessMutex);
}

BOOL OSXStack::deviceInquiryLock(JNIEnv* env) {
    if (deviceInquiryInProcess && deviceInquiryTerminated) {
        // Wait until it terminates
        dispatch_semaphore_wait(deviceInquiryFinishedEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 3));
    }
    if (deviceInquiryInProcess) {
        throwBluetoothStateException(env, cINQUIRY_RUNNING);
        return false;
    }
    if (pthread_mutex_trylock(&deviceInquiryInProcessMutex) != 0) {
        throwBluetoothStateException(env, cINQUIRY_RUNNING);
        return false;
    }
    deviceInquiryInProcess = true;
    deviceInquiryBusy = true;
    return true;
}

BOOL OSXStack::deviceInquiryUnlock() {
    deviceInquiryInProcess = false;
    BOOL rc = (pthread_mutex_unlock(&deviceInquiryInProcessMutex) == 0);
    dispatch_semaphore_signal(deviceInquiryFinishedEvent);
    return rc;
}

Runnable::Runnable() {
    magic1b = MAGIC_1;
    magic2b = MAGIC_2;
    magic1e = MAGIC_1;
    magic2e = MAGIC_2;
    
    name = "n/a";
    sData[0] = '\0';
    error = 0;
    lData = 0;
    bData = false;
    for (int i = 0; i < RUNNABLE_DATA_MAX; i++) {
        pData[i] = NULL;
    }
}

Runnable::~Runnable() {
    magic1b = 0;
    magic2b = 0;
    magic1e = 0;
    magic2e = 0;
}

BOOL isRunnableCorrupted(Runnable* r) {
    return ((r == NULL) || (r->magic1b != MAGIC_1) || (r->magic2b != MAGIC_2) || (r->magic1e != MAGIC_1) || (r->magic2e != MAGIC_2));
}

// --- One Native Thread and RunLoop, An issue with the OS X BT implementation is all the calls need to come from the same thread.

#ifdef USE_WORKER_THREAD
CFRunLoopRef            mainRunLoop = NULL;
CFRunLoopSourceRef      btOperationSource;

typedef struct BTOperationParams {
    Runnable* runnable;
} BTOperationParams_t;

void *oneNativeThreadMain(void *pThreadParams);

void performBTOperationCallBack(void *info);
pthread_mutex_t btOperationInProgress;
dispatch_semaphore_t synchronousBTOperationCallComplete;

typedef struct NativeThreadParams {
    JavaVM *vm;
    dispatch_semaphore_t initializedNotificationEvent;
} NativeThreadParams_t;

#else

dispatch_queue_t        mainQueue = NULL;

#endif


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    
#ifdef USE_WORKER_THREAD
    
    pthread_t thread;
    NativeThreadParams threadParams;
    threadParams.vm = vm;
    threadParams.initializedNotificationEvent = dispatch_semaphore_create(0);
    
    // Starting the OS X init and run thread
    pthread_create(&thread, NULL, oneNativeThreadMain, (void*) &threadParams);
    // Test for race condition
    //sleep(3);
    
    // wait until the OS X thread has initialized before returning
    OSStatus status = dispatch_semaphore_wait(threadParams.initializedNotificationEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 10));
    
    // clean up
    dispatch_release(threadParams.initializedNotificationEvent);
    
    if (status == kMPTimeoutErr) {
        return -1;
    }
#else
    mainQueue = dispatch_queue_create(NULL, DISPATCH_QUEUE_SERIAL);
#endif
    return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    
#ifdef USE_WORKER_THREAD
    
    if (mainRunLoop != NULL) {
        CFRunLoopStop(mainRunLoop);
        mainRunLoop = NULL;
    }
    
#else
    dispatch_release(mainQueue);
#endif
    
}

#ifdef USE_WORKER_THREAD

void *oneNativeThreadMain(void * pThreadParams) {
    
    NativeThreadParams* threadParams = (NativeThreadParams*)pThreadParams;
    
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_2;
    args.name = (char *)"OS X Bluetooth CFRunLoop";
    args.group = NULL;
    JNIEnv  *env;
    threadParams->vm->AttachCurrentThreadAsDaemon((void**)&env, &args);
    
    // setup OS X managed memory environment
    NSAutoreleasePool *autoreleasepool = [[NSAutoreleasePool alloc] init];
    
    mainRunLoop = CFRunLoopGetCurrent();
    
    pthread_mutex_init(&btOperationInProgress, NULL);
    synchronousBTOperationCallComplete = dispatch_semaphore_create(0);
    
    // create event sources, i.e. requests from the java VM
    CFRunLoopSourceContext context = {0};
    BTOperationParams operationParams = {0};
    // An arbitrary pointer to program-defined data, which can be associated with the CFRunLoopSource at creation time. This pointer is passed to callbacks.
    context.info = &operationParams;
    context.perform = performBTOperationCallBack;
    btOperationSource = CFRunLoopSourceCreate(kCFAllocatorDefault, 0, &context);
    CFRunLoopAddSource(mainRunLoop, btOperationSource, kCFRunLoopDefaultMode);
    
    // Init complete, releasing the library load thread
    dispatch_semaphore_signal(threadParams->initializedNotificationEvent); // , 1);
    
    ndebug(("Starting the CFRunLoop"));
    // Starting the CFRunLoop
    CFRunLoopRun();
    // should only reach this point when getting unloaded
    pthread_mutex_destroy(&btOperationInProgress);
    dispatch_semaphore_signal(synchronousBTOperationCallComplete); // , 0);
    dispatch_release(synchronousBTOperationCallComplete);
    
    [autoreleasepool release];
    return NULL;
}


void performBTOperationCallBack(void *info) {
    BTOperationParams* params = (BTOperationParams*)info;
    if (params->runnable != NULL) {
        if (isRunnableCorrupted(params->runnable)) {
            ndebug(("Error: execute BTOperation got corrupted runnable"));
        } else {
            ndebug((" execute  BTOperation %s", params->runnable->name));
            params->runnable->run();
            ndebug((" finished BTOperation %s", params->runnable->name));
        }
    }
    dispatch_semaphore_signal(synchronousBTOperationCallComplete); // , 1);
}

void synchronousBTOperation(Runnable* runnable) {
    
    pthread_mutex_lock(&btOperationInProgress);
    
    CFRunLoopSourceContext  context={0};
    CFRunLoopSourceGetContext(btOperationSource, &context);
    BTOperationParams* params = (BTOperationParams*)context.info;
    params->runnable = runnable;
    
    ndebug(("invoke    BTOperation %s", params->runnable->name));
    CFRunLoopSourceSignal(btOperationSource);
    CFRunLoopWakeUp(mainRunLoop);
    
    dispatch_semaphore_wait(synchronousBTOperationCallComplete, kDurationForever);
    
    pthread_mutex_unlock(&btOperationInProgress);
    ndebug(("return    BTOperation %s", params->runnable->name));
}

#else

void synchronousBTOperation(Runnable* runnable) {
    if (runnable != NULL) {
        ndebug(("invoke    BTOperation %s", runnable->name));
        
        dispatch_block_t block = ^
        {
            if (isRunnableCorrupted(runnable)) {
                ndebug(("Error: execute BTOperation got corrupted runnable"));
            }
            else {
                ndebug((" execute  BTOperation %s", runnable->name));
                runnable->run();
                ndebug((" finished BTOperation %s", runnable->name));
            }
        };
        
        // Prevent deadlocking the main thread with dispatch_sync
        // by checking if we are on the main thread first.
        if ([NSThread isMainThread])
        {
            block();
        }
        else
        {
            dispatch_sync(dispatch_get_main_queue(), block);
        }        

        ndebug(("return    BTOperation %s", runnable->name));
    }
}

#endif

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

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isNativeCodeLoaded
(JNIEnv *env, jobject peer) {
    return JNI_TRUE;
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
    
    if ([IOBluetoothHostController defaultController] == nil) {
        error = 1;
        return;
    }
    
    IOBluetoothHostController* controller = [IOBluetoothHostController defaultController];
    if (controller != NULL) {
        NSString* address = [controller addressAsString];
        strncpy(sData, [address cStringUsingEncoding:NSUnicodeStringEncoding], RUNNABLE_DATA_MAX);
        return;
    }
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceBluetoothAddress
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceBluetoothAddress"));
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
    
    if ([IOBluetoothHostController defaultController] == nil) {
        error = 1;
        return;
    }
    
    IOBluetoothHostController* controller = [IOBluetoothHostController defaultController];
    if (controller != NULL) {
        NSString* localName = [controller nameAsString];
        
        strncpy(sData, [localName cStringUsingEncoding:NSUnicodeStringEncoding], RUNNABLE_DATA_MAX);
        return;
    }
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceName
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceName"));
    GetLocalDeviceName runnable;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return NULL;
    }
    return env->NewStringUTF(runnable.sData);
}

RUNNABLE(GetDeviceClass, "GetDeviceClass") {
    
    if ([IOBluetoothHostController defaultController] == nil) {
        error = 1;
        return;
    }
    
    
    IOBluetoothHostController* controller = [IOBluetoothHostController defaultController];
    
    BluetoothClassOfDevice cod = [controller classOfDevice];
    
    lData = cod;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getDeviceClassImpl
(JNIEnv *env, jobject) {
    Edebug(("getDeviceClassImpl"));
    GetDeviceClass runnable;
    synchronousBTOperation(&runnable);
    return (jint)runnable.lData;
}

RUNNABLE(IsLocalDevicePowerOn, "IsLocalDevicePowerOn") {
    if ([IOBluetoothHostController defaultController] == nil) {
        error = 1;
        bData = false;
        return;
    }
    BluetoothHCIPowerState powerState = [[IOBluetoothHostController defaultController] powerState];
    bData = (powerState == kBluetoothHCIPowerStateON)?true:false;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDevicePowerOn
(JNIEnv *env, jobject) {
    Edebug(("isLocalDevicePowerOn"));
    IsLocalDevicePowerOn runnable;
    synchronousBTOperation(&runnable);
    return (runnable.bData)?JNI_TRUE:JNI_FALSE;
}

int IOBluetoothPreferenceGetDiscoverableState();

RUNNABLE(IsLocalDeviceDiscoverable, "IsLocalDeviceDiscoverable") {
    
    if ([IOBluetoothHostController defaultController] == nil) {
        error = 1;
        bData = false;
        return;
    }
    
    // Boolean discoverableStatus = IOBluetoothPreferenceGetDiscoverableState();
    
    bData = YES; // discoverableStatus;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceDiscoverableImpl
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceDiscoverableImpl"));
    IsLocalDeviceDiscoverable runnable;
    synchronousBTOperation(&runnable);
    return (runnable.bData)?JNI_TRUE:JNI_FALSE;
}

RUNNABLE(GetBluetoothHCISupportedFeatures, "GetBluetoothHCISupportedFeatures") {
    BluetoothHCISupportedFeatures features;
    //if (IOBluetoothLocalDeviceReadSupportedFeatures(&features, NULL, NULL, NULL)) {
    error = 1;
    return;
    //}
    lData = features.data[iData];
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDeviceFeatureSwitchRoles
(JNIEnv *env, jobject) {
    Edebug(("isLocalDeviceFeatureSwitchRoles"));
    GetBluetoothHCISupportedFeatures runnable;
    runnable.iData = 7;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return JNI_FALSE;
    }
    return (kBluetoothFeatureSwitchRoles & runnable.lData)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isLocalDeviceFeatureParkMode
(JNIEnv *env, jobject) {
    Edebug(("isLocalDeviceFeatureParkMode"));
    GetBluetoothHCISupportedFeatures runnable;
    runnable.iData = 6;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return JNI_FALSE;
    }
    return (kBluetoothFeatureParkMode & runnable.lData)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceL2CAPMTUMaximum
(JNIEnv *env, jobject) {
    return (jint)kBluetoothL2CAPMTUMaximum;
}

RUNNABLE(GetLocalDeviceVersion, "GetLocalDeviceVersion") {
    NumVersion* btVersion = (NumVersion*)pData[0];
    // BluetoothHCIVersionInfo* hciVersion = (BluetoothHCIVersionInfo*)pData[1];
    
    btVersion->majorRev = 10;
    btVersion->minorAndBugRev = 8;
    btVersion->nonRelRev = 2;
    btVersion->stage = 0;
    
    // hciVersion->manufacturerName = "Apple";
    // if (IOBluetoothGetVersion(btVersion, hciVersion)) {
    error = 1;
    //}
    
    error = 0;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceSoftwareVersionInfo
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceSoftwareVersionInfo"));
    NumVersion btVersion;
    char swVers[133];
    GetLocalDeviceVersion runnable;
    runnable.pData[0] = &btVersion;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return NULL;
    }
    
    snprintf(swVers, 133, "%1d.%1d.%1d rev %d", btVersion.majorRev, (btVersion.minorAndBugRev >> 4) & 0x0F,
             btVersion.minorAndBugRev & 0x0F, btVersion.nonRelRev);
    return env->NewStringUTF(swVers);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceSupportedSoftwareVersion
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceSupportedSoftwareVersion"));
    NumVersion btVersion;
    GetLocalDeviceVersion runnable;
    runnable.pData[0] = &btVersion;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return 0;
    }
    
    // Define starts with 0 and interprited as Octal constants
    int compiledFor = 0;
#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER
    // not Octal since API version 2
    compiledFor = 10803; // BLUETOOTH_VERSION_CURRENT;
#else
    compiledFor = 10603;
#endif
    //log_info("compiled for         %d", compiledFor);
    //log_info(" this majorRev       %d", (int)btVersion.majorRev);
    //log_info(" this minorAndBugRev %d", (int)btVersion.minorAndBugRev);
    
    jint v = (100 * ((100 * btVersion.majorRev) + ((btVersion.minorAndBugRev >> 4) & 0x0F))) + (btVersion.minorAndBugRev & 0x0F);
    //log_info(" this                %d", v);
    if (v < compiledFor) {
        localDeviceSupportedSoftwareVersion = v;
    } else {
        localDeviceSupportedSoftwareVersion = compiledFor;
    }
    return localDeviceSupportedSoftwareVersion;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceManufacturer
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceManufacturer"));
    BluetoothHCIVersionInfo hciVersion;
    GetLocalDeviceVersion runnable;
    runnable.pData[0] = &hciVersion;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return 0;
    }
    return hciVersion.manufacturerName;
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getLocalDeviceVersion
(JNIEnv *env, jobject) {
    Edebug(("getLocalDeviceVersion"));
    BluetoothHCIVersionInfo hciVersion;
    GetLocalDeviceVersion runnable;
    runnable.pData[0] = &hciVersion;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return 0;
    }
    char swVers[133];
    snprintf(swVers, 133, "LMP Version: %d.%d, HCI Version: %d.%d", hciVersion.lmpVersion, hciVersion.lmpSubVersion,
             hciVersion.hciVersion, hciVersion.hciRevision);
    return env->NewStringUTF(swVers);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_retrieveDevicesImpl
(JNIEnv *env, jobject peer, jint option, jobject retrieveDevicesCallback) {
    Edebug(("retrieveDevicesImpl"));
    RetrieveDevicesCallback callback;
    if (!callback.builCallback(env, peer, retrieveDevicesCallback)) {
        return JNI_FALSE;
    }
    
    jboolean result = JNI_TRUE;

    switch (option) {
        case RETRIEVEDEVICES_OPTION_PREKNOWN: {
            NSArray* pPairedDevices = [IOBluetoothDevice pairedDevices];
    
            if (pPairedDevices != NULL) {
                int deviceCount = [pPairedDevices count];
                for (int index = 0; index < deviceCount; index++) {
                    IOBluetoothDevice* device = (IOBluetoothDevice*)[pPairedDevices objectAtIndex:index];
                    jlong deviceAddr = OSxAddrToLong( [device getAddress] );
                    jint deviceClass = (jint) [device classOfDevice];
                    jstring deviceName = OSxNewJString(env, [device name]);
                    jboolean paired = JNI_TRUE;
               
                    if (!callback.callDeviceFoundCallback(env, deviceAddr, deviceClass, deviceName, paired)) {
                        result = JNI_FALSE;
                        break;
                    }
                }
            }
        }
        break;
            
    default:
      //Not supported
      result = JNI_FALSE;
    }
    
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isRemoteDeviceTrustedImpl
(JNIEnv *, jobject, jlong) {
    //TODO: This is just a dummy implementation to prevent UnstatisfiedLinkErrors on OS X
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_isRemoteDeviceAuthenticatedImpl
(JNIEnv *, jobject, jlong) {
    //TODO: This is just a dummy implementation to prevent UnstatisfiedLinkErrors on OS X
    return JNI_FALSE;
}



