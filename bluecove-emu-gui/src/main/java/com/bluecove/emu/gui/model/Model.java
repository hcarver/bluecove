package com.bluecove.emu.gui.model;

import static com.bluecove.emu.gui.model.DatumNotification.Type.ADDED;
import static com.bluecove.emu.gui.model.DatumNotification.Type.REMOVED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Map.Entry;

import com.intel.bluetooth.emu.MonitorDevice;
import com.intel.bluetooth.emu.MonitoringService;
import com.intel.bluetooth.rmi.Client;

public class Model extends Observable implements Runnable {

	private static Model instance;

	private boolean stop = false;

	private MonitoringService service;

	protected List<Device> devices = new ArrayList<Device>();

	static {
		instance = new Model();
		new Thread(instance).start();
	}

	private Model() {
	}

	public static Model instance() {
		return instance;
	}

	public void stop() {
		this.stop = true;
	}

	public void run() {
		String host = null;
		String port = null;
		try {
			service = (MonitoringService) Client.getService(MonitoringService.class, false, host, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (!stop) {
			try {
				List<MonitorDevice> devices = service.getDevices();
				updateDeviceList(devices);
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	synchronized void updateDeviceList(List<MonitorDevice> newDevices) {
		Map<Long, MonitorDevice> newDevicesMap = new HashMap<Long, MonitorDevice>();
		for (Iterator<MonitorDevice> iterator = newDevices.iterator(); iterator.hasNext();) {
			MonitorDevice monitorDevice = iterator.next();
			newDevicesMap.put(monitorDevice.getDeviceDescriptor().getAddress(), monitorDevice);
		}
		for (Iterator<Device> iterator = devices.iterator(); iterator.hasNext();) {
			Device device = iterator.next();
			if (newDevicesMap.containsKey(device.getId())) {
				device.setMonitorDevice(newDevicesMap.get(device.getId()));
				newDevicesMap.remove(device.getId());
			} else {
				iterator.remove();
				setChanged();
				notifyObservers(new DatumNotification(REMOVED, device));
			}
		}
		for (Iterator<Entry<Long, MonitorDevice>> iterator = newDevicesMap.entrySet().iterator(); iterator.hasNext();) {
			Entry<Long, MonitorDevice> newEntry = iterator.next();
			Device device = new Device(newEntry.getKey(), newEntry.getValue());
			devices.add(device);
			setChanged();
			notifyObservers(new DatumNotification(ADDED, device));
		}

	}
}
