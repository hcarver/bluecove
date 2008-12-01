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
package net.sf.bluecove.se;

import com.intel.bluetooth.DebugLog;

import net.sf.bluecove.Logger.LoggerAppender;

class BlueCoveLoggerAppender implements com.intel.bluetooth.DebugLog.LoggerAppender {
	
	static Object blueCoveLoggerAppender;
	
	net.sf.bluecove.Logger.LoggerAppender appender;
	
	public BlueCoveLoggerAppender(LoggerAppender appender) {
		this.appender = appender;
		DebugLog.addAppender(this);
		blueCoveLoggerAppender = this;
	}
	
	public void appendLog(int level, String message, Throwable throwable) {
		appender.appendLog(level, message, throwable);
	}
	
	public static void removeAppender() {
		DebugLog.removeAppender((BlueCoveLoggerAppender)blueCoveLoggerAppender);
	}
	
	public static boolean changeDebug() {
		boolean dbg = !com.intel.bluetooth.DebugLog.isDebugEnabled();
		if (!dbg) {
			DebugLog.debug("BlueCove Disable debug");
		}
		DebugLog.setDebugEnabled(dbg);
		if (dbg) {
			DebugLog.debug("BlueCove Debug enabled");
		}
		return dbg;
	}
}