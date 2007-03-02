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

import java.util.HashSet;
import java.util.Set;

/**
 * The methods of this calls would be removed automaticaly because they are
 * empty if debugCompiledOut = true.
 * 
 * This class itself will disappear from bytecode after obfuscation by proguard.
 * 
 */
public class DebugLog {

	private static final boolean debugCompiledOut = false;
	
	private static boolean debugEnabled = false;

	private static boolean initialized = false;
	
	private static final String FQCN = DebugLog.class.getName();
	
	private static final Set fqcnSet = new HashSet();
	
	private static boolean java13 = false;
	
	 static {
	      fqcnSet.add(FQCN);
	 }
	
	private static void initialize() {
		initialized = true;
		String d = System.getProperty("bluecove.debug");
		debugEnabled = ((d != null) && (d.equalsIgnoreCase("true") || d.equalsIgnoreCase("1")));
		if (debugEnabled && debugCompiledOut) {
			System.err.println("BlueCove debug functions have been Compiled Out");
		}
	}
	
	public static boolean isDebugEnabled() {
		if (!initialized) {
			initialize();
		}
		return debugEnabled;
	}
	
	public static void debug(String message) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message);
			printLocation();
		}
	}
	
	public static void debug(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v);
			printLocation();
		}
	}

	public static void debug(String message, Object obj) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + obj.toString());
			printLocation();
		}
	}
	
	public static void debug(String message, String v, String v2) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v + " " + v2);
			printLocation();
		}
	}
	
	public static void debug(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + String.valueOf(v));
			printLocation();
		}
	}

	public static void debug(String message, boolean v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v);
			printLocation();
		}
	}
	
	public static void error(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + v);
			printLocation();
		}
	}
	
	public static void error(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + v);
			printLocation();
		}
	}
	
	public static void error(String message, Throwable t) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + t);
			printLocation();
			t.printStackTrace(System.out);
		}
	}

	public static void fatal(String message, Throwable t) {
		System.out.println("error " + message + " " + t);
		printLocation();
		t.printStackTrace(System.out);
	}
	
	private static void printLocation() {
		if (java13) {
			return;
		}
		try {
		 System.out.println("\t  "+ formatLocation(getLocation()));
		} catch (Throwable e) {
			java13 = true;
		}
	}
	
	private static String formatLocation(StackTraceElement ste) {
		if (ste == null) {
			return "";
		}
		// Make Line# clickable in eclipse
		return ste.getClassName() + "." + ste.getMethodName() + "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")";
	}
	
    private static StackTraceElement getLocation() {
		if (java13) {
			return null;
		}
		try {
			StackTraceElement[] ste = new Throwable().getStackTrace();
			for (int i = 0; i < ste.length - 1; i++) {
				if (fqcnSet.contains(ste[i].getClassName())) {
					String nextClassName = ste[i + 1].getClassName();
					if (nextClassName.startsWith("java.") || nextClassName.startsWith("sun.")) {
						continue;
					}
					if (!fqcnSet.contains(nextClassName)) {
						return ste[i + 1];
					}
				}
			}
		} catch (Throwable e) {
			java13 = true;
		}
		return null;
	}

}
