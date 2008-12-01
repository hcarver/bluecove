/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth;

/**
 * Connection to native test functions.
 *
 * This functions are only executed during UnitTests.
 */
class NativeTestInterfaces {

	static boolean loadDllMS() {
		return NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_MS);
	}

	static boolean loadDllWIDCOMM() {
		return NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_WIDCOMM);
	}

	static native byte[] testUUIDConversion(byte[] uuidValue);

	static native long testReceiveBufferCreate(int size);

	static native void testReceiveBufferClose(long bufferHandler);

	static native int testReceiveBufferWrite(long bufferHandler, byte[] send);

	static native int testReceiveBufferRead(long bufferHandler, byte[] rcv);

	static native int testReceiveBufferRead(long bufferHandler);

	static native int testReceiveBufferSkip(long bufferHandler, int size);

	static native int testReceiveBufferAvailable(long bufferHandler);

	static native boolean testReceiveBufferIsOverflown(long bufferHandler);

	static native boolean testReceiveBufferIsCorrupted(long bufferHandler);

	static native void testThrowException(int type) throws Exception;

	static native void testDebug(int argc, String message);

	static native byte[] testOsXDataElementConversion(int testType, int type, long ldata, byte[] bdata);

	static native void testOsXRunnableLoop(int testType, int runLoops);

	static native boolean testWIDCOMMConstants();
}
