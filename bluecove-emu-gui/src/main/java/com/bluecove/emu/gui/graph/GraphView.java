package com.bluecove.emu.gui.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.GraphLayoutCache;

import com.bluecove.emu.gui.model.DatumNotification;
import com.bluecove.emu.gui.model.Model;
import com.bluecove.emu.gui.model.Device;

public class GraphView extends GraphLayoutCache implements Observer {

	private static final long serialVersionUID = 1L;

	private Map<Device, DeviceCell> devices = new HashMap<Device, DeviceCell>();
	
	public GraphView() {
		super();
		Model.instance().addObserver(this);
		setFactory(new DeviceCellViewFactory());
	}

	public void addStaff() {
		
				
//				DeviceCell dev1 = new DeviceCell(new Device(22));
//				insertDevice(dev1);
//				DeviceCell dev2 = new DeviceCell("dev2");
//				insertDevice(dev2);
//				DeviceCell dev3 = new DeviceCell("dev3");
//				insertDevice(dev3);
//				DeviceCell dev4 = new DeviceCell("dev4");
//				insertDevice(dev4);	
//				DeviceCell dev5 = new DeviceCell("dev5");
//				insertDevice(dev5);
//				DeviceCell dev6 = new DeviceCell("dev6");
//				insertDevice(dev6);
//				DeviceCell dev7 = new DeviceCell("dev7");
//				insertDevice(dev7);
//				DeviceCell dev8 = new DeviceCell("dev8");
//				insertDevice(dev8);
//				
//				ConnectionEdge connection1 = new ConnectionEdge("", dev1, dev2);
//				insertConnection(connection1);
//				ConnectionEdge connection2 = new ConnectionEdge("", dev1, dev3);
//				insertConnection(connection2);
//				ConnectionEdge connection3 = new ConnectionEdge("", dev4, dev5);
//				insertConnection(connection3);
		
	}
	
	
	public void insertDevice(DeviceCell device) {
		device.beforeInsert();
		HashMap<DeviceCell, AttributeMap> at = new HashMap<DeviceCell, AttributeMap>();
		at.put(device, device.getAttributes());
		insert(new Object[] { device }, at, null,
				null, null);
		devices.put(device.getDevice(), device);
	}

	public void removeDevice(DeviceCell device) {
		device.afterRemove();
		remove(new Object[] { device });
		devices.remove(device.getDevice());
	}
	
	public void insertConnection(ConnectionEdge connection) {
		HashMap<ConnectionEdge, AttributeMap> at = new HashMap<ConnectionEdge, AttributeMap>();
		at.put(connection, connection.getAttributes());
		insert(new Object[] { connection }, at, connection.getConnectionSet(),
				null, null);
	}

	public void removeConnection(ConnectionEdge connection) {
		remove(new Object[] { connection });

	}
	
	public void update(Observable o, Object arg) {
		System.out.println("Observable-"+o);
		System.out.println("Object-"+arg);
		if (DatumNotification.class.isAssignableFrom(arg.getClass())) {
			DatumNotification notif = (DatumNotification)arg;
			switch (notif.getType()) {
			case REMOVED:
				if (Device.class.isAssignableFrom(notif.getDatum().getClass())) {
					removeDevice(devices.get((Device)notif.getDatum()));
				}
				break;
			case ADDED:
				if (Device.class.isAssignableFrom(notif.getDatum().getClass())) {
					insertDevice(new DeviceCell((Device)notif.getDatum()));
				}
				break;
			default:
				break;
			}
		}
		
	}

}
