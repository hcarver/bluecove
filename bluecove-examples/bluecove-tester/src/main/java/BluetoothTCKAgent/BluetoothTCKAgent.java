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

import javax.microedition.midlet.*;
import javax.bluetooth.*;

public class BluetoothTCKAgent extends MIDlet {

    RFCOMMThread rfcommthread;

    L2CAPThread l2capthread;

    GOEPThread goepthread;

    PushBTConnectionThread pushThread;

    private final String DEFAULT_PORT = "9006";

    public void startApp() throws MIDletStateChangeException {
        System.out.println("BluetoothTCKAgent: "
                + "Setting Device to Discoverable");
        try {
            (LocalDevice.getLocalDevice()).setDiscoverable(DiscoveryAgent.GIAC);
        } catch (BluetoothStateException ex) {
            System.out.println("BluetoothTCKAgent: " + "Exception Occured :"
                    + ex);
            System.out.println("BluetoothTCKAgent: Unable to continue.");
            return;
        }

        // Get the "bluetooth.agent_mtu" configuration
        String agentMtu = System.getProperty(L2CAPThread.BLUETOOTH_AGENT_MTU);
        if (agentMtu == null) {
        	agentMtu = getAppProperty(L2CAPThread.BLUETOOTH_AGENT_MTU);
        }
        rfcommthread = new RFCOMMThread("RFCOMM Thread");
        l2capthread = new L2CAPThread("L2CAP Thread", agentMtu);
        goepthread = new GOEPThread("GOEP Thread");

        // Retreive the port number on which the PushBTConnectionThread should
        // listen on, from the application properties.
        
        // Some implementations (e.g the RI) allows you to set system
        // properties on the command line using the -D option. First
        // check if this has been set. If not, look up the App properties.
               
        String port = System.getProperty("PushConnection-Port");
        if (port == null) {
            port = getAppProperty("PushConnection-Port");
        }
        
        boolean invalidPort = false;

        // Validate the property value
        if (port == null) {
            invalidPort = true;
        }

        try {
            int portNum = Integer.parseInt(port);
            if (portNum <= 0) {
                invalidPort = true;
            }
        } catch (NumberFormatException nfe) {
            invalidPort = true;
        }

        // Replace with default if invalid
        if (invalidPort) {
            System.out.println("BluetoothTCKAgent: Invalid port specified "
                    + port + " . Using default: " + DEFAULT_PORT);
            port = DEFAULT_PORT;
        }

        pushThread = new PushBTConnectionThread("Push Thread", port);

        rfcommthread.start();
        l2capthread.start();
        goepthread.start();
        pushThread.start();
    }

    /**
     * Stop
     */
    protected void pauseApp() {
        // stop threads
    }

    /**
     * Called by the framework before the application is unloaded
     */
    protected void destroyApp(boolean unconditional) {
    }

}
