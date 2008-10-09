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

import net.sf.bluecove.Logger.LoggerAppender;

/**
 * Enables indirect connection to BlueCove internals if one present
 * 
 * @author vlads
 */
public class BlueCoveSpecific {

	public static void addAppender(LoggerAppender appender) {
		try {
			new BlueCoveLoggerAppender(appender);
		} catch (Throwable ignore) {
		}
	}
	
	public static boolean changeDebug() {
		try {
			return BlueCoveLoggerAppender.changeDebug();
		} catch (Throwable ignore) {
			return false;
		}
	}
	
	public static void removeAppender() {
		try {
			BlueCoveLoggerAppender.removeAppender();
		} catch (Throwable ignore) {
		}
	}
}
