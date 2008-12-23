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

import java.io.InterruptedIOException;
import javax.bluetooth.DataElement;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;
import BluetoothTCKAgent.Connector;

public class L2CAPThread extends Thread {

	/**
	 * Specifies via the configuration the connection string parameter
	 * receiveMTU and transmitMTU for the L2CAP service that the implementation
	 * supports. The value assigned here must equal the value assignment to the
	 * parameter "bluetooth.agent_mtu" configured on the client side in the
	 * global.xml file.
	 */
	static final String BLUETOOTH_AGENT_MTU = "bluetooth.agent_mtu";

	static final String TIMEOUT = "timeout";

	/**
	 * This variable refers to the value the parameters ReceiveMTU and
	 * TransmitMTU have in the L2CAP service that this thread starts up. The
	 * default value is 512 if it is not overriden by the user via
	 * "bluetooth.agent_mtu" when this thread is started.
	 */
	private int agentMtu = 512;

	/**
	 * Default timeout value if not set by the user
	 */
	private int configTimeout = TCKAgentUtil.SHORT;

	public static String message;

	private String data = "data", command = "Command";

	private L2CAPConnectionNotifier server = null;

	private L2CAPConnection channel = null, helperchannel = null;

	int buffersize = 680, counter = 1, bytes_read = 0, timeout = 0;

	byte[] buffer;

	private boolean can_run = true;

	private String connString;

	private LocalDevice localdevice;

	public L2CAPThread(String str, final String mtu, final String customTimeout) {
		super(str);
		this.setAgentMtu(mtu);
		this.setConfigTimeout(customTimeout);
		try {
			System.out.println("L2CAPThread: Starting L2CAP Service using agentMtu: " + agentMtu + " and timeout: " + configTimeout);
			connString = "btl2cap://localhost:"
					+ "3B9FA89520078C303355AAA694238F07;" + "ReceiveMTU="
					+ agentMtu + ";TransmitMTU=" + agentMtu;
			server = (L2CAPConnectionNotifier) Connector.open(connString);
			
			//Adding an attribute value to the service record with a DataElement
			//of type DataElement.URL 
			try{
				localdevice = LocalDevice.getLocalDevice();
				System.out.println("Local Device bluetooth address is = "
						+ localdevice.getBluetoothAddress());						 
				ServiceRecord srv_record = 
							localdevice.getRecord(server);					
				DataElement docURL = new DataElement(DataElement.URL,
										"http://www.motorola.com/");
				srv_record.setAttributeValue(0x000A, docURL);
				
				
				localdevice.updateRecord(srv_record);	
				DataElement tmp = srv_record.getAttributeValue(0x000A);
				System.out.println("The URL attribute added is = " + 
						(String)tmp.getValue());
				
			}catch (Exception e){
				System.out.println("Error updating the service record in " +
						"the local device running the agent. " +
						"WARNING: Tests trying to retreive service record " +
						" with an attribute value of dataelement type " +
						"DataElement.URL will fail.");						
			}//attribute value added to the service record				
			
		} catch (Exception e) {
			System.out.println("L2CAPThread: Error starting L2CAP"
					+ "service. Aborting service.");
			can_run = false;
		}
	}

