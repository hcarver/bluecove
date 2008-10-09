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

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryListener;

/**
 * This is Common class to solve JNI call backs problem
 *
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 */
class DeviceInquiryThread extends Thread {

	private BluetoothStack stack;

	private DeviceInquiryRunnable inquiryRunnable;

	private int accessCode;

	private DiscoveryListener listener;

	private BluetoothStateException startException;

	private boolean started = false;

	private boolean terminated = false;

	private Object inquiryStartedEvent = new Object();

	private static int threadNumber;

	private static synchronized int nextThreadNum() {
		return threadNumber++;
	}

	private DeviceInquiryThread(BluetoothStack stack, DeviceInquiryRunnable inquiryRunnable, int accessCode,
			DiscoveryListener listener) {
		super("DeviceInquiryThread-" + nextThreadNum());
		this.stack = stack;
		this.inquiryRunnable = inquiryRunnable;
		this.accessCode = accessCode;
		this.listener = listener;
	}

	/**
	 * Start DeviceInquiry and wait for startException or
	 * deviceInquiryStartedCallback
	 */
	static boolean startInquiry(BluetoothStack stack, DeviceInquiryRunnable inquiryRunnable, int accessCode,
			DiscoveryListener listener) throws BluetoothStateException {
		DeviceInquiryThread t = (new DeviceInquiryThread(stack, inquiryRunnable, accessCode, listener));
		// In case the BTStack hangs, exit JVM anyway
		UtilsJavaSE.threadSetDaemon(t);
		synchronized (t.inquiryStartedEvent) {
			t.start();
			while (!t.started && !t.terminated) {
				try {
					t.inquiryStartedEvent.wait();
				} catch (InterruptedException e) {
					return false;
				}
				if (t.startException != null) {
					throw t.startException;
				}
			}
		}
		DebugLog.debug("startInquiry return", t.started);
		return t.started;
	}

	public static int getConfigDeviceInquiryDuration() {
		String duration = BlueCoveImpl.getConfigProperty(BlueCoveConfigProperties.PROPERTY_INQUIRY_DURATION);
		if (duration != null) {
			return Integer.parseInt(duration);
		} else {
			return BlueCoveConfigProperties.PROPERTY_INQUIRY_DURATION_DEFAULT;
		}
	}

	public void run() {
		int discType = DiscoveryListener.INQUIRY_ERROR;
		try {
			BlueCoveImpl.setThreadBluetoothStack(stack);
			discType = inquiryRunnable.runDeviceInquiry(this, accessCode, listener);
		} catch (BluetoothStateException e) {
			DebugLog.debug("runDeviceInquiry throw", e);
			startException = e;
		} catch (Throwable e) {
			DebugLog.error("runDeviceInquiry", e);
			// Fine, If Not started then startInquiry return false
		} finally {
			terminated = true;
			synchronized (inquiryStartedEvent) {
				inquiryStartedEvent.notifyAll();
			}
			DebugLog.debug("runDeviceInquiry ends");
			if (started) {
				Utils.j2meUsagePatternDellay();
				listener.inquiryCompleted(discType);
			}
		}
	}

	public void deviceInquiryStartedCallback() {
		DebugLog.debug("deviceInquiryStartedCallback");
		started = true;
		synchronized (inquiryStartedEvent) {
			inquiryStartedEvent.notifyAll();
		}
	}

}
