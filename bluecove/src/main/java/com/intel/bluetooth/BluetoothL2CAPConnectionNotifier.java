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
import java.io.InterruptedIOException;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;

/**
 *
 *
 */
class BluetoothL2CAPConnectionNotifier extends BluetoothConnectionNotifierBase implements L2CAPConnectionNotifier {

    private int transmitMTU;

    private int psm = -1;

    public BluetoothL2CAPConnectionNotifier(BluetoothStack bluetoothStack, BluetoothConnectionNotifierParams params, int receiveMTU, int transmitMTU)
            throws IOException {
        super(bluetoothStack, params);

        this.handle = bluetoothStack.l2ServerOpen(params, receiveMTU, transmitMTU, serviceRecord);

        this.psm = serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID);

        this.transmitMTU = transmitMTU;

        this.serviceRecord.attributeUpdated = false;

        this.securityOpt = Utils.securityOpt(params.authenticate, params.encrypt);

        this.connectionCreated();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()
     */
    public L2CAPConnection acceptAndOpen() throws IOException {
        if (closed) {
            throw new IOException("Notifier is closed");
        }
        updateServiceRecord(true);
        try {
            long clientHandle = bluetoothStack.l2ServerAcceptAndOpenServerConnection(handle);
            int clientSecurityOpt = bluetoothStack.l2GetSecurityOpt(clientHandle, this.securityOpt);
            return new BluetoothL2CAPServerConnection(bluetoothStack, clientHandle, this.transmitMTU, clientSecurityOpt);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            if (closed) {
                throw new InterruptedIOException("Notifier has been closed; " + e.getMessage());
            }
            throw e;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intel.bluetooth.BluetoothConnectionNotifierBase#stackServerClose(long)
     */
    protected void stackServerClose(long handle) throws IOException {
        bluetoothStack.l2ServerClose(handle, serviceRecord);
    }

    protected void validateServiceRecord(ServiceRecord srvRecord) {
        if (this.psm != serviceRecord.getChannel(BluetoothConsts.L2CAP_PROTOCOL_UUID)) {
            throw new IllegalArgumentException("Must not change the PSM");
        }
        super.validateServiceRecord(srvRecord);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.intel.bluetooth.BluetoothConnectionNotifierBase#updateStackServiceRecord(com
     * .intel.bluetooth.ServiceRecordImpl, boolean)
     */
    protected void updateStackServiceRecord(ServiceRecordImpl serviceRecord, boolean acceptAndOpen) throws ServiceRegistrationException {
        bluetoothStack.l2ServerUpdateServiceRecord(handle, serviceRecord, acceptAndOpen);
    }

}
