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

public interface ServiceRecord {
	
	/*
	 * Authentication and encryption are not needed on a connection to this
	 * service. Used with getConnectionURL() method. NOAUTHENTICATE_NOENCRYPT is
	 * set to the constant value 0x00 (0).
	 * 
	 * See Also: getConnectionURL(int, boolean)
	 */

	public static final int NOAUTHENTICATE_NOENCRYPT = 0;

	/*
	 * Authentication is required for connections to this service, but not
	 * encryption. It is OK for encryption to be either on or off for the
	 * connection. Used with getConnectionURL() method. AUTHENTICATE_NOENCRYPT
	 * is set to the constant value 0x01 (1).
	 * 
	 * See Also: getConnectionURL(int, boolean)
	 */
	
	public static final int AUTHENTICATE_NOENCRYPT = 1;

	/*
	 * Authentication and encryption are required for connections to this
	 * service. Used with getConnectionURL() method. AUTHENTICATE_ENCRYPT is set
	 * to the constant value 0x02 (2).
	 */

	public static final int AUTHENTICATE_ENCRYPT = 2;

	/*
	 * Returns the value of the service attribute ID provided it is present in
	 * the service record, otherwise this method returns null. Parameters:
	 * attrID - the attribute whose value is to be returned Returns: the value
	 * of the attribute ID if present in the service record, otherwise null
	 * Throws: IllegalArgumentException - if attrID is negative or greater than
	 * or equal to 2^16
	 */

	public DataElement getAttributeValue(int attrID);

	/*
	 * Returns the remote Bluetooth device that populated the service record
	 * with attribute values. It is important to note that the Bluetooth device
	 * that provided the value might not be reachable anymore, since it can
	 * move, turn off, or change its security mode denying all further
	 * transactions. Returns: the remote Bluetooth device that populated the
	 * service record, or null if the local device populated this ServiceRecord
	 */

	public RemoteDevice getHostDevice();

	/*
	 * Returns the service attribute IDs whose value could be retrieved by a
	 * call to getAttributeValue(). The list of attributes being returned is not
	 * sorted and includes default attributes. Returns: an array of service
	 * attribute IDs that are in this object and have values for them; if there
	 * are no attribute IDs that have values, this method will return an array
	 * of length zero. See Also: getAttributeValue(int)
	 */

	public int[] getAttributeIDs();

	/*
	 * Retrieves the values by contacting the remote Bluetooth device for a set
	 * of service attribute IDs of a service that is available on a Bluetooth
	 * device. (This involves going over the air and contacting the remote
	 * device for the attribute values.) The system might impose a limit on the
	 * number of service attribute ID values one can request at a time.
	 * Applications can obtain the value of this limit as a String by calling
	 * LocalDevice.getProperty("bluetooth.sd.attr.retrievable.max"). The method
	 * is blocking and will return when the results of the request are
	 * available. Attribute IDs whose values could be obtained are added to this
	 * service record. If there exist attribute IDs for which values are
	 * retrieved this will cause the old values to be overwritten. If the remote
	 * device cannot be reached, an IOException will be thrown. Parameters:
	 * attrIDs - the list of service attributes IDs whose value are to be
	 * retrieved; the number of attributes cannot exceed the property
	 * bluetooth.sd.attr.retrievable.max; the attributes in the request must be
	 * legal, i.e. their values are in the range of [0, 2^16-1]. The input
	 * attribute IDs can include attribute IDs from the default attribute set
	 * too. Returns: true if the request was successful in retrieving values for
	 * some or all of the attribute IDs; false if it was unsuccessful in
	 * retrieving any values Throws: java.io.IOException - if the local device
	 * is unable to connect to the remote Bluetooth device that was the source
	 * of this ServiceRecord; if this ServiceRecord was deleted from the SDDB of
	 * the remote device IllegalArgumentException - if the size of attrIDs
	 * exceeds the system specified limit as defined by
	 * bluetooth.sd.attr.retrievable.max; if the attrIDs array length is zero;
	 * if any of their values are not in the range of [0, 2^16-1]; if attrIDs
	 * has duplicate values NullPointerException - if attrIDs is null
	 * RuntimeException - if this ServiceRecord describes a service on the local
	 * device rather than a service on a remote device
	 */

	public boolean populateRecord(int[] attrIDs) throws IOException;

