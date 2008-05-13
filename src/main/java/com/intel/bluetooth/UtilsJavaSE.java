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

import java.security.PrivilegedActionException;
import java.util.Properties;
import java.util.Vector;

import com.ibm.oti.vm.VM;

/**
 * 
 * J2ME/J9 compatibility module.
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 * @author vlads
 */
public class UtilsJavaSE {

	static final boolean javaSECompiledOut = false;

	static class StackTraceLocation {

		public String className;

		public String methodName;

		public String fileName;

		public int lineNumber;
	}

	static boolean java13 = false;

	static boolean java14 = false;

	static final boolean ibmJ9midp = detectJ9midp();

	static final boolean canCallNotLoadedNativeMethod = !ibmJ9midp;

	private UtilsJavaSE() {

	}

	private static boolean detectJ9midp() {
		String ibmJ9config;
		try {
			ibmJ9config = System.getProperty("com.ibm.oti.configuration");
		} catch (SecurityException webstart) {
			return false;
		}
		return (ibmJ9config != null) && (ibmJ9config.indexOf("midp") != -1);
	}

	static StackTraceLocation getLocation(Vector fqcnSet) {
		if (java13 || ibmJ9midp) {
			return null;
		}
		if (!javaSECompiledOut) {
			if (!java14) {
				try {
					Class.forName("java.lang.StackTraceElement");
					java14 = true;
					DebugLog.debug("Java 1.4+ detected");
				} catch (ClassNotFoundException e) {
					java13 = true;
					return null;
				}
			}
			try {
				return getLocationJava14(fqcnSet);
			} catch (Throwable e) {
				java13 = true;
			}
		}
		return null;
	}

	private static StackTraceLocation getLocationJava14(Vector fqcnSet) {
		if (!UtilsJavaSE.javaSECompiledOut) {
			StackTraceElement[] ste = new Throwable().getStackTrace();
			for (int i = 0; i < ste.length - 1; i++) {
				if (fqcnSet.contains(ste[i].getClassName())) {
					String nextClassName = ste[i + 1].getClassName();
					if (nextClassName.startsWith("java.") || nextClassName.startsWith("sun.")) {
						continue;
					}
					if (!fqcnSet.contains(nextClassName)) {
						StackTraceElement st = ste[i + 1];
						StackTraceLocation loc = new StackTraceLocation();
						loc.className = st.getClassName();
						loc.methodName = st.getMethodName();
						loc.fileName = st.getFileName();
						loc.lineNumber = st.getLineNumber();
						return loc;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Marks the thread as a daemon thread. The Java Virtual Machine exits when
	 * the only threads running are all daemon threads.
	 * 
	 * @see java.lang.Thread#setDaemon(boolean)
	 */
	public static void threadSetDaemon(Thread thread) {
		try {
			if ((!javaSECompiledOut) && (!ibmJ9midp)) {
				thread.setDaemon(true);
			}
		} catch (Throwable javaJ9) {
		}
	}

	static boolean runtimeAddShutdownHook(Thread thread) {
		try {
			// since Java 1.3
			if (!javaSECompiledOut) {
				if (ibmJ9midp) {
					VM.addShutdownClass(thread);
					return true;
				} else {
					Runtime.getRuntime().addShutdownHook(thread);
					return true;
				}
			}
		} catch (Throwable java12) {
		}
		return false;
	}

	static void runtimeRemoveShutdownHook(Thread thread) {
		try {
			// since Java 1.3
			if ((!javaSECompiledOut) && (!ibmJ9midp)) {
				Runtime.getRuntime().removeShutdownHook(thread);
			}
		} catch (Throwable java12) {
		}
	}

	static void setSystemProperty(String propertyName, String propertyValue) {
		if (ibmJ9midp) {
			return;
		}
		boolean propertySet = false;
		try {
			Properties props = System.getProperties();
			if (propertyValue != null) {
				props.put(propertyName, propertyValue);
				propertySet = propertyValue.equals(System.getProperty(propertyName));
			} else {
				props.remove(propertyName);
				propertySet = (System.getProperty(propertyName) == null);
			}
		} catch (SecurityException e) {
		}
		if (!propertySet) {
			try {
				if (propertyValue != null) {
					System.setProperty(propertyName, propertyValue);
				} else {
					// Java 1.5 - OK
					System.clearProperty(propertyName);
				}
			} catch (Throwable java11) {
			}
		}
	}

	public static Throwable initCause(Throwable throwable, Throwable cause) {
		if ((!java14) || (cause == null)) {
			return throwable;
		}
		try {
			return throwable.initCause(cause);
		} catch (Throwable j9pp10) {
			return throwable;
		}

	}

	/**
	 * Support for JBM J9 PPRO 10
	 */
	static boolean isCurrentThreadInterrupted() {
		if (ibmJ9midp) {
			return false;
		}
		return Thread.interrupted();
	}

	/**
	 * Support for JBM J9 PPRO 10
	 */
	static Throwable getCause(PrivilegedActionException e) {
		try {
			return e.getCause();
		} catch (Throwable j9pp10) {
		}
		// Use older function
		try {
			return e.getException();
		} catch (Throwable ignore) {
		}
		return null;
	}
}
