package org.bluez.v3;

import org.bluez.Adapter;
import org.bluez.Error;
import org.freedesktop.dbus.DBusInterfaceName;

@DBusInterfaceName("org.bluez.Adapter") 
public interface AdapterV3 extends Adapter {

    /**
     * Returns the device address for a given path. Example: "00:11:22:33:44:55"
     */
    String GetAddress();
    
    /**
     * Returns the local adapter name (friendly name) in UTF-8.
     */
    String GetName() throws Error.NotReady, Error.Failed;
}
