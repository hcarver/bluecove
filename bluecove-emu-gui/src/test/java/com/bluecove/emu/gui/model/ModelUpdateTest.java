package com.bluecove.emu.gui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import junit.framework.TestCase;

import com.intel.bluetooth.emu.MonitorDevice;

/**
 * @author vlads
 * 
 */
public class ModelUpdateTest extends TestCase implements Observer {

	private Model model;

	protected void setUp() throws Exception {
		super.setUp();
		try {
			model = Model.instance();
			model.addObserver(this);
		} catch (Exception e) {
		}
	}

	public void testUpdateDeviceList() throws IOException {
		List<MonitorDevice> newDevices1 = new ArrayList<MonitorDevice>();
		newDevices1.add(new MonitorDeviceMock(22, "22-1", 1));
		newDevices1.add(new MonitorDeviceMock(23, "23-1", 1));
		newDevices1.add(new MonitorDeviceMock(24, "24-1", 1));
		newDevices1.add(new MonitorDeviceMock(25, "25-1", 1));

		model.updateDeviceList(newDevices1);
		assertEquals(4, newDevices1.size());

		List<MonitorDevice> newDevices2 = new ArrayList<MonitorDevice>();
		newDevices2.add(new MonitorDeviceMock(32, "32-2", 1));
		newDevices2.add(new MonitorDeviceMock(23, "23-2", 1));
		newDevices2.add(new MonitorDeviceMock(24, "24-2", 1));
		newDevices2.add(new MonitorDeviceMock(35, "35-2", 1));
		model.updateDeviceList(newDevices2);
		assertEquals(4, newDevices2.size());
		// This is invalid tests since it depends on order in HashMap
		// assertEquals("23-2",
		// model.devices.get(0).getMonitorDevice().getDeviceDescriptor().getName());
		// assertEquals("24-2",
		// model.devices.get(1).getMonitorDevice().getDeviceDescriptor().getName());
		// assertEquals("32-2",
		// model.devices.get(2).getMonitorDevice().getDeviceDescriptor().getName());
		// assertEquals("35-2",
		// model.devices.get(3).getMonitorDevice().getDeviceDescriptor().getName());

	}

	public void update(Observable o, Object arg) {
		System.out.println(arg);
	}

}
