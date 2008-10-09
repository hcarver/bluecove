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

struct SDPAttributeValue {
    jint attrID;
    jint attrType;
    jlong numberValue;
    jbyte *arrayValue;
	int   arrayLen;
};

#define SDP_SEQUENCE_DEPTH_MAX 10

class ServerController : public PoolableObject {
public:
    BOOL isClosed;

    NSMutableDictionary* sdpEntries;
    BluetoothSDPServiceRecordHandle sdpServiceRecordHandle;

    int sdpSequenceDepthCurrent;
    NSMutableArray *sdpSequence[SDP_SEQUENCE_DEPTH_MAX];

    MPEventID incomingChannelNotificationEvent;
    IOBluetoothUserNotificationRef incomingChannelNotification;

    MPEventID acceptedEvent;
    volatile BOOL openningClient;

public:
    ServerController();
    virtual ~ServerController();

    char* addAttribute(SDPAttributeValue* value);
    char* addAttributeSequence(jint attrID, jint attrType);
    char* addDataElement(jint attrID, NSObject* value);

    void init();
    virtual IOReturn updateSDPServiceRecord() = 0;
};

NSMutableDictionary* createIntDataElement(int size, int type, int value);

extern NSString *kServiceItemKeyServiceClassIDList;
extern NSString *kServiceItemKeyServiceName;
extern NSString *kServiceItemKeyProtocolDescriptorList;

extern NSString *kDataElementSize;
extern NSString *kDataElementType;
extern NSString *kDataElementValue;
