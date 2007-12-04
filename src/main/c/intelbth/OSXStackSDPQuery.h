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

#import <Cocoa/Cocoa.h>

#include "OSXStack.h"

class StackSDPQueryStart: public Runnable {
public:
    volatile BOOL complete;
    jlong address;
    CFDateRef startTime;
    volatile IOReturn status;
    volatile int recordsSize;
    IOBluetoothDeviceRef deviceRef;

    StackSDPQueryStart();
    virtual void run();

    void sdpQueryComplete(IOBluetoothDeviceRef deviceRef, IOReturn status);
};

#define DATA_BLOB_MAX  0x4000

class GetAttributeDataElement: public Runnable {
public:
    jlong address;
    jlong serviceRecordIndex;
    jint attrID;

    // To avoid memory allocation problem we return standard BLOB to Java thread. See com.intel.bluetooth.SDPInputStream
    int   dataLen;
    UInt8 data[DATA_BLOB_MAX];

    GetAttributeDataElement();
    virtual void run();

    void getData(const IOBluetoothSDPDataElementRef dataElement);
};

class SDPOutputStream {
public:
    CFMutableDataRef data;

    SDPOutputStream();
    ~SDPOutputStream();

    void write(const UInt8 byte);
    void write(const UInt8 *bytes, CFIndex length);
    void writeLong(UInt64 l, int size);

    BOOL writeElement(const IOBluetoothSDPDataElementRef dataElement);
    int getLength(const IOBluetoothSDPDataElementRef dataElement);
    void getBytes(int max, int*  dataLen, UInt8* buf);
};