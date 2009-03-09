/**
 * 
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.bluetooth.DataElement;

import junit.framework.TestCase;

/**
 * @author vlads
 * 
 */
public class BlueZServiceRecordXMLTest extends TestCase {

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
            e.printStackTrace();
            return false;
        }
    }

    static public void assertEquals(String message, DataElement expected, DataElement actual) {
        if (equals(expected, actual)) {
            return;
        }
        fail(message + " expected:[" + expected + "] actual:" + actual + "]");
    }

    private void assertParser(DataElement expected, int id, String[] xml) throws IOException {
        StringBuffer b = new StringBuffer();
        for (String x : xml) {
            b.append(x).append('\n');
        }
        Map<Integer, DataElement> elements = BlueZServiceRecordXML.parsXMLRecord(b.toString());
        DataElement actual = elements.get(id);
        assertEquals(b.toString(), expected, actual);
    }

    public void testINT() throws IOException {

        assertParser(new DataElement(DataElement.U_INT_1, 0x4), 0, new String[] { "<record>",

        "<attribute id=\"0x0000\">",

        "<uint8 value=\"4\" />",

        "</attribute>",

        "</record>" });

        
        assertParser(new DataElement(DataElement.U_INT_2, 0x2794), 0, new String[] { "<record>",

        "<attribute id=\"0x0000\">",

        "<uint16 value=\"0x2794\" />",

        "</attribute>",

        "</record>" });

        
        assertParser(new DataElement(DataElement.U_INT_4, 0xabcdef40L), 0, new String[] { "<record>",

        "<attribute id=\"0x0000\">",

        "<uint32 value=\"0xabcdef40\" />",

        "</attribute>",

        "</record>" });

        
        assertParser(new DataElement(DataElement.INT_1, 0x4), 0, new String[] { "<record>",

            "<attribute id=\"0x0000\">",

            "<int8 value=\"0x4\" />",

            "</attribute>",

            "</record>" });
        
        
        assertParser(new DataElement(DataElement.INT_2, 0x584A), 0, new String[] { "<record>",

            "<attribute id=\"0x0000\">",

            "<int16 value=\"0x584A\" />",

            "</attribute>",

            "</record>" });
    }
}
