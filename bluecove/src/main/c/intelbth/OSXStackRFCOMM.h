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

#import <IOBluetooth/objc/IOBluetoothRFCOMMChannel.h>

@class IOBluetoothRFCOMMChannel;

class RFCOMMChannelController;

@interface RFCOMMChannelDelegate : NSObject <IOBluetoothRFCOMMChannelDelegate> {
    RFCOMMChannelController* _controller;
}
- (id)initWithController:(RFCOMMChannelController*)controller;
- (void)connectionComplete:(IOBluetoothDevice *)device status:(IOReturn)status;
- (void)close;
@end

class RFCOMMChannelController : public ChannelController {
public:

    RFCOMMChannelDelegate* delegate;
    IOBluetoothRFCOMMChannel* rfcommChannel;

    BluetoothRFCOMMMTU	rfcommChannelMTU;

public:
    RFCOMMChannelController();
    virtual ~RFCOMMChannelController();

    virtual void initDelegate();
    virtual id getDelegate();

    void connectionComplete(IOBluetoothDevice *device, IOReturn status);
    void rfcommChannelOpenComplete(IOReturn error);
    void rfcommChannelData(void*dataPointer, size_t dataLength);
    void rfcommChannelClosed();
    void rfcommChannelWriteComplete(void* refcon, IOReturn status);

    void openIncomingChannel(IOBluetoothRFCOMMChannel* newRfcommChannel);

    IOReturn close();

};

class RFCOMMConnectionOpen: public Runnable {
public:
    jlong address;
    jint channel;
    jboolean authenticate;
    jboolean encrypt;
    jint timeout;

    RFCOMMChannelController* comm;
    volatile IOReturn status;

    RFCOMMConnectionOpen();
    virtual void run();
};

long RFCOMMChannelCloseExec(RFCOMMChannelController* comm);

class RFCOMMConnectionWrite: public Runnable {
public:
    BOOL writeComplete;
    void *data;
    UInt16 length;
    IOReturn ioerror;

    RFCOMMChannelController* comm;
    volatile IOReturn status;

    RFCOMMConnectionWrite();

    void rfcommChannelWriteComplete(IOReturn status);
    virtual void run();
};