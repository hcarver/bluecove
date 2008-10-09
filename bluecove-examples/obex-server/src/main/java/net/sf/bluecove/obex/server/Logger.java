/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex.server;

/**
 * @author vlads
 * 
 */
public class Logger {

	static void debug(String message) {
		System.out.println(message);
	}

	static void debug(String message, Object o) {
		System.out.println(message + " " + o);
	}

	static void debug(String message, Throwable e) {
		System.out.println(message + " " + e.getMessage());
		e.printStackTrace();
	}

	static void debug(Throwable e) {
		System.out.println(e.getMessage());
		e.printStackTrace();
	}

	static void error(String message) {
		System.out.println(message);
	}

	static void error(String message, Throwable e) {
		System.out.println(message + " " + e.getMessage());
		e.printStackTrace();
	}

}
