/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

	static interface JavaSE5Features {

		public void clearProperty(String propertyName);

	}

	static final int javaSpecificationVersion = getJavaSpecificationVersion();

	static boolean java13 = false;

	static boolean java14 = false;

	static boolean detectJava5Helper = true;

	static JavaSE5Features java5Helper;

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

	private static int getJavaSpecificationVersion() {
		try {
			String javaV = System.getProperty("java.specification.version");
			if ((javaV == null) || (javaV.length() < 3)) {
				return 0;
			}
			return Integer.valueOf(javaV.charAt(2)).intValue();
		} catch (Throwable e) {
			return 0;
		}
	}

	private static void detectJava5Helper() {
		if (java13 || ibmJ9midp || (!detectJava5Helper) || (javaSpecificationVersion < 5)) {
			return;
		}
		detectJava5Helper = false;
		try {
			Class klass = Class.forName("com.intel.bluetooth.UtilsJavaSE5");
			java5Helper = (JavaSE5Features) klass.newInstance();
			DebugLog.debug("Use java 1.5+ features:", vmInfo());
		} catch (Throwable e) {
		}
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
					DebugLog.debug("Java 1.4+ detected:", vmInfo());
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

	static String vmInfo() {
		try {
			return System.getProperty("java.version") + "; " + System.getProperty("java.vm.name") + "; "
					+ System.getProperty("java.vendor");
		} catch (SecurityException ignore) {
			return "";
		}
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
			// Fall back if security managed allow to change only specific key
			try {
				if (propertyValue != null) {
					System.setProperty(propertyName, propertyValue);
				} else {
					detectJava5Helper();
					if (java5Helper != null) {
						java5Helper.clearProperty(propertyName);
					}
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
