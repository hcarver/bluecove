package com.bluecove.emu.gui.model;

import com.intel.bluetooth.emu.MonitorDevice;

public class Device extends Datum {

	private MonitorDevice monitorDevice;
	
	public Device(long id) {
		super(id);
	}

	public Device(long id, MonitorDevice monitorDevice) {
		super(id);
		this.monitorDevice = monitorDevice;
	}

	public MonitorDevice getMonitorDevice() {
		return monitorDevice;
	}

	public void setMonitorDevice(MonitorDevice monitorDevice) {
		this.monitorDevice = monitorDevice;
		setChanged();
		notifyObservers(this);
	}

}
