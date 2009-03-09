/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  =======================================================================================
 *
 *  BlueZ Java docs licensed under GNU Free Documentation License, Version 1.1 http://www.fsf.org
 *  Copyright (C) 2004-2008  Marcel Holtmann <marcel@holtmann.org>
 *  Copyright (C) 2005-2006  Johan Hedberg <johan.hedberg@nokia.com>
 *  Copyright (C) 2005-2006  Claudio Takahasi <claudio.takahasi@indt.org.br>
 *  Copyright (C) 2006-2007  Luiz von Dentz <luiz.dentz@indt.org.br>
 *
 *  @author vlads
 *  @version $Id$
 */
package org.bluez.v3;

import java.util.Map;

import org.bluez.Error;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * The "Adapter" interface provides methods to setup the local adapter(s), search remote devices, pairing and search for
 * services.
 * <p>
 * This interface is considered stable and no change is planned.
 * <p>
 * For each available adapter the hcid register an adapter object instance provided in the path
 * "/org/bluez/hci{0, 1, 2, ...}".
 * 
 * Service org.bluez
 * <p>
 * Interface org.bluez.Adapter
 * <p>
 * Object path /org/bluez/{hci0,hci1,...}
 * 
 * Created base on D-Bus API description for BlueZ bluez-utils-3.36/hcid/dbus-api.txt
 */
@DBusInterfaceName("org.bluez.Adapter")
public interface Adapter extends org.bluez.Adapter {

	/**
	 * Returns the properties of the local adapter.
	 */
	Map GetInfo() throws Error.NotReady;

	/**
	 * Returns the device address for a given path. Example: "00:11:22:33:44:55"
	 */
	String GetAddress() throws Error.NotReady;

	/**
	 * Returns the version of the Bluetooth chip. This version is compiled from the LMP version. In case of EDR the
	 * features attribute must be checked.
	 * 
	 * @return Example: "Bluetooth 2.0 + EDR"
	 */
	String GetVersion();

	/**
	 * Returns the revision of the Bluetooth chip. This is a vendor specific value and in most cases it represents the
	 * firmware version. This might derive from the HCI revision and LMP subversion values or via extra vendor specific
	 * commands.
	 * 
	 * In case the revision of a chip is not available. This method should return the LMP subversion value as a String.
	 * 
	 * 
	 * @return Example: "HCI 19.2"
	 * @throws Error.Failed
	 */
	String GetRevision() throws Error.Failed;

	/**
	 * Returns the manufacturer of the Bluetooth chip. If the company id is not know the sting "Company ID %d" where %d
	 * should be replaced with the numeric value from the manufacturer field.
	 * 
	 * Example: "Cambridge Silicon Radio"
	 */
	String GetManufacturer() throws Error.Failed;

	/**
	 * Returns the company name from the OUI database of the Bluetooth device address. This function will need a valid
	 * and up-to-date oui.txt from the IEEE. This value will be different from the manufacturer string in the most
	 * cases.
	 * 
	 * If the oui.txt file is not present or the OUI part of the BD_ADDR is not listed, it should return the string
	 * "OUI %s" where %s is the actual OUI.
	 * 
	 * Example: "Apple Computer"
	 */
	String GetCompany() throws Error.Failed;

	/**
	 * Returns a list of available modes the adapter can be switched into.
	 */
	String[] ListAvailableModes();

	/**
	 * Returns the current mode of a adapter.
	 * 
	 * @return Valid modes: "off", "connectable", "discoverable", "limited"
	 */
	String GetMode();

	/**
	 * Sets mode of the adapter. See GetMode for valid Strings for the mode parameter.
	 * 
	 * Sets mode of the adapter. See GetMode for valid strings for the mode parameter. In addition to the valid strings
	 * for GetMode, this method also supports a special parameter value "on" which will change the mode to the previous
	 * non-off mode (or do nothing if the current mode isn't "off").
	 * 
	 * @param mode
	 */
	void SetMode(String mode) throws Error.Failed, Error.NoSuchAdapter;

	/**
	 * Returns the discoverable timeout in seconds. A value of zero means that the timeout is disabled and it will stay
	 * in discoverable/limited mode forever.
	 * 
	 * The default value for the discoverable timeout should be 180 seconds (3 minutes).
	 */
	UInt32 GetDiscoverableTimeout();

