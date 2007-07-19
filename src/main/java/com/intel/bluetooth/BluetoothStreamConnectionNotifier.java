/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class BluetoothStreamConnectionNotifier implements StreamConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

	/**
	 * Used to find BluetoothStreamConnectionNotifier by ServiceRecord returned by LocalDevice.getRecord()
	 */
	private static Hashtable serviceRecordsMap = new Hashtable/*<ServiceRecord, BluetoothConnectionNotifierServiceRecordAccess>*/();

	private long handle;

	private int rfcommChannel = -1;

	private ServiceRecordImpl serviceRecord;

	protected boolean closing = false;

	private boolean closed;

	private int securityOpt;

	public BluetoothStreamConnectionNotifier(BluetoothConnectionNotifierParams params) throws IOException {

		this.closed = false;
		if (params.name == null) {
			throw new NullPointerException("Service name is null");
		}

		/*
		 * create service record to be later updated by BluetoothStack
		 */
		this.serviceRecord = new ServiceRecordImpl(null, 0);

		this.handle = BlueCoveImpl.instance().getBluetoothStack().rfServerOpen(params, serviceRecord);

		this.rfcommChannel = serviceRecord.getRFCOMMChannel();

		this.serviceRecord.attributeUpdated = false;

		this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);
	}

	/*
	 * Close the connection. When a connection has been closed, access to any of
	 * its methods except this close() will cause an an IOException to be
	 * thrown. Closing an already closed connection has no effect. Streams
	 * derived from the connection may be open when method is called. Any open
	 * streams will cause the connection to be held open until they themselves
	 * are closed. In this latter case access to the open streams is permitted,
	 * but access to the connection is not.
	 *
	 * Throws: IOException - If an I/O error occurs
	 */

	public void close() throws IOException {
		if (!closed) {
			serviceRecordsMap.remove(serviceRecord);
			closing = true;
			try {
				BlueCoveImpl.instance().getBluetoothStack().rfServerClose(handle, serviceRecord);
				closed = true;
			} finally {
				closing = false;
			}
		}
	}

	/*
	 * Returns a StreamConnection that represents a server side socket
	 * connection. Returns: A socket to communicate with a client. Throws:
	 * IOException - If an I/O error occurs.
	 */

	public StreamConnection acceptAndOpen() throws IOException {
		if (closed) {
			throw new IOException("Notifier is closed");
		}
		if (((ServiceRecordImpl) serviceRecord).attributeUpdated) {
			updateServiceRecord(true);
		}
		try {
			long clientHandle = BlueCoveImpl.instance().getBluetoothStack().rfServerAcceptAndOpenRfServerConnection(handle);
			int clientSecurityOpt = BlueCoveImpl.instance().getBluetoothStack().getSecurityOpt(clientHandle, this.securityOpt);
			return new BluetoothRFCommServerConnection(clientHandle, clientSecurityOpt);
		} catch (IOException e) {
			if (closed || closing) {
				throw new InterruptedIOException("Notifier has been closed");
			}
			throw e;
		}
	}

	public ServiceRecord getServiceRecord() {
		if (closed) {
			throw new IllegalArgumentException("StreamConnectionNotifier is closed");
		}
		serviceRecordsMap.put(serviceRecord, this);
		return serviceRecord;
	}

	private void validateServiceRecord(ServiceRecord srvRecord) {
		DataElement protocolDescriptor = srvRecord.getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
		if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
			throw new IllegalArgumentException("ProtocolDescriptorList is mandatory");
		}

		if (this.rfcommChannel != serviceRecord.getRFCOMMChannel()) {
			throw new IllegalArgumentException("Must not change the RFCOMM server channel number");
		}

		DataElement serviceClassIDList = srvRecord.getAttributeValue(BluetoothConsts.ServiceClassIDList);
		if ((serviceClassIDList == null) || (serviceClassIDList.getDataType() != DataElement.DATSEQ) || serviceClassIDList.getSize() == 0) {
			throw new IllegalArgumentException("ServiceClassIDList is mandatory");
		}

		boolean isL2CAPpresent = false;
		for (Enumeration protocolsSeqEnum = (Enumeration) protocolDescriptor.getValue(); protocolsSeqEnum.hasMoreElements();) {
			DataElement elementSeq = (DataElement) protocolsSeqEnum.nextElement();
			if (elementSeq.getDataType() == DataElement.DATSEQ) {
				Enumeration elementSeqEnum = (Enumeration) elementSeq.getValue();
				if (elementSeqEnum.hasMoreElements()) {
					DataElement protocolElement = (DataElement) elementSeqEnum.nextElement();
					if ((protocolElement.getDataType() == DataElement.UUID) && (BluetoothConsts.L2CAP_PROTOCOL_UUID.equals(protocolElement.getValue()))) {
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
	
	/**
	 * @param acceptAndOpen wrap validation in ServiceRegistrationException
	 * @throws ServiceRegistrationException
	 */
	private void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		try {
			validateServiceRecord(this.serviceRecord);
		} catch (IllegalArgumentException e) {
			if (acceptAndOpen) {
				throw new ServiceRegistrationException(e.getMessage());
			} else {
				throw e;
			}
		}
		BlueCoveImpl.instance().getBluetoothStack().rfServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
		serviceRecord.attributeUpdated = false;
	}

	public static void updateServiceRecord(ServiceRecord srvRecord) throws ServiceRegistrationException {
		BluetoothStreamConnectionNotifier owner = (BluetoothStreamConnectionNotifier)serviceRecordsMap.get(srvRecord);
		if (owner == null) {
			throw new IllegalArgumentException("Service record is not registered");
		}
		owner.updateServiceRecord(false);
	}
}