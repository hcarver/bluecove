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
#include "com_intel_bluetooth_NativeTestInterfaces.h"

#import <IOBluetooth/objc/IOBluetoothSDPDataElement.h>
#import <IOBluetooth/objc/IOBluetoothSDPUUID.h>
#import <Foundation/Foundation.h>

#define CPP_FILE "OSXStackTest.mm"

#define TEST_DEBUG true

void tdebug(const char *fmt, ...) {
	va_list ap;
	va_start(ap, fmt);
	if (TEST_DEBUG) {
	    fprintf(stderr, "NATIVE:");
        vfprintf(stderr, fmt, ap);
        fprintf(stderr, "\n");
        fflush(stderr);
    }
    va_end(ap);
}

IOBluetoothSDPDataElementRef createTestDataElementSimple(jint type, jlong ldata, jbyte *bytes, int inBytesLen) {
    BluetoothSDPDataElementTypeDescriptor newType;
    BluetoothSDPDataElementSizeDescriptor newSizeDescriptor;
    UInt32 newSize;
    NSObject* newValue;
    switch (type) {
    case DATA_ELEMENT_TYPE_U_INT_1:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 0;
        newSize = 1;
        newValue = [NSNumber numberWithUnsignedInt:((unsigned int)ldata)];
        break;
    case DATA_ELEMENT_TYPE_U_INT_2:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 1;
        newSize = 2;
        newValue = [NSNumber numberWithUnsignedInt:((unsigned int)ldata)];
        break;
    case DATA_ELEMENT_TYPE_U_INT_4:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 2;
        newSize = 4;
        newValue = [NSNumber numberWithUnsignedLong:((unsigned long)ldata)];
        break;
    case DATA_ELEMENT_TYPE_INT_1:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        newSizeDescriptor = 0;
        newSize = 1;
        newValue = [NSNumber numberWithInt:((int)ldata)];
        break;
    case DATA_ELEMENT_TYPE_INT_2:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        newSizeDescriptor = 1;
        newSize = 2;
        newValue = [NSNumber numberWithInt:((int)ldata)];
        break;
    case DATA_ELEMENT_TYPE_INT_4:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        newSizeDescriptor = 2;
        newSize = 4;
        newValue = [NSNumber numberWithLong:ldata];
        break;
    case DATA_ELEMENT_TYPE_INT_8:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        newSizeDescriptor = 3;
        newSize = 8;
        newValue = [NSNumber numberWithLongLong:ldata];
        break;
    case DATA_ELEMENT_TYPE_U_INT_8:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 3;
        newSize = 8;
        newValue = [NSData dataWithBytes:bytes length:8];
        break;
    case DATA_ELEMENT_TYPE_U_INT_16:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 4;
        newSize = 16;
        newValue = [NSData dataWithBytes:bytes length:16];
        break;
    case DATA_ELEMENT_TYPE_INT_16:
        newType = kBluetoothSDPDataElementTypeSignedInt;
        newSizeDescriptor = 4;
        newSize = 16;
        newValue = [NSData dataWithBytes:bytes length:16];
        break;

    case DATA_ELEMENT_TYPE_NULL:
        newType = kBluetoothSDPDataElementTypeNil;
        newSizeDescriptor = 0;
        newSize = 0;
        newValue = NULL;
        break;
    case DATA_ELEMENT_TYPE_BOOL:
        newType = kBluetoothSDPDataElementTypeBoolean;
        newSizeDescriptor = 0;
        newSize = 1;
        newValue = [NSNumber numberWithInt:((int)ldata)];
        break;
    case DATA_ELEMENT_TYPE_UUID:
        newType = kBluetoothSDPDataElementTypeUUID;
        newSizeDescriptor = 4;
        newSize = inBytesLen;
        newValue = [IOBluetoothSDPUUID uuidWithBytes:(const void *)bytes length:(unsigned)inBytesLen];
        break;
    case DATA_ELEMENT_TYPE_STRING:
        newType = kBluetoothSDPDataElementTypeString;
        tdebug("createTestDataElementSimple string %i", inBytesLen);
        newValue = [[NSString alloc] initWithBytes:(const void *)bytes length:(unsigned long)inBytesLen encoding:NSUTF8StringEncoding];
        IOBluetoothSDPDataElement* ds = [IOBluetoothSDPDataElement withElementValue:newValue];
        return [ds getSDPDataElementRef];
    case DATA_ELEMENT_TYPE_URL:
        newType = kBluetoothSDPDataElementTypeURL;
        tdebug("createTestDataElementSimple URL %i", inBytesLen);
        // UTF8 is just encoding for test interface.
        NSString* newStrValue = [[NSString alloc] initWithBytes:(const void *)bytes length:(unsigned long)inBytesLen encoding:NSNonLossyASCIIStringEncoding];
        newValue = newStrValue;
        newSize = [newStrValue length];
        if (newSize < 0x100) {
		    newSizeDescriptor  = 5;
		} else if (newSize < 0x10000) {
		    newSizeDescriptor  = 6;
		} else {
		    newSizeDescriptor  = 7;
		}
		//newSize + 1;
        break;
    default:
        return NULL;
    }
    //[newValue retain];
    IOBluetoothSDPDataElement* de = [IOBluetoothSDPDataElement withType:newType sizeDescriptor:newSizeDescriptor size:newSize value:newValue];
    //[de retain];
    tdebug("createTestDataElementSimple %i %i %i", newType, newSizeDescriptor, newSize);
    return [de getSDPDataElementRef];
}

IOBluetoothSDPDataElementRef createTestDataElement(jint testType, jint type, jlong ldata, jbyte *bytes, int inBytesLen) {
    switch (testType) {
    case 0:
            return createTestDataElementSimple(type, ldata, bytes, inBytesLen);
        default:
            return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testOsXDataElementConversion
  (JNIEnv* env, jclass, jint testType, jint type, jlong ldata, jbyteArray bdata) {

    OSXJNIHelper allocHelper;
    tdebug("testOsXDataElementConversion %i %i [%lli]", testType, type, ldata);

	jbyte *inBytes = NULL;
	int inBytesLen = 0;
	if (bdata != NULL) {
	    inBytes = env->GetByteArrayElements(bdata, 0);
	    inBytesLen = env->GetArrayLength(bdata);
    }

    const IOBluetoothSDPDataElementRef dataElement = createTestDataElement(testType, type, ldata, inBytes, inBytesLen);

    tdebug("dataElement created");

    SDPOutputStream os;
    int   dataLen = 0;
    UInt8 data[DATA_BLOB_MAX];

    BOOL written = os.writeElement(dataElement);

    if (bdata != NULL) {
        //env->ReleaseByteArrayElements(bdata, inBytes, 0);
    }

    if (!written) {
        return NULL;
    } else {
        tdebug("dataElement written");
        os.getBytes(DATA_BLOB_MAX, &dataLen, data);
    }

    // construct byte array to hold blob
	jbyteArray result = env->NewByteArray(dataLen);
    jbyte *bytes = env->GetByteArrayElements(result, 0);
	memcpy(bytes, data, dataLen);
	env->ReleaseByteArrayElements(result, bytes, 0);
	return result;
}