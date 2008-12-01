/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
import java.util.Vector;

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRegistrationException;

import com.intel.bluetooth.emu.ServicesDescriptor;

/**
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
