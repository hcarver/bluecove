/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

import com.intel.bluetooth.gcf.socket.ServerSocketConnection;
import com.intel.bluetooth.gcf.socket.SocketConnection;
import com.intel.bluetooth.obex.OBEXClientSessionImpl;
import com.intel.bluetooth.obex.OBEXSessionNotifierImpl;

/**
 * 
 * This is renamed class javax.microedition.io.Connector
 * 
 */
public class MicroeditionConnector {
	/*
	 * Access mode READ. The value 1 is assigned to READ.
	 */

	public static final int READ = Connector.READ;

	/*
	 * Access mode WRITE. The value 2 is assigned to WRITE.
	 */

	public static final int WRITE = Connector.WRITE;

	/*
	 * Access mode READ_WRITE. The value 3 is assigned to READ_WRITE.
	 */

	public static final int READ_WRITE = Connector.READ_WRITE;

	private static Hashtable/*<String, any>*/ suportScheme = new Hashtable();
	
	private static Hashtable/*<String, any>*/ params = new Hashtable();
	
	private static Hashtable/*<String, any>*/ cliParams = new Hashtable();
	
	private static final String  AUTHENTICATE = "authenticate";
	private static final String  AUTHORIZE = "authorize"; 
	private static final String  ENCRYPT = "encrypt";
	private static final String  MASTER = "master";
	private static final String  NAME = "name";
	
