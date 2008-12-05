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
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * BlueCove log system.
 * 
 * If enabled "-Dbluecove.debug=true" System.out.println would be used for debug. Alternatively if log4j is available in
 * classpath Bluecove log would be redirected to log4j and can be enable using log4j configuration.
 * 
 * The methods of this class would be removed automaticaly because they are empty if debugCompiledOut = true. This class
 * itself will disappear from bytecode after obfuscation by proguard in this case. Change to create mini jar.
 * 
 */
public abstract class DebugLog {

	private static final boolean debugCompiledOut = false;

	public final static int DEBUG = 1;

	public final static int ERROR = 4;

	private static boolean debugEnabled = false;

	private static boolean initialized = false;

	private static boolean debugInternalEnabled = false;

	private static final String FQCN = DebugLog.class.getName();

	private static final Vector fqcnSet = new Vector();

	private static boolean java13 = false;

	private static Vector loggerAppenders = new Vector();

	/**
	 * Different log system can be injected in BlueCove using DebugLog.addAppender(customLoggerAppender)
	 * 
	 */
	public static interface LoggerAppender {
		public void appendLog(int level, String message, Throwable throwable);
	}

	public static interface LoggerAppenderExt extends LoggerAppender {
		public boolean isLogEnabled(int level);
	}

	static {
		fqcnSet.addElement(FQCN);
	}

	private DebugLog() {

	}

