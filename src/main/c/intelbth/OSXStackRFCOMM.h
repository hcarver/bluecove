/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
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