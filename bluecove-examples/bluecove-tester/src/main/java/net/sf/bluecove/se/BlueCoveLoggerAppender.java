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