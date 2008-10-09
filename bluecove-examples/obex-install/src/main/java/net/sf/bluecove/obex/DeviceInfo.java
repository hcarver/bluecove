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
 *  @version $Id$
 */
package net.sf.bluecove.obex;

import java.io.IOException;
import java.util.StringTokenizer;

class DeviceInfo implements Storable {

	String btAddress;

	String name;

	String obexUrl;

	boolean obexServiceFound = false;

	public String toString() {
		if ((name != null) && (name.length() > 0)) {
			return name;
		} else {
			return btAddress;
		}
	}

	private String fixNull(String str) {
		if (str.equalsIgnoreCase("null")) {
			return null;
		}
		return str;
	}

	public void loadFromLine(String line) throws IOException {
		StringTokenizer st = new StringTokenizer(line, "|");
		if (!st.hasMoreTokens()) {
			throw new IOException();
		}
		btAddress = fixNull(st.nextToken());
		if (!st.hasMoreTokens()) {
			throw new IOException();
		}
		name = fixNull(st.nextToken());
		if (!st.hasMoreTokens()) {
			throw new IOException();
		}
		obexUrl = fixNull(st.nextToken());
	}

	public String saveAsLine() {
		return btAddress + "|" + name + "|" + obexUrl;
	}

	public boolean isValid() {
		if ((obexUrl == null) || (btAddress == null)) {
			return false;
		}
		return (obexUrl.toLowerCase().indexOf("://" + btAddress.toLowerCase() + ":") != -1);
	}
}