/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
