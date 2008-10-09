/**
 *  BlueCove - Java library for Bluetooth
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

import java.io.IOException;

public interface SocketConnection extends StreamConnection {

	public static final byte DELAY = 0;

	public static final byte LINGER = 1;

	public static final byte KEEPALIVE = 2;

	public static final byte RCVBUF = 3;

	public static final byte SNDBUF = 4;

	public void setSocketOption(byte option, int value) throws IllegalArgumentException, IOException;

	public int getSocketOption(byte option) throws IllegalArgumentException, IOException;

	public String getLocalAddress() throws IOException;

	public int getLocalPort() throws IOException;

	public String getAddress() throws IOException;

	public int getPort() throws IOException;
}
