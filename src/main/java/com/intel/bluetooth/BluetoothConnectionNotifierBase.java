/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
import java.util.Enumeration;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.microedition.io.Connection;

/**
 * @author vlads
 *
 */
abstract class BluetoothConnectionNotifierBase implements Connection, BluetoothConnectionNotifierServiceRecordAccess {

	protected BluetoothStack bluetoothStack;
	
	protected volatile long handle;

	protected ServiceRecordImpl serviceRecord;
	
	protected boolean closed;
	
	protected boolean closing;
	
	protected int securityOpt;
	
	protected BluetoothConnectionNotifierBase(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params) throws BluetoothStateException, Error {
		this.bluetoothStack = bluetoothStack;
		this.closed = false;
		this.closing = false;
		if (params.name == null) {
			throw new NullPointerException("Service name is null");
		}
		/*
		 * create service record to be later updated by BluetoothStack
		 */
		this.serviceRecord = new ServiceRecordImpl(this.bluetoothStack, null, 0);
	}

	protected abstract void closeStack(long handle) throws IOException;
	
	/*
	 * Close the connection. When a connection has been closed, access to any of
	 * its methods except this close() will cause an an IOException to be
	 * thrown. Closing an already closed connection has no effect. Streams
	 * derived from the connection may be open when method is called. Any open
	 * streams will cause the connection to be held open until they themselves
	 * are closed. In this latter case access to the open streams is permitted,
	 * but access to the connection is not.
	 */
	
	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (!closed) {
			long h = handle;
			handle = 0;
			if (h != 0) {
				ServiceRecordsRegistry.unregister(serviceRecord);
				closing = true;
				DebugLog.debug("closing ConnectionNotifier");
				try {
					if (h != 0) {
						closeStack(h);
					}
					closed = true;
				} finally {
					closing = false;
				}
			}
		}
	}
	
	/* (non-Javadoc)
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
	
	protected abstract void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException;
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#updateServiceRecord(boolean)
	 */
	public void updateServiceRecord(boolean acceptAndOpen) throws ServiceRegistrationException {
		try {
			validateServiceRecord(this.serviceRecord);
		} catch (IllegalArgumentException e) {
			if (acceptAndOpen) {
				throw new ServiceRegistrationException(e.getMessage());
			} else {
				throw e;
			}
		}
		updateStackServiceRecord(serviceRecord, acceptAndOpen);
		serviceRecord.attributeUpdated = false;
	}

}
