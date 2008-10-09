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

#import "OSXStackChannelController.h"

#import <IOBluetooth/objc/IOBluetoothL2CAPChannel.h>

class L2CAPChannelController;

@interface L2CAPChannelDelegate : NSObject <IOBluetoothL2CAPChannelDelegate> {
    L2CAPChannelController* _controller;
}
- (id)initWithController:(L2CAPChannelController*)controller;
- (void)connectionComplete:(IOBluetoothDevice *)device status:(IOReturn)status;
- (void)close;
@end

class L2CAPChannelController : public ChannelController {
public:

    L2CAPChannelDelegate* delegate;
    IOBluetoothL2CAPChannel* l2capChannel;

    int receiveMTU;
    int transmitMTU;

public:
    L2CAPChannelController();
    virtual ~L2CAPChannelController();

    virtual void initDelegate();
    virtual id getDelegate();

    void connectionComplete(IOBluetoothDevice *device, IOReturn status);
    void l2capChannelData(void* dataPointer, size_t dataLength);
    void l2capChannelOpenComplete(IOReturn error);
    void l2capChannelClosed();
    void l2capChannelWriteComplete(void* refcon, IOReturn status);

    void openIncomingChannel(IOBluetoothL2CAPChannel* newL2CAPChannel);

    IOReturn close();
};

class L2CAPConnectionOpen: public Runnable {
public:
    jlong address;
    jint channel;
    jboolean authenticate;
    jboolean encrypt;
    jint timeout;

    L2CAPChannelController* comm;
    volatile IOReturn status;

    L2CAPConnectionOpen();
    virtual void run();
};

long L2CAPChannelCloseExec(L2CAPChannelController* comm);

class L2CAPConnectionWrite: public Runnable {
public:
    BOOL writeComplete;
    void *data;
    UInt16 length;
    IOReturn ioerror;

    L2CAPChannelController* comm;
    volatile IOReturn status;

    L2CAPConnectionWrite();

    void l2capChannelWriteComplete(IOReturn status);
    virtual void run();
};