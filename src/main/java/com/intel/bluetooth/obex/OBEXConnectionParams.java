/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
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
package com.intel.bluetooth.obex;

/**
 * @author vlads
 * @since bluecove 2.0.2
 */
public class OBEXConnectionParams {

	public static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;

	/**
	 * Enables timeouts.
	 * 
	 * @see javax.microedition.io.Connector#open(String,int,boolean)
	 */
	public boolean timeouts;

	/**
	 * The amount of time in milliseconds for which the implementation will
	 * attempt to successfully transmit a packet before it throws
	 * InterruptedIOException.
	 * 
	 * Java System property "bluecove.obex.timeout" can be used to define the
	 * value.
	 */
	public int timeout = DEFAULT_TIMEOUT;

	/**
	 * You can increase network speed by changing mtu to bigger value.
	 * 
	 * Java System property "bluecove.obex.mtu" can be used to define the value.
	 */
	public int mtu = 10 * OBEXOperationCodes.OBEX_DEFAULT_MTU;
}
