/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007-2009 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex.server;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.bluetooth.LocalDevice;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * 
 */
public class Main extends JFrame implements ActionListener, UserInteraction {

	private static final long serialVersionUID = 1L;

	private JLabel iconLabel;

	private String status;

	JProgressBar progressBar;

	private ImageIcon btIcon;

	private ImageIcon transferIcon;

	private JButton btExit;

	private OBEXServer server;

	private static void createAndShowGUI(final String[] args) {
		final Main app = new Main();
		app.pack();
		app.center();
		app.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				app.initializeServer();
			}
		});
	}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(args);
			}
		});
	}

	private void center() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(((screenSize.width - this.getWidth()) / 2), ((screenSize.height - this.getHeight()) / 2));
	}

	private Main() {
		super("BlueCove OBEX Server");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Image btImage = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png"));
		btIcon = new ImageIcon(btImage);
		transferIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/transfer.png"))));

		this.setIconImage(btImage);

		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout(10, 10));
		contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		iconLabel = new JLabel();
		iconLabel.setIcon(btIcon);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		progressPanel.add(iconLabel, c);

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		progressPanel.add(progressBar, c);

		getContentPane().add(progressPanel, BorderLayout.NORTH);

		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.LINE_AXIS));
		actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		actionPanel.add(Box.createHorizontalGlue());

		actionPanel.add(btExit = new JButton("Exit"));
		btExit.addActionListener(this);

		contentPane.add(actionPanel, BorderLayout.SOUTH);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btExit) {
			shutdown();
			System.exit(0);
		}
	}

	public void showStatus(final String message) {
		status = message;
		progressBar.setString(message);
	}

	public void setProgressMaximum(int n) {
		progressBar.setMaximum(n);
	}

	public void setProgressValue(int n) {
		progressBar.setValue(n);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setString(status);
			}
		});
	}

	public void setProgressDone() {
		progressBar.setValue(0);
	}

	protected void disabledBluetooth() {
		showStatus("BlueCove not avalable");
		iconLabel.setIcon(new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/bt-off.png")))));
	}

	private boolean initializeServer() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			if ("000000000000".equals(localDevice.getBluetoothAddress())) {
				throw new Exception();
			}
			server = OBEXServer.startServer(this);
			showStatus("BlueCove Ready");
			return true;
		} catch (Throwable e) {
			Logger.debug(e);
			disabledBluetooth();
			return false;
		}
	}

	private void shutdown() {
		if (server != null) {
			server.close();
		}
	}
}
