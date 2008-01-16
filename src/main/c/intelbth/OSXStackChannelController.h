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