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


/**
 * This class assists the OBEX TCK Agent my terminating old
 * connections.  This thread detects when a connection may have
 * ended on the TCK client and cleans up the local Connection
 * object.
 */
public class OBEXAgentThread extends Thread {

    OBEXTCKAgentApp parent;
    private boolean TIMEOUT;

    /**
     * Creates a <code>OBEXAgentThread</code>.
     *
     * @param p the thread that is accepting OBEX connections
     */
    public OBEXAgentThread(OBEXTCKAgentApp p) {
        parent = p;
    }

    /**
     * Resets the timer.
     */
    public void resetAgent() {
        TIMEOUT = false;
    }

    /**
     * Main thread body.  This method will continue sleeping and
     * then waking up to verify that action has occurred on the
     * connection within the last time period.
     */
    public void run() {
        TIMEOUT = false;

        /*
         * Keep looping until TCK Test keeps communicating with
         * OBEXThread.
         */
        while(!TIMEOUT) {
            TIMEOUT = true;
            try {
            	this.sleep(parent.timeout * 40);
            } catch (Exception e) {
                System.out.println("Exception while thread.sleep");
            }
        }

        parent.cont = true;
    }
}


