/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
 * @version $Id: BlueCoveBlueZ_Tests.c 1721 2008-01-31 01:12:08Z skarzhevskyy $
 */
#define CPP__FILE "BlueCoveBlueZ_Tests.c"

#include "BlueCoveBlueZ.h"
#include "com_intel_bluetooth_BluetoothStackBlueZDBusNativeTests.h"

#include <bluetooth/sdp_lib.h>

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBusNativeTests_testThrowException
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

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBusNativeTests_testDebug
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
