/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package javax.bluetooth;

import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.Utils;

public class UUID {
	
	private byte[] uuidValue;

	/*
	 * Creates a UUID object from long value uuidValue. A UUID is defined as an
	 * unsigned integer whose value can range from [0 to 2^128-1]. However, this
	 * constructor allows only those values that are in the range of [0 to 2^32
	 * -1]. Negative values and values in the range of [2^32, 2^63-1] are not
	 * allowed and will cause an IllegalArgumentException to be thrown.
	 * Parameters: uuidValue - the 16-bit or 32-bit value of the UUID Throws:
	 * IllegalArgumentException - if uuidValue is not in the range [0, 2^32-1]
	 */

	public UUID(long longValue) {
		this(Long.toHexString(longValue), true);
	}

	/*
	 * Creates a UUID object from the string provided. The characters in the
	 * string must be from the hexadecimal set [0-9, a-f, A-F]. It is important
	 * to note that the prefix "0x" generally used for hex representation of
	 * numbers is not allowed. If the string does not have characters from the
	 * hexadecimal set, an exception will be thrown. The string length has to be
	 * positive and less than or equal to 32. A string length that exceeds 32 is
	 * illegal and will cause an exception. Finally, a null input is also
	 * considered illegal and causes an exception. If shortUUID is true,
	 * uuidValue represents a 16-bit or 32-bit UUID. If uuidValue is in the
	 * range 0x0000 to 0xFFFF then this constructor will create a 16-bit UUID.
	 * If uuidValue is in the range 0x000010000 to 0xFFFFFFFF, then this
	 * constructor will create a 32-bit UUID. Therefore, uuidValue may only be 8
	 * characters long.
	 * 
	 * On the other hand, if shortUUID is false, then uuidValue represents a
	 * 128-bit UUID. Therefore, uuidValue may only be 32 character long
	 * 
	 * Parameters: uuidValue - the string representation of a 16-bit, 32-bit or
	 * 128-bit UUID shortUUID - indicates the size of the UUID to be
	 * constructed; true is used to indicate short UUIDs, i.e. either 16-bit or
	 * 32-bit; false indicates an 128-bit UUID Throws: NumberFormatException -
	 * if uuidValue has characters that are not defined in the hexadecimal set
	 * [0-9, a-f, A-F] IllegalArgumentException - if uuidValue length is zero;
	 * if shortUUID is true and uuidValue's length is greater than 8; if
	 * shortUUID is false and uuidValue's length is greater than 32
	 * NullPointerException - if uuidValue is null
	 */

	public UUID(String stringValue, boolean shortUUID) {
		int length = stringValue.length();
		if (shortUUID) {
			if (length < 1 || length > 8) {
				throw new IllegalArgumentException();
			}
			uuidValue = Utils.UUIDToByteArray("00000000".substring(length) + stringValue + BluetoothConsts.SHORT_UUID_BASE);
		} else {
			if (length < 1 || length > 32) {
				throw new IllegalArgumentException();
			}
			uuidValue = Utils.UUIDToByteArray("00000000000000000000000000000000".substring(length) + stringValue);
		}
	}

	/*
	 * Returns the string representation of the 128-bit UUID object. The string
	 * being returned represents a UUID that contains characters from the
	 * hexadecimal set, [0-9, A-F]. It does not include the prefix "0x" that is
	 * generally used for hex representation of numbers. The return value will
	 * never be null. Overrides: toString in class java.lang.Object Returns: the
	 * string representation of the UUID
	 */

	public String toString() {
		return Utils.UUIDByteArrayToString(uuidValue);
	}

	/*
	 * Determines if two UUIDs are equal. They are equal if their 128 bit values
	 * are the same. This method will return false if value is null or is not a
	 * UUID object. Overrides: equals in class java.lang.Object Parameters:
	 * value - the object to compare to Returns: true if the 128 bit values of
	 * the two objects are equal, otherwise false
	 */

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof UUID)) {
			return false;
		}

		for (int i = 0; i < 16; i++) {
			if (uuidValue[i] != ((UUID) obj).uuidValue[i]) {
				return false;
			}
		}

		return true;
	}

	/*
	 * Computes the hash code for this object. This method retains the same
	 * semantic contract as defined in the class java.lang.Object while
	 * overriding the implementation. Overrides: hashCode in class
	 * java.lang.Object Returns: the hash code for this object
	 */

	public int hashCode() {
		return uuidValue[12] << 24 & 0xff000000 | uuidValue[13] << 16
				& 0x00ff0000 | uuidValue[14] << 8 & 0x0000ff00 | uuidValue[15]
				& 0x000000ff;
	}
}