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
 * @author vlads
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
