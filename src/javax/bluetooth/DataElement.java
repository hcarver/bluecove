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

import java.util.Enumeration;
import java.util.Vector;

public class DataElement {
	/*
	 * Defines data of type NULL. The value for data type DataElement.NULL is
	 * implicit, i.e., there is no representation of it. Accordingly there is no
	 * method to retrieve it, and attempts to retrieve the value will throw an
	 * exception. The value of NULL is 0x00 (0).
	 */

	public static final int NULL = 0;

	/*
	 * Defines an unsigned integer of size one byte. The value of the constant
	 * U_INT_1 is 0x08 (8).
	 */

	public static final int U_INT_1 = 8;

	/*
	 * Defines an unsigned integer of size two bytes. The value of the constant
	 * U_INT_2 is 0x09 (9).
	 */

	public static final int U_INT_2 = 9;

	/*
	 * Defines an unsigned integer of size four bytes. The value of the constant
	 * U_INT_4 is 0x0A (10).
	 */

	public static final int U_INT_4 = 10;

	/*
	 * Defines an unsigned integer of size eight bytes. The value of the
	 * constant U_INT_8 is 0x0B (11).
	 */

	public static final int U_INT_8 = 11;

	/*
	 * Defines an unsigned integer of size sixteen bytes. The value of the
	 * constant U_INT_16 is 0x0C (12).
	 */

	public static final int U_INT_16 = 12;

	/*
	 * Defines a signed integer of size one byte. The value of the constant
	 * INT_1 is 0x10 (16).
	 */

	public static final int INT_1 = 16;

	/*
	 * Defines a signed integer of size two bytes. The value of the constant
	 * INT_2 is 0x11 (17).
	 */

	public static final int INT_2 = 17;

	/*
	 * Defines a signed integer of size four bytes. The value of the constant
	 * INT_4 is 0x12 (18).
	 */

	public static final int INT_4 = 18;

	/*
	 * Defines a signed integer of size eight bytes. The value of the constant
	 * INT_8 is 0x13 (19).
	 */

	public static final int INT_8 = 19;

	/*
	 * Defines a signed integer of size sixteen bytes. The value of the constant
	 * INT_16 is 0x14 (20).
	 */

	public static final int INT_16 = 20;

	/*
	 * Defines data of type URL. The value of the constant URL is 0x40 (64).
	 */

	public static final int URL = 64;

	/*
	 * Defines data of type UUID. The value of the constant UUID is 0x18 (24).
	 */

	public static final int UUID = 24;

	/*
	 * Defines data of type BOOL. The value of the constant BOOL is 0x28 (40).
	 */

	public static final int BOOL = 40;

	/*
	 * Defines data of type STRING. The value of the constant STRING is 0x20
	 * (32).
	 */

	public static final int STRING = 32;

	/*
	 * Defines data of type DATSEQ. The service attribute value whose data has
	 * this type must consider all the elements of the list, i.e. the value is
	 * the whole set and not a subset. The elements of the set can be of any
	 * type defined in this class, including DATSEQ. The value of the constant
	 * DATSEQ is 0x30 (48).
	 */

	public static final int DATSEQ = 48;

	/*
	 * Defines data of type DATALT. The service attribute value whose data has
	 * this type must consider only one of the elements of the set, i.e., the
	 * value is the not the whole set but only one element of the set. The user
	 * is free to choose any one element. The elements of the set can be of any
	 * type defined in this class, including DATALT. The value of the constant
	 * DATALT is 0x38 (56).
	 */

	public static final int DATALT = 56;

	private Object value;

	private int valueType;

	/*
	 * Creates a DataElement of type NULL, DATALT, or DATSEQ. Parameters:
	 * valueType - the type of DataElement to create: NULL, DATALT, or DATSEQ
	 * Throws: IllegalArgumentException - if valueType is not NULL, DATALT, or
	 * DATSEQ See Also: NULL, DATALT, DATSEQ
	 */

	public DataElement(int valueType) {
		switch (valueType) {
		case NULL:
			value = null;
			break;
		case DATALT:
		case DATSEQ:
			value = new Vector();
			break;
		default:
			throw new IllegalArgumentException();
		}

		this.valueType = valueType;
	}

