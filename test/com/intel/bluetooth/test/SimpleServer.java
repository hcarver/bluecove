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
package com.intel.bluetooth.test;

import java.io.DataInputStream;
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import com.intel.bluetooth.DebugLog;

public class SimpleServer {
	
	public static final UUID uuid = new UUID(Consts.TEST_UUID, false);

	public SimpleServer(String name) throws BluetoothStateException {
		
		//System.setProperty("bluecove.debug", "true");
		//System.setProperty("bluecove.native.path", ".");
		//System.setProperty("bluecove.native.path", "../rel/1.2.1");
		
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		System.out.println("Local bt address " + localDevice.getBluetoothAddress());
 	    System.out.println("Local bt name    " + localDevice.getFriendlyName());
 	    
		int connectionsCount = 0;
		
		while (run(name) && connectionsCount < 10) {
			connectionsCount ++;
		}
		
		System.exit(0);
	}
	
	public boolean run(String name) {
		try {
			StreamConnectionNotifier server = (StreamConnectionNotifier) Connector
					.open("btspp://localhost:"
							+ uuid
							+ ";name="
							+ name
							+ ";authorize=false;authenticate=false;encrypt=false");

			System.out.println("Server started " + name);
			
			StreamConnection conn = server.acceptAndOpen();

			System.out.println("Server received connection");
			
			DataInputStream dis = new DataInputStream(conn.openInputStream());

			System.out.print("Got message[");
			System.out.print(dis.readUTF());
			System.out.println("]");
			
			dis.close();

			conn.close();

			server.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length == 1)
				new SimpleServer(Consts.TEST_SERVERNAME_PREFIX + args[0]);
			else {
				System.out.println("syntax: SimpleServer <service name>");
				new SimpleServer(Consts.TEST_SERVERNAME_PREFIX + "1");
			}
		} catch (Throwable e) {
			DebugLog.fatal("initialization error", e);
		}
	}
}