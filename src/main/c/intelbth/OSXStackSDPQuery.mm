/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
 *  @version $Id: OSXStackDiscovery.mm 1174 2007-11-12 04:19:25Z skarzhevskyy $
 */

#import "OSXStackSDPQuery.h"

#define CPP_FILE "OSXStackSDPQuery.mm"

#define MAX_TERMINATE 10

jint terminatedTansID[MAX_TERMINATE] = {0};

StackSDPQueryStart::StackSDPQueryStart() {
    name = "StackSDPQueryStart";
    complete = FALSE;
    error = 0;
}

void callbackSDPQueryIsComplete(void* userRefCon, IOBluetoothDeviceRef deviceRef, IOReturn status) {
    if (!((StackSDPQueryStart*)userRefCon)->isCorrupted()) {
        ((StackSDPQueryStart*)userRefCon)->sdpQueryComplete(deviceRef, status);
    }
}

void StackSDPQueryStart::run() {
    startTime = CFDateCreate(kCFAllocatorDefault, CFAbsoluteTimeGetCurrent());

    BluetoothDeviceAddress btAddress;
    LongToOSxBTAddr(address, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    status = IOBluetoothDevicePerformSDPQuery(deviceRef, callbackSDPQueryIsComplete, this);
    if (kIOReturnSuccess != status) {
        error = 1;
    }
}

void StackSDPQueryStart::sdpQueryComplete(IOBluetoothDeviceRef deviceRef, IOReturn status) {
    this->status = status;
    if (kIOReturnSuccess != status) {
        this->error = 1;
    } else {
        CFArrayRef services = IOBluetoothDeviceGetServices(deviceRef);
        if (services != NULL) {
            recordsSize = CFArrayGetCount(services);
            CFDateRef updatedTime = IOBluetoothDeviceGetLastServicesUpdate(deviceRef);
            if (CFDateGetTimeIntervalSinceDate(updatedTime, this->startTime) < 0) {
                this->status = kIOReturnNotFound;
                this->error = 1;
            }
        } else {
            recordsSize = 0;
        }
    }
    CFRelease(this->startTime);
    this->complete = TRUE;
    if (stack != NULL) {
        MPSetEvent(stack->deviceInquiryNotificationEvent, 1);
    }
}

BOOL isSearchServicesTerminated(jint transID) {
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (terminatedTansID[i] == transID) {
            terminatedTansID[i] = 0;
            return TRUE;
        }
    }
    return FALSE;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_runSearchServicesImpl
(JNIEnv *env, jobject, jlong address, jint transID) {
    StackSDPQueryStart runnable;
    runnable.address = address;
    synchronousBTOperation(&runnable);
    while ((stack != NULL) && (runnable.error == 0) && (!runnable.complete)) {
        MPEventFlags flags;
        MPWaitForEvent(stack->deviceInquiryNotificationEvent, &flags, kDurationMillisecond * 500);
        if (isSearchServicesTerminated(transID)) {
            return 0;
        }
    }
    if (stack == NULL) {
        return 0;
    }
    if (runnable.error) {
        if (runnable.status == kIOReturnNotFound) {
            throwException(env, "com/intel/bluetooth/SearchServicesDeviceNotReachableException", "");
        } else {
            debug1("SearchServices error 0x%08x", runnable.status);
            throwException(env, "com/intel/bluetooth/SearchServicesException", "");
        }
        return 0;
    }
    Edebug1("runSearchServicesImpl found %i records", runnable.recordsSize);
    return runnable.recordsSize;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_cancelServiceSearchImpl
(JNIEnv* env, jobject, jint transID) {
    // This function is synchronized in Java
    for(int i = 0; i < MAX_TERMINATE; i ++) {
        if (terminatedTansID[i] == 0) {
            terminatedTansID[i] = transID;
            if (stack != NULL) {
                MPSetEvent(stack->deviceInquiryNotificationEvent, 1);
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
    LongToOSxBTAddr(address, &btAddress);
    IOBluetoothDeviceRef deviceRef = IOBluetoothDeviceCreateWithAddress(&btAddress);
    CFArrayRef services = IOBluetoothDeviceGetServices(deviceRef);
    if (services == NULL) {
        error = 1;
        return;
    }
    if (serviceRecordIndex >= CFArrayGetCount(services)) {
        error = 1;
        return;
    }
    IOBluetoothSDPServiceRecordRef serviceRef = (IOBluetoothSDPServiceRecordRef)CFArrayGetValueAtIndex(services, serviceRecordIndex);
    IOBluetoothSDPDataElementRef dataElementRef = IOBluetoothSDPServiceRecordGetAttributeDataElement(serviceRef, attrID);
    if (dataElementRef == NULL) {
        error = 1;
    }
    getData(dataElementRef);
}

void GetAttributeDataElement::getData(const IOBluetoothSDPDataElementRef dataElement) {
    if (dataElement == NULL) {
        return;
    }
    SDPOutputStream os;
    if (!os.writeElement(dataElement)) {
        error = 1;
    } else {
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

int SDPOutputStream::getLength(const IOBluetoothSDPDataElementRef dataElement) {
    BluetoothSDPDataElementTypeDescriptor typeDescrip = IOBluetoothSDPDataElementGetTypeDescriptor(dataElement);
    BluetoothSDPDataElementSizeDescriptor sizeDescriptor = IOBluetoothSDPDataElementGetSizeDescriptor(dataElement);
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
                IOBluetoothSDPUUIDRef	aUUIDRef = IOBluetoothSDPDataElementGetUUIDValue(dataElement);
				UInt8 length = IOBluetoothSDPUUIDGetLength(aUUIDRef);
				if (length <= 2) {
				    return 1 + 2;
				} else if (length <= 4) {
				    return 1 + 4;
				} else {
				    return 1 + 16;
			    }
            }
        case kBluetoothSDPDataElementTypeURL:
		case kBluetoothSDPDataElementTypeString: {
		        CFStringRef str = IOBluetoothSDPDataElementGetStringValue(dataElement);
		        CFIndex length = CFStringGetLength(str);
		        CFRelease(str);
		        if (length < 0x100) {
				    return length + 2;
			    } else if (length < 0x10000) {
				    return length + 3;
			    } else {
				    return length + 5;
			    }
		    }
		case kBluetoothSDPDataElementTypeBoolean:
            return 2;
        case kBluetoothSDPDataElementTypeDataElementSequence:
        case kBluetoothSDPDataElementTypeDataElementAlternative: {
            int len = 5;
            CFArrayRef array = IOBluetoothSDPDataElementGetArrayValue(dataElement);
            CFIndex count = CFArrayGetCount(array);
            for(CFIndex i = 0; i < count; i++) {
                const IOBluetoothSDPDataElementRef item = (IOBluetoothSDPDataElementRef)CFArrayGetValueAtIndex(array, i);
                len += getLength(item);
            }
            return len;
        }
        default:
            return 0;
    }
}

// See com.intel.bluetooth.SDPOutputStream
BOOL SDPOutputStream::writeElement(const IOBluetoothSDPDataElementRef dataElement) {
    BluetoothSDPDataElementTypeDescriptor typeDescrip = IOBluetoothSDPDataElementGetTypeDescriptor(dataElement);
    BluetoothSDPDataElementSizeDescriptor sizeDescriptor = IOBluetoothSDPDataElementGetSizeDescriptor(dataElement);
    ndebug("sizeDescriptor %i", sizeDescriptor);
    BOOL isSeq = false;
    BOOL isURL = false;
    BOOL isUnsigned = false;
    switch (typeDescrip) {
        case kBluetoothSDPDataElementTypeNil:
            write(0 | 0);
            break;
        case kBluetoothSDPDataElementTypeBoolean:
            write(40 | 0);
			CFNumberRef	aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
            UInt8 aBool;
			CFNumberGetValue(aNumber, kCFNumberCharType, &aBool);
			write(aBool);
            break;
        case kBluetoothSDPDataElementTypeUnsignedInt:
            isUnsigned = true;
        case kBluetoothSDPDataElementTypeSignedInt: {
                UInt8 type = isUnsigned ? 8: 16;
                ndebug("processing number %i", type);
                write(type | sizeDescriptor);
                if (sizeDescriptor == 4) { /* 16 byte integer */
				    CFDataRef bigData = IOBluetoothSDPDataElementGetDataValue(dataElement);
				    const UInt8 *byteArray = CFDataGetBytePtr(bigData);
				    write(byteArray, 16);
			    } else {
			        int length;
                    switch (sizeDescriptor) {
                        case 0: length = 1; break;
                        case 1: length = 2; break;
                        case 2: length = 4; break;
                        case 3: length = 8; break;
                    }
				    CFNumberRef	number = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				    SInt64 l = 0LL;
				    CFNumberGetValue(number, kCFNumberSInt64Type, &l);
				    ndebug("number len %i, %lli", length, l);
				    writeLong(l, length);
			    }
            }
            break;
        case kBluetoothSDPDataElementTypeUUID: {
                IOBluetoothSDPUUIDRef aUUIDRef = IOBluetoothSDPDataElementGetUUIDValue(dataElement);
			    const UInt8* uuidBytes = (UInt8*)IOBluetoothSDPUUIDGetBytes(aUUIDRef);
				UInt8 length = IOBluetoothSDPUUIDGetLength(aUUIDRef);
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
		        CFStringRef str = IOBluetoothSDPDataElementGetStringValue(dataElement);
		        CFIndex strLength = CFStringGetLength(str);
		        CFIndex maxBufLen = 4* sizeof(UInt8)*strLength;
		        UInt8* buffer = (UInt8*)malloc(maxBufLen);
		        CFIndex usedBufLen = 0;
		         CFStringEncoding encoding = isURL?kCFStringEncodingASCII:kCFStringEncodingUTF8;
		        CFStringGetBytes(str, CFRangeMake(0, strLength), encoding, '?', true, buffer, maxBufLen, &usedBufLen);
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
            isSeq = true;
            write(48 | 7);
        case kBluetoothSDPDataElementTypeDataElementAlternative: {
                if (!isSeq) {
                    write(56 | 7);
                }
                writeLong(getLength(dataElement) - 5, 4);
                CFArrayRef array = IOBluetoothSDPDataElementGetArrayValue(dataElement);
                CFIndex count = CFArrayGetCount(array);
                for(CFIndex i = 0; i < count; i++) {
                    const IOBluetoothSDPDataElementRef item = (IOBluetoothSDPDataElementRef)CFArrayGetValueAtIndex(array, i);
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