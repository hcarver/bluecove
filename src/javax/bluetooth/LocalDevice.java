/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package javax.bluetooth;

import java.io.IOException;

import javax.microedition.io.Connection;

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BluetoothPeer;
import com.intel.bluetooth.BluetoothStreamServiceRecordAccess;
import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.NotImplementedError;

public class LocalDevice {
	
	private static LocalDevice localDevice;

	private DiscoveryAgent discoveryAgent;

	private String address;

	private long bluetoothAddress;

	private LocalDevice() {
		
		BluetoothPeer bluetoothPeer = BlueCoveImpl.instance().getBluetoothPeer();

		discoveryAgent = new DiscoveryAgent();

		try {
			int socket = bluetoothPeer.socket(false, false);
			DebugLog.debug("bluetoothPeer socket", socket);
			
			bluetoothPeer.bind(socket);
			
			bluetoothAddress = bluetoothPeer.getsockaddress(socket);

			address = Long.toHexString(bluetoothAddress);

			bluetoothPeer.close(socket);
		} catch (IOException e) {
			DebugLog.error("get local bluetoothAddress", e);
			address = "";
		}

		address = "000000000000".substring(address.length()) + address;
	}

	/*
	 * Retrieves the LocalDevice object for the local Bluetooth device. Multiple
	 * calls to this method will return the same object. This method will never
	 * return null. Returns: an object that represents the local Bluetooth
	 * device Throws: BluetoothStateException - if the Bluetooth system could
	 * not be initialized
	 */

	public static LocalDevice getLocalDevice() throws BluetoothStateException {
		if (localDevice == null) {
			localDevice = new LocalDevice();
		}
		return localDevice;
	}

	/*
	 * Returns the discovery agent for this device. Multiple calls to this
	 * method will return the same object. This method will never return null.
	 * Returns: the discovery agent for the local device
	 */

	public DiscoveryAgent getDiscoveryAgent() {
		return discoveryAgent;
	}

	/*
	 * Retrieves the name of the local device. The Bluetooth specification calls
	 * this name the "Bluetooth device name" or the "user-friendly name".
	 * Returns: the name of the local device; null if the name could not be
	 * retrieved
	 */

	public String getFriendlyName() {
		return BlueCoveImpl.instance().getBluetoothPeer().getradioname(bluetoothAddress);
	}

	/*
	 * Retrieves the DeviceClass object that represents the service classes,
	 * major device class, and minor device class of the local device. This
	 * method will return null if the service classes, major device class, or
	 * minor device class could not be determined. Returns: the service classes,
	 * major device class, and minor device class of the local device, or null
	 * if the service classes, major device class or minor device class could
	 * not be determined
	 */

	public DeviceClass getDeviceClass() {
		return new DeviceClass(BlueCoveImpl.instance().getBluetoothPeer().getDeviceClass());
	}

	/*
	 * Sets the discoverable mode of the device. The mode may be any number in
	 * the range 0x9E8B00 to 0x9E8B3F as defined by the Bluetooth Assigned
	 * Numbers Document. When this specification was defined, only GIAC
	 * (DiscoveryAgent.GIAC) and LIAC (DiscoveryAgent.LIAC) were defined, but
	 * Bluetooth profiles may add additional access codes in the future. To
	 * determine what values may be used, check the Bluetooth Assigned Numbers
	 * document at http://www.bluetooth.org/assigned-numbers/baseband.htm. If
	 * DiscoveryAgent.GIAC or DiscoveryAgent.LIAC are provided, then this method
	 * will attempt to put the device into general or limited discoverable mode,
	 * respectively. To take a device out of discoverable mode, provide the
	 * DiscoveryAgent.NOT_DISCOVERABLE flag. The BCC decides if the request will
	 * be granted. In addition to the BCC, the Bluetooth system could effect the
	 * discoverability of a device. According to the Bluetooth Specification, a
	 * device should only be limited discoverable (DiscoveryAgent.LIAC) for 1
	 * minute. This is handled by the implementation of the API. After the
	 * minute is up, the device will revert back to the previous discoverable
	 * mode.
	 * 
	 * Parameters: mode - the mode the device should be in; valid modes are
	 * DiscoveryAgent.GIAC, DiscoveryAgent.LIAC, DiscoveryAgent.NOT_DISCOVERABLE
	 * and any value in the range 0x9E8B00 to 0x9E8B3F Returns: true if the
	 * request succeeded, otherwise false if the request failed because the BCC
	 * denied the request; false if the Bluetooth system does not support the
	 * access mode specified in mode Throws: IllegalArgumentException - if the
	 * mode is not DiscoveryAgent.GIAC, DiscoveryAgent.LIAC,
	 * DiscoveryAgent.NOT_DISCOVERABLE, or in the range 0x9E8B00 to 0x9E8B3F
	 * BluetoothStateException - if the Bluetooth system is in a state that does
	 * not allow the discoverable mode to be changed See Also:
	 * DiscoveryAgent.GIAC, DiscoveryAgent.LIAC, DiscoveryAgent.NOT_DISCOVERABLE
	 */

