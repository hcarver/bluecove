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
package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.intel.bluetooth.DebugLog;
import com.intel.bluetooth.MicroeditionConnector;

/**
 * 
 * This class delegated all calls to MicroeditionConnector
 * 
 * 1) Solves Bytecode comatibilty problems. Application compiled with
 * bluecove.jar should run on java me platform.
 * 
 * 2) Use standard Protocol initialization to enable integration in other
 * environments.
 * 
 * vlads changes: Move Original code to MicroeditionConnector
 * 
 */

public class Connector {
	/*
	 * Access mode READ. The value 1 is assigned to READ.
	 */

	public static final int READ = 1;

	/*
	 * Access mode WRITE. The value 2 is assigned to WRITE.
	 */

	public static final int WRITE = 2;

	/*
	 * Access mode READ_WRITE. The value 3 is assigned to READ_WRITE.
	 */

	public static final int READ_WRITE = 3;

	private Connector() {

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
		DebugLog.debug("open using BlueCove javax.microedition.io.Connector");
		return MicroeditionConnector.open(name);
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
		return MicroeditionConnector.open(name, mode);
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
		return MicroeditionConnector.open(name, mode, timeouts);
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
		return MicroeditionConnector.openDataInputStream(name);
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
		return MicroeditionConnector.openDataOutputStream(name);
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
		return MicroeditionConnector.openInputStream(name);
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
		return MicroeditionConnector.openOutputStream(name);
	}
}