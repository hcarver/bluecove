/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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

import com.intel.bluetooth.DebugLog.LoggerAppenderExt;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Log4j wrapper
 * 
 * @author vlads
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
