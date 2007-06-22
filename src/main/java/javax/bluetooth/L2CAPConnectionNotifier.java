/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
 *  
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *   
 *   
 *  @version $Id$
 */ 
package javax.bluetooth;

/**
 * The L2CAPConnectionNotifier interface provides an L2CAP connection notifier.
 * <p>
 * To create a server connection, the protocol must be btl2cap. The target contains 
 * "localhost:" and the UUID of the service. The parameters are ReceiveMTU and 
 * TransmitMTU, the same parameters used to define a client connection. Here is 
 * an example of a valid server connection string:
 * {@code btl2cap://localhost:3B9FA89520078C303355AAA694238F07;ReceiveMTU=512;TransmitMTU=512}
 * <p>
 * A call to {@link javax.microedition.io.Connector#open(String, int, boolean)} with this string 
 * will return a {@link javax.bluetooth.L2CAPConnectionNotifier} object. An {@link 
 * javax.bluetooth.L2CAPConnectionNotifier} object is obtained from the 
 * {@link javax.bluetooth.L2CAPConnectionNotifier} by calling the method
 * {@link javax.bluetooth.L2CAPConnectionNotifier#acceptAndOpen()}.
 *
 */
public interface L2CAPConnectionNotifier {

	/**
	 * Waits for a client to connect to this L2CAP service. Upon connection
	 * returns an {@link javax.bluetooth.L2CAPConnection} that can be used to
	 * communicate with this client.
	 * <p>
	 * A service record associated with this connection will be added to the
	 * SDDB associated with this {@link javax.bluetooth.L2CAPConnectionNotifier}
	 * object if one does not exist in the SDDB. This method will put the local
	 * device in connectable mode so that it may respond to connection attempts
	 * by clients.
	 * <p>
	 * The following checks are done to verify that any modifications made by
	 * the application to the service record after it was created by
	 * {@link javax.microedition.io.Connector#open(String, int, boolean)} have
	 * not created an invalid service record. If any of these checks fail, then
	 * a {@link javax.bluetooth.ServiceRegistrationException} is thrown.
	 * <p>
	 * <ul>
	 * <li>ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a btl2cap service record, must be present in the service
	 * record.</li>
	 * <li>L2CAP must be in the ProtocolDescriptorList.</li>
	 * <li>The PSM value must not have changed in the service record.</li>
	 * </ul>
	 * <p>
	 * This method will not ensure that the service record created is a
	 * completely valid service record. It is the responsibility of the
	 * application to ensure that the service record follows all of the
	 * applicable syntactic and semantic rules for service record correctness.
	 * <p>
	 * Note : once an application invokes {@code close()} on any
	 * {@code L2CAPConnectionNotifier}, {@code SessionNotifier}, or
	 * {@code StreamConnectionNotifer} instance, all pending
	 * {@code acceptAndOpen()} methods that have been invoked previously on that
	 * instance MUST throw {@code InterruptedIOException}. This mechanism
	 * provides an application with the means to cancel any outstanding
	 * {@code acceptAndOpen()} method calls
	 * 
	 * @return a connection to communicate with the client
	 * @throws java.io.IOException
	 *             if the notifier is closed before {@code acceptAndOpen()} is
	 *             called
	 * @throws javax.bluetooth.ServiceRegistrationException
	 *             if the structure of the associated service record is invalid
	 *             or if the service record could not be added successfully to
	 *             the local SDDB. The structure of service record is invalid if
	 *             the service record is missing any mandatory service
	 *             attributes, or has changed any of the values described above
	 *             which are fixed and cannot be changed. Failures to add the
	 *             record to the SDDB could be due to insufficient disk space,
	 *             database locks, etc.
	 * @throws javax.bluetooth.BluetoothStateException
	 *             if the server device could not be placed in connectable mode
	 *             because the device user has configured the device to be
	 *             non-connectable.
	 */
	public L2CAPConnection acceptAndOpen() throws java.io.IOException;
}
