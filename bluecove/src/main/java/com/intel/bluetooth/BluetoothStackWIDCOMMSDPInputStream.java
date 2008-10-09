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

import java.io.IOException;
import java.io.InputStream;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

/**
 * Read WIDCOMM SDP_DISC_ATTTR_VAL C struct and convert to DataElements
 * 
 * @author vlads
 */
class BluetoothStackWIDCOMMSDPInputStream extends InputStream {

    public static final boolean debug = false;

    private InputStream source;

    protected BluetoothStackWIDCOMMSDPInputStream(InputStream in) throws IOException {
        this.source = in;
        readVersionInfo();
    }

    public int read() throws IOException {
        return source.read();
    }

    private long readLong(int size) throws IOException {
        long result = 0;
        for (int i = 0; i < size; i++) {
            result += ((long) read()) << (8 * i);
        }
        return result;
    }

    private long readLongDebug(int size) throws IOException {
        long result = 0;
        for (int i = 0; i < size; i++) {
            int data = read();
            if (debug) {
                DebugLog.debug("readLong data[" + i + "]", data);
            }
            result += ((long) data) << (8 * i);
        }
        return result;
    }

    private int readInt() throws IOException {
        return (int) readLong(4);
    }

    private byte[] readBytes(int size) throws IOException {
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) read();
        }
        return result;
    }

    static String hexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            buf.append(Integer.toHexString(b[i] >> 4 & 0xf));
            buf.append(Integer.toHexString(b[i] & 0xf));
        }
        return buf.toString();
    }

    static byte[] getUUIDHexBytes(UUID uuid) {
        return Utils.UUIDToByteArray(uuid);
    }

    static final int ATTR_TYPE_INT = 0; // Attribute value is an integer

    static final int ATTR_TYPE_TWO_COMP = 1; // Attribute value is an 2's

    // complement integer

    static final int ATTR_TYPE_UUID = 2; // Attribute value is a UUID

    static final int ATTR_TYPE_BOOL = 3; // Attribute value is a boolean

    static final int ATTR_TYPE_ARRAY = 4; // Attribute value is an array of

    // bytes

    static final int MAX_SEQ_ENTRIES = 20;

    static final int MAX_ATTR_LEN_OLD = 256;

    private int valueSize = 0;

    private void readVersionInfo() throws IOException {
        valueSize = readInt();
    }

    public DataElement readElement() throws IOException {

        DataElement result = null;
        DataElement mainSeq = null;
        DataElement currentSeq = null;
        int elements = readInt();
        if (elements < 0 || elements > MAX_SEQ_ENTRIES) {
            throw new IOException("Unexpected number of elements " + elements);
        }
        if (debug) {
            DebugLog.debug("elements", elements);
        }
        for (int i = 0; i < elements; i++) {
            if (debug) {
                DebugLog.debug("element", i);
            }
            int type = readInt();
            int length = readInt();
            boolean start_of_seq = (readInt() != 0);
            if (debug) {
                DebugLog.debug("type", type);
                DebugLog.debug("length", length);
                DebugLog.debug("start_of_seq", start_of_seq);
            }
            if (length < 0 || valueSize < length) {
                throw new IOException("Unexpected length " + length);
            }
            DataElement dataElement;
            switch (type) {
            case ATTR_TYPE_INT:
                switch (length) {
                case 1:
                    dataElement = new DataElement(DataElement.U_INT_1, readLong(1));
                    break;
                case 2:
                    dataElement = new DataElement(DataElement.U_INT_2, readLong(2));
                    break;
                case 4:
                    dataElement = new DataElement(DataElement.U_INT_4, readLong(4));
                    break;
                case 8:
                    dataElement = new DataElement(DataElement.U_INT_8, readBytes(8));
                    break;
                case 16:
                    dataElement = new DataElement(DataElement.U_INT_16, readBytes(16));
                    break;
                default:
                    throw new IOException("Unknown U_INT length " + length);
                }
                break;
            case ATTR_TYPE_TWO_COMP:
                switch (length) {
                case 1:
                    dataElement = new DataElement(DataElement.INT_1, (byte) readLong(1));
                    break;
                case 2:
                    dataElement = new DataElement(DataElement.INT_2, (short) readLong(2));
                    break;
                case 4:
                    dataElement = new DataElement(DataElement.INT_4, (int) readLong(4));
                    break;
                case 8:
                    dataElement = new DataElement(DataElement.INT_8, readLongDebug(8));
                    break;
                case 16:
                    dataElement = new DataElement(DataElement.INT_16, readBytes(16));
                    break;
                default:
                    throw new IOException("Unknown INT length " + length);
                }
                break;
            case ATTR_TYPE_UUID:
                UUID uuid = null;
                switch (length) {
                case 2:
                    uuid = new UUID(readLong(2));
                    break;
                case 4:
                    uuid = new UUID(readLong(4));
                    break;
                case 16:
                    uuid = new UUID(hexString(readBytes(16)), false);
                    break;
                default:
                    throw new IOException("Unknown UUID length " + length);
                }
                dataElement = new DataElement(DataElement.UUID, uuid);
                break;
            case ATTR_TYPE_BOOL:
                dataElement = new DataElement(readLong(length) != 0);
                break;
            case ATTR_TYPE_ARRAY:
                dataElement = new DataElement(DataElement.STRING, Utils.newStringUTF8(readBytes(length)));
                break;
            default:
                throw new IOException("Unknown data type " + type);
            }

            if (debug) {
                DebugLog.debug("dataElement " + dataElement);
            }

            if (start_of_seq) {
                DataElement newSeq = new DataElement(DataElement.DATSEQ);
                newSeq.addElement(dataElement);
                dataElement = newSeq;

                if (i != 0) {
                    // Second or other sequence
                    if (mainSeq != null) {
                        mainSeq.addElement(newSeq);
                    } else {
                        // move first sequence to new main sequence
                        mainSeq = new DataElement(DataElement.DATSEQ);
                        result = mainSeq;
                        mainSeq.addElement(currentSeq);
                        mainSeq.addElement(newSeq);
                    }
                }
                currentSeq = newSeq;
            } else if (currentSeq != null) {
                currentSeq.addElement(dataElement);
            }

            if (result == null) {
                result = dataElement;
            }

            if ((i < (elements - 1)) && (skip(valueSize - length) != (valueSize - length))) {
                throw new IOException("Unexpected end of data");
            }
        }
        return result;

    }
}