	public boolean setDiscoverable(int mode) throws BluetoothStateException {
		switch (mode) {
		case DiscoveryAgent.NOT_DISCOVERABLE:
			BlueCoveImpl.instance().getBluetoothPeer().setDiscoverable(false);
			break;
		case DiscoveryAgent.GIAC:
			BlueCoveImpl.instance().getBluetoothPeer().setDiscoverable(true);
			break;
		case DiscoveryAgent.LIAC:
			BlueCoveImpl.instance().getBluetoothPeer().setDiscoverable(true);
			// TODO Timer to turn it off
			break;
		}
		return true;
	}

	public static boolean isPowerOn() {
		int mode = BlueCoveImpl.instance().getBluetoothPeer().getBluetoothRadioMode();
		return ((mode == BluetoothPeer.BTH_MODE_CONNECTABLE) || (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE));
	}
	
	/*
	 * Retrieves the local device's discoverable mode. The return value will be
	 * DiscoveryAgent.GIAC, DiscoveryAgent.LIAC,
	 * DiscoveryAgent.NOT_DISCOVERABLE, or a value in the range 0x9E8B00 to
	 * 0x9E8B3F. Returns: the discoverable mode the device is presently in See
	 * Also: DiscoveryAgent.GIAC, DiscoveryAgent.LIAC,
	 * DiscoveryAgent.NOT_DISCOVERABLE
	 */

	public int getDiscoverable() {
		int mode = BlueCoveImpl.instance().getBluetoothPeer().getBluetoothRadioMode();
		if (mode == BluetoothPeer.BTH_MODE_DISCOVERABLE) {
			return DiscoveryAgent.GIAC;
		} else {
			return DiscoveryAgent.NOT_DISCOVERABLE;
		}
	}
	
	/*
	 * Retrieves Bluetooth system properties. The following properties must be
	 * supported, but additional values are allowed: Property Name Description
	 * bluetooth.api.version The version of the Java API for Bluetooth wireless
	 * technology that is supported. For this version it will be set to "1.0".
	 * bluetooth.master.switch Is master/slave switch allowed? Valid values are
	 * either "true" or "false". bluetooth.sd.attr.retrievable.max Maximum
	 * number of service attributes to be retrieved per service record. The
	 * string will be in Base 10 digits. bluetooth.connected.devices.max The
	 * maximum number of connected devices supported. This number may be greater
	 * than 7 if the implementation handles parked connections. The string will
	 * be in Base 10 digits. bluetooth.l2cap.receiveMTU.max The maximum
	 * ReceiveMTU size in bytes supported in L2CAP. The string will be in Base
	 * 10 digits, e.g. "32". bluetooth.sd.trans.max Maximum number of concurrent
	 * service discovery transactions. The string will be in Base 10 digits.
	 * bluetooth.connected.inquiry.scan Is Inquiry scanning allowed during
	 * connection? Valid values are either "true" or "false".
	 * bluetooth.connected.page.scan Is Page scanning allowed during connection?
	 * Valid values are either "true" or "false". bluetooth.connected.inquiry Is
	 * Inquiry allowed during a connection? Valid values are either "true" or
	 * "false". bluetooth.connected.page Is paging allowed during a connection?
	 * In other words, can a connection be established to one device if it is
	 * already connected to another device. Valid values are either "true" or
	 * "false".
	 * 
	 * Parameters: property - the property to retrieve as defined in this class.
	 * Returns: the value of the property specified; null if the property is not
	 * defined
	 */

	public static String getProperty(String property) {
		if ("bluetooth.api.version".equals(property)) {
			return "1.0";
		} else if ("bluecove".equals(property)) {
			return BlueCoveImpl.version;
		}
		return null;
	}

	
	/*
	 * Retrieves the Bluetooth address of the local device. The Bluetooth
	 * address will never be null. The Bluetooth address will be 12 characters
	 * long. Valid characters are 0-9 and A-F. Returns: the Bluetooth address of
	 * the local device
	 */

	public String getBluetoothAddress() {
		return address;
	}

