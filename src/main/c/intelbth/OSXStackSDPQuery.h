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

    StackSDPQueryStart();
    virtual void run();

    void sdpQueryComplete(IOBluetoothDeviceRef deviceRef, IOReturn status);
};

#define DATA_BLOB_MAX  0x4000

#define DATA_ELEMENT_TYPE_NULL 0x0000
#define DATA_ELEMENT_TYPE_U_INT_1 0x0008
#define DATA_ELEMENT_TYPE_U_INT_2 0x0009
#define DATA_ELEMENT_TYPE_U_INT_4 0x000A
#define DATA_ELEMENT_TYPE_U_INT_8 0x000B
#define DATA_ELEMENT_TYPE_U_INT_16 0x000C
#define DATA_ELEMENT_TYPE_INT_1 0x0010
#define DATA_ELEMENT_TYPE_INT_2 0x0011
#define DATA_ELEMENT_TYPE_INT_4 0x0012
#define DATA_ELEMENT_TYPE_INT_8 0x0013
#define DATA_ELEMENT_TYPE_INT_16 0x0014
#define DATA_ELEMENT_TYPE_URL 0x0040
#define DATA_ELEMENT_TYPE_UUID 0x0018
#define DATA_ELEMENT_TYPE_BOOL 0x0028
#define DATA_ELEMENT_TYPE_STRING 0x0020
#define DATA_ELEMENT_TYPE_DATSEQ 0x0030
#define DATA_ELEMENT_TYPE_DATALT 0x0038

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
    void writeLong(long l, int size);

    BOOL writeElement(const IOBluetoothSDPDataElementRef dataElement);
    int getLength(const IOBluetoothSDPDataElementRef dataElement);
    void getBytes(int max, int*  dataLen, UInt8* buf);
};