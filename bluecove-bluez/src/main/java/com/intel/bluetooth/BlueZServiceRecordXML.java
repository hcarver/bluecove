/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2009 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *
 * @version $Id$
 */
package com.intel.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Read XML representation of Service records to JAR-82 format records.
 * 
 */
class BlueZServiceRecordXML {

    private static final Map<String, Integer> integerXMLtypes = new HashMap<String, Integer>();

    private static final Map<Integer, String> allXMLtypes = new HashMap<Integer, String>();
    
    static {
        integerXMLtypes.put("uint8", DataElement.U_INT_1);
        integerXMLtypes.put("uint16", DataElement.U_INT_2);
        integerXMLtypes.put("uint32", DataElement.U_INT_4);

        integerXMLtypes.put("int8", DataElement.INT_1);
        integerXMLtypes.put("int16", DataElement.INT_2);
        integerXMLtypes.put("int32", DataElement.INT_4);
        integerXMLtypes.put("int64", DataElement.INT_8);
        
        allXMLtypes.put(DataElement.U_INT_1, "uint8");
        allXMLtypes.put(DataElement.U_INT_2, "uint16");
        allXMLtypes.put(DataElement.U_INT_4, "uint32");
        allXMLtypes.put(DataElement.U_INT_8, "uint64");
        allXMLtypes.put(DataElement.U_INT_16, "uint128");
        allXMLtypes.put(DataElement.INT_1, "int8");
        allXMLtypes.put(DataElement.INT_2, "int16");
        allXMLtypes.put(DataElement.INT_4, "int32");
        allXMLtypes.put(DataElement.INT_8, "int64");
        allXMLtypes.put(DataElement.INT_16, "int128");
        allXMLtypes.put(DataElement.BOOL, "boolean");
        allXMLtypes.put(DataElement.UUID, "uuid");
        allXMLtypes.put(DataElement.STRING, "text");
        allXMLtypes.put(DataElement.URL, "url");
        allXMLtypes.put(DataElement.NULL, "nil");
        allXMLtypes.put(DataElement.DATALT, "alternate");
        allXMLtypes.put(DataElement.DATSEQ, "sequence");
    }

    private static String toHexStringSigned(long l) {
        if (l >= 0) {
            return "0x" + Long.toHexString(l);
        } else {
            return "-0x" + Long.toHexString(-l);
        }
    }
    
    private static void appendUUID(StringBuffer buf, UUID uuid) {
        String str = uuid.toString().toUpperCase();
        int shortIdx = str.indexOf(BluetoothConsts.SHORT_UUID_BASE);
        if ((shortIdx != -1) && (shortIdx + BluetoothConsts.SHORT_UUID_BASE.length() == str.length())) {
            // This is short 16-bit or 32-bit UUID
            buf.append("0x").append(str.substring(0, shortIdx));
        } else {
            //Make it in format 12345678-1234-1234-1234-123456789012
            buf.append(str.substring(0, 8)).append('-').append(str.substring(8, 12)).append('-');
            buf.append(str.substring(12, 16)).append('-').append(str.substring(16, 20)).append('-').append(str.substring(20));
        }
    }
    
