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
package com.intel.bluetooth;

import java.io.IOException;
import java.io.InputStream;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

public class SDPInputStream extends InputStream {
	
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

		for (int i = 0; i < size; i++)
			result = result << 8 | read();

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

		for (int i = 0; i < size; i++)
			result[i] = (byte) read();

		pos += size;

		return result;
	}

	public DataElement readElement() throws IOException {
		int header = read();
		int type = header >> 3 & 0x1f;
		int size = header & 0x07;

		pos++;

		switch (type) {
		case 0: // NULL
			return new DataElement(DataElement.NULL);
		case 1: // U_INT
			switch (size) {
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
			switch (size) {
			case 0:
				return new DataElement(DataElement.INT_1,
						(long) (byte) readLong(1));
			case 1:
				return new DataElement(DataElement.INT_2,
						(long) (short) readLong(2));
			case 2:
				return new DataElement(DataElement.INT_4,
						(long) (int) readLong(4));
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

			switch (size) {
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

			switch (size) {
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

			return new DataElement(DataElement.STRING, new String(
					readBytes(length)));
		}
		case 5: // BOOL
			return new DataElement(readLong(1) != 0);
		case 6: // DATSEQ
		{
			long length;

			switch (size) {
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

			for (long end = pos + length; pos < end;)
				element.addElement(readElement());

			return element;
		}
		case 7: // DATALT
		{
			long length;

			switch (size) {
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

			for (long end = pos + length; pos < end;)
				element.addElement(readElement());

			return element;
		}
		case 8: // URL
		{
			int length = -1;

			switch (size) {
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

			return new DataElement(DataElement.URL, new String(
					readBytes(length)));
		}
		default:
			throw new IOException();
		}
	}

}