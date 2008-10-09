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

import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.StreamConnection;

import net.sf.bluecove.util.IOUtils;

/**
 * @author vlads
 * 
 */
public class ConnectionHolderStream extends ConnectionHolder {

	public StreamConnection conn = null;

	public InputStream is = null;

	public OutputStream os = null;

	public ConnectionHolderStream() {
		super();
	}

	public ConnectionHolderStream(StreamConnection conn) {
		super();
		this.conn = conn;
	}

	public void shutdown() {
		setConnectionOpen(false);
		IOUtils.closeQuietly(os);
		IOUtils.closeQuietly(is);
		IOUtils.closeQuietly(conn);
	}

}
