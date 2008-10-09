/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package net.sf.bluecove.obex.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

/**
 * @author vlads
 * 
 */
public class OBEXServer implements Runnable {

	private SessionNotifier serverConnection;

	private boolean isStoped = false;

	private boolean isRunning = false;

	public final UUID OBEX_OBJECT_PUSH = new UUID(0x1105);

	public static final String SERVER_NAME = "OBEX Object Push";

	private UserInteraction interaction;

	private OBEXServer(UserInteraction interaction) {
		this.interaction = interaction;
	}

	public static OBEXServer startServer(UserInteraction interaction) {
		OBEXServer srv = new OBEXServer(interaction);
		Thread thread = new Thread(srv);
		thread.start();
		while (!srv.isRunning && !srv.isStoped) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new Error(e);
			}
		}
		if (!srv.isRunning) {
			throw new Error("Can't start server");
		}
		return srv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		isStoped = false;
		LocalDevice localDevice;
		try {
			localDevice = LocalDevice.getLocalDevice();
			if (!localDevice.setDiscoverable(DiscoveryAgent.GIAC)) {
				Logger.error("Fail to set LocalDevice Discoverable");
			}
			serverConnection = (SessionNotifier) Connector.open("btgoep://localhost:" + OBEX_OBJECT_PUSH + ";name="
					+ SERVER_NAME);
		} catch (Throwable e) {
			Logger.error("OBEX Server start error", e);
			isStoped = true;
			return;
		}

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
			Logger.error("Updating SDP", e);
		}

		try {
			int errorCount = 0;
			int count = 0;
			isRunning = true;
			while (!isStoped) {
				RequestHandler handler = new RequestHandler();
				try {
					count++;
					Logger.debug("Accepting OBEX connections");
					handler.connectionAccepted(serverConnection.acceptAndOpen(handler));
				} catch (InterruptedIOException e) {
					isStoped = true;
					break;
				} catch (Throwable e) {
					if ("Stack closed".equals(e.getMessage())) {
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
			Logger.debug("OBEX Server finished!");
			isRunning = false;
		}
	}

	public void close() {
		isStoped = true;
		try {
			if (serverConnection != null) {
				serverConnection.close();
			}
			Logger.debug("OBEX ServerConnection closed");
		} catch (Throwable e) {
			Logger.error("OBEX Server stop error", e);
		}
	}

	private static File homePath() {
		String path = "bluetooth";
		boolean isWindows = false;
		String sysName = System.getProperty("os.name");
		if (sysName != null) {
			sysName = sysName.toLowerCase();
			if (sysName.indexOf("windows") != -1) {
				isWindows = true;
				path = "My Documents";
			}
		}
		File dir;
		try {
			dir = new File(System.getProperty("user.home"), path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					throw new SecurityException();
				}
			}
		} catch (SecurityException e) {
			dir = new File(new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name")), path);
		}
		if (isWindows) {
			dir = new File(dir, "Bluetooth Exchange Folder");
		}
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				return null;
			}
		} else if (!dir.isDirectory()) {
			dir.delete();
			if (!dir.mkdirs()) {
				return null;
			}
		}
		return dir;
	}

	private void showStatus(final String message) {
		interaction.showStatus(message);
	}

	private class RequestHandler extends ServerRequestHandler {

		Timer notConnectedTimer = new Timer();

		boolean isConnected = false;

		boolean receivedOk = false;

		Connection cconn;

		void connectionAccepted(Connection cconn) {
			Logger.debug("Received OBEX connection");
			showStatus("Client connected");
			this.cconn = cconn;
			if (!isConnected) {
				notConnectedTimer.schedule(new TimerTask() {
					public void run() {
						notConnectedClose();
					}
				}, 1000 * 30);
			}
		}

		void notConnectedClose() {
			if (!isConnected) {
				Logger.debug("OBEX connection timeout");
				try {
					cconn.close();
				} catch (IOException e) {
				}
				if (!receivedOk) {
					showStatus("Disconnected");
				}
			}
		}

		public int onConnect(HeaderSet request, HeaderSet reply) {
			isConnected = true;
			notConnectedTimer.cancel();
			Logger.debug("OBEX onConnect");
			return ResponseCodes.OBEX_HTTP_OK;
		}

		public void onDisconnect(HeaderSet request, HeaderSet reply) {
			Logger.debug("OBEX onDisconnect");
			if (!receivedOk) {
				showStatus("Disconnected");
			}
		}

		public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {
			Logger.debug("OBEX onSetPath");
			return super.onSetPath(request, reply, backup, create);
		}

		public int onDelete(HeaderSet request, HeaderSet reply) {
			Logger.debug("OBEX onDelete");
			return super.onDelete(request, reply);
		}

		public int onPut(Operation op) {
			Logger.debug("OBEX onPut");
			try {
				HeaderSet hs = op.getReceivedHeaders();
				String name = (String) hs.getHeader(HeaderSet.NAME);
				if (name != null) {
					Logger.debug("name:" + name);
					showStatus("Receiving " + name);
				} else {
					name = "xxx.xx";
					showStatus("Receiving file");
				}
				Long len = (Long) hs.getHeader(HeaderSet.LENGTH);
				if (len != null) {
					Logger.debug("file lenght:" + len);
					interaction.setProgressValue(0);
					interaction.setProgressMaximum(len.intValue());
				}
				File f = new File(homePath(), name);
				FileOutputStream out = new FileOutputStream(f);
				InputStream is = op.openInputStream();

				int received = 0;

				while (!isStoped) {
					int data = is.read();
					if (data == -1) {
						Logger.debug("EOS received");
						break;
					}
					out.write(data);
					received++;
					if ((len != null) && (received % 100 == 0)) {
						interaction.setProgressValue(received);
					}
				}
				op.close();
				out.close();
				Logger.debug("file saved:" + f.getAbsolutePath());
				showStatus("Received " + name);
				receivedOk = true;
				return ResponseCodes.OBEX_HTTP_OK;
			} catch (IOException e) {
				Logger.error("OBEX Server onPut error", e);
				return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
			} finally {
				Logger.debug("OBEX onPut ends");
				interaction.setProgressDone();
			}
		}

		public int onGet(Operation op) {
			Logger.debug("OBEX onGet");
			try {
				HeaderSet hs = op.getReceivedHeaders();
				String name = (String) hs.getHeader(HeaderSet.NAME);

				return ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED;

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
