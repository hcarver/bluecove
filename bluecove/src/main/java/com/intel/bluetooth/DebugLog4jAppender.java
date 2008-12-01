/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import com.intel.bluetooth.DebugLog.LoggerAppenderExt;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Log4j redirection wrapper
 *
 */
class DebugLog4jAppender implements LoggerAppenderExt {

	private static final String FQCN = DebugLog.class.getName();

	private Logger logger;

	DebugLog4jAppender() {
		logger = Logger.getLogger("com.intel.bluetooth");
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.DebugLog.LoggerAppender#appendLog(int, java.lang.String, java.lang.Throwable)
	 */
	public void appendLog(int level, String message, Throwable throwable) {
		switch (level) {
		case DebugLog.DEBUG:
			this.logger.log(FQCN, Level.DEBUG, message, throwable);
			break;
		case DebugLog.ERROR:
			this.logger.log(FQCN, Level.ERROR, message, throwable);
			break;
		}
	}

	/* (non-Javadoc)
	 * @see com.intel.bluetooth.DebugLog.LoggerAppenderExt#isLogEnabled(int)
	 */
	public boolean isLogEnabled(int level) {
		switch (level) {
		case DebugLog.DEBUG:
			return this.logger.isDebugEnabled();
		case DebugLog.ERROR:
			return true;
		default:
			return false;
		}
	}

}
