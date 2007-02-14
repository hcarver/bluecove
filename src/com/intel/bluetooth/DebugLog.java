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

/**
 * The methods of this calls would be removed automaticaly because they are
 * empty if debugCompiledOut = true.
 * 
 * This class itself will disappear from bytecode after obfuscation by proguard.
 * 
 */
public class DebugLog {

	private static final boolean debugCompiledOut = true;
	
	private static boolean debugEnabled = false;

	private static boolean initialized = true;
	
	private static void initialize() {
		initialized = true;
		if (!debugCompiledOut) {
			String d = System.getProperty("bluecove.debug");
			debugEnabled = ((d != null) && (d.equalsIgnoreCase("true") || d.equalsIgnoreCase("1")));
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
		}
	}
	
	public static void debug(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v);
		}
	}

	public static void debug(String message, Object obj) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + obj.toString());
		}
	}
	
	public static void debug(String message, String v, String v2) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v + " " + v2);
		}
	}
	
	public static void debug(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + String.valueOf(v));
		}
	}

	public static void debug(String message, boolean v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println(message + " " + v);
		}
	}
	
	public static void error(String message, long v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + v);
		}
	}
	
	public static void error(String message, String v) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + v);
		}
	}
	
	public static void error(String message, Throwable t) {
		if (!debugCompiledOut && isDebugEnabled()) {
			System.out.println("error " + message + " " + t);
			t.printStackTrace();
		}
	}

}
