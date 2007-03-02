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

public class JSR82APIDeclarationsTest extends APIDeclarationsTestCase {

	File wtkLibDir;
	
	public JSR82APIDeclarationsTest() {

	}

	public JSR82APIDeclarationsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		String wtkHome = System.getProperty("WTK_HOME");
		if (wtkHome == null) {
			throw new Error("System  property WTK_HOME not found");
		}
		wtkLibDir = new File(wtkHome, "lib");
		if (!wtkLibDir.exists() || (!wtkLibDir.isDirectory())) {
			throw new Error("Invalid system  property WTK_HOME = [" + wtkHome + "]");
		}

		reportExtra = false;
		reportOnly = false;
		reportConstructors = false;
	}

	private List getWtkJarURLList(String[] jarNames) throws Exception {
		List files = new Vector();
		for (int i = 0; i < jarNames.length; i++) {
			File jar = new File(wtkLibDir, jarNames[i]);
			if (!jar.exists()) {
				throw new FileNotFoundException(jar.getAbsolutePath());
			}
			files.add(jar.getCanonicalFile().toURI().toURL());
		}
		return files;
	}

	public void testGenericConnectionFrameworkAPI() throws Exception {

		ClassPool wtkClassPool = createClassPool(getWtkJarURLList(new String[] { "midpapi20.jar", "cldcapi11.jar" }));
		ClassPool ourClassPool = createClassPool("javax.microedition.io.Connection");

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

		verifyClassList(names, ourClassPool, wtkClassPool);
	}

	public void testJsr082API() throws Exception {

		ClassPool wtkClassPool = createClassPool(getWtkJarURLList(new String[] { "midpapi20.jar", "cldcapi11.jar",
				"jsr082.jar" }));
		ClassPool ourClassPool = createClassPool("javax.bluetooth.ServiceRecord");

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

		verifyClassList(names, ourClassPool, wtkClassPool);
	}

}
