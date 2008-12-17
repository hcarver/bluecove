/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package net.sf.bluecove;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.bluetooth.DataElement;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;
import org.bluecove.tester.util.RuntimeDetect;
import org.bluecove.tester.util.StringUtils;
import org.bluecove.tester.util.TimeUtils;

import net.sf.bluecove.util.BluetoothTypesInfo;

public class TestResponderServerOBEX implements Runnable {

	private SessionNotifier serverConnection;

	private boolean isStoped = false;

	private boolean isRunning = false;

	private Thread thread;

	private Object threadLocalBluetoothStack;

	public static final UUID OBEX_OBJECT_PUSH = new UUID(0x1105);

	public static final String OBEX_OBJECT_PUSH_SERVER_NAME = "OBEX Object Push";

	private TestResponderServerOBEX() {

	}

	public static TestResponderServerOBEX startServer() {
		TestResponderServerOBEX srv = new TestResponderServerOBEX();
		srv.threadLocalBluetoothStack = Configuration.threadLocalBluetoothStack;
		srv.thread = RuntimeDetect.cldcStub.createNamedThread(srv, "ServerOBEX");
		srv.thread.start();
		return srv;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void run() {
		isStoped = false;
		boolean deviceServiceClassesUpdated = false;
		RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);
		LocalDevice localDevice;
		try {
			localDevice = LocalDevice.getLocalDevice();
			if (Configuration.testServerOBEX_TCP.booleanValue()) {
				serverConnection = (SessionNotifier) Connector
						.open(BluetoothTypesInfo.PROTOCOL_SCHEME_TCP_OBEX + "://");
			} else {
				StringBuffer url = new StringBuffer(BluetoothTypesInfo.PROTOCOL_SCHEME_BT_OBEX);
				url.append("://localhost:");
				if (Configuration.testServerOBEXObjectPush) {
					url.append(OBEX_OBJECT_PUSH);
					url.append(";name=");
					url.append(OBEX_OBJECT_PUSH_SERVER_NAME);
				} else {
					url.append(Configuration.blueCoveOBEXUUID());
					url.append(";name=");
					url.append(Consts.RESPONDER_SERVERNAME);
					url.append("_ox");
				}

				url.append(Configuration.serverURLParams());

				serverConnection = (SessionNotifier) Connector.open(url.toString());
				if (Configuration.testServerOBEXObjectPush) {
					try {
						ServiceRecord record = localDevice.getRecord(serverConnection);

						final int OBJECT_TRANSFER_SERVICE = 0x100000;

						try {
							record.setDeviceServiceClasses(OBJECT_TRANSFER_SERVICE);
						} catch (Throwable e) {
							Logger.debug("setDeviceServiceClasses", e);
						}

						DataElement bluetoothProfileDescriptorList = new DataElement(DataElement.DATSEQ);
						DataElement obbexPushProfileDescriptor = new DataElement(DataElement.DATSEQ);
						obbexPushProfileDescriptor.addElement(new DataElement(DataElement.UUID, OBEX_OBJECT_PUSH));
						obbexPushProfileDescriptor.addElement(new DataElement(DataElement.U_INT_2, 0x100));
						bluetoothProfileDescriptorList.addElement(obbexPushProfileDescriptor);
						record.setAttributeValue(0x0009, bluetoothProfileDescriptorList);

						final short ATTR_SUPPORTED_FORMAT_LIST_LIST = 0x0303;
						DataElement supportedFormatList = new DataElement(DataElement.DATSEQ);
						// any type of object.
						supportedFormatList.addElement(new DataElement(DataElement.U_INT_1, 0xFF));
						record.setAttributeValue(ATTR_SUPPORTED_FORMAT_LIST_LIST, supportedFormatList);

						final short UUID_PUBLICBROWSE_GROUP = 0x1002;
						final short ATTR_BROWSE_GRP_LIST = 0x0005;
						DataElement browseClassIDList = new DataElement(DataElement.DATSEQ);
						UUID browseClassUUID = new UUID(UUID_PUBLICBROWSE_GROUP);
						browseClassIDList.addElement(new DataElement(DataElement.UUID, browseClassUUID));
						record.setAttributeValue(ATTR_BROWSE_GRP_LIST, browseClassIDList);

						localDevice.updateRecord(record);
					} catch (Throwable e) {
						Logger.error("OBEX Service Updating SDP", e);
					}
				} else if (Configuration.testServiceAttributes.booleanValue()) {
					ServiceRecord record = localDevice.getRecord(serverConnection);
					if (record == null) {
						Logger.warn("Bluetooth ServiceRecord is null");
					} else {
						TestResponderServer.buildServiceRecord(record);

						try {
							record.setDeviceServiceClasses(BluetoothTypesInfo.DeviceClassConsts.INFORMATION_SERVICE);
							deviceServiceClassesUpdated = true;
						} catch (Throwable e) {
							if (e.getMessage().startsWith("Not Supported on")) {
								Logger.error("setDeviceServiceClasses " + e.getMessage());
							} else {
								Logger.error("setDeviceServiceClasses", e);
							}
						}

						try {
							LocalDevice.getLocalDevice().updateRecord(record);
							Logger.debug("OBEX ServiceRecord updated");
						} catch (Throwable e) {
							Logger.error("OBEX Service Record update error", e);
						}
					}
				}
			}
		} catch (Throwable e) {
			Logger.error("OBEX Server start error", e);
			isStoped = true;
			return;
		}

		if (deviceServiceClassesUpdated) {
			Logger.info("DeviceClass:" + BluetoothTypesInfo.toString(localDevice.getDeviceClass()));
		}

		try {
			int errorCount = 0;
			int count = 0;
			isRunning = true;
			boolean showServiceRecordOnce = true;
			while (!isStoped) {
				RequestHandler handler = new RequestHandler();
				try {
					count++;
					Logger.info("Accepting OBEX connections");
					if (showServiceRecordOnce) {
						try {
							Logger.debug("OxUrl:"
									+ localDevice.getRecord(serverConnection).getConnectionURL(
											Configuration.getRequiredSecurity(), false));
						} catch (IllegalArgumentException e) {
							Logger.debug("Can't get local serviceRecord", e);
						}
						showServiceRecordOnce = false;
					}
					if (Configuration.authenticateOBEX.getValue() != 0) {
						handler.auth = new OBEXTestAuthenticator("server" + count);
						handler.connectionAccepted(serverConnection.acceptAndOpen(handler, handler.auth));
					} else {
						handler.connectionAccepted(serverConnection.acceptAndOpen(handler));
					}
				} catch (InterruptedIOException e) {
					isStoped = true;
					break;
				} catch (Throwable e) {
					if (errorCount > 3) {
						isStoped = true;
					}
					if (isStoped) {
						return;
					}
					errorCount++;
					Logger.error("acceptAndOpen ", e);
					continue;
				}
				errorCount = 0;
			}
		} finally {
			close();
			Logger.info("OBEX Server finished! " + TimeUtils.timeNowToString());
			isRunning = false;
		}
	}

