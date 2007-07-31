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

import java.io.OutputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

class SDPOutputStream extends OutputStream {
	
	OutputStream dst;
	
	public SDPOutputStream(OutputStream out) {
		this.dst = out;
	}

	public void write(int oneByte) throws IOException {
		this.dst.write(oneByte);
	}
	
	private void writeLong(long l, int size) throws IOException {
		for (int i = 0; i < size; i++) {
			write((int) (l >> (size - 1 << 3)));
			l <<= 8;
		}
	}

	private void writeBytes(byte[] b) throws IOException {
		for (int i = 0; i < b.length; i++) {
			write(b[i]);
		}
	}

	private int getLength(DataElement d) {
		switch (d.getDataType()) {
		case DataElement.NULL:
			return 1;

			
		case DataElement.BOOL:
		case DataElement.U_INT_1:
		case DataElement.INT_1:
			return 2;
		
		case DataElement.U_INT_2:
		case DataElement.INT_2:
			return 3;
		
		case DataElement.U_INT_4:
		case DataElement.INT_4:
			return 5;
		
		case DataElement.U_INT_8:
		case DataElement.INT_8:
			return 9;
		
		case DataElement.U_INT_16:
		case DataElement.INT_16:
		case DataElement.UUID:
			return 17;

		case DataElement.STRING:
		case DataElement.URL: {
			byte[] b = ((String) d.getValue()).getBytes();

			if (b.length < 0x100)
				return b.length + 2;
			else if (b.length < 0x10000)
				return b.length + 3;
			else
				return b.length + 5;
		}

		case DataElement.DATSEQ:
		case DataElement.DATALT: {
			int result = 5;

			for (Enumeration e = (Enumeration) d.getValue(); e.hasMoreElements();) {
				result += getLength((DataElement) e.nextElement());
			}

			return result;
		}

		default:
			throw new IllegalArgumentException();
		}
	}

	public void writeElement(DataElement d) throws IOException {
		switch (d.getDataType()) {
		case DataElement.NULL:
			write(0 | 0);
			break;

		case DataElement.U_INT_1:
			write(8 | 0);
			writeLong(d.getLong(), 1);
			break;
		case DataElement.U_INT_2:
			write(8 | 1);
			writeLong(d.getLong(), 2);
			break;
		case DataElement.U_INT_4:
			write(8 | 2);
			writeLong(d.getLong(), 4);
			break;
		case DataElement.U_INT_8:
			write(8 | 3);
			writeBytes((byte[]) d.getValue());
			break;
		case DataElement.U_INT_16:
			write(8 | 4);
			writeBytes((byte[]) d.getValue());
			break;

		case DataElement.INT_1:
			write(16 | 0);
			writeLong(d.getLong(), 1);
			break;
		case DataElement.INT_2:
			write(16 | 1);
			writeLong(d.getLong(), 2);
			break;
		case DataElement.INT_4:
			write(16 | 2);
			writeLong(d.getLong(), 4);
			break;
		case DataElement.INT_8:
			write(16 | 3);
			writeLong(d.getLong(), 8);
			break;
		case DataElement.INT_16:
			write(16 | 4);
			writeBytes((byte[]) d.getValue());
			break;

		case DataElement.UUID:
			write(24 | 4);
			writeBytes(Utils.UUIDToByteArray((UUID)d.getValue()));
			break;

		case DataElement.STRING: {
			byte[] b = ((String) d.getValue()).getBytes();

			if (b.length < 0x100) {
				write(32 | 5);
				writeLong(b.length, 1);
			} else if (b.length < 0x10000) {
				write(32 | 6);
				writeLong(b.length, 2);
			} else {
				write(32 | 7);
				writeLong(b.length, 4);
			}

			writeBytes(b);
			break;
		}

		case DataElement.BOOL:
			write(40 | 0);
			writeLong(d.getBoolean() ? 1 : 0, 1);
			break;

		case DataElement.DATSEQ:
			write(48 | 7);
			writeLong(getLength(d) - 5, 4);

			for (Enumeration e = (Enumeration) d.getValue(); e.hasMoreElements();) {
				writeElement((DataElement) e.nextElement());
			}

			break;
		case DataElement.DATALT:
			write(56 | 7);
			writeLong(getLength(d) - 5, 4);

			for (Enumeration e = (Enumeration) d.getValue(); e.hasMoreElements();) {
				writeElement((DataElement) e.nextElement());
			}

			break;

		case DataElement.URL: {
			byte[] b = ((String) d.getValue()).getBytes();

			if (b.length < 0x100) {
				write(64 | 5);
				writeLong(b.length, 1);
			} else if (b.length < 0x10000) {
				write(64 | 6);
				writeLong(b.length, 2);
			} else {
				write(64 | 7);
				writeLong(b.length, 4);
			}

			writeBytes(b);
			break;
		}

		default:
			throw new IOException();
		}
	}

}