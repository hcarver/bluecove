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
 * Service unique name; Interface org.bluez.AuthorizationAgent; Object path
 * freely definable
 * 
 * @author vlads
 * 
 */
public interface AuthorizationAgent {
/*
	void Authorize(String adapter_path, String address,
			String service_path, String uuid)

	This method gets called when the service daemon wants
	to get an authorization for accessing a service. This
	method should return if the remote user is granted
	access or an error otherwise.

	The adapter_path parameter is the object path of the
	local adapter. The address, service_path and action
	parameters correspond to the remote device address,
	the object path of the service and the uuid of the
	profile.

	throws Error.Rejected
	                 Error.Canceled

void Cancel(String adapter_path, String address,
			String service_path, String uuid)

	This method cancels a previous authorization request.
	The adapter_path, address, service_path and uuid
	parameters must match the same values that have been
	used when the Authorize() method was called.

void Release()

	This method gets called when the service daemon
	unregisters an authorization agent. An agent can
	use it to do cleanup tasks. There is no need to
	unregister the agent, because when this method
	gets called it has already been unregistered.
*/	
}
