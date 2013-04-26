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

#import "OSXStackSDPQuery.h"

#import <IOBluetooth/IOBluetooth.h>
#import <CoreFoundation/CoreFoundation.h>
#import <CoreFoundation/CFDate.h>

#define CPP_FILE "OSXStackSDPQuery.mm"

#define MAX_TERMINATE 10

@implementation SDPQueryHandler

-(id)initWithOwner:(StackSDPQueryStart *)owner
{
    if ((self = [super init]) != nil) {
        _owner = owner;
    }
    
    return self;
}

-(void)sdpQueryComplete:(IOBluetoothDevice *)device status:(IOReturn)status
{
    if (_owner != NULL) {
        _owner->sdpQueryComplete(device, status);
    }
}

-(void)connectionComplete:(IOBluetoothDevice *)device status:(IOReturn)status
{
    
}

- (void)remoteNameRequestComplete:(IOBluetoothDevice *)device status:(IOReturn)status
{
    
}

@end

jint terminatedTansID[MAX_TERMINATE] = {0};
jint runningTansID[MAX_TERMINATE] = {0};
void* searchServicesIgnore[MAX_TERMINATE] = {0};

void setSearchServicesIgnore(void* ptr) {
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (searchServicesIgnore[i] == NULL) {
            searchServicesIgnore[i] = ptr;
            break;
        }
    }
}

BOOL isSearchServicesIgnore(void* ptr) {
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (searchServicesIgnore[i] == ptr) {
            searchServicesIgnore[i] = NULL;
            return true;
        }
    }
    return false;
}

StackSDPQueryStart::StackSDPQueryStart() {
    name = "StackSDPQueryStart";
    complete = FALSE;
    device = nil;
    handler = nil;
    error = 0;
}

/*
void callbackSDPQueryIsComplete(void* userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status) {
    if ((!isSearchServicesIgnore(userRefCon)) && (!isRunnableCorrupted((StackSDPQueryStart*)userRefCon))) {
        ((StackSDPQueryStart*)userRefCon)->sdpQueryComplete(deviceRef, status);
    }
}
*/

void StackSDPQueryStart::run() {
    startTime = [NSDate date];

    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(this->address, &btAddress);
    IOBluetoothDevice* device = [IOBluetoothDevice deviceWithAddress:(const BluetoothDeviceAddress*)&btAddress];
    if (device == NULL) {
        error = 1;
        return;
    }
    
    handler = [[SDPQueryHandler alloc] initWithOwner:this];
    
    [device performSDPQuery:handler];
}

void StackSDPQueryStart::sdpQueryComplete(IOBluetoothDevice* device, IOReturn status)
{
    ndebug(("sdpQueryComplete 0x%08x", status));
    
    this->status = status;
    
    // Apperantly connection to device is still open after SDP query for some time. This may affect other connections.
    if (device != nil) {
        [device closeConnection];
    }
    
    if (kIOReturnSuccess != status) {
        this->error = 1;
    }
    else {
        NSArray* services = [device services];
        
        if (services != NULL) {
            recordsSize = [services count];
            
            NSDate* updatedTime = [device lastNameUpdate];
            
            if ([updatedTime compare:this->startTime] < 0) {
                this->status = kIOReturnNotFound;
                this->error = 1;
            }
        }
        else {
            recordsSize = 0;
        }
    }
    this->complete = TRUE;
    if (stack != NULL) {
        dispatch_semaphore_signal(stack->deviceInquiryNotificationEvent); // , 1);
    }
}

BOOL isSearchServicesTerminated(jint transID) {
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (terminatedTansID[i] == transID) {
            terminatedTansID[i] = 0;
            runningTansID[i] = 0;
            return TRUE;
        }
    }
    return FALSE;
}

void setSearchServicesRunning(jint transID) {
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (runningTansID[i] == 0) {
            runningTansID[i] = transID;
            break;
        }
    }
}

