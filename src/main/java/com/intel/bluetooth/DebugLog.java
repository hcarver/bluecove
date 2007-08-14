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

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * BlueCove log system. If enabled "-Dbluecove.debug=true" System.out.println would be used for debug.
 * 
 * @author vlads
 *
 */
/*
 * The methods of this calls would be removed automaticaly because they are
 * empty if debugCompiledOut = true.
 * This class itself will disappear from bytecode after obfuscation by proguard.
 * 
 */
public abstract class DebugLog {

	private static final boolean debugCompiledOut = false;

	public final static int DEBUG = 1;

	public final static int ERROR = 4;
	
	private static boolean debugEnabled = false;

	private static boolean initialized = false;
	
	private static final String FQCN = DebugLog.class.getName();
	
	//private static final Set fqcnSet = new HashSet();
	private static final Vector fqcnSet = new Vector(); 
	
	private static boolean java13 = false;

	private static Vector loggerAppenders = new Vector();
	
	/**
	 * Different log system can be injected in BlueCove using DebugLog.addAppender(customLoggerAppender)
	 * 
	 * @author vlads
	 *
	 */
	public static interface LoggerAppender {
		public void appendLog(int level, String message, Throwable throwable);
	}

	static {
		fqcnSet.addElement(FQCN);
	}
	
	private DebugLog() {
		
	}
	
	private static void initialize() {
		initialized = true;
		String d = System.getProperty("bluecove.debug");
		debugEnabled = ((d != null) && (d.equalsIgnoreCase("true") || d.equalsIgnoreCase("1")));
		if (debugEnabled && debugCompiledOut) {
			debugEnabled = false;
			System.err.println("BlueCove debug functions have been Compiled Out");
		}
	}
	
	public static boolean isDebugEnabled() {
		if (!initialized) {
			initialize();
		}
		return debugEnabled;
	}
	
	public static void setDebugEnabled(boolean debugEnabled) {
		//boolean initialized = DebugLog.initialized;
		initialize();
		if (debugEnabled && debugCompiledOut) {
			debugEnabled = false;
			System.err.println("BlueCove debug functions have been Compiled Out");
		} else {
			BlueCoveImpl.instance().enableNativeDebug(debugEnabled);
			DebugLog.debugEnabled = debugEnabled;
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
				} else {
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
			log(message, " ", String.valueOf(v));
			printLocation();
			callAppenders(DEBUG, message + " " + String.valueOf(v), null);
		}
	}

	public static void debug0x(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " 0x", Utils.toHexString(v));
			printLocation();
			callAppenders(DEBUG, message + " 0x" + Utils.toHexString(v), null);
		}
	}
	
	public static void debug(String message, boolean v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			log(message, " ", String.valueOf(v));
			printLocation();
			callAppenders(DEBUG, message + " " + v, null);
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
			log("error ", message,  " " + v);
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
				} else {
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
			} else {
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
		} catch (Throwable ignore) {}
	}
	
	private static void printLocation() {
		if (java13) {
			return;
		}
		try {
			UtilsJavaSE.StackTraceLocation ste = UtilsJavaSE.getLocation(fqcnSet);
			if (ste != null) {
				System.out.println("\t  "+ formatLocation(ste));
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
