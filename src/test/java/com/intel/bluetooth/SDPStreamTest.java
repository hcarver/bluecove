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
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.bluetooth.DataElement;

import junit.framework.TestCase;

/**
 * @author vlads
 * 
 */
public class SDPStreamTest extends TestCase {

	public static boolean equals(DataElement de1, DataElement de2) {
		if ((de1 == null) || (de2 == null)) {
			return false;
		}
		try {
			if (de1.getDataType() != de2.getDataType()) {
				return false;
			}
			switch (de1.getDataType()) {
			case DataElement.U_INT_1:
			case DataElement.U_INT_2:
			case DataElement.U_INT_4:
			case DataElement.INT_1:
			case DataElement.INT_2:
			case DataElement.INT_4:
			case DataElement.INT_8:
				return (de1.getLong() == de2.getLong());
			case DataElement.URL:
			case DataElement.STRING:
			case DataElement.UUID:
				return de1.getValue().equals(de2.getValue());
			case DataElement.INT_16:
			case DataElement.U_INT_8:
			case DataElement.U_INT_16:
				byte[] byteAray1 = (byte[]) de1.getValue();
				byte[] byteAray2 = (byte[]) de2.getValue();
				if (byteAray1.length != byteAray2.length) {
					return false;
				}
				for (int k = 0; k < byteAray1.length; k++) {
					if (byteAray1[k] != byteAray2[k]) {
						return false;
					}
				}
				return true;
			case DataElement.NULL:
				return true;
			case DataElement.BOOL:
				return (de1.getBoolean() == de2.getBoolean());
			case DataElement.DATSEQ:
			case DataElement.DATALT:
				Enumeration en1 = (Enumeration) de1.getValue();
				Enumeration en2 = (Enumeration) de2.getValue();
				for (; en1.hasMoreElements() && en2.hasMoreElements();) {
					DataElement d1 = (DataElement) en1.nextElement();
					DataElement d2 = (DataElement) en2.nextElement();
					if (!equals(d1, d2)) {
						return false;
					}
				}
				if (en1.hasMoreElements() || en2.hasMoreElements()) {
					return false;
				}
				return true;
			default:
				return false;
			}
		} catch (Throwable e) {
			DebugLog.error("DataElement equals", e);
			return false;
		}
	}

	private DataElement doubleCovert(DataElement element) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		(new SDPOutputStream(out)).writeElement(element);
		byte blob[] = out.toByteArray();
		return (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
	}

	private void validateConversion(DataElement element) throws IOException {
		DataElement elementConverted = doubleCovert(element);
		assertTrue(equals(element, elementConverted));
	}

	public void testInt() throws IOException {
		validateConversion(new DataElement(DataElement.U_INT_1, 1));
		validateConversion(new DataElement(DataElement.U_INT_2, 60));
		validateConversion(new DataElement(DataElement.U_INT_4, 77839));

		validateConversion(new DataElement(DataElement.INT_1, -7));
		validateConversion(new DataElement(DataElement.INT_2, -5));
		validateConversion(new DataElement(DataElement.INT_4, -25678));
		validateConversion(new DataElement(DataElement.INT_8, 998652497));

		validateConversion(new DataElement(DataElement.U_INT_1, 0));
		validateConversion(new DataElement(DataElement.U_INT_1, 0xBC));
		validateConversion(new DataElement(DataElement.U_INT_2, 0));
		validateConversion(new DataElement(DataElement.U_INT_2, 0xABCD));
		validateConversion(new DataElement(DataElement.U_INT_4, 0));
		validateConversion(new DataElement(DataElement.U_INT_4, 0xABCDEF40l));
		validateConversion(new DataElement(DataElement.INT_1, 0));
		validateConversion(new DataElement(DataElement.INT_1, 0x4C));
		validateConversion(new DataElement(DataElement.INT_1, -0x1E));
		validateConversion(new DataElement(DataElement.INT_2, 0));
		validateConversion(new DataElement(DataElement.INT_2, 0x5BCD));
		validateConversion(new DataElement(DataElement.INT_2, -0x7EFD));
		validateConversion(new DataElement(DataElement.INT_4, 0));
		validateConversion(new DataElement(DataElement.INT_4, 0x1BCDEF35l));
		validateConversion(new DataElement(DataElement.INT_4, -0x2BC7EF35l));
		validateConversion(new DataElement(DataElement.INT_8, 0));
		validateConversion(new DataElement(DataElement.INT_8, 0x3eC6EF355892EA8Cl));
		validateConversion(new DataElement(DataElement.INT_8, -0x7F893012AB39FB72l));

		validateConversion(new DataElement(DataElement.U_INT_8, new byte[] { 1, -2, 3, 4, -5, 6, 7, -8 }));
		validateConversion(new DataElement(DataElement.INT_16, new byte[] { 11, -22, 33, 44, -5, 6, 77, 88, 9, -10, 11,
				12, -13, 14, 15, 16 }));
		validateConversion(new DataElement(DataElement.U_INT_16, new byte[] { 21, -32, 43, 54, -65, 76, 87, 98, 11,
				-110, 111, 112, -113, 114, 115, 16 }));
	}
}
