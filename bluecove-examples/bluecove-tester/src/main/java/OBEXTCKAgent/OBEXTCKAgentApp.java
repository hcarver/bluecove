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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.Authenticator;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.PasswordAuthentication;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import BluetoothTCKAgent.System;

/**
 * The OBEXTCKAgent class contains the main method of the OBEX TCK Agent application.
 */
public class OBEXTCKAgentApp extends Thread {

	final String BTGOEP_AGENT_SERVICE_SRVCLASS_ID = "4000000031b811d88698000874b33fc0";

	boolean cont;

	int AUTHENTICATE_TESTED = 0;

	RequestHandlerImpl handler = null;

	MyAuthenticator auth = null;

	OBEXAgentThread agentthread = null;

	// default timeout value
	long TIME_OUT = 100;

	// the timeout set by the user. If it is not set, use the default timeout
	// value
	long timeout;

	// the transport protocol set by the user. If it is not set, "btgoep" is the
	// default
	String transport = "btgoep";

	/**
	 * Creates a <code>OBEXTCKAgentApp</code> object.
	 * 
	 * @param timeoutVal
	 *            the timeout value specified by the user
	 * @param transportVal
	 *            the transport protocol specified by the user
	 */
	public OBEXTCKAgentApp(String timeoutVal, String transportVal) {
		cont = false;
		handler = new RequestHandlerImpl(this);
		auth = new MyAuthenticator(this);
		setTimeout(timeoutVal);
		setTransport(transportVal);
	}

	/**
	 * Starts the <code>OBEXThread</code> which accepts OBEX connections.
	 * 
	 * @param args
	 *            "timeout a_value" can be specified to indicate a timeout value. If neither "timeout" is specified from
	 *            the system property nor the command line, the default value will be used.
	 */
	public static void main(String[] args) {
		// parse the timeout specified from the system property
		String timeoutVal = System.getProperty(HelperUtil.TIMEOUT);
		String transportVal = System.getProperty(HelperUtil.TRANSPORT);
		// parse the timeout specified from the arguements
		for (int i = 0; i < args.length - 1; i++) {
			System.out.println("args[" + i + "] is: " + args[i]);
			if (args[i].equals(HelperUtil.TIMEOUT)) {
				timeoutVal = args[i + 1];
			} else if (args[i].equals(HelperUtil.TRANSPORT)) {
				transportVal = args[i + 1];
			}
		}
		OBEXTCKAgentApp app = new OBEXTCKAgentApp(timeoutVal, transportVal);
		app.run();
	}

	/**
	 * Sets the timeout. If it is null, default value is used.
	 * 
	 * @param timeout
	 *            the timeout set by the user
	 */
	private void setTimeout(String value) {
		if (value == null) {
			// use default value
			System.out.println("DEFAULT TIMEOUT: " + this.TIME_OUT);
			this.timeout = this.TIME_OUT;
		} else {
			System.out.println("TIMEOUT value set by user: " + value);
			// use the value set by the user
			this.timeout = Long.parseLong(value.trim());
		}
		System.out.println("TIMEOUT: " + timeout);
	}

	/**
	 * Sets the transport protocol configured by the user. If it is null or not one of the supported OBEX transport
	 * protocols, the default transport protocol btgoep is used.
	 * 
	 * @param value
	 *            the transport protocol configured by the user
	 */
	private void setTransport(String value) {
		if (value == null) {
			// the default transport is btgoep
			return;
		}
		if (value.equals(HelperUtil.BT) || value.equals(HelperUtil.TCP) || value.equals(HelperUtil.IRDA)) {
			this.transport = value;
		} else {
			System.out.println("Unsupported transport protocol: " + value + ". Default transport " + this.transport
					+ " protocol is used.");
		}
	}

