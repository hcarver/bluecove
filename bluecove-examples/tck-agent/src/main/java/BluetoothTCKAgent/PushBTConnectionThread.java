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
import javax.microedition.io.*;

public class PushBTConnectionThread extends Thread {

    private String message;

    private String port;

    private String data = "data", command = "Command";

    private StreamConnectionNotifier server = null;

    private InputStream in = null;

    private OutputStream out = null;

    private StreamConnection channel = null;

    private boolean can_run = true;

    int buffersize = 700, counter = 0, ch = -5, timeout = 0;

    private byte[] buffer;

    private static final String SUCCESS = "PASSED";

    private static final String FAIL = "FAILED";

    private static final String MSG_TERMINATOR = "\n";

    /**
	 * Default timeout value if not set by the user
	 */
	private int configTimeout = TCKAgentUtil.SHORT;

    public PushBTConnectionThread(String str, String port, final String customTimeout) {
        super(str);
        try {           
            this.port = port;
            server = (StreamConnectionNotifier) Connector.open(
                    "socket://:" + port, Connector.READ_WRITE, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setConfigTimeout(customTimeout);
    }

    public void run() {
        while (can_run) {
            try {
                System.out
                        .println("PushBTConnectionThread: Waiting for Client to Connect on port " + port);
                channel = server.acceptAndOpen();
            } catch (Exception e) {
                System.out
                        .println("PushBTConnectionThread: Error occurred when "
                                + "connecting with client:" + e);
                can_run = false;
                command = "CLOSE";
            }
            if (can_run) {
                System.out.println("PushBTConnectionThread: Client made "
                        + "a Connection");
                try {
                    in = channel.openInputStream();
                    out = channel.openOutputStream();
                } catch (Exception e) {
                    System.out
                            .println("PushBTConnectionThread: Opening of "
                                    + "InputStream() & OutputStream"
                                    + " failed : " + e);
                    command = "CLOSE";
                }
            } else {
                command = "CLOSE";
            }

            // can_run = true;
            while (!command.equals("CLOSE")) {
                buffer = new byte[buffersize];
                counter = 0;
                timeout = 0;
                try {
                    while ((in.available() == 0) && (timeout < 10)) {
                        TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                        timeout++;
                    }
                } catch (Exception e) {
                }

                if (timeout == 10) {
                    counter = buffersize;
                }
                try {
                    // Newlines and carriage returns should end the read
                    // These are all below ASCII 20
                    while ((ch = in.read()) > 20 && counter < buffersize) {
                        buffer[counter++] = (byte) ch;
                    }
                } catch (Exception e) {
                    System.out.println("PushBTConnectionThread: Error while"
                            + " reading InputStream " + e);
                    ch = -1;
                }

                if (ch == -1 || counter == buffersize || counter == 0) {
                    /*
                     * Implies Connection Timed-Out, Error occurred while
                     * reading, or read buffer got full.
                     */
                    message = "CLOSE connection";
                    buffer = message.getBytes();
                }

                message = (new String(buffer)).trim();
                int space = message.indexOf(" ");
                command = message.substring(0, space);
                data = message.substring(space + 1);

                if (command.equals("CONNECT")) {
                    System.out
                            .println("PushBTConnectionThread: CONNECT Command Called");

                    space = data.indexOf(" ");
                    String devId = data.substring(0, space);
                    System.out.println("PushBTConnectionThread: Device Id "
                            + devId);
                    int uuidEnd = data.indexOf(' ', space + 1);
                    String uuid = data.substring(space + 1, uuidEnd);
                    System.out.println("PushBTConnectionThread: UUID " + uuid);

                    int responseRqtEnd = data.indexOf(' ', uuidEnd + 1);
                    String responseRequired = null;
                    String handshake = null;

                    if (responseRqtEnd != -1) {
                        responseRequired = data.substring(uuidEnd + 1,
                                responseRqtEnd);
                        handshake = data.substring(responseRqtEnd);
                    } else {
                        responseRequired = data.substring(uuidEnd + 1);
                    }
                    System.out
                            .println("PushBTConnectionThread: Response Reqt. "
                                    + responseRequired);
                    System.out.println("PushBTConnectionThread: Handshake "
                            + handshake);

                    boolean verify = responseRequired
                            .equals("response-required");

                    TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                    buffer = new byte[buffersize];
                    try {
                        if (connect(devId, uuid, verify, handshake)) {
                            System.out
                                    .println("PushBTConnectionThread: L2CAPConnection Successful");
                            out.write(SUCCESS.getBytes());
                            out.write(MSG_TERMINATOR.getBytes());
                            out.flush();
                        } else {
                            System.out
                                    .println("PushBTConnectionThread: L2CAPConnection Failed");
                            out.write(FAIL.getBytes());
                            out.write(MSG_TERMINATOR.getBytes());
                            out.flush();
                        }
                    } catch (Exception ne) {
                        ne.printStackTrace();
                    }
                } else if (command.equals("CONNECT2URL")) {
                    System.out
                            .println("PushBTConnectionThread: CONNECT2URL Command Called");

                    space = data.indexOf(" ");
                    String connectionURL = data.substring(0, space);
                    System.out.println("PushBTConnectionThread: ConnectionURL "
                            + connectionURL);

                    int responseRqtEnd = data.indexOf(' ', space + 1);
                    String responseRequired = null;
                    String handshake = null;

                    if (responseRqtEnd != -1) {
                        responseRequired = data.substring(space + 1,
                                responseRqtEnd);
                        handshake = data.substring(responseRqtEnd);
                    } else {
                        responseRequired = data.substring(space + 1);
                    }
                    System.out
                            .println("PushBTConnectionThread: Response Reqt. "
                                    + responseRequired);
                    System.out.println("PushBTConnectionThread: Handshake "
                            + handshake);

                    boolean verify = responseRequired
                            .equals("response-required");

                    TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                    buffer = new byte[buffersize];
                    try {
                        if (connect(connectionURL, verify, handshake)) {
                            System.out
                                    .println("PushBTConnectionThread: L2CAPConnection Successful");
                            out.write(SUCCESS.getBytes());
                            out.write(MSG_TERMINATOR.getBytes());
                            out.flush();
                        } else {
                            System.out
                                    .println("PushBTConnectionThread: L2CAPConnection Failed");
                            out.write(FAIL.getBytes());
                            out.write(MSG_TERMINATOR.getBytes());
                            out.flush();
                        }
                    } catch (Exception ne) {
                        ne.printStackTrace();
                    }
                } else if (command.equals("WAIT")) {

                    try {
                        System.out.println("PushBTConnectionThread: WAIT "
                                + "Command Called");
                        int timetowait;
                        try {
                            timetowait = Integer.parseInt(data);
                        } catch (NumberFormatException nfe) {
                            timetowait = 10000; // default
                        }
                        System.out.println("PushBTConnectionThread TIME: "
                                + timetowait);

                        Thread.sleep(timetowait);
                    } catch (Exception e) {
                    }
                } else if (command.equals("GETSRHANDLE")) {
                    System.out.println("PushBTConnectionThread: GETSRHANDLE "
                            + "Command Called");
                    space = data.indexOf(" ");
                    String devId = data.substring(0, space);
                    System.out.println("PushBTConnectionThread: Device Id "
                            + devId);
                    String uuid = data.substring(space + 1);
                    System.out.println("PushBTConnectionThread: UUID " + uuid);

                    TCKAgentUtil.pause(TCKAgentUtil.SHORT);

                    long handle = getServiceRecordHandle(devId, uuid);
                    String handleStr = Long.toString(handle);
                    try {
                        out.write(handleStr.getBytes());
                        out.write(MSG_TERMINATOR.getBytes());
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (command.equals("GETSDCLASS")) {
                    System.out.println("PushBTConnectionThread: GETSDCLASS "
                            + "Command called.");
                    String address = data.substring(0, 12);
                    TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                    int serviceClasses = TCKAgentUtil.getServiceClass(address, this.configTimeout);
                    String srvClassesStr = Integer.toString(serviceClasses);
                    try {
                        out.write(srvClassesStr.getBytes());
                        out.write(MSG_TERMINATOR.getBytes());
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (command.equals("GETFRNAME")) {
                    System.out.println("PushBTConnectionThread: GETFRNAME "
                            + "Command called.");
                    String address = data.substring(0, 12);
                    TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                    String friendlyName = null;
                    String localDeviceAddress = null;
                    LocalDevice localDevice = null;
                    try {
                        localDevice = LocalDevice.getLocalDevice();
                        localDeviceAddress = localDevice.getBluetoothAddress();
                        System.out
                                .println("PushBTConnectionThread.run(): Local Device "
                                        + "address : "
                                        + localDeviceAddress
                                        + " , Address "
                                        + "parameter: "
                                        + address);
                    } catch (BluetoothStateException bse) {
                        System.out.println("PushBTConnectionThread: Exception "
                                + "while retreiving the friendly name for "
                                + "local device. Ex: " + bse);
                    }

                    // If the address we are looking for is the local device,
                    // then we do not need to do a device inquiry
                    if (localDeviceAddress != null
                            && localDeviceAddress.equalsIgnoreCase(address
                                    .trim())) {
                        friendlyName = localDevice.getFriendlyName();
                        System.out
                                .println("PushBTConnectionThread.run(): Retrieved "
                                        + "local device friendly name: "
                                        + friendlyName);
                    } else {

                        System.out
                                .println("PushBTConnectionThread.run(): Retreiving "
                                        + "friendly name for remote device "
                                        + address);
                        RemoteDevice remote = TCKAgentUtil
                                .getRemoteDevice(address);

                        try {
                            friendlyName = remote.getFriendlyName(true);
                        } catch (IOException e) {
                            System.out
                                    .println("PushBTConnectionThread: Exception "
                                            + "while retrieving the friendly name for "
                                            + "device : "
                                            + address
                                            + ". Ex: "
                                            + e);
                            // Return NOT_AVAILABLE in the friendly name to
                            // indicate error
                        }
                    }

                    // This would happen if the Bluetooth subsystem did not
                    // support this feature or if the local device could not
                    // contact the remote device
                    if (friendlyName == null) {
                        friendlyName = "RETRIEVED_NULL_OR_ERROR";
                    }

                    try {
                        System.out
                                .println("PushBTConnectionThread.run(): Returning "
                                        + "friendly name: " + friendlyName);
                        out.write(friendlyName.getBytes());
                        out.write(MSG_TERMINATOR.getBytes());
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // If no command is executed, then CLOSE connectiom
                    if (!command.equals("CLOSE")) {
                        System.out.println("PushBTConnectionThread: ERROR."
                                + "Unrecognized Command");
                    }
                }
            } // While Channel Connection Not Closed

            System.out
                    .println("PushBTConnectionThread: Closing all Connections"
                            + " to the Client");
            try {
                in.close();
                out.close();
                channel.close();
            } catch (Exception e) {
                System.out.println("PushBTConnectionThread: Error closing "
                        + "Connections: " + e);
            }
            command = "Command";
        } // while(can_run)

        System.out.println("PushBTConnectionThread: Shutting Down Service");
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                System.out.println("Error closing Server Socket : "
                        + e.getMessage());
            }
        }
    } // method run()

    long getServiceRecordHandle(String devId, String uuid) {
        long handle = -1;

        UUID uarr[] = { new UUID(uuid, false) };

        ServiceRecord records[] = TCKAgentUtil.getServiceRecords(devId, uarr);
        ServiceRecord record = null;

        if (records != null && records.length != 0) {
            record = records[0];
        }

        if (record == null) {
            System.out
                    .println("DEBUG: PushBTConnectionThread : No records found");
            handle = -1;
        } else {
            // Get the Service Record Handle (Attribute 0x0000)
            DataElement value = record.getAttributeValue(0x0000);
            try {
                if (value == null) {
                    handle = -1;
                } else {
                    handle = value.getLong();
                }
            } catch (ClassCastException ce) {
                // This should not happen
                handle = -1; // The attribute cannot be represented as a long
            }
        }

        return handle;
    }

    boolean connect(String devId, String uuid, boolean verify, String handshake)
            throws IOException {
        L2CAPConnection clientChannel = null;

        UUID uarr[] = { new UUID(uuid, false) };

        ServiceRecord records[] = TCKAgentUtil.getServiceRecords(devId, uarr);
        ServiceRecord record = null;

        if (records != null && records.length != 0) {
            record = records[0];
        }

        if (record == null) {
            System.out
                    .println("DEBUG: PushBTConnectionThread : No records found");
            return false;
        }

        String url = record.getConnectionURL(
                ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);

        System.out.println("remote url " + url);

        int bytes_read = -1;
        byte[] buffer = new byte[512];

        String msg = (handshake != null) ? handshake : "HANDSHAKE";
        String recvdMsg = "";

        String protocol = url.substring(0, url.indexOf(":"));
        if (!protocol.equals("btl2cap")) {
            return false;
        }

        try {
            clientChannel = (L2CAPConnection) Connector.open(url);
            buffer = new byte[clientChannel.getReceiveMTU()];

            System.out.println("PushBTConnectionThread: Connected "
                    + "successfully to device");

            clientChannel.send(msg.getBytes());
            clientChannel.send(MSG_TERMINATOR.getBytes());
            System.out.println("PushBTConnectionThread: Sent " + "message \""
                    + msg + "\" to the device.");

            if (verify) {
                System.out.println("PushBTConnectionThread: Waiting for "
                        + "handshake from the device ...");
                TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);

                recvdMsg = "";
                // Read the handshake message, terminated by a \n
                while (recvdMsg.indexOf('\n') == -1) {
                    timeout = 0;
                    while (!clientChannel.ready() && timeout < 10) {
                        TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
                        timeout++;
                    }

                    if (timeout < 10) {
                        bytes_read = clientChannel.receive(buffer);
                        if (bytes_read > 0) {
                            recvdMsg += new String(buffer);
                        }
                    } else {
                        System.out.println("PushBTConnectionThread: Timed out "
                                + "waiting for handshake ...");
                        break;
                    }
                }

                if (recvdMsg.indexOf('\n') != -1) {
                    return msg.trim().equals(
                            recvdMsg.substring(0, recvdMsg.indexOf('\n'))
                                    .trim());
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } catch (Exception e) {
            System.out.println("PushBTConnectionThread: Caught an exception "
                    + "while connecting to the URL: " + url);
            System.out.println("Message: " + msg);
            System.out.println("Exception : " + e);
        } finally {
            try {
                TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                clientChannel.close();
            } catch (Exception e) {
            }
        }

        return false;
    }

    boolean connect(String url, boolean verify, String handshake) {
        L2CAPConnection clientChannel = null;

        int bytes_read = -1;
        byte[] buffer = new byte[512];

        String msg = (handshake != null) ? handshake : "HANDSHAKE";
        String recvdMsg = "";

        String protocol = url.substring(0, url.indexOf(":"));
        if (!protocol.equals("btl2cap")) {
            return false;
        }
        
        try {
            clientChannel = (L2CAPConnection) Connector.open(url);
            buffer = new byte[clientChannel.getReceiveMTU()];

            System.out.println("PushBTConnectionThread: Connected "
                    + "successfully to device");

            clientChannel.send(msg.getBytes());
            clientChannel.send(MSG_TERMINATOR.getBytes());
            System.out.println("PushBTConnectionThread: Sent " + "message \""
                    + msg + "\" to the device.");

            if (verify) {
                System.out.println("PushBTConnectionThread: Waiting for "
                        + "handshake from the device ...");
                TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);

                recvdMsg = "";
                // Read the handshake message, terminated by a \n
                while (recvdMsg.indexOf('\n') == -1) {
                    timeout = 0;
                    while (!clientChannel.ready() && timeout < 10) {
                        TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
                        timeout++;
                    }

                    if (timeout < 10) {
                        bytes_read = clientChannel.receive(buffer);
                        if (bytes_read > 0) {
                            recvdMsg += new String(buffer);
                        }
                    } else {
                        System.out.println("PushBTConnectionThread: Timed out "
                                + "waiting for handshake ...");
                        break;
                    }
                }

                if (recvdMsg.indexOf('\n') != -1) {
                    return msg.trim().equals(
                            recvdMsg.substring(0, recvdMsg.indexOf('\n'))
                                    .trim());
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } catch (Exception e) {
            System.out.println("PushBTConnectionThread: Caught an exception "
                    + "while connecting to the URL: " + url);
            System.out.println("Message: " + msg);
            System.out.println("Exception : " + e);
        } finally {
            try {
                TCKAgentUtil.pause(TCKAgentUtil.SHORT);
                clientChannel.close();
            } catch (Exception e) {
            }
        }

        return false;
    }
    /**
	 * @return the configTimeout
	 */
	public int getConfigTimeout() {
		return configTimeout;
	}

	/**
	 * @param configTimeout the configTimeout to set
	 */
	private void setConfigTimeout(String customizedTimeout) {
		if ( customizedTimeout != null ) {
			configTimeout = Integer.parseInt(customizedTimeout.trim());
			System.out.println("Push Use customized timeout sets to: " + configTimeout );
		} else {
			System.out.println("Push Use default timeout: " + configTimeout);
		}
	
	}
    
} // Class PushBTConnectionThread