	/**
	 * Sets the discoverable timeout in seconds. A value of zero disables the timeout and the adapter would be always
	 * discoverable/limited.
	 * 
	 * Changing this value doesn't set the adapter into discoverable/limited mode. The SetMode method must be used.
	 * 
	 */
	void SetDiscoverableTimeout(UInt32 timeout) throws Error.NotReady, Error.InvalidArguments;

	/**
	 * Returns true if the local adapter is connectable and false if it is switched off.
	 * 
	 * It is also possible to use GetMode to retrieve this information.
	 */
	boolean IsConnectable();

	/**
	 * Returns true if the local adapter is discoverable/limited and false if it is only connectable or switched off.
	 * 
	 * It is also possible to use GetMode to retrieve this information.
	 */
	boolean IsDiscoverable();

	/**
	 * Return true if the local adapter is connected to the remote device.
	 * 
	 * @param address
	 * @return
	 */
	boolean IsConnected(String address) throws Error.InvalidArguments;

	/**
	 * Returns a list with addresses of currently connected remote devices.
	 */
	String[] ListConnections();

	/**
	 * Returns the current major class value for this system. Currently, only "computer" is supported. For the other
	 * values, unsupported major class error is returned.
	 */
	String GetMajorClass() throws Error.InvalidArguments, Error.UnsupportedMajorClass;

	/**
	 * Returns a list of available minor classes for the currently used major class. At the moment this should only
	 * return a list of minor classes if the major class is set to "computer".
	 * 
	 * If the major class is not "computer" an error should be returned.
	 */
	String[] ListAvailableMinorClasses() throws Error.InvalidArguments, Error.UnsupportedMajorClass;

	/**
	 * Returns the current minor class value for this system where the default major class is "computer".
	 * 
	 * If the major class is not "computer" an error should be returned.
	 * 
	 * Valid values: "uncategorized", "desktop", "server", "laptop", "handheld", "palm", "wearable"
	 * 
	 * The default value is "uncategorized".
	 */
	String GetMinorClass() throws Error.InvalidArguments, Error.UnsupportedMajorClass;

	/**
	 * Sets the local minor class and on success it sends a MinorClassChanged signal.
	 * 
	 * If the major class is not "computer" an error should be returned.
	 */
	void SetMinorClass(String minor) throws Error.NotReady, Error.InvalidArguments, Error.NoSuchAdapter, Error.Failed,
			Error.UnsupportedMajorClass;

	/**
	 * Returns the current set of service classes.
	 * 
	 * In the case no service classes are set (when no service has been registered) an empty list should be returned.
	 * 
	 * Valid values: "positioning", "networking", "rendering", "capturing", "object transfer", "audio", "telephony",
	 * "information"
	 */
	String[] GetServiceClasses() throws Error.NotReady, Error.NoSuchAdapter, Error.Failed;

	/**
	 * Returns the local adapter name (friendly name) in UTF-8.
	 */
	String GetName() throws Error.NotReady, Error.Failed;

	/**
	 * Sets the local adapter name. If EIR is supported by the local hardware this modifies also the extended response
	 * data value.
	 * 
	 * Questions: What to do (in case of EIR) if one low-level API call fails.
	 */
	void SetName(String name) throws Error.InvalidArguments, Error.Failed;

	/**
	 * Returns the properties for a remote device.
	 */
	Map GetRemoteInfo(String address);