	public void run() {
		boolean canRun = true;
		SessionNotifier server = null;
		Connection session = null;
		OBEXTCKAgentApp agent = new OBEXTCKAgentApp(String.valueOf(this.timeout), this.transport);

		try {
			if (transport.equals(HelperUtil.BT)) {
				HelperUtil.initialize();
				server = (SessionNotifier) Connector.open("btgoep://localhost:" + BTGOEP_AGENT_SERVICE_SRVCLASS_ID);
			} else if (transport.equals(HelperUtil.TCP)) {

				// port number 650 is opened by default
				server = (SessionNotifier) Connector.open("tcpobex://");
			} else if (transport.equals(HelperUtil.IRDA)) {
				server = (SessionNotifier) Connector.open("irdaobex://localhost");
			}
		} catch (Exception e) {
			System.out.println("Exception Occured with " + "OBEX Connector.open :" + e);
			canRun = false;

			return;
		}

		System.out.println("OBEX TCK Agent Started");
		while (canRun) {
			agent = new OBEXTCKAgentApp(String.valueOf(this.timeout), this.transport);

			try {
				session = server.acceptAndOpen(agent.handler, agent.auth);
			} catch (InterruptedIOException e) {
				System.out.println("OBEX TCK Agent Interrupted");
				return;
			} catch (Exception e) {
				System.out.println("Exception: " + e.getClass().getName() + " " + e.getMessage());
				if ("Stack closed".equals(e.getMessage())) {
					return;
				}
				try {
					Thread.sleep(timeout * 10);
				} catch (Exception ex) {
					System.out.println("Problem Closing Server : " + ex.getMessage());
				}
			}

			if (canRun) {
				agent.agentthread = new OBEXAgentThread(agent);
				agent.agentthread.start();
				agent.cont = false;

				while (!agent.cont) {
					try {
						Thread.sleep(timeout);
					} catch (Exception threadex) {
						System.out.println("Exception occured : " + threadex.getMessage());
					}
				}

				try {
					Thread.sleep(timeout * 10);
				} catch (Exception threadex) {
					System.out.println("Exception occured : " + threadex.getMessage());
				}

				try {
					session.close();
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}

			}
		}
	}

	/**
	 * This class is used to test authentication procedures.
	 */
	class MyAuthenticator implements Authenticator {

		OBEXTCKAgentApp parent;

		/**
		 * Creates a <code>MyAuthenticator</code> object with the parent thread specified.
		 * 
		 * @param p
		 *            the parent of this authenticator thread
		 */
		public MyAuthenticator(OBEXTCKAgentApp p) {
			parent = p;
		}

		/**
		 * Called each time the server receives an authentication challenge header. It returns with a null user ID and
		 * the password of "Password1".
		 * 
		 * @param description
		 *            ignored
		 * 
		 * @param isUserIdRequired
		 *            ignored
		 * 
		 * @param isFullAccess
		 *            ignored
		 * 
		 * @return a <code>PasswordAuthentication</code> object with a <code>null</code> user ID and a password of
		 *         "Password1"
		 */
		public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired,
				boolean isFullAccess) {
			System.out.println("<<<<onAuthenticationChallenge called description: " + description + " and flags "
					+ isUserIdRequired + isFullAccess);

			if (isUserIdRequired) {
				return new PasswordAuthentication(new String("UserName").getBytes(), new String("Password").getBytes());
			} else {
				return new PasswordAuthentication(null, new String("Password").getBytes());
			}
		}

