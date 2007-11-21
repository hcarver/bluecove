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
import javax.bluetooth.UUID;

import junit.framework.TestCase;

/**
 * @author vlads
 * 
 */
public class SDPStreamTest extends TestCase {

    public static final String stringUTFData = "\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";

    public static void assertEquals(String message, DataElement de1, DataElement de2) {
        if ((de1 == null) || (de2 == null)) {
            fail(message + "NULL elements");
        }
        assertEquals(message + "Type", de1.getDataType(), de2.getDataType());
        switch (de1.getDataType()) {
        case DataElement.U_INT_1:
        case DataElement.U_INT_2:
        case DataElement.U_INT_4:
        case DataElement.INT_1:
        case DataElement.INT_2:
        case DataElement.INT_4:
        case DataElement.INT_8:
            assertEquals(message + "long", de1.getLong(), de2.getLong());
            return;
        case DataElement.URL:
        case DataElement.STRING:
        case DataElement.UUID:
            assertEquals(message + "value", de1.getValue(), de2.getValue());
            return;
        case DataElement.INT_16:
        case DataElement.U_INT_8:
        case DataElement.U_INT_16:
            byte[] byteAray1 = (byte[]) de1.getValue();
            byte[] byteAray2 = (byte[]) de2.getValue();
            assertEquals(message + "length", byteAray1.length, byteAray2.length);
            for (int k = 0; k < byteAray1.length; k++) {
                assertEquals(message + "byteAray[" + k + "]", byteAray1[k], byteAray2[k]);
            }
            return;
        case DataElement.NULL:
            return;
        case DataElement.BOOL:
            assertEquals(message + "boolean", de1.getBoolean(), de2.getBoolean());
            return;
        case DataElement.DATSEQ:
        case DataElement.DATALT:
            int i = 0;
            Enumeration en1 = (Enumeration) de1.getValue();
            Enumeration en2 = (Enumeration) de2.getValue();
            for (; en1.hasMoreElements() && en2.hasMoreElements();) {
                DataElement d1 = (DataElement) en1.nextElement();
                DataElement d2 = (DataElement) en2.nextElement();
                assertEquals(message + "DataElement[" + i + "]", d1, d2);
                i++;
            }
            if (en1.hasMoreElements() || en2.hasMoreElements()) {
                fail(message + "unknown hasMoreElements");
            }
            return;
        default:
            fail(message + "unknown type");
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
        assertEquals("", element, elementConverted);
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

    public void testUUID() throws IOException {
        validateConversion(new DataElement(DataElement.UUID, new UUID("0100", true)));
        validateConversion(new DataElement(DataElement.UUID, new UUID("B10C0BE1111111111111111111110001", false)));

        // This is the same UUIDs
        validateConversion(new DataElement(DataElement.UUID, new UUID("0000110500001000800000805f9b34fb", false)));
        validateConversion(new DataElement(DataElement.UUID, new UUID(0x1105)));
    }

    public static String getLongString(int lenght) {
        StringBuffer b = new StringBuffer();
        b.append("b");
        for (int i = 0; i < lenght - 2; i++) {
            b.append("Z");
        }
        b.append("e");
        return b.toString();
    }

    public void testString() throws IOException {
        validateConversion(new DataElement(DataElement.STRING, ""));
        validateConversion(new DataElement(DataElement.STRING, "12345"));
        validateConversion(new DataElement(DataElement.STRING, stringUTFData));

        validateConversion(new DataElement(DataElement.STRING, getLongString(0x100 + 2)));
    }

    public void testDATSEQ() throws IOException {
        DataElement seq1 = new DataElement(DataElement.DATSEQ);
        seq1.addElement(new DataElement(DataElement.STRING, "BlueCove-seq1"));
        seq1.addElement(new DataElement(DataElement.U_INT_1, 0x12));
        seq1.addElement(new DataElement(DataElement.URL, "http://blueCove/"));
        seq1.addElement(new DataElement(DataElement.STRING, stringUTFData));
        seq1.addElement(new DataElement(DataElement.UUID, new UUID("B10C0BE1111111111111111111110001", false)));

        DataElement seq2 = new DataElement(DataElement.DATSEQ);
        seq2.addElement(new DataElement(DataElement.U_INT_8, new byte[] { 1, -2, 3, 4, -5, 6, 7, -8 }));
        seq2.addElement(new DataElement(DataElement.STRING, getLongString(0x100 + 2)));
        seq2.addElement(new DataElement(DataElement.U_INT_2, 0x14));

        DataElement seq3 = new DataElement(DataElement.DATSEQ);
        seq3.addElement(new DataElement(DataElement.U_INT_4, 0x15));
        seq3.addElement(new DataElement(DataElement.STRING, stringUTFData));
        seq3.addElement(new DataElement(DataElement.UUID, new UUID(0x1105)));
        seq3.addElement(new DataElement(DataElement.INT_8, 0x16));

        seq1.addElement(seq2);
        seq1.addElement(seq3);
        seq1.addElement(new DataElement(DataElement.INT_4, 0x1BCDEF35l));
        validateConversion(seq1);
    }

    public void testOtherAttributes() throws IOException {
        validateConversion(new DataElement(true));
        validateConversion(new DataElement(false));
        validateConversion(new DataElement(DataElement.NULL));
    }

}
