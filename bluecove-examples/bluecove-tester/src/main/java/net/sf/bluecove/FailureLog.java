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
package net.sf.bluecove;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.bluecove.util.TimeUtils;

/**
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