		/**
		 * Called each time the server receives an authentication response header. It tracks that the method was called
		 * and returns with the "Password" password.
		 * 
		 * @userName ignored
		 * 
		 * @return the password of "Password"
		 */
		public byte[] onAuthenticationResponse(byte[] userName) {
			System.out.println(">>>>>onAuthenticationResponse is called and userName: " + new String(userName));
			parent.AUTHENTICATE_TESTED++;
			return (new String("Password")).getBytes();
		}
	}

	/**
	 * This class processes OBEX requests made to the OBEX TCK Agent.
	 */
	class RequestHandlerImpl extends ServerRequestHandler {
		int testNum;

		OBEXTCKAgentApp parent;

		/**
		 * Creates a <code>RequestHandlerImpl</code> with the parent specified.
		 * 
		 * @param p
		 *            the parent to this ServerRequestHandler
		 */
		public RequestHandlerImpl(OBEXTCKAgentApp p) {
			super();
			parent = p;
		}

		/**
		 * Called when the client issues a SETPATH request.
		 * 
		 * @param request
		 *            the headers received in the request
		 * 
		 * @param reply
		 *            the headers to send in the reply
		 * 
		 * @param backup
		 *            <code>true</code> if the "back up" flag is set; otherwise <code>false</code>
		 * 
		 * @param create
		 *            <code>true</code> if the "create" flag is set; otherwise <code>false</code>
		 * 
		 * @return the response code to send to the client
		 */
		public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {

			String name = null;

			try {
				name = (String) request.getHeader(HeaderSet.NAME);
			} catch (IOException e) {
			}
			parent.agentthread.resetAgent();

			if (name == null) {
				System.out.println("setPath() returns OK with name = null");
				return ResponseCodes.OBEX_HTTP_OK;
			}

			if (name.equals("NOTEST")) {
				System.out.println("setPath() returns OK with name = NOTESTS");
				return ResponseCodes.OBEX_HTTP_OK;
			}

			if (name.equals("LENGTH")) {
				try {
					String description = (String) request.getHeader(HeaderSet.DESCRIPTION);
					if (description == null) {
						return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
					}
					reply.setHeader(HeaderSet.LENGTH, new Long(description.length()));
					return ResponseCodes.OBEX_HTTP_OK;
				} catch (IOException e) {
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}

			if (name.equals("TEST_BACKUP=yes_CREATE=yes")) {
				if (backup && create) {
					return ResponseCodes.OBEX_HTTP_OK;
				}
			}

			if (name.equals("TEST_BACKUP=no_CREATE=yes")) {
				if (!backup && create) {
					return ResponseCodes.OBEX_HTTP_OK;
				}
			}

			if (name.equals("TEST_BACKUP=yes_CREATE=no")) {
				if (backup && !create) {
					return ResponseCodes.OBEX_HTTP_OK;
				}
			}

			if (name.equals("TEST_BACKUP=no_CREATE=no")) {
				if (!backup && !create) {
					return ResponseCodes.OBEX_HTTP_OK;
				}
			}

			if ((name.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 0)) {

				try {
					reply.createAuthenticationChallenge("Server Auth Test", true, true);
				} catch (Exception ex) {
					System.out.println("Exception Occured " + ex);
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
				parent.agentthread.resetAgent();
				return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
			} else if ((name.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 1)) {
				return ResponseCodes.OBEX_HTTP_OK;
			} else if ((name.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED > 1)) {
				System.out.println("setPath() returns OBEX_HTTP_INTERNAL_ERROR with AUTHENTICATE_TESTED: "
						+ AUTHENTICATE_TESTED);
				return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}

			return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
		}

		/**
		 * Called when the client issues a DISCONNECT request.
		 * 
		 * @param request
		 *            the headers received in the request
		 * 
		 * @param reply
		 *            the headers to send in the reply
		 * 
		 * @return the response code to send to the client
		 */
		public void onDisconnect(HeaderSet headers, HeaderSet reply) {
			String testName = null;

			try {
				testName = (String) headers.getHeader(HeaderSet.NAME);
			} catch (IOException e) {
			}

			if (testName != null) {
				if (testName.equals("HEADER-RESPONSE")) {
					reply.setHeader(HeaderSet.DESCRIPTION, "Received Request");
				}
			}

			parent.agentthread.resetAgent();
			parent.AUTHENTICATE_TESTED = 0;

			return;
		}

		/**
		 * Called when the client issues a CONNECT request. Tests the headers and sends the proper response to the
		 * server.
		 * 
		 * @param request
		 *            the headers received in the request
		 * 
		 * @param reply
		 *            the headers to send in the reply
		 * 
		 * @return the response code to send to the client
		 */
		public int onConnect(HeaderSet headers, HeaderSet reply) {
			String testName = null;

			while ((parent.agentthread == null) || (!parent.agentthread.isAlive())) {
				try {
					Thread.sleep(parent.timeout * 5);
				} catch (Exception e) {
				}
			}

			parent.agentthread.resetAgent();

			try {
				testName = (String) headers.getHeader(HeaderSet.NAME);
			} catch (IOException e) {
				System.out.println("Exception Occured " + e.getMessage());
			}

			if (testName == null) {
				return ResponseCodes.OBEX_HTTP_OK;
			}

			if (testName.startsWith("CLIENT ")) {
				String url = testName.substring(testName.indexOf(" "));
				OBEXClientThread clientthread = new OBEXClientThread(url, parent.timeout);
				clientthread.start();

				return ResponseCodes.OBEX_HTTP_OK;
			}

			if (testName.equals("BADTEST")) {
				return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}

			if ((testName.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 0)) {
				try {
					reply.createAuthenticationChallenge("Server Auth Test", true, true);

				} catch (Exception ex) {
					System.out.println("Exception Occured " + ex.getMessage());
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
				parent.agentthread.resetAgent();
				return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
			} else if ((testName.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 1)) {
				return ResponseCodes.OBEX_HTTP_OK;
			} else if ((testName.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED > 1)) {
				System.out
						.println("onConnect() TESTAUTHENTICATE returns OBEX_HTTP_INTERNAL_ERROR with AUTHENTICATE_TESTED: "
								+ AUTHENTICATE_TESTED);
				return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}

			if (testName.equals("RESPONSECODE")) {

				try {
					Long resp = (Long) headers.getHeader(HeaderSet.LENGTH);
					int responsecode = (int) resp.longValue();
					return responsecode;
				} catch (Exception e) {
					System.out.println("ERROR: Exception " + e.getMessage());
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}

			return ResponseCodes.OBEX_HTTP_OK;

		}

		/**
		 * Called when the client issues a GET request.
		 * 
		 * @param tran
		 *            the <code>Operation</code> object to use with this request
		 * 
		 * @return the response code to send in the final reply
		 */
		public int onGet(Operation tran) {
			String testName = null;
			int responsecode = ResponseCodes.OBEX_HTTP_OK;
			HeaderSet hSet = null;

			parent.agentthread.resetAgent();

			try {
				hSet = tran.getReceivedHeaders();
				testName = (String) hSet.getHeader(HeaderSet.NAME);
			} catch (IOException e) {
				System.out.println("Exception Occured " + e.getMessage());
				responsecode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}

			if (testName == null) {
				testName = "INVALID COMMAND";
			}

			if (testName.equals("ECHO")) {
				try {
					byte[] message = ((String) hSet.getHeader(HeaderSet.DESCRIPTION)).getBytes();

					OutputStream os = tran.openOutputStream();

					for (int i = 0; i < message.length; i++) {
						os.write(message[i]);
					}

					os.flush();

					os.close();

				} catch (IOException e) {
					System.out.println("IO Exception Occured: " + e);
					responsecode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				} catch (Exception e) {
					System.out.println("General Exception Occured: " + e);
					responsecode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}

			if (testName.equals("RESPONSECODE")) {
				try {
					Long resp = (Long) hSet.getHeader(HeaderSet.LENGTH);
					responsecode = (int) resp.longValue();
				} catch (Exception e) {
					System.out.println("ERROR: Exception " + e.getMessage());
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}

			if (testName.equals("READ")) {
				try {
					Long resp = (Long) hSet.getHeader(HeaderSet.LENGTH);
					InputStream in = tran.openInputStream();

					for (long i = 0; i < resp.longValue(); i++) {
						int value = in.read();
						parent.agentthread.resetAgent();
						if ((byte) value != (byte) i) {
							responsecode = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
							break;
						}
					}

					in.close();
				} catch (Exception e) {
					System.out.println("ERROR: Exception " + e.getMessage());
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}
			// Challenge the client
			if ((testName.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 0)) {

				try {
					// server request client to authenticate itself
					HeaderSet headerSet = createHeaderSet();
					headerSet.createAuthenticationChallenge("Server Auth Test", true, true);
					tran.sendHeaders(headerSet);
				} catch (Exception ex) {
					System.out.println("Exception Occured " + ex.getMessage());
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
				parent.agentthread.resetAgent();
				return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
			} else if ((testName.equals("TESTAUTHENTICATE")) && AUTHENTICATE_TESTED == 1) {
				return ResponseCodes.OBEX_HTTP_OK;
			} else if ((testName.equals("TESTAUTHENTICATE")) && AUTHENTICATE_TESTED > 1) {
				System.out
						.println("onGet() TESTAUTHENTICATE returns OBEX_HTTP_INTERNAL_ERROR with AUTHENTICATE_TESTED: "
								+ AUTHENTICATE_TESTED);
				return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}

			if (testName.equals("WRITE")) {

				try {
					int length = (int) ((Long) hSet.getHeader(HeaderSet.LENGTH)).longValue();
					System.out.println("Client requests server to send back data of size: " + length);

					String input = (String) hSet.getHeader(HeaderSet.DESCRIPTION);

					System.out.println("input: " + input);

					// Send data to client
					OutputStream os = tran.openOutputStream();
					int data = Integer.parseInt(input);
					System.out.println("Writing data to client with value " + data);

					for (int i = 0; i < length; i++) {
						parent.agentthread.resetAgent();
						os.write(data);
					}

					os.flush();
					os.close();

				} catch (IOException e) {
					System.out.println("IO Exception Occured: " + e);
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				} catch (Exception e) {
					System.out.println("General Exception Occured: " + e);
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}

			try {
				tran.close();
			} catch (Exception e) {
				System.out.println("ERROR: Exception " + e.getMessage());
				responsecode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
			}

			return responsecode;
		}

		/**
		 * Called when the client issues a PUT request.
		 * 
		 * @param tran
		 *            the <code>Operation</code> object to use with this request
		 * 
		 * @return the response code to send in the final reply
		 */
		public int onPut(Operation tran) {
			String testName = null;
			String desc = null;
			HeaderSet hSet = null;

			parent.agentthread.resetAgent();
			try {
				hSet = tran.getReceivedHeaders();
				testName = (String) hSet.getHeader(HeaderSet.NAME);
			} catch (IOException e) {
				System.out.println("Exception Occured " + e.getMessage());
			}

			if (testName == null) {
				testName = "INVALID COMMAND";
			}

			try {
				if (testName.equals("TEST_LENGTH_TYPE")) {
					try {
						desc = (String) hSet.getHeader(HeaderSet.DESCRIPTION);

						hSet = createHeaderSet();
						hSet.setHeader(HeaderSet.TYPE, desc);
						hSet.setHeader(HeaderSet.LENGTH, new Long(desc.length()));
						tran.sendHeaders(hSet);
					} catch (Exception e) {
						System.out.println("IO Exception Occured");
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
				}

				if (testName.equals("READ")) {
					try {
						int length = (int) ((Long) hSet.getHeader(HeaderSet.LENGTH)).longValue();

						InputStream is = tran.openInputStream();

						int ch = 0;
						while ((ch = is.read()) < 0) {
							try {
								Thread.sleep(parent.timeout);
							} catch (Exception threadex) {
								System.out.println("Exception while sleeping");
							}
						}

						for (int i = 0; i < length - 1; i++) {
							agentthread.resetAgent();
							ch = is.read();

							if (ch != -1 && ((byte) ch != (byte) (i + 1))) {
								System.out.println("Incorrect byte received");
								System.out.println(ch);
								System.out.println(i);
								return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
							}
						}
						is.close();
					} catch (IOException e) {
						System.out.println("IO Exception Occured");
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					} catch (Exception e) {
						System.out.println("General Exception Occured");
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
				}

				if (testName.equals("RESPONSECODE")) {
					try {
						Long resp = (Long) hSet.getHeader(HeaderSet.LENGTH);
						return (int) resp.longValue();

					} catch (Exception e) {
						System.out.println("ERROR: Exception " + e.getMessage());
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
				}

				if (testName.equals("ECHO")) {
					try {
						byte[] message = ((String) hSet.getHeader(HeaderSet.DESCRIPTION)).getBytes();
						System.out.println("onPut() got message from header: " + new String(message));
						OutputStream os = tran.openOutputStream();

						for (int i = 0; i < message.length; i++) {
							os.write(message[i]);
						}

						os.flush();

						os.close();

					} catch (IOException e) {
						System.out.println("IO Exception Occured");
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					} catch (Exception e) {
						System.out.println("General Exception Occured");
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
				}
				// read the data sent by the client and sends back the data read
				// to client
				if (testName.equals("WRITE")) {

					try {
						int length = (int) ((Long) hSet.getHeader(HeaderSet.LENGTH)).longValue();
						System.out.println("length: " + length);
						String input = (String) hSet.getHeader(HeaderSet.DESCRIPTION);
						int receivedData = Integer.parseInt(input);
						System.out.println("receivedData: " + receivedData);

						InputStream is = tran.openInputStream();

						// construct the data to be sent back to the client
						byte[] sendData = new byte[length];

						for (int i = 0; i < sendData.length; i++) {
							sendData[i] = (byte) receivedData;

						}
						int ch = 0;

						for (int i = 0; i < length; i++) {
							parent.agentthread.resetAgent();
							ch = is.read();
							if (ch != -1 && ch != receivedData) {
								System.out.println("****Incorrect byte received for i: " + i);
								System.out.println(ch);
								System.out
										.println("Did not receive the expected data sent by client. Return OBEX_HTTP_BAD_REQUEST");
								return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
							}
						}

						// Send data to client
						OutputStream os = tran.openOutputStream();
						for (int i = 0; i < sendData.length; i++) {
							parent.agentthread.resetAgent();
							os.write(sendData[i]);
						}

						os.flush();
						is.close();
						os.close();

					} catch (IOException e) {
						System.out.println("IO Exception Occured: " + e);
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					} catch (Exception e) {
						System.out.println("General Exception Occured: " + e);
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
				}
				// Challenge the client
				if ((testName.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 0)) {

					try {
						// server request client to authenticate itself
						HeaderSet headerSet = createHeaderSet();
						headerSet.createAuthenticationChallenge("Server Auth Test", true, true);
						tran.sendHeaders(headerSet);

					} catch (Exception ex) {
						System.out.println("Exception Occured " + ex.getMessage());
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
					parent.agentthread.resetAgent();
					return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
				} else if ((testName.equals("TESTAUTHENTICATE")) && AUTHENTICATE_TESTED == 1) {
					return ResponseCodes.OBEX_HTTP_OK;
				} else if ((testName.equals("TESTAUTHENTICATE")) && AUTHENTICATE_TESTED > 1) {
					System.out
							.println("onPut() TESTAUTHENTICATE returns OBEX_HTTP_INTERNAL_ERROR with AUTHENTICATE_TESTED: "
									+ AUTHENTICATE_TESTED);
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}

			} finally {
				try {
					if (tran != null) {
						tran.close();
					}
				} catch (IOException e) {
					System.out.println("ERROR: Exception " + e);
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}

			return ResponseCodes.OBEX_HTTP_OK;
		}

		/**
		 * Called when the client issues a DELETE request.
		 * 
		 * @param request
		 *            the headers received in the request
		 * 
		 * @param reply
		 *            the headers to send in the reply
		 * 
		 * @return the response code to send to the client
		 */
		public int onDelete(HeaderSet request, HeaderSet reply) {
			String name = null;

			try {
				name = (String) request.getHeader(HeaderSet.NAME);
			} catch (IOException e) {
			}

			if (name != null) {
				if (name.equals("NAME")) {
					reply.setHeader(HeaderSet.NAME, "DELETE");
				}

				if (name.equals("LENGTH")) {
					try {
						String description = (String) request.getHeader(HeaderSet.DESCRIPTION);
						if (description == null) {
							return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
						}
						reply.setHeader(HeaderSet.LENGTH, new Long(description.length()));
						return ResponseCodes.OBEX_HTTP_OK;
					} catch (IOException e) {
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
				}
				if ((name.equals("TESTAUTHENTICATE")) && (AUTHENTICATE_TESTED == 0)) {

					try {
						reply.createAuthenticationChallenge("Server Auth Test", true, true);

					} catch (Exception ex) {
						System.out.println("Exception Occured " + ex.getMessage());
						return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
					}
					parent.agentthread.resetAgent();
					return ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
				} else if ((name.equals("TESTAUTHENTICATE")) && AUTHENTICATE_TESTED == 1) {
					return ResponseCodes.OBEX_HTTP_OK;
				} else if ((name.equals("TESTAUTHENTICATE")) && AUTHENTICATE_TESTED > 1) {
					System.out
							.println("onDelete() TESTAUTHENTICATE returns OBEX_HTTP_INTERNAL_ERROR with AUTHENTICATE_TESTED: "
									+ AUTHENTICATE_TESTED);
					return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
				}
			}
			return ResponseCodes.OBEX_HTTP_OK;
		}
	}
}
