/**
 *  BlueCove - Java library for Bluetooth
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
package BluetoothTCKAgent;

import java.io.IOException;

import javax.bluetooth.BluetoothConnectionException;
import javax.microedition.io.Connection;
import net.sf.bluecove.Logger;

/**
 * Small hack to enable connection retry while working on other implementations.
 * This will improve test stability and make them reproducible without tweaks in
 * timeouts.
 *
 * @author vlads
 *
 */
public class Connector {

	public static final int READ = javax.microedition.io.Connector.READ;

	public static final int WRITE = javax.microedition.io.Connector.WRITE;

	public static final int READ_WRITE = javax.microedition.io.Connector.READ_WRITE;

	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		return javax.microedition.io.Connector.open(name, mode, timeouts);
	}

	public static Connection open(String name, int mode) throws IOException {
		int retryMax = 3;
		int retry = 0;
		while (true) {
			try {
				return javax.microedition.io.Connector.open(name, mode);
			} catch (BluetoothConnectionException e) {
				if (retry >= retryMax) {
					Logger.error(name);
					throw e;
				}
				retry++;
				Logger.debug("retry " + retry, e);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ie) {
					throw new IOException(ie.getMessage());
				}
			}
		}
	}

	public static Connection open(String name) throws IOException {
		return open(name, Connector.READ_WRITE);
	}
}
