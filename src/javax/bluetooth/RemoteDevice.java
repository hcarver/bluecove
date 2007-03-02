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

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BluetoothConnection;
import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.NotImplementedError;

public class RemoteDevice {
	private String name;

	long address;

	RemoteDevice(String name, long address) {
		DebugLog.debug("new RemoteDevice", name);
		this.name = name;
		this.address = address;
	}

	/*
	 * Creates a Bluetooth device based upon its address. The Bluetooth address
	 * must be 12 hex characters long. Valid characters are 0-9, a-f, and A-F.
	 * There is no preceding "0x" in the string. For example, valid Bluetooth
	 * addresses include but are not limited to: 008037144297 00af8300cd0b
	 * 014bd91DA8FC Parameters: address - the address of the Bluetooth device as
	 * a 12 character hex string Throws: NullPointerException - if address is
	 * null IllegalArgumentException - if address is the address of the local
	 * device or is not a valid Bluetooth address
	 */

	protected RemoteDevice(String address) {
		DebugLog.debug("new RemoteDevice", address);
		this.address = Long.parseLong(address, 16);
	}

	/*
	 * Determines if this is a trusted device according to the BCC. Returns:
	 * true if the device is a trusted device, otherwise false
	 */

	public boolean isTrustedDevice() {
		// TODO not yet implemented
		return false;
	}

	/*
	 * Returns the name of this device. The Bluetooth specification calls this
	 * name the "Bluetooth device name" or the "user-friendly name". This method
	 * will only contact the remote device if the name is not known or alwaysAsk
	 * is true. Parameters: alwaysAsk - if true then the device will be
	 * contacted for its name, otherwise, if there exists a known name for this
	 * device, the name will be returned without contacting the remote device
	 * Returns: the name of the device, or null if the Bluetooth system does not
	 * support this feature; if the local device is able to contact the remote
	 * device, the result will never be null; if the remote device does not have
	 * a name then an empty string will be returned Throws: java.io.IOException -
	 * if the remote device can not be contacted or the remote device could not
	 * provide its name
	 */

	public String getFriendlyName(boolean alwaysAsk) throws IOException {
		if (alwaysAsk || name == null || name.equals("")) {
			name = BlueCoveImpl.instance().getBluetoothPeer().getpeername(address);
		}
		return name;
	}

	/*
	 * Retrieves the Bluetooth address of this device. The Bluetooth address
	 * will be 12 characters long. Valid characters are 0-9 and A-F. This method
	 * will never return null. Returns: the Bluetooth address of the remote
	 * device
	 */

	public final String getBluetoothAddress() {
		String s = Long.toHexString(address);

		return "000000000000".substring(s.length()) + s;
	}

	/*
	 * Determines if two RemoteDevices are equal. Two devices are equal if they
	 * have the same Bluetooth device address. Overrides: equals in class
	 * java.lang.Object Parameters: obj - the object to compare to Returns: true
	 * if both devices have the same Bluetooth address; false if both devices do
	 * not have the same address; false if obj is null; false if obj is not a
	 * RemoteDevice
	 */

	public boolean equals(Object obj) {
		return obj != null && obj instanceof RemoteDevice
				&& ((RemoteDevice) obj).address == address;
	}

	/*
	 * Computes the hash code for this object. This method will return the same
	 * value when it is called multiple times on the same object. Overrides:
	 * hashCode in class java.lang.Object Returns: the hash code for this object
	 */

	public int hashCode() {
		return (int) address;
	}

	/*
	 * Retrieves the Bluetooth device that is at the other end of the Bluetooth
	 * Serial Port Profile connection, L2CAP connection, or OBEX over RFCOMM
	 * connection provided. This method will never return null. Parameters: conn -
	 * the Bluetooth Serial Port connection, L2CAP connection, or OBEX over
	 * RFCOMM connection whose remote Bluetooth device is needed Returns: the
	 * remote device involved in the connection Throws: IllegalArgumentException -
	 * if conn is not a Bluetooth Serial Port Profile connection, L2CAP
	 * connection, or OBEX over RFCOMM connection; if conn is a
	 * L2CAPConnectionNotifier, StreamConnectionNotifier, or SessionNotifier
	 * java.io.IOException - if the connection is closed NullPointerException -
	 * if conn is null
	 */