	/*
	 * Creates a DataElement whose data type is BOOL and whose value is equal to
	 * bool Parameters: bool - the value of the DataElement of type BOOL. See
	 * Also: BOOL
	 */

	public DataElement(boolean bool) {
		value = new Boolean(bool);
		valueType = BOOL;
	}

	/*
	 * Creates a DataElement that encapsulates an integer value of size U_INT_1,
	 * U_INT_2, U_INT_4, INT_1, INT_2, INT_4, and INT_8. The legal values for
	 * the valueType and the corresponding attribute values are: Value Type
	 * Value Range U_INT_1 [0, 2^8-1] U_INT_2 [0, 2^16-1] U_INT_4 [0, 2^32-1]
	 * INT_1 [-2^7, 2^7-1] INT_2 [-2^15, 2^15-1] INT_4 [-2^31, 2^31-1] INT_8
	 * [-2^63, 2^63-1] All other pairings are illegal and will cause an
	 * IllegalArgumentException to be thrown. Parameters: valueType - the data
	 * type of the object that is being created; must be one of the following:
	 * U_INT_1, U_INT_2, U_INT_4, INT_1, INT_2, INT_4, or INT_8 value - the
	 * value of the object being created; must be in the range specified for the
	 * given valueType Throws: IllegalArgumentException - if the valueType is
	 * not valid or the value for the given legal valueType is outside the valid
	 * range See Also: U_INT_1, U_INT_2, U_INT_4, INT_1, INT_2, INT_4, INT_8
	 */

	public DataElement(int valueType, long value) {
		switch (valueType) {
		case U_INT_1:
			if (value < 0 || value > 0xff)
				throw new IllegalArgumentException();
			break;
		case U_INT_2:
			if (value < 0 || value > 0xffff)
				throw new IllegalArgumentException();
			break;
		case U_INT_4:
			if (value < 0 || value > 0xffffffffl)
				throw new IllegalArgumentException();
			break;
		case INT_1:
			if (value < -0x80 || value > 0x7f)
				throw new IllegalArgumentException();
			break;
		case INT_2:
			if (value < -0x8000 || value > 0x7fff)
				throw new IllegalArgumentException();
			break;
		case INT_4:
			if (value < -0x80000000 || value > 0x7fffffff)
				throw new IllegalArgumentException();
			break;
		case INT_8:
			break;
		default:
			throw new IllegalArgumentException();
		}

		this.value = new Long(value);
		this.valueType = valueType;
	}

	/*
	 * Creates a DataElement whose data type is given by valueType and whose
	 * value is specified by the argument value. The legal values for the
	 * valueType and the corresponding attribute values are: Value Type Java
	 * Type / Value Range URL java.lang.String UUID javax.bluetooth.UUID STRING
	 * java.lang.String INT_16 [-2127, 2127-1] as a byte array whose length must
	 * be 16 U_INT_8 [0, 264-1] as a byte array whose length must be 8 U_INT_16
	 * [0, 2128-1] as a byte array whose length must be 16 All other pairings
	 * are illegal and would cause an IllegalArgumentException exception.
	 * Parameters: valueType - the data type of the object that is being
	 * created; must be one of the following: URL, UUID, STRING, INT_16,
	 * U_INT_8, or U_INT_16 value - the value for the DataElement being created
	 * of type valueType Throws: IllegalArgumentException - if the value is not
	 * of the valueType type or is not in the range specified or is null See
	 * Also: URL, UUID, STRING, U_INT_8, INT_16, U_INT_16
	 */

	public DataElement(int valueType, Object value) {
		if (value == null)
			throw new IllegalArgumentException();

		switch (valueType) {
		case URL:
		case STRING:
			if (!(value instanceof String))
				throw new IllegalArgumentException();
			break;
		case UUID:
			if (!(value instanceof UUID))
				throw new IllegalArgumentException();
			break;
		case U_INT_8:
			if (!(value instanceof byte[]) || ((byte[]) value).length != 8)
				throw new IllegalArgumentException();
			break;
		case U_INT_16:
		case INT_16:
			if (!(value instanceof byte[]) || ((byte[]) value).length != 16)
				throw new IllegalArgumentException();
			break;
		default:
			throw new IllegalArgumentException();
		}

		this.value = value;
		this.valueType = valueType;
	}

