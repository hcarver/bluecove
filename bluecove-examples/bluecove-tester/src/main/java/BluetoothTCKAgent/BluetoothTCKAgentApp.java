/*
 *  $HeadURL$
 *
 *
 *  Copyright (c) 2001-2008 Motorola, Inc.  All rights reserved. 
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *        
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  
 *  Revision History:
 *
 *  Date             Author                   Comment
 *  ---------------------------------------------------------------------------------
 *  Oct 15,2006      Motorola, Inc.           Initial creation        
 *
 */

package BluetoothTCKAgent;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;

public class BluetoothTCKAgentApp {

	private static RFCOMMThread rfcommthread;

	private static L2CAPThread l2capthread;

	private static GOEPThread goepthread;

	public static void main(String[] args) {

		System.out.println("BluetoothTCKAgentApp: "
				+ "Setting Device to Discoverable");
		try {
			(LocalDevice.getLocalDevice()).setDiscoverable(DiscoveryAgent.GIAC);
		} catch (BluetoothStateException ex) {
			System.out.println("BluetoothTCKAgentApp: " + "Exception Occured :"
					+ ex);
			System.out.println("BluetoothTCKAgent: Unable to continue.");
			return;
		}

		String agentMtu = System.getProperty(L2CAPThread.BLUETOOTH_AGENT_MTU);
		for (int i = 0; i < args.length - 1; i++) {
			System.out.println("args[" + i + "] is: " + args[i]);
			if (args[i].equals(L2CAPThread.BLUETOOTH_AGENT_MTU)) {
				agentMtu = args[i + 1];
			}
		}

		rfcommthread = new RFCOMMThread("RFCOMM Thread");
		l2capthread = new L2CAPThread("L2CAP Thread", agentMtu);
		goepthread = new GOEPThread("GOEP Thread");

		rfcommthread.start();
		l2capthread.start();
		goepthread.start();
	}
}
