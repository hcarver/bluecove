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

import java.io.*;
import javax.obex.*;
import javax.microedition.io.*;


public class GOEPClientThread extends Thread {

    String conn_string = null;
    ClientSession conn = null;
    int i;

    public GOEPClientThread(String c) {
        conn_string = c.trim();
    }

    public void run() {

        TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
        
        for (i=0; i<3; i++) {
            try {
                conn = (ClientSession)Connector.open(conn_string);
                break;
            } catch (Exception e) {
                System.out.println("GOEPThread: Error occured when " +
                                    "attempting to connect to client " +
                                    "with connection string :" + 
                                    conn_string);
                TCKAgentUtil.pause(3000);
            }
        }

        if (i==3) {
            System.out.println("GOEPThread: Unable to connect with " + 
                                "client even after " + i + "attempts");
        }

        if(conn != null) {
            TCKAgentUtil.pause(4000);
            try {
                conn.close();
            } catch (Exception e) {
                System.out.println("GOEPThread: Exception while Closing");
            }
        }
    } //method run()
} // class GOEPClientThread