void setSearchServicesEnds(jint transID) {
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (runningTansID[i] == transID) {
            runningTansID[i] = 0;
            break;
        }
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runSearchServicesImpl
(JNIEnv *env, jobject, jlong address, jint transID) {
    setSearchServicesRunning(transID);
    StackSDPQueryStart runnable;
    runnable.address = address;
    synchronousBTOperation(&runnable);
    while ((stack != NULL) && (runnable.error == 0) && (!runnable.complete)) {
                dispatch_semaphore_wait(stack->deviceInquiryNotificationEvent, dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_MSEC * 500));
        if (isSearchServicesTerminated(transID)) {
            setSearchServicesIgnore(&runnable);
            return 0;
        }
    }
    setSearchServicesEnds(transID);
    if (!runnable.complete) {
        setSearchServicesIgnore(&runnable);
    }
    if (stack == NULL) {
        return 0;
    }
    if (runnable.error) {
        if (runnable.status == kIOReturnNotFound) {
            throwException(env, "com/intel/bluetooth/SearchServicesDeviceNotReachableException", "");
        } else {
            debug(("SearchServices error 0x%08x", runnable.status));
            throwException(env, "com/intel/bluetooth/SearchServicesException", "");
        }
        return 0;
    }
    Edebug(("runSearchServicesImpl %i found %i records", transID, runnable.recordsSize));
    return runnable.recordsSize;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_cancelServiceSearchImpl
(JNIEnv* env, jobject, jint transID) {
    // This function is synchronized in Java
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if ((runningTansID[i] == transID) && (terminatedTansID[i] == 0)) {
            terminatedTansID[i] = transID;
            if (stack != NULL) {
                dispatch_semaphore_signal(stack->deviceInquiryNotificationEvent); // , 1);
            }
            break;
        }
    }
}

GetAttributeDataElement::GetAttributeDataElement() {
    name = "GetAttributeDataElement";
}

void GetAttributeDataElement::run() {

    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(this->address, &btAddress);
    IOBluetoothDevice* device = [IOBluetoothDevice deviceWithAddress:(const BluetoothDeviceAddress*)&btAddress];
    if (device == NULL) {
        error = 1;
        return;
    }
    
    NSArray* services = [device services];

    if (services == NULL) {
        error = 1;
        return;
    }
    
    if (serviceRecordIndex >= [services count]) {
        error = 1;
        return;
    }
    
    IOBluetoothSDPServiceRecord* service = (IOBluetoothSDPServiceRecord*)[services objectAtIndex:serviceRecordIndex];
    IOBluetoothSDPDataElement* dataElement = [service getAttributeDataElement:attrID];
    
    if (dataElement == nil) {
        error = 1;
    }
    
    getData(dataElement);
}

void GetAttributeDataElement::getData(const IOBluetoothSDPDataElement* dataElement)
{
    if (dataElement == NULL) {
        return;
    }
    
    SDPOutputStream os;
    if (!os.writeElement(dataElement)) {
        error = 1;
    }
    else {
        os.getBytes(DATA_BLOB_MAX, &dataLen, data);
    }
}

SDPOutputStream::SDPOutputStream() {
    data = CFDataCreateMutable(NULL, 0);
}

SDPOutputStream::~SDPOutputStream() {
    CFRelease(data);
}

void SDPOutputStream::write(const UInt8 byte) {
    CFDataAppendBytes(data, &byte, 1);
}

void SDPOutputStream::write(const UInt8* bytes, CFIndex length) {
    CFDataAppendBytes(data, bytes, length);
}

void SDPOutputStream::writeLong(UInt64 l, int size) {
	UInt64 v = l;
	for (int i = 0; i < size; i++) {
		write((UInt8) (0xFFLL & (v >> (size - 1 << 3))));
		v <<= 8;
	}
}

void SDPOutputStream::getBytes(int max, int* dataLen, UInt8* buf) {
    CFIndex len = MIN(max, CFDataGetLength(data));
    CFDataGetBytes(data, CFRangeMake(0, len), buf);
    (*dataLen) = len;
}

