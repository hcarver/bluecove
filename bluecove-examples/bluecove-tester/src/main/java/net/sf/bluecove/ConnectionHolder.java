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

/**
 * @author vlads
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
