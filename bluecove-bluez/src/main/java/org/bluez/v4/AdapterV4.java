package org.bluez.v4;

import org.bluez.Adapter;
import org.freedesktop.dbus.DBusInterfaceName;

import com.intel.bluetooth.dbus.DBusProperties;
import com.intel.bluetooth.dbus.Property;

@DBusInterfaceName("org.bluez.Adapter") 
public interface AdapterV4 extends Adapter, DBusProperties {

	public static enum Properties implements Property {
		
	    /**
	     * The Bluetooth device address. Example: "00:11:22:33:44:55"
	     */
	    Address,
	    
	    /**
		 * The Bluetooth friendly name. This value can be changed and a
		 * PropertyChanged signal will be emitted.
		 * 
		 * [readwrite]
		 */
		Name;
	    
//
//	     		boolean Powered [readwrite]
//
//	     			Switch an adapter on or off. This will also set the
//	     			appropiate connectable state.
//
//	     		boolean Discoverable [readwrite]
//
//	     			Switch an adapter to discoverable or non-discoverable
//	     			to either make it visible or hide it. This is a global
//	     			setting and should only be used by the settings
//	     			application.
//
//	     			If the DiscoverableTimeout is set to a non-zero
//	     			value then the system will set this value back to
//	     			false after the timer expired.
//
//	     			In case the adapter is switched off, setting this
//	     			value will fail.
//
//	     			When changing the Powered property the new state of
//	     			this property will be updated via a PropertyChanged
//	     			signal.
//
//	     		boolean Pairable [readwrite]
//
//	     			Switch an adapter to pairable or non-pairable. This is
//	     			a global setting and should only be used by the
//	     			settings application.
//
//	     			Note that this property only affects incoming pairing
//	     			requests.
//
//	     		uint32 PaireableTimeout [readwrite]
//
//	     			The pairable timeout in seconds. A value of zero
//	     			means that the timeout is disabled and it will stay in
//	     			pareable mode forever.
//
//	     		uint32 DiscoverableTimeout [readwrite]
//
//	     			The discoverable timeout in seconds. A value of zero
//	     			means that the timeout is disabled and it will stay in
//	     			discoverable/limited mode forever.
//
//	     			The default value for the discoverable timeout should
//	     			be 180 seconds (3 minutes).
//
//	     		boolean Discovering [readonly]
//
//	     			Indicates that a device discovery procedure is active.
	}

}
