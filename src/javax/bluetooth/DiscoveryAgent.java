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

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.NotImplementedError;

public class DiscoveryAgent {
	/*
	 * Takes the device out of discoverable mode. The value of NOT_DISCOVERABLE
	 * is 0x00 (0).
	 */

	public static final int NOT_DISCOVERABLE = 0;

	/*
	 * The inquiry access code for General/Unlimited Inquiry Access Code (GIAC).
	 * This is used to specify the type of inquiry to complete or respond to.
	 * The value of GIAC is 0x9E8B33 (10390323). This value is defined in the
	 * Bluetooth Assigned Numbers document.
	 */

	public static final int GIAC = 0x9E8B33;

	/*
	 * The inquiry access code for Limited Dedicated Inquiry Access Code (LIAC).
	 * This is used to specify the type of inquiry to complete or respond to.
	 * The value of LIAC is 0x9E8B00 (10390272). This value is defined in the
	 * Bluetooth Assigned Numbers document.
	 */

	public static final int LIAC = 0x9E8B00;

	/*
	 * Used with the retrieveDevices() method to return those devices that were
	 * found via a previous inquiry. If no inquiries have been started, this
	 * will cause the method to return null. The value of CACHED is 0x00 (0).
	 * 
	 * See Also: retrieveDevices(int)
	 */

	public static final int CACHED = 0;

	/*
	 * Used with the retrieveDevices() method to return those devices that are
	 * defined to be pre-known devices. Pre-known devices are specified in the
	 * BCC. These are devices that are specified by the user as devices with
	 * which the local device will frequently communicate. The value of PREKNOWN
	 * is 0x01 (1).
	 * 
	 * See Also: retrieveDevices(int)
	 */

	public static final int PREKNOWN = 1;

	DiscoveryAgent() {
	}

	/*
	 * Returns an array of Bluetooth devices that have either been found by the
	 * local device during previous inquiry requests or been specified as a
	 * pre-known device depending on the argument. The list of previously found
	 * devices is maintained by the implementation of this API. (In other words,
	 * maintenance of the list of previously found devices is an implementation
	 * detail.) A device can be set as a pre-known device in the Bluetooth
	 * Control Center. Parameters: option - CACHED if previously found devices
	 * should be returned; PREKNOWN if pre-known devices should be returned
	 * Returns: an array containing the Bluetooth devices that were previously
	 * found if option is CACHED; an array of devices that are pre-known devices
	 * if option is PREKNOWN; null if no devices meet the criteria Throws:
	 * IllegalArgumentException - if option is not CACHED or PREKNOWN
	 */

	public RemoteDevice[] retrieveDevices(int option) {
		return null; // TODO keep a cache of previous devices
	}

	/*
	 * Places the device into inquiry mode. The length of the inquiry is
	 * implementation dependent. This method will search for devices with the
	 * specified inquiry access code. Devices that responded to the inquiry are
	 * returned to the application via the method deviceDiscovered() of the
	 * interface DiscoveryListener. The cancelInquiry() method is called to stop
	 * the inquiry. Parameters: accessCode - the type of inquiry to complete
	 * listener - the event listener that will receive device discovery events
	 * Returns: true if the inquiry was started; false if the inquiry was not
	 * started because the accessCode is not supported Throws:
	 * IllegalArgumentException - if the access code provided is not LIAC, GIAC,
	 * or in the range 0x9E8B00 to 0x9E8B3F NullPointerException - if listener
	 * is null BluetoothStateException - if the Bluetooth device does not allow
	 * an inquiry to be started due to other operations that are being performed
	 * by the device See Also: cancelInquiry(javax.bluetooth.DiscoveryListener),
	 * GIAC, LIAC
	 */

	public boolean startInquiry(int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		if (listener == null) {
			throw new NullPointerException("DiscoveryListener is null");
		}
		return BlueCoveImpl.instance().getBluetoothStack().startInquiry(accessCode, listener);
	}

	/*
	 * Removes the device from inquiry mode. An inquiryCompleted() event will
	 * occur with a type of INQUIRY_TERMINATED as a result of calling this
	 * method. After receiving this event, no further deviceDiscovered() events
	 * will occur as a result of this inquiry.
	 * 
	 * This method will only cancel the inquiry if the listener provided is the
	 * listener that started the inquiry.
	 * 
	 * Parameters: listener - the listener that is receiving inquiry events
	 * Returns: true if the inquiry was canceled; otherwise false if the inquiry
	 * was not canceled or if the inquiry was not started using listener Throws:
	 * NullPointerException - if listener is null
	 * 
	 * TODO: Deviation from spec. We always cancel, regardless of the value of
	 * listener
	 */

