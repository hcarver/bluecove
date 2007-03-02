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

import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class BluetoothStreamConnectionNotifier implements StreamConnectionNotifier {
	
	private int socket;

	private int handle;

	ServiceRecord serviceRecord;

	private boolean closed;

	public BluetoothStreamConnectionNotifier(UUID uuid, boolean authenticate, boolean encrypt, String name) throws IOException {
		/*
		 * open socket
		 */

		BluetoothPeer peer = BlueCoveImpl.instance().getBluetoothPeer();

		socket = peer.socket(authenticate, encrypt);

		peer.bind(socket);
		
		peer.listen(socket);

		/*
		 * create service record
		 */

		serviceRecord = new ServiceRecordImpl(null, 0);

		/*
		 * service handle (direct insert to avoid IllegalArgumentException from
		 * setAttributeValue)
		 */

		((ServiceRecordImpl) serviceRecord).attributes.put(
				new Integer(ServiceRecord.ServiceRecordHandle), 
				new DataElement(DataElement.U_INT_4, 0x00010020));

		/*
		 * service class ID list
		 */

		DataElement serviceClassIDList = new DataElement(DataElement.DATSEQ);
		serviceClassIDList.addElement(new DataElement(DataElement.UUID, uuid));

		serviceRecord.setAttributeValue(ServiceRecord.ServiceClassIDList, serviceClassIDList);

		/*
		 * protocol descriptor list
		 */

		DataElement protocolDescriptorList = new DataElement(DataElement.DATSEQ);

		DataElement L2CAPDescriptor = new DataElement(DataElement.DATSEQ);
		L2CAPDescriptor.addElement(new DataElement(DataElement.UUID, UUID.L2CAP_PROTOCOL_UUID));
		protocolDescriptorList.addElement(L2CAPDescriptor);

		DataElement RFCOMMDescriptor = new DataElement(DataElement.DATSEQ);
		RFCOMMDescriptor.addElement(new DataElement(DataElement.UUID, UUID.RFCOMM_PROTOCOL_UUID));
		RFCOMMDescriptor.addElement(new DataElement(DataElement.U_INT_1, peer.getsockchannel(socket)));
		protocolDescriptorList.addElement(RFCOMMDescriptor);

		serviceRecord.setAttributeValue(ServiceRecord.ProtocolDescriptorList, protocolDescriptorList);

		/*
		 * name
		 */

		if (name != null) {
			serviceRecord.setAttributeValue(0x0100, new DataElement(DataElement.STRING, name));
		}

		/*
		 * register service
		 */

		handle = peer.registerService(((ServiceRecordImpl) serviceRecord).toByteArray());
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
			BluetoothPeer peer = BlueCoveImpl.instance().getBluetoothPeer();

			/*
			 * close socket
			 */

			peer.close(socket);

			/*
			 * unregister service
			 */

			peer.unregisterService(handle);

			closed = true;
		}
	}

	/*
	 * Returns a StreamConnection that represents a server side socket
	 * connection. Returns: A socket to communicate with a client. Throws:
	 * IOException - If an I/O error occurs.
	 */

	public StreamConnection acceptAndOpen() throws IOException {
		return new BluetoothConnection(BlueCoveImpl.instance().getBluetoothPeer().accept(socket));
	}

	public ServiceRecord getServiceRecord() {
		return serviceRecord;
	}
}