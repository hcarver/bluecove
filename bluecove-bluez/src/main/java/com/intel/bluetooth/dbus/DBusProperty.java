package com.intel.bluetooth.dbus;

public abstract class DBusProperty {

	public static String getValue(DBusProperties dBusInterface, Property propertyEnum) {
		return (String)dBusInterface.GetProperties().get(getPropertyName(propertyEnum)).getValue();
	}
	
	public static String getPropertyName(Property propertyEnum) {
        return propertyEnum.toString();
    }
}