	public boolean cancelInquiry(DiscoveryListener listener) {
		if (listener == null) {
			throw new NullPointerException("DiscoveryListener is null");
		}
		return BlueCoveImpl.instance().getBluetoothPeer().cancelInquiry();
	}

	/*
	 * Searches for services on a remote Bluetooth device that have all the
	 * UUIDs specified in uuidSet. Once the service is found, the attributes
	 * specified in attrSet and the default attributes are retrieved. The
	 * default attributes are ServiceRecordHandle (0x0000), ServiceClassIDList
	 * (0x0001), ServiceRecordState (0x0002), ServiceID (0x0003), and
	 * ProtocolDescriptorList (0x0004).If attrSet is null then only the default
	 * attributes will be retrieved. attrSet does not have to be sorted in
	 * increasing order, but must only contain values in the range [0 -
	 * (216-1)]. Parameters: attrSet - indicates the attributes whose values
	 * will be retrieved on services which have the UUIDs specified in uuidSet
	 * uuidSet - the set of UUIDs that are being searched for; all services
	 * returned will contain all the UUIDs specified here btDev - the remote
	 * Bluetooth device to search for services on discListener - the object that
	 * will receive events when services are discovered Returns: the transaction
	 * ID of the service search; this number must be positive Throws:
	 * BluetoothStateException - if the number of concurrent service search
	 * transactions exceeds the limit specified by the bluetooth.sd.trans.max
	 * property obtained from the class LocalDevice or the system is unable to
	 * start one due to current conditions IllegalArgumentException - if attrSet
	 * has an illegal service attribute ID or exceeds the property
	 * bluetooth.sd.attr.retrievable.max defined in the class LocalDevice; if
	 * attrSet or uuidSet is of length 0; if attrSet or uuidSet contains
	 * duplicates NullPointerException - if uuidSet, btDev, or discListener is
	 * null; if an element in uuidSet array is null See Also: DiscoveryListener
	 */

	public int searchServices(int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		BlueCoveImpl.instance().getBluetoothPeer().startSearchServices(attrSet, uuidSet, device, listener);
		return 0;
	}

	/*
	 * Cancels the service search transaction that has the specified transaction
	 * ID. The ID was assigned to the transaction by the method
	 * searchServices(). A serviceSearchCompleted() event with a discovery type
	 * of SERVICE_SEARCH_TERMINATED will occur when this method is called. After
	 * receiving this event, no further servicesDiscovered() events will occur
	 * as a result of this search. Parameters: transID - the ID of the service
	 * search transaction to cancel; returned by searchServices() Returns: true
	 * if the service search transaction is terminated, else false if the
	 * transID does not represent an active service search transaction
	 */

	public boolean cancelServiceSearch(int transID) {
		if (NotImplementedError.enabled) {
			throw new NotImplementedError();
		} else {
			return false;
		}
	}

	/*
	 * Attempts to locate a service that contains uuid in the ServiceClassIDList
	 * of its service record. This method will return a string that may be used
	 * in Connector.open() to establish a connection to the service. How the
	 * service is selected if there are multiple services with uuid and which
	 * devices to search is implementation dependent. Parameters: uuid - the
	 * UUID to search for in the ServiceClassIDList security - specifies the
	 * security requirements for a connection to this service; must be one of
	 * ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
	 * ServiceRecord.AUTHENTICATE_NOENCRYPT, or
	 * ServiceRecord.AUTHENTICATE_ENCRYPT master - determines if this client
	 * must be the master of the connection; true if the client must be the
	 * master; false if the client can be the master or the slave Returns: the
	 * connection string used to connect to the service with a UUID of uuid; or
	 * null if no service could be found with a UUID of uuid in the
	 * ServiceClassIDList Throws: BluetoothStateException - if the Bluetooth
	 * system cannot start the request due to the current state of the Bluetooth
	 * system NullPointerException - if uuid is null IllegalArgumentException -
	 * if security is not ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
	 * ServiceRecord.AUTHENTICATE_NOENCRYPT, or
	 * ServiceRecord.AUTHENTICATE_ENCRYPT See Also:
	 * ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
	 * ServiceRecord.AUTHENTICATE_NOENCRYPT, ServiceRecord.AUTHENTICATE_ENCRYPT
	 */
	
	public String selectService(UUID uuid, int security, boolean master) throws BluetoothStateException {
		if (NotImplementedError.enabled) {
			throw new NotImplementedError();
		} else {
			return null;
		}
	}

}