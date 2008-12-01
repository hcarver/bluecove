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

/**
 * 
 */
public abstract class ConnectionHolder implements CanShutdown {

	long lastActivityTime;

	int testTimeOutSec;

	int concurrentCount = 0;

	protected Vector concurrentConnections;

	boolean connectionOpen = true;

	ConnectionHolder() {
		active();
	}

	public void active() {
		lastActivityTime = System.currentTimeMillis();
	}

	public long lastActivityTime() {
		return lastActivityTime;
	}

	public void registerConcurrent(Vector concurrentConnections) {
		this.concurrentConnections = concurrentConnections;
		synchronized (concurrentConnections) {
			concurrentConnections.addElement(this);
		}
	}

	public void concurrentNotify() {
		synchronized (concurrentConnections) {
			int concurNow = concurrentConnections.size();
			setConcurrentCount(concurNow);
			if (concurNow > 1) {
				// Update all other working Threads
				for (Enumeration iter = concurrentConnections.elements(); iter.hasMoreElements();) {
					ConnectionHolder t = (ConnectionHolder) iter.nextElement();
					t.setConcurrentCount(concurNow);
				}
			}
		}
	}

	public void disconnected() {
		setConnectionOpen(false);
		if (concurrentConnections != null) {
			synchronized (concurrentConnections) {
				concurrentConnections.removeElement(this);
			}
		}
	}

	private void setConcurrentCount(int concurNow) {
		if (concurrentCount < concurNow) {
			concurrentCount = concurNow;
		}
	}

	public int getTestTimeOutSec() {
		return this.testTimeOutSec;
	}

	public void setTestTimeOutSec(int testTimeOutSec) {
		this.testTimeOutSec = testTimeOutSec;
	}

	public boolean isConnectionOpen() {
		return this.connectionOpen;
	}

	public void setConnectionOpen(boolean connectionOpen) {
		this.connectionOpen = connectionOpen;
	}

}
