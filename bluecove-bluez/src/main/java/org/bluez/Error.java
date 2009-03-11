/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2009 Vlad Skarzhevskyy
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
 *  =======================================================================================
 *
 *  BlueZ Java docs licensed under GNU Free Documentation License, Version 1.1 http://www.fsf.org
 *  Copyright (C) 2004-2008  Marcel Holtmann <marcel@holtmann.org>
 *  Copyright (C) 2005-2006  Johan Hedberg <johan.hedberg@nokia.com>
 *  Copyright (C) 2005-2006  Claudio Takahasi <claudio.takahasi@indt.org.br>
 *  Copyright (C) 2006-2007  Luiz von Dentz <luiz.dentz@indt.org.br> 
 *
 *  @author vlads
 *  @version $Id$
 */
package org.bluez;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Errors thrown by hcid or any bluetooth service.
 * 
 * Created base on D-Bus API description for BlueZ bluez-utils-3.36/hcid/dbus-api.txt and
 * bluez-4.32/doc/*.txt
 * 
 */
public interface Error extends DBusInterface {

    /**
     * The remote device is either powered down or out of range.
     */
    @SuppressWarnings("serial")
    public class DeviceUnreachable extends DBusExecutionException {
        public DeviceUnreachable(String message) {
            super(message);
        }
    }

    /**
     * A connection request has been received on an already connected device.
     */
    @SuppressWarnings("serial")
    public class AlreadyConnected extends DBusExecutionException {
        public AlreadyConnected(String message) {
            super(message);
        }
    }

    /**
     * An unexpected error (other than DeviceUnreachable) error has occurred while
     * attempting a connection to a device.
     */
    @SuppressWarnings("serial")
    public class ConnectionAttemptFailed extends DBusExecutionException {
        public ConnectionAttemptFailed(String message) {
            super(message);
        }
    }

    /**
     * This is a the most generic error. It is thrown when something unexpected happens.
     * 
     * The error messages is taken from the strerror(errno) function.
     * 
     */
    @SuppressWarnings("serial")
    public class Failed extends DBusExecutionException {
        public Failed(String message) {
            super(message);
        }
    }

    /**
     * The DBUS request does not contain the right number of arguments with the right
     * type, or the arguments are there but their value is wrong, or does not makes sense
     * in the current context.
     */
    @SuppressWarnings("serial")
    public class InvalidArguments extends DBusExecutionException {
        public InvalidArguments(String message) {
            super(message);
        }
    }

    /**
     * Error returned when the caller of a method is not authorized. This might happen if
     * a caller tries to terminate a connection that it hasn't created.
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class NotAuthorized extends DBusExecutionException {
        public NotAuthorized(String message) {
            super(message);
        }
    }

    /**
     * Error returned when a memory allocation via malloc() fails. This error is similar
     * to ENOMEM.
     */
    @SuppressWarnings("serial")
    public class OutOfMemory extends DBusExecutionException {
        public OutOfMemory(String message) {
            super(message);
        }
    }

    /**
     * Error returned when the requested adapter doesn't exists. This error is similar to
     * ENODEV.
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class NoSuchAdapter extends DBusExecutionException {
        public NoSuchAdapter(String message) {
            super(message);
        }
    }

    /**
     * Error returned when the adapter is DOWN.
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class NotReady extends DBusExecutionException {
        public NotReady(String message) {
            super(message);
        }
    }

    /**
     * This is an experimental method.
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class UnknwownMethod extends DBusExecutionException {
        public UnknwownMethod(String message) {
            super(message);
        }
    }

    /**
     * Error returned when a specified record is not available.
     */
    @SuppressWarnings("serial")
    public class NotAvailable extends DBusExecutionException {
        public NotAvailable(String message) {
            super(message);
        }
    }

    /**
     * The remote device is not connected, while the method call would expect it to be, or
     * is not in the expected state to perform the action.
     */
    @SuppressWarnings("serial")
    public class NotConnected extends DBusExecutionException {
        public NotConnected(String message) {
            super(message);
        }
    }

    /**
     * One of the requested elements already exists
     * 
     * Error returned if a record for a specific procedure already exists and it has been
     * tried create a new one. The error message however should indicate the procedure
     * that fails. For example "Bonding already exists"
     */
    @SuppressWarnings("serial")
    public class AlreadyExists extends DBusExecutionException {
        public AlreadyExists(String message) {
            super(message);
        }
    }

    /**
     * One of the requested elements does not exist
     * 
     * Error returned if a record for a specific procedure doesn't exist. The error
     * message however should indicate the procedure that fails. For example
     * "Bonding does not exist".
     */
    @SuppressWarnings("serial")
    public class DoesNotExist extends DBusExecutionException {
        public DoesNotExist(String message) {
            super(message);
        }
    }

    /**
     * Error returned if an operation is in progress. Since this is a generic error that
     * can be used in various situations, the error message should be more clear about
     * what is in progress. For example "Bonding in progress".
     */
    @SuppressWarnings("serial")
    public class InProgress extends DBusExecutionException {
        public InProgress(String message) {
            super(message);
        }
    }

    /**
     * Rejected
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class Rejected extends DBusExecutionException {
        public Rejected(String message) {
            super(message);
        }
    }

    /**
     * The operation was canceled.
     */
    @SuppressWarnings("serial")
    public class Canceled extends DBusExecutionException {
        public Canceled(String message) {
            super(message);
        }
    }

    /**
     * The remote device does not support the expected feature.
     */
    @SuppressWarnings("serial")
    public class NotSupported extends DBusExecutionException {
        public NotSupported(String message) {
            super(message);
        }
    }

    /**
     * No Such Service.
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class NoSuchService extends DBusExecutionException {
        public NoSuchService(String message) {
            super(message);
        }
    }

    /**
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class RequestDeferred extends DBusExecutionException {
        public RequestDeferred(String message) {
            super(message);
        }
    }

    /**
     * Not In Progress
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class NotInProgress extends DBusExecutionException {
        public NotInProgress(String message) {
            super(message);
        }
    }

    /**
     * Unsupported Device Class
     * 
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class UnsupportedMajorClass extends DBusExecutionException {
        public UnsupportedMajorClass(String message) {
            super(message);
        }
    }

    /**
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class AuthenticationFailed extends DBusExecutionException {
        public AuthenticationFailed(String message) {
            super(message);
        }
    }

    /**
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class AuthenticationTimeout extends DBusExecutionException {
        public AuthenticationTimeout(String message) {
            super(message);
        }
    }

    /**
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class AuthenticationRejected extends DBusExecutionException {
        public AuthenticationRejected(String message) {
            super(message);
        }
    }

    /**
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class AuthenticationCanceled extends DBusExecutionException {
        public AuthenticationCanceled(String message) {
            super(message);
        }
    }

    /**
     * Hcid specific Error (Can be thrown by hcid only)
     */
    @SuppressWarnings("serial")
    public class RepeatedAttempts extends DBusExecutionException {
        public RepeatedAttempts(String message) {
            super(message);
        }
    }

}