	public static RemoteDevice getRemoteDevice(
			javax.microedition.io.Connection conn) throws IOException {
		// BluetoothPeer peer =
		// (LocalDevice.getLocalDevice()).getBluetoothPeer();

		if (!(conn instanceof BluetoothConnection))
			throw new IllegalArgumentException("Not a Bluetooth connection");
		return new RemoteDevice("", ((BluetoothConnection) conn)
				.getRemoteAddress());
	}

	/*
	 * Attempts to authenticate this RemoteDevice. Authentication is a means of
	 * verifying the identity of a remote device. Authentication involves a
	 * device-to-device challenge and response scheme that requires a 128-bit
	 * common secret link key derived from a PIN code shared by both devices. If
	 * either side's PIN code does not match, the authentication process fails
	 * and the method returns false. The method will also return false if
	 * authentication is incompatible with the current security settings of the
	 * local device established by the BCC, if the stack does not support
	 * authentication at all, or if the stack does not support authentication
	 * subsequent to connection establishment. If this RemoteDevice has
	 * previously been authenticated, then this method returns true without
	 * attempting to re-authenticate this RemoteDevice.
	 * 
	 * Returns: true if authentication is successful; otherwise false Throws:
	 * java.io.IOException - if there are no open connections between the local
	 * device and this RemoteDevice
	 */
	/*
	 * public boolean authenticate() throws IOException { }
	 */
	/*
	 * Determines if this RemoteDevice should be allowed to continue to access
	 * the local service provided by the Connection. In Bluetooth, authorization
	 * is defined as the process of deciding if device X is allowed to access
	 * service Y. The implementation of the authorize(Connection conn) method
	 * asks the Bluetooth Control Center (BCC) to decide if it is acceptable for
	 * RemoteDevice to continue to access a local service over the connection
	 * conn. In devices with a user interface, the BCC is expected to consult
	 * with the user to obtain approval. Some Bluetooth systems may allow the
	 * user to permanently authorize a remote device for all local services.
	 * When a device is authorized in this way, it is known as a "trusted
	 * device" -- see isTrustedDevice().
	 * 
	 * The authorize() method will also check that the identity of the
	 * RemoteDevice can be verified through authentication. If this RemoteDevice
	 * has been authorized for conn previously, then this method returns true
	 * without attempting to re-authorize this RemoteDevice.
	 * 
	 * Parameters: conn - the connection that this RemoteDevice is using to
	 * access a local service Returns: true if this RemoteDevice is successfully
	 * authenticated and authorized, otherwise false if authentication or
	 * authorization fails Throws: IllegalArgumentException - if conn is not a
	 * connection to this RemoteDevice, or if the local device initiated the
	 * connection, i.e., the local device is the client rather than the server.
	 * This exception is also thrown if conn was created by RemoteDevice using a
	 * scheme other than btspp, btl2cap, or btgoep. This exception is thrown if
	 * conn is a notifier used by a server to wait for a client connection,
	 * since the notifier is not a connection to this RemoteDevice.
	 * java.io.IOException - if conn is closed See Also: isTrustedDevice()
	 */
	/*
	 * public boolean authorize(javax.microedition.io.Connection conn) throws
	 * IOException { }
	 */
	/*
	 * Attempts to turn encryption on or off for an existing connection. In the
	 * case where the parameter on is true, this method will first authenticate
	 * this RemoteDevice if it has not already been authenticated. Then it will
	 * attempt to turn on encryption. If the connection is already encrypted
	 * then this method returns true. Otherwise, when the parameter on is true,
	 * either: the method succeeds in turning on encryption for the connection
	 * and returns true, or the method was unsuccessful in turning on encryption
	 * and returns false. This could happen because the stack does not support
	 * encryption or because encryption conflicts with the user's security
	 * settings for the device. In the case where the parameter on is false,
	 * there are again two possible outcomes:
	 * 
	 * encryption is turned off on the connection and true is returned, or
	 * encryption is left on for the connection and false is returned.
	 * Encryption may be left on following encrypt(conn, false) for a variety of
	 * reasons. The user's current security settings for the device may require
	 * encryption or the stack may not have a mechanism to turn off encryption.
	 * Also, the BCC may have determined that encryption will be kept on for the
	 * physical link to this RemoteDevice. The details of the BCC are
	 * implementation dependent, but encryption might be left on because other
	 * connections to the same device need encryption. (All of the connections
	 * over the same physical link must be encrypted if any of them are
	 * encrypted.) While attempting to turn encryption off may not succeed
	 * immediately because other connections need encryption on, there may be a
	 * delayed effect. At some point, all of the connections over this physical
	 * link needing encryption could be closed or also have had the method
	 * encrypt(conn, false) invoked for them. In this case, the BCC may turn off
	 * encryption for all connections over this physical link. (The policy used
	 * by the BCC is implementation dependent.) It is recommended that
	 * applications do encrypt(conn, false) once they no longer need encryption
	 * to allow the BCC to determine if it can reduce the overhead on
	 * connections to this RemoteDevice.
	 * 
	 * The fact that encrypt(conn, false) may not succeed in turning off
	 * encryption has very few consequences for applications. The stack handles
	 * encryption and decryption, so the application does not have to do
	 * anything different depending on whether the connection is still encrypted
	 * or not.
	 * 
	 * Parameters: conn - the connection whose need for encryption has changed
	 * on - true attempts to turn on encryption; false attempts to turn off
	 * encryption Returns: true if the change succeeded, otherwise false if it
	 * failed Throws: java.io.IOException - if conn is closed
	 * IllegalArgumentException - if conn is not a connection to this
	 * RemoteDevice; if conn was created by the client side of the connection
	 * using a scheme other than btspp, btl2cap, or btgoep (for example, this
	 * exception will be thrown if conn was created using the file or http
	 * schemes.); if conn is a notifier used by a server to wait for a client
	 * connection, since the notifier is not a connection to this RemoteDevice
	 */
	/*
	 * public boolean encrypt(javax.microedition.io.Connection conn, boolean on)
	 * throws IOException { }
	 */
	/*
	 * Determines if this RemoteDevice has been authenticated. A device may have
	 * been authenticated by this application or another application.
	 * Authentication applies to an ACL link between devices and not on a
	 * specific L2CAP, RFCOMM, or OBEX connection. Therefore, if authenticate()
	 * is performed when an L2CAP connection is made to device A, then
	 * isAuthenticated() may return true when tested as part of making an RFCOMM
	 * connection to device A.
	 * 
	 * Returns: true if this RemoteDevice has previously been authenticated;
	 * false if it has not been authenticated or there are no open connections
	 * between the local device and this RemoteDevice
	 */