	/*
	 * Adds a DataElement to this DATALT or DATSEQ DataElement object. The elem
	 * will be added at the end of the list. The elem can be of any DataElement
	 * type, i.e., URL, NULL, BOOL, UUID, STRING, DATSEQ, DATALT, and the
	 * various signed and unsigned integer types. The same object may be added
	 * twice. If the object is successfully added the size of the DataElement is
	 * increased by one. Parameters: elem - the DataElement object to add
	 * Throws: ClassCastException - if the method is invoked on a DataElement
	 * whose type is not DATALT or DATSEQ NullPointerException - if elem is null
	 */

	public void addElement(DataElement elem) {
		if (elem == null)
			throw new NullPointerException();

		switch (valueType) {
		case DATALT:
		case DATSEQ:
			((Vector) value).addElement(elem);
			break;
		default:
			throw new ClassCastException();
		}
	}

	/*
	 * Inserts a DataElement at the specified location. This method can be
	 * invoked only on a DATALT or DATSEQ DataElement. elem can be of any
	 * DataElement type, i.e., URL, NULL, BOOL, UUID, STRING, DATSEQ, DATALT,
	 * and the various signed and unsigned integers. The same object may be
	 * added twice. If the object is successfully added the size will be
	 * increased by one. Each element with an index greater than or equal to the
	 * specified index is shifted upward to have an index one greater than the
	 * value it had previously. The index must be greater than or equal to 0 and
	 * less than or equal to the current size. Therefore, DATALT and DATSEQ are
	 * zero-based objects.
	 * 
	 * Parameters: elem - the DataElement object to add index - the location at
	 * which to add the DataElement Throws: ClassCastException - if the method
	 * is invoked on an instance of DataElement whose type is not DATALT or
	 * DATSEQ IndexOutOfBoundsException - if index is negative or greater than
	 * the size of the DATALT or DATSEQ NullPointerException - if elem is null
	 */

	public void insertElementAt(DataElement elem, int index) {
		if (elem == null)
			throw new NullPointerException();

		switch (valueType) {
		case DATALT:
		case DATSEQ:
			((Vector) value).insertElementAt(elem, index);
			break;
		default:
			throw new ClassCastException();
		}
	}

	/*
	 * Returns the number of DataElements that are present in this DATALT or
	 * DATSEQ object. It is possible that the number of elements is equal to
	 * zero. Returns: the number of elements in this DATALT or DATSEQ Throws:
	 * ClassCastException - if this object is not of type DATALT or DATSEQ
	 */

	public int getSize() {
		switch (valueType) {
		case DATALT:
		case DATSEQ:
			return ((Vector) value).size();
		default:
			throw new ClassCastException();
		}
	}

	/*
	 * Removes the first occurrence of the DataElement from this object. elem
	 * may be of any type, i.e., URL, NULL, BOOL, UUID, STRING, DATSEQ, DATALT,
	 * or the variously sized signed and unsigned integers. Only the first
	 * object in the list that is equal to elem will be removed. Other objects,
	 * if present, are not removed. Since this class doesn't override the
	 * equals() method of the Object class, the remove method compares only the
	 * references of objects. If elem is successfully removed the size of this
	 * DataElement is decreased by one. Each DataElement in the DATALT or DATSEQ
	 * with an index greater than the index of elem is shifted downward to have
	 * an index one smaller than the value it had previously. Parameters: elem -
	 * the DataElement to be removed Returns: true if the input value was found
	 * and removed; else false Throws: ClassCastException - if this object is
	 * not of type DATALT or DATSEQ NullPointerException - if elem is null
	 */

	public boolean removeElement(DataElement elem) {
		if (elem == null)
			throw new NullPointerException();

		switch (valueType) {
		case DATALT:
		case DATSEQ:
			return ((Vector) value).removeElement(elem);
		default:
			throw new ClassCastException();
		}
	}

