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
package net.sf.bluecove;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.bluecove.util.TimeUtils;


public class Logger {

	public final static int DEBUG = 1;

	public final static int INFO = 2;

	public final static int WARN = 3;

	public final static int ERROR = 4;
	
	private static Vector loggerAppenders = new Vector();
	
	public static interface LoggerAppender {
		public void appendLog(int level, String message, Throwable throwable);
	}

	private static void systemOutTimeStamp() {
		if (Configuration.logTimeStamp) {
			System.out.print(TimeUtils.timeStampNowToString());
			System.out.print(" ");
		}
	}
	
	/**
	 * We want to ingore Error when writing to console on Windows.
	 *  e.g.  java.lang.NullPointerException
 	 *  at java.io.PrintStream.write(Unknown Source)
	 */
	public static void debug(String message) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {}
		callAppenders(DEBUG, message, null);
	}
	
	public static void debug(String message, byte[] data) {
		debug(message, data, 0, (data==null)?0:data.length);
	}
	
	public static void debug(String message, byte[] data, int off, int len) {
		StringBuffer buf = new StringBuffer(message);
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
		try {
			systemOutTimeStamp();
			System.out.println(buf.toString());
		} catch (Throwable ignore) {}
		callAppenders(DEBUG, buf.toString(), null);
	}
	
	public static void debug(String message, Throwable t) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {}
		callAppenders(DEBUG, message, t);
	}
	
	public static void info(String message) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {}
		callAppenders(INFO, message, null);
	}

	public static void warn(String message) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {}
		callAppenders(WARN, message, null);
	}
	
	public static void error(String message, Throwable t) {
		try {
			systemOutTimeStamp();
			System.out.println("error " + message + " " + t);
			if (t != null) {
				t.printStackTrace();
			}
		} catch (Throwable ignore) {}
		callAppenders(ERROR, message, t);
	}

	public static void error(String message) {
		try {
			systemOutTimeStamp();
			System.out.println("error " + message);
		} catch (Throwable ignore) {}
		callAppenders(ERROR, message, null);
	}
	
	public static void addAppender(LoggerAppender newAppender) {
		loggerAppenders.addElement(newAppender);
	}
	
	public static void removeAppender(LoggerAppender newAppender) {
		loggerAppenders.removeElement(newAppender);
	}
	
    private static void callAppenders(int level, String message, Throwable throwable) {
		for (Enumeration iter = loggerAppenders.elements(); iter.hasMoreElements();) {
			LoggerAppender a = (LoggerAppender) iter.nextElement();
			a.appendLog(level, message, throwable);
		};
	}
}
