/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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