	void close() {
		try {
			if (serverConnection != null) {
				serverConnection.close();
			}
			Logger.debug("OBEX ServerConnection closed");
		} catch (Throwable e) {
			Logger.error("OBEX Server stop error", e);
		}
	}

	void closeServer() {
		isStoped = true;
		close();
	}

	/*
	 * We testing on Java 1.1 and Timer is not important
	 */
	private class NoTimeWrapper {

		Object timer;

		NoTimeWrapper() {
			try {
				timer = new Timer();
			} catch (Throwable e) {
				Logger.warn("OBEX Server has no timer");
			}
		}

		void schedule(final RequestHandler handler) {
			if (timer != null) {
				((Timer) timer).schedule(new TimerTask() {
					public void run() {
						handler.notConnectedClose();
					}
				}, 1000 * 30);
			}
		}

		void cancel() {
			if (timer != null) {
				((Timer) timer).cancel();
			}
		}
	}

	private class RequestHandler extends ServerRequestHandler {

		OBEXTestAuthenticator auth;

		NoTimeWrapper notConnectedTimer = new NoTimeWrapper();

		boolean isConnected = false;

		Connection cconn;

		RemoteDevice remoteDevice;

		void connectionAccepted(Connection cconn) {
			Logger.info("Received OBEX connection");
			this.cconn = cconn;
			if (!Configuration.testServerOBEX_TCP.booleanValue()) {
				try {
					remoteDevice = RemoteDevice.getRemoteDevice(cconn);
					Logger.debug("connected toBTAddress " + remoteDevice.getBluetoothAddress());
				} catch (IOException e) {
					Logger.error("OBEX Server error", e);
				}
			}
			if (!isConnected) {
				notConnectedTimer.schedule(this);
			}
		}