	/*
	 * Gets the service record corresponding to a btspp, btl2cap, or btgoep
	 * notifier. In the case of a run-before-connect service, the service record
	 * returned by getRecord() was created by the same call to Connector.open()
	 * that created the notifier. If a connect-anytime server application does
	 * not already have a service record in the SDDB, either because a service
	 * record for this service was never added to the SDDB or because the
	 * service record was added and then removed, then the ServiceRecord
	 * returned by getRecord() was created by the same call to Connector.open()
	 * that created the notifier.
	 * 
	 * In the case of a connect-anytime service, there may be a service record
	 * in the SDDB corresponding to this service prior to application startup.
	 * In this case, the getRecord() method must return a ServiceRecord whose
	 * contents match those of the corresponding service record in the SDDB. If
	 * a connect-anytime server application made changes previously to its
	 * service record in the SDDB (for example, during a previous execution of
	 * the server), and that service record is still in the SDDB, then those
	 * changes must be reflected in the ServiceRecord returned by getRecord().
	 * 
	 * Two invocations of this method with the same notifier argument return
	 * objects that describe the same service attributes, but the return values
	 * may be different object references.
	 * 
	 * Parameters: notifier - a connection that waits for clients to connect to
	 * a Bluetooth service Returns: the ServiceRecord associated with notifier
	 * Throws: IllegalArgumentException - if notifier is closed, or if notifier
	 * does not implement one of the following interfaces:
	 * javax.microedition.io.StreamConnectionNotifier,
	 * javax.bluetooth.L2CapConnectionNotifier, or javax.obex.SessionNotifier.
	 * This exception is also thrown if notifier is not a Bluetooth notifier,
	 * e.g., a StreamConnectionNotifier created with a scheme other than btspp.
	 * NullPointerException - if notifier is null
	 */

	public ServiceRecord getRecord(Connection notifier) {
		if (notifier == null)
			throw new NullPointerException();

		if (!(notifier instanceof BluetoothStreamServiceRecordAccess)) {
			throw new IllegalArgumentException();
		}

		return ((BluetoothStreamServiceRecordAccess) notifier).getServiceRecord();
	}

	/*
	 * Updates the service record in the local SDDB that corresponds to the
	 * ServiceRecord parameter. Updating is possible only if srvRecord was
	 * obtained using the getRecord() method. The service record in the SDDB is
	 * modified to have the same service attributes with the same contents as
	 * srvRecord. If srvRecord was obtained from the SDDB of a remote device
	 * using the service search methods, updating is not possible and this
	 * method will throw an IllegalArgumentException.
	 * 
	 * If the srvRecord parameter is a btspp service record, then before the
	 * SDDB is changed the following checks are performed. If any of these
	 * checks fail, then an IllegalArgumentException is thrown.
	 * 
	 * ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a btspp service record, must be present in srvRecord.
	 * L2CAP and RFCOMM must be in the ProtocolDescriptorList. srvRecord must
	 * not have changed the RFCOMM server channel number from the channel number
	 * that is currently in the SDDB version of this service record. If the
	 * srvRecord parameter is a btl2cap service record, then before the SDDB is
	 * changed the following checks are performed. If any of these checks fail,
	 * then an IllegalArgumentException is thrown.
	 * 
	 * ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a btl2cap service record, must be present in srvRecord.
	 * L2CAP must be in the ProtocolDescriptorList. srvRecord must not have
	 * changed the PSM value from the PSM value that is currently in the SDDB
	 * version of this service record. If the srvRecord parameter is a btgoep
	 * service record, then before the SDDB is changed the following checks are
	 * performed. If any of these checks fail, then an IllegalArgumentException
	 * is thrown.
	 * 
	 * ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a btgoep service record, must be present in srvRecord.
	 * L2CAP, RFCOMM and OBEX must all be in the ProtocolDescriptorList.
	 * srvRecord must not have changed the RFCOMM server channel number from the
	 * channel number that is currently in the SDDB version of this service
	 * record. updateRecord() is not required to ensure that srvRecord is a
	 * completely valid service record. It is the responsibility of the
	 * application to ensure that srvRecord follows all of the applicable
	 * syntactic and semantic rules for service record correctness.
	 * 
	 * If there is currently no SDDB version of the srvRecord service record,
	 * then this method will do nothing.
	 * 
	 * Parameters: srvRecord - the new contents to use for the service record in
	 * the SDDB Throws: NullPointerException - if srvRecord is null
	 * IllegalArgumentException - if the structure of the srvRecord is missing
	 * any mandatory service attributes, or if an attempt has been made to
	 * change any of the values described as fixed. ServiceRegistrationException -
	 * if the local SDDB could not be updated successfully due to insufficient
	 * disk space, database locks, etc.
	 */
	public void updateRecord(ServiceRecord srvRecord) throws ServiceRegistrationException { 
		if (NotImplementedError.enabled) {
			throw new NotImplementedError();
		}
	}
	
}