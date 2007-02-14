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
package org.microemu;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Vector;

import javassist.ClassPool;

public class TestJSR82APIDeclarations extends APIDeclarationsTester {

	String wtkHome;

	 public TestJSR82APIDeclarations() {
			
	 }
		
	 public TestJSR82APIDeclarations(String name) {
		 super(name);
	 }

	ClassLoader wtkLoader;

	ClassPool wtkClassPool;

	ClassLoader ourLoader;

	protected void setUp() throws Exception {
		super.setUp();
		// TODO read Environment variable
		wtkHome = "/D:/di/tools/WTK22/";

		reportOnly = true;
		reportConstructors = false;
		
		List l = getWtkJarURLList(new String[] { "midpapi20.jar", "cldcapi11.jar" });
		wtkLoader = getClassLoader(l);
		wtkClassPool = getClassPool(l);
		ourLoader = this.getClass().getClassLoader();
	}

	private List getWtkJarURLList(String[] jarNames) throws Exception {
		List files = new Vector();
		File lib = new File(wtkHome, "lib");
		File lib2 = new File(wtkHome, "wtklib");
		for (int i = 0; i < jarNames.length; i++) {
			File jar = new File(lib, jarNames[i]);
			if (!jar.exists()) {
				jar = new File(lib2, jarNames[i]);
			}
			if (!jar.exists()) {
				throw new FileNotFoundException(jarNames[i]);
			}
			files.add(jar.getCanonicalFile().toURI().toURL());
		}
		return files;
	}

	public void testGenericConnectionFrameworkAPI() throws Exception {

		List names = new Vector();

		String aPackage = "javax.microedition.io.";
		// Interface
		//names.add(aPackage + "CommConnection");
		names.add(aPackage + "Connection");
		names.add(aPackage + "ContentConnection");
		//names.add(aPackage + "Datagram");
		//names.add(aPackage + "DatagramConnection");
		//names.add(aPackage + "HttpConnection");
		//names.add(aPackage + "HttpsConnection");
		names.add(aPackage + "InputConnection");
		names.add(aPackage + "OutputConnection");
		//names.add(aPackage + "SecureConnection");
		//names.add(aPackage + "SecurityInfo");
		//names.add(aPackage + "ServerSocketConnection");
		//names.add(aPackage + "SocketConnection");
		names.add(aPackage + "StreamConnection");
		names.add(aPackage + "StreamConnectionNotifier");
		//names.add(aPackage + "UDPDatagramConnection");

		// Class
		names.add(aPackage + "Connector");
		//names.add(aPackage + "PushRegistry");

		// Exception
		names.add(aPackage + "ConnectionNotFoundException");

		verifyClassList(names, wtkLoader, ourLoader, wtkClassPool);
	}

	public void testJsr082API() throws Exception {

		List l = getWtkJarURLList(new String[] { "midpapi20.jar", "cldcapi11.jar", "jsr082.jar" });
		wtkLoader = getClassLoader(l);
		wtkClassPool = getClassPool(l);

		List names = new Vector();

		String aPackage = "javax.bluetooth.";
		// Interface
		names.add(aPackage + "DiscoveryListener");
		//names.add(aPackage + "L2CAPConnection");
		//names.add(aPackage + "L2CAPConnectionNotifier");
		names.add(aPackage + "ServiceRecord");

		// Class
		names.add(aPackage + "DataElement");
		names.add(aPackage + "DeviceClass");
		names.add(aPackage + "DiscoveryAgent");
		names.add(aPackage + "LocalDevice");
		names.add(aPackage + "RemoteDevice");
		names.add(aPackage + "UUID");

		// Exception
		names.add(aPackage + "BluetoothConnectionException");
		names.add(aPackage + "BluetoothStateException");
		names.add(aPackage + "ServiceRegistrationException");

		verifyClassList(names, wtkLoader, ourLoader, wtkClassPool);
	}


}