	/*
	 * Returns a String including optional parameters that can be used by a
	 * client to connect to the service described by this ServiceRecord. The
	 * return value can be used as the first argument to Connector.open(). In
	 * the case of a Serial Port service record, this string might look like
	 * "btspp://0050CD00321B:3;authenticate=true;encrypt=false;master=true",
	 * where "0050CD00321B" is the Bluetooth address of the device that provided
	 * this ServiceRecord, "3" is the RFCOMM server channel mentioned in this
	 * ServiceRecord, and there are three optional parameters related to
	 * security and master/slave roles. If this method is called on a
	 * ServiceRecord returned from LocalDevice.getRecord(), it will return the
	 * connection string that a remote device will use to connect to this
	 * service.
	 * 
	 * Parameters: requiredSecurity - determines whether authentication or
	 * encryption are required for a connection mustBeMaster - true indicates
	 * that this device must play the role of master in connections to this
	 * service; false indicates that the local device is willing to be either
	 * the master or the slave Returns: a string that can be used to connect to
	 * the service or null if the ProtocolDescriptorList in this ServiceRecord
	 * is not formatted according to the Bluetooth specification Throws:
	 * IllegalArgumentException - if requiredSecurity is not one of the
	 * constants NOAUTHENTICATE_NOENCRYPT, AUTHENTICATE_NOENCRYPT, or
	 * AUTHENTICATE_ENCRYPT See Also: NOAUTHENTICATE_NOENCRYPT,
	 * AUTHENTICATE_NOENCRYPT, AUTHENTICATE_ENCRYPT
	 */

	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster);

	/*
	 * Used by a server application to indicate the major service class bits
	 * that should be activated in the server's DeviceClass when this
	 * ServiceRecord is added to the SDDB. When client devices do device
	 * discovery, the server's DeviceClass is provided as one of the arguments
	 * of the deviceDiscovered method of the DiscoveryListener interface. Client
	 * devices can consult the DeviceClass of the server device to get a general
	 * idea of the kind of device this is (e.g., phone, PDA, or PC) and the
	 * major service classes it offers (e.g., rendering, telephony, or
	 * information). A server application should use the setDeviceServiceClasses
	 * method to describe its service in terms of the major service classes.
	 * This allows clients to obtain a DeviceClass for the server that
	 * accurately describes all of the services being offered. When
	 * acceptAndOpen() is invoked for the first time on the notifier associated
	 * with this ServiceRecord, the classes argument from the
	 * setDeviceServiceClasses method is OR'ed with the current setting of the
	 * major service class bits of the local device. The OR operation
	 * potentially activates additional bits. These bits may be retrieved by
	 * calling getDeviceClass() on the LocalDevice object. Likewise, a call to
	 * LocalDevice.updateRecord() will cause the major service class bits to be
	 * OR'ed with the current settings and updated.
	 * 
	 * The documentation for DeviceClass gives examples of the integers that
	 * describe each of the major service classes and provides a URL for the
	 * complete list. These integers can be used individually or OR'ed together
	 * to describe the appropriate value for classes.
	 * 
	 * Later, when this ServiceRecord is removed from the SDDB, the
	 * implementation will automatically deactivate the device bits that were
	 * activated as a result of the call to setDeviceServiceClasses. The only
	 * exception to this occurs if there is another ServiceRecord that is in the
	 * SDDB and setDeviceServiceClasses has been sent to that other
	 * ServiceRecord to request that some of the same bits be activated.
	 * 
	 * Parameters: classes - an integer whose binary representation indicates
	 * the major service class bits that should be activated Throws:
	 * IllegalArgumentException - if classes is not an OR of one or more of the
	 * major service class integers in the Bluetooth Assigned Numbers document.
	 * While Limited Discoverable Mode is included in this list of major service
	 * classes, its bit is activated by placing the device in Limited
	 * Discoverable Mode (see the GAP specification), so if bit 13 is set this
	 * exception will be thrown. RuntimeExceptin - if the ServiceRecord
	 * receiving the message was obtained from a remote device
	 */
	/*
	 * public void setDeviceServiceClasses(int classes) { }
	 */
	/*
	 * Modifies this ServiceRecord to contain the service attribute defined by
	 * the attribute-value pair (attrID, attrValue). If the attrID does not
	 * exist in the ServiceRecord, this attribute-value pair is added to this
	 * ServiceRecord object. If the attrID is already in this ServiceRecord, the
	 * value of the attribute is changed to attrValue. If attrValue is null, the
	 * attribute with the attribute ID of attrID is removed from this
	 * ServiceRecord object. If attrValue is null and attrID does not exist in
	 * this object, this method will return false. This method makes no
	 * modifications to a service record in the SDDB. In order for any changes
	 * made by this method to be reflected in the SDDB, a call must be made to
	 * the acceptAndOpen() method of the associated notifier to add this
	 * ServiceRecord to the SDDB for the first time, or a call must be made to
	 * the updateRecord() method of LocalDevice to modify the version of this
	 * ServiceRecord that is already in the SDDB.
	 * 
	 * This method prevents the ServiceRecordHandle from being modified by
	 * throwing an IllegalArgumentException.
	 * 
	 * Parameters: attrID - the service attribute ID attrValue - the DataElement
	 * which is the value of the service attribute Returns: true if the service
	 * attribute was successfully added, removed, or modified; false if
	 * attrValue is null and attrID is not in this object Throws:
	 * IllegalArgumentException - if attrID does not represent a 16-bit unsigned
	 * integer; if attrID is the value of ServiceRecordHandle (0x0000)
	 * RuntimeException - if this method is called on a ServiceRecord that was
	 * created by a call to DiscoveryAgent.searchServices()
	 */

	public boolean setAttributeValue(int attrID, DataElement attrValue);

	/**
	 * @param service_telephony
	 */
	public void setDeviceServiceClasses(int service_telephony);
}