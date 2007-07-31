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
import java.io.InterruptedIOException;
import java.util.Enumeration;

import javax.bluetooth.DataElement;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;

/**
 * @author vlads
 *
 */
class BluetoothL2CAPConnectionNotifier implements L2CAPConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

	private volatile long handle;

	private int psm = -1;
	
	private ServiceRecordImpl serviceRecord;
	
	private boolean closed;
	
	private int securityOpt;
	
	public BluetoothL2CAPConnectionNotifier(BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU) throws IOException {
		
		this.closed = false;
		
		if (params.name == null) {
			throw new NullPointerException("Service name is null");
		}
		
		/*
		 * create service record to be later updated by BluetoothStack
		 */
		this.serviceRecord = new ServiceRecordImpl(null, 0);
		
		this.handle = BlueCoveImpl.instance().getBluetoothStack().l2ServerOpen(params, receiveMTU, transmitMTU, serviceRecord);
		
		this.psm = serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID);
		
		this.serviceRecord.attributeUpdated = false;
		
		this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		if (closed) {
			throw new IllegalArgumentException("L2CAPConnectionNotifier is closed");
		}
		ServiceRecordsRegistry.register(this, serviceRecord);
		return serviceRecord;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public L2CAPConnection acceptAndOpen() throws IOException {
		if (closed) {
			throw new IOException("Notifier is closed");
		}
		if (((ServiceRecordImpl) serviceRecord).attributeUpdated) {
			updateServiceRecord(true);
		}
		try {
			long clientHandle = BlueCoveImpl.instance().getBluetoothStack().l2ServerAcceptAndOpenServerConnection(handle);
			int clientSecurityOpt = BlueCoveImpl.instance().getBluetoothStack().getSecurityOpt(clientHandle, this.securityOpt);
			return new BluetoothL2CAPServerConnection(clientHandle, clientSecurityOpt);
		} catch (IOException e) {
			if (closed) {
				throw new InterruptedIOException("Notifier has been closed");
			}
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			ServiceRecordsRegistry.unregister(serviceRecord);
			long h = handle;
			handle = 0;
			BlueCoveImpl.instance().getBluetoothStack().l2ServerClose(h, serviceRecord);
		}
	}

	private void validateServiceRecord(ServiceRecord srvRecord) {
		DataElement protocolDescriptor = srvRecord.getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
		if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
			throw new IllegalArgumentException("ProtocolDescriptorList is mandatory");
		}

		if (this.psm != serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
			throw new IllegalArgumentException("Must not change the PSM");
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
		BlueCoveImpl.instance().getBluetoothStack().l2ServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
		serviceRecord.attributeUpdated = false;
		
	}

}
