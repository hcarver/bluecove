/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
 *  @version $Id: NativeTestCase.java 1570 2008-01-16 22:15:56Z skarzhevskyy $
 */
package com.intel.bluetooth;

import junit.framework.TestCase;

/**
 * Base class for test cases that are calling native function.
 * 
 * Native Debug automatically enabled when running tests in Eclipse
 * 
 * @author vlads
 * 
 */
public abstract class NativeTestCase extends TestCase {

	// Use this to debug tests
	protected boolean debug = false;

	protected boolean debugOnInEclipse = true;

	protected void setUp() throws Exception {
		super.setUp();

		boolean eclipse = isEclipse();

		// Use this to avoid project refresh in Eclipse after dll build in VC
		if (eclipse) {
			System.getProperties().put("bluecove.native.path", "./src/main/resources");
		}

		if (eclipse && debugOnInEclipse) {
			debug = true;
		}

		// Use this to debug tests
		if (debug) {
			System.getProperties().put("bluecove.debug", "true");
			BlueCoveImpl.instance().enableNativeDebug(true);
		}

		BluetoothStack anyStack = new BluetoothStackBlueZ();
		BlueCoveImpl.loadNativeLibraries(anyStack);
	}

	boolean isEclipse() {
		StackTraceElement[] ste = new Throwable().getStackTrace();
		return (ste[ste.length - 1].getClassName().startsWith("org.eclipse.jdt"));
	}

}
