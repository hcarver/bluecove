/**
 *  BlueCove - Java library for Bluetooth
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
package net.sf.bluecove;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * @author vlads
 * 
 */
public class ExampleTest extends TestCase {

	private static final UUID uuid = new UUID(0x2108);

	private Thread serverThread;

	private static final String echoGreeting = "I echo";

	protected void setUp() throws Exception {
		super.setUp();
		EmulatorTestsHelper.startInProcessServer();
		EmulatorTestsHelper.useThreadLocalEmulator();
		serverThread = EmulatorTestsHelper.runNewEmulatorStack(new EchoServerRunnable());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if ((serverThread != null) && (serverThread.isAlive())) {
			serverThread.interrupt();
			serverThread.join();
		}
		EmulatorTestsHelper.stopInProcessServer();
	}

	private class EchoServerRunnable implements Runnable {

		public void run() {

			StreamConnectionNotifier service = null;

			try {
				String url = "btspp://localhost:" + uuid.toString() + ";name=TServer";
				service = (StreamConnectionNotifier) Connector.open(url);

				StreamConnection conn = (StreamConnection) service.acceptAndOpen();

				System.out.println("Server received connection");

				DataOutputStream dos = conn.openDataOutputStream();
				DataInputStream dis = conn.openDataInputStream();

				dos.writeUTF(echoGreeting);
				dos.flush();

				String received = dis.readUTF();
				System.out.println("Server received:" + received);

				dos.writeUTF(received);
				dos.flush();

				dos.close();
				dis.close();

				conn.close();
			} catch (Throwable e) {
				System.err.print(e.toString());
				e.printStackTrace();
			} finally {
				if (service != null) {
					try {
						service.close();
					} catch (IOException ignore) {
					}
				}
			}
		}
	}

	public void testConnection() throws Exception {
		DiscoveryAgent discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
		// Find service
		String serverURL = discoveryAgent.selectService(uuid, ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
		Assert.assertNotNull("service not found", serverURL);

		StreamConnection conn = null;
		try {
			conn = (StreamConnection) Connector.open(serverURL);
			DataOutputStream dos = conn.openDataOutputStream();
			DataInputStream dis = conn.openDataInputStream();

			String received = dis.readUTF();
			Assert.assertEquals("handshake", echoGreeting, received);

			String message = "TestMe";
			System.out.println("Client Sending message:" + message);
			dos.writeUTF(message);

			received = dis.readUTF();
			Assert.assertEquals("echo", received, message);

			dos.close();
			dis.close();

		} catch (IOException e) {
			System.err.print(e.toString());
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
}
