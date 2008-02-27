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