	/*
	 * Returns the data type of the object this DataElement represents. Returns:
	 * the data type of this DataElement object; the legal return values are:
	 * URL, NULL, BOOL, UUID, STRING, DATSEQ, DATALT, U_INT_1, U_INT_2, U_INT_4,
	 * U_INT_8, U_INT_16, INT_1, INT_2, INT_4, INT_8, or INT_16
	 */

	public int getDataType() {
		return valueType;
	}

	/*
	 * Returns the value of the DataElement if it can be represented as a long.
	 * The data type of the object must be U_INT_1, U_INT_2, U_INT_4, INT_1,
	 * INT_2, INT_4, or INT_8. Returns: the value of the DataElement as a long
	 * Throws: ClassCastException - if the data type of the object is not
	 * U_INT_1, U_INT_2, U_INT_4, INT_1, INT_2, INT_4, or INT_8
	 */

	public long getLong() {
		switch (valueType) {
		case U_INT_1:
		case U_INT_2:
		case U_INT_4:
		case INT_1:
		case INT_2:
		case INT_4:
		case INT_8:
			return ((Long) value).longValue();
		default:
			throw new ClassCastException();
		}
	}

	/*
	 * Returns the value of the DataElement if it is represented as a boolean.
	 * Returns: the boolean value of this DataElement object Throws:
	 * ClassCastException - if the data type of this object is not of type BOOL
	 */

	public boolean getBoolean() {
		if (valueType == BOOL)
			return ((Boolean) value).booleanValue();
		else
			throw new ClassCastException();
	}

	/*
	 * Returns the value of this DataElement as an Object. This method returns
	 * the appropriate Java object for the following data types: URL, UUID,
	 * STRING, DATSEQ, DATALT, U_INT_8, U_INT_16, and INT_16. Modifying the
	 * returned Object will not change this DataElement. The following are the
	 * legal pairs of data type and Java object type being returned. DataElement
	 * Data Type Java Data Type URL java.lang.String UUID javax.bluetooth.UUID
	 * STRING java.lang.String DATSEQ java.util.Enumeration DATALT
	 * java.util.Enumeration U_INT_8 byte[] of length 8 U_INT_16 byte[] of
	 * length 16 INT_16 byte[] of length 16
	 * 
	 * Returns: the value of this object Throws: ClassCastException - if the
	 * object is not a URL, UUID, STRING, DATSEQ, DATALT, U_INT_8, U_INT_16, or
	 * INT_16
	 */

	public Object getValue() {
		switch (valueType) {
		case URL:
		case STRING:
		case UUID:
		case U_INT_8:
		case U_INT_16:
		case INT_16:
			return value;
		case DATSEQ:
		case DATALT:
			return ((Vector) value).elements();
		default:
			throw new ClassCastException();
		}
	}

	/**
	 * @depreacted Use ((Object)dataElement).toString() if you want your application to run in MDIP profile
	 */
	public String toString() {
		switch (valueType) {
		case U_INT_1:
		case U_INT_2:
		case U_INT_4:
		case INT_1:
		case INT_2:
		case INT_4:
		case INT_8:
			return "0x" + Long.toHexString(((Long) value).longValue());
		case BOOL:
		case URL:
		case STRING:
		case UUID:
			return value.toString();
		case U_INT_8:
		case U_INT_16:
		case INT_16: {
			byte[] b = (byte[]) value;

			StringBuffer buf = new StringBuffer();

			for (int i = 0; i < b.length; i++) {
				buf.append(Integer.toHexString(b[i] >> 4 & 0xf));
				buf.append(Integer.toHexString(b[i] & 0xf));
			}

			return buf.toString();
		}
		case DATSEQ: {
			StringBuffer buf = new StringBuffer("DATSEQ {\n");

			for (Enumeration e = ((Vector) value).elements(); e
					.hasMoreElements();) {
				buf.append(e.nextElement());
				buf.append("\n");
			}

			buf.append("}");

			return buf.toString();
		}
		case DATALT: {
			StringBuffer buf = new StringBuffer("DATALT {\n");

			for (Enumeration e = ((Vector) value).elements(); e
					.hasMoreElements();) {
				buf.append(e.nextElement());
				buf.append("\n");
			}

			buf.append("}");

			return buf.toString();
		}
		default:
			return "???";
		}
	}
}
