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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This interface defines the capabilities that an output stream connection must have.
 */
public interface OutputConnection extends Connection {

	/**
	 * Open and return an output stream for a connection. 
	 * 
	 * @return An output stream
	 * 
	 * @exception IOException If an I/O error occur
	 */
	public OutputStream openOutputStream() throws IOException;

	/**
	 * Open and return a data output stream for a connection. 
	 * 
	 * @return An output stream
	 * 
	 * @exception IOException If an I/O error occur
	 */
	public DataOutputStream openDataOutputStream() throws IOException;
}