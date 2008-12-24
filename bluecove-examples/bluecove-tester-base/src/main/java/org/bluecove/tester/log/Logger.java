/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package org.bluecove.tester.log;

import java.util.Enumeration;
import java.util.Vector;

import org.bluecove.tester.util.StringUtils;
import org.bluecove.tester.util.TimeUtils;

public class Logger {

	public final static int DEBUG = 1;

	public final static int INFO = 2;

	public final static int WARN = 3;

	public final static int ERROR = 4;

	public static boolean logTimeStamp = false;

	private static Vector loggerAppenders = new Vector();

	private static void systemOutTimeStamp() {
		if (logTimeStamp) {
			System.out.print(TimeUtils.timeStampNowToString());
			System.out.print(" ");
		}
	}

	/**
	 * We want to ingore Error when writing to console on Windows. e.g. java.lang.NullPointerException at
	 * java.io.PrintStream.write(Unknown Source)
	 */
	public static void debug(String message) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {
		}
		callAppenders(DEBUG, message, null);
	}

	public static void debug(String message, String data) {
		debug(message + " " + data);
	}

	public static void debug(String message, int data) {
		debug(message, String.valueOf(data));
	}

	public static void debug(String message, boolean data) {
		debug(message, ((data) ? "true" : "false"));
	}

	public static void debug(String message, byte[] data) {
		debug(message, data, 0, (data == null) ? 0 : data.length);
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
		} catch (Throwable ignore) {
		}
		callAppenders(DEBUG, buf.toString(), null);
	}

	public static void debug(String message, Throwable t) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {
		}
		callAppenders(DEBUG, message, t);
	}

	public static void info(String message) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {
		}
		callAppenders(INFO, message, null);
	}

	public static void warn(String message) {
		try {
			systemOutTimeStamp();
			System.out.println(message);
		} catch (Throwable ignore) {
		}
		callAppenders(WARN, message, null);
	}

	public static void error(String message, Throwable t) {
		try {
			systemOutTimeStamp();
			System.out.println("error " + message + " " + t);
			if (t != null) {
				t.printStackTrace();
			}
		} catch (Throwable ignore) {
		}
		callAppenders(ERROR, message, t);
	}

	public static void error(String message) {
		try {
			systemOutTimeStamp();
			System.out.println("error " + message);
		} catch (Throwable ignore) {
		}
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
		}
	}
	
    public static void runGarbageCollector() {
        Runtime runtime = Runtime.getRuntime();
        long initialFree = runtime.freeMemory();
        runtime.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            return;
        }
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        Logger.info("Mem Total " + StringUtils.formatLong(total));
        Logger.info("Mem Used  " + StringUtils.formatLong(total - free));
        Logger.info("Mem GC    " + StringUtils.formatLong(free - initialFree));
    }
}

