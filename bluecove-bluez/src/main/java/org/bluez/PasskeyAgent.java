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
package org.bluez;

/**
 * 
 * Service unique name; Interface org.bluez.PasskeyAgent; Object path freely
 * definable
 * 
 * @author vlads
 * 
 */
public interface PasskeyAgent {
/*
	String Request(String path, String address, boolean numeric)

	This method gets called when the service daemon
	needs to get the passkey for an authentication. The
	return value is actual passkey.

	The first argument contains the path of the local
	adapter and the second one the remote address. The
	third argument signals if a numeric PIN code is
	expected or not. The default is a 1 to 16 byte PIN
	code in UTF-8 format.

	throws Error.Rejected
	                 Error.Canceled

void Confirm(String path, String address, String value)

	This method gets called when the service daemon
	needs to verify a passkey. The verification is
	done by showing the value to the passkey agent
	and returning means a successful confirmation.
	In case the values don't match an error must
	be returned.

	throws Error.Rejected
	                 Error.Canceled

void Display(String path, String address, String value)

	This method gets called when the service daemon
	needs to display the passkey value. No return
	value is needed. A successful paring will be
	indicated by the Complete method and a failure
	will be signaled with Cancel.

void Keypress(String path, String address)

	This method indicates keypresses from the remote
	device. This can happen when pairing with a keyboard.

void Complete(String path, String address)

	This method gets called to indicate that the
	authentication has been completed.

void Cancel(String path, String address)

	This method gets called to indicate that the
	authentication request failed before a reply was
	returned by the Request method.

void Release()

	This method gets called when the service daemon
	unregisters a passkey agent. An agent can use
	it to do cleanup tasks. There is no need to
	unregister the agent, because when this method
	gets called it has already been unregistered.
*/	
}