    @SuppressWarnings("unchecked")
    static void appendDataElement(StringBuffer buf, DataElement d) {
        int valueType = d.getDataType();
        String type = allXMLtypes.get(valueType);
        buf.append("<").append(type);
        
        switch (valueType) {
        case DataElement.U_INT_1:
        case DataElement.U_INT_2:
        case DataElement.U_INT_4:
            buf.append(" value=\"0x").append(Long.toHexString(d.getLong())).append("\">");
            break;
        case DataElement.INT_1:
        case DataElement.INT_2:
        case DataElement.INT_4:
        case DataElement.INT_8:
            buf.append(" value=\"").append(toHexStringSigned(d.getLong())).append("\">");
            break;
        case DataElement.BOOL:
            buf.append(" value=\"").append(d.getBoolean()?"true":"false").append("\">");
            break;
        case DataElement.URL:
        case DataElement.STRING:
            //TODO add encoding
            buf.append(" value=\"").append(d.getValue()).append("\">");
            break;
        case DataElement.UUID:
            buf.append(" value=\"");
            appendUUID(buf, ((UUID) d.getValue()));
            buf.append("\">");
            break;
        case DataElement.U_INT_8:
            byte[] b8 = (byte[]) d.getValue();
            buf.append(" value=\"0x");
            for (int i = 0; i < b8.length; i++) {
                buf.append(Integer.toHexString(b8[i] >> 4 & 0xf));
                buf.append(Integer.toHexString(b8[i] & 0xf));
            }
            buf.append("\">");
            break;
        case DataElement.U_INT_16:
        case DataElement.INT_16:
            byte[] b16 = (byte[]) d.getValue();
            buf.append(" value=\"");
            for (int i = b16.length - 1; i >= 0; i--) {
                buf.append(Integer.toHexString(b16[i] >> 4 & 0xf));
                buf.append(Integer.toHexString(b16[i] & 0xf));
            }
            buf.append("\">");
            break;
        case DataElement.DATALT:
        case DataElement.DATSEQ:
            buf.append(">\n");
            for (Enumeration en = (Enumeration) (d.getValue()); en.hasMoreElements();) {
                appendDataElement(buf, (DataElement)en.nextElement());
            }
            break;
        case DataElement.NULL:
            buf.append(">\n");
            break;
        default:
            throw new IllegalArgumentException("DataElement type " + valueType );
        }
        buf.append("</").append(type).append(">\n");
    }
    
    public static String exportXMLRecord(ServiceRecord serviceRecord) {
        StringBuffer b = new StringBuffer();
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        b.append("<record>\n");
        int[] ids = serviceRecord.getAttributeIDs();
        Vector<Integer> sorted = new Vector<Integer>();
        for (int i = 0; i < ids.length; i++) {
            sorted.addElement(new Integer(ids[i]));
        }
        Collections.sort(sorted);
        for(Integer id : sorted) {
            b.append("<attribute id=\"0x").append(Long.toHexString(id)).append("\" >\n");
            appendDataElement(b, serviceRecord.getAttributeValue(id));
            b.append("</attribute>\n");
        }
        b.append("</record>");
        return b.toString();
    }
    
