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
package com.intel.bluetooth.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.DebugLog;

public class Server {

	static final int rmiRegistryPortDefault = 8090;

	static int rmiRegistryPort = rmiRegistryPortDefault;

	// Prevents GC
	private static Server server;

	private Registry registry = null;

	private Remote srv;

	public static void main(String[] args) {
		String port = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--port") && i < (args.length - 1)) {
				i++;
				port = args[i];
			} else if (args[i].equalsIgnoreCase("--help")) {
				help();
				return;
			} else {
				help();
				return;
			}
		}
		if (port == null) {
			port = System.getProperty(BlueCoveConfigProperties.PROPERTY_EMULATOR_PORT);
		}
		start(port);
	}

	private static void help() {
		StringBuffer usage = new StringBuffer();
		usage.append("Usage:\n java ").append(Server.class.getName());
		usage.append("[--port rmiListeningPort]");
		System.out.println(usage);
	}

	public static void start(String port) {
		if (server != null) {
			return;
		}
		server = new Server();
		server.run(port);
	}

	private void run(String port) {
		startRMIRegistry(port);
		startRMIService();

		DebugLog.debug("Emulator RMI Service listening on port " + rmiRegistryPort);

		// wait for RMI threads to start up
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}

	private void startRMIRegistry(String port) {
		try {
			if ((port != null) && (port.length() > 0)) {
				rmiRegistryPort = Integer.parseInt(port);
			}
			registry = LocateRegistry.createRegistry(rmiRegistryPort);
		} catch (RemoteException e) {
			throw new Error("Fails to start RMIRegistry", e);
		}
	}

	private void startRMIService() {
		try {
			srv = new RemoteServiceImpl();
			if (srv instanceof UnicastRemoteObject) {
				registry.rebind(RemoteService.SERVICE_NAME, srv);
			} else {
				Remote stub = UnicastRemoteObject.exportObject(srv, 0);
				registry.rebind(RemoteService.SERVICE_NAME, stub);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
