/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 *  @version $Id$
 */
package com.intel.bluetooth;

/**
 * Connection to native test functions.
 * 
 * This functions are only executed during UnitTests.
 * 
 * @author vlads
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

	static native void testDebug(String message);

	static native byte[] testOsXDataElementConversion(int testType, int type, long ldata, byte[] bdata);

	static native void testOsXRunnableLoop(int testType, int runLoops);

	static native boolean testWIDCOMMConstants();
}
