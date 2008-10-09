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

/**
 * @author vlads
 *
 */
public class FailureLog {

	public String name;
	
	public int countFailure = 0;
	
	private Vector logMessages = new Vector();
	
	public FailureLog(String name) {
		this.name = name;
	}
	
	public void clear() {
		countFailure = 0;
		logMessages = new Vector();
	}

	public void addFailure(String message, Throwable throwable) {
		StringBuffer buf = new StringBuffer(message);
		if (throwable != null) {
			buf.append(' ');
			String className = throwable.getClass().getName();
			buf.append(className.substring(1 + className.lastIndexOf('.')));
			buf.append(':');
			buf.append(throwable.getMessage());
		}
		addFailure(buf.toString());
	}
	
	public void addFailure(String message) {
		countFailure ++;
		logMessages.addElement(TimeUtils.timeNowToString() + " " + message);
	}
	
	public void writeToLog() {
		Logger.info(name + " " + countFailure);
		for (Enumeration iter = logMessages.elements(); iter.hasMoreElements();) {
			Logger.debug((String)iter.nextElement());
		}
	}
}
