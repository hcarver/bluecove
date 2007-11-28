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

#import "OSXStackChannelController.h"

#define OBJC_VERSION

#ifdef OBJC_VERSION

#import <IOBluetooth/objc/IOBluetoothDevice.h>
#import <IOBluetooth/objc/IOBluetoothRFCOMMChannel.h>

@class IOBluetoothRFCOMMChannel;

class RFCOMMChannelController;

@interface RFCOMMChannelDelegate : NSObject <IOBluetoothRFCOMMChannelDelegate> {
    RFCOMMChannelController* _controller;
}
- (id)initWithController:(RFCOMMChannelController*)controller;
- (void)close;
@end

#endif

class RFCOMMChannelController : public ChannelController {
public:

#ifdef OBJC_VERSION
    RFCOMMChannelDelegate* delegate;
    IOBluetoothRFCOMMChannel* rfcommChannel;
#else
    IOBluetoothRFCOMMChannelRef rfcommChannel;
#endif

    BluetoothRFCOMMMTU	rfcommChannelMTU;

public:
    RFCOMMChannelController();
    ~RFCOMMChannelController();

#ifdef OBJC_VERSION
    void initDelegate();
    void rfcommChannelOpenComplete(IOReturn error);
    void rfcommChannelData(void*dataPointer, size_t dataLength);
    void rfcommChannelClosed();
    void rfcommChannelWriteComplete(void* refcon, IOReturn status);
#else
    void rfcommEvent(IOBluetoothRFCOMMChannelRef rfcommChannelRef, IOBluetoothRFCOMMChannelEvent *event);
#endif

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