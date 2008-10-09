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
package com.intel.bluetooth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRegistrationException;

import com.intel.bluetooth.emu.ServicesDescriptor;

/**
 * @author vlads
 * 
 */
abstract class EmulatorServiceConnection extends EmulatorConnection {

	protected BluetoothConnectionNotifierParams params;

	EmulatorServiceConnection(EmulatorLocalDevice localDevice, long handle) {
		super(localDevice, handle);
	}

	private void addServiceClassUUID(Vector<String> uuids, ServiceRecordImpl serviceRecord) {
		DataElement attrDataElement = serviceRecord.getAttributeValue(BluetoothConsts.ServiceClassIDList);
		if ((attrDataElement == null) || (attrDataElement.getDataType() != DataElement.DATSEQ)
				|| attrDataElement.getSize() == 0) {
			return;
		}

		Object value = attrDataElement.getValue();
		if ((value == null) || (!(value instanceof Enumeration))) {
			return;
		}
		for (Enumeration<?> e = (Enumeration<?>) value; e.hasMoreElements();) {
			Object element = e.nextElement();
			if (!(element instanceof DataElement)) {
				continue;
			}
			DataElement dataElement = (DataElement) element;
			if ((dataElement.getDataType() == DataElement.UUID) && (!uuids.contains(dataElement.getValue().toString()))) {
				uuids.add(dataElement.getValue().toString());
			}
		}
	}

	private void addProtocolDescriptorUUID(Vector<String> uuids, ServiceRecordImpl serviceRecord) {
		DataElement protocolDescriptor = serviceRecord.getAttributeValue(BluetoothConsts.ProtocolDescriptorList);
		if ((protocolDescriptor == null) || (protocolDescriptor.getDataType() != DataElement.DATSEQ)) {
			return;
		}
		for (Enumeration<?> protocolsSeqEnum = (Enumeration<?>) protocolDescriptor.getValue(); protocolsSeqEnum
				.hasMoreElements();) {
			Object element = protocolsSeqEnum.nextElement();
			if (!(element instanceof DataElement)) {
				throw new IllegalArgumentException("SDP protocol descriptor list");
			}
			DataElement elementSeq = (DataElement) element;

			if (elementSeq.getDataType() == DataElement.DATSEQ) {
				Enumeration<?> elementSeqEnum = (Enumeration<?>) elementSeq.getValue();
				if (elementSeqEnum.hasMoreElements()) {
					DataElement protocolElement = (DataElement) elementSeqEnum.nextElement();
					if ((protocolElement.getDataType() == DataElement.UUID)
							&& (!uuids.contains(protocolElement.getValue().toString()))) {
						uuids.add(protocolElement.getValue().toString());
					}
				}
			}
		}
	}

	void updateServiceRecord(ServiceRecordImpl serviceRecord) throws ServiceRegistrationException {
		Vector<String> uuids = new Vector<String>();
		addServiceClassUUID(uuids, serviceRecord);
		addProtocolDescriptorUUID(uuids, serviceRecord);
		byte[] sdpBinary;
		try {
			sdpBinary = serviceRecord.toByteArray();
		} catch (IOException e) {
			throw (ServiceRegistrationException) UtilsJavaSE.initCause(
					new ServiceRegistrationException(e.getMessage()), e);
		}
		localDevice.getDeviceManagerService().updateServiceRecord(
				localDevice.getAddress(),
				serviceRecord.getHandle(),
				new ServicesDescriptor((String[]) uuids.toArray(new String[uuids.size()]), sdpBinary,
						serviceRecord.deviceServiceClasses));
	}
}
