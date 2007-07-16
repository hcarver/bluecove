/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.bluetooth.UUID;

public abstract class Utils {

	public static byte[] UUIDToByteArray(String uuidStringValue) {
		byte[] uuidValue = new byte[16];
		if(uuidStringValue.indexOf('-') != -1) {
            throw new NumberFormatException("The '-' character is not allowed in UUID: " + uuidStringValue);
		}
		for (int i = 0; i < 16; i++) {
			uuidValue[i] = (byte) Integer.parseInt(uuidStringValue.substring(i * 2, i * 2 + 2), 16);
		}
		return uuidValue;
	}
	
	public static byte[] UUIDToByteArray(UUID uuid) {
		return UUIDToByteArray(uuid.toString());
	}
	
	public static String UUIDByteArrayToString(byte[] uuidValue) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < uuidValue.length; i++) {
			buf.append(Integer.toHexString(uuidValue[i] >> 4 & 0xf));
			buf.append(Integer.toHexString(uuidValue[i] & 0xf));
		}
		return buf.toString();
	}
	
	public static int UUIDTo16Bit(UUID uuid) {
		if (uuid == null) {
			return -1;
		}
		String str = uuid.toString().toUpperCase();
		int shortIdx = str.indexOf(BluetoothConsts.SHORT_UUID_BASE);
		if ((shortIdx != -1) && (shortIdx + BluetoothConsts.SHORT_UUID_BASE.length() == str.length())) {
			// This is short 16-bit UUID
			return Integer.parseInt(str.substring(0, shortIdx), 16);
		}
		return -1;
	}
	
	public static boolean is16Bit(UUID uuid) {
		return (UUIDTo16Bit(uuid) != -1);
	}
	
	public static void readFully(InputStream is, byte[] b) throws IOException, EOFException {
		readFully(is, b, 0, b.length);
	}
	
	public static void readFully(InputStream is, byte[] b, int off, int len) throws IOException, EOFException {
		if (len < 0) {
		    throw new IndexOutOfBoundsException();
		}
		int got = 0;
		while (got < len) {
			int rc = is.read(b, off + got, len - got);
			if (rc < 0) {
				throw new EOFException();
			}
			got += rc;
		}
	}
	
	public static byte hiByte(int value) {
		return (byte)((value >> 8) & 0xFF);
	}
	
	public static byte loByte(int value) {
		return (byte)(0xFF & value);
	}

	public static int bytesToShort(byte valueHi, byte valueLo) {
		return ((((int)valueHi << 8) & 0xFF00) + (valueLo & 0xFF));
	}
	
	public static boolean isStringSet(String str) {
        return ((str != null) && (str.length() > 0));
	}
	
	private static String loadString(InputStream inputstream) {	
		if (inputstream == null) {
			return null;
		}
		try {
			byte[] buf = new byte[256];
			int len = inputstream.read(buf);
			return new String(buf,0, len);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				inputstream.close();
			} catch (IOException ignore) {
			}
		}
	}
	
	public static String getResourceProperty(Object owner, String resourceName) {
		try {
			String value = loadString(owner.getClass().getResourceAsStream(resourceName));
			int cr = value.indexOf('\n');
			if (cr != -1) {
				value = value.substring(1, cr);
			}
			return value;
		} catch (RuntimeException e) {
			return null;
		}
	}
	
	/**
	 * J2ME/J9 compatibility instead of Vector.toArray
	 * 
	 */
	public static Object[] vector2toArray(Vector vector, Object[] anArray) {
		vector.copyInto(anArray);
		return anArray;
	}
	
	public static String toHexString(long l) {
		StringBuffer buf = new StringBuffer();
		String lo = Integer.toHexString((int)l);
		if (l > 0xffffffffl) {
			String hi = Integer.toHexString((int)(l >> 32)); 
			buf.append(hi);
			for (int i = lo.length(); i < 8; i++) {
				buf.append('0');
			}
		}
		buf.append(lo);
		return buf.toString();
	}
	
	public static void j2meUsagePatternDellay() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	}
	
	public static class TimerThread extends Thread {
		
		long delay; 
		
		Runnable run;
		
		public TimerThread(long delay, Runnable run) {
			this.delay = delay;
			this.run = run;
		}
		
		public void run() {
			try {
				Thread.sleep(delay);
				run.run();
			} catch (InterruptedException e) {
			}
		}

	}
	/**
	 * Java 1.1 compatible. Schedules the specified task for execution after the specified delay.
	 * 
	 * @param delay delay in milliseconds before task is to be executed.
	 * @param run task to be scheduled.
	 */
	public static TimerThread schedule(final long delay, final Runnable run) {
		TimerThread t = new TimerThread(delay, run);
		UtilsJavaSE.threadSetDaemon(t);
		t.start();
		return t;
	}
}
