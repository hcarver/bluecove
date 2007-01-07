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
