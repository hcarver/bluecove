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

#import "OSXStackSDPServer.h"
#import <IOBluetooth/objc/IOBluetoothSDPUUID.h>

#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER
#import <IOBluetooth/objc/IOBluetoothHostController.h>
#endif

#define CPP_FILE "OSXStackSDPServer.mm"

NSString *kServiceItemKeyServiceClassIDList = @"0001 - ServiceClassIDList";
NSString *kServiceItemKeyServiceName = @"0100 - ServiceName*";
NSString *kServiceItemKeyProtocolDescriptorList = @"0004 - ProtocolDescriptorList";

NSString *kDataElementSize = @"DataElementSize";
NSString *kDataElementType = @"DataElementType";
NSString *kDataElementValue = @"DataElementValue";

ServerController::ServerController() {
    isClosed = false;
    sdpEntries = NULL;
    sdpServiceRecordHandle = 0;
    sdpSequenceDepthCurrent = -1;
    for(int i = 0; i < SDP_SEQUENCE_DEPTH_MAX; i ++) {
        sdpSequence[i] = NULL;
    }
    incomingChannelNotification = NULL;
    incomingChannelNotificationEvent = dispatch_semaphore_create(0);
    acceptedEvent = dispatch_semaphore_create(0);
}

ServerController::~ServerController() {
    isClosed = true;
    dispatch_semaphore_signal(incomingChannelNotificationEvent); // , 0);
    dispatch_release(incomingChannelNotificationEvent);
    dispatch_semaphore_signal(acceptedEvent); // , 0);
    dispatch_release(acceptedEvent);
}

void ServerController::init() {
    sdpEntries = [[NSMutableDictionary dictionaryWithCapacity:256] retain];
}

ServerController* validServerControllerHandle(JNIEnv *env, jlong handle, jchar handleType) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return NULL;
	}
	return (ServerController*)stack->commPool->getObject(env, handle, handleType);
}

RUNNABLE(SDPServiceUpdateServiceRecord, "SDPServiceUpdateServiceRecord") {
    ServerController* comm = (ServerController*)pData[0];
    lData = comm->updateSDPServiceRecord();
    if (lData != kIOReturnSuccess) {
	    error = 1;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_sdpServiceUpdateServiceRecordPublish
  (JNIEnv *env, jobject peer, jlong handle, jchar handleType) {
    ServerController* comm = validServerControllerHandle(env, handle, handleType);
    if (comm == NULL) {
		return;
	}
	SDPServiceUpdateServiceRecord runnable;
	runnable.pData[0] = comm;
    synchronousBTOperation(&runnable);

    if (runnable.error != 0) {
        throwServiceRegistrationException(env, "Failed to publish service [0x%08x]", runnable.lData);
    }
}

NSMutableDictionary* createIntDataElement(int size, int type, int value) {
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithCapacity:3];
    [dict setObject:[NSNumber numberWithInt:size] forKey:kDataElementSize];
    [dict setObject:[NSNumber numberWithInt:type] forKey:kDataElementType];
    [dict setObject:[NSNumber numberWithInt:value] forKey:kDataElementValue];
    return dict;
}

NSMutableDictionary* createDataElement(BluetoothSDPDataElementTypeDescriptor type, UInt32 size, NSObject* value) {
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithCapacity:3];
    if (size > 0) {
        [dict setObject:[NSNumber numberWithInt:size] forKey:kDataElementSize];
    }
    [dict setObject:[NSNumber numberWithInt:type] forKey:kDataElementType];
    if (value != NULL) {
        [dict setObject:value forKey:kDataElementValue];
    }
    return dict;
}

const char* ServerController::addAttributeSequence(jint attrID, jint attrType) {
    if (attrType == -1) {
        if (sdpSequenceDepthCurrent < 0) {
            return "Sequence End overflow";
        } else {
            sdpSequence[sdpSequenceDepthCurrent] = NULL;
            sdpSequenceDepthCurrent --;
            return NULL;
        }
    }
    if (sdpSequenceDepthCurrent >= SDP_SEQUENCE_DEPTH_MAX) {
            return "Sequence Start overflow";
    }

    BluetoothSDPDataElementTypeDescriptor newType;
    switch (attrType) {
        case DATA_ELEMENT_TYPE_DATSEQ:
            newType = kBluetoothSDPDataElementTypeDataElementSequence;
            break;
        case DATA_ELEMENT_TYPE_DATALT:
            newType = kBluetoothSDPDataElementTypeDataElementAlternative;
            break;
        default:
            return "Invalid sequence attribute type";
    }
    NSMutableArray *sequence = [NSMutableArray array];
    addDataElement(attrID, createDataElement(newType, 0, sequence));

    sdpSequenceDepthCurrent ++;
    sdpSequence[sdpSequenceDepthCurrent] = sequence;

    return NULL;
}

