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

//import javax.microedition.io.Connector;
import BluetoothTCKAgent.Connector;
import javax.obex.ClientSession;

/**
 * The OBEXClientThread class is used with the CLIENT command.
 * When the OBEX TCK Agent receives the CLIENT command, it will
 * spawn this thread to make a connection to the TCK application.
 */
public class OBEXClientThread extends Thread {

    private String conn_string = null;
    
    // the timeout value specified by the user
    private long timeout;

    /**
     * Creates a <code>OBEXClientThread</code> which uses the
     * string <code>c</code> to make a connection to the TCK client.
     *
     * @param c the string used to make a connection to the client
     * @param timeout the timeout value specified 
     */
    public OBEXClientThread(String c, long timeout) {
        conn_string = c.trim();
        this.timeout = timeout;
    }

    /**
     * Attempts to make a connection to the TCK client with the
     * connection string provided in the constructor.  It will make
     * 3 attempts to connect to the TCK client.
     */
    public void run() {
        ClientSession conn = null;
        int i = 0;
        try {
        	this.sleep(this.timeout * 80);
        } catch (Exception e) {
            System.out.println("Exception while thread.sleep");
        }

        for (i=0; i<5; i++) {
            try {
                conn = (ClientSession)Connector.open(conn_string);
                break;
            } catch (Exception e) {
                try {
                	Thread.sleep(this.timeout * 30);
                } catch (Exception e2) {
                    System.out.println("Exception in Thread.sleep.");
                }
            }
        }

        if (i==5) {
            System.out.println("Connection did not succeed to TCK client");
        }

        if(conn != null) {
            try {
                this.sleep(this.timeout * 40);
                conn.close();
            } catch (Exception e) {
                System.out.println("Exception while Closing");
            }
        }
    }
}

