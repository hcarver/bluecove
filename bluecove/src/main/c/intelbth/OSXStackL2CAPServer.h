/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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

#import "OSXStackL2CAP.h"
#import "OSXStackSDPServer.h"

#import <IOBluetooth/objc/IOBluetoothSDPServiceRecord.h>
#import <IOBluetooth/objc/IOBluetoothSDPUUID.h>

class L2CAPServerController : public ServerController {
public:

    BluetoothL2CAPPSM l2capPSM;

    L2CAPChannelController* acceptClientComm;

    NSMutableDictionary* l2capPSMDataElement;

    jboolean authenticate;

    int receiveMTU;
    int transmitMTU;

public:
    L2CAPServerController();
    virtual ~L2CAPServerController();

    void createPSMDataElement();
    virtual IOReturn updateSDPServiceRecord();
    IOReturn publish();
    void close();
};

class L2CAPServicePublish: public Runnable {
public:
    jbyte* uuidValue;
    int uuidValueLength;
    jboolean authenticate;
    jboolean encrypt;
    const jchar *serviceName;
    int serviceNameLength;

    jint assignPsm;

    L2CAPServerController* comm;
    volatile IOReturn status;

    L2CAPServicePublish();
    virtual void run();
};