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


package OBEXTCKAgent;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The OBEXTCKAgent class contains the main method of the OBEX TCK
 * Agent MIDlet.
 */
public class OBEXTCKAgent extends MIDlet {
    /**
     * Creates a <code>OBEXTCKAgent</code> object.
     */
    public OBEXTCKAgent() {
    }

    /**
     * Starts the OBEX TCK Agent.  This MIDlet responds to requests
     * made from the TCK Client to test the OBEX API.
     *
     * @exception MIDletStateChangeException never thrown
     */
    public void startApp() throws MIDletStateChangeException {
    	
      	// To fix bug 12833 to support configurable timeout
    	// read the timeout value set by the user and pass it to the constructor
    	// Some implementations (e.g the RI) allows you to set system
        // properties on the command line using the -D option. First
        // check if this has been set. If not, look up the App properties.
               
        String timeout = System.getProperty(HelperUtil.TIMEOUT);
        if (timeout == null) {
        	timeout = getAppProperty(HelperUtil.TIMEOUT);
        }
        
        // Get the transport configuration
        String transport = System.getProperty(HelperUtil.TRANSPORT);
        if (transport == null) {
        	transport = getAppProperty(HelperUtil.TRANSPORT);
        }
        OBEXTCKAgentApp app = new OBEXTCKAgentApp(timeout, transport);
        app.start();
    }

    /**
     * Called when the MIDlet is paused.  This method does nothing.
     */
    public void pauseApp() {

    }

    /**
     * Called when the MIDlet is destroyed.  This method does nothing.
     *
     * @param unconditional ignored
     *
     * @exception MIDletStateChangeException never thrown
     */
    public void destroyApp(boolean unconditional)
        throws MIDletStateChangeException {

    }
}
