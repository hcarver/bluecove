/**
 * 
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

import junit.framework.TestCase;

/**
 * @author vlads
 * 
 */
public class BlueZServiceRecordXMLTest extends TestCase {

    public static final String stringUTFData = "TODO";//"\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";
    
    @SuppressWarnings("unchecked")
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

    public void testParserINT() throws IOException {

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
    
    private DataElement doubleCovert(DataElement element, StringBuffer b) throws Exception {
        b.append("<record>");
        int id = 100;
        b.append("<attribute id=\"").append(id).append("\" >");
        BlueZServiceRecordXML.appendDataElement(b, element);
        b.append("</attribute>");
        b.append("</record>");
        
        Map<Integer, DataElement> elements = BlueZServiceRecordXML.parsXMLRecord(b.toString());
        assertEquals("Parsed elements", 1, elements.size());
        return elements.get(id);
    }

    private void validateConversion(DataElement element) throws Exception {
        StringBuffer b = new StringBuffer();
        boolean passed = false;
        DataElement elementConverted = null;
        try {
            elementConverted = doubleCovert(element, b);
            assertEquals("", element, elementConverted);
            passed = true;
        } finally {
            if (!passed) {
                System.out.println("ERROR: in  " + ((Object) element).toString());
                System.out.println("ERROR: out " + ((elementConverted == null)?"null":((Object) elementConverted).toString()));
                System.out.println("ERROR: in xml + " + b.toString());
            }
        }
    }
    
    public void testIntTypes() throws Exception {
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

    public void testUUID() throws Exception {
        validateConversion(new DataElement(DataElement.UUID, new UUID("0100", true)));
        validateConversion(new DataElement(DataElement.UUID, new UUID("B10C0BE1111111111111111111110001", false)));

        // This is the same UUIDs
        validateConversion(new DataElement(DataElement.UUID, new UUID("0000110500001000800000805f9b34fb", false)));
        validateConversion(new DataElement(DataElement.UUID, new UUID(0x1105)));
        // UUID 32-bit
        validateConversion(new DataElement(DataElement.UUID, new UUID(0x21301107)));
    }
    
    public void testString() throws Exception {
        validateConversion(new DataElement(DataElement.STRING, ""));
        validateConversion(new DataElement(DataElement.STRING, "12345"));
        validateConversion(new DataElement(DataElement.STRING, stringUTFData));
    }

    public void testDATSEQ() throws Exception {
        DataElement seq1 = new DataElement(DataElement.DATSEQ);
        seq1.addElement(new DataElement(DataElement.STRING, "BlueCove-seq1"));
        seq1.addElement(new DataElement(DataElement.U_INT_1, 0x12));
        seq1.addElement(new DataElement(DataElement.URL, "http://blueCove/"));
        seq1.addElement(new DataElement(DataElement.STRING, "AData"));
        seq1.addElement(new DataElement(DataElement.UUID, new UUID("B10C0BE1111111111111111111110001", false)));

        DataElement seq2 = new DataElement(DataElement.DATSEQ);
        seq2.addElement(new DataElement(DataElement.U_INT_8, new byte[] { 1, -2, 3, 4, -5, 6, 7, -8 }));
        seq2.addElement(new DataElement(DataElement.STRING, "DataData"));
        seq2.addElement(new DataElement(DataElement.U_INT_2, 0x14));

        DataElement seq3 = new DataElement(DataElement.DATSEQ);
        seq3.addElement(new DataElement(DataElement.U_INT_4, 0x15));
        seq3.addElement(new DataElement(DataElement.STRING, "MoreDataData"));
        seq3.addElement(new DataElement(DataElement.UUID, new UUID(0x1105)));
        seq3.addElement(new DataElement(DataElement.INT_8, 0x16));

        seq1.addElement(seq2);
        seq1.addElement(seq3);
        seq1.addElement(new DataElement(DataElement.INT_4, 0x1BCDEF35l));
        validateConversion(seq1);
    }

    public void testDATSEQ16() throws Exception {
        DataElement seq = new DataElement(DataElement.DATSEQ);
        // INT_8 = 9 bytes;
        int nElements = (0xFF / 9) + 1;
        for (int i = 0; i < nElements; i++) {
            seq.addElement(new DataElement(DataElement.INT_8, i));
        }
        int l = SDPOutputStream.getLength(seq);
        assertTrue("DATSEQ16 len(" + l + ")>0xFF", l > 0xFF);
        assertTrue("DATSEQ16 len(" + l + ")<0xFFFF", l < 0xFFFF);
        validateConversion(seq);
    }

    public void testDATSEQ32() throws Exception {
        DataElement seq = new DataElement(DataElement.DATSEQ);
        // INT_8 = 9 bytes;
        int nElements = (0xFFFF / 9) + 1;
        for (int i = 0; i < nElements; i++) {
            seq.addElement(new DataElement(DataElement.INT_8, i));
        }
        int l = SDPOutputStream.getLength(seq);
        assertTrue("DATSEQ32 len(" + l + ")>0xFFFF", l > 0xFFFF);
        validateConversion(seq);
    }

    public void testOtherAttributes() throws Exception {
        validateConversion(new DataElement(true));
        validateConversion(new DataElement(false));
        validateConversion(new DataElement(DataElement.NULL));
    }
}
