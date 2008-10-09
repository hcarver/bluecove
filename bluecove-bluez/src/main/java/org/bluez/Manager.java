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

import org.bluez.Error;
import org.freedesktop.dbus.DBusInterface; 
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * This interface provides methods to list the available adapters, retrieve the
 * default local adapter path and list/activate services. There is just one
 * Manager object instance provided in the path "/org/bluez".
 * 
 * Service org.bluez; Interface org.bluez.Manager; Object path /org/bluez
 * 
 * Created base on D-Bus API description for BlueZ.
 * bluez-utils-3.17/hcid/dbus-api.txt
 * 
 * @author vlads
 * 
 */
public interface Manager extends DBusInterface {

	/**
	 * 
	 * @return the current interface version. At the moment only version 0 is
	 *         supported.
	 */
	public UInt32 InterfaceVersion() throws Error.InvalidArguments;
	 
	/**
	 * Returns object path for the default adapter.
	 * 
	 * @return
	 */
	String DefaultAdapter() throws Error.InvalidArguments, Error.NoSuchAdapter;
	
	/**
	 * Returns object path for the specified adapter.
	 * 
	 * @param pattern
	 *            "hci0" or "00:11:22:33:44:55"
	 * @return
	 */
	String FindAdapter(String pattern) throws Error.InvalidArguments, Error.NoSuchAdapter;


	/**
	 * Returns list of adapter object paths under /org/bluez
	 * 
	 * @return
	 */
	String[] ListAdapters() throws Error.InvalidArguments, Error.Failed, Error.OutOfMemory;


	/**
	 * Returns object path for the specified service. Valid patterns are the
	 * unqiue identifier or a bus name.
	 * 
	 * @param pattern
	 * @return
	 */
	String FindService(String pattern) throws Error.InvalidArguments, Error.NoSuchService;


	/**
	 * Returns list of object paths of current services.
	 * 
	 * @return
	 * @throws Error.InvalidArguments
	 */
	String[] ListServices() throws Error.InvalidArguments;


	/**
	 * Returns the unqiue bus id of the specified service. Valid patterns are
	 * the same as for FindService(). If the service is not running it will be
	 * started.
	 * 
	 * @param pattern
	 * @return
	 */
	String ActivateService(String pattern);

	/**
	 * Parameter is object path of added adapter.
	 * @author vlads
	 *
	 */
	public class AdapterAdded extends DBusSignal {
		public AdapterAdded(String path) throws DBusException {
			super(path);
		}
	}
	
//	void AdapterRemoved(String path)
//
//	Parameter is object path of removed adapter.
//
	
//void DefaultAdapterChanged(String path)
//
//	Parameter is object path of the new default adapter.
//

//	void ServiceAdded(String path)
//
//	Parameter is object path of registered service agent.
//

//void ServiceRemoved(String path)
//
//	Parameter is object path of unregistered service agent.
}
