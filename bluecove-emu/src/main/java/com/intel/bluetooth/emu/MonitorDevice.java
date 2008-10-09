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
public class MonitorDevice implements MonitorItem {

	private static final long serialVersionUID = 1L;

	private transient Device device;

	protected DeviceDescriptor deviceDescriptor;

	private boolean hasServices;

	private boolean listening;

	private Long[] connectedTo;

	protected MonitorDevice() {

	}

	MonitorDevice(Device device) {
		this.device = device;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		updateFields();
		out.defaultWriteObject();
	}

	protected void updateFields() {
		if (device != null) {
			deviceDescriptor = device.getDescriptor();
			hasServices = device.isHasServices();
			listening = device.isListening();
			connectedTo = device.getConnectedTo();
		}
	}

	public DeviceDescriptor getDeviceDescriptor() {
		return deviceDescriptor;
	}

	public boolean isHasServices() {
		return hasServices;
	}

	public boolean isListening() {
		return listening;
	}

	public Long[] getConnectedTo() {
		return connectedTo;
	}

}
