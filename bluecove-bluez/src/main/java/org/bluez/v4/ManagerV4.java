package org.bluez.v4;

import org.bluez.Error;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.Path;

@DBusInterfaceName("org.bluez.Manager") 
public interface ManagerV4 extends Manager {

	/**
	 * Returns object path for the default adapter.
	 * 
	 * @return returns Object in BlueZ 4
	 */
	Path DefaultAdapter() throws Error.InvalidArguments, Error.NoSuchAdapter;

	/**
	 * Returns object path for the specified adapter.
	 * 
	 * @param pattern
	 *            "hci0" or "00:11:22:33:44:55"
	 * @return returns Object in BlueZ 4
	 */
	Path FindAdapter(String pattern) throws Error.InvalidArguments,
			Error.NoSuchAdapter;

	/**
	 * Returns list of adapter object paths under /org/bluez
	 * 
	 * @return returns Object[] in BlueZ 4
	 */
	Path[] ListAdapters() throws Error.InvalidArguments, Error.Failed,
			Error.OutOfMemory;
}
