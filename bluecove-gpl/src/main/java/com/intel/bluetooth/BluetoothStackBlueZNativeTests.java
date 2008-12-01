/**
 *  BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
 *  @author vlads
 *  @version $Id: BluetoothStackBlueZ.java 1562 2008-01-16 18:31:25Z skarzhevskyy $
 */
package com.intel.bluetooth;

/**
 *
 *
 */
public class BluetoothStackBlueZNativeTests {

	static native void testThrowException(int type) throws Exception;

	static native void testDebug(int argc, String message);

	static native byte[] testServiceRecordConvert(byte[] record);
}
