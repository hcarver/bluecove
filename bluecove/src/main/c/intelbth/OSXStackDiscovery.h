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

#import <Cocoa/Cocoa.h>

#import <IOBluetooth/objc/IOBluetoothDevice.h>
#import <IOBluetooth/objc/IOBluetoothDeviceInquiry.h>

#include "OSXStack.h"

/**
 * OS x BUG. If discovery has been cancelled by stop. For next discovery deviceInquiryComplete function is called for previous Delegate Object, not for current
 */
#define BUG_Inquiry_stop TRUE

@interface OSXStackDiscovery : NSObject {

    int                             _logID;
    volatile BOOL                   _busy;
    volatile BOOL                   _started;
    IOBluetoothDeviceInquiry*       _inquiry;
    NSMutableArray*                 _foundDevices;

    MPEventID*                      _notificationEvent;

    volatile BOOL                   _aborted;
    volatile IOReturn               _error;
    volatile BOOL                   _finished;

}

-(void) addDeviceToList:(IOBluetoothDevice*)inDeviceRef;
-(void) updateDeviceInfo:(IOBluetoothDevice *)inDevice;

-(BOOL) startSearch:(int)logID inquiryLength:(int)inquiryLength;

-(void) stopSearch;

-(BOOL) wait;

//Accessor methods
-(BOOL) busy;
-(BOOL) started;
-(BOOL) aborted;
-(IOReturn) error;
-(IOBluetoothDevice*)getDeviceToReport;

@end

class GetRemoteDeviceFriendlyName: public Runnable {
public:
    MPEventID inquiryFinishedEvent;
    IOBluetoothDeviceRef deviceRef;

    GetRemoteDeviceFriendlyName();
    virtual ~GetRemoteDeviceFriendlyName();

    virtual void run();
};


class RetrieveDevices: public Runnable {
public:
    NSArray *pairedDevices;
    NSArray *favoriteDevices;
    NSArray *recentDevices;

    RetrieveDevices();

    virtual void run();
};