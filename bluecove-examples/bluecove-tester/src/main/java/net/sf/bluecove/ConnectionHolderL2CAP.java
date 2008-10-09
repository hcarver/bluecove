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
package net.sf.bluecove;

import javax.bluetooth.L2CAPConnection;

import net.sf.bluecove.util.IOUtils;

/**
 * @author vlads
 * 
 */
public class ConnectionHolderL2CAP extends ConnectionHolder {

	public L2CAPConnection channel;

	public ConnectionHolderL2CAP() {
		super();
	}

	public ConnectionHolderL2CAP(L2CAPConnection channel) {
		super();
		this.channel = channel;
	}

	public void shutdown() {
		setConnectionOpen(false);
		IOUtils.closeQuietly(channel);
	}

}
