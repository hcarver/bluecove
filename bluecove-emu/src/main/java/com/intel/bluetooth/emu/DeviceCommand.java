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

import java.io.Serializable;

/**
 * @author vlads
 * 
 */
public class DeviceCommand implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum DeviceCommandType {
		keepAlive, chagePowerState, updateLocalDeviceProperties, createThreadDumpStdOut, createThreadDumpFile, shutdownJVM
	};

	final static DeviceCommand keepAliveCommand = new DeviceCommand(DeviceCommandType.keepAlive);

	private DeviceCommandType type;

	private Object[] parameters;

	public DeviceCommand(DeviceCommandType type) {
		this.type = type;
	}

	public DeviceCommand(DeviceCommandType type, Object parameter) {
		this(type);
		parameters = new Object[1];
		parameters[0] = parameter;
	}

	public DeviceCommandType getType() {
		return this.type;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

}