int SDPOutputStream::getLength(const IOBluetoothSDPDataElement* dataElement)
{
    BluetoothSDPDataElementTypeDescriptor typeDescrip = [dataElement getTypeDescriptor];
    BluetoothSDPDataElementSizeDescriptor sizeDescriptor = [dataElement getSizeDescriptor];
    
    BOOL isURL = false;
    
    switch (typeDescrip) {
        case kBluetoothSDPDataElementTypeNil:
            return 1;
        case kBluetoothSDPDataElementTypeUnsignedInt:
        case kBluetoothSDPDataElementTypeSignedInt: {
                int length;
                switch (sizeDescriptor) {
                    case 0: length = 1; break;
                    case 1: length = 2; break;
                    case 2: length = 4; break;
                    case 3: length = 8; break;
                    case 4: length = 16; break;
                }
                return 1 + length;
            }
        case kBluetoothSDPDataElementTypeUUID: {
                IOBluetoothSDPUUID*	aUUIDRef = [dataElement getUUIDValue];
                if (aUUIDRef == NULL) {
                    return -1;
                }
				UInt8 length = [aUUIDRef length];
				if (length <= 2) {
				    return 1 + 2;
				} else if (length <= 4) {
				    return 1 + 4;
				} else {
				    return 1 + 16;
			    }
            }
        case kBluetoothSDPDataElementTypeURL:
            isURL = true;
		case kBluetoothSDPDataElementTypeString: {
                NSString* str = [dataElement getStringValue];
                if (str == NULL) {
                    return -1;
                }
                CFIndex strLength = [str length];
		        NSUInteger usedBufLen = 0;
		        CFStringEncoding encoding = isURL?kCFStringEncodingASCII:kCFStringEncodingUTF8;
            
                [str getBytes:NULL maxLength:strLength usedLength:&usedBufLen encoding:encoding options:NULL range:NSMakeRange(0, strLength) remainingRange:NULL];
		        //CFStringGetBytes(str, CFRangeMake(0, strLength), encoding, '?', true, NULL, 0, &usedBufLen);
		        if (usedBufLen < 0x100) {
				    return usedBufLen + 2;
			    } else if (usedBufLen < 0x10000) {
				    return usedBufLen + 3;
			    } else {
				    return usedBufLen + 5;
			    }
		    }
		case kBluetoothSDPDataElementTypeBoolean:
            return 2;
        case kBluetoothSDPDataElementTypeDataElementSequence:
        case kBluetoothSDPDataElementTypeDataElementAlternative: {
            int len = 5;
            NSArray* array = [dataElement getArrayValue];
            if (array == NULL) {
                return -1;
            }
            CFIndex count = [array count];
            for(CFIndex i = 0; i < count; i++) {
                const IOBluetoothSDPDataElement* item = (IOBluetoothSDPDataElement*)[array objectAtIndex:i];
                len += getLength(item);
            }
            return len;
        }
        default:
            return 0;
    }
}