	public boolean isAuthenticated() {
		if (NotImplementedError.enabled) {
			throw new NotImplementedError();
		}
		return false;
	}

	/*
	 * Determines if this RemoteDevice has been authorized previously by the BCC
	 * of the local device to exchange data related to the service associated
	 * with the connection. Both clients and servers can call this method.
	 * However, for clients this method returns false for all legal values of
	 * the conn argument. Parameters: conn - a connection that this RemoteDevice
	 * is using to access a service or provide a service Returns: true if conn
	 * is a server-side connection and this RemoteDevice has been authorized;
	 * false if conn is a client-side connection, or a server-side connection
	 * that has not been authorized Throws: IllegalArgumentException - if conn
	 * is not a connection to this RemoteDevice; if conn was not created using
	 * one of the schemes btspp, btl2cap, or btgoep; or if conn is a notifier
	 * used by a server to wait for a client connection, since the notifier is
	 * not a connection to this RemoteDevice. java.io.IOException - if conn is
	 * closed
	 */

	public boolean isAuthorized(javax.microedition.io.Connection conn)
			throws IOException {
		// TODO not yet implemented
		return false;
	}

	/*
	 * Determines if data exchanges with this RemoteDevice are currently being
	 * encrypted. Encryption may have been previously turned on by this or
	 * another application. Encryption applies to an ACL link between devices
	 * and not on a specific L2CAP, RFCOMM, or OBEX connection. Therefore, if
	 * encrypt() is performed with the on parameter set to true when an L2CAP
	 * connection is made to device A, then isEncrypted() may return true when
	 * tested as part of making an RFCOMM connection to device A.
	 * 
	 * Returns: true if data exchanges with this RemoteDevice are being
	 * encrypted; false if they are not being encrypted, or there are no open
	 * connections between the local device and this RemoteDevice
	 */

	public boolean isEncrypted() {
		// TODO not yet implemented
		return false;
	}

}
