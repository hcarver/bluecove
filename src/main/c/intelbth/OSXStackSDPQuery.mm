/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
 *  Copyright (C) 2007 Eric Wagner
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
        if (isSearchServicesTerminated(transID) {
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

GetAttributeDataElement:GetAttributeDataElement() {
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
    if (CFArrayGetCount(services) <= serviceRecordIndex)) {
        error = 1;
        return;
    }
    IOBluetoothSDPServiceRecordRef serviceRef = (IOBluetoothSDPServiceRecordRef)CFArrayGetValueAtIndex(services, serviceRecordIndex);
    dataElementRef = IOBluetoothSDPServiceRecordGetAttributeDataElement(serviceRef, attrID);
    if (dataElementRef == NULL) {
        error = 1;
    }
}
jobject buildJDataElement(JNIEnv *env, IOBluetoothSDPDataElementRef dataElement);

JNIEXPORT jobject JNICALL Java_com_intel_bluetooth_BluetoothStackOSX_getServiceAttributeImpl
(JNIEnv* env, jobject, jlong address, jlong serviceRecordIndex, jint attrID) {
    GetAttributeDataElement runnable;
    runnable.address = address;
    runnable.serviceRecordIndex = serviceRecordIndex;
    runnable.attrID = attrID;
    synchronousBTOperation(&runnable);
    if (runnable.error) {
        return NULL;
    }
    return buildJDataElement(runnable.dataElementRef);
}

#define JAVA_ENV_CHECK(x) (*env)->x; if ((*env)->ExceptionOccurred(env)) {(*env)->ExceptionDescribe(env); debug("Error exception Occurred"); return NULL;}

jobject buildJDataElement(JNIEnv *env, IOBluetoothSDPDataElementRef dataElement) {

	jclass									dataElementClass;
	jobject									jDataElement;
	jmethodID								constructor;
	BluetoothSDPDataElementTypeDescriptor	typeDescrip;
	BluetoothSDPDataElementSizeDescriptor	typeSize;
	UInt32									byteSize;
	jboolean								isUnsigned, isURL, isSequence;

	if((*env)->ExceptionOccurred(env)) (*env)->ExceptionDescribe(env);
	dataElementClass = (*env)->FindClass(env, "javax/bluetooth/DataElement");
	typeDescrip = IOBluetoothSDPDataElementGetTypeDescriptor(dataElement);
	typeSize = IOBluetoothSDPDataElementGetSizeDescriptor(dataElement);
	byteSize = IOBluetoothSDPDataElementGetSize(dataElement);
	isUnsigned = 0;
	isURL = 0;
	isSequence = 0;

	switch(typeDescrip) {
		case kBluetoothSDPDataElementTypeNil:
				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(I)V");
				jDataElement = (*env)->NewObject(env, dataElementClass, constructor, 0);
				break;
		case kBluetoothSDPDataElementTypeUnsignedInt:
			isUnsigned = 1;
		case kBluetoothSDPDataElementTypeSignedInt:
			if(typeSize==4) { /* 16 byte integer */
				CFDataRef			bigData;
				const UInt8			*byteArray;
				jbyteArray			aJByteArray;

				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V");
				bigData = IOBluetoothSDPDataElementGetDataValue(dataElement);
				byteArray = CFDataGetBytePtr(bigData);
				aJByteArray = (*env)->NewByteArray(env, 16);
				(*env)->SetByteArrayRegion(env, aJByteArray, 0, 16, (jbyte*)byteArray);
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, isUnsigned ? 0x0C : 0x14, aJByteArray));
			} else {
				CFNumberRef		aNumber;
				jint			typeValue;
				jlong			aBigInt = 0LL;

				constructor = (*env)->GetMethodID(env, dataElementClass, "<init>", "(IJ)V");
				typeValue = 0;
				aBigInt = 0;
				aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				CFNumberGetValue (aNumber, kCFNumberLongLongType, &aBigInt);
				switch(typeSize) {
					case 0: /* 1 byte int */
						if(isUnsigned && (aBigInt < 0)) aBigInt += 0x100;
						typeValue = (isUnsigned ? 0x08 : 0x10 );
						break;
					case 1: /* 2 byte int */
						if(isUnsigned && (aBigInt < 0)) aBigInt += 0x10000;
						typeValue = (isUnsigned ? 0x09 : 0x11 );
						break;
					case 2: /* 4 byte int */
						if(isUnsigned && (aBigInt < 0)) aBigInt += 0x100000000;
						typeValue	= (isUnsigned ? 0x0A : 0x12 );
						break;
					case 3: /* 8 byte int */
						typeValue = (isUnsigned ? 0x0B : 0x13 );
						break;
					}
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, typeValue, aBigInt));
			}
			break;
		case kBluetoothSDPDataElementTypeUUID:
			{
				IOBluetoothSDPUUIDRef	aUUIDRef;
				const jbyte				*uuidBytes;
				UInt8					length, k;
				CFMutableStringRef		stringUUID;
				jstring					jStringUUID;
				UniChar					*charBuf;
				CFRange					range;
				jclass					jUUIDClass;
				jmethodID				jUUIDConstructor;
				jobject					jUUID;

				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V"));
				stringUUID = CFStringCreateMutable (NULL, 500);
				aUUIDRef = IOBluetoothSDPDataElementGetUUIDValue(dataElement);
				uuidBytes = IOBluetoothSDPUUIDGetBytes(aUUIDRef);
				length =  IOBluetoothSDPUUIDGetLength(aUUIDRef);
				for(k=0;k<length;k++) {
					CFStringAppendFormat(stringUUID, NULL, CFSTR("%02x"), uuidBytes[k]);
				}
				range.location = 0;
				range.length = CFStringGetLength(stringUUID);
				charBuf = malloc(sizeof(UniChar) *range.length);
				CFStringGetCharacters(stringUUID, range, charBuf);
				jStringUUID = JAVA_ENV_CHECK(NewString(env, (jchar*)charBuf, (jsize)range.length));
				free(charBuf);
				jUUIDClass = JAVA_ENV_CHECK(FindClass(env, "javax/bluetooth/UUID"));
				jUUIDConstructor = JAVA_ENV_CHECK(GetMethodID(env, jUUIDClass, "<init>", "(Ljava/lang/String;Z)V"));
				jUUID = JAVA_ENV_CHECK(NewObject(env, jUUIDClass, jUUIDConstructor, jStringUUID, (range.length == 8) ? 0:1));

				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, 0x18, jUUID));
				CFRelease(stringUUID);
			}
			break;
		case kBluetoothSDPDataElementTypeURL:
			isURL = 1;
		case kBluetoothSDPDataElementTypeString:
			{
				CFStringRef				aString;
				jstring					jStringRef;
				UniChar					*charBuf;
				CFRange					range;

				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(ILjava/lang/Object;)V"));
				aString = IOBluetoothSDPDataElementGetStringValue(dataElement);
				range.location = 0;
				range.length = CFStringGetLength(aString);
				charBuf = malloc(sizeof(UniChar)*range.length);
				CFStringGetCharacters(aString, range, charBuf);
				jStringRef = JAVA_ENV_CHECK(NewString(env, (jchar*)charBuf, (jsize)range.length));
				free(charBuf);
				CFRelease(aString);
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, isURL ? 0x40: 0x20, jStringRef));
			}
			break;
		case kBluetoothSDPDataElementTypeBoolean:
			{
				jboolean			aBool;
				CFNumberRef			aNumber;

				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(Z)V"));
				aNumber = IOBluetoothSDPDataElementGetNumberValue(dataElement);
				CFNumberGetValue(aNumber, kCFNumberCharType, &aBool);
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, aBool));
			}
			break;
		case kBluetoothSDPDataElementTypeDataElementSequence:
			isSequence = 1;
		case kBluetoothSDPDataElementTypeDataElementAlternative:
			{
				CFArrayRef			anArray;
				CFIndex				m, count;
				jmethodID			addElement;

				addElement = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "addElement", "(Ljavax/bluetooth/DataElement;)V"));
				constructor = JAVA_ENV_CHECK(GetMethodID(env, dataElementClass, "<init>", "(I)V"));
				jDataElement = JAVA_ENV_CHECK(NewObject(env, dataElementClass, constructor, isSequence ? 0x30 : 0x38));
				anArray = IOBluetoothSDPDataElementGetArrayValue(dataElement);
				count = CFArrayGetCount(anArray);
				for(m=0;m<count;m++) {
					const IOBluetoothSDPDataElementRef	anItem = (IOBluetoothSDPDataElementRef)CFArrayGetValueAtIndex (anArray, m);
					jobject								ajElement;

					ajElement = getjDataElement(env, anItem);
					JAVA_ENV_CHECK(CallVoidMethod(env, jDataElement, addElement, ajElement));
				}
			}
			break;
		default:
			debug1("getjDataElement: Unknown data element type encounterd! %i", typeDescrip);
			jDataElement = NULL;
			break;

		}
	return jDataElement;

}