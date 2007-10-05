/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
public class NativeTestInterfaces {

	public static boolean loadDllMS() {
		return NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_MS);
	}
	
	public static boolean loadDllWIDCOMM() {
		return NativeLibLoader.isAvailable(BlueCoveImpl.NATIVE_LIB_WIDCOMM);
	}
	
	public static native byte[] testUUIDConversion(byte[] uuidValue);

	public static native long testReceiveBufferCreate(int size);

	public static native void testReceiveBufferClose(long bufferHandler);

	public static native int testReceiveBufferWrite(long bufferHandler, byte[] send);

	public static native int testReceiveBufferRead(long bufferHandler, byte[] rcv);

	public static native int testReceiveBufferRead(long bufferHandler);

	public static native int testReceiveBufferSkip(long bufferHandler, int size);

	public static native int testReceiveBufferAvailable(long bufferHandler);

	public static native boolean testReceiveBufferIsOverflown(long bufferHandler);

	public static native boolean testReceiveBufferIsCorrupted(long bufferHandler);

	public static native void testThrowException(int type) throws Exception;

	public static native void testDebug(String message);

}
