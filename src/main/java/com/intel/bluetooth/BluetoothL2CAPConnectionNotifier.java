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

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

/**
 * @author vlads
 *
 */
public class BluetoothL2CAPConnectionNotifier implements L2CAPConnectionNotifier, BluetoothConnectionNotifierServiceRecordAccess {

	private long handle;

	private int l2capChannel = -1;
	
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
		
		this.l2capChannel = serviceRecord.getRFCOMMChannel();
		
		this.serviceRecord.attributeUpdated = false;
		
		this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);
	}
	
	/* (non-Javadoc)
	 * @see com.intel.bluetooth.BluetoothConnectionNotifierServiceRecordAccess#getServiceRecord()
	 */
	public ServiceRecord getServiceRecord() {
		// TODO Auto-generated method stub
		return serviceRecord;
	}

	/* (non-Javadoc)
	 * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
	 */
	public L2CAPConnection acceptAndOpen() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