	private synchronized static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		debugEnabled = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG, false);
		if (debugEnabled && debugCompiledOut) {
			debugEnabled = false;
			System.err.println("BlueCove debug functions have been Compiled Out");
		}
		boolean useStdOut = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG_STDOUT, true);
		debugInternalEnabled = useStdOut && debugEnabled;
		boolean useLog4j = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_DEBUG_LOG4J, true);
		if (useLog4j) {
			try {
				LoggerAppenderExt log4jAppender = (LoggerAppenderExt) Class.forName(
						"com.intel.bluetooth.DebugLog4jAppender").newInstance();
				System.out.println("BlueCove log redirected to log4j");
				addAppender(log4jAppender);
				if (log4jAppender.isLogEnabled(DEBUG)) {
					debugEnabled = true || debugEnabled;
				}
			} catch (Throwable e) {
			}
		}
	}

	public static boolean isDebugEnabled() {
		if (!initialized) {
			initialize();
		}
		return debugEnabled;
	}

	public static void setDebugEnabled(boolean debugEnabled) {
		if (!initialized) {
			initialize();
		}
		if (debugEnabled && debugCompiledOut) {
			debugEnabled = false;
			System.err.println("BlueCove debug functions have been Compiled Out");
		} else {
			BlueCoveImpl.instance().enableNativeDebug(debugEnabled);
			DebugLog.debugEnabled = debugEnabled;
			DebugLog.debugInternalEnabled = DebugLog.debugEnabled;
		}
	}

	public static void debug(String message) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, null, null);
			printLocation();
			callAppenders(DEBUG, message, null);
		}
	}

	public static void debug(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", v);
			printLocation();
			callAppenders(DEBUG, message + " " + v, null);
		}
	}

	public static void debug(String message, Throwable t) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", t.toString());
			printLocation();
			// I have the reson not to make this as function.
			if (!UtilsJavaSE.javaSECompiledOut) {
				if (!UtilsJavaSE.ibmJ9midp) {
					t.printStackTrace(System.out);
				} else if (debugInternalEnabled) {
					t.printStackTrace();
				}
			} else {
				t.printStackTrace();
			}
			callAppenders(DEBUG, message, t);
		}
	}

	public static void debug(String message, Object obj) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", obj.toString());
			printLocation();
			callAppenders(DEBUG, message + " " + obj.toString(), null);
		}
	}

	public static void debug(String message, String v, String v2) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", v + " " + v2);
			printLocation();
			callAppenders(DEBUG, message + " " + v + " " + v2, null);
		}
	}

	public static void debug(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", Long.toString(v));
			printLocation();
			callAppenders(DEBUG, message + " " + Long.toString(v), null);
		}
	}

	public static void debug0x(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " 0x", Utils.toHexString(v));
			printLocation();
			callAppenders(DEBUG, message + " 0x" + Utils.toHexString(v), null);
		}
	}

	public static void debug0x(String message, String s, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " " + s + " 0x", Utils.toHexString(v));
			printLocation();
			callAppenders(DEBUG, message + " " + s + " 0x" + Utils.toHexString(v), null);
		}
	}

	public static void debug(String message, boolean v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", String.valueOf(v));
			printLocation();
			callAppenders(DEBUG, message + " " + v, null);
		}
	}

	public static void debug(String message, byte[] data) {
		debug(message, data, 0, (data == null) ? 0 : data.length);
	}

	public static void debug(String message, byte[] data, int off, int len) {
		if (!debugCompiledOut && isDebugEnabled()) {
			StringBuffer buf = new StringBuffer();
			if (data == null) {
				buf.append(" null byte[]");
			} else {
				buf.append(" [");
				for (int i = off; i < off + len; i++) {
					if (i != off) {
						buf.append(", ");
					}
					buf.append((new Byte(data[i])).toString());
				}
				buf.append("]");
				if (len > 4) {
					buf.append(" ").append(len);
				}
			}
			log(message, buf.toString(), null);
			printLocation();
			callAppenders(DEBUG, message + buf.toString(), null);
		}
	}

	public static void debug(String message, int[] data) {
		debug(message, data, 0, (data == null) ? 0 : data.length);
	}

	public static void debug(String message, int[] data, int off, int len) {
		if (!debugCompiledOut && isDebugEnabled()) {
			StringBuffer buf = new StringBuffer();
			if (data == null) {
				buf.append(" null int[]");
			} else {
				buf.append(" [");
				for (int i = off; i < off + len; i++) {
					if (i != off) {
						buf.append(", ");
					}
					buf.append(Integer.toString(data[i]));
				}
				buf.append("]");
				if (len > 4) {
					buf.append(" ").append(len);
				}
			}
			log(message, buf.toString(), null);
			printLocation();
			callAppenders(DEBUG, message + buf.toString(), null);
		}
	}

	public static void nativeDebugCallback(String fileName, int lineN, String message) {
		try {
			if ((fileName != null) && fileName.startsWith(".\\")) {
				fileName = fileName.substring(2);
			}
			DebugLog.debugNative(fileName + ":" + lineN, message);
		} catch (Throwable e) {
			try {
				System.out.println("Error when calling debug " + e);
			} catch (Throwable e2) {
				// We don't want any Exception propagate to Native Code.
			}
		}
	}

	public static void debugNative(String location, String message) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, "\n\t  ", location);
			callAppenders(DEBUG, message, null);
		}
	}

	public static void error(String message) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log("error ", message, null);
			printLocation();
			callAppenders(ERROR, message, null);
		}
	}

	public static void error(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log("error ", message, " " + v);
			printLocation();
			callAppenders(ERROR, message + " " + v, null);
		}
	}

	public static void error(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log("error ", message, " " + v);
			printLocation();
			callAppenders(ERROR, message + " " + v, null);
		}
	}

	public static void error(String message, Throwable t) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log("error ", message, " " + t);
			printLocation();
			// I have the reson not to make this as function.
			if (!UtilsJavaSE.javaSECompiledOut) {
				if (!UtilsJavaSE.ibmJ9midp) {
					t.printStackTrace(System.out);
				} else if (debugInternalEnabled) {
					t.printStackTrace();
				}
			} else {
				t.printStackTrace();
			}

			callAppenders(ERROR, message, t);
		}
	}

	public static void fatal(String message) {
		log("error ", message, null);
		printLocation();
		callAppenders(ERROR, message, null);
	}

	public static void fatal(String message, Throwable t) {
		log("error ", message, " " + t);
		printLocation();
		// I have the reson not to make this as function.
		if (!UtilsJavaSE.javaSECompiledOut) {
			if (!UtilsJavaSE.ibmJ9midp) {
				t.printStackTrace(System.out);
			} else if (debugInternalEnabled) {
				t.printStackTrace();
			}
		} else {
			t.printStackTrace();
		}

		callAppenders(ERROR, message, t);
	}

	private static void callAppenders(int level, String message, Throwable throwable) {
		for (Enumeration iter = loggerAppenders.elements(); iter.hasMoreElements();) {
			LoggerAppender a = (LoggerAppender) iter.nextElement();
			a.appendLog(level, message, throwable);
		}
	}

	public static void addAppender(LoggerAppender newAppender) {
		loggerAppenders.addElement(newAppender);
	}

	public static void removeAppender(LoggerAppender newAppender) {
		loggerAppenders.removeElement(newAppender);
	}

	private static String d00(int i) {
		if (i > 9) {
			return String.valueOf(i);
		} else {
			return "0" + String.valueOf(i);
		}
	}

	private static String d000(int i) {
		if (i > 99) {
			return String.valueOf(i);
		} else if (i > 9) {
			return "0" + String.valueOf(i);
		} else {
			return "00" + String.valueOf(i);
		}
	}

	private static void log(String message, String va1, String va2) {
		if (!debugInternalEnabled) {
			return;
		}
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date(System.currentTimeMillis()));

			StringBuffer sb;
			sb = new StringBuffer();
			sb.append(d00(calendar.get(Calendar.HOUR_OF_DAY))).append(":");
			sb.append(d00(calendar.get(Calendar.MINUTE))).append(":");
			sb.append(d00(calendar.get(Calendar.SECOND))).append(".");
			sb.append(d000(calendar.get(Calendar.MILLISECOND))).append(" ");

			sb.append(message);
			if (va1 != null) {
				sb.append(va1);
			}
			if (va2 != null) {
				sb.append(va2);
			}

			System.out.println(sb.toString());
		} catch (Throwable ignore) {
		}
	}

	private static void printLocation() {
		if (java13 || !debugInternalEnabled) {
			return;
		}
		try {
			UtilsJavaSE.StackTraceLocation ste = UtilsJavaSE.getLocation(fqcnSet);
			if (ste != null) {
				System.out.println("\t  " + formatLocation(ste));
			}
		} catch (Throwable e) {
			java13 = true;
		}
	}

	private static String formatLocation(UtilsJavaSE.StackTraceLocation ste) {
		if (ste == null) {
			return "";
		}
		// Make Line# clickable in eclipse
		return ste.className + "." + ste.methodName + "(" + ste.fileName + ":" + ste.lineNumber + ")";
	}

}
