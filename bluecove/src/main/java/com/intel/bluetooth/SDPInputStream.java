/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.io.InputStream;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

class SDPInputStream extends InputStream {

	private InputStream source;

	private long pos;

	public SDPInputStream(InputStream in) {
		this.source = in;
		pos = 0;
	}

	public int read() throws IOException {
		return source.read();
	}

	private long readLong(int size) throws IOException {
		long result = 0;

		for (int i = 0; i < size; i++) {
			result = result << 8 | read();
		}

		pos += size;

		return result;
	}

	private String hexString(byte[] b) throws IOException {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < b.length; i++) {
			buf.append(Integer.toHexString(b[i] >> 4 & 0xf));
			buf.append(Integer.toHexString(b[i] & 0xf));
		}

		return buf.toString();
	}

	private byte[] readBytes(int size) throws IOException {
		byte[] result = new byte[size];

		for (int i = 0; i < size; i++) {
			result[i] = (byte) read();
		}

		pos += size;

		return result;
	}

	public DataElement readElement() throws IOException {
		int header = read();
		int type = header >> 3 & 0x1f;
		int sizeDescriptor = header & 0x07;

		pos++;

		switch (type) {
		case 0: // NULL
			return new DataElement(DataElement.NULL);
		case 1: // U_INT
			switch (sizeDescriptor) {
			case 0:
				return new DataElement(DataElement.U_INT_1, readLong(1));
			case 1:
				return new DataElement(DataElement.U_INT_2, readLong(2));
			case 2:
				return new DataElement(DataElement.U_INT_4, readLong(4));
			case 3:
				return new DataElement(DataElement.U_INT_8, readBytes(8));
			case 4:
				return new DataElement(DataElement.U_INT_16, readBytes(16));
			default:
				throw new IOException();
			}
		case 2: // INT
			switch (sizeDescriptor) {
			case 0:
				return new DataElement(DataElement.INT_1, (long) (byte) readLong(1));
			case 1:
				return new DataElement(DataElement.INT_2, (long) (short) readLong(2));
			case 2:
				return new DataElement(DataElement.INT_4, (long) (int) readLong(4));
			case 3:
				return new DataElement(DataElement.INT_8, readLong(8));
			case 4:
				return new DataElement(DataElement.INT_16, readBytes(16));
			default:
				throw new IOException();
			}
		case 3: // UUID
		{
			UUID uuid = null;

			switch (sizeDescriptor) {
			case 1:
				uuid = new UUID(readLong(2));
				break;
			case 2:
				uuid = new UUID(readLong(4));
				break;
			case 4:
				uuid = new UUID(hexString(readBytes(16)), false);
				break;
			default:
				throw new IOException();
			}

			return new DataElement(DataElement.UUID, uuid);
		}
		case 4: // STRING
		{
			int length = -1;

			switch (sizeDescriptor) {
			case 5:
				length = (int) readLong(1);
				break;
			case 6:
				length = (int) readLong(2);
				break;
			case 7:
				length = (int) readLong(4);
				break;
			default:
				throw new IOException();
			}

			return new DataElement(DataElement.STRING, Utils.newStringUTF8(readBytes(length)));
		}
		case 5: // BOOL
			return new DataElement(readLong(1) != 0);
		case 6: // DATSEQ
		{
			long length;

			switch (sizeDescriptor) {
			case 5:
				length = readLong(1);
				break;
			case 6:
				length = readLong(2);
				break;
			case 7:
				length = readLong(4);
				break;
			default:
				throw new IOException();
			}

			DataElement element = new DataElement(DataElement.DATSEQ);

			long started = pos;

			for (long end = pos + length; pos < end;) {
				element.addElement(readElement());
			}
			if (started + length != pos) {
				throw new IOException("DATSEQ size corruption " + (started + length - pos));
			}
			return element;
		}
		case 7: // DATALT
		{
			long length;

			switch (sizeDescriptor) {
			case 5:
				length = readLong(1);
				break;
			case 6:
				length = readLong(2);
				break;
			case 7:
				length = readLong(4);
				break;
			default:
				throw new IOException();
			}

			DataElement element = new DataElement(DataElement.DATALT);

			long started = pos;

			for (long end = pos + length; pos < end;) {
				element.addElement(readElement());
			}
			if (started + length != pos) {
				throw new IOException("DATALT size corruption " + (started + length - pos));
			}
			return element;
		}
		case 8: // URL
		{
			int length;

			switch (sizeDescriptor) {
			case 5:
				length = (int) readLong(1);
				break;
			case 6:
				length = (int) readLong(2);
				break;
			case 7:
				length = (int) readLong(4);
				break;
			default:
				throw new IOException();
			}

			return new DataElement(DataElement.URL, Utils.newStringASCII(readBytes(length)));
		}
		default:
			throw new IOException("Unknown type " + type);
		}
	}

}