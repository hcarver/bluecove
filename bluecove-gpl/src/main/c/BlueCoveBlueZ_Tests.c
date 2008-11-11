/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @version $Id$
 */
#define CPP__FILE "BlueCoveBlueZ_Tests.c"

#include "BlueCoveBlueZ.h"
#include "com_intel_bluetooth_BluetoothStackBlueZNativeTests.h"
#include <dlfcn.h>
#include <bluetooth/sdp_lib.h>

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZNativeTests_testThrowException
(JNIEnv *env, jclass peer, jint extype) {
	switch (extype) {
		case 0: throwException(env, "java/lang/Exception", "0"); break;
		case 1: throwException(env, "java/lang/Exception", "1[%s]", "str"); break;
		case 2: throwIOException(env, "2"); break;
		case 3: throwIOException(env, "3[%s]", "str"); break;
	    case 4: throwBluetoothStateException(env, "4"); break;
		case 5: throwBluetoothStateException(env, "5[%s]", "str"); break;
		case 6: throwRuntimeException(env, "6"); break;
		case 7: throwBluetoothConnectionException(env, 1, "7"); break;
		case 8: throwBluetoothConnectionException(env, 2, "8[%s]", "str"); break;

		case 22:
			// Throw Exception two times in a row. Second Exception ignored
			throwException(env, "java/lang/Exception", "22.1");
			throwIOException(env, "22.2");
			break;
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZNativeTests_testDebug
(JNIEnv *env, jclass peer, jint argc, jstring message) {
	if ((argc == 0) || (message == NULL)) {
	    debug("message");
	    return;
	}
	const char *c = (*env)->GetStringUTFChars(env, message, 0);
	switch (argc) {
		case 1: debug("message[%s]", c); break;
		case 2: debug("message[%s],[%s]", c, c); break;
		case 3: debug("message[%s],[%s],[%i]", c, c, argc); break;
	}
	(*env)->ReleaseStringUTFChars(env, message, c);
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZNativeTests_testServiceRecordConvert
(JNIEnv *env, jclass peer, jbyteArray record) {
    int length = (*env)->GetArrayLength(env, record);
    jbyte *bytes = (*env)->GetByteArrayElements(env, record, 0);
    int length_scanned = length;
    sdp_record_t *rec = bluecove_sdp_extract_pdu(env, bytes, length, &length_scanned);
    if(!rec) {
        return NULL;
    }

    debug("pdu scanned %i -> %i", length, length_scanned);
    if (rec == NULL) {
        throwServiceRegistrationException(env, "Can not convert SDP record. [%d] %s", errno, strerror(errno));
        (*env)->ReleaseByteArrayElements(env, record, bytes, 0);
        return NULL;
    }
    debugServiceRecord(env, rec);

    sdp_buf_t pdu;
    sdp_gen_record_pdu(rec, &pdu);
    debug("pdu.data_size %i -> %i", length, pdu.data_size);

    // construct byte array to hold pdu
    jbyteArray result = (*env)->NewByteArray(env, pdu.data_size);
    jbyte *result_bytes = (*env)->GetByteArrayElements(env, result, 0);
    memcpy(result_bytes, pdu.data, pdu.data_size);
    (*env)->ReleaseByteArrayElements(env, result, result_bytes, 0);

    free(pdu.data);

    (*env)->ReleaseByteArrayElements(env, record, bytes, 0);
    return result;
}