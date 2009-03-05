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
package net.sf.bluecove.awt;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.RuntimeDetect;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.RemoteDeviceIheritance;
import net.sf.bluecove.TestResponderCommon;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.J2MEStringTokenizer;
import net.sf.bluecove.util.Storage;

import com.intel.bluetooth.RemoteDeviceHelper;

/**
 *
 * 
 */
public class ClientConnectionDialog extends Dialog {

	private static final long serialVersionUID = 1L;

	private static final String configConnectionURL = "connectionURL";

	private static final String configLastDataSent = "lastDataSent";

	Button btnConnect, btnDisconnect, btnCancel, btnSend, btnInterrupt;

	TextField tfURL;

	Choice choiceAllURLs;

	TextField tfData;

	Choice choiceDataSendType;

	Choice choiceDataReceiveType;

	Label status;

	Checkbox cbSaveToFile;

	Timer monitorTimer;

	Object threadLocalBluetoothStack;

	String localBluetoothStackAddress = "";

	ClientConnectionThread thread;

	boolean inSendLoop = false;

	private int connectionWindowID = 0;

	private static Vector openConnectionDialogs = new Vector();

	private static final String RECENT_RFCOMM_URLS = "recentRFCOMM";

	private static Vector recentConnections = new Vector();

	private class ConnectionMonitor extends TimerTask {

		boolean wasConnected = false;

		boolean wasStarted = false;

		int connectingCount = 0;

		public void run() {
			if ((openConnectionDialogs.size() > 1) || (Configuration.hasManyDevices)) {
				String title = localBluetoothStackAddress + " Client Connection " + connectionWindowID;
				if (thread != null) {
					thread.logPrefix = "[" + connectionWindowID + "]";
					title += " " + thread.getLocalBluetoothId();
				}
				ClientConnectionDialog.this.setTitle(title);
			}
			if (thread == null) {
				if (wasConnected || wasStarted) {
					status.setText("Idle");
					setCursorDefault();
					btnDisconnect.setEnabled(false);
					btnConnect.setEnabled(true);
					btnSend.setEnabled(false);
					btnInterrupt.setEnabled(false);
					wasConnected = false;
					wasStarted = false;
					connectingCount = 0;
				}
			} else if (thread.isRunning) {
				if (!wasConnected) {
					setCursorDefault();
					btnSend.setEnabled(true);
				}
				wasConnected = true;
				if (thread.receivedCount == 0) {
					status.setText("Connected");
				} else {
					status.setText("Received " + thread.receivedCount);
				}
			} else {
				wasStarted = true;
				if (thread.isConnecting) {
					StringBuffer progress = new StringBuffer("Connecting ");
					for (int i = 0; i <= connectingCount; i++) {
						progress.append('.');
					}
					status.setText(progress.toString());
					connectingCount++;
				} else {
					setCursorDefault();
					status.setText("Disconnected");
					connectingCount = 0;
				}
			}
		}
	}

