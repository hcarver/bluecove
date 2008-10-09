/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
import java.io.IOException;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

/**
 * @author vlads
 *
 */
public class NativeSDPStreamOSXTest extends NativeOSXTestCase {

    private void validateConversion(DataElement elementExpect, int testType, int type, long ldata, byte[] bdata)
            throws IOException {
        byte[] blob = NativeTestInterfaces.testOsXDataElementConversion(testType, type, ldata, bdata);
        assertNotNull("NULL blob", blob);
        DataElement element = (new SDPInputStream(new ByteArrayInputStream(blob))).readElement();
        SDPStreamTest.assertEquals("", elementExpect, element);
    }

    /**
     * Tests for simple types
     */
    private void validateConversion(DataElement element) throws IOException {
        int type = element.getDataType();
        switch (type) {
        case DataElement.NULL:
            validateConversion(element, 0, type, 0, null);
            break;
        case DataElement.BOOL:
            validateConversion(element, 0, type, element.getBoolean() ? 1 : 0, null);
            break;
        case DataElement.U_INT_1:
        case DataElement.INT_1:
        case DataElement.U_INT_2:
        case DataElement.INT_2:
        case DataElement.U_INT_4:
        case DataElement.INT_4:
        case DataElement.INT_8:
            validateConversion(element, 0, type, element.getLong(), null);
            break;
        case DataElement.U_INT_8:
        case DataElement.U_INT_16:
        case DataElement.INT_16:
            validateConversion(element, 0, type, 0, (byte[]) element.getValue());
            break;
        case DataElement.UUID:
            validateConversion(element, 0, type, 0, Utils.UUIDToByteArray((UUID) element.getValue()));
            break;
        case DataElement.STRING:
            byte[] bs = Utils.getUTF8Bytes((String) element.getValue());
            validateConversion(element, 0, type, 0, bs);
            break;
        case DataElement.URL:
            byte[] bu = Utils.getASCIIBytes((String) element.getValue());
            validateConversion(element, 0, type, 0, bu);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void testInt() throws IOException {
        validateConversion(new DataElement(DataElement.U_INT_1, 0));
        validateConversion(new DataElement(DataElement.U_INT_1, 1));
        validateConversion(new DataElement(DataElement.U_INT_1, 1));
        validateConversion(new DataElement(DataElement.U_INT_2, 60));
        validateConversion(new DataElement(DataElement.U_INT_4, 77839));

        validateConversion(new DataElement(DataElement.U_INT_1, 0));
        validateConversion(new DataElement(DataElement.U_INT_1, 0xBC));
        validateConversion(new DataElement(DataElement.U_INT_2, 0));
        validateConversion(new DataElement(DataElement.U_INT_2, 0xABCD));
        validateConversion(new DataElement(DataElement.U_INT_4, 0));
        validateConversion(new DataElement(DataElement.U_INT_4, 0xABCDEF40l));

        validateConversion(new DataElement(DataElement.INT_1, -7));
        validateConversion(new DataElement(DataElement.INT_2, -5));
        validateConversion(new DataElement(DataElement.INT_4, -25678));

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

        validateConversion(new DataElement(DataElement.INT_8, 1));
        validateConversion(new DataElement(DataElement.INT_8, 257));
        validateConversion(new DataElement(DataElement.INT_8, 2497));
        validateConversion(new DataElement(DataElement.INT_8, -1));
        validateConversion(new DataElement(DataElement.INT_8, 998652497));

        validateConversion(new DataElement(DataElement.INT_8, 0x3eC6EF355892EA8Cl));
        validateConversion(new DataElement(DataElement.INT_8, -0x7F893012AB39FB72l));

        validateConversion(new DataElement(DataElement.U_INT_8, new byte[] { 1, -2, 3, 4, -5, 6, 7, -8 }));
        validateConversion(new DataElement(DataElement.INT_16, new byte[] { 11, -22, 33, 44, -5, 6, 77, 88, 9, -10, 11,
                12, -13, 14, 15, 16 }));
        validateConversion(new DataElement(DataElement.U_INT_16, new byte[] { 21, -32, 43, 54, -65, 76, 87, 98, 11,
                -110, 111, 112, -113, 114, 115, 16 }));
    }

    public void testUUID() throws IOException {
        validateConversion(new DataElement(DataElement.UUID, new UUID("0100", true)));
        validateConversion(new DataElement(DataElement.UUID, new UUID("B10C0BE1111111111111111111110001", false)));

        // This is the same UUIDs
        validateConversion(new DataElement(DataElement.UUID, new UUID("0000110500001000800000805f9b34fb", false)));
        validateConversion(new DataElement(DataElement.UUID, new UUID(0x1105)));
    }

    public void testString() throws IOException {
        validateConversion(new DataElement(DataElement.STRING, ""));
        validateConversion(new DataElement(DataElement.STRING, "12345"));
        validateConversion(new DataElement(DataElement.STRING, SDPStreamTest.stringUTFData));

        StringBuffer b = new StringBuffer();
        b.append("b");
        for (int i = 0; i < 0x100; i++) {
            b.append("Z");
        }
        b.append("e");
        validateConversion(new DataElement(DataElement.STRING, b.toString()));
    }

    public void testURL() throws IOException {
        validateConversion(new DataElement(DataElement.URL, ""));
        validateConversion(new DataElement(DataElement.URL, "12345"));
        validateConversion(new DataElement(DataElement.URL, "http://blueCove/"));

        StringBuffer b = new StringBuffer();
        b.append("b");
        for (int i = 0; i < 0x100; i++) {
            b.append("Z");
        }
        b.append("e");
        validateConversion(new DataElement(DataElement.URL, b.toString()));
    }

    public void testOtherAttributes() throws IOException {
        validateConversion(new DataElement(true));
        validateConversion(new DataElement(false));
        validateConversion(new DataElement(DataElement.NULL));
    }
}