		void notConnectedClose() {
			if (!isConnected) {
				Logger.debug("OBEX connection timeout");
				IOUtils.closeQuietly(cconn);
			}
		}

		private void debugHeaderSet(HeaderSet headers) {
			if (headers == null) {
				return;
			}
			try {
				int[] headerIDArray = headers.getHeaderList();
				if (headerIDArray == null) {
					return;
				}
				Logger.debug("Headers.length:" + headerIDArray.length);

				for (int i = 0; i < headerIDArray.length; i++) {
					int hi = headerIDArray[i];
					Object value = headers.getHeader(hi);
					Logger.debug("h[" + hi + "]=" + value);
				}
			} catch (IOException e) {
				Logger.error("headers", e);
			}
		}

		public int onConnect(HeaderSet request, HeaderSet reply) {
			isConnected = true;
			notConnectedTimer.cancel();
			Logger.debug("OBEX onConnect");
			debugHeaderSet(request);
			if (Configuration.authenticate.booleanValue()) {
				if (!remoteDevice.isAuthenticated()) {
					return ResponseCodes.OBEX_HTTP_FORBIDDEN;
				}
				Logger.debug("OBEX connection Authenticated");
			}
			return ResponseCodes.OBEX_HTTP_OK;
		}

		public void onDisconnect(HeaderSet request, HeaderSet reply) {
			Logger.debug("OBEX onDisconnect");
			debugHeaderSet(request);
		}

		public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {
			Logger.debug("OBEX onSetPath");
			debugHeaderSet(request);
			return super.onSetPath(request, reply, backup, create);
		}

		public int onDelete(HeaderSet request, HeaderSet reply) {
			Logger.debug("OBEX onDelete");
			debugHeaderSet(request);
			return super.onDelete(request, reply);
		}

		public int onPut(Operation op) {
			Logger.debug("OBEX onPut");
			try {
				HeaderSet hs = op.getReceivedHeaders();
				debugHeaderSet(hs);
				String name = (String) hs.getHeader(HeaderSet.NAME);
				if (name != null) {
					Logger.debug("name:" + name);

					HeaderSet sendHeaders = createHeaderSet();
					sendHeaders.setHeader(HeaderSet.DESCRIPTION, name);
					op.sendHeaders(sendHeaders);
				}

				InputStream is = op.openInputStream();

				StringBuffer buf = new StringBuffer();
				while (!isStoped) {
					int data = is.read();
					if (data == -1) {
						Logger.debug("EOS recived");
						break;
					}
					char c = (char) data;
					buf.append(c);
					if ((c == '\n') || (buf.length() > 30)) {
						Logger.debug("cc:" + StringUtils.toBinaryText(buf));
						buf = new StringBuffer();
					}
				}
				if (buf.length() > 0) {
					Logger.debug("cc:" + StringUtils.toBinaryText(buf));
				}
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				Logger.error("OBEX Server onPut error", e);
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			} finally {
				Logger.debug("OBEX onPut ends");
			}
		}

		public int onGet(Operation op) {
			Logger.debug("OBEX onGet");
			String message = "Hello client! now " + new Date().toString();
			try {
				HeaderSet hs = op.getReceivedHeaders();
				debugHeaderSet(hs);
				String name = (String) hs.getHeader(HeaderSet.NAME);

				if (name != null) {
					message += "\nYou ask for [" + name + "]";
				}
				byte[] messageBytes = message.getBytes();
				if (name != null) {
					HeaderSet sendHeaders = createHeaderSet();
					sendHeaders.setHeader(HeaderSet.DESCRIPTION, name);
					sendHeaders.setHeader(HeaderSet.LENGTH, new Long(messageBytes.length));
					op.sendHeaders(sendHeaders);
				}

				OutputStream os = op.openOutputStream();
				os.write(messageBytes);
				os.flush();
				os.close();
				op.close();
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				Logger.error("OBEX Server onGet error", e);
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			} finally {
				Logger.debug("OBEX onGet ends");
			}
		}

		public void onAuthenticationFailure(byte[] userName) {
			Logger.debug("OBEX AuthFailure " + new String(userName));
		}

	}
}
