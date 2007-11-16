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

#define CPP_FILE "OSXStackTest.mm"

IOBluetoothSDPDataElementRef createTestDataElementSimple(jint type, jlong ldata, jbyte *bytes) {
    BluetoothSDPDataElementTypeDescriptor newType;
    BluetoothSDPDataElementSizeDescriptor newSizeDescriptor;
    UInt32 newSize;
    NSObject* newValue;

    switch (testType) {
    case DATA_ELEMENT_TYPE_U_INT_1:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 0;
        newSize = 1;
        newValue = CFNumberCreate(NULL, kCFNumberLongType, &ldata);
        break;
    case DATA_ELEMENT_TYPE_U_INT_2:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 0;
        newSize = 2;
        newValue = CFNumberCreate(NULL, kCFNumberLongType, &ldata);
        break;
    case DATA_ELEMENT_TYPE_U_INT_4:
        newType = kBluetoothSDPDataElementTypeUnsignedInt;
        newSizeDescriptor = 0;
        newSize = 4;
        newValue = CFNumberCreate(NULL, kCFNumberLongType, &ldata);
        break;
    default:
            return NULL;
    }
    IOBluetoothSDPDataElement de;
    de = [IOBluetoothSDPDataElement initWithType: ];
    return [de getSDPDataElementRef];
}

IOBluetoothSDPDataElementRef createTestDataElement(jint testType, jint type, jlong ldata, jbyte *bytes) {
    switch (testType) {
    case 0:
            return createTestDataElementSimple(testType, ldata, );
        default:
            return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_NativeTestInterfaces_testOsXDataElementConversion
  (JNIEnv* env, jclass, jint testType, jint type, jlong ldata, jbyteArray bdata) {

    OSXJNIHelper allocHelper;

	jbyte *bytes = NULL;
	if (bdata != NULL) {
	    bytes = env->GetByteArrayElements(bdata, 0);
    }

    const IOBluetoothSDPDataElementRef dataElement = createTestDataElement(testType, type, ldata, bytes);

    if (bdata != NULL) {
        env->ReleaseByteArrayElements(bdata, bytes, 0);
    }

    SDPOutputStream os;
    int   dataLen;
    UInt8 data[DATA_BLOB_MAX];

    if (!os.writeElement(dataElement)) {
        return NULL;
    } else {
        os.getBytes(DATA_BLOB_MAX, &dataLen, data);
    }

    // construct byte array to hold blob
	jbyteArray result = env->NewByteArray(dataLen);
    jbyte *bytes = env->GetByteArrayElements(result, 0);
	memcpy(bytes, runnable.data, dataLen);
	env->ReleaseByteArrayElements(result, bytes, 0);
	return result;
}