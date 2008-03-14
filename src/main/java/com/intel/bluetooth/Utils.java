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
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

/**
 * Conversion and JVM compatibility functions.
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 * @author vlads
 */
public abstract class Utils {

	private static final String blueCoveImplPackage = getPackage(MicroeditionConnector.class.getName());

	private Utils() {

	}

	private static String getPackage(String className) {
		int pStart = className.lastIndexOf('.');
		if (pStart == -1) {
			return "";
		} else {
			return className.substring(0, pStart);
		}
	}

	public static byte[] UUIDToByteArray(String uuidStringValue) {
		byte[] uuidValue = new byte[16];
		if (uuidStringValue.indexOf('-') != -1) {
			throw new NumberFormatException("The '-' character is not allowed in UUID: " + uuidStringValue);
		}
		for (int i = 0; i < 16; i++) {
			uuidValue[i] = (byte) Integer.parseInt(uuidStringValue.substring(i * 2, i * 2 + 2), 16);
		}
		return uuidValue;
	}

	static byte[] UUIDToByteArray(final UUID uuid) {
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

	static long UUIDTo32Bit(UUID uuid) {
		if (uuid == null) {
			return -1;
		}
		String str = uuid.toString().toUpperCase();
		int shortIdx = str.indexOf(BluetoothConsts.SHORT_UUID_BASE);
		if ((shortIdx != -1) && (shortIdx + BluetoothConsts.SHORT_UUID_BASE.length() == str.length())) {
			// This is short 16-bit or 32-bit UUID
			return Long.parseLong(str.substring(0, shortIdx), 16);
		}
		return -1;
	}

	static boolean is32Bit(UUID uuid) {
		return (UUIDTo32Bit(uuid) != -1);
	}

	public static int securityOpt(boolean authenticate, boolean encrypt) {
		int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
		if (authenticate) {
			if (encrypt) {
				security = ServiceRecord.AUTHENTICATE_ENCRYPT;
			} else {
				security = ServiceRecord.AUTHENTICATE_NOENCRYPT;
			}
		} else if (encrypt) {
			throw new IllegalArgumentException("Illegal encrypt configuration");
		}
		return security;
	}

	static boolean isStringSet(String str) {
		return ((str != null) && (str.length() > 0));
	}

	static String loadString(InputStream inputstream) {
		if (inputstream == null) {
			return null;
		}
		try {
			byte[] buf = new byte[256];
			int len = inputstream.read(buf);
			return new String(buf, 0, len);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				inputstream.close();
			} catch (IOException ignore) {
			}
		}
	}

	static String getResourceProperty(Class owner, String resourceName) {
		try {
			String value = loadString(owner.getResourceAsStream("/" + resourceName));
			if (value != null) {
				int cr = value.indexOf('\n');
				if (cr != -1) {
					value = value.substring(0, cr - 1);
				}
			}
			return value;
		} catch (Throwable e) {
			return null;
		}
	}

	/**
	 * Modifying the returned Object will not change the internal representation
	 * of the object.
	 * 
	 * @param value
	 * @return a clone of the array
	 */
	public static byte[] clone(byte[] value) {
		if (value == null) {
			return null;
		}
		int length = ((byte[]) value).length;
		byte[] bClone = new byte[length];
		System.arraycopy(value, 0, bClone, 0, length);
		return bClone;
	}

	static String newStringUTF8(byte bytes[]) {
		try {
			return new String(bytes, "UTF-8");
		} catch (IllegalArgumentException e) {
			return new String(bytes);
		} catch (UnsupportedEncodingException e) {
			return new String(bytes);
		}
	}

	static byte[] getUTF8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (IllegalArgumentException e) {
			return str.getBytes();
		} catch (UnsupportedEncodingException e) {
			return str.getBytes();
		}
	}

	static String newStringASCII(byte bytes[]) {
		try {
			return new String(bytes, "US-ASCII");
		} catch (IllegalArgumentException e) {
			return new String(bytes);
		} catch (UnsupportedEncodingException e) {
			return new String(bytes);
		}
	}

	static byte[] getASCIIBytes(String str) {
		try {
			return str.getBytes("US-ASCII");
		} catch (IllegalArgumentException e) {
			return str.getBytes();
		} catch (UnsupportedEncodingException e) {
			return str.getBytes();
		}
	}

	/**
	 * J2ME/J9 compatibility instead of Vector.toArray
	 * 
	 */
	static Object[] vector2toArray(Vector vector, Object[] anArray) {
		vector.copyInto(anArray);
		return anArray;
	}

	/**
	 * J2ME/J9 compatibility instead of Long.toHexString
	 * 
	 */
	public static String toHexString(long l) {
		StringBuffer buf = new StringBuffer();
		String lo = Integer.toHexString((int) l);
		if (l > 0xffffffffl) {
			String hi = Integer.toHexString((int) (l >> 32));
			buf.append(hi);
			for (int i = lo.length(); i < 8; i++) {
				buf.append('0');
			}
		}
		buf.append(lo);
		return buf.toString();
	}

	static void j2meUsagePatternDellay() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	}

	static class TimerThread extends Thread {

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
	 * Java 1.1 compatible. Schedules the specified task for execution after the
	 * specified delay.
	 * 
	 * @param delay
	 *            delay in milliseconds before task is to be executed.
	 * @param run
	 *            task to be scheduled.
	 */
	static TimerThread schedule(final long delay, final Runnable run) {
		TimerThread t = new TimerThread(delay, run);
		UtilsJavaSE.threadSetDaemon(t);
		t.start();
		return t;
	}

	public static void isLegalAPICall(Vector fqcnSet) throws Error {
		UtilsJavaSE.StackTraceLocation ste = UtilsJavaSE.getLocation(fqcnSet);
		if (ste != null) {
			if (ste.className.startsWith("javax.bluetooth.")) {
				return;
			}
			if (ste.className.startsWith(blueCoveImplPackage + ".")) {
				return;
			}
			throw new Error("Illegal use of the JSR-82 API");
		}
	}
}
