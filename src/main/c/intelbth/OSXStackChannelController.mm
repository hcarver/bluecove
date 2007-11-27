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
 *  @version $Id: OSXStackRFCOMM.mm 1244 2007-11-27 04:06:32Z skarzhevskyy $
 */

#import "OSXStackChannelController.h"

#define CPP_FILE "OSXStackChannelController.mm"

ChannelController::ChannelController() {
    openStatus = kIOReturnSuccess;
    closedStatus = kIOReturnSuccess;
    isClosed = false;
	isConnected = false;
	MPCreateEvent(&notificationEvent);
	MPCreateEvent(&writeCompleteNotificationEvent);
}

ChannelController::~ChannelController() {
    MPSetEvent(notificationEvent, 0);
    MPSetEvent(writeCompleteNotificationEvent, 0);
    MPDeleteEvent(notificationEvent);
    MPDeleteEvent(writeCompleteNotificationEvent);
}

BOOL ChannelController::waitForConnection(JNIEnv *env, jint timeout) {
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent ();
    while ((stack != NULL) && (!isClosed) && (!isConnected)) {
        MPEventFlags flags;
        MPWaitForEvent(notificationEvent, &flags, kDurationMillisecond * 500);
        CFAbsoluteTime nowTime = CFAbsoluteTimeGetCurrent ();
        if ((timeout > 0) && ((nowTime - startTime) * 1000  > timeout)) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_TIMEOUT, "Connection timeout");
        }
    }
    if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return false;
	}
    if (isClosed) {
	    throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection [0x%08x]", closedStatus);
	    return false;
    }

    if (openStatus != kIOReturnSuccess) {
        isConnected = false;
        throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection [0x%08x]", openStatus);
        return false;
    }

    if (isConnected) {
        return true;
    }

    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to open connection");
	return false;
}