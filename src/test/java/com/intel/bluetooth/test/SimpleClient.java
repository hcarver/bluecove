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

import javax.microedition.io.Connector;
import java.io.DataOutputStream;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.BluetoothStateException;
import java.io.IOException;
import javax.microedition.io.StreamConnection;
import java.util.Enumeration;
import java.util.Vector;

public class SimpleClient implements DiscoveryListener  {

	public static final UUID uuid = new UUID(Consts.TEST_UUID, false);

	Vector devices;

	Vector records;
	
	CancelThread cancelThread;
	
	class CancelThread extends Thread {
		
		SimpleClient client;

		boolean inquiryCompleted;
		
		CancelThread(SimpleClient client) {
			this.client = client;
			this.inquiryCompleted = false;
		}

		public void run() {
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (!this.inquiryCompleted) {
				System.out.println("cancelling inquiry on timeout");

				try {
					if (LocalDevice.getLocalDevice().getDiscoveryAgent().cancelInquiry(client)) {
						System.out.println("inquiry cancelled");		
					}
				} catch (BluetoothStateException bse) {
					System.out.println("Got BluetoothStateException: " + bse);
				}
			}
		}
	}

	public SimpleClient(String message) {
		
		devices = new Vector();

		cancelThread = new CancelThread(this); 
		cancelThread.start();

		synchronized (this) {
			try {
				LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, this);
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (BluetoothStateException e) {
				e.printStackTrace();
			}
		}

		for (Enumeration enum_d = devices.elements(); enum_d.hasMoreElements();) {
			RemoteDevice d = (RemoteDevice) enum_d.nextElement();

			try {
				System.out.println("discovered name: " + d.getFriendlyName(false));
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("discovered address: " + d.getBluetoothAddress());

			synchronized (this) {
				records = new Vector();

				try {
					LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(new int[] { 0x0100, 0x0101 },
							new UUID[] { uuid }, d, this);
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} catch (BluetoothStateException e) {
					e.printStackTrace();
				}
			}

			/*
			 * 
			 * BUGBUG: need to give the system time to sort itself out after
			 * doing a service attribute request
			 * 
			 */

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			for (Enumeration enum_r = records.elements(); enum_r.hasMoreElements();) {
				ServiceRecord r = (ServiceRecord) enum_r.nextElement();

				String name = r.getAttributeValue(0x0100).getValue().toString();
				System.out.println("Name attribute: " + name);

				if (name.startsWith(Consts.TEST_SERVERNAME_PREFIX)) {
					try {

						StreamConnection conn = (StreamConnection) Connector.open(r.getConnectionURL(
								ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false));
						DataOutputStream dos = new DataOutputStream(conn.openOutputStream());

						System.out.println("Sending message");
						dos.writeUTF(message);

						dos.close();
						conn.close();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		devices.addElement(btDevice);
		System.out.println("deviceDiscovered DeviceClass: " + ((Object)cod).toString());
	}

	public synchronized void inquiryCompleted(int discType) {
		System.out.println("inquiry completed: discType = " + discType);
		cancelThread.inquiryCompleted = true;
		notifyAll();
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		for (int i = 0; i < servRecord.length; i++) {
			records.addElement(servRecord[i]);
		}
	}

	public synchronized void serviceSearchCompleted(int transID, int respCode) {
		System.out.println("service search completed: respCode = " + respCode);
		notifyAll();
	}
	
	public static void main(String[] args) {
		//System.getProperties().put("bluecove.debug", "true");
		System.getProperties().put("bluecove.native.path", "./src/main/resources");
		
		if (args.length == 1) {
			new SimpleClient(args[0]);
		} else {
			System.out.println("syntax: SimpleClient <message>");
			new SimpleClient("bluecove test message");
		}
	}

}