const char* ServerController::addDataElement(jint attrID, NSObject* value) {
    if ((attrID < 0) && (sdpSequenceDepthCurrent < 0)) {
        return "sequence expected";
    }
    if (sdpSequenceDepthCurrent >= 0) {
        [sdpSequence[sdpSequenceDepthCurrent] addObject:value];
    } else {
        NSString *keyName = [NSString stringWithFormat:@"%03lx",  (long) attrID];
        [sdpEntries setObject:value forKey:keyName];
    }
    return NULL;
}

const char* ServerController::addAttribute(SDPAttributeValue* value) {
    BluetoothSDPDataElementTypeDescriptor newType;
    UInt32 newSize;
    NSObject* newValue;

    const char* rc = NULL;

    switch (value->attrType) {
    case DATA_ELEMENT_TYPE_U_INT_1:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        //newSizeDescriptor = 0;
        newSize = 1;
        newValue = [NSNumber numberWithUnsignedInt:((unsigned int)value->numberValue)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_U_INT_2:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        //newSizeDescriptor = 1;
        newSize = 2;
        newValue = [NSNumber numberWithUnsignedInt:((unsigned int)value->numberValue)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_U_INT_4:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        //newSizeDescriptor = 2;
        newSize = 4;
        newValue = [NSNumber numberWithUnsignedLong:((unsigned long)value->numberValue)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_INT_1:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        //newSizeDescriptor = 0;
        newSize = 1;
        newValue = [NSNumber numberWithInt:((int)value->numberValue)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_INT_2:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        //newSizeDescriptor = 1;
        newSize = 2;
        newValue = [NSNumber numberWithInt:((int)value->numberValue)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_INT_4:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        //newSizeDescriptor = 2;
        newSize = 4;
        newValue = [NSNumber numberWithLong:value->numberValue];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_INT_8:
        //TODO Not properly discovered!
        newType = kBluetoothSDPDataElementTypeSignedInt;
        //newSizeDescriptor = 3;
        newSize = 8;
        newValue = [NSNumber numberWithLongLong:value->numberValue];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_U_INT_8:
        //TODO Not properly discovered!
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        //newSizeDescriptor = 3;
        newSize = 8;
        {
            jbyte rbytes[8];
            for(int i = 0; i < 8; i ++) {
                rbytes[i] = value->arrayValue[7 - i];
            }
            unsigned long long lvalue = 0;
            memcpy(&lvalue, rbytes, 8);
            newValue = [NSNumber numberWithUnsignedLongLong:lvalue];
            //newValue = [NSData dataWithBytes:(value->arrayValue) length:8];
            rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        }
        break;
    case DATA_ELEMENT_TYPE_U_INT_16:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        //newSizeDescriptor = 4;
        newSize = 16;
        newValue = [NSData dataWithBytes:(value->arrayValue) length:16];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_INT_16:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        //newSizeDescriptor = 4;
        newSize = 16;
        newValue = [NSData dataWithBytes:(value->arrayValue) length:16];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;

    case DATA_ELEMENT_TYPE_NULL:
        newType = kBluetoothSDPDataElementTypeNil;
        //newSizeDescriptor = 0;
        newSize = 0;
        newValue = NULL;
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_BOOL:
        // TODO
        //newType = kBluetoothSDPDataElementTypeBoolean;
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        //newSizeDescriptor = 0;
        newSize = 1;
        newValue = [NSNumber numberWithInt:((int)value->numberValue)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_UUID:
        newType = kBluetoothSDPDataElementTypeUUID;
        //newSizeDescriptor = 4;
        newSize = value->arrayLen;
        newValue = [IOBluetoothSDPUUID uuidWithBytes:(const void *)(value->arrayValue) length:(unsigned)(value->arrayLen)];
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_STRING:
        newType = kBluetoothSDPDataElementTypeString;
        newValue = [[NSString alloc] initWithBytes:(const void *)(value->arrayValue) length:(unsigned long)(value->arrayLen) encoding:NSUTF8StringEncoding];
        newSize = 0;
        rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    case DATA_ELEMENT_TYPE_URL:
        //TODO
        //newType = kBluetoothSDPDataElementTypeURL;
        newType = kBluetoothSDPDataElementTypeString;
        // UTF8 is just encoding for test interface.
        newValue = [[NSString alloc] initWithBytes:(const void *)(value->arrayValue) length:(unsigned long)(value->arrayLen) encoding:NSNonLossyASCIIStringEncoding];
        newSize = 0;
		rc = addDataElement(value->attrID, createDataElement(newType, newSize, newValue));
        break;
    default:
        rc = "Invalid attribute type";
    }

    return rc;
}

RUNNABLE(SDPServiceAddAttribute, "SDPServiceAddAttribute") {
    ServerController* comm = (ServerController*)pData[0];
    pData[2] = comm->addAttribute((SDPAttributeValue*)pData[1]);
    if (pData[2] != NULL) {
	    error = 1;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_sdpServiceAddAttribute
  (JNIEnv *env, jobject peer, jlong handle, jchar handleType, jint attrID, jint attrType, jlong numberValue, jbyteArray arrayValue) {
    ServerController* comm = validServerControllerHandle(env, handle, handleType);
    if (comm == NULL) {
		return;
	}
	SDPServiceAddAttribute runnable;
	runnable.pData[0] = comm;
      
	SDPAttributeValue value = {0};
	runnable.pData[1] = &value;

	value.attrID = attrID;
    value.attrType = attrType;
	value.numberValue = numberValue;
	if (arrayValue != NULL) {
	    value.arrayValue = env->GetByteArrayElements(arrayValue, 0);
	    value.arrayLen = env->GetArrayLength(arrayValue);
    }

    synchronousBTOperation(&runnable);

    if (arrayValue != NULL) {
        env->ReleaseByteArrayElements(arrayValue, value.arrayValue, 0);
    }

    if (runnable.error != 0) {
        throwServiceRegistrationException(env, "Failed to update service attribute %x [%s]", attrID, runnable.pData[2]);
    }
}

RUNNABLE(SDPServiceAddAttributeSequence, "SDPServiceAddAttributeSequence") {
    ServerController* comm = (ServerController*)pData[0];
    pData[2] = comm->addAttributeSequence(((SDPAttributeValue*)pData[1])->attrID, ((SDPAttributeValue*)pData[1])->attrType);
    if (pData[2] != NULL) {
	    error = 1;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_sdpServiceSequenceAttributeStart
  (JNIEnv *env, jobject peer, jlong handle, jchar handleType, jint attrID, jint attrType) {
    ServerController* comm = validServerControllerHandle(env, handle, handleType);
    if (comm == NULL) {
		return;
	}
	SDPServiceAddAttributeSequence runnable;
	runnable.pData[0] = comm;

	SDPAttributeValue value = {0};
	runnable.pData[1] = &value;
	value.attrID = attrID;
    value.attrType = attrType;

    synchronousBTOperation(&runnable);

    if (runnable.error != 0) {
        throwServiceRegistrationException(env, "Failed to update service attribute %x [%s]", attrID, runnable.pData[2]);
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_sdpServiceSequenceAttributeEnd
  (JNIEnv *env, jobject peer, jlong handle, jchar handleType, jint attrID) {
    ServerController* comm = validServerControllerHandle(env, handle, handleType);
    if (comm == NULL) {
		return;
	}
	SDPServiceAddAttributeSequence runnable;
	runnable.pData[0] = comm;

	SDPAttributeValue value = {0};
	runnable.pData[1] = &value;
	value.attrID = attrID;
    value.attrType = -1;

    synchronousBTOperation(&runnable);

    if (runnable.error != 0) {
        throwServiceRegistrationException(env, "Failed to update service attribute %x [%s]", attrID, runnable.pData[2]);
    }
}

#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER

BluetoothClassOfDevice updatedServiceClasses = 0;

RUNNABLE(SetDeviceClass, "SetDeviceClass") {
    IOBluetoothHostController* controller = [IOBluetoothHostController defaultController];

    BluetoothClassOfDevice SERVICE_MASK = 0xFFC000;

    BluetoothClassOfDevice serviceClasses = SERVICE_MASK & lData;
    BluetoothClassOfDevice currentClassOfDevice = [controller classOfDevice];

    // Unset previously set bits
    BluetoothClassOfDevice newClassofDevice = currentClassOfDevice & (~updatedServiceClasses);
    newClassofDevice = newClassofDevice | serviceClasses;
    //if (newClassofDevice == currentClassOfDevice) {
        // Need to run update again with short dellay, ClassOfDevice was not Reverted
        //ndebug(("SetDeviceClass [0x%08x] not set [0x%08x] service [0x%08x]", currentClassOfDevice, newClassofDevice, serviceClasses));
        //bData = false;
        //return;
    //}

    ndebug(("SetDeviceClass [0x%08x] -> [0x%08x] service [0x%08x]", currentClassOfDevice, newClassofDevice, serviceClasses));
    bData = true;

    NSTimeInterval seconds = 120;
    IOReturn status = [controller setClassOfDevice:newClassofDevice forTimeInterval:seconds];
    if (status != kIOReturnSuccess) {
        error = 1;
        lData = status;
    } else {
        updatedServiceClasses = serviceClasses;
    }
}

#endif

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_setLocalDeviceServiceClassesImpl
 (JNIEnv *env, jobject peer, jint classOfDevice) {
#ifdef AVAILABLE_BLUETOOTH_VERSION_2_0_AND_LATER
    SetDeviceClass runnable;
    runnable.lData = classOfDevice;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        debug(("setClassOfDevice [0x%08x]", runnable.lData));
    }
    if (runnable.bData) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
#else
    return JNI_FALSE;
#endif
}
