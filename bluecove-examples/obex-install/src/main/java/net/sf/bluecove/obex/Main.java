/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * @author vlads
 * 
 */
public class Main extends JFrame implements ActionListener, UserInteraction {

	private static final long serialVersionUID = 1L;

	private static final int BLUETOOTH_DISCOVERY_STD_SEC = 11;

	private JLabel iconLabel;

	private String status;

	JProgressBar progressBar;

	private ImageIcon btIcon;

	private ImageIcon transferIcon;

	private ImageIcon searchIcon;

	private ImageIcon downloadIcon;

	private JComboBox cbDevices;

	private JButton btFindDevice;

	private JButton btSend;

	private JButton btCancel;

	private BluetoothInquirer bluetoothInquirer;

	private Hashtable devices = new Hashtable();

	private JFileChooser fileChooser;

	private String fileName;

	private byte[] data;

	private List queue = new Vector();

	protected Main() {
		super("BlueCove OBEX Push");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Image btImage = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png"));
		btIcon = new ImageIcon(btImage);
		transferIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/transfer.png"))));
		searchIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/search.png"))));
		downloadIcon = new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/download.png"))));

		this.setIconImage(btImage);

		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout(10, 10));
		contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
		contentPane.setTransferHandler(new DropTransferHandler(this));

		contentPane.addMouseListener(new MouseDoubleClickListener());

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

		JPanel optionsPanel = new JPanel();

		JLabel deviceLabel = new JLabel("Send to:");
		optionsPanel.add(deviceLabel);
		cbDevices = new JComboBox();
		cbDevices.addItem("{no device found}");
		cbDevices.setEnabled(false);
		optionsPanel.add(cbDevices);
		optionsPanel.add(btFindDevice = new JButton("Find"));
		btFindDevice.addActionListener(this);

