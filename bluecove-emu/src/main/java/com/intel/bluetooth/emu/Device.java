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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import javax.bluetooth.BluetoothConnectionException;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.RemoteDeviceHelper;

/**
 * @author vlads
 * 
 */
class Device {

	private boolean isReleased = false;

	private DeviceDescriptor descriptor;

	private DeviceSDP sdp;

	private Hashtable<String, String> servicesOpen = new Hashtable<String, String>();

	private Vector<ServiceListener> serviceListeners;

	private Object serviceNotification = new Object();

	private Hashtable<Long, ConnectionBuffer> connections = new Hashtable<Long, ConnectionBuffer>();

	private Queue<DeviceCommand> commandQueue = new LinkedList<DeviceCommand>();

	private long lastEvent = System.currentTimeMillis();

	Device(DeviceDescriptor descriptor) {
		this.descriptor = descriptor;
		this.serviceListeners = new Vector<ServiceListener>();
	}

	DeviceDescriptor getDescriptor() {
		return this.descriptor;
	}

	boolean isHasServices() {
		return !servicesOpen.isEmpty();
	}

	boolean isListening() {
		return !serviceListeners.isEmpty();
	}

	Long[] getConnectedTo() {
		List<Long> connectedTo = new Vector<Long>();
		synchronized (connections) {
			for (Enumeration<ConnectionBuffer> iterator = connections.elements(); iterator.hasMoreElements();) {
				ConnectionBuffer c = (ConnectionBuffer) iterator.nextElement();
				if (!c.isServerSide()) {
					connectedTo.add(new Long(c.getRemoteAddress()));
				}

			}
		}
		return connectedTo.toArray(new Long[connectedTo.size()]);
	}

	synchronized DeviceSDP getDeviceSDP(boolean create) {
		if (create && (sdp == null)) {
			sdp = new DeviceSDP(descriptor.getAddress());
		}
		return sdp;
	}

	void openService(String portID) {
		servicesOpen.put(portID, portID);
	}

	void closeService(String portID) {
		servicesOpen.remove(portID);
		ServiceListener sl;
		while ((sl = removeServiceListener(portID)) != null) {
			sl.close();
		}
	}

	ServiceListener createServiceListener(String portID) {
		ServiceListener sl = new ServiceListener(portID);
		synchronized (serviceListeners) {
			serviceListeners.addElement(sl);
		}
		return sl;
	}

	private ServiceListener removeServiceListener(String portID) {
		ServiceListener sl = null;
		synchronized (serviceListeners) {
			for (Enumeration<ServiceListener> iterator = serviceListeners.elements(); iterator.hasMoreElements();) {
				ServiceListener s = (ServiceListener) iterator.nextElement();
				if (s.getPortID().equals(portID)) {
					serviceListeners.removeElement(s);
					sl = s;
					break;
				}
			}
		}
		return sl;
	}

	void serviceListenerAccepting(String portID) {
		synchronized (serviceNotification) {
			serviceNotification.notifyAll();
		}
	}

	ServiceListener connectService(String portID, long timeout) throws IOException {
		if (servicesOpen.get(portID) == null) {
			return null;
		}
		ServiceListener sl = removeServiceListener(portID);
		long endOfDellay = System.currentTimeMillis() + timeout;
		while ((sl == null) && (timeout > 0)) {
			long timeleft = endOfDellay - System.currentTimeMillis();
			if (timeleft <= 0) {
				throw new BluetoothConnectionException(BluetoothConnectionException.TIMEOUT, "Service " + portID
						+ " not accepting");
			}
			try {
				synchronized (serviceNotification) {
					serviceNotification.wait(timeleft);
				}
			} catch (InterruptedException e) {
				break;
			}
			if (servicesOpen.get(portID) == null) {
				break;
			}
			sl = removeServiceListener(portID);
		}
		return sl;
	}

	void addConnectionBuffer(long connectionId, ConnectionBuffer c) {
		synchronized (connections) {
			connections.put(new Long(connectionId), c);
		}
	}

	ConnectionBuffer getConnectionBuffer(long connectionId) {
		return (ConnectionBuffer) connections.get(new Long(connectionId));
	}

	void closeConnection(long connectionId) throws IOException {
		ConnectionBuffer c;
		synchronized (connections) {
			c = connections.remove(new Long(connectionId));
		}
		if (c == null) {
			throw new IOException("No such connection " + connectionId);
		}
		c.close();
	}

	void setDevicePower(boolean on) {
		this.descriptor.setPoweredOn(on);
		if (!on) {
			close();
		}
		putCommand(new DeviceCommand(DeviceCommand.DeviceCommandType.chagePowerState, new Boolean(on)));
	}

	void putCommand(DeviceCommand command) {
		synchronized (commandQueue) {
			commandQueue.add(command);
			commandQueue.notifyAll();
		}
	}

	DeviceCommand pollCommand() {
		lastEvent = System.currentTimeMillis();
		DeviceCommand command = null;
		synchronized (commandQueue) {
			while (command == null && (!isReleased)) {
				command = commandQueue.poll();
				if (command == null) {
					try {
						commandQueue.wait(DeviceManagerServiceImpl.configuration.getKeepAliveSeconds() * 1000);
						if ((!isReleased) && commandQueue.isEmpty()) {
							command = DeviceCommand.keepAliveCommand;
							break;
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}
		return command;
	}

	boolean isAlive() {
		return System.currentTimeMillis() < (lastEvent + (DeviceManagerServiceImpl.configuration.getKeepAliveSeconds() + 7) * 1000);
	}

	void died() {
		DebugLog.debug("device died", RemoteDeviceHelper.getBluetoothAddress(descriptor.getAddress()));
		release();
	}

	void release() {
		isReleased = true;
		close();
	}

	void close() {
		synchronized (commandQueue) {
			commandQueue.notifyAll();
		}
		servicesOpen.clear();
		for (Enumeration<ServiceListener> iterator = serviceListeners.elements(); iterator.hasMoreElements();) {
			ServiceListener s = (ServiceListener) iterator.nextElement();
			s.close();
		}
		serviceListeners.clear();
		synchronized (connections) {
			for (Enumeration<ConnectionBuffer> iterator = connections.elements(); iterator.hasMoreElements();) {
				ConnectionBuffer c = (ConnectionBuffer) iterator.nextElement();
				try {
					c.close();
				} catch (IOException e) {
				}
			}
			connections.clear();
		}
	}
}
