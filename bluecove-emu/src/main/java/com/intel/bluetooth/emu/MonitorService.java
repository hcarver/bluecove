/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
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
package com.intel.bluetooth.emu;

import java.io.IOException;

/**
 * @author vlads
 * 
 */
public class MonitorService implements MonitorItem {

	private static final long serialVersionUID = 1L;

	private long device;

	private String portId;

	private long internalHandle;

	private boolean listening;

	private int connectionCount;

	private int sdpAttributes;

	private int sdpSize;

	private String displayName;

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	public long getDevice() {
		return device;
	}

	public boolean isListening() {
		return listening;
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public String getPortId() {
		return portId;
	}

	public long getInternalHandle() {
		return internalHandle;
	}

	public int getSdpAttributes() {
		return sdpAttributes;
	}

	public int getSdpSize() {
		return sdpSize;
	}

	public String getDisplayName() {
		return displayName;
	}
}
