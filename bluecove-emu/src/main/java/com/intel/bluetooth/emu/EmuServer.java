/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import com.intel.bluetooth.rmi.Server;

public class EmuServer /* extends HTTPServer */{

	// private final static Logger logger = Logger.getLogger(EmuServer.class);
	//
	// public EmuServer() throws Exception {
	// super();
	// }
	//

	public static void main(String[] args) throws Exception {
		// try {
		// new EmuServer().start();
		// } catch (IOException ioe) {
		// logger.error("Couldn't start server:", ioe);
		// System.exit(-1);
		// }

		Server.main(args);
	}

}