	public ClientConnectionDialog(Frame owner) {
		super(owner, "Client Connection", false);

		TestResponderCommon.initLocalDevice();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		Panel panelItems = new BorderPanel(gridbag);
		this.add(panelItems, BorderLayout.NORTH);

		Label lURL = new Label("URL:");
		panelItems.add(lURL);
		panelItems.add(tfURL = new TextField("", 25));
		c.gridwidth = 1;
		gridbag.setConstraints(lURL, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tfURL, c);

		if (Configuration.storage != null) {
			String url = Configuration.storage.retriveData(configConnectionURL);
			if (url == null) {
				url = Configuration.storage.retriveData(Storage.configLastServiceURL);
			}
			tfURL.setText(url);
		}

		Label lDiscovered = new Label("Discovered:");
		panelItems.add(lDiscovered);
		choiceAllURLs = new Choice();
		c.gridwidth = 1;
		gridbag.setConstraints(lDiscovered, c);
		panelItems.add(choiceAllURLs);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(choiceAllURLs, c);

		Font logFont = new Font("Monospaced", Font.PLAIN, Configuration.screenSizeSmall ? 9 : 12);
		choiceAllURLs.setFont(logFont);

		ServiceRecords.populateChoice(choiceAllURLs, false);
		populateRecentConnections(choiceAllURLs);
		choiceAllURLs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				selectURL();
			}
		});

		Label lData = new Label("Data:");
		panelItems.add(lData);
		panelItems.add(tfData = new TextField());
		c.gridwidth = 1;
		gridbag.setConstraints(lData, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(tfData, c);

		if (Configuration.storage != null) {
			String d = Configuration.storage.retriveData(configLastDataSent);
			if (d != null) {
				tfData.setText(d);
			}
		}

		Label l3 = new Label("");
		panelItems.add(l3);
		c.gridwidth = 1;
		gridbag.setConstraints(l3, c);

		panelItems.add(btnSend = new Button("Send"));
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});
		btnSend.setEnabled(false);
		c.gridwidth = 1;
		gridbag.setConstraints(btnSend, c);

		choiceDataSendType = new Choice();
		choiceDataSendType.add("as String.getBytes()+CR");
		choiceDataSendType.add("as String.getBytes()");
		choiceDataSendType.add("as parseByte(text)");
		choiceDataSendType.add("Continuously");
		// choiceDataType.add("as byte list");

		panelItems.add(choiceDataSendType);
		c.gridwidth = 1;
		gridbag.setConstraints(choiceDataSendType, c);

		Label lReceive = new Label("  Receive:");
		panelItems.add(lReceive);
		c.gridwidth = 1;
		gridbag.setConstraints(lReceive, c);

		choiceDataReceiveType = new Choice();
		choiceDataReceiveType.add("as char");
		choiceDataReceiveType.add("as charArray");
		choiceDataReceiveType.add("stats only char");
		choiceDataReceiveType.add("stats only charArray");
		choiceDataReceiveType.add("do not read");
		// choiceDataType.add("as byte list");
		// choiceDataReceiveType.add("as Echo");
		panelItems.add(choiceDataReceiveType);
		c.gridwidth = 1;
		gridbag.setConstraints(choiceDataReceiveType, c);

		choiceDataReceiveType.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateDataReceiveType();
			}
		});

		Label lRemainder = new Label("");
		panelItems.add(lRemainder);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(lRemainder, c);

		Label lSaveToFile = new Label("Save to file:");
		panelItems.add(lSaveToFile);
		panelItems.add(cbSaveToFile = new Checkbox());
		c.gridwidth = 1;
		gridbag.setConstraints(lSaveToFile, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(cbSaveToFile, c);
		cbSaveToFile.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateDataReceiveType();
			}
		});

		Label lStatus = new Label("Status:");
		panelItems.add(lStatus);
		c.gridwidth = 1;
		gridbag.setConstraints(lStatus, c);

		status = new Label("Idle");
		panelItems.add(status);
		c.gridwidth = 2;
		gridbag.setConstraints(status, c);

		Panel panelBtns = new Panel();
		this.add(panelBtns, BorderLayout.SOUTH);

		panelBtns.add(btnConnect = new Button("Connect"));
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});

		panelBtns.add(btnDisconnect = new Button("Disconnect"));
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				shutdown();
			}
		});
		btnDisconnect.setEnabled(false);

		panelBtns.add(btnInterrupt = new Button("Interrupt"));
		btnInterrupt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				interrupt();
			}
		});
		btnInterrupt.setEnabled(false);

		if (RuntimeDetect.isBlueCove) {
			Button btnBond = new Button("Bond");
			panelBtns.add(btnBond);
			btnBond.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onBond();
				}
			});

			Button btnUnBond = new Button("UnBond");
			panelBtns.add(btnUnBond);
			btnUnBond.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onUnBond();
				}
			});

			Button btnInfo = new Button("Info");
			panelBtns.add(btnInfo);
			btnInfo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onInfo();
				}
			});
		}

		panelBtns.add(btnCancel = new Button("Cancel"));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		});

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
		this.pack();

		synchronized (openConnectionDialogs) {
			int maxId = 0;
			for (Enumeration iter = openConnectionDialogs.elements(); iter.hasMoreElements();) {
				ClientConnectionDialog w = (ClientConnectionDialog) iter.nextElement();
				if (maxId < w.connectionWindowID) {
					maxId = w.connectionWindowID;
				}
			}
			this.connectionWindowID = maxId + 1;
			openConnectionDialogs.add(this);
		}

		OkCancelDialog.centerParent(this);

		try {
			monitorTimer = new Timer();
			monitorTimer.schedule(new ConnectionMonitor(), 1000, 1000);
		} catch (Throwable java11) {
		}
	}

	protected void connect() {
		if (thread != null) {
			thread.shutdown();
			thread = null;
		}
		String url = tfURL.getText();
		storeRecentConnection(url);
		setCursorWait();
		thread = new ClientConnectionThread(url);
		thread.setDaemon(true);
		if (this.threadLocalBluetoothStack == null) {
			this.threadLocalBluetoothStack = Configuration.threadLocalBluetoothStack;
		}
		thread.threadLocalBluetoothStack = this.threadLocalBluetoothStack;

		if (localBluetoothStackAddress.length() == 0) {
			try {
				LocalDevice localDevice = LocalDevice.getLocalDevice();
				String bluetoothAddress = localDevice.getBluetoothAddress();
				localBluetoothStackAddress = bluetoothAddress;
				String w = TestResponderCommon.getWhiteDeviceName(bluetoothAddress);
				if (w != null) {
					localBluetoothStackAddress += "[" + w + "]";
				}
			} catch (BluetoothStateException e) {
				Logger.debug("local error", e);
			}
		}

		thread.start();
		btnDisconnect.setEnabled(true);
		btnInterrupt.setEnabled(true);
		btnConnect.setEnabled(false);
		updateDataReceiveType();
	}

	private static void populateRecentConnections(Choice choice) {
		loadRecentConnections();
		for (Enumeration en = recentConnections.elements(); en.hasMoreElements();) {
			choice.add((String) en.nextElement());
		}
	}

	static void loadRecentConnections() {
		synchronized (recentConnections) {
			if ((recentConnections.size() > 0) || (Configuration.storage == null)) {
				return;
			}
			String urls = Configuration.storage.retriveData(RECENT_RFCOMM_URLS);
			if (urls == null) {
				return;
			}
			J2MEStringTokenizer st = new J2MEStringTokenizer(urls, "|");
			if (st.hasMoreTokens()) {
				while (st.hasMoreTokens()) {
					String v = st.nextToken().trim();
					if ((v.length() > 0) && (!recentConnections.contains(v))) {
						recentConnections.add(v);
					}
				}
			} else {
				if (urls.length() > 0) {
					recentConnections.add(urls);
				}
			}
		}
	}

	static void storeRecentConnection(String url) {
		if (recentConnections.contains(url)) {
			recentConnections.remove(url);
		}
		recentConnections.add(url);
		Configuration.setLastServerURL(url);
		if (Configuration.storage == null) {
			return;
		}
		Configuration.storage.storeData(configConnectionURL, url);
		StringBuffer h = new StringBuffer();
		for (Enumeration iter = recentConnections.elements(); iter.hasMoreElements();) {
			h.append((String) iter.nextElement()).append("|");
		}
		Configuration.storage.storeData(RECENT_RFCOMM_URLS, h.toString());
	}

	protected void updateDataReceiveType() {
		if (thread != null) {
			thread.updateDataReceiveType(choiceDataReceiveType.getSelectedIndex(), cbSaveToFile.getState());
		}
	}

	protected void selectURL() {
		String url = ServiceRecords.getChoiceURL(choiceAllURLs);
		if (url != null) {
			tfURL.setText(url);
		}
	}

	protected void send() {
		inSendLoop = false;
		if (thread != null) {
			do {
				String text = tfData.getText();
				if (Configuration.storage != null) {
					Configuration.storage.storeData(configLastDataSent, text);
				}
				int type = choiceDataSendType.getSelectedIndex();
				byte data[];
				switch (type) {
				case 3: // Continuously
					inSendLoop = true;
				case 0:
					data = (text + "\n").getBytes();
					break;
				case 1:
					data = text.getBytes();
					break;
				case 2:
					J2MEStringTokenizer st = new J2MEStringTokenizer(text, ",");
					Vector bts = new Vector();
					while (st.hasMoreTokens()) {
						bts.addElement(st.nextToken().trim());
					}
					data = new byte[bts.size()];
					int j = 0;
					for (Enumeration en = bts.elements(); en.hasMoreElements();) {
						int i = Integer.parseInt((String) en.nextElement());
						data[j] = (byte) (i & 0xFF);
						j++;
					}
					break;
				default:
					data = new byte[] { 0 };
				}
				thread.send(data);
				if (inSendLoop) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
			} while (inSendLoop);
		}
	}

	public void shutdown() {
		if (thread != null) {
			thread.shutdown();
			thread = null;
		}
	}

	public void interrupt() {
		if (thread != null) {
			thread.interrupt();
			Logger.debug("thread.interrupt() called");
		}
	}

	protected void onClose() {
		synchronized (openConnectionDialogs) {
			openConnectionDialogs.remove(this);
		}
		shutdown();
		try {
			monitorTimer.cancel();
		} catch (Throwable java11) {
		}
		setVisible(false);
	}

	private void setCursorWait() {
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void setCursorDefault() {
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void onBond() {
		boolean test = false;
		if (test) {
			onNativeFunction();
			return;
		}

		String url = tfURL.getText();
		String pinStr = tfData.getText();
		if (pinStr.equals("null")) {
			pinStr = null;
		}
		final String pin = pinStr;
		final String deviceAddress = BluetoothTypesInfo.extractBluetoothAddress(url);
		if (deviceAddress == null) {
			Logger.error("invalid url");
			return;
		}
		final RemoteDevice device = new RemoteDeviceIheritance(deviceAddress);
		Logger.debug("authenticate:" + deviceAddress + " pin:" + pin);
		Thread t = new Thread("Authenticate") {
			public void run() {
				try {
					setCursorWait();
					RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);
					boolean rc = RemoteDeviceHelper.authenticate(device, pin);
					Logger.info("authenticate returns: " + rc);
				} catch (IOException e) {
					Logger.error("can't authenticate", e);
				} catch (Throwable e) {
					Logger.error("authenticate error", e);
				} finally {
					setCursorDefault();
				}
				Logger.debug(deviceAddress + " isAuthenticated", device.isAuthenticated());
				Logger.debug(deviceAddress + " isTrustedDevice", device.isTrustedDevice());
			}
		};
		t.start();
	}

	private void onUnBond() {
		String url = tfURL.getText();
		final String deviceAddress = BluetoothTypesInfo.extractBluetoothAddress(url);
		if (deviceAddress == null) {
			Logger.error("invalid url");
			return;
		}
		final RemoteDevice device = new RemoteDeviceIheritance(deviceAddress);
		Logger.debug("removed authentication:" + deviceAddress);
		Thread t = new Thread("UnAuthenticate") {
			public void run() {
				try {
					setCursorWait();
					RuntimeDetect.cldcStub.setThreadLocalBluetoothStack(threadLocalBluetoothStack);
					RemoteDeviceHelper.removeAuthentication(device);
					Logger.info("removed authentication");
				} catch (IOException e) {
					Logger.error("can't removed authentication", e);
				} catch (Throwable e) {
					Logger.error("removed authentication error", e);
				} finally {
					setCursorDefault();
				}
				Logger.debug(deviceAddress + " isAuthenticated", device.isAuthenticated());
				Logger.debug(deviceAddress + " isTrustedDevice", device.isTrustedDevice());
			}
		};
		t.start();
	}

	private void onNativeFunction() {
		String url = tfURL.getText();
		try {
			String deviceAddress = BluetoothTypesInfo.extractBluetoothAddress(url);
			if (deviceAddress == null) {
				Logger.error("invalid url");
				return;
			}
			Logger.debug(deviceAddress + " setSniffMode : "
					+ LocalDevice.getProperty("bluecove.nativeFunction:setSniffMode:" + deviceAddress));
			// Logger.debug(deviceAddress + " cancelSniffMode : "
			// +
			// LocalDevice.getProperty("bluecove.nativeFunction:cancelSniffMode:"
			// + deviceAddress));
		} catch (Throwable e) {
			Logger.error("error", e);
		}
	}

	private void onInfo() {
		String url = tfURL.getText();
		try {
			String deviceAddress = BluetoothTypesInfo.extractBluetoothAddress(url);
			if (deviceAddress == null) {
				Logger.error("invalid url");
				return;
			}
			RemoteDevice device = new RemoteDeviceIheritance(deviceAddress);
			Logger.debug(deviceAddress + " isAuthenticated", device.isAuthenticated());
			Logger.debug(deviceAddress + " isTrustedDevice", device.isTrustedDevice());

			Logger.debug(deviceAddress + " linkMode is:"
					+ LocalDevice.getProperty("bluecove.nativeFunction:getRemoteDeviceLinkMode:" + deviceAddress));
			Logger.debug(deviceAddress + " info:"
					+ LocalDevice.getProperty("bluecove.nativeFunction:getRemoteDeviceVersionInfo:" + deviceAddress));

			//B4 BlueCove 2.1.1
			//			Logger.debug(deviceAddress + " RSSI:"
			//					+ LocalDevice.getProperty("bluecove.nativeFunction:getRemoteDeviceRSSI:" + deviceAddress));
			try {
                Logger.debug(deviceAddress + " RSSI:", RemoteDeviceHelper.readRSSI(device));
            } catch (IOException e) {
                Logger.debug(deviceAddress + " RSSI:", e.getMessage());
            }
		} catch (Throwable e) {
			Logger.error("error", e);
		}
	}

}
