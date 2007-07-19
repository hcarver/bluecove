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

import javax.bluetooth.L2CAPConnection;
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

	private static Hashtable/*<String, String>*/ suportScheme = new Hashtable();
	
	private static Hashtable/*<String, String>*/ srvParams = new Hashtable();
	
	private static Hashtable/*<String, String>*/ cliParams = new Hashtable();
	
	private static Hashtable/*<String, String>*/ cliParamsL2CAP = new Hashtable();
	
	private static Hashtable/*<String, String>*/ srvParamsL2CAP = new Hashtable();
	
	private static final String  AUTHENTICATE = "authenticate";
	private static final String  AUTHORIZE = "authorize"; 
	private static final String  ENCRYPT = "encrypt";
	private static final String  MASTER = "master";
	private static final String  NAME = "name";
	private static final String  RECEIVE_MTU = "ReceiveMTU"; 
	private static final String  TRANSMIT_MTU = "TransmitMTU";
	
	static {
	    //cliParams    ::== master | encrypt | authenticate
		cliParams.put(AUTHENTICATE, AUTHENTICATE);
		cliParams.put(ENCRYPT, ENCRYPT);
		cliParams.put(MASTER, MASTER);

		//srvParams    ::== name | master | encrypt | authorize | authenticate
		copyAll(srvParams, cliParams);
		srvParams.put(AUTHORIZE, AUTHORIZE);
		srvParams.put(NAME, NAME);
		
		copyAll(cliParamsL2CAP, cliParams);
		cliParamsL2CAP.put(RECEIVE_MTU, RECEIVE_MTU);
		cliParamsL2CAP.put(TRANSMIT_MTU, TRANSMIT_MTU);
		// Some docs describe lower case names. e.g http://developers.sun.com/mobility/midp/ttips/gcfcs/index.html
		cliParamsL2CAP.put("receiveMTU", RECEIVE_MTU);
		cliParamsL2CAP.put("transmitMTU", TRANSMIT_MTU);
		
		copyAll(srvParamsL2CAP, cliParamsL2CAP);
		srvParamsL2CAP.put(AUTHORIZE, AUTHORIZE);
		srvParamsL2CAP.put(NAME, NAME);
		
		// "socket://" host ":" port
		// no validation for socket, since this is internal connector 
		
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_TCP_OBEX, Boolean.TRUE);
		suportScheme.put(BluetoothConsts.PROTOCOL_SCHEME_L2CAP, Boolean.TRUE);
		suportScheme.put("socket", Boolean.TRUE);
	}

	static void copyAll(Hashtable dest, Hashtable src) {
		for(Enumeration en = src.keys(); en.hasMoreElements(); ) {
			Object key = en.nextElement();
			dest.put(key, src.get(key));
		}
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
		boolean schemeBluetooth = (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM)) || (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX) || (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_L2CAP)));
		boolean isL2CAP = scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_L2CAP);		
		
		boolean isServer;
		
		int hostEnd = name.indexOf(':', scheme.length() + 3);
		
		if (hostEnd > -1) {
			host = name.substring(scheme.length() + 3, hostEnd);
			isServer = host.equals("localhost");
			
			Hashtable params;
			if (isL2CAP) {
				if (isServer) {
					params = srvParamsL2CAP;
				} else {
					params = cliParamsL2CAP;
				}
			} else {
				if (isServer) {
					params = srvParams;
				} else {
					params = cliParams;
				}
			}
			
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
					if (params.containsKey(param)) {
						values.put(params.get(param), value);
					} else {
						throw new IllegalArgumentException("invalid param [" + param + "] value [" + value +"]");
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
		
		BluetoothConnectionNotifierParams notifierParams = null;

		BluetoothConnectionParams connectionParams = null;
		
		int channel = 0;
		if (isServer) {
           if (!allowServer) {
        	   throw new IllegalArgumentException("Can't use server connection URL");
           }
           if (values.get(NAME) == null) {
        	   values.put(NAME, "BlueCove");
           } else if (schemeBluetooth) {
        	   validateBluetoothServiceName((String)values.get(NAME));
           }
           if (schemeBluetooth) {
        	   notifierParams = new BluetoothConnectionNotifierParams(new UUID(portORuuid, false), 
        			   paramBoolean(values, AUTHENTICATE), paramBoolean(values, ENCRYPT), 
        			   (String) values.get(NAME), paramBoolean(values, MASTER));
           }
		} else { // (!isServer)
			try {
				channel = Integer.parseInt(portORuuid, isL2CAP?16:10);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("channel " + portORuuid);
			}
			if (channel < 0) {
				throw new IllegalArgumentException("channel " + portORuuid);
			}
			if (schemeBluetooth) {
				if (isL2CAP) {
					// Valid PSM range: 0x0001-0x0019 (0x1001-0xFFFF dynamically assigned, 0x0019-0x0100 reserved for future use).
					if ((channel <= 3) || (channel > 0xFFFF)) {
						// PSM 1 discovery, 3 RFCOMM
						throw new IllegalArgumentException("PCM " + portORuuid);
					}
					//has the 9th bit (0x100) set to zero
					if ((channel & 0x100) != 0) {
						throw new IllegalArgumentException("9th bit set in PCM " + portORuuid);
					}
					if ((channel % 2) != 1) {
						throw new IllegalArgumentException("PSM value " + portORuuid + " should be odd");
					}
				} else {
					if (channel > 30) {
						throw new IllegalArgumentException("channel " + portORuuid);
					}
				}
				
				connectionParams = new BluetoothConnectionParams(RemoteDeviceHelper.getAddress(host), channel, 
					paramBoolean(values, AUTHENTICATE), paramBoolean(values, ENCRYPT));
			}
		}
		/*
		 * create connection
		 */
		if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM)) {
			if (isServer) {
				return new BluetoothStreamConnectionNotifier(notifierParams);
			} else {
				return new BluetoothRFCommClientConnection(connectionParams);
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_BT_OBEX)) {
			if (isServer) {
				return new OBEXSessionNotifierImpl(notifierParams);
			} else {
				return new OBEXClientSessionImpl(new BluetoothRFCommClientConnection(connectionParams)); 
			}
		} else if (scheme.equals(BluetoothConsts.PROTOCOL_SCHEME_L2CAP)) {
			if (isServer) {
				return new BluetoothL2CAPConnectionNotifier(notifierParams,
						paramL2CAPMTU(values, RECEIVE_MTU), paramL2CAPMTU(values, TRANSMIT_MTU));
			} else {
				return new BluetoothL2CAPClientConnection(connectionParams,
						paramL2CAPMTU(values, RECEIVE_MTU), paramL2CAPMTU(values, TRANSMIT_MTU)); 
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
	
	private static void validateBluetoothServiceName(String serviceName) {
		 if(serviceName.length() == 0) {
             throw new IllegalArgumentException("zero length service name");
		 }
		 final String allowNameCharactes = " -_";
         for(int i = 0; i < serviceName.length(); i++) {
             char c = serviceName.charAt(i);
             if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || allowNameCharactes.indexOf(c) != -1) {
            	 continue;
             }
             throw new IllegalArgumentException("Illegal character '" + c + "' in service name");
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
	
	private static int paramL2CAPMTU(Hashtable values, String name) {
		String v = (String) values.get(name);
		if (v == null) {
			return L2CAPConnection.DEFAULT_MTU;
		}
		try {
			int mtu = Integer.parseInt(v);
			if (mtu >= L2CAPConnection.MINIMUM_MTU) {
				return mtu;
			}
			if ((mtu >0) && (mtu < L2CAPConnection.MINIMUM_MTU) && (name.equals(TRANSMIT_MTU))) {
				return L2CAPConnection.MINIMUM_MTU;
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid MTU value " + v);
		}
		throw new IllegalArgumentException("invalid MTU param value " + name + "=" + v);
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