    public static Map<Integer, DataElement> parsXMLRecord(String xml) throws IOException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes())));
            Element root = doc.getDocumentElement();
            if (!"record".equals(root.getTagName())) {
                throw new IOException("SDP xml record expected, got " + root.getTagName());
            }

            Map<Integer, DataElement> elements = new HashMap<Integer, DataElement>();

            NodeList nodes = root.getElementsByTagName("attribute");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Node idNode = node.getAttributes().getNamedItem("id");
                int id = parsInt(idNode.getNodeValue());
                NodeList children = node.getChildNodes();
                for (int j = 0; (children != null) && (j < children.getLength()); j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        elements.put(id, parsDataElement(child));
                        break;
                    }
                }
            }
            return elements;
        } catch (ParserConfigurationException e) {
            throw (IOException) UtilsJavaSE.initCause(new IOException(e.getMessage()), e);
        } catch (SAXException e) {
            throw (IOException) UtilsJavaSE.initCause(new IOException(e.getMessage()), e);
        }
    }

    private static int parsInt(String value) {
        if (value.startsWith("0x")) {
            return Integer.valueOf(value.substring(2), 16).intValue();
        } else {
            return Integer.valueOf(value).intValue();
        }
    }

    private static long parsLong(String value) {
        if (value.startsWith("0x")) {
            return Long.valueOf(value.substring(2), 16).longValue();
        } else if (value.startsWith("-0x")) {
            return - Long.valueOf(value.substring(3), 16).longValue();
        } else {
            return Long.valueOf(value).longValue();
        }
    }

    private static long getLongValue(Node node) throws IOException {
        Node valueNode = node.getAttributes().getNamedItem("value");
        if (valueNode == null) {
            throw new IOException("value attribute expected in " + node.getNodeName());
        } else {
            return parsLong(valueNode.getNodeValue());
        }
    }

    private static boolean getBoolValue(Node node) throws IOException {
        Node valueNode = node.getAttributes().getNamedItem("value");
        if (valueNode == null) {
            throw new IOException("value attribute expected in " + node.getNodeName());
        } else {
            return "true".equals(valueNode.getNodeValue());
        }
    }

    private static UUID getUUIDValue(Node node) throws IOException {
        Node valueNode = node.getAttributes().getNamedItem("value");
        if (valueNode == null) {
            throw new IOException("value attribute expected in " + node.getNodeName());
        }
        String value = valueNode.getNodeValue();
        if (value.length() == 32) {
            return new UUID(value.replace("-", ""), false);
        } else if (value.startsWith("0x")) {
            return new UUID(Long.valueOf(value.substring(2), 16).longValue());
        } else {
            String value2 = value.replace("-", "");
            if (value2.length() == 32) {
                return new UUID(value2, false);
            }
            throw new IOException("Unknown UUID format " + value);
        }
    }

    private static byte[] getByteArrayValue(Node node, int length) throws IOException {
        Node valueNode = node.getAttributes().getNamedItem("value");
        if (valueNode == null) {
            throw new IOException("value attribute expected in " + node.getNodeName());
        }
        String value = valueNode.getNodeValue();
        if (length != value.length() / 2) {
            throw new IOException("value attribute invalid length " + value.length());
        }
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[(length - 1) - i] = (byte)(Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16) & 0xFF);
        }
        return result;
    }

    private static byte[] getByteArrayUINT8(Node node, int length) throws IOException {
        Node valueNode = node.getAttributes().getNamedItem("value");
        if (valueNode == null) {
            throw new IOException("value attribute expected in " + node.getNodeName());
        }
        String value = valueNode.getNodeValue();
        int radix = 10;
        if (value.startsWith("0x")) {
            value = value.substring(2);
            radix = 16;
        }
        if (length != value.length() / 2) {
            throw new IOException("value attribute invalid length " + value.length());
        }
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte)(Integer.parseInt(value.substring(i * 2, i * 2 + 2), radix) & 0xFF);
        }
        return result;
    }
    
    private static String getTextValue(Node node) throws IOException {
        Node valueNode = node.getAttributes().getNamedItem("value");
        if (valueNode == null) {
            throw new IOException("value attribute expected in " + node.getNodeName());
        } else {
            Node encodingNode = node.getAttributes().getNamedItem("encoding");
            if (encodingNode == null) {
                return valueNode.getNodeValue();
            }
            if ("hex".equals(encodingNode.getNodeValue())) {
                StringBuffer b = new StringBuffer();
                String value = valueNode.getNodeValue();
                for (int i = 0; i < value.length() / 2; i++) {
                    b.append((char) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16));
                }
                return b.toString();
            } else {
                throw new IOException("Unknown text encoding " + encodingNode.getNodeValue());
            }
        }
    }

    private static DataElement parsDataElement(Node node) throws IOException {
        String name = node.getNodeName();
        Integer intValueType = integerXMLtypes.get(name);
        if (intValueType != null) {
            return new DataElement(intValueType.intValue(), getLongValue(node));
        } else if ("sequence".equals(name)) {
            DataElement seq = new DataElement(DataElement.DATSEQ);
            NodeList children = node.getChildNodes();
            for (int j = 0; (children != null) && (j < children.getLength()); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    seq.addElement(parsDataElement(child));
                }
            }
            return seq;
        } else if ("alternate".equals(name)) {
            DataElement seq = new DataElement(DataElement.DATALT);
            NodeList children = node.getChildNodes();
            for (int j = 0; (children != null) && (j < children.getLength()); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    seq.addElement(parsDataElement(child));
                }
            }
            return seq;
        } else if ("uuid".equals(name)) {
            return new DataElement(DataElement.UUID, getUUIDValue(node));
        } else if ("text".equals(name)) {
            return new DataElement(DataElement.STRING, getTextValue(node));
        } else if ("url".equals(name)) {
            return new DataElement(DataElement.URL, getTextValue(node));
        } else if ("nil".equals(name)) {
            return new DataElement(DataElement.NULL);
        } else if ("boolean".equals(name)) {
            return new DataElement(getBoolValue(node));
        } else if ("uint64".equals(name)) {
            return new DataElement(DataElement.U_INT_8, getByteArrayUINT8(node, 8));
        } else if ("int128".equals(name)) {
            return new DataElement(DataElement.INT_16, getByteArrayValue(node, 16));
        } else if ("uint128".equals(name)) {
            return new DataElement(DataElement.U_INT_16, getByteArrayValue(node, 16));   
        } else {
            throw new IOException("Unrecognized DataElement " + name);
        }
    }

}
