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
public class BluetoothStackWIDCOMMSDPInputStreamTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        System.getProperties().put("bluecove.debug", "true");
    }

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

    static class BluetoothStackWIDCOMMSDPOutputStream extends ByteArrayOutputStream {

        /*
         * private long readLong(int size) throws IOException { long result = 0; for (int i = 0; i < size; i++) { result +=
         * ((long)read()) << (8 * i); } return result; }
         */

        public void writeLong(long value, int size) throws IOException {
            long data = value;
            byte[] b = new byte[size];
            for (int i = 0; i < size; i++) {
                b[i] = (byte) ((data) & 0xFF);
                data >>>= 8;
            }
            write(b);
        }

        public void writeInt(int i) throws IOException {
            writeLong(i, 4);
        }

        public void write0(int size) throws IOException {
            for (int i = 0; i < size; i++) {
                write(0);
            }
        }

    }

    public void testUUID() {
        UUID uuid = new UUID("E10C0FE1121111A11111161911110003", false);
        byte[] bytes = BluetoothStackWIDCOMMSDPInputStream.getUUIDHexBytes(uuid);
        UUID uuid2 = new UUID(BluetoothStackWIDCOMMSDPInputStream.hexString(bytes), false);
        assertEquals("UUID", uuid, uuid2);
    }

    public void testServiceClassIDList() throws IOException {
        /*
         * DATSEQ { UUID 0000110100001000800000805f9b34fb (SERIAL_PORT) }
         */
        DataElement expect = new DataElement(DataElement.DATSEQ);
        expect.addElement(new DataElement(DataElement.UUID, new UUID("1101", true)));

        BluetoothStackWIDCOMMSDPOutputStream bos = new BluetoothStackWIDCOMMSDPOutputStream();
        int valueSize = BluetoothStackWIDCOMMSDPInputStream.MAX_ATTR_LEN_OLD;
        bos.writeInt(valueSize);

        bos.writeInt(1); // num_elem
        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_UUID); // type
        bos.writeInt(2); // len
        bos.writeInt(1); // start_of_seq
        bos.writeLong(0x1101, 2);
        bos.flush();

        DataElement element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(bos.toByteArray())))
                .readElement();

        assertEquals("Element stream 1 item", expect, element);

        /*
         * DATSEQ { UUID 0000110100001000800000805f9b34fb (SERIAL_PORT) UUID 0000120300001000800000805f9b34fb }
         */
        expect.addElement(new DataElement(DataElement.UUID, new UUID("1203", true)));

        bos = new BluetoothStackWIDCOMMSDPOutputStream();
        bos.writeInt(valueSize);

        bos.writeInt(2); // num_elem
        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_UUID); // type
        bos.writeInt(2); // len
        bos.writeInt(1); // start_of_seq
        bos.writeLong(0x1101, 2);
        bos.write0(valueSize - 2);
        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_UUID); // type
        bos.writeInt(2); // len
        bos.writeInt(0); // start_of_seq
        bos.writeLong(0x1203, 2);
        bos.flush();

        element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(bos.toByteArray()))).readElement();

        assertEquals("Element stream 2 items", expect, element);
    }

    public void testProtocolDescriptorList() throws IOException {
        /*
         * DATSEQ { DATSEQ { UUID 0000010000001000800000805f9b34fb (L2CAP) } DATSEQ { UUID
         * 0000000300001000800000805f9b34fb (RFCOMM) U_INT_1 0x9 } DATSEQ { UUID 0000000800001000800000805f9b34fb (OBEX) } }
         */

        DataElement expect = new DataElement(DataElement.DATSEQ);
        DataElement e1 = new DataElement(DataElement.DATSEQ);
        e1.addElement(new DataElement(DataElement.UUID, new UUID("0100", true)));
        DataElement e2 = new DataElement(DataElement.DATSEQ);
        e2.addElement(new DataElement(DataElement.UUID, new UUID("0003", true)));
        e2.addElement(new DataElement(DataElement.U_INT_1, 9));
        DataElement e3 = new DataElement(DataElement.DATSEQ);
        e3.addElement(new DataElement(DataElement.UUID, new UUID("0008", true)));
        expect.addElement(e1);
        expect.addElement(e2);
        expect.addElement(e3);

        BluetoothStackWIDCOMMSDPOutputStream bos = new BluetoothStackWIDCOMMSDPOutputStream();

        int valueSize = BluetoothStackWIDCOMMSDPInputStream.MAX_ATTR_LEN_OLD;
        bos.writeInt(valueSize);

        bos.writeInt(4); // num_elem

        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_UUID); // type
        bos.writeInt(2); // len
        bos.writeInt(1); // start_of_seq
        bos.writeLong(0x0100, 2);
        bos.write0(valueSize - 2);

        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_UUID); // type
        bos.writeInt(2); // len
        bos.writeInt(1); // start_of_seq
        bos.writeLong(0x0003, 2);
        bos.write0(valueSize - 2);

        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_INT); // type
        bos.writeInt(1); // len
        bos.writeInt(0); // start_of_seq
        bos.writeLong(9, 1);
        bos.write0(valueSize - 1);

        bos.writeInt(BluetoothStackWIDCOMMSDPInputStream.ATTR_TYPE_UUID); // type
        bos.writeInt(2); // len
        bos.writeInt(1); // start_of_seq
        bos.writeLong(0x0008, 2);
        bos.write0(valueSize - 2);

        bos.flush();

        DataElement element = (new BluetoothStackWIDCOMMSDPInputStream(new ByteArrayInputStream(bos.toByteArray())))
                .readElement();

        assertEquals("Element stream", expect, element);
    }
}
