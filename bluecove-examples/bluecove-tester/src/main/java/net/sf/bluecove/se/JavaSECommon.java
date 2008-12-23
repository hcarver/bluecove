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
package net.sf.bluecove.se;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import net.sf.bluecove.Configuration;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.CLDCStub;
import org.bluecove.tester.util.IOUtils;
import org.bluecove.tester.util.RuntimeDetect;

public class JavaSECommon implements CLDCStub {

	private static boolean initialized = false;

	public static void initOnce() {
		if (initialized) {
			return;
		}
		initialized = true;
		Configuration.logTimeStamp = true;
		Logger.addAppender(new LoggerJavaSEAppender());

		if (Configuration.serverAcceptWhileConnectedOnJavaSE) {
			Configuration.serverAcceptWhileConnected.setValue(true);
			// Configuration.testIgnoreNotWorkingServiceAttributes = false;
		}

		RuntimeDetect.cldcStub = new JavaSECommon();
	}

	public boolean canInterruptThread() {
		return true;
	}

	public void interruptThread(Thread t) {
		if (t != null) {
			t.interrupt();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.CLDCStub#createNamedThread(java.lang.Runnable, java.lang.String)
	 */
	public Thread createNamedThread(Runnable target, String name) {
		return new Thread(target, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.CLDCStub#setThreadLocalBluetoothStack(java.lang.Object)
	 */
	public void setThreadLocalBluetoothStack(Object id) {
		LocalDeviceManager.setThreadLocalBluetoothStack(id);
	}

	public static boolean isJava5() {
		try {
			return java5Function();
		} catch (Throwable e) {
			return false;
		}
	}

	static boolean java5Function() {
		return (Thread.currentThread().getStackTrace() != null);
	}

	public static void threadDump() {
		SimpleDateFormat fmt = new SimpleDateFormat("MM-dd_HH-mm-ss");
		OutputStreamWriter out = null;
		try {
			File file = new File("ThreadDump-" + fmt.format(new Date()) + ".log");
			out = new FileWriter(file);
			Map traces = Thread.getAllStackTraces();
			for (Iterator iterator = traces.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				Thread thread = (Thread) entry.getKey();
				out.write("Thread= " + thread.getName() + " " + (thread.isDaemon() ? "daemon" : "") + " prio="
						+ thread.getPriority() + "id=" + thread.getId() + " " + thread.getState());
				out.write("\n");

				StackTraceElement[] ste = (StackTraceElement[]) entry.getValue();
				for (int i = 0; i < ste.length; i++) {
					out.write("\t");
					out.write(ste[i].toString());
					out.write("\n");
				}
				out.write("---------------------------------\n");
			}
			out.close();
			out = null;
			Logger.info("Full ThreadDump created " + file.getAbsolutePath());
		} catch (Throwable ignore) {
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
}