	static {
	    //srvParams    ::== name | master | encrypt | authorize | authenticate
		params.put(AUTHENTICATE, AUTHENTICATE);
		params.put(AUTHORIZE, AUTHORIZE);
		params.put(ENCRYPT, ENCRYPT);
		params.put(MASTER, MASTER);
		params.put(NAME, NAME);
	    //cliParams    ::== master | encrypt | authenticate
		cliParams.put(AUTHENTICATE, AUTHENTICATE);
		cliParams.put(ENCRYPT, ENCRYPT);
		cliParams.put(MASTER, MASTER);
		
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX, Boolean.TRUE);
		suportScheme.put("socket", Boolean.TRUE);
	}

	/*
	 * Create and open a Connection. Parameters: name - The URL for the
	 * connection. Returns: A new Connection object. Throws:
	 * IllegalArgumentException - If a parameter is invalid.
	 * ConnectionNotFoundException - If the requested connection cannot be made,
	 * or the protocol type does not exist. java.io.IOException - If some other
	 * kind of I/O error occurs. SecurityException - If a requested protocol
	 * handler is not permitted.
	 */

	public static Connection open(String name) throws IOException {
		return openImpl(name, true);
	}
	
	private static Connection openImpl(String name, boolean allowServer) throws IOException {
		/*
		 * parse URL
		 */

		String host = null;
		String portORuuid = null;

		Hashtable values = new Hashtable();
		
		// scheme  : // host : port [;param=val]
		int schemeEnd = name.indexOf("://");
		if (schemeEnd == -1) {
			throw new ConnectionNotFoundException(name);
		}
		String scheme = name.substring(0, schemeEnd);
		if (!suportScheme.containsKey(scheme)) {
			throw new ConnectionNotFoundException(scheme);
		}
		
		int hostEnd = name.indexOf(':', scheme.length() + 3);

		if (hostEnd > -1) {
			host = name.substring(scheme.length() + 3, hostEnd);

			String paramsStr = name.substring(hostEnd + 1);
			UtilsStringTokenizer tok = new UtilsStringTokenizer(paramsStr, ";");
			if (tok.hasMoreTokens()) {
				portORuuid = tok.nextToken();
			} else {
				portORuuid = paramsStr;
			}
			while (tok.hasMoreTokens()) {
				String t = tok.nextToken();
				int equals = t.indexOf('=');
				if (equals > -1) {
					String param = t.substring(0, equals);
					String value = t.substring(equals + 1);
					if (params.contains(param)) {
						values.put(param, value);
					} else {
						throw new IllegalArgumentException("invalid param [" + t + "]");
					}
				} else {
					throw new IllegalArgumentException("invalid param [" + t +"]");
				}
			}
		} else {
			throw new IllegalArgumentException(name.substring(scheme.length() + 3));
		}

		if (host == null || portORuuid == null) {
			throw new IllegalArgumentException();
		}

		boolean isServer = host.equals("localhost");
		int channel = 0;
		if (isServer) {
           if (!allowServer) {
        	   throw new IllegalArgumentException("Can't use server connection URL");
           }
           if (values.get(NAME) == null) {
        	   values.put(NAME, "BlueCove");
           }
		} else { // (!isServer)
			try {
				channel = Integer.parseInt(portORuuid);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("channel " + portORuuid);
			}
		
			for (Enumeration e = values.keys(); e.hasMoreElements();) {
				String paramName = (String)e.nextElement();
				if (!cliParams.containsKey(paramName)) {
					throw new IllegalArgumentException("invalid client connection param [" + paramName + "]");
				}
			}
		}
		/*
		 * create connection
		 */
		if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM)) {
			if (isServer) {
				return new BluetoothStreamConnectionNotifier(new UUID(portORuuid, false), paramBoolean(values,
						AUTHENTICATE), paramBoolean(values, ENCRYPT), (String) values.get(NAME));
			} else {
				return new BluetoothRFCommClientConnection(RemoteDeviceHelper.getAddress(host), channel, paramBoolean(
						values, AUTHENTICATE), paramBoolean(values, ENCRYPT));
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX)) {
			if (isServer) {
				return new OBEXSessionNotifierImpl(new UUID(portORuuid, false), paramBoolean(values,
						AUTHENTICATE), paramBoolean(values, ENCRYPT), (String) values.get(NAME));
			} else {
				return new OBEXClientSessionImpl(new BluetoothRFCommClientConnection(RemoteDeviceHelper.getAddress(host), channel, paramBoolean(
						values, AUTHENTICATE), paramBoolean(values, ENCRYPT))); 
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX)) {
			if (isServer) {
				throw new ConnectionNotFoundException(scheme);
			} else {
				return new OBEXClientSessionImpl(new SocketConnection(host, channel)); 
			}
		} else if (scheme.equals("socket")) {
			if (isServer) {
				try {
					channel = Integer.parseInt(portORuuid);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("port " + portORuuid);
				}
				return new ServerSocketConnection(channel);
			} else {
				return new SocketConnection(host, channel);
			}
		} else {
			throw new ConnectionNotFoundException(scheme);
		}
	}
	
	private static boolean paramBoolean(Hashtable values, String name) {
		String v = (String)values.get(name);
		if (v == null) {
			return false;
		} else if ("true".equals(v)) {
			return true;
		} else if ("false".equals(v)) {
			return false;
		} else {
			throw new IllegalArgumentException("invalid param value " + name + "=" + v);
		}
	}

	/*
	 * Create and open a Connection. Parameters: name - The URL for the
	 * connection. mode - The access mode. Returns: A new Connection object.
	 * Throws: IllegalArgumentException - If a parameter is invalid.
	 * ConnectionNotFoundException - If the requested connection cannot be made,
	 * or the protocol type does not exist. java.io.IOException - If some other
	 * kind of I/O error occurs. SecurityException - If a requested protocol
	 * handler is not permitted.
	 */

	public static Connection open(String name, int mode) throws IOException {
		return open(name);
	}

	/*
	 * Create and open a Connection. Parameters: name - The URL for the
	 * connection mode - The access mode timeouts - A flag to indicate that the
	 * caller wants timeout exceptions Returns: A new Connection object Throws:
	 * IllegalArgumentException - If a parameter is invalid.
	 * ConnectionNotFoundException - if the requested connection cannot be made,
	 * or the protocol type does not exist. java.io.IOException - If some other
	 * kind of I/O error occurs. SecurityException - If a requested protocol
	 * handler is not permitted.
	 */

	public static Connection open(String name, int mode, boolean timeouts)
			throws IOException {
		return open(name);
	}

	/*
	 * Create and open a connection input stream. Parameters: name - The URL for
	 * the connection. Returns: A DataInputStream. Throws:
	 * IllegalArgumentException - If a parameter is invalid.
	 * ConnectionNotFoundException - If the connection cannot be found.
	 * java.io.IOException - If some other kind of I/O error occurs.
	 * SecurityException - If access to the requested stream is not permitted.
	 */

	public static DataInputStream openDataInputStream(String name)
			throws IOException {
		return new DataInputStream(openInputStream(name));
	}

	/*
	 * Create and open a connection output stream. Parameters: name - The URL
	 * for the connection. Returns: A DataOutputStream. Throws:
	 * IllegalArgumentException - If a parameter is invalid.
	 * ConnectionNotFoundException - If the connection cannot be found.
	 * java.io.IOException - If some other kind of I/O error occurs.
	 * SecurityException - If access to the requested stream is not permitted.
	 */

	public static DataOutputStream openDataOutputStream(String name)
			throws IOException {
		return new DataOutputStream(openOutputStream(name));
	}

	/*
	 * Create and open a connection input stream. Parameters: name - The URL for
	 * the connection. Returns: An InputStream. Throws: IllegalArgumentException -
	 * If a parameter is invalid. ConnectionNotFoundException - If the
	 * connection cannot be found. java.io.IOException - If some other kind of
	 * I/O error occurs. SecurityException - If access to the requested stream
	 * is not permitted.
	 */

	public static InputStream openInputStream(String name) throws IOException {
		return ((InputConnection)openImpl(name, false)).openInputStream();
	}

	/*
	 * Create and open a connection output stream. Parameters: name - The URL
	 * for the connection. Returns: An OutputStream. Throws:
	 * IllegalArgumentException - If a parameter is invalid.
	 * ConnectionNotFoundException - If the connection cannot be found.
	 * java.io.IOException - If some other kind of I/O error occurs.
	 * SecurityException - If access to the requested stream is not permitted.
	 */

	public static OutputStream openOutputStream(String name) throws IOException {
		return ((OutputConnection)openImpl(name, false)).openOutputStream();
	}

}