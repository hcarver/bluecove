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
import javax.bluetooth.*;
import BluetoothTCKAgent.Connector;
import javax.microedition.io.*;

public class RFCOMMThread extends Thread {

    private String message;
    private String data = "data", command = "Command";
    private StreamConnectionNotifier server = null;
    private InputStream in = null;
    private OutputStream out = null;
    private StreamConnection channel = null, helperchannel = null;
    private boolean can_run=true;
    int buffersize = 700, counter = 0, ch = -5, timeout = 0;
    private byte[] buffer;

    public RFCOMMThread(String str) {
        super(str);
        try {
            System.out.println("RFCOMMThread: Starting RFCOMM Service");

            server = (StreamConnectionNotifier)Connector.open(
                "btspp://localhost:2000000031b811d88698000874b33fc0",
                Connector.READ_WRITE, true);
        } catch (Exception e) {
            System.out.println("RFCOMMThread: Unable to start" +
                                " RFCOMM Service. Aborting.");
            can_run = false;
        }
    }

    public void run() {
        while (can_run) {
            try {
                System.out.println("RFCOMMThread: Waiting for Client" +
                                    " to Connect");
                channel = server.acceptAndOpen();
            } catch (InterruptedIOException e) {
            	System.out.println("RFCOMMThread:TCK Interrupted");
            	return;
            } catch (Exception e) {
                System.out.println("RFCOMMThread: Error occurred when " +
                                    "connecting with client:" + e);
                can_run = false;
                command = "CLOSE";
                if ("Stack closed".equals(e.getMessage())) {
                	return;
                }
            }

            if (can_run) {
                System.out.println("RFCOMMThread: Client made " +
                                                        "a Connection");
                try {
                    in = channel.openInputStream();
                    out = channel.openOutputStream();
                } catch (Exception e) {
                    System.out.println("RFCOMMThread: Opening of " +
                                       "InputStream() & OutputStream" +
                                       " failed : " + e);
                    command = "CLOSE";
                }
            } else {
                command = "CLOSE";
            }

            can_run = true;
            while (!command.equals("CLOSE")) {
                buffer = new byte[buffersize];
                counter = 0;
                System.out.println("RFCOMMThread: Reading InputStream");
               /*
                * Keep reading until data comes in
                * or buffer gets full.
                */
                timeout = 0;
                try {
                    while ((in.available() == 0) && (timeout < 10)) {

                        TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                        timeout++;
                    }
                } catch (Exception e) {}

                if (timeout == 10) {
                    counter = buffersize;
                }

                try {
                    while ((ch = in.read()) > 20
                                && counter < buffersize) {
                        buffer[counter++] = (byte)ch;
                    }
                } catch (Exception e) {
                    System.out.println("RFCOMMThread: Error while" +
                                        " reading InputStream " + e);
                    ch = -1;
                }

                if (ch == -1 || counter==buffersize || counter==0) {
                    /* Implies Connection Timed-Out,
                     * Error occurred while reading, or
                     * read buffer got full.
                     */
                    message = "CLOSE connection";
                    buffer = message.getBytes();
                }

                message = (new String(buffer)).trim();
                int space = message.indexOf(" ");
                command = message.substring(0, space);
                data = message.substring(space + 1);

                if (command.equals("ECHO")) {
                    System.out.println("RFCOMMThread: ECHO Command Called");

                    TCKAgentUtil.pause(TCKAgentUtil.SHORT);

                    data = data + "\n";
                    buffer = data.getBytes();
                    try {
                        out.write(buffer, 0, buffer.length);
                        out.flush();
                    } catch (Exception e) {
                        System.out.println("RFCOMMThread: Error while " +
                                           "ECHOing data: " + e);
                        command = "CLOSE";
                    }
                } // ECHO Command

                else if (command.equals("READ")) {
                    System.out.println("RFCOMMThread: READ Command Called");
                } // READ Command

                else if (command.equals("LOG")) {
                    System.out.println("RFCOMMThread: LOG Command Called");
                } // LOG Command

                else if (command.equals("WAIT")) {
                    System.out.println("RFCOMMThread: WAIT Command Called");
                    int timetowait = Integer.parseInt(data);

                    try{
                        this.sleep(timetowait);
                    } catch (Exception e) {
                        System.out.println("RFCOMMThread: Error in WAIT: " + e);
                        command = "CLOSE";
                    }
                } // WAIT Command

                else if (command.equals("CLIENT")) {
                    System.out.println("RFCOMMThread: CLIENT Command Called");

                    command = "CLOSE";
                    TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
                    try {
                        in.close();
                        out.close();
                        channel.close();
                    } catch (Exception e) {
                        System.out.println("RFCOMMThread: Error Closing" +
                                           " Channel :" + e);
                    }

                    TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);

                    try{
                        helperchannel = (StreamConnection)
                                Connector.open(data, Connector.READ_WRITE);
                    } catch (Exception e) {
                        System.out.println("RFCOMMThread: Error while" +
                                           " Connecting to client" + e);
                    }

                    TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
                    try {
                        helperchannel.close();
                    } catch (Exception e) {
                        System.out.println("RFCOMMThread: Error Closing" +
                                           " Channel :" + e);
                    }

                } // CLIENT Command

                else { // If no command is executed, then CLOSE connection
                    if(!command.equals("CLOSE")) {
                        System.out.println("RFCOMMThread: ERROR." +
                                            "Unrecognized Command");
                    }
                }
            } //While Channel Connection Not Closed

            System.out.println("RFCOMMThread: Closing all Connections" +
                               " to the Client");
            try {
                in.close();
                out.close();
                channel.close();
            } catch (Exception e) {
                System.out.println("RFCOMMThread: Error closing " +
                                   "Connections: " + e);
            }

            command = "Command";
        } // while(can_run)

        System.out.println("RFCOMMThread: Shutting Down Service");
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                System.out.println(
                    "Error closing RFCOMM Service : " +
                        e.getMessage());
            }
        }
    } //method run()
} // Class RFCOMMThread