	public void run() {
		while (can_run) {
			try {
				System.out.println("L2CAPThread: "
						+ "Waiting for Client to Connect");
				channel = server.acceptAndOpen();
            } catch (InterruptedIOException e) {
            	System.out.println("L2CAPThread:TCK Interrupted");
            	return;
			} catch (Exception e) {
				System.out.println("L2CAPThread: Error connecting to "
						+ "client. Aborting connection.");
				can_run = false;
                if ("Stack closed".equals(e.getMessage())) {
                	return;
                }
			} finally {
				if (!can_run) {
					command = "CLOSE";
				} else {
					System.out.println("L2CAPThread: Client made a "
							+ "connection");
				}
			}

			can_run = true;
			while (!command.equals("CLOSE")) {
				buffer = new byte[buffersize];
				System.out.println("L2CAPThread: Reading "
						+ "L2CAPConnection Stream");
				try {
					/*
					 * Keep reading until data comes in
					 */
					timeout = 0;
					while (!channel.ready() && timeout < 10) {
						TCKAgentUtil.pause(1000);
						timeout++;
					}

					if (timeout < 10) {
						bytes_read = channel.receive(buffer);
						System.out
								.println("L2CAPThread.run(): Channel ReceiveMTU: "
										+ channel.getReceiveMTU());
						System.out.println("L2CAPThread.run(): Bytes Read: "
								+ bytes_read);
					}
				} catch (Exception e) {
					System.out.println("L2CAPThread: Failure while "
							+ "reading L2CAPConnection.");
					timeout = 10;
				}

				if (timeout == 10) {
					System.out.println("L2CAPThread: Client Connection "
							+ "Timed Out. Closing connection");
					message = "CLOSE connection";
					buffer = message.getBytes();
				}

				// allow sending the empty message. Therefore, do not trim the
				// message as doing so will not be able to parse out the
				// command
				message = (new String(buffer));
				System.out.println("L2CAPThread.run(): Message \"" + message
						+ "\" and message size: " + message.length());
				int space = message.indexOf(" ");
				if ( space != -1 ) {
				command = message.substring(0, space);
				data = message.substring(space + 1);
					System.out.println("data: " + data + " data len: " + data.length());
				// since the buffer was allocated with size more than the
				// client message sent
				// trim it to get rid of access before sending to client
				data = data.trim();
				System.out.println(" data size after trim: "
						+ data.length() + " data: " + data);
				}
			
				if (command.equals("ECHO")) {
					System.out.println("L2CAPThread: ECHO Command Called");
					TCKAgentUtil.pause(TCKAgentUtil.SHORT);

					try {
						channel.send(data.getBytes());
					} catch (Exception e) {
						System.out.println("L2CAPThread: Error writing "
								+ "to client. Closing connection");
						command = "CLOSE";
					}
				} // ECHO Command

				else if (command.equals("READ")) {
					System.out.println("L2CAPThread: READ " + "Command Called");
				} // READ Command

				else if (command.equals("LOG")) {
					System.out.println("L2CAPThread LOG: " + data);
				} // LOG Command

				else if (command.equals("WAIT")) {
					data = data.trim();
					try {
						System.out.println("L2CAPThread: WAIT "
								+ "Command Called");
						int timetowait = Integer.parseInt(data);
						L2CAPThread.sleep(timetowait);
					} catch (Exception e) {
					}
				} // WAIT Command

				else if (command.equals("CLIENT")) {
					System.out.println("L2CAPThread: CLIENT Command Called");

					command = "CLOSE";
					try {
						TCKAgentUtil.pause(TCKAgentUtil.SHORT);
						channel.close();
						TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
					} catch (Exception e) {
						System.out.println("L2CAPThread: Error closing"
								+ " existing connection.");
					}

					data = "btl2cap://" + data
					+ ";authenticate=false;encrypt=false;master=false"
					+ ";ReceiveMTU=" + agentMtu + ";TransmitMTU=" + agentMtu;
					try {
						helperchannel = (L2CAPConnection) Connector.open(data);
						System.out.println("L2CAPThread: Connected "
								+ "successfully to client");
					} catch (Exception e) {
						System.out.println("L2CAPThread: Unable to "
								+ "connect to the client "
								+ "the following connection" + " string: "
								+ data + " with error: " + e);
					}

					try {
						TCKAgentUtil.pause(TCKAgentUtil.SHORT);
						helperchannel.close();
					} catch (Exception e) {
					}
				} // CLIENT Command

				else if (command.equals("GETSDCLASS")) {
					System.out.println("L2CAPThread: GETSDCLASS "
							+ "Command Called");

					int sdClass = -1;
					String msg = null, btAddress;

					btAddress = data.substring(0, 12);
					command = "CLOSE";

					try {
						channel.close();
						//https://opensource.motorola.com/sf/discussion/do/listPosts/projects.jsr82/discussion.google_jsr_82_support.topc1845
						//TCKAgentUtil.pause(TCKAgentUtil.SHORT);
						TCKAgentUtil.pause(TCKAgentUtil.MEDIUM);
					} catch (Exception e) {
						System.out.println("L2CAPThread: Error closing"
								+ " existing connection.");
					}

					sdClass = TCKAgentUtil.getServiceClass(btAddress, this.configTimeout);
					System.out.println("L2CAPThread: Retrieved the service "
							+ "classes for " + btAddress + " : "
							+ Integer.toString(sdClass, 16));

					if (sdClass == -1) {
						System.out.println("L2CAPThread: Unable to "
								+ "retrieve ServiceDevice"
								+ "Class of the client" + " device: "
								+ btAddress);
					}

					msg = Integer.toString(sdClass);

					data = "btl2cap://" + data
					+ ";authenticate=false;encrypt=false;master=false"
					+ ";ReceiveMTU=" + agentMtu + ";TransmitMTU=" + agentMtu;
					try {
						helperchannel = (L2CAPConnection) Connector.open(data);
						helperchannel.send(msg.getBytes());
						TCKAgentUtil.pause(TCKAgentUtil.SHORT);
						helperchannel.close();
					} catch (Exception e) {
						System.out.println("L2CAPThread: Unable to "
								+ "send data with the " + "connection string: "
								+ data);
					}
				} // GETSDCLASS Command
				else if (command.equals("GETSRHANDLE")) {
					System.out.println("L2CAPThread: GETSRHANDLE "
							+ "Command Called");
					System.out.println("L2CAPThread: WITH DATA " + data);
					long recordHandle = -1;
					String msg = null, btAddress;

					space = data.indexOf(' ');
					if (space == -1) {
						System.out.println("L2CAPThread: Missing UUID");
					}

					String url = data.substring(0, space);
					System.out
							.println("L2CAPThread.run(): URL \"" + url + "\"");

					String uuid = data.substring(space + 1);
					System.out.println("L2CAPThread.run(): UUID \"" + uuid
							+ "\"");

					btAddress = url.substring(0, 12);
					System.out.println("L2CAPThread.run(): BTADDRESS: \""
							+ btAddress + "\"");
					command = "CLOSE";

					try {
						channel.close();
						TCKAgentUtil.pause(TCKAgentUtil.SHORT);
					} catch (Exception e) {
						System.out.println("L2CAPThread: Error closing"
								+ " existing connection.");
					}

					UUID uuids[] = { new UUID(uuid, false) };
					System.out.println("L2CAPThread: Searching for services "
							+ "with uuid \"" + uuid + "\" on device "
							+ btAddress);
					ServiceRecord records[] = TCKAgentUtil.getServiceRecords(
							btAddress, uuids);

					if (records == null || records.length == 0) {
						System.out
								.println("L2CAPThread: Unable to retreive "
										+ "Service records for the service with uuid \""
										+ uuid + "\" ,running on device \""
										+ btAddress + "\" .");
					} else {
						System.out.println("L2CAPThread: Retreived a service "
								+ "record");
						ServiceRecord record = records[0];
						DataElement elem = record.getAttributeValue(0x0000);
						if (elem == null) {
							System.out.println("L2CAPThread: Missing Record "
									+ "handle for service record.");
						} else {
							recordHandle = elem.getLong();
						}
					}

					msg = Long.toString(recordHandle);
					System.out.println("L2CAPThread.run(): Retreived handle "
							+ recordHandle);

					url = "btl2cap://" + url
					+ ";authenticate=false;encrypt=false;master=false"
					+ ";ReceiveMTU=" + agentMtu + ";TransmitMTU=" + agentMtu;
					try {
						helperchannel = (L2CAPConnection) Connector.open(url);
						System.out.println("L2CAPThread.run(): Sending msg "
								+ "\"" + msg + "\"");
						helperchannel.send(msg.getBytes());
						TCKAgentUtil.pause(TCKAgentUtil.SHORT);
						helperchannel.close();
					} catch (Exception e) {
						System.out.println("L2CAPThread: Unable to "
								+ "send data with the connection string: "
								+ url + ". Exception: " + e);
					}
				} // GETSRHANDLE command
				else { // If no command is executed, then CLOSE connection
					if (!command.equals("CLOSE")) {
						System.out.println("L2CAPThread: Unrecognized Command");
					}
				}
			} // While Channel Connection Not Closed

			System.out.println("Closing Channel");

			if (channel != null) {
				try {
					channel.close();
				} catch (Exception e) {
				}
			}

			command = "Command";
			System.out.println("L2CAPThread: Connection Closed By Client");
		} // While true
	} // method run()



	/**
	 * @return the agentMtu
	 */
	public int getAgentMtu() {
		return agentMtu;
	}

	/**
	 * @param customizedMTU the agentMtu to set
	 */
	private void setAgentMtu(String customizedMTU) {
		if ( customizedMTU != null ) {
			this.agentMtu = Integer.parseInt(customizedMTU.trim());
			System.out.println("Use customized agentMtu sets to: " + this.agentMtu );
		} else {
			System.out.println("Use default agent MTU: " + this.agentMtu);
		}
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
			System.out.println("Use customized timeout sets to: " + configTimeout );
		} else {
			System.out.println("Use default timeout: " + configTimeout);
		}
	
	}

} // class L2CAPThread
