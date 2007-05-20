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
package com.sun.cdc.io.j2me.btspp;

import java.io.IOException;

import javax.microedition.io.Connection;

import com.intel.bluetooth.MicroeditionConnector;
import com.sun.cdc.io.ConnectionBaseInterface;

/**
 * This class is Proxy for btspp Connection implementations used in WTK and MicroEmulator
 * 
 * @author vlads
 */
public class Protocol implements ConnectionBaseInterface {

	public Connection openPrim(String name, int mode, boolean timeouts) throws IOException {
		return MicroeditionConnector.open("btspp:" + name, mode, timeouts);
	}

}
