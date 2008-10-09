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

#include "OSXStack.h"

#import <IOBluetooth/objc/IOBluetoothDevice.h>

class ChannelController : public PoolableObject {
public:
    MPEventID notificationEvent;
	MPEventID writeCompleteNotificationEvent;
    volatile IOReturn openStatus;

    IOBluetoothDevice* bluetoothDevice;

    volatile BOOL isClosed;
	volatile BOOL isBasebandConnected;
	volatile BOOL isConnected;
	jlong address;

    ReceiveBuffer receiveBuffer;

public:
    ChannelController();
    virtual ~ChannelController();

    virtual void initDelegate() = 0;
    virtual id getDelegate() = 0;

    BOOL waitForConnection(JNIEnv *env, jobject peer, BOOL baseband, jint timeout);
};

class BasebandConnectionOpen: public Runnable {
public:
    jlong address;
    jboolean authenticate;
    jboolean encrypt;
    jint timeout;

    ChannelController* comm;

    volatile IOReturn status;

    BasebandConnectionOpen();
    virtual void run();
};

class BasebandConnectionGetOptions: public Runnable {
public:
    jboolean encrypted;

    ChannelController* comm;

    volatile IOReturn status;

    BasebandConnectionGetOptions();
    virtual void run();
};