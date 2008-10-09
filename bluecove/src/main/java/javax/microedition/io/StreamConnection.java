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

/**
 * This interface defines the capabilities that a stream connection must have.
 * <p>
 * In a typical implementation of this interface (for instance in MIDP 2.0), all
 * StreamConnections have one underlying InputStream and one OutputStream.
 * Opening a DataInputStream counts as opening an InputStream and opening a
 * DataOutputStream counts as opening an OutputStream. Trying to open another
 * InputStream or OutputStream causes an IOException. Trying to open the
 * InputStream or OutputStream after they have been closed causes an
 * IOException.
 * <p>
 * The methods of StreamConnection are not synchronized. The only stream method that can be called safely in another thread is close. 
 */
public interface StreamConnection extends InputConnection, OutputConnection {
}