// See com.intel.bluetooth.SDPOutputStream
BOOL SDPOutputStream::writeElement(const IOBluetoothSDPDataElement* dataElement)
{
    BluetoothSDPDataElementTypeDescriptor typeDescrip = [dataElement getTypeDescriptor];
    BluetoothSDPDataElementSizeDescriptor sizeDescriptor = [dataElement getSizeDescriptor];
    
    ndebug(("sizeDescriptor %i", sizeDescriptor));
    
    BOOL isSeq = false;
    BOOL isURL = false;
    BOOL isUnsigned = false;
    switch (typeDescrip) {
        case kBluetoothSDPDataElementTypeNil:
            {
                write(0 | 0);
            }
            break;
            
        case kBluetoothSDPDataElementTypeBoolean:
            {
                write(40 | 0);
                NSNumber* bNumber = [dataElement getNumberValue];
                if (bNumber == NULL) {
                    return FALSE;
                }
                UInt8 aBool = [bNumber boolValue];
                write(aBool);
            }
            break;
        case kBluetoothSDPDataElementTypeUnsignedInt:
            {
                isUnsigned = true;
            }
            
        case kBluetoothSDPDataElementTypeSignedInt:
            {
                UInt8 type = isUnsigned ? 8: 16;
                ndebug(("processing number %i", type));
                write(type | sizeDescriptor);
                if (sizeDescriptor == 4) { /* 16 byte integer */
                    NSData* bigData = [dataElement getDataValue];
				    if (bigData == NULL) {
				        return FALSE;
				    }
				    const UInt8 *byteArray = (UInt8 *)[bigData bytes];
				    write(byteArray, 16);
			    } else {
			        int length;
                    switch (sizeDescriptor) {
                        case 0: length = 1; break;
                        case 1: length = 2; break;
                        case 2: length = 4; break;
                        case 3: length = 8; break;
                    }
				    NSNumber* number = [dataElement getNumberValue];
				    if (number == NULL) {
				        return FALSE;
				    }
				    SInt64 l = [number longLongValue];
 				    ndebug(("number len %i, %lli", length, l));
				    writeLong(l, length);
			    }
            }
            break;
            
        case kBluetoothSDPDataElementTypeUUID: {
                IOBluetoothSDPUUID* aUUIDRef = [dataElement getUUIDValue];
                if (aUUIDRef == NULL) {
                    return FALSE;
                }
                const UInt8* uuidBytes = (UInt8*)[aUUIDRef bytes];
            	UInt8 length = [aUUIDRef length];
				UInt8 size = 0;
                if (length <= 2) {
                    size = 2;
                    write(24 | 1);
				} else if (length <= 4) {
				    size = 4;
				    write(24 | 2);
				} else if (length <= 16) {
				    size = 16;
				    write(24 | 4);
			    } else {
			        return FALSE;
			    }
			    for(int p = length; p < size; p ++) {
			        // This should not happen anyway.
			        write(0);
			    }
			    write(uuidBytes, length);
			}
            break;
        case kBluetoothSDPDataElementTypeURL:
            isURL = true;
		case kBluetoothSDPDataElementTypeString: {
		        UInt8 type = isURL ? 0x40: 0x20;
		        NSString* str = [dataElement getStringValue];
		        if (str == NULL) {
		            return FALSE;
		        }
		        CFIndex strLength = [str length];
		        CFIndex maxBufLen = 4* sizeof(UInt8)*strLength;
		        UInt8* buffer = (UInt8*)malloc(maxBufLen);
		        NSUInteger usedBufLen = 0;
		        CFStringEncoding encoding = isURL?kCFStringEncodingASCII:kCFStringEncodingUTF8;
                [str getBytes:buffer maxLength:maxBufLen usedLength:&usedBufLen encoding:encoding options:NULL range:NSMakeRange(0, strLength) remainingRange:NULL];
		        //CFStringGetBytes(str, CFRangeMake(0, strLength), encoding, '?', false, buffer, maxBufLen, &usedBufLen);
		        if (usedBufLen < 0x100) {
				    write(type | 5);
				    writeLong(usedBufLen, 1);
			    } else if (usedBufLen < 0x10000) {
				    write(type | 6);
				    writeLong(usedBufLen, 2);
			    } else {
				    write(type | 7);
				    writeLong(usedBufLen, 4);
			    }
		        write(buffer, usedBufLen);
		        free(buffer);
		    }
            break;
        case kBluetoothSDPDataElementTypeDataElementSequence:
            {
                isSeq = true;
                write(48 | 7);
            }
        case kBluetoothSDPDataElementTypeDataElementAlternative: {
                if (!isSeq) {
                    write(56 | 7);
                }
                int len = getLength(dataElement);
                if (len < 0) {
		            return FALSE;
		        }
                writeLong(len - 5, 4);
                NSArray* array = [dataElement getArrayValue];
                if (array == NULL) {
		            return FALSE;
		        }
                CFIndex count = [array count];
                for(CFIndex i = 0; i < count; i++) {
                    const IOBluetoothSDPDataElement* item = (IOBluetoothSDPDataElement*)[array objectAtIndex:i];
                    if (!writeElement(item)) {
                        return FALSE;
                    }
                }
            }
            break;
    }
    return TRUE;
}


JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getServiceAttributeImpl
(JNIEnv* env, jobject, jlong address, jlong serviceRecordIndex, jint attrID) {
    GetAttributeDataElement runnable;
    runnable.address = address;
    runnable.serviceRecordIndex = serviceRecordIndex;
    runnable.attrID = attrID;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return NULL;
    }
    // construct byte array to hold blob
	jbyteArray result = env->NewByteArray(runnable.dataLen);
    jbyte *bytes = env->GetByteArrayElements(result, 0);
	memcpy(bytes, runnable.data, runnable.dataLen);
	env->ReleaseByteArrayElements(result, bytes, 0);
	return result;
}
