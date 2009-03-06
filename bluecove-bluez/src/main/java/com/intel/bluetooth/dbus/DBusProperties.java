package com.intel.bluetooth.dbus;

import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;

public interface DBusProperties extends DBusInterface {

	/**
	 * Returns all properties for the interface. See the properties section for
	 * available properties.
	 * 
	 * @return
	 */
	public Map<String, Variant<Object>> GetProperties();

	/**
	 * Changes the value of the specified property. Only properties that are
	 * listed a read-write are changeable.
	 * 
	 * @param name
	 * @param value
	 */
	public void SetProperty(String name, Variant<Object> value);
}