		getContentPane().add(optionsPanel, BorderLayout.CENTER);

		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.LINE_AXIS));
		actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		actionPanel.add(Box.createHorizontalGlue());
		actionPanel.add(btSend = new JButton("Send"));
		btSend.addActionListener(this);
		actionPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		actionPanel.add(btCancel = new JButton("Cancel"));
		btCancel.addActionListener(this);

		contentPane.add(actionPanel, BorderLayout.SOUTH);
		btSend.setEnabled(false);
		String selected = Persistence.loadDevices(devices);
		updateDevices(selected);
	}

	private static void createAndShowGUI(final String[] args) {
		final Main app = new Main();
		app.pack();
		app.center();
		app.setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (app.initializeBlueCove()) {
					if (args.length != 0) {
						app.downloadFile(args[0]);
					}
				}
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

	public void showStatus(final String message) {
		setStatus(message);
	}

	protected void setStatus(final String message) {
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
		btFindDevice.setEnabled(false);
		cbDevices.setEnabled(false);
		setStatus("BlueCove not avalable");
		btSend.setEnabled(false);
		iconLabel.setIcon(new ImageIcon((Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/bt-off.png")))));
	}

	protected boolean initializeBlueCove() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			if ("000000000000".equals(localDevice.getBluetoothAddress())) {
				throw new Exception();
			}
			bluetoothInquirer = new BluetoothInquirer(this);
			setStatus("BlueCove Ready");
			return true;
		} catch (Throwable e) {
			Logger.error(e);
			disabledBluetooth();
			return false;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btFindDevice) {
			bluetoothDiscovery();
		} else if (e.getSource() == btCancel) {
			shutdown();
			System.exit(0);
		} else if (e.getSource() == btSend) {
			obexSend();
		}
	}

	private class MouseDoubleClickListener implements MouseListener {

		private long firstClick = 0;

		public void mouseClicked(MouseEvent e) {
			long now = System.currentTimeMillis();
			if ((firstClick != 0) && (firstClick - now < 1000)) {
				fireDoubleClick();
			} else {
				firstClick = now;
			}

		}

		public void mouseEntered(MouseEvent e) {
			firstClick = 0;
		}

		public void mouseExited(MouseEvent e) {
			firstClick = 0;
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

	}

	public void fireDoubleClick() {
		if (fileChooser == null) {
			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select File to send...");
			fileChooser.setCurrentDirectory(new File(Persistence.getProperty("recentDirectory", ".")));
		}
		int returnVal = fileChooser.showOpenDialog(Main.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			Persistence.setProperty("recentDirectory", fileChooser.getCurrentDirectory().getAbsolutePath());
			downloadFile(DropTransferHandler.getCanonicalFileURL(fileChooser.getSelectedFile()));
			saveConfig();
		}

	}

	private void selectNextFile() {
		if (queue.size() > 0) {
			String url = (String) queue.remove(0);
			downloadFile(url);
		}
	}

	public void queueFile(String url) {
		queue.add(url);
	}

	private void saveConfig() {
		Persistence.storeDevices(devices, getSelectedDeviceAddress());
	}

	private class DiscoveryTimerListener implements ActionListener {
		int seconds = 0;

		public void actionPerformed(ActionEvent e) {
			if (seconds < BLUETOOTH_DISCOVERY_STD_SEC) {
				seconds++;
				setProgressValue(seconds);
			}
		}
	}

	private void addDevice(String btAddress, String name, String obexUrl) {
		String key = btAddress.toLowerCase();
		DeviceInfo di = (DeviceInfo) devices.get(key);
		if (di == null) {
			di = new DeviceInfo();
		}
		di.btAddress = btAddress;
		// Update name if one found
		if (di.name == null) {
			di.name = name;
		} else if (btAddress.equals(di.name)) {
			di.name = name;
		}
		di.obexUrl = obexUrl;
		di.obexServiceFound = true;
		devices.put(key, di);
	}

	private void updateDevices(String selected) {
		cbDevices.removeAllItems();
		if (devices.size() == 0) {
			cbDevices.addItem("{no device found}");
			btSend.setEnabled(false);
			cbDevices.setEnabled(false);
		} else {
			for (Enumeration i = devices.keys(); i.hasMoreElements();) {
				String addr = (String) i.nextElement();
				DeviceInfo di = (DeviceInfo) devices.get(addr);
				cbDevices.addItem(di);
				if ((selected != null) && (selected.equals(di.btAddress))) {
					cbDevices.setSelectedItem(di);
				}
			}
			cbDevices.setEnabled(true);
			btSend.setEnabled(true);
		}
	}

	private void bluetoothDiscovery() {
		final Timer timer = new Timer(1000, new DiscoveryTimerListener());
		progressBar.setMaximum(BLUETOOTH_DISCOVERY_STD_SEC);
		setProgressValue(0);
		Thread t = new Thread() {
			public void run() {
				if (bluetoothInquirer.startInquiry()) {
					iconLabel.setIcon(searchIcon);
					setStatus("Bluetooth discovery started");
					btFindDevice.setEnabled(false);
					btSend.setEnabled(false);
					timer.start();
					while (bluetoothInquirer.inquiring) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
					}
					timer.stop();
					// setStatus("Bluetooth discovery finished");

					setProgressValue(0);
					int idx = 0;
					progressBar.setMaximum(bluetoothInquirer.devices.size());
					for (Iterator iter = bluetoothInquirer.devices.iterator(); iter.hasNext();) {
						RemoteDevice dev = (RemoteDevice) iter.next();
						String obexUrl = bluetoothInquirer.findOBEX(dev.getBluetoothAddress());
						if (obexUrl != null) {
							Logger.debug("found obex url", obexUrl);
							addDevice(dev.getBluetoothAddress(), BluetoothInquirer.getFriendlyName(dev), obexUrl);
						}
						idx++;
						setProgressValue(idx);
					}
					setProgressValue(0);
					saveConfig();
					updateDevices(null);
					btFindDevice.setEnabled(true);
					btSend.setEnabled(true);
					iconLabel.setIcon(btIcon);
				}
			}
		};
		t.start();
	}

	private String blueSoleilFindOBEX(String btAddress, String obexUrl) {
		if ("bluesoleil".equals(LocalDevice.getProperty("bluecove.stack"))) {
			RemoteDevice dev = new RemoteDeviceExt(btAddress);
			String foundObexUrl = bluetoothInquirer.findOBEX(dev.getBluetoothAddress());
			if (foundObexUrl != null) {
				Logger.debug("found", btAddress);
				addDevice(dev.getBluetoothAddress(), BluetoothInquirer.getFriendlyName(dev), foundObexUrl);
			}
			return foundObexUrl;
		}
		return obexUrl;
	}

	private DeviceInfo getSelectedDevice() {
		Object o = cbDevices.getSelectedItem();
		if ((o == null) || !(o instanceof DeviceInfo)) {
			return null;
		}
		return (DeviceInfo) o;
	}

	private String getSelectedDeviceAddress() {
		DeviceInfo d = getSelectedDevice();
		if (d == null) {
			return null;
		}
		return d.btAddress;
	}

	private void obexSend() {
		if (fileName == null) {
			setStatus("No file selected");
			return;
		}
		final DeviceInfo d = getSelectedDevice();
		if (d == null) {
			setStatus("No Device selected");
			return;
		}
		final ObexBluetoothClient o = new ObexBluetoothClient(this, fileName, data);
		Thread t = new Thread() {
			public void run() {
				btSend.setEnabled(false);
				iconLabel.setIcon(transferIcon);
				String obexUrl = d.obexUrl;
				if (!d.obexServiceFound) {
					obexUrl = blueSoleilFindOBEX(d.btAddress, obexUrl);
				}
				if (obexUrl != null) {
					if (o.obexPut(obexUrl)) {
						selectNextFile();
					}
				} else {
					setStatus("Service not found");
				}
				btSend.setEnabled(true);
				iconLabel.setIcon(btIcon);
				saveConfig();
			}

		};
		t.start();
	}

	private static String simpleFileName(String filePath) {
		int idx = filePath.lastIndexOf('/');
		if (idx == -1) {
			idx = filePath.lastIndexOf('\\');
		}
		if (idx == -1) {
			return filePath;
		}
		return filePath.substring(idx + 1);
	}

	void downloadFile(final String filePath) {
		Thread t = new Thread() {
			public void run() {
				InputStream is = null;
				try {
					iconLabel.setIcon(downloadIcon);
					String path = filePath;
					String inputFileName;
					File file = new File(filePath);
					if (file.exists()) {
						is = new FileInputStream(file);
						inputFileName = file.getName();
					} else {
						URL url = new URL(path);
						is = url.openConnection().getInputStream();
						inputFileName = url.getFile();
					}
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					byte[] buffer = new byte[0xFF];
					int i = is.read(buffer);
					int done = 0;
					while (i != -1) {
						bos.write(buffer, 0, i);
						done += i;
						// setProgressValue(done);
						i = is.read(buffer);
					}
					data = bos.toByteArray();
					fileName = simpleFileName(inputFileName);
					setStatus((data.length / 1024) + "k " + fileName);
				} catch (Throwable e) {
					Logger.error(e);
					setStatus("Download error " + e.getMessage());
				} finally {
					IOUtils.closeQuietly(is);
					iconLabel.setIcon(btIcon);
				}
			}
		};
		t.start();

	}

	private void shutdown() {
		if (bluetoothInquirer != null) {
			bluetoothInquirer.shutdown();
			bluetoothInquirer = null;
		}
	}
}
