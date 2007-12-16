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

#import <IOBluetooth/objc/IOBluetoothDevice.h>
#import <IOBluetooth/objc/IOBluetoothL2CAPChannel.h>

class L2CAPChannelController;

@interface L2CAPChannelDelegate : NSObject <IOBluetoothL2CAPChannelDelegate> {
    L2CAPChannelController* _controller;
}
- (id)initWithController:(L2CAPChannelController*)controller;
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

    void initDelegate();

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