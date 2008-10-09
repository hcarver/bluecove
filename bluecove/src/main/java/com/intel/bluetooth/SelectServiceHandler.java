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
package com.intel.bluetooth;

import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

/**
 * Implementation of DiscoveryAgent.selectService().
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 * @author vlads
 * 
 */
public class SelectServiceHandler implements DiscoveryListener {

	private DiscoveryAgent agent;

	private Object inquiryCompletedEvent = new Object();

	private boolean inquiryCompleted;

	private Object serviceSearchCompletedEvent = new Object();

	private boolean serviceSearchCompleted;

	private Hashtable devicesProcessed = new Hashtable();

	private Vector serviceSearchDeviceQueue = new Vector();

	private ServiceRecord servRecordDiscovered;

	private static int threadNumber;

	private static synchronized int nextThreadNum() {
		return threadNumber++;
	}

	public SelectServiceHandler(DiscoveryAgent agent) {
		this.agent = agent;
	}

	/**
	 * Attempts to locate a service that contains <code>uuid</code> in the
	 * ServiceClassIDList of its service record. This method will return a
	 * string that may be used in <code>Connector.open()</code> to establish a
	 * connection to the service. How the service is selected if there are
	 * multiple services with <code>uuid</code> and which devices to search is
	 * implementation dependent.
	 * 
	 * @see ServiceRecord#NOAUTHENTICATE_NOENCRYPT
	 * @see ServiceRecord#AUTHENTICATE_NOENCRYPT
	 * @see ServiceRecord#AUTHENTICATE_ENCRYPT
	 * 
	 * @param uuid
	 *            the UUID to search for in the ServiceClassIDList
	 * 
	 * @param security
	 *            specifies the security requirements for a connection to this
	 *            service; must be one of
	 *            <code>ServiceRecord.NOAUTHENTICATE_NOENCRYPT</code>,
	 *            <code>ServiceRecord.AUTHENTICATE_NOENCRYPT</code>, or
	 *            <code>ServiceRecord.AUTHENTICATE_ENCRYPT</code>
	 * 
	 * @param master
	 *            determines if this client must be the master of the
	 *            connection; <code>true</code> if the client must be the
	 *            master; <code>false</code> if the client can be the master
	 *            or the slave
	 * 
	 * @return the connection string used to connect to the service with a UUID
	 *         of <code>uuid</code>; or <code>null</code> if no service
	 *         could be found with a UUID of <code>uuid</code> in the
	 *         ServiceClassIDList
	 * 
	 * @exception BluetoothStateException
	 *                if the Bluetooth system cannot start the request due to
	 *                the current state of the Bluetooth system
	 * 
	 * @exception NullPointerException
	 *                if <code>uuid</code> is <code>null</code>
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>security</code> is not
	 *                <code>ServiceRecord.NOAUTHENTICATE_NOENCRYPT</code>,
	 *                <code>ServiceRecord.AUTHENTICATE_NOENCRYPT</code>, or
	 *                <code>ServiceRecord.AUTHENTICATE_ENCRYPT</code>
	 */
	public String selectService(UUID uuid, int security, boolean master) throws BluetoothStateException {
		if (uuid == null) {
			throw new NullPointerException("uuid is null");
		}
		switch (security) {
		case ServiceRecord.NOAUTHENTICATE_NOENCRYPT:
		case ServiceRecord.AUTHENTICATE_NOENCRYPT:
		case ServiceRecord.AUTHENTICATE_ENCRYPT:
			break;
		default:
			throw new IllegalArgumentException();
		}

		RemoteDevice[] devs = agent.retrieveDevices(DiscoveryAgent.PREKNOWN);
		for (int i = 0; (devs != null) && (i < devs.length); i++) {
			ServiceRecord sr = findServiceOnDevice(uuid, devs[i]);
			if (sr != null) {
				return sr.getConnectionURL(security, master);
			}
		}
		devs = agent.retrieveDevices(DiscoveryAgent.CACHED);
		for (int i = 0; (devs != null) && (i < devs.length); i++) {
			ServiceRecord sr = findServiceOnDevice(uuid, devs[i]);
			if (sr != null) {
				return sr.getConnectionURL(security, master);
			}
		}
		ParallelSearchServicesThread t = new ParallelSearchServicesThread(uuid);
		t.start();

		synchronized (inquiryCompletedEvent) {
			if (!agent.startInquiry(DiscoveryAgent.GIAC, this)) {
				return null;
			}
			while (!inquiryCompleted) {
				try {
					inquiryCompletedEvent.wait();
				} catch (InterruptedException e) {
					return null;
				}
			}
			agent.cancelInquiry(this);
		}

		if ((servRecordDiscovered == null) && (!t.processedAll())) {
			synchronized (serviceSearchDeviceQueue) {
				serviceSearchDeviceQueue.notifyAll();
			}
			try {
				t.join();
			} catch (InterruptedException e) {
				return null;
			}
		}
		t.interrupt();

		if (servRecordDiscovered != null) {
			return servRecordDiscovered.getConnectionURL(security, master);
		}

		return null;
	}