	/**
	 * Get the version info for a remote device. This request returns always this information based on its cached data.
	 * The base for this string is the LMP version value and the features for EDR support.
	 * 
	 * Not available can be received if the remote device was not contacted(connected) previously. Remote data is
	 * automatically retrieved in the first connection.
	 * 
	 * Example: "Bluetooth 2.0 + EDR"
	 */
	String GetRemoteVersion(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the revision of the Bluetooth chip. This is a vendor specific value and in most cases it represents the
	 * firmware version. This derives only from the LMP subversion value.
	 * 
	 * Example: "HCI 19.2"
	 */
	String GetRemoteRevision(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the manufacturer of the chip for a remote device.
	 * 
	 * Example: "Nokia Mobile Phones"
	 */
	String GetRemoteManufacturer(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the company name from the OUI database of the Bluetooth device address. This function will need a valid and
	 * up-to-date oui.txt from the IEEE. This value will be different from the manufacturer string in the most cases.
	 * 
	 * Example: "Microsoft Corporation"
	 */
	String GetRemoteCompany(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the major device class of the specified device.
	 * 
	 * Example: "computer"
	 */
	String GetRemoteMajorClass(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the minor device class of the specified device.
	 * 
	 * Example: "laptop"
	 */
	String GetRemoteMinorClass(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the service classes of the specified device.
	 * 
	 * Example: ["networking", "object transfer"]
	 */
	String[] GetRemoteServiceClasses(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the remote major, minor, and service classes encoded as 32 bit integer.
	 */
	UInt32 GetRemoteClass(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the remote features encoded as bit mask.
	 */
	byte[] GetRemoteFeatures(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Get the remote device's name. This request returns always a cached name. The service daemon is responsible for
	 * updating the cache.
	 * 
	 * NotAvailable error is returned if the name is not in the cache. But if there is a discovery running, then this
	 * function will return RequestDeferred. In this case the service daemon will queue the request and it will try to
	 * resolve the name at the next possible opportunity. On success a RemoteNameUpdated signal will be send and if a
	 * failure happens it will be indicated by a RemoteNameFailed signal.
	 * 
	 * If this is an empty String, the UI might want to display the BD_ADDR instead.
	 * 
	 * Example: "00:11:22:33:44:55", "Nokia 770"
	 * 
	 * throws Error.InvalidArguments Error.NotAvailable Error.NotReady Error.RequestDeferred
	 */
	String GetRemoteName(String address) throws Error.InvalidArguments, Error.NotAvailable, Error.NotReady,
			Error.RequestDeferred;

	/**
	 * Returns alias name for remote device. If this is an empty String value, the UI should show the remote name
	 * instead.
	 * 
	 * An alias should supersede the remote name.
	 */
	String GetRemoteAlias(String address) throws Error.InvalidArguments, Error.NotAvailable;

	/**
	 * Sets alias name for remote device. If alias name is empty, then no alias is set.
	 * 
	 * On success the SetRemoteAlias method will produce a RemoteAliasChanged signal which applications can use to
	 * update their current display of the remote device name.
	 */
	void SetRemoteAlias(String address, String alias) throws Error.Failed, Error.InvalidArguments;

	/**
	 * Resets alias name for remote device. If there is no alias set for the device this method will silently succeed,
	 * but no RemoteAliasCleared signal has to be sent in this case.
	 * 
	 * On success the ClearRemoteAlias method will produce a RemoteAliasCleared signal.
	 */
	void ClearRemoteAlias(String address) throws Error.Failed, Error.InvalidArguments;

	// String LastSeen(String address)
	//
	// Returns the date and time when the adapter has been
	// seen by a discover procedure.
	//
	// Example: "2006-02-08 12:00:00 GMT"
	//
	// throws Error.InvalidArguments
	// Error.NotAvailable
	//
	// Question: Can we find a better name?
	//
	// String LastUsed(String address)
	//
	// Returns the date and time of the last time when the
	// adapter has been connected.
	//
	// Example: "2006-02-08 12:00:00 GMT"
	//
	// throws Error.InvalidArguments
	// Error.NotAvailable
	//
	// Question: Can we find a better name?
	//
	// void DisconnectRemoteDevice(String address)
	//
	// This method disconnects a specific remote device by
	// terminating the low-level ACL connection. The use of
	// this method should be restricted to administrator
	// use.
	//
	// A RemoteDeviceDisconnectRequested signal will be
	// sent and the actual disconnection will only happen 2
	// seconds later. This enables upper-level applications
	// to terminate their connections gracefully before the
	// ACL connection is terminated.
	//
	// throws Error.NotReady
	// Error.Failed
	// Error.NoSuchAdapter
	// Error.InvalidArguments
	// Error.NotConnected
	// Error.InProgress
	//

	/**
	 * This method creates a bonding with a remote device.
	 * 
	 * If a link key for this adapter already exists, this procedure should fail instead of trying to create a new
	 * pairing.
	 * 
	 * If no connection to the remote device exists, a low-level ACL connection must be created.
	 * 
	 * This function will block and the calling application should take care of setting are higher timeout. This might
	 * be needed in case of a page timeout from the low-level HCI commands.
	 * 
	 * In case of success it will send a BondingCreated signal.
	 */
	void CreateBonding(String address) throws Error.NotReady, Error.Failed, Error.InvalidArguments,
			Error.AlreadyExists, Error.InProgress, Error.NoSuchAdapter, Error.ConnectionAttemptFailed,
			Error.AuthenticationFailed, Error.AuthenticationTimeout, Error.AuthenticationRejected,
			Error.AuthenticationCanceled;

	// void CancelBondingProcess(String address)
	//
	// This method will cancel the CreateBonding process.
	//
	// The CreateBonding method will return
	// AuthenticationCanceled to signal that an attempt to
	// create a bonding has been canceled.
	//
	// throws Error.NotReady
	// Error.Failed
	// Error.InvalidArguments
	// Error.NotInProgress
	// Error.NotAuthorized
	//

	/**
	 * This method removes the bonding with a remote device.
	 * 
	 * For security reasons this includes removing the actual link key and also disconnecting any open connections for
	 * the remote device.
	 * 
	 * If the link key was stored on the Bluetooth chip, it must be removed from there, too.
	 * 
	 * After deleting the link key this method will send a BondingRemoved signal.
	 */
	void RemoveBonding(String address) throws Error.NotReady, Error.Failed, Error.InvalidArguments,
			Error.NoSuchAdapter, Error.DoesNotExist;

	/**
	 * Returns true if the remote device is bonded and false if no link key is available.
	 * 
	 * @param address
	 * @return
	 */
	boolean HasBonding(String address) throws Error.InvalidArguments;

	/**
	 * List device addresses of currently bonded adapter.
	 * 
	 * @return
	 */
	String[] ListBondings();

	//
	// uint8 GetPinCodeLength(String address)
	//
	// Returns the PIN code length that was used in the
	// pairing process.
	//
	// throws Error.InvalidArguments
	// Error.DoesNotExist
	//
	// uint8 GetEncryptionKeySize(String address)
	//
	// Returns the currently used encryption key size.
	//
	// This method will fail if no connection to the address
	// has been established.
	//
	// throws Error.InvalidArguments
	// Error.Failed
	//
	// void SetTrusted(String address)
	//
	// Marks the remote device as trusted. Authorization
	// request will automatically succeed.
	//
	// throws Error.InvalidArguments
	// Error.AlreadyExists
	//
	// boolean IsTrusted(String address)
	//
	// Returns true if the user is trusted or false otherwise.
	// The address parameter must match one of the remote
	// devices of the service.
	//
	// throws Error.InvalidArguments
	//

	/**
	 * Marks the remote device as not trusted.
	 */
	void RemoveTrust(String address) throws Error.InvalidArguments, Error.DoesNotExist;

	/**
	 * Returns a list of remote devices that are trusted.
	 * 
	 * @return
	 */
	String[] ListTrusts();

	/**
	 * This method starts the device discovery procedure. This includes an inquiry procedure and remote device name
	 * resolving.
	 * <p>
	 * On start up this process will generate a DiscoveryStarted signal and then return RemoteDeviceFound and also
	 * RemoteNameUpdated signals. If the procedure has been finished an DiscoveryCompleted signal will be sent.
	 */
	void DiscoverDevices() throws Error.NotReady, Error.Failed, Error.InProgress, Error.NoSuchAdapter;

	/**
	 * This method starts the device discovery procedure. This includes an inquiry and an optional remote device name
	 * resolving. The remote names can be retrieved with GetRemoteName and in the case a name doesn't exist it will be
	 * queued for later resolving and GetRemoteName will return an error.
	 * <p>
	 * While this procedure is running every found device will be returned with RemoteDeviceFound. While
	 * DiscoverDevices() automatically resolves unknown devices names and sends RemoteNameUpdated in this case it will
	 * only happen if GetRemoteName has been called and no previously stored name is available.
	 */
	void DiscoverDevicesWithoutNameResolving() throws Error.NotReady, Error.Failed, Error.InProgress,
			Error.NoSuchAdapter;

	/**
	 * This method will cancel any previous DiscoverDevices or DiscoverDevicesWithoutNameResolving actions.
	 */
	void CancelDiscovery() throws Error.NotReady, Error.Failed, Error.NotAuthorized, Error.NoSuchAdapter;

	// void StartPeriodicDiscovery()
	//
	// This method starts a periodic discovery.
	//
	// throws Error.NotReady
	// Error.Failed
	// Error.InProgress
	// Error.NoSuchAdapter
	//
	// void StopPeriodicDiscovery()
	//
	// This method stops a periodic discovery. If the
	// adapter is not in the periodic inquiry mode an
	// error(not authorized) is returned. Everyone can
	// request exit from this mode, it is not restricted
	// to start requestor.
	//
	// throws Error.NotReady
	// Error.Failed
	// Error.NotAuthorized
	// Error.NoSuchAdapter
	//
	// boolean IsPeriodicDiscovery()
	//
	// Returns true if the periodic inquiry is active and
	// false if it is switched off.
	//
	// throws none
	//
	// void SetPeriodicDiscoveryNameResolving(boolean resolve_names)
	//
	// Enable or disable automatic remote name resolving for
	// periodic discovery.
	//
	// throws Error.InvalidArguments
	//
	// boolean GetPeriodicDiscoveryNameResolving()
	//
	// Check if automatic remote name resolving is enabled or not
	// for periodic discovery.
	//
	// Possible error: Error.InvalidArguments
	//
	//

	// array{UInt32} GetRemoteServiceHandles(String address, String match)

	/**
	 * 
	 This method will request the SDP database of a remote device and retrieve the service record handles. To request
	 * service browse send an empty match String.
	 */
	public UInt32[] GetRemoteServiceHandles(String address, String match) throws Error.InvalidArguments,
			Error.InProgress, Error.ConnectionAttemptFailed, Error.Failed;

	/**
	 * This method will request the SDP database of a remote device for a service record and return the binary stream of
	 * it.
	 * 
	 */
	public byte[] GetRemoteServiceRecord(String address, UInt32 handle) throws Error.InvalidArguments,
			Error.InProgress, Error.Failed;

	/**
	 * This method will request the SDP database of a remote device for a service record and return its data in XML
	 * format.
	 */
	public String GetRemoteServiceRecordAsXML(String address, UInt32 handle) throws Error.InvalidArguments,
			Error.InProgress, Error.Failed;

	//
	// array{String} GetRemoteServiceIdentifiers(String address)
	//
	// This method will request the SDP database of a remote
	// device for all supported services. The identifiers are
	// returned in UUID 128 String format.
	//
	// throws Error.InProgress
	// Error.Failed
	//
	// void FinishRemoteServiceTransaction(String address)
	//
	// This method will finish all SDP transaction for that
	// given address. In general this call is not needed,
	// but in cases of resources restricted devices it
	// is useful to call this to finish the SDP transaction
	// before proceeded with profile specific connections.
	//
	// array{String} ListRemoteDevices()
	//
	// List addresses of all known remote devices (bonded,
	// trusted and used).
	//
	// throws none
	//
	// array{String} ListRecentRemoteDevices(String date)
	//
	// List addresses of all bonded, trusted, seen or used remote
	// devices since date. Bonded and trusted devices are always
	// included(the date informed is not applied).
	//
	// date format is "YYYY-MM-DD HH:MM:SS GMT"
	//
	// throws none
	//
	// Signals void ModeChanged(String mode)
	//
	// If the current mode is changed with SetMode this signal
	// will inform about the new mode.
	//
	// This signal can also be triggered by low-level HCI
	// commands.
	//
	// void DiscoverableTimeoutChanged(UInt32 timeout)
	//
	// After changing the discoverable timeout this signal
	// provide the new timeout value.
	//
	// void MinorClassChanged(String minor)
	//
	// After changing the minor class with SetMinorClass this
	// signal will provide the new class value.
	//
	// void NameChanged(String name)
	//
	// After changing the local adapter name with SetName this
	// signal will provide the new name.
	//
	// This signal can also be triggered by low-level HCI
	// commands.
	//
	/**
	 * This signal indicates that a device discovery procedure has been started.
	 */
	public class DiscoveryStarted extends DBusSignal {
		public DiscoveryStarted(String path) throws DBusException {
			super(path);
		}
	}

	/*
	 * This signal indicates that a device discovery procedure has been completed.
	 */
	public class DiscoveryCompleted extends DBusSignal {
		public DiscoveryCompleted(String path) throws DBusException {
			super(path);
		}
	}

	// void PeriodicDiscoveryStarted()
	//
	// This signal indicates that a periodic discovery
	// procedure has been started.
	//
	// void PeriodicDiscoveryStopped()
	//
	// This signal indicates that a periodic discovery
	// procedure has been completed.

	/**
	 * This signal will be send every time an inquiry result has been found by the service daemon. In general they only
	 * appear during a device discovery.
	 */
	public class RemoteDeviceFound extends DBusSignal {

		private final String address;

		private final UInt32 deviceClass;

		private final int rssi;

		public RemoteDeviceFound(String path, String address, UInt32 deviceClass, int rssi) throws DBusException {
			super(path, address, deviceClass, rssi);
			this.address = address;
			this.deviceClass = deviceClass;
			this.rssi = rssi;
		}

		/**
		 * @return the address
		 */
		public String getDeviceAddress() {
			return address;
		}

		/**
		 * @return the deviceClass
		 */
		public UInt32 getDeviceClass() {
			return deviceClass;
		}

		/**
		 * @return the rssi
		 */
		public int getDeviceRssi() {
			return rssi;
		}
	}

	// void RemoteDeviceDisappeared(String address)
	//
	// This signal will be send when an inquiry session for
	// a periodic discovery finishes and previously found
	// devices are no longer in range or visible.
	//
	// void RemoteClassUpdated(String address, UInt32 class)
	//
	// This signal will be send every time the remote class
	// of device has been changed. This happens for example
	// after a remote connection attempt. This signal will
	// not be send if the class of device hasn't changed
	// compared to cached one.

	/**
	 * This signal will be send every time the service daemon detect a new name for a remote device.
	 */
	public class RemoteNameUpdated extends DBusSignal {

		private final String address;

		private final String name;

		public RemoteNameUpdated(String path, String address, String name) throws DBusException {
			super(path, address, name);
			this.address = address;
			this.name = name;
		}

		/**
		 * @return the address
		 */
		public String getDeviceAddress() {
			return address;
		}

		/**
		 * @return the name
		 */
		public String getDeviceName() {
			return name;
		}
	}

	//
	// void RemoteIdentifiersUpdated(String address, array{String identifiers})
	//
	// This signal is sent to indicate the provided services of a given
	// remote device. It will be sent after GetRemoteServiceIdentifiers
	// calls. This signal has at least one identifier and it does not
	// contain repeated entries.
	//
	// void RemoteNameFailed(String address)
	//
	// This signal will be sent every time the service daemon
	// tries to resolve a remote and this fails.
	//
	// void RemoteNameRequested(String address)
	//
	// This signal will be sent every time the service daemon
	// tries to resolve a remote name during discovery.
	//
	// void RemoteAliasChanged(String address, String alias)
	//
	// After changing an alias with SetRemoteAlias this
	// signal will indicate the new alias.
	//
	// void RemoteAliasCleared(String address)
	//
	// After removing an alias with ClearRemoteAlias this
	// signal will indicate that the alias is no longer
	// valid.
	//
	// void RemoteDeviceConnected(String address)
	//
	// This signal will be send if a low level connection
	// between two devices has been created.
	//
	// void RemoteDeviceDisconnectRequested(String address)
	//
	// This signal will be sent when a low level
	// disconnection to a remote device has been requested.
	// The actual disconnection will happen 2 seconds later.
	//
	// void RemoteDeviceDisconnected(String address)
	//
	// This signal will be send if a low level connection
	// between two devices has been terminated.
	//
	// void BondingCreated(String address)
	//
	// Signals that a successful bonding has been created.
	//
	// void BondingRemoved(String address)
	//
	// Signals that a bonding was removed.
	//
	// void TrustAdded(String address)
	//
	// Sent when SetTrusted() is called.
	//
	// void TrustRemoved(String address)
	//
	// Sent when RemoveTrust() is called.
	//

}
