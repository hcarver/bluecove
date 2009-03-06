package org.bluez.v3;

import org.bluez.Error;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.UInt32;

@DBusInterfaceName("org.bluez.Manager") 
public interface ManagerV3 extends Manager {

	/**
	 * Deprecated in BlueZ 4
	 * 
	 * @return the current interface version. At the moment only version 0 is
	 *         supported.
	 */
	public UInt32 InterfaceVersion() throws Error.InvalidArguments;

	/**
	 * Returns object path for the default adapter.
	 *
	 * @return returns Object in BlueZ 4
	 */
	String DefaultAdapter() throws Error.InvalidArguments, Error.NoSuchAdapter;
	
	/**
	 * Returns object path for the specified adapter.
	 *
	 * @param pattern
	 *            "hci0" or "00:11:22:33:44:55"
	 * @return returns Object in BlueZ 4
	 */
	String FindAdapter(String pattern) throws Error.InvalidArguments, Error.NoSuchAdapter;

	/**
	 * Returns list of adapter object paths under /org/bluez
	 *
	 * @return returns Object[] in BlueZ 4
	 */
	String[] ListAdapters() throws Error.InvalidArguments, Error.Failed, Error.OutOfMemory;
}