	private class ParallelSearchServicesThread extends Thread {

		private boolean stoped = false;

		private int processedNext = 0;

		private int processedSize = 0;

		private UUID uuid;

		ParallelSearchServicesThread(UUID uuid) {
			super("SelectServiceThread-" + nextThreadNum());
			this.uuid = uuid;
		}

		boolean processedAll() {
			return (processedNext == serviceSearchDeviceQueue.size());
		}

		public void interrupt() {
			stoped = true;
			synchronized (serviceSearchDeviceQueue) {
				serviceSearchDeviceQueue.notifyAll();
			}
			super.interrupt();
		}

		public void run() {
			mainLoop: while ((!stoped) && (servRecordDiscovered == null)) {
				synchronized (serviceSearchDeviceQueue) {
					if ((inquiryCompleted) && (processedSize == serviceSearchDeviceQueue.size())) {
						return;
					}
					if (processedSize == serviceSearchDeviceQueue.size()) {
						try {
							serviceSearchDeviceQueue.wait();
						} catch (InterruptedException e) {
							return;
						}
					}
					processedSize = serviceSearchDeviceQueue.size();
				}
				for (int i = processedNext; i < processedSize; i++) {
					RemoteDevice btDevice = (RemoteDevice) serviceSearchDeviceQueue.elementAt(i);
					if (findServiceOnDevice(uuid, btDevice) != null) {
						break mainLoop;
					}
				}
				processedNext = processedSize + 1;
			}
		}

	}

	private ServiceRecord findServiceOnDevice(UUID uuid, RemoteDevice device) {
		if (devicesProcessed.containsKey(device)) {
			return null;
		}
		devicesProcessed.put(device, device);
		DebugLog.debug("searchServices on ", device);
		synchronized (serviceSearchCompletedEvent) {
			try {
				serviceSearchCompleted = false;
				agent.searchServices(null, new UUID[] { uuid }, device, this);
			} catch (BluetoothStateException e) {
				DebugLog.error("searchServices", e);
				return null;
			}
			while (!serviceSearchCompleted) {
				try {
					serviceSearchCompletedEvent.wait();
				} catch (InterruptedException e) {
					return null;
				}
			}
		}
		return servRecordDiscovered;
	}

	public void deviceDiscovered(final RemoteDevice btDevice, DeviceClass cod) {
		if (devicesProcessed.containsKey(btDevice)) {
			return;
		}
		synchronized (serviceSearchDeviceQueue) {
			serviceSearchDeviceQueue.addElement(btDevice);
			serviceSearchDeviceQueue.notifyAll();
		}
	}

	public void inquiryCompleted(int discType) {
		synchronized (inquiryCompletedEvent) {
			inquiryCompleted = true;
			inquiryCompletedEvent.notifyAll();
		}
	}

	public void serviceSearchCompleted(int transID, int respCode) {
		synchronized (serviceSearchCompletedEvent) {
			serviceSearchCompleted = true;
			serviceSearchCompletedEvent.notifyAll();
		}
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		if ((servRecord.length > 0) && (servRecordDiscovered == null)) {
			servRecordDiscovered = servRecord[0];
			synchronized (serviceSearchCompletedEvent) {
				serviceSearchCompleted = true;
				serviceSearchCompletedEvent.notifyAll();
			}
			synchronized (inquiryCompletedEvent) {
				inquiryCompleted = true;
				inquiryCompletedEvent.notifyAll();
			}
		}
	}
}
