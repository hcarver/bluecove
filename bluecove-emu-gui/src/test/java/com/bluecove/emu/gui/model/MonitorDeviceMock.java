package com.bluecove.emu.gui.model;

import com.intel.bluetooth.emu.DeviceDescriptor;
import com.intel.bluetooth.emu.MonitorDevice;

public class MonitorDeviceMock extends MonitorDevice {
	
	public MonitorDeviceMock(long id, String name, int deviceClass) {
		deviceDescriptor = new com.intel.bluetooth.emu.DeviceDescriptor(id,name,deviceClass);
	}

}
