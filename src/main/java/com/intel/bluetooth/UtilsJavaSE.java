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

import java.util.Vector;

/**
 * @author vlads
 *
 * J2ME/J9 compatibility module.
 * 
 */
public class UtilsJavaSE {
	
	public static final boolean javaSECompiledOut = false;
	
	public static class StackTraceLocation {
		
		public String className;
		
		public String methodName;
		
		public String fileName;
		
		public int lineNumber;
	}
	
	public static boolean java13 = false;
	
	public static boolean java14 = false;
	
	public static final boolean ibmJ9midp = detectJ9midp();
	
	private static boolean detectJ9midp() {
		String ibmJ9config = System.getProperty("com.ibm.oti.configuration");
		return (ibmJ9config != null) &&  (ibmJ9config.indexOf("midp") != -1);
	}
	
	public static StackTraceLocation getLocation(Vector fqcnSet) {
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
	
	public static StackTraceLocation getLocationJava14(Vector fqcnSet) {
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
	
	public static void threadSetDaemon(Thread thread) {
		try {
			if ((!javaSECompiledOut) && (!ibmJ9midp)) {
				thread.setDaemon(true);
			}
		} catch (Throwable javaJ9) {
		}
	}
	
	public static void runtimeAddShutdownHook(Thread thread) {
		try {
			// since Java 1.3
			if ((!javaSECompiledOut) && (!ibmJ9midp)) {
				Runtime.getRuntime().addShutdownHook(thread);
			}
		} catch (Throwable java12) {
		}
	}
}
