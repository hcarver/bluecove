/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.Connection;

/**
 *
 */
abstract class BluetoothConnectionNotifierBase implements Connection, BluetoothConnectionNotifierServiceRecordAccess {

	private static Hashtable stackConnections = new Hashtable();

	protected BluetoothStack bluetoothStack;

	protected volatile long handle;

	protected ServiceRecordImpl serviceRecord;

	protected boolean closed;

	protected int securityOpt;

	static void shutdownConnections(BluetoothStack bluetoothStack) {
		Vector connections;
		synchronized (stackConnections) {
			connections = (Vector) stackConnections.get(bluetoothStack);
		}
		if (connections == null) {
			return;
		}
		Vector c2shutdown = new Vector();
		c2shutdown = Utils.clone(connections.elements());
		for (Enumeration en = c2shutdown.elements(); en.hasMoreElements();) {
			BluetoothConnectionNotifierBase c = (BluetoothConnectionNotifierBase) en.nextElement();
			try {
				c.shutdown();
			} catch (IOException e) {
				DebugLog.debug("connection shutdown", e);
			}
		}
	}

	protected BluetoothConnectionNotifierBase(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params)
			throws BluetoothStateException, Error {
		this.bluetoothStack = bluetoothStack;
		this.closed = false;
		if (params.name == null) {
			throw new NullPointerException("Service name is null");
		}
		/*
		 * create service record to be later updated by BluetoothStack
		 */
		this.serviceRecord = new ServiceRecordImpl(this.bluetoothStack, null, 0);
	}

	protected void connectionCreated() {
		Vector connections;
		synchronized (stackConnections) {
			connections = (Vector) stackConnections.get(this.bluetoothStack);
			if (connections == null) {
				connections = new Vector();
				stackConnections.put(this.bluetoothStack, connections);
			}
		}
		connections.addElement(this);
	}

	protected abstract void stackServerClose(long handle) throws IOException;

	/*
	 * Close the connection. When a connection has been closed, access to any of
	 * its methods except this close() will cause an an IOException to be
	 * thrown. Closing an already closed connection has no effect. Streams
	 * derived from the connection may be open when method is called. Any open
	 * streams will cause the connection to be held open until they themselves
	 * are closed. In this latter case access to the open streams is permitted,
	 * but access to the connection is not.
	 */

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (!closed) {
			shutdown();
		}
	}

	public void shutdown() throws IOException {
		closed = true;
		if (handle != 0) {
			DebugLog.debug("closing ConnectionNotifier", handle);
			Vector connections;
			synchronized (stackConnections) {
				connections = (Vector) stackConnections.get(this.bluetoothStack);
			}
			connections.removeElement(this);
			long synchronizedHandle;
			synchronized (this) {
				synchronizedHandle = handle;
				handle = 0;
			}
			if (synchronizedHandle != 0) {

				ServiceRecordsRegistry.unregister(serviceRecord);

				if ((serviceRecord.deviceServiceClasses != 0)
						&& ((bluetoothStack.getFeatureSet() & BluetoothStack.FEATURE_SET_DEVICE_SERVICE_CLASSES) != 0)) {
					bluetoothStack.setLocalDeviceServiceClasses(ServiceRecordsRegistry.getDeviceServiceClasses());
				}

				stackServerClose(synchronizedHandle);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		if (closed) {
			throw new IllegalArgumentException("ConnectionNotifier is closed");
		}
		ServiceRecordsRegistry.register(this, serviceRecord);
		return serviceRecord;
	}

	protected void validateServiceRecord(ServiceRecord srvRecord) {
		DataElement protocolDescriptor = srvRecord.getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
		if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
			throw new IllegalArgumentException("ProtocolDescriptorList is mandatory");
		}

		DataElement serviceClassIDList = srvRecord.getAttributeValue(BluetoothConsts.ServiceClassIDList);
		if ((serviceClassIDList == null) || (serviceClassIDList.getDataType() != DataElement.DATSEQ)
				|| serviceClassIDList.getSize() == 0) {
			throw new IllegalArgumentException("ServiceClassIDList is mandatory");
		}

		boolean isL2CAPpresent = false;
		for (Enumeration protocolsSeqEnum = (Enumeration) protocolDescriptor.getValue(); protocolsSeqEnum
				.hasMoreElements();) {
			DataElement elementSeq = (DataElement) protocolsSeqEnum.nextElement();
			if (elementSeq.getDataType() == DataElement.DATSEQ) {
				Enumeration elementSeqEnum = (Enumeration) elementSeq.getValue();
				if (elementSeqEnum.hasMoreElements()) {
					DataElement protocolElement = (DataElement) elementSeqEnum.nextElement();
					if ((protocolElement.getDataType() == DataElement.UUID)
							&& (BluetoothConsts.L2CAP_PROTOCOL_UUID.equals(protocolElement.getValue()))) {
						isL2CAPpresent = true;
						break;
					}
				}
			}
		}
		if (!isL2CAPpresent) {
			throw new IllegalArgumentException("L2CAP UUID is mandatory in ProtocolDescriptorList");
		}
	}

	protected abstract void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen)
			throws ServiceRegistrationException;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#updateServiceRecord(boolean)
	 */
	public void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		if (serviceRecord.attributeUpdated || (!acceptAndOpen)) {
			try {
				validateServiceRecord(this.serviceRecord);
			} catch (IllegalArgumentException e) {
				if (acceptAndOpen) {
					throw new ServiceRegistrationException(e.getMessage());
				} else {
					throw e;
				}
			}
			try {
				updateStackServiceRecord(serviceRecord, acceptAndOpen);
			} finally {
				serviceRecord.attributeUpdated = false;
			}
		}
		if ((serviceRecord.deviceServiceClasses != serviceRecord.deviceServiceClassesRegistered)
				&& ((bluetoothStack.getFeatureSet() & BluetoothStack.FEATURE_SET_DEVICE_SERVICE_CLASSES) != 0)) {

			bluetoothStack.setLocalDeviceServiceClasses(ServiceRecordsRegistry.getDeviceServiceClasses());

			serviceRecord.deviceServiceClassesRegistered = serviceRecord.deviceServiceClasses;
		}
	}
}
