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
package com.intel.bluetooth;

/**
 * 
 * When ThreadLocal not available on Java 1.1 or MIDP will do nothing.
 * 
 * @author vlads
 * 
 */
class ThreadLocalWrapper {

	static boolean java11 = false;

	private Object threadLocal;

	private Object java11Object;

	ThreadLocalWrapper() {
		if (java11) {
			return;
		}
		try {
			threadLocal = new ThreadLocal();
		} catch (Throwable ejava11) {
			java11 = true;
		}
	}

	public Object get() {
		if (java11) {
			return java11Object;
		} else {
			return ((ThreadLocal) threadLocal).get();
		}
	}

	public void set(Object value) {
		if (java11) {
			java11Object = value;
		} else {
			((ThreadLocal) threadLocal).set(value);
		}
	}
}
