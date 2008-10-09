/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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

/**
 * @author vlads
 * 
 */
public class LongRunningTestMonitor extends Thread {

	private boolean testFinished = false;

	private Thread testThread;

	private int gracePeriod = 0;

	private String testName;

	LongRunningTestMonitor(int gracePeriod, String testName) {
		super("TestMonitor");
		this.gracePeriod = gracePeriod;
		this.testName = testName;
		testThread = Thread.currentThread();
	}

	public void run() {
		try {
			sleep(gracePeriod);
		} catch (InterruptedException e) {
			return;
		}

		int count = 0;
		while (!testFinished) {

			try {
				sleep(4 * 1000);
			} catch (InterruptedException e) {
				return;
			}

			if (!testFinished) {
				System.out.println("Long running test " + testName + " detected in thread:" + testThread.getName());
				StackTraceElement[] ste = testThread.getStackTrace();
				StringBuffer buf = new StringBuffer();
				buf.append("stack trace:\n");
				for (int i = 0; i < ste.length; i++) {
					buf.append("\t").append(ste[i].toString()).append('\n');
					if (ste[i].getClassName().startsWith("junit.framework")) {
						break;
					}
				}
				System.out.println(buf.toString());
				count++;
				if (count > 4) {
					System.out.println("Sending ThreadDeath");
					testThread.stop();
					break;
				} else {
					System.out.println("Sending InterruptedException");
					testThread.interrupt();
				}
			}
		}
	}

	public void finish() {
		testFinished = true;
		interrupt();
